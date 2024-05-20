package com.hmdp.utils;

/**
 * @author 韩飞龙
 * @version 1.0
 */
public interface ILock {
    boolean tryLock(long timeoutSec);
    void unlock();
}
