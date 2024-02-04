package com.asuala.mock.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @description:
 * @create: 2024/01/18
 **/
@Slf4j
public class CacheUtils {


    //缓存过期时间 单位 s
    public static final int expireTime = 120;

    private static Map<String, LocalDateTime> cache = new HashMap<>();

    private static Lock cacheLock = new ReentrantLock();

    public static Map<String, LocalDateTime> cache(String key, LocalDateTime time) {
        cacheLock.lock();
        try {
            if (null != time) {
                if (StringUtils.isNotBlank(key)){
                    cache.put(key, time);
                }else {
                    LocalDateTime oldTime = time.minusSeconds(expireTime);
                    Iterator<Map.Entry<String, LocalDateTime>> iterator = cache.entrySet().iterator();
                    while (iterator.hasNext()){
                        Map.Entry<String, LocalDateTime> entry = iterator.next();
                        if (oldTime.isAfter(entry.getValue())) {
                            iterator.remove();
                        }
                    }
                }
            } else if (StringUtils.isNotBlank(key)){
                cache.remove(key);
            }
            return cache;
        } finally {
            cacheLock.unlock();
        }
    }

//    public static void setCache(Map<String, LocalDateTime> cache) {
//        CacheUtils.cache = cache;
//    }


    public static int x = 1;

    public static int pauseSecond = 60;
    private static LocalDateTime pauseTime;
    private static Lock pauseLock = new ReentrantLock();

    public static boolean downFlag(LocalDateTime time) {
        pauseLock.lock();
        try {
            if (null == time) {
                if (null == pauseTime) {
                    return true;
                }
                boolean flag = LocalDateTime.now().minusSeconds(pauseSecond * x).isAfter(pauseTime);
                return flag;
            }
            if (null != time) {
                x++;
                log.debug("暂停因数 x = {}", CacheUtils.x);
                pauseTime = time;
            }
            return false;
        } finally {
            pauseLock.unlock();
        }
    }


    private static long lastId = -1L;
    private static Lock lock = new ReentrantLock();

    public static long setLastId(Long id) {
        lock.lock();
        try {
            if (null == id) {
                return lastId;
            } else if (lastId == id) {
                lastId = -1;
            } else if (lastId < id) {
                lastId = id;
            }
            // 原子操作
            return lastId;
        } finally {
            lock.unlock();
        }
    }
}