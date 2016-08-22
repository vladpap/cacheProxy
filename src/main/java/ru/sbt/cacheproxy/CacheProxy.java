package ru.sbt.cacheproxy;


import ru.sbt.annotations.Cache;
import ru.sbt.annotations.CacheType;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.*;

import static java.lang.ClassLoader.getSystemClassLoader;

public class CacheProxy<T extends Serializable> implements InvocationHandler {

    private Object delegate;
    private final CacheProxyMap<Object, Object> resultByArg;
    private final String directoryToSaveFile;
    private final String extensionFile = ".cache";


    public CacheProxy(String dirToSaveFile) {
        this.directoryToSaveFile = ((dirToSaveFile == null) || (dirToSaveFile.length() == 0)) ? "./cache_directory/" : dirToSaveFile;
        this.delegate = null;
        this.resultByArg = null;
    }

    private CacheProxy(Object object, String dir) {
        this.delegate = object;
        this.resultByArg = new CacheProxyMap<>();
        this.directoryToSaveFile = dir;
    }


    public T cache(T object) {
        Method[] methods = object.getClass().getMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(Cache.class)) {
                this.delegate = object;
                Object o = Proxy.newProxyInstance(getSystemClassLoader(),
                        object.getClass().getInterfaces(),
                        new CacheProxy<T>(object, directoryToSaveFile));
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

        List<Class> identityByClass = Arrays.asList(cache.identityBy());
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

        String fileName = (cache.fileNamePrefix() + delegateMethod.getName() + key(delegateMethod, argsKey)).replace(".", "_");

        Object result;
        if (cache.cacheType().equals(CacheType.IN_FILE)) {
            try {
                result = readFile(fileName, cache.zip());
                if (result == null) {
                    throw new FileNotFoundException();
                }
            } catch (FileNotFoundException e) {
                result = invoke(delegateMethod, args);
                result = getObjectAndCheckInstanceList(cache, result);
                saveFile(result, fileName, cache.zip());
            }
        } else {
            if (!resultByArg.containsKey(key(delegateMethod, argsKey))) {
                result = invoke(delegateMethod, args);
                result = getObjectAndCheckInstanceList(cache, result);
                resultByArg.put(key(delegateMethod, argsKey), result);
            } else {
                result = resultByArg.get(key(delegateMethod, argsKey));
            }
        }

        return result;
    }

    private Object getObjectAndCheckInstanceList(Cache cache, Object result) {
        result = (result instanceof List<?>) ? new ArrayList<>(((List) result).subList(0, cache.maxListList())) : result;
        return result;
    }

    private void saveFile(Object object, String fileName, boolean zip) {

        entryDirectoy();

        if (zip) {

            File f = new File(directoryToSaveFile + fileName + ".zip");
            ZipOutputStream out = null;
            try {
                out = new ZipOutputStream(new FileOutputStream(f));
                ZipEntry entry = new ZipEntry(fileName + extensionFile);
                out.putNextEntry(entry);
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] data = new byte[0];
            try {
                data = converToByte(object);
            } catch (IOException e) {
                throw new RuntimeException("Exception conver Object to byte[]", e);
            }
            try {
                out.write(data, 0, data.length);
                out.closeEntry();

                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {

            File file = new File(directoryToSaveFile + fileName + extensionFile);
            FileOutputStream fileOutputStream;
            try {
                fileOutputStream = new FileOutputStream(file);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(object);
                objectOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace(); // TODO: 21.08.16
            } catch (IOException e) {
                e.printStackTrace(); // TODO: 21.08.16
            }

        }
    }

    private byte[] converToByte(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput output = new ObjectOutputStream(bos)) {
            output.writeObject(object);
            return bos.toByteArray();
        }
    }

    private Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput input = new ObjectInputStream(bis)) {
            return input.readObject();
        }
    }

    private void entryDirectoy() {
        if (!Files.isDirectory(Paths.get(directoryToSaveFile))) {
            try {
                new File(directoryToSaveFile).mkdir();
            } catch (Exception e) {
                new RuntimeException("Error create directory : " + directoryToSaveFile, e);
            }
        }
    }

    private Object readFile(String fileName, boolean zip) throws IOException {

        if (zip) {
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(directoryToSaveFile + fileName + ".zip");
            } catch (IOException e) {
                throw new FileNotFoundException();
            }

            try {
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    extractEntry(entry, zipFile.getInputStream(entry));
                }
            } catch (IOException e) {
                e.printStackTrace();   // TODO: 22.08.16  
            } finally {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace(); // TODO: 22.08.16
                }
            }


            Object result = readFileNotZipArchive(fileName);
            deleteFile(fileName);
            return result;

        } else {
            return readFileNotZipArchive(fileName);
        }

    }

    private void deleteFile(String fileName) {
        try {
            Files.delete(Paths.get(directoryToSaveFile + fileName + extensionFile));
        } catch (IOException e) {
            e.printStackTrace(); // TODO: 22.08.16
        }
    }

    private Object readFileNotZipArchive(String fileName) throws FileNotFoundException {
        File file = new File(directoryToSaveFile + fileName + extensionFile);
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        }
        ObjectInputStream objectInputStream;
        Object fileObj;
        try {
            objectInputStream = new ObjectInputStream(fileInputStream);
            fileObj = (Object) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Object input stream exception file name :" + file.toString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found exception from read object in file name :" + file.toString());
        }

        return fileObj;
    }

    private void extractEntry(final ZipEntry entry, InputStream is) throws IOException {
        String exractedFile = directoryToSaveFile + entry.getName();
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(exractedFile);
            final byte[] buf = new byte[2048];
            int read = 0;
            int length;

            while ((length = is.read(buf, 0, buf.length)) >= 0) {
                fos.write(buf, 0, length);
            }

        } catch (IOException ioex) {
            fos.close();
        }

    }


    private String key(Method method, Object[] args) {

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
