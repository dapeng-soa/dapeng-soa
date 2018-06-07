package com.github.dapeng.router.condition;

import com.github.dapeng.router.pattern.Pattern;

import java.util.List;

/**
 * 描述: 路由表达式的每一个匹配规则，即为一个 Matcher
 * etc. method match 'setFoo' ; version match '1.0.0' =>
 * <p>
 * 这里就会有两个matcher
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:39
 */
public class Matcher {

    private String id;
    private List<Pattern> patterns;

    public Matcher(String id, List<Pattern> patterns) {
        this.id = id;
        this.patterns = patterns;
    }

    public String getId() {
        return id;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    @Override
    public String toString() {
        return "Matcher{" +
                "id='" + id + '\'' +
                ", patterns=" + patterns +
                '}';
    }
}
