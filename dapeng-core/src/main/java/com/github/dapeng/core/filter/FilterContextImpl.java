package com.github.dapeng.core.filter;


import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.Filter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class FilterContextImpl implements FilterContext {

    private Map<Filter, Map<String, Object>> attachmentsWithFilter = new HashMap<>();

    private Map<String, Object> attachments = new HashMap<>();

    @Override
    public void setAttach(Filter filter, String key, Object value) {
        Map<String, Object> attches = attachmentsWithFilter.get(filter);
        if(attches == null){
            attches = new HashMap<>();
            attachmentsWithFilter.put(filter, attches);
        }
        attches.put(key, value);
    }

    @Override
    public Object getAttach(Filter filter, String key) {
        Map<String, Object> attaches = attachmentsWithFilter.get(filter);
        if(attaches != null)
            return attaches.get(key);
        else return null;
    }

    @Override
    public void setAttribute(String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<String, Object>();
        }
        attachments.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        if (attachments == null){
            return null;
        }
        return attachments.get(key);
    }


}
