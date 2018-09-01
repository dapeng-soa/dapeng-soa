package com.github.dapeng.doc.util;

import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.doc.dto.EventDto;
import com.github.dapeng.doc.dto.EventVo;
import com.github.dapeng.doc.properties.ServiceConstants;
import com.github.dapeng.json.OptimizedMetadata;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author with struy.
 * Create by 2018/3/26 11:54
 * email :yq1724555319@gmail.com
 * 服务元数据注解工具
 */

public class ServiceAnnotationsUtil {

    /**
     * 服务分组
     *
     * @param services
     * @return
     */
    public static Map<String, Collection<Service>> groupingServices(Collection<OptimizedMetadata.OptimizedService> services) {
        Map<String, Collection<Service>> groupedServices = new HashMap<>(16);
        List<Service> groupedServiceList = new ArrayList<>();
        Collection<Service> tempServices = new ArrayList<>(services.stream().map(OptimizedMetadata.OptimizedService::getService).collect(Collectors.toList()));
        services.forEach(optimizedService -> {
            Service service = optimizedService.getService();
            if (null != service.annotations) {
                service.annotations.stream().filter(x -> x.key.equals(ServiceConstants.SERVICE_GROUP_KEY))
                        .collect(Collectors.toList())
                        .forEach(att -> {
                            boolean existGroup = ServiceConstants.SERVICE_GROUP_KEY.equals(att.key);
                            if (existGroup) {
                                if (groupedServices.containsKey(att.value)) {
                                    groupedServices.get(att.value).add(service);
                                } else {
                                    List<Service> list = new ArrayList<>();
                                    list.add(service);
                                    groupedServices.put(att.value, list);
                                }
                            }
                        });
            }
        });

        for (Collection<Service> x : groupedServices.values()) {
            groupedServiceList.addAll(x);
        }
        tempServices.removeAll(groupedServiceList);
        if (tempServices.size() > 0){
            groupedServices.put(ServiceConstants.SERVICE_GROUP_DEFAULT, tempServices);
        }
        return groupedServices;
    }

    /**
     * 排除指定注解修饰的方法，避免站点恶意调用
     *
     * @param service
     * @return
     */
    public static Service excludeMethods(Service service) {
        List<Method> removeMethods = new ArrayList<>();
        if (null != service) {
            service.methods.forEach(method -> {
                if (null != method.annotations) {
                    method.annotations.forEach(att -> {
                        boolean isVirtual = ServiceConstants.SERVICE_VIRTUAL_KEY
                                .equals(att.key) && ServiceConstants.SERVICE_VIRTUAL_VAL
                                .equals(att.value.trim());
                        if (isVirtual) {
                            removeMethods.add(method);
                        }
                    });
                }
            });
            service.methods.removeAll(removeMethods);
        }
        return service;
    }


    /**
     * 查找方法内的事件列表
     *
     * @param method
     * @return
     */
    public static List<EventDto> findEventsByMethod(Method method) {

        List<EventDto> events = new ArrayList<>();
        if (null != method.getAnnotations()) {
            List<EventDto> eventDtos = method
                    .getAnnotations()
                    .stream()
                    .filter(x -> ServiceConstants.SERVICE_EVENT_KAY.equals(x.key))
                    .map(y -> new EventDto(
                            method.name,
                            y.value,
                            y.value.substring(y.value.lastIndexOf(".") + 1),
                            ""))
                    .collect(Collectors.toList());

            for (EventDto eventDto : eventDtos) {
                if (eventDto.getEvent().contains(",")) {
                    Arrays.stream(eventDto.getEvent()
                            .split(","))
                            .forEach(x ->
                                    events.add(new EventDto(
                                            method.name, x,
                                            x.substring(x.lastIndexOf(".") + 1), "")));
                } else {
                    events.addAll(eventDtos);
                }
            }
        }
        return events;
    }

    /**
     * 按照服务查找事件列表
     *
     * @param service
     * @return
     */
    public static List<EventVo> findEvents(Service service) {
        List<EventDto> events = new ArrayList<>();
        List<EventVo> eventVos = new ArrayList<>();
        if (null != service) {
            for (Method method : service.getMethods()) {
                events.addAll(findEventsByMethod(method));
            }
            Map<String, EventVo> tempVoMap = new HashMap<>();

            events.forEach(eventDto -> {
                if (tempVoMap.size() > 0) {
                    if (tempVoMap.containsKey(eventDto.getEvent())) {
                        tempVoMap.get(eventDto.getEvent()).getTouchMethods().add(eventDto.getTouchMethod());
                    } else {
                        tempVoMap.put(eventDto.getEvent(), newEventVo(eventDto));
                    }
                } else {
                    tempVoMap.put(eventDto.getEvent(), newEventVo(eventDto));
                }

            });
            tempVoMap.forEach((e, vo) -> {
                eventVos.add(vo);
            });
        }
        return eventVos;
    }

    private static EventVo newEventVo(EventDto eventDto) {
        EventVo eventVo = new EventVo();
        List<String> methods = new ArrayList<>();
        methods.add(eventDto.getTouchMethod());
        eventVo.setTouchMethods(methods);
        eventVo.setEvent(eventDto.getEvent());
        eventVo.setMark(eventDto.getMark());
        eventVo.setShortName(eventDto.getShortName());
        return eventVo;
    }
}
