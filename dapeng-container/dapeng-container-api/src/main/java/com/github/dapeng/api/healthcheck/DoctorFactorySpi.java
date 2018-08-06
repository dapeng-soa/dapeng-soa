package com.github.dapeng.api.healthcheck;

import java.util.List;

/**
 * @Author: zhup
 * @Date: 2018/8/6 11:15
 */

public interface DoctorFactorySpi {
    Doctor createInstance(List<ClassLoader> applicationCls);
}

