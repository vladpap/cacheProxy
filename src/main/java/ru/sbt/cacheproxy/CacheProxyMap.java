package ru.sbt.cacheproxy;

import java.io.Serializable;
import java.util.HashMap;

public class CacheProxyMap<K,V> extends HashMap<K,V> implements Serializable{
    public static final long serialVersionUID = 1945054006882561550L;

    public CacheProxyMap() {
        super();
    }
}
