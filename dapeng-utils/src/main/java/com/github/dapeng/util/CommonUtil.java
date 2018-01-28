package com.github.dapeng.util;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author ever
 */
public class CommonUtil {
    /**
     * 线程安全的单例创建方法
     * 判断nullable是否为null, 如果不是, 那么直接返回; 否则通过双重校验模式创建一个新的实例.
     *
     * @param nullableObject 可能为null的对象
     * @param supplier       对象生成器
     * @param lock           同步锁
     * @param <T>
     */
    public static <T> T newInstWithDoubleCheck(T nullableObject,
                                               Supplier<T> supplier,
                                               ReentrantLock lock) {
        try {
            lock.lock();
            if (nullableObject == null) {
                nullableObject = supplier.get();
            }
            return nullableObject;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 线程安全的集合元素创建方法
     * 根据nullChecker判断是否需要创建新元素; 如果需要, 通过双重校验模式创建一个新的实例.
     *
     * @param nullChecker 判断是否需要创建新元素
     * @param supplier    对象生成器
     * @param lock        同步锁
     * @param <T>
     */
    public static <T> void newElementWithDoubleCheck(Supplier<Boolean> nullChecker,
                                                     Supplier<T> supplier,
                                                     ReentrantLock lock) {
        try {
            lock.lock();
            if (nullChecker.get()) {
                supplier.get();
            }
        } finally {
            lock.unlock();
        }
    }
}
