package ru.sbt.service;


import ru.sbt.cacheproxy.CacheProxy.Cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    @Cache(cacheType = IN_FILE)
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

    @Override
    @Cache
    public List<String> work(String item) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 123_000; i++) {
            list.add(item + " - " + i);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return list;
    }
}
