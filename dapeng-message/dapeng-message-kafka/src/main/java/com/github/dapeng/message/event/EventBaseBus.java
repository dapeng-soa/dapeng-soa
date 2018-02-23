package com.github.dapeng.message.event;


/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月23日 上午11:11
 */
public abstract class EventBaseBus {

    public static void fireEvent(Object event) {
        dispatchEvent(event);
        PersistenceEvent(event);

    }

    protected static void dispatchEvent(Object event){};

    private static void PersistenceEvent(Object event) {


    }

    protected abstract void matchCaseEvent(Object event);


}
