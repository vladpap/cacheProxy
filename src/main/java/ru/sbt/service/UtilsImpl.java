package ru.sbt.service;

import ru.sbt.cacheproxy.CacheProxy.Cache;
import ru.sbt.cacheproxy.CacheType;

import static ru.sbt.cacheproxy.CacheType.IN_FILE;

public class UtilsImpl implements Utils {

    public UtilsImpl() {
    }

    @Override
    @Cache
    public String doWorker(String item, int i) {
        item = item.toUpperCase();
        String result = "";
        for (int j = 0; j < i; j++) {
            result += item;
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}
