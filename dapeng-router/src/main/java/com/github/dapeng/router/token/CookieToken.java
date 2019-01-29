package com.github.dapeng.router.token;


/**
 * @author <a href=mailto:leihuazhe@gmail.com>maple</a>
 * @since 2018-10-24 4:06 PM
 */
public class CookieToken extends SimpleToken {


    private final String cookieKey;

    private final String cookieValue;

    public CookieToken(String cookieKey, String cookieValue) {
        super(COOKIE);
        this.cookieKey = cookieKey;
        this.cookieValue = cookieValue;
    }

    public String getCookieKey() {
        return cookieKey;
    }

    public String getCookieValue() {
        return cookieValue;
    }

    @Override
    public String toString() {
        return "CookieToken{ type=" + type + "cookieKey='" + cookieKey + '\'' + ", cookieValue='" + cookieValue + '\'' + '}';
    }
}