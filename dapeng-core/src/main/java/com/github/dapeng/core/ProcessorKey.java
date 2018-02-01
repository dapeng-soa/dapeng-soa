package com.github.dapeng.core;

/**
 * Created by tangliu on 2016/3/29.
 */
public class ProcessorKey {

    public ProcessorKey(String serviceName, String versionName) {
        this.serviceName = serviceName;
        this.versionName = versionName;
    }

    public final  String serviceName;

    public final String versionName;

    @Override
    public int hashCode() {
        return serviceName.hashCode() + versionName.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof ProcessorKey) {
            ProcessorKey target = (ProcessorKey) o;

            if (target.serviceName.equals(this.serviceName) && target.versionName.equals(this.versionName)) {
                return true;
            }
        }

        return false;
    }
}
