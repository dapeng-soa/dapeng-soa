package com.github.dapeng.router;

import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.router.condition.Condition;
import com.github.dapeng.router.condition.Matcher;
import com.github.dapeng.router.condition.Matchers;
import com.github.dapeng.router.condition.Otherwise;
import com.github.dapeng.router.exception.ParsingException;
import com.github.dapeng.router.pattern.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
                logger.debug("过滤结果 size: {}", instances.size());
                break;
            } else {
                logger.debug("路由没有过滤, size {}", instances.size());
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
        for (Matcher matcher : matchers) {
            String actuallyConditionValue = matchContextValue(ctx, matcher);
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
        MatchPair pair = new MatchPair();
        actions.forEach(ip -> {
            if (ip.not) {
                pair.notMatches.add(ip);
            } else {
                pair.matches.add(ip);
            }
        });
        List<RuntimeInstance> filters = instances.stream().filter(inst -> {
            try {
                return pair.isMatch(IPUtils.transferIp(inst.ip));

                //todo
            } catch (Exception e) {
                logger.error("ip host is unKnown");
            }
            return false;
        }).collect(Collectors.toList());
        return filters;
    }


    private static final class MatchPair {
        final Set<ThenIp> matches = new HashSet<>();
        final Set<ThenIp> notMatches = new HashSet<>();

        private boolean isMatch(int value) {
            if (!matches.isEmpty() && notMatches.isEmpty()) {
                for (ThenIp match : matches) {
                    boolean result = matchMask(match.ip, value, match.mask);
                    if (result) {
                        return true;
                    }
                }
                return false;
            }

            if (!matches.isEmpty() && !notMatches.isEmpty()) {
                //when both not matches and matches contain the same value, then using notmatches first
                for (ThenIp notMatch : notMatches) {

                    boolean result = matchMask(notMatch.ip, value, notMatch.mask);
                    if (result) {
                        return false;
                    }
                }
                return true;

                /*for (ThenIp match : matches) {
                    boolean matchResult = matchMask(match.ip, value, match.mask);
                    if (matchResult) {
                        return true;
                    }
                }
                return false;*/
            }

            if (!notMatches.isEmpty() && matches.isEmpty()) {
                for (ThenIp notMatch : notMatches) {
                    //掩码支持
                    boolean result = matchMask(notMatch.ip, value, notMatch.mask);
                    if (result) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }


        /**
         * 子网掩码支持
         *
         * @param targetIp 输入ip 去匹配的 ip表达式
         * @param serverIp 输入ip ，即当前服务节点 ip
         * @param mask     子网掩码
         * @return
         */
        private static boolean matchMask(int targetIp, int serverIp, int mask) {
            int maskIp = (0xFFFFFFFF << (32 - mask));
            return (serverIp & maskIp) == (targetIp & maskIp);
        }
    }

    /**
     * match on Matcher.id
     *
     * @param ctx
     * @param matcher
     */
    private static String matchContextValue(InvocationContextImpl ctx, Matcher matcher) {
        // IdToken name
        String id = matcher.getId();
        String context;
        switch (id) {
            case "service":
                context = ctx.serviceName();
                break;
            case "method":
                context = ctx.methodName();
                break;
            case "version":
                context = ctx.versionName();
                break;
            case "userId":
                context = ctx.userId().map(userId -> userId.toString()).orElse("");
                break;
            case "calleeIp":
                context = ctx.calleeIp().orElse("");
                break;
            default:
                context = null;
                break;

        }
        return context;
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
            return MatchPair.matchMask(ipPattern.ip, IPUtils.transferIp(value), ipPattern.mask);
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
                long userId = Long.valueOf(value);
                long result = userId % mode.base;
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
            long userId = Long.parseLong(value);
            long from = number.number;
            return userId == from;
        }

        return false;
    }
}
