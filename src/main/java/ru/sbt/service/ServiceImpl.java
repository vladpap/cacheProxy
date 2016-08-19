package ru.sbt.service;


import ru.sbt.cacheproxy.CacheProxy.Cache;

import java.io.Serializable;

import static ru.sbt.cacheproxy.CacheType.IN_FILE;

public class ServiceImpl implements Service {

    public ServiceImpl() {
    }

    @Override
    @Cache(cacheType = IN_FILE, fileNamePrefix = "data", zip = false, identityBy = {Integer.class})
    public double doHardWork(String work, int i) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 2.0 * Math.PI * i;
    }

    @Override
    @Cache
    public String doWorkEasy(String work) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return work.toUpperCase();
    }

    @Override
    public String doDo() {
        return "DoDo";
    }

}
