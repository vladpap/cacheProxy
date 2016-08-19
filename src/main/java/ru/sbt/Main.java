package ru.sbt;

import ru.sbt.cacheproxy.CacheProxy;
import ru.sbt.service.ServiceImpl;
import ru.sbt.service.Service;

public class Main {
    public static void main(String[] args) {
        CacheProxy<Service> cacheProxy = new CacheProxy<>();
        Service service = cacheProxy.cache(new ServiceImpl());

        System.out.println(service.doHardWork("45", 27));
        System.out.println(service.doHardWork("45", 34));
        System.out.println(service.doHardWork("45", 27));
//        System.out.println(service.doWorkEasy("doWorkEasy"));
//        System.out.println(service.doDo());
//        System.out.println(service.doWorkEasy("doWorkEas"));
//        System.out.println(service.doWorkEasy("doWorkEasy"));
//        System.out.println(service.doWorkEasy("dsfsdf"));
        System.out.println(service.doHardWork("44", 27));
    }
}
