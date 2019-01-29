package com.github.dapeng.router.condition;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述: matchers list
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:38
 */
public class Matchers implements Condition {
    public List<Matcher> matchers = new ArrayList<>();

    @Override
    public String toString() {
        return "Matchers{" + "matchers=" + matchers + '}';
    }
}
