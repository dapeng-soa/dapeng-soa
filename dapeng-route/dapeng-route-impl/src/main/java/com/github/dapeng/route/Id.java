package com.github.dapeng.route;

/**
 * Created by tangliu on 2016/6/19.
 */
public class Id {

    public String name;

    public boolean isParameter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public void setParameter(boolean parameter) {
        isParameter = parameter;
    }

    public Id(String name, boolean isParameter) {
        this.name = name;
        this.isParameter = isParameter;
    }
}
