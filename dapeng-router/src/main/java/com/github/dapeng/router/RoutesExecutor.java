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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 描述:
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
        RoutesParser parser = new RoutesParser(new RoutesLexer1(content));
        List<Route> routes = parser.routes();
        return routes;
    }

    /**
     * product
     */
    public static List<RuntimeInstance> executeRoutes(InvocationContextImpl ctx, List<Route> routes, List<RuntimeInstance> instances) {
        logger.debug("开始过滤：过滤前 size  {}", instances.size());
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
            String value = matchContextValue(ctx, matcher);
            List<Pattern> patterns = matcher.getPatterns();

            boolean isMatch = false;
            for (Pattern pattern : patterns) {
                boolean result = matcherPattern(pattern, value);
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
                return pair.isMatch(inst.ip);
            } catch (UnknownHostException e) {
                logger.error("ip host is unKnown");
            }
            return false;
        }).collect(Collectors.toList());
        return filters;
    }


    private static final class MatchPair {
        final Set<ThenIp> matches = new HashSet<>();
        final Set<ThenIp> notMatches = new HashSet<>();

        private boolean isMatch(String value) throws UnknownHostException {
            if (!matches.isEmpty() && notMatches.isEmpty()) {
                for (ThenIp match : matches) {
                    //掩码支持
                    if (match.mask != 0) {
                        return matchMask(match.ip, value, match.mask);
                    }

                    if (match.ip.equals(value)) {
                        return true;
                    }
                }
                return false;
            }

            if (!matches.isEmpty() && !notMatches.isEmpty()) {
                //when both notmatches and matches contain the same value, then using notmatches first
                for (ThenIp notMatch : notMatches) {

                    //掩码支持
                    if (notMatch.mask != 0) {
                        return !matchMask(notMatch.ip, value, notMatch.mask);
                    }

                    if (notMatch.ip.equals(value)) {
                        return false;
                    }
                }
                for (ThenIp match : matches) {
                    //掩码支持
                    if (match.mask != 0) {
                        return matchMask(match.ip, value, match.mask);
                    }

                    if (match.ip.equals(value)) {
                        return true;
                    }
                }
                return false;
            }

            if (!notMatches.isEmpty() && matches.isEmpty()) {
                for (ThenIp notMatch : notMatches) {
                    //掩码支持
                    if (notMatch.mask != 0) {
                        return !matchMask(notMatch.ip, value, notMatch.mask);
                    }

                    if (notMatch.ip.equals(value)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        /**
         * 子网掩码支持
         */
        private boolean matchMask(String targetIpSeq, String serverIpSeq, int mask) throws UnknownHostException {
            int serverIp = IPUtils.transferIp(serverIpSeq);
            int targetIp = IPUtils.transferIp(targetIpSeq);
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
                try {
                    context = ctx.userId().get().toString();
                } catch (NoSuchElementException e) {
                    context = "";
                }
                break;

            case "calleeIp":
                try {
                    context = ctx.calleeIp().get();
                } catch (NoSuchElementException e) {
                    context = "";
                }
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
            if (content.equals(value)) {
                return true;
            }
        } else if (pattern instanceof NotPattern) {
            Pattern pattern1 = ((NotPattern) pattern).pattern;
            boolean result = matcherPattern(pattern1, value);
            return !result;
        } else if (pattern instanceof IpPattern) {
            //todo
            return false;
        } else if (pattern instanceof RegexpPattern) {
            String regex = ((RegexpPattern) pattern).regex;
            return value.matches(regex);
        } else if (pattern instanceof RangePattern) {
            RangePattern range = ((RangePattern) pattern);
            long userId = Long.parseLong(value);
            long from = range.from;
            long to = range.to;
            if (userId <= to && userId >= from) {
                return true;
            }
            return false;
        } else if (pattern instanceof ModePattern) {
            ModePattern mode = ((ModePattern) pattern);
            try {
                long userId = Long.valueOf(value);
                long result = userId % mode.base;
                Optional<Long> from = mode.from;
                long to = mode.to;

                if (from.isPresent()) {
                    if (result >= from.get() && result <= to) {
                        return true;
                    }
                } else {
                    if (result == to) {
                        return true;
                    }
                }
                return false;

            } catch (NumberFormatException e) {
                logger.error("输入参数 value 应为数字类型的id ，but get {}", value);
            }
            return false;

        } else if (pattern instanceof NumberPattern) {
            NumberPattern number = ((NumberPattern) pattern);
            long userId = Long.parseLong(value);
            long from = number.number;
            if (userId == from) {
                return true;
            }
            return false;
        }

        return false;
    }


}
