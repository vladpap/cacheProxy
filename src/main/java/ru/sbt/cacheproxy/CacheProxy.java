package ru.sbt.cacheproxy;


import ru.sbt.annotations.Cache;
import ru.sbt.annotations.CacheType;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static java.lang.ClassLoader.getSystemClassLoader;

public class CacheProxy implements InvocationHandler {

    private Object delegate;
    private final Map<Object, Object> resultByArg;
    private final String directoryToSaveFile;
    private static final String DEFAULT_DIRECTORY = "./cache_directory/";


    public CacheProxy() {
        this.directoryToSaveFile = DEFAULT_DIRECTORY;
        this.delegate = null;
        this.resultByArg = null;
    }

    public CacheProxy(String dirToSaveFile) {
        this.directoryToSaveFile = ((dirToSaveFile == null) || (dirToSaveFile.length() == 0)) ? DEFAULT_DIRECTORY : dirToSaveFile;
        this.delegate = null;
        this.resultByArg = null;
    }

    private CacheProxy(Object object, String dir) {
        this.delegate = object;
        this.resultByArg = new HashMap<>();
        this.directoryToSaveFile = dir;
    }


    public<T extends Serializable> T cache(T object) {
        Method[] methods = object.getClass().getMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(Cache.class)) {
                this.delegate = object;
                Object o = Proxy.newProxyInstance(getSystemClassLoader(),
                        object.getClass().getInterfaces(),
                        new CacheProxy(object, directoryToSaveFile));
                return (T) o;
            }
        }
        return (T) object;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Method delegateMethod = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());
        Object[] argsKey;
        if (!delegateMethod.isAnnotationPresent(Cache.class)) {
            return invoke(delegateMethod, args);
        }
        Cache cache = delegateMethod.getAnnotation(Cache.class);

        argsKey = getArgumentsByIndentityInMethod(args, Arrays.asList(cache.identityBy()));

        String fileName = (cache.fileNamePrefix() + delegateMethod.getName() + key(delegateMethod, argsKey));

        Object result;
        if (cache.cacheType().equals(CacheType.IN_FILE)) {
            result = getResultCacheFromFile(args, delegateMethod, cache, fileName);
        } else {
            result = getResultCacheFromMemory(args, delegateMethod, argsKey, cache);
        }
        return result;
    }

    private Object getResultCacheFromMemory(Object[] args, Method delegateMethod, Object[] argsKey, Cache cache) throws Throwable {
        Object result;
        if (!resultByArg.containsKey(key(delegateMethod, argsKey))) {
            result = invoke(delegateMethod, args);
            result = getObjectAndCheckInstanceList(cache, result);
            resultByArg.put(key(delegateMethod, argsKey), result);
        } else {
            result = resultByArg.get(key(delegateMethod, argsKey));
        }
        return result;
    }

    private Object getResultCacheFromFile(Object[] args, Method delegateMethod, Cache cache, String fileName) throws Throwable {
        FilesForCache filesForCache = new FilesForCache(directoryToSaveFile);
        Object result;
        try {
            result = filesForCache.readFile(fileName, cache.zip());
            if (result == null) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            result = invoke(delegateMethod, args);
            result = getObjectAndCheckInstanceList(cache, result);
            filesForCache.saveFile(result, fileName, cache.zip());
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
        result = (result instanceof List<?>) ? new ArrayList<>(((List) result).subList(0, cache.maxListList())) : result;
        return result;
    }


    private String key(Method method, Object[] args) {
        return (method.getName() + Arrays.toString(args)).replace(",", "").replace(".", "_").replaceAll("\\s", "");
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
}
