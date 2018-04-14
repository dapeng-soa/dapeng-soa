package com.github.dapeng.router.token;

/**
 * 描述: 词法解析 最小单元 token
 *
 * @author hz.lei
 * @date 2018年04月13日 下午8:51
 */
public interface Token {
    /**
     * 回车换行符
     */
    int EOL = 1;

    /**
     * =>
     */
    int THEN = 2;

    /**
     * otherwise
     */
    int OTHERWISE = 3;

    /**
     * match 关键字
     */
    int MATCH = 4;

    /**
     * not in    ~
     */
    int NOT = 5;

    /**
     * string类型: eg. A B C D
     */
    int STRING = 6;

    /**
     * regex 正则
     */
    int REGEXP = 7;

    /**
     * range 范围
     */
    int RANGE = 8;

    /**
     * 数字类型
     */
    int NUMBER = 9;

    /**
     * ip "192.168.1.1"
     */
    int IP = 10;

    /**
     * k v 存储
     */
    int KV = 11;

    /**
     * mode  %
     */
    int MODE = 12;
    /**
     * 匹配类型id  like  method ，version ，service
     */
    int ID = 13;

    /**
     * 文件结束符
     */
    int EOF = -1;

    /**
     * 分号 < ；>  区分多个 Matcher
     */
    int SEMI_COLON = 14;

    /**
     * 逗号: <,>  区分多个 pattern
     */
    int COMMA = 15;

    /**
     * token id
     */
    int id();

}
