package ru.sbt.cacheproxy;


import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static ru.sbt.cacheproxy.CacheType.IN_MEMORY;

public class CacheProxy<T extends Serializable> implements InvocationHandler {

    private final Object delegate;
    private final CacheProxyMap<Object, Object> resultByArg;
    private String directoryToSaveFile = "./";
    private final String extensionFile = ".cache";


    @Target(value = ElementType.METHOD)
    @Retention(value = RetentionPolicy.RUNTIME)
    @Documented
    public @interface Cache {
        CacheType cacheType() default IN_MEMORY;

        String fileNamePrefix() default "";

        boolean zip() default false;

        Class<?>[] identityBy() default {};

        int maxListList() default 100_000;
    }


    public CacheProxy() {
        this.delegate = null;
        this.resultByArg = null;
    }

    private CacheProxy(Object object) {
        this.delegate = object;
        this.resultByArg = new CacheProxyMap<>();
    }

    public void setDirectoryToSaveFile(String directoryToSaveFile) {
        if (delegate == null) {
            this.directoryToSaveFile = directoryToSaveFile;
        } else {
            throw new RuntimeException("No set directory to save file after call method cache");
        }
    }

    public T cache(T object) {
        Method[] methods = object.getClass().getMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(Cache.class)) {
                /*
                System.out.print("Method \'" + method.getName() + "\' ");
                Cache cache = method.getAnnotation(Cache.class);
                CacheType cacheType = cache.cacheType();
                switch (cacheType) {
                    case IN_FILE:
                        System.out.println("Cached in file " + cache.fileNamePrefix() + (cache.zip() ? ".zip" : ".dat"));
                        break;
                    case IN_MEMORY:
                        System.out.println("Cached in memory ");
                        break;
                    case IN_DATABASE:
                        System.out.println("Cached in data base ");
                        break;
                }
                System.out.println("Goto invoke");
                */
                Object o = Proxy.newProxyInstance(getSystemClassLoader(),
                        object.getClass().getInterfaces(),
                        new CacheProxy<T>(object));
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
        List<Class<?>> identityByClass = Arrays.asList(cache.identityBy());
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
        String fileName = (cache.fileNamePrefix().length() == 0) ? delegateMethod.getName() : cache.fileNamePrefix();

        if (cache.cacheType().equals(CacheType.IN_FILE)) {
            readFile(fileName, cache.zip());
        }

        if (!resultByArg.containsKey(key(delegateMethod, argsKey))) {
            Object result = invoke(delegateMethod, args);
            if (result instanceof List<?>) {
                List list = (List) result;
                result = list.subList(0, cache.maxListList());
            }
            resultByArg.put(key(delegateMethod, argsKey), result);
            if (cache.cacheType().equals(CacheType.IN_FILE)) {
                saveFile(fileName, cache.zip());
            }
        }

        return resultByArg.get(key(delegateMethod, argsKey));
    }

    private void saveFile(String fileName, boolean zip) {

        if (zip) {

        } else {
            File file = new File(directoryToSaveFile + fileName + extensionFile);
            FileOutputStream fileOutputStream = null; //-=-=-==-=-
            try {
                fileOutputStream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            ObjectOutputStream objectOutputStream = null; // -=-=-=-==-=
            try {
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(resultByArg);
                objectOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void readFile(String fileName, boolean zip) {

        if (zip) {

        } else {
            File file = new File(directoryToSaveFile + fileName + extensionFile);
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                return;
            }
            ObjectInputStream objectInputStream = null;
            CacheProxyMap<Object, Object> fileObj = null;
            try {
                objectInputStream = new ObjectInputStream(fileInputStream);
                fileObj = (CacheProxyMap<Object, Object>) objectInputStream.readObject();
                objectInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Object input stream exception file name :" + file.toString());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found exception from read object in file name :" + file.toString());
            }

            for (Map.Entry<Object, Object> entry : fileObj.entrySet()) {
                resultByArg.put(entry.getKey(), entry.getValue());
            }

        }

    }

    private Object key(Method method, Object[] args) {

        String key = method.getName();
        for (Object arg : args) {
            key += arg.toString();
        }
        return key;
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
