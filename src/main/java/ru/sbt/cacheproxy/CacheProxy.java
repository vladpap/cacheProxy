package ru.sbt.cacheproxy;


import java.io.Serializable;
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
    private final Map<Object, Object> resultByArg;


    @Target(value = ElementType.METHOD)
    @Retention(value = RetentionPolicy.RUNTIME)
    @Documented
    public @interface Cache {
        CacheType cacheType() default IN_MEMORY;

        String fileNamePrefix();

        boolean zip() default true;

        Class<?>[] identityBy() default {};
    }


    public CacheProxy() {
        this.delegate = null;
        this.resultByArg = null;
    }

    private CacheProxy(Object object) {
        this.delegate = object;
        this.resultByArg = new HashMap<>();
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
        } else {
            Cache cache = delegateMethod.getAnnotation(Cache.class);
            List<Class<?>> identityByClass = Arrays.asList(cache.identityBy());
            if (identityByClass.size() != 0) {
                List<Object> list = new ArrayList<>();
                for (Object arg : args) {
                    if (identityByClass.contains(arg.getClass())) {
                        list.add(arg);
                    }
                }
                argsKey = new Object[list.size()];
                argsKey = list.toArray();
            } else {
                argsKey = args;
            }
            if (!resultByArg.containsKey(key(delegateMethod, argsKey))) {
                Object result = invoke(delegateMethod, args);
                resultByArg.put(key(delegateMethod, argsKey), result);
            }
        }
        return resultByArg.get(key(delegateMethod, argsKey));
    }

    private Object key(Method method, Object[] args) {
        List<Object> key = new ArrayList<>();
        key.add(method);
        key.addAll(asList(args));
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
