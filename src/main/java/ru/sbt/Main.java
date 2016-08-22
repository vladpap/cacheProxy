package ru.sbt;

import ru.sbt.cacheproxy.CacheProxy;
import ru.sbt.service.ServiceImpl;
import ru.sbt.service.Service;
import ru.sbt.service.Utils;
import ru.sbt.service.UtilsImpl;


public class Main {
    public static void main(String[] args) {
        CacheProxy<Service> cacheProxy = new CacheProxy<>("");
        Service service = cacheProxy.cache(new ServiceImpl());

        CacheProxy<Utils> utilsCacheProxy = new CacheProxy<>("");
        Utils utils = utilsCacheProxy.cache(new UtilsImpl());

        Object result;
        long start = System.currentTimeMillis();
        System.out.println(service.doHardWork("45", 34));
        System.out.println(service.doHardWork("45", 27));
        System.out.println(service.doWorkEasy("doWorkEasy"));
        System.out.println(service.doDo());
        System.out.println(service.doWorkEasy("doWorkEas"));
        System.out.println(service.doWorkEasy("doWorkEasy"));
        System.out.println(service.doWorkEasy("dsfsdf"));
        System.out.println(service.doHardWork("45", 27));
        System.out.println(service.work("Hello").size());
        System.out.println(service.doHardWork("44", 27));
        System.out.println(service.work("Hello").size());
        System.out.println(utils.doWorker("dfsdf", 5));
        System.out.println(utils.doWorker("dfsdf", 5));
        System.out.println("-----------------------------------");
        System.out.println("Total time : " + ((System.currentTimeMillis() - start) / 1_000) + " sec.");
    }
}
