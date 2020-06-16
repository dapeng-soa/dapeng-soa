/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.router;


import com.github.dapeng.router.condition.Condition;
import com.github.dapeng.router.condition.*;
import com.github.dapeng.router.exception.ParsingException;
import com.github.dapeng.router.pattern.*;
import com.github.dapeng.router.token.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.dapeng.router.RoutesLexer.*;
import static com.github.dapeng.router.token.Token.STRING;

/**
 * 描述: 语法, 路由规则解析
 * <pre>
 * routes :  (route eol)*
 * route  : left '=>' right
 * left  : 'otherwise' | matcher (';' matcher)*
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
public class RoutesParser {

    private static Logger logger = LoggerFactory.getLogger(RoutesParser.class);

    protected RoutesLexer lexer;

    public RoutesParser(RoutesLexer lexer) {
        this.lexer = lexer;
    }

    /**
     * 第一步： 多行路由规则，根据回车符 ' \n '  进行split  do while 解析
     */
    public List<Route> routes() {
        List<Route> routes = new ArrayList<>();
        Token token = lexer.peek();
        switch (token.type()) {
            case Token.EOL:
            case Token.OTHERWISE:
            case Token.ID:
                Route route = route();
                if (route != null) {
                    routes.add(route);
                }
                while (lexer.peek() == Token_EOL) {
                    lexer.next(Token.EOL);
                    Route route1 = route();
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
    public Route route() {
        Token token = lexer.peek();
        switch (token.type()) {
            case Token.OTHERWISE:
            case Token.ID:
                Condition left = left();
                lexer.next(Token.THEN);
                List<ThenIp> right = right();
                return new Route(left, right);
            default:
                warn("expect `otherwise` or `id match ...` but got " + token);
        }
        return null;
    }

    /**
     * left  : 'otherwise' matcher (';' matcher)*
     * <p>
     * method match pattern1,pattern2
     * <p>
     * method match s'getFoo',s'setFoo' ; version match s'1.0.0',s'1.0.1' => right
     * 分号 分隔 之间 是一个 Matcher
     * <p>
     * 一个 Matcher 有多个 pattern
     */
    public Condition left() {
        Matchers matchers = new Matchers();
        Token token = lexer.peek();
        switch (token.type()) {
            case Token.OTHERWISE:
                lexer.next();
                return new Otherwise();
            case Token.ID:
                Matcher matcher = matcher();
                matchers.matchers.add(matcher);
                while (lexer.peek() == Token_SEMI_COLON) {
                    lexer.next(Token.SEMI_COLON);
                    matchers.matchers.add(matcher());
                }
                return matchers;
            default:
                error("expect `otherwise` or `id match ...` but got " + token);
                return null;
        }
    }

    /**
     * matcher : id 'match' patterns
     * <p>
     * method match "getFoo","setFoo"
     */
    public Matcher matcher() {

        // method
        IdToken id = (IdToken) lexer.next();
        // match
        lexer.next(Token.MATCH);
        List<Pattern> patterns = patterns();

        return new Matcher(id.name, patterns);
    }

    /**
     * patterns: pattern (',' pattern)*
     * <p>
     * pattern : '~' pattern
     * | string
     * | regexpString
     * | rangeString
     * | number
     * | ip
     * | kv
     * | mod
     */

    /**
     * method match s'getFoo',s'setFoo';version match s'1.0.0',s'1.0.1' => right    (1)
     * <p>
     * method match s'getFoo',s'setFoo' => right                (2)
     */
    public List<Pattern> patterns() {
        List<Pattern> patterns = new ArrayList<>();

        Pattern p = pattern();
        patterns.add(p);
        while (lexer.peek() == Token_COMMA) {
            lexer.next(Token.COMMA);
            patterns.add(pattern());
        }


        return patterns;
    }

    /**
     * s'getFoo'
     * <p>
     * s'setFoo'
     * <p>
     * s'getFoo*'
     */
    public Pattern pattern() {
        // s'getFoo'
        Token token = lexer.peek();
        switch (token.type()) {
            case Token.NOT:
                lexer.next(Token.NOT);
                Pattern it = pattern();
                return new NotPattern(it);
            case STRING:
                // getFoo
                StringToken st = (StringToken) lexer.next(Token.STRING);
                return new StringPattern(st.content);
            case Token.REGEXP:
                // get.*
                RegexToken regex = (RegexToken) lexer.next(Token.REGEXP);
                return new RegexPattern(regex.pattern);
            case Token.RANGE:
                // getFoo
                RangeToken rt = (RangeToken) lexer.next(Token.RANGE);
                return new RangePattern(rt.from, rt.to);
            case Token.NUMBER:
                NumberToken nt = (NumberToken) lexer.next(Token.NUMBER);
                return new NumberPattern(nt.number);
            case Token.IP:
                IpToken ipToken = (IpToken) lexer.next(Token.IP);
                return new IpPattern(ipToken.ip, ipToken.mask);
            case Token.KV:
                //todo not implemented
                throw new ParsingException("[KV]", "KV pattern not implemented yet");
            case Token.MODE:
                ModeToken modeToken = (ModeToken) lexer.next(Token.MODE);
                return new ModePattern(modeToken.base, modeToken.from, modeToken.to);
            default:
                throw new ParsingException("[UNKNOWN TOKEN]", "UnKnown token:" + token);
        }
    }

    /**
     * right : rightPattern (',' rightPattern)*
     * rightPattern : '~' rightPattern
     * | ip
     */
    public List<ThenIp> right() {
        List<ThenIp> thenIps = new ArrayList<>();

        Token token = lexer.peek();
        switch (token.type()) {
            case Token.NOT:
            case Token.IP:
                ThenIp it = rightPattern();
                thenIps.add(it);
                // => ip"" ,
                // => 后 只会跟三种  Token_EOF(结束符号)  Token_COMMA(逗号) EOL(换行符)
                validate(lexer.peek(), Token_COMMA, Token_EOF, Token_EOL);
                while (lexer.peek() == Token_COMMA) {
                    lexer.next(Token.COMMA);
                    ThenIp it2 = rightPattern();
                    thenIps.add(it2);
                }
                return thenIps;
            default:
                error("expect '~ip' or 'ip' but got:" + token);
                return null;
        }
    }

    /**
     * ？
     */
    public ThenIp rightPattern() {
        Token token = lexer.peek();
        switch (token.type()) {
            case Token.NOT: {
                lexer.next(Token.NOT);
                ThenIp it = rightPattern();
                return new ThenIp(!it.not, it.ip, it.port, it.mask);
            }
            case Token.IP: {
                IpToken ip = (IpToken) lexer.next(Token.IP);
                return new ThenIp(false, ip.ip, ip.port, ip.mask);
            }
            default:
                error("expect '~ip' or 'ip' but got:" + token);
                return null;
        }
    }


    protected void error(String errorInfo) {
        logger.error(errorInfo);
    }

    protected void warn(String errorInfo) {
        logger.warn(errorInfo);
    }

    protected void validate(Token target, Token... expects) {
        boolean flag = false;
        for (Token expect : expects) {
            if (target == expect) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            throw new ParsingException("[Validate Token Error]",
                    "target token: " + convert(target) + " is not in expects token: " + convert(expects));
        }
    }

    private List<String> convert(Token... tokens) {
        return Arrays.stream(tokens).map(token -> TokenEnum.findById(token.type()).toString()).collect(Collectors.toList());
    }

}
