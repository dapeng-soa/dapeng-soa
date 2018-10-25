package com.github.dapeng.cookie;

import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.router.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href=mailto:leihuazhe@gmail.com>maple</a>
 * @since 2018-10-24 5:58 PM
 */
public class CookieExecutor extends RoutesExecutor {
    private static Logger logger = LoggerFactory.getLogger(CookieExecutor.class);

    /**
     * 解析 Cookie routes 路由规则
     *
     * @param content 设置在 zk cookies 节点下的路由内容信息
     * @return
     */
    public static List<CookieRoute> parseCookieRoutes(String content) {
        CookieParser parser = new CookieParser(new RoutesLexer(content));
        return parser.cookieRoutes();
    }

    /**
     * 执行 路由规则 匹配， 返回 经过路由后的 实例列表
     */
    public static void cookieRoutes(InvocationContextImpl ctx, List<CookieRoute> routes) {
        boolean isMatched;
        for (CookieRoute route : routes) {
            try {
                isMatched = matchCondition(ctx, route.getLeft());
                // 匹配成功，执行右边逻辑
                if (isMatched) {
                    List<CookieRight> cookieRights = route.getCookieRightList();

                    cookieRights.forEach(right -> {
                        ctx.setCookie(right.cookieKey(), right.cookieValue());
                    });

                    if (logger.isDebugEnabled()) {
                        logger.debug(CookieExecutor.class.getSimpleName() + "::route left " + route.getLeft().toString() +
                                        "::cookies  size: {}, content: {}",
                                cookieRights.size(), cookieRights.toString());
                    }
                    break;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(CookieExecutor.class.getSimpleName() + "::route left " + route.getLeft().toString() + ":: cookies no route");
                    }
                }
            } catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }
}
