package com.github.dapeng.pinpoint.plugin;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

import java.util.List;

/**
 * Created by tangliu on 16/12/6.
 */
public class DapengConfiguration {

    private final boolean dapengEnabled;
    private final List<String> dapengBootstrapMains;

    public DapengConfiguration(ProfilerConfig config) {

        this.dapengEnabled = config.readBoolean("profiler.dapeng.enbalse", true);
        this.dapengBootstrapMains = config.readList("profiler.dapeng.bootstrap.main");
    }

    public boolean isDapengEnabled() {
        return dapengEnabled;
    }

    public List<String> getDapengBootstrapMains() {
        return dapengBootstrapMains;
    }
}
