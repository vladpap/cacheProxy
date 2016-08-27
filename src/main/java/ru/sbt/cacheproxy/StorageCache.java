package ru.sbt.cacheproxy;


import java.io.FileNotFoundException;

public interface StorageCache<T> {
    void writeInStorage(Object object, T keyCached, boolean isArchived);
    Object readFromStorage(T keyCached, boolean isArchived) throws FileNotFoundException;
}