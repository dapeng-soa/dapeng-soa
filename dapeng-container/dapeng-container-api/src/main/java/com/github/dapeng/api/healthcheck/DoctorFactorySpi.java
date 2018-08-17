package com.github.dapeng.api.healthcheck;


/**
 * @Author: zhup
 * @Date: 2018/8/6 11:15
 */

public interface DoctorFactorySpi {
    Doctor createInstance();
}

