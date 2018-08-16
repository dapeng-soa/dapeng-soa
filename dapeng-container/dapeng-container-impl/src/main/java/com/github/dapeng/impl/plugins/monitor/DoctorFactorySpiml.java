package com.github.dapeng.impl.plugins.monitor;

import com.github.dapeng.api.healthcheck.Doctor;
import com.github.dapeng.api.healthcheck.DoctorFactorySpi;

import java.util.List;

/**
 * @Author: zhup
 * @Date: 2018/8/6 17:53
 */
public class DoctorFactorySpiml implements DoctorFactorySpi {
    public DoctorFactorySpiml() {
    }

    @Override
    public Doctor createInstance() {
        return new DapengDoctor();
    }
}
