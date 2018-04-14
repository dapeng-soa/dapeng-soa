package com.github.dapeng.router;

import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
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
        RoutesParser parser = new RoutesParser(new RoutesLexer(content));
        List<Route> routes = parser.routes();
        return routes;
    }

    public static Set<InetAddress> execute(InvocationContextImpl ctx, List<Route> routes, List<String> servers) {
        Set<InetAddress> result = new HashSet<>();
        for (Route route : routes) {

            boolean isMatched = matchCondition(ctx, route.getLeft());
            // 匹配成功，执行右边逻辑
            if (isMatched) {
                List<InetAddress> addresses = matchActions(servers, route);
                result.addAll(addresses);
            } else {
                // otherwise ?
            }
        }
        return result;
    }


    /**
     * product
     */
    public static List<RuntimeInstance> executeRoutes(InvocationContextImpl ctx, List<Route> routes, List<RuntimeInstance> instances) {
        logger.info("开始过滤： " + instances.size());
        for (Route route : routes) {
            boolean isMatched = matchCondition(ctx, route.getLeft());
            // 匹配成功，执行右边逻辑

            if (isMatched) {
                instances = matchActionsProducet(instances, route);
                logger.info("过滤结果：" + instances.size());
            } else {
                logger.info("路由没有过滤" + instances.size());
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
            String value = matchMatcher(ctx, matcher);
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
     * 匹配 右边
     *
     * @param servers
     * @param route
     */
    private static List<InetAddress> matchActions(List<String> servers, Route route) {
        List<ThenIp> actions = route.getActions();
        MatchPair pair = new MatchPair();
        List<InetAddress> addresses = new ArrayList<>();
        actions.forEach(ip -> {
            if (ip.not) {
                pair.notMatches.add(ip);
            } else {
                pair.matches.add(ip);
            }
        });

        for (String server : servers) {
            boolean isMatch = pair.isMatch(server);
            if (isMatch) {
                try {
                    addresses.add(InetAddress.getByName(server));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
        return addresses;
    }

    /**
     * 匹配右边重载
     *
     * @param instances
     * @param route
     * @return
     */
    private static List<RuntimeInstance> matchActionsProducet(List<RuntimeInstance> instances, Route route) {
        List<ThenIp> actions = route.getActions();
        MatchPair pair = new MatchPair();
        actions.forEach(ip -> {
            if (ip.not) {
                pair.notMatches.add(ip);
            } else {
                pair.matches.add(ip);
            }
        });
        List<RuntimeInstance> filters = instances.stream().filter(inst -> pair.isMatch(inst.ip)).collect(Collectors.toList());
        return filters;
    }


    private static final class MatchPair {
        final Set<ThenIp> matches = new HashSet<>();
        final Set<ThenIp> notMatches = new HashSet<>();

        private boolean isMatch(String value) {
            if (!matches.isEmpty() && notMatches.isEmpty()) {
                for (ThenIp match : matches) {
                    if (match.ip.equals(value)) {
                        return true;
                    }
                }
                return false;
            }

            if (!matches.isEmpty() && !notMatches.isEmpty()) {
                //when both notmatches and matches contain the same value, then using notmatches first
                for (ThenIp notMatch : notMatches) {
                    if (notMatch.ip.equals(value)) {
                        return false;
                    }
                }
                for (ThenIp match : matches) {
                    if (match.ip.equals(value)) {
                        return true;
                    }
                }
                return false;
            }

            if (!notMatches.isEmpty() && matches.isEmpty()) {
                for (ThenIp notMatch : notMatches) {
                    if (notMatch.ip.equals(value)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * match on Matcher.id
     *
     * @param ctx
     * @param matcher
     */
    private static String matchMatcher(InvocationContextImpl ctx, Matcher matcher) {
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
        }
        return false;
    }
}
