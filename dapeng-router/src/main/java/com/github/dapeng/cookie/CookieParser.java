package com.github.dapeng.cookie;


import com.github.dapeng.router.*;
import com.github.dapeng.router.condition.Condition;
import com.github.dapeng.router.token.*;

import java.util.ArrayList;
import java.util.List;

import static com.github.dapeng.router.RoutesLexer.*;

/**
 * 描述: 语法, 路由规则解析
 * <pre>
 * routes :  (route eol)*
 * route  : left '=>' right
 * left  : 'otherwise' matcher (';' matcher)*
 * matcher : id 'match' patterns
 * patterns: pattern (',' pattern)*
 * pattern : '~' pattern
 * | string
 * | regexpString
 * | rangeString
 * | number
 * | ip
 * | kv
 * | mod
 * right : rightPattern (',' rightPattern)*
 * rightPattern : '~' rightPattern | ip
 * </pre>
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:34
 */
public class CookieParser extends RoutesParser {

    public CookieParser(RoutesLexer lexer) {
        super(lexer);
    }

    /**
     * 第一步： 多行路由规则，根据回车符 ' \n '  进行split  do while 解析
     */
    public List<CookieRoute> cookieRoutes() {
        List<CookieRoute> routes = new ArrayList<>();
        Token token = lexer.peek();
        switch (token.type()) {
            case Token.EOL:
            case Token.OTHERWISE:
            case Token.ID:
                CookieRoute route = cookieRoute();
                if (route != null) {
                    routes.add(route);
                }
                while (lexer.peek() == Token_EOL) {
                    lexer.next(Token.EOL);
                    CookieRoute route1 = cookieRoute();
                    if (route1 != null) {
                        routes.add(route1);
                    }
                }
                break;
            case Token.EOF:
                warn("current service hava no route express config");
                break;
            default:
                error("expect `otherwise` or `id match ...` but got " + token);
        }
        return routes;
    }

    /**
     * 解析一条路由规则，形如:
     * route  : left '=>' right
     * <p>
     * method match s'getFoo'  => ~ip'192.168.3.39'
     */
    public CookieRoute cookieRoute() {
        Token token = lexer.peek();
        switch (token.type()) {
            case Token.OTHERWISE:
            case Token.ID:
                Condition left = left();
                lexer.next(Token.THEN);
                List<CookieRight> right = cookieRiget();
                return new CookieRoute(left, right);
            default:
                warn("expect `otherwise` or `id match ...` but got " + token);
        }
        return null;
    }


    /**
     * right : rightPattern (',' rightPattern)*
     * rightPattern : '~' rightPattern
     * | ip
     */
    protected List<CookieRight> cookieRiget() {
        List<CookieRight> cookieInfoList = new ArrayList<>();

        Token token = lexer.peek();
        switch (token.type()) {
            case Token.COOKIE:
                CookieToken ct = (CookieToken) lexer.next(Token.COOKIE);
                cookieInfoList.add(new CookieRight(ct.getCookieKey(), ct.getCookieValue()));
                // => ip"" ,
                // => 后 只会跟三种  Token_EOF(结束符号)  Token_COMMA(逗号) EOL(换行符)
                validate(lexer.peek(), Token_COMMA, Token_EOF, Token_EOL);
                while (lexer.peek() == Token_COMMA) {
                    lexer.next(Token.COMMA);
                    CookieToken ct2 = (CookieToken) lexer.next(Token.COOKIE);
                    cookieInfoList.add(new CookieRight(ct2.getCookieKey(), ct2.getCookieValue()));
                }
                return cookieInfoList;
            default:
                error("expect 'c' cookie token , but got:" + token);
                return null;
        }
    }
}
