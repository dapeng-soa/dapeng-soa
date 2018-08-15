package com.github.dapeng.api.healthcheck;

import java.util.ServiceLoader;

/**
 * @Author: zhup
 * @Date: 2018/8/6 10:59
 */

public class DoctorFactory {

    private static Doctor doctor;

    public static void createDoctor(ClassLoader containerCl) {
        if (doctor == null) {
            synchronized (DoctorFactory.class) {
                if (doctor == null) {
                    ServiceLoader<DoctorFactorySpi> doctorFactorySpis = ServiceLoader.load(DoctorFactorySpi.class, containerCl);
                    assert doctorFactorySpis.iterator().hasNext();
                    doctor = doctorFactorySpis.iterator().next().createInstance();
                }
            }
        }
    }

    public static Doctor getDoctor() {
        return doctor;
    }
}
