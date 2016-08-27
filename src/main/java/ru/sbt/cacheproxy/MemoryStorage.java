package ru.sbt.cacheproxy;

import java.util.HashMap;
import java.util.Map;

public class MemoryStorage<T> implements StorageCache<T> {
    private final Map cacheMap;

    public MemoryStorage() {
        this.cacheMap = new HashMap<T, Object>();
    }

    @Override
    public void writeInStorage(Object object, T keyCached, boolean isArchived) {
        cacheMap.put(keyCached, object);
    }

    @Override
    public Object readFromStorage(T keyCached, boolean isArchived) {
        return cacheMap.getOrDefault(keyCached, null);
    }
}