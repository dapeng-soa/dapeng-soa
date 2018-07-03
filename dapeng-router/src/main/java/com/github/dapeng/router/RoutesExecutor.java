package com.github.dapeng.router;

import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.router.condition.*;
import com.github.dapeng.router.pattern.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.dapeng.core.helper.IPUtils.matchIpWithMask;

/**
 * 描述:  按指定路由规则多可用服务实例进行过滤
 *
 * @author hz.lei
 * @date 2018年04月13日 下午10:02
 */
public class RoutesExecutor {

    private static Logger logger = LoggerFactory.getLogger(RoutesExecutor.class);
    private static final String COOKIE_PREFIX = "cookie_";

    /**
     * 解析 路由规则
     *
     * @param content
     * @return
     */
    public static List<Route> parseAll(String content) {
        RoutesParser parser = new RoutesParser(new RoutesLexer(content));
        List<Route> routes = parser.routes();
        return routes;
    }

    /**
     * 执行 路由规则 匹配， 返回 经过路由后的 实例列表
     */
    public static List<RuntimeInstance> executeRoutes(InvocationContextImpl ctx, List<Route> routes, List<RuntimeInstance> instances) {
        StringBuilder logAppend = new StringBuilder();
        instances.forEach(ins -> logAppend.append(ins.toString() + " "));
        logger.debug(RoutesExecutor.class.getSimpleName() + "::executeRoutes$开始过滤：过滤前 size  {}，实例: {}", instances.size(), logAppend.toString());
        boolean isMatched = false;
        for (Route route : routes) {
            try {
                isMatched = matchCondition(ctx, route.getLeft());
                // 匹配成功，执行右边逻辑
                if (isMatched) {
                    instances = matchThenRouteIp(instances, route);

                    if (logger.isDebugEnabled()) {
                        StringBuilder append = new StringBuilder();
                        instances.forEach(ins -> append.append(ins.toString() + " "));
                        logger.debug(RoutesExecutor.class.getSimpleName() + "::executeRoutes过滤结果 size: {}, 实例: {}",
                                instances.size(), append.toString());
                    }
                    break;
                } else {
                    logger.debug(RoutesExecutor.class.getSimpleName() + "::executeRoutes路由没有过滤, size {}", instances.size());
                }
            } catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return instances;
    }


    /**
     * 是否匹配左边
     *
     * @param ctx
     * @param left
     * @return
     */
    private static boolean matchCondition(InvocationContextImpl ctx, Condition left) {
        if (left instanceof Otherwise) {
            return true;
        }
        Matchers matcherCondition = (Matchers) left;
        List<Matcher> matchers = matcherCondition.macthers;
        /**
         * left = matcher(;matcher)*
         * matcher = id match patterns
         * patterns = pattern(,pattern)*
         * matcher之间是与的关系
         * pattern之间是或的关系
         */
        for (Matcher matcher : matchers) {
            String actuallyConditionValue = getValueFromInvocationCtx(ctx, matcher);
            List<Pattern> patterns = matcher.getPatterns();

            boolean isMatch = false;
            for (Pattern pattern : patterns) {
                boolean result = matcherPattern(pattern, actuallyConditionValue);
                if (result) {
                    isMatch = true;
                    break;
                }
            }
            if (!isMatch) {
                return false;
            }
        }
        return true;

    }

    /**
     * matchThenRouteIp  传入 RuntimeInstance 进行match
     *
     * @param instances
     * @param route
     * @return
     */
    private static List<RuntimeInstance> matchThenRouteIp(List<RuntimeInstance> instances, Route route) {
        // 获取 路由规则 then 之后 指向的 ip list
        List<ThenIp> thenRouteIps = route.getThenRouteIps();
        Set<ThenIp> ips = new HashSet<>(16);
        Set<ThenIp> notIps = new HashSet<>(16);

        thenRouteIps.forEach(ip -> {
            if (ip.not) {
                notIps.add(ip);
            } else {
                ips.add(ip);
            }
        });
        List<RuntimeInstance> filters = instances.stream().filter(inst ->
                ipMatch(ips, notIps, IPUtils.transferIp(inst.ip))).collect(Collectors.toList());
        return filters;
    }

    /**
     * 传入 runtime instance ip 和 路由规则 right 端定义的ip 进行匹配
     *
     * @param ips    路由规则定义路由到的 ip 列表
     * @param notIps 路由规则定义 非 路由到的 ip 列表
     * @param value  传入的 instance ip
     * @return
     */
    private static boolean ipMatch(Set<ThenIp> ips, Set<ThenIp> notIps, int value) {
        if (!ips.isEmpty() && notIps.isEmpty()) {
            for (ThenIp ip : ips) {
                boolean result = matchIpWithMask(ip.ip, value, ip.mask);
                if (result) {
                    return true;
                }
            }
            return false;
        }

        /**
         * right 同时存在 撇配 ip 和 非 匹配 ip 模式时。
         * 1.先对非匹配ip模式进行 match判断，如果 instances ip 匹配上， 则 返回 false ，因为这里是 非匹配模式
         * 2。 如果非匹配模式里的ip 没有和 instance ip 匹配上，则进入下一步
         * 3,  instance ip 和 正常 匹配模式进行匹配 ，如果 匹配上 返回 true ,如果 都没匹配上，则返回 false
         */
        if (!ips.isEmpty() && !notIps.isEmpty()) {
            for (ThenIp notMatch : notIps) {
                boolean result = matchIpWithMask(notMatch.ip, value, notMatch.mask);
                if (result) {
                    return false;
                }
            }

            for (ThenIp match : ips) {
                boolean matchResult = matchIpWithMask(match.ip, value, match.mask);
                if (matchResult) {
                    return true;
                }
            }
            return false;
        }

        if (!notIps.isEmpty() && ips.isEmpty()) {
            for (ThenIp notMatch : notIps) {
                boolean result = matchIpWithMask(notMatch.ip, value, notMatch.mask);
                if (result) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * match on Matcher.id
     * <p>
     * service -> ctx.serviceName
     *
     * @param ctx
     * @param matcher
     * @skuId -> args.skuId
     * cookie_posid -> cookies.posid
     */
    private static String getValueFromInvocationCtx(InvocationContextImpl ctx, Matcher matcher) {
        // IdToken name
        String id = matcher.getId();
        String ctxValue;
        switch (id) {
            case "service":
                ctxValue = ctx.serviceName();
                break;
            case "method":
                ctxValue = ctx.methodName();
                break;
            case "version":
                ctxValue = ctx.versionName();
                break;
            case "userId":
                ctxValue = ctx.userId().map(userId -> userId.toString()).orElse("");
                break;
            case "calleeIp":
                ctxValue = ctx.calleeIp().map(calleeIp->String.valueOf(calleeIp)).orElse("");
                break;
            default:
                if (id.startsWith(COOKIE_PREFIX)) {
                    String cookie = id.substring(COOKIE_PREFIX.length());
                    InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
                    if (invocationContext != null) {
                        logger.debug("cookies content: {}", invocationContext.cookie(cookie));
                        return invocationContext.cookie(cookie);
                    } else {
                        return null;
                    }
                }
                ctxValue = null;
                break;

        }
        return ctxValue;
    }

    /**
     * 路由规则的值和 ctx值 是否匹配
     *
     * @param pattern
     * @param value
     * @return
     */
    private static boolean matcherPattern(Pattern pattern, String value) {
        if (value == null || value.trim().equals("")) {
            return false;
        }

        if (pattern instanceof StringPattern) {
            String content = ((StringPattern) pattern).content;
            return content.equals(value);
        } else if (pattern instanceof NotPattern) {
            Pattern pattern1 = ((NotPattern) pattern).pattern;
            return !matcherPattern(pattern1, value);
        } else if (pattern instanceof IpPattern) {
            IpPattern ipPattern = ((IpPattern) pattern);
            return matchIpWithMask(ipPattern.ip, Integer.valueOf(value), ipPattern.mask);
        } else if (pattern instanceof RegexPattern) {
            /**
             * 使用缓存好的 pattern 进行 正则 匹配
             */
            java.util.regex.Pattern regex = ((RegexPattern) pattern).pattern;
            return regex.matcher(value).matches();

        } else if (pattern instanceof RangePattern) {
                RangePattern range = ((RangePattern) pattern);
                long from = range.from;
                long to = range.to;

                long valueAsLong = Long.parseLong(value);
                return valueAsLong <= to && valueAsLong >= from;

        } else if (pattern instanceof ModePattern) {
            ModePattern mode = ((ModePattern) pattern);
            try {
                long valueAsLong = Long.valueOf(value);
                long result = valueAsLong % mode.base;
                Optional<Long> from = mode.from;
                long to = mode.to;

                if (from.isPresent()) {
                    return result >= from.get() && result <= to;
                } else {
                    return result == to;
                }
            } catch (NumberFormatException e) {
                logger.error("[ModePattern]::输入参数 value 应为数字类型的id ，but get {}", value);
            } catch (Exception e) {
                logger.error("[ModePattern]::throw exception:" + e.getMessage(), e);
            }
            return false;
        } else if (pattern instanceof NumberPattern) {
            try {
                NumberPattern number = ((NumberPattern) pattern);
                long valueAsLong = Long.parseLong(value);
                long numberLong = number.number;
                return valueAsLong == numberLong;
            } catch (Exception e) {
                logger.error("[NumberPattern]::throw exception:" + e.getMessage(), e);
            }
            return false;
        }

        return false;
    }
}
