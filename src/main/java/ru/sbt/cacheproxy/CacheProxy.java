package ru.sbt.cacheproxy;


import ru.sbt.annotations.Cache;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static java.lang.ClassLoader.getSystemClassLoader;

public class CacheProxy implements InvocationHandler {
    private static final String DEFAULT_DIRECTORY = "./cache_directory/";

    private final Object delegate;
    private final String directoryToSaveFile;
    private final StorageCache storageInMemory;
    private final StorageCache storageInFile;

    public CacheProxy() {
        this.directoryToSaveFile = DEFAULT_DIRECTORY;
        this.delegate = null;
        this.storageInMemory = null;
        this.storageInFile = null;
    }

    public CacheProxy(String dirToSaveFile) {
        this.directoryToSaveFile = ((dirToSaveFile == null) || (dirToSaveFile.length() == 0)) ? DEFAULT_DIRECTORY : dirToSaveFile;
        this.delegate = null;
        this.storageInMemory = null;
        this.storageInFile = null;
    }

    private CacheProxy(Object object, String dir) {
        this.delegate = object;
        this.directoryToSaveFile = dir;
        this.storageInMemory = new MemoryStorage<String>();
        this.storageInFile = new FileStorage<String>(dir);
    }

    public <T extends Serializable> T cache(T object) {
        for (Method method : object.getClass().getMethods()) {
            if (method.isAnnotationPresent(Cache.class)) {
                return (T) Proxy.newProxyInstance(getSystemClassLoader(),
                        object.getClass().getInterfaces(),
                        new CacheProxy(object, directoryToSaveFile));
            }
        }
        return object;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Method delegateMethod = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());

        if (!delegateMethod.isAnnotationPresent(Cache.class)) {
            return invoke(delegateMethod, args);
        }

        Cache cache = delegateMethod.getAnnotation(Cache.class);

        Object[] argsKey = getArgumentsByIndentityInMethod(args, Arrays.asList(cache.identityBy()));

        Object result;
        switch (cache.cacheType()) {
            case IN_FILE:
                String fileName = (cache.fileNamePrefix() + delegateMethod.getName() + key(delegateMethod, argsKey));
                result = getResultCacheFromFile(args, delegateMethod, cache, fileName);
                break;
            case IN_MEMORY:
            default:
                result = getResultCacheFromMemory(args, delegateMethod, argsKey, cache);
                break;
        }
        return result;
    }

    private Object invoke(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access exception in method : " + method.getName(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Invocation exception in method : " + method.getName(), e);
        }
    }

    private Object getResultCacheFromMemory(Object[] args, Method delegateMethod, Object[] argsKey, Cache cache) throws Throwable {
        Object result;
        result = storageInMemory.readFromStorage(key(delegateMethod, argsKey), false);
        if (result == null) {
            result = invoke(delegateMethod, args);
            result = getObjectAndCheckInstanceList(cache, result);
            storageInMemory.writeInStorage(result, key(delegateMethod, argsKey), false);
        }
        return result;
    }

    private Object getResultCacheFromFile(Object[] args, Method delegateMethod, Cache cache, String fileName) throws Throwable {
        Object result;
        try {
            result = storageInFile.readFromStorage(fileName, cache.zip());
        } catch (FileNotFoundException e) {
            result = invoke(delegateMethod, args);
            result = getObjectAndCheckInstanceList(cache, result);
            storageInFile.writeInStorage(result, fileName, cache.zip());
        }
        return result;
    }

    private Object[] getArgumentsByIndentityInMethod(Object[] args, List<Class> identityByClass) {
        Object[] argsKey;
        if (identityByClass.size() != 0) {
            List<Object> list = new ArrayList<>();
            for (Object arg : args) {
                if (identityByClass.contains(arg.getClass())) {
                    list.add(arg);
                }
            }
            argsKey = list.toArray();
        } else {
            argsKey = args;
        }
        return argsKey;
    }

    private Object getObjectAndCheckInstanceList(Cache cache, Object result) {
        if (result instanceof List<?>) result = new ArrayList<>(((List) result).subList(0, cache.maxListList()));
        return result;
    }

    private String key(Method method, Object[] args) {
        return (method.getName() + Arrays.toString(args)).replace(",", "").replace(".", "_").replaceAll("\\s", "");
    }

}