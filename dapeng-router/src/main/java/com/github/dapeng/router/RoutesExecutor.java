package com.github.dapeng.router;

import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.router.condition.Condition;
import com.github.dapeng.router.condition.Matcher;
import com.github.dapeng.router.condition.Matchers;
import com.github.dapeng.router.condition.Otherwise;
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

    /**
     * @param content
     * @return
     */
    public static List<Route> parseAll(String content) {
        RoutesParser parser = new RoutesParser(new RoutesLexer(content));
        List<Route> routes = parser.routes();
        return routes;
    }

    /**
     * product
     */
    public static List<RuntimeInstance> executeRoutes(InvocationContextImpl ctx, List<Route> routes, List<RuntimeInstance> instances) {
        logger.debug(RoutesExecutor.class.getSimpleName() + "::executeRoutes$开始过滤：过滤前 size  {}", instances.size());
        for (Route route : routes) {
            boolean isMatched = matchCondition(ctx, route.getLeft());
            // 匹配成功，执行右边逻辑
            if (isMatched) {
                instances = matchActionThenIp(instances, route);
                logger.debug(RoutesExecutor.class.getSimpleName() + "::executeRoutes过滤结果 size: {}", instances.size());
                break;
            } else {
                logger.debug(RoutesExecutor.class.getSimpleName() + "::executeRoutes路由没有过滤, size {}", instances.size());
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
     * matchActionThenIp  传入 RuntimeInstance 进行match
     *
     * @param instances
     * @param route
     * @return
     */
    private static List<RuntimeInstance> matchActionThenIp(List<RuntimeInstance> instances, Route route) {
        List<ThenIp> actions = route.getActions();
        Set<ThenIp> ips = new HashSet<>(16);
        Set<ThenIp> notIps = new HashSet<>(16);

        actions.forEach(ip -> {
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

        // TODO 逻辑??
        if (!ips.isEmpty() && !notIps.isEmpty()) {
            //when both not matches and matches contain the same value, then using notmatches first
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
     *
     * @param ctx
     * @param matcher
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
                ctxValue = ctx.calleeIp().orElse("");
                break;
            default:
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
        if (pattern instanceof StringPattern) {
            String content = ((StringPattern) pattern).content;
            return content.equals(value);
        } else if (pattern instanceof NotPattern) {
            Pattern pattern1 = ((NotPattern) pattern).pattern;
            return !matcherPattern(pattern1, value);
        } else if (pattern instanceof IpPattern) {
            IpPattern ipPattern = ((IpPattern) pattern);
            return matchIpWithMask(ipPattern.ip, IPUtils.transferIp(value), ipPattern.mask);
        } else if (pattern instanceof RegexPattern) {
            String regex = ((RegexPattern) pattern).regex;
            return value.matches(regex);
        } else if (pattern instanceof RangePattern) {
            RangePattern range = ((RangePattern) pattern);
            long valueAsLong = Long.parseLong(value);
            long from = range.from;
            long to = range.to;
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
                logger.error("输入参数 value 应为数字类型的id ，but get {}", value);
            }
            return false;
        } else if (pattern instanceof NumberPattern) {
            NumberPattern number = ((NumberPattern) pattern);
            long valueAsLong = Long.parseLong(value);
            long numberLong = number.number;
            return valueAsLong == numberLong;
        }

        return false;
    }
}
