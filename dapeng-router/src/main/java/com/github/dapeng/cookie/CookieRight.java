package com.github.dapeng.cookie;

/**
 * @author <a href=mailto:leihuazhe@gmail.com>maple</a>
 * @since 2018-10-24 5:07 PM
 */
public class CookieRight {

    private final String cookieKey;

    private final String cookieValue;

    public CookieRight(String cookieKey, String cookieValue) {
        this.cookieKey = cookieKey;
        this.cookieValue = cookieValue;
    }

    public String cookieKey() {
        return cookieKey;
    }

    public String cookieValue() {
        return cookieValue;
    }

    @Override
    public String toString() {
        return "CookieToken{ cookieKey = '" + cookieKey + '\'' + ", cookieValue='" + cookieValue + '\'' + '}';
    }
}
