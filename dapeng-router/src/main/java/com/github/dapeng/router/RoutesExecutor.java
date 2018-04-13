package com.github.dapeng.router;

import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.router.condition.Condition;
import com.github.dapeng.router.condition.Matcher;
import com.github.dapeng.router.condition.Matchers;
import com.github.dapeng.router.condition.Otherwise;
import com.github.dapeng.router.pattern.IpPattern;
import com.github.dapeng.router.pattern.NotPattern;
import com.github.dapeng.router.pattern.Pattern;
import com.github.dapeng.router.pattern.StringPattern;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午10:02
 */
public class RoutesExecutor {

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
            Object value = matchMatcher(ctx, matcher);
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
     * 过滤服务器
     *
     * @param thenIp
     * @param servers
     */
    private static void filterServer(List<ThenIp> thenIp, List<String> servers) {
        List<InetAddress> inetAddresses = new ArrayList<>();
        for (String server : servers) {
            try {
                InetAddress inetAddress = InetAddress.getByName(server);
                if (matched(inetAddress, thenIp, true)) {
                    inetAddresses.add(inetAddress);
                }
            } catch (Exception e) {
                e.printStackTrace();
//                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public static boolean matched(InetAddress address, List<ThenIp> thenIp, boolean inOrNot) {
        for (ThenIp ip : thenIp) {
            if (matched(address, ip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断ip地址是否符合规则
     *
     * @param address
     * @param ipPattern
     * @return
     */
    private static boolean matched(InetAddress address, ThenIp ipPattern) {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(ipPattern.ip);
        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
        byte[] bytes = ip.getAddress();
        int ipInt = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | ((bytes[3] & 0xFF));
        int mask2 = 32 - ipPattern.mask;  // 8
        int mask2Flag = (1 << mask2) - 1;
        int mask1Flag = -1 & (~mask2Flag);

        byte[] addressBytes = address.getAddress();
        int addressInt = ((addressBytes[0] & 0xFF) << 24) | ((addressBytes[1] & 0xFF) << 16) | ((addressBytes[2] & 0xFF) << 8) | ((addressBytes[3] & 0xFF));

        return (addressInt & mask1Flag) == (ipInt & mask1Flag);

    }


    /**
     * match on Matcher.id
     *
     * @param ctx
     * @param matcher
     */
    private static Object matchMatcher(InvocationContextImpl ctx, Matcher matcher) {
        // IdToken name
        String id = matcher.getId();
        String context;
        switch (id) {
            case "method":
                context = ctx.serviceName();
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
    private static boolean matcherPattern(Pattern pattern, Object value) {
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
        }
        return false;
    }
}
