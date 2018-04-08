package com.github.dapeng.route;


import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.route.pattern.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eric
 * @date
 */
public class RouteExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouteExecutor.class);


    /**
     * 通过请求上下文，规则列表，当前服务ip，判断该请求是否可以访问此ip
     * <p>
     * 对于多个规则可能冲突的情况，目前的处理是，匹配到第一个规则，则跳出匹配
     *
     * @param ctx    上下文
     * @param routes 规则列表
     * @param server 服务器ip地址
     * @return
     */
    public static boolean isServerMatched(InvocationContext ctx, List<Route> routes, InetAddress server) {

        boolean matchOne = false;
        boolean result = false;
        for (Route route : routes) {
            boolean isMatched = checkRouteCondition(ctx, route.getLeft());
            if (isMatched) {
                matchOne = true;
                if (route.getRight() instanceof IpPattern) {
                    result = matched(server, (IpPattern) route.getRight());
                } else if (route.getRight() instanceof NotPattern) {
                    result = !matched(server, (IpPattern) ((NotPattern) route.getRight()).getPattern());
                } else {
                    throw new AssertionError("route right must be IpPattern or ~IpPattern");
                }
                break;
            }
        }
        if (matchOne)
            return result;
        return true;
    }


    /**
     * 通过请求上下文，规则，以及备选的服务器ip列表，获取该请求可以访问的ip列表
     *
     * @param ctx     上下文
     * @param routes  规则列表
     * @param servers 备选的服务器ip列表
     * @return
     */
    public static Set<InetAddress> execute(InvocationContext ctx, List<Route> routes, List<String> servers) {
        Set added = new HashSet<InetAddress>();// 匹配的服务器
        Set removed = new HashSet<InetAddress>();// 拒绝的服务器，对应于 ~ip"" 模式

        for (Route route : routes) {
            boolean isMatched = checkRouteCondition(ctx, route.getLeft());
            if (isMatched) {
                Pattern right = route.getRight();
                if (right instanceof IpPattern) {
                    added.addAll(filterServer(servers, right));
                } else if (right instanceof NotPattern) {
                    removed.addAll(filterServer(servers, right));
                } else {
                    throw new AssertionError("route right must be IpPattern or ~IpPattern");
                }
            }
        }
        //如果没有可匹配的服务，则匹配规则失效， 将服务列表全部作为信任服务
        if (added.isEmpty()) {
            for (String server : servers) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(server);
                    added.add(inetAddress);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        added.removeAll(removed);
        return added;
    }


    /**
     * 返回满足Pattern条件的服务ip列表
     *
     * @param servers
     * @param right
     * @return
     */
    public static List<InetAddress> filterServer(List<String> servers, Pattern right) {
        List<InetAddress> inetAddresses = new ArrayList<>();
        for (String server : servers) {
            try {
                InetAddress inetAddress = InetAddress.getByName(server);
                if (matched(inetAddress, (IpPattern) (right instanceof NotPattern ? ((NotPattern) right).getPattern() : right))) {
                    inetAddresses.add(inetAddress);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return inetAddresses;
    }

    /**
     * 判断上下文是否满足Matchers条件,Matchers由一系列Matcher组成:
     * <p>
     * 如果是“and（与）”关系，则全部满足才返回true，否则返回false;
     * 如果是“or(或)”关系，则有一个满足则返回true,都不满足返回false
     *
     * @param ctx
     * @param left
     * @return
     */
    public static boolean checkRouteCondition(InvocationContext ctx, MatchLeftSide left) {

        if (left instanceof OtherWise) {
            return true;
        } else {
            List<Matcher> matchers = ((Matchers) left).getMatchers();

            if (((Matchers) left).isAndOrOr()) {
                for (Matcher matcher : matchers) {
                    Object value = checkFieldMatcher(ctx, matcher);
                    if (value != null) {
                        List<Pattern> patterns = matcher.getPatterns();
                        for (Pattern pattern : patterns) {
                            if (!matched(pattern, value))
                                return false;
                        }
                    } else {
                        return false;   //如果没有找到值，那么认为肯定不满足条件
                    }
                }
                return true;
            } else {
                for (Matcher matcher : matchers) {
                    Object value = checkFieldMatcher(ctx, matcher);
                    if (value != null) {
                        List<Pattern> patterns = matcher.getPatterns();
                        for (Pattern pattern : patterns) {
                            if (matched(pattern, value))
                                return true;
                        }
                    }
                }
                return false;
            }
        }
    }

    /**
     * 根据matcher.Id，返回上下文中对应的值
     *
     * @param ctx
     * @param matcher
     * @return
     */
    public static Object checkFieldMatcher(InvocationContext ctx, Matcher matcher) {
        Id id = matcher.getId();
        if ("operatorId".equals(id.getName())) {
            return ctx.operatorId().orElse(null);
        } else if ("callerMid".equals(id.getName())) {
            return ctx.callerMid().orElse(null);
        } else if ("ip".equals(id.getName())) {
            return ctx.userIp();
        } else if ("userId".equals(id.getName())) {
            return ctx.userId().orElse(null);
        } else if ("service".equals(id.getName())) {
            return ctx.serviceName();
        } else if ("method".equals(id.getName())) {
            return ctx.methodName();
        } else if ("version".equals(id.getName())) {
            return ctx.versionName();
        } else {
            throw new AssertionError("not support Field: " + id.getName());
        }
    }


    public static boolean matched(Pattern pattern, Object value) {
        if (pattern instanceof NotPattern) {
            return !(matched(((NotPattern) pattern).getPattern(), value));
        } else if (pattern instanceof ModPattern) {
            return matched(Long.valueOf(value.toString()), (ModPattern) pattern);
        } else if (pattern instanceof StringPattern) {
            return matched((String) value, (StringPattern) pattern);
        } else if (pattern instanceof RegexpPattern) {
            return ((String) value).matches(((RegexpPattern) pattern).getValue());
        } else if (pattern instanceof NumberPattern) {
            return matched(Long.valueOf(value.toString()), (NumberPattern) pattern);
        } else if (pattern instanceof RangePattern) {
            return Long.valueOf(value.toString()) > ((RangePattern) pattern).low && Long.valueOf(value.toString()) <= ((RangePattern) pattern).high;
        } else if (pattern instanceof IpPattern) {
            try {
                return matched(InetAddress.getByName((String) value), (IpPattern) pattern);
            } catch (Exception e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
        return false;
    }

    public static boolean matched(String str, StringPattern stringPattern) {
        boolean isMatch = false;
        for (String temp : stringPattern.getValue()) {
            if (str.equals(temp)) {
                isMatch = true;
                break;
            }
        }
        return isMatch;
    }

    private static boolean matched(long num, ModPattern modPattern) {
        long remain = num % modPattern.base;
        return remain >= modPattern.remain.low && remain <= modPattern.remain.high;
    }

    private static boolean matched(long num, NumberPattern numberPattern) {
        boolean isMatch = false;
        for (long temp : numberPattern.value) {
            if (temp == num) {
                isMatch = true;
                break;
            }
        }
        return isMatch;
    }

    /**
     * 判断ip是否符合IpPattern
     *
     * @param address
     * @param ipPattern
     * @return
     */
    private static boolean matched(InetAddress address, IpPattern ipPattern) {

        List<IpNode> ips = ipPattern.ips;

        for (IpNode node : ips) {
            if (matched(address, node))
                return true;
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
    private static boolean matched(InetAddress address, IpNode ipPattern) {

        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(ipPattern.ip);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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
}
