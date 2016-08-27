package ru.sbt.cacheproxy;


public interface StorageCache<T> {
    void writeInStorage(Object object, T keyCached, boolean isArchived);
    Object readFromStorage(T keyCached, boolean isArchived);
}