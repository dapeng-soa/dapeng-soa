package com.github.dapeng.api.healthcheck;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @Author: zhup
 * @Date: 2018/8/6 10:59
 */

public class DoctorFactory {

    public static volatile Doctor doctor;

    public static void createDoctor(List<ClassLoader> applicationCls, ClassLoader containerCl){
        if (doctor == null) {
            synchronized (DoctorFactory.class) {
                ServiceLoader<DoctorFactorySpi> doctorFactorySpis = ServiceLoader.load(DoctorFactorySpi.class, containerCl);
                assert doctorFactorySpis.iterator().hasNext();
                doctor = doctorFactorySpis.iterator().next().createInstance(applicationCls);
            }
        }

    }

    public static Doctor getDoctor(){
        return doctor;
    }
}
