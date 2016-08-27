package ru.sbt.cacheproxy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileStorage<T> implements StorageCache<T> {
    private static final String EXTENSION_FILE = ".cache";
    private final String directoryToSave;

    private FileStorage() {
        throw new IllegalArgumentException("Constructor \'FileStorage()\' not used. Use \'new FileStorage(String directoryToSave)\'.");
    }

    public FileStorage(String directoryToSave) {
        this.directoryToSave = directoryToSave;
    }

    @Override
    public Object readFromStorage(T keyCached, boolean isArchived) throws FileNotFoundException {
        String fileName = keyCached.toString();
        if (isArchived) readFileInZipArchive(fileName);

        Object result = readFileNotZipArchive(fileName);
        if (isArchived) deleteFile(fileName);
        return result;
    }

    @Override
    public void writeInStorage(Object object, T keyCached, boolean isArchived) {
        String fileName = keyCached.toString();
        presentDirecory();
        if (isArchived) {
            saveFileInZipAchive(object, fileName);
        } else {

            saveFileWithoutArchive(object, fileName);
        }
    }

    private void saveFileWithoutArchive(Object object, String fileName) {
        File file = new File(directoryToSave + fileName + EXTENSION_FILE);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(object);
        } catch (IOException e) {
            throw new RuntimeException("Error save file: " + directoryToSave + fileName + EXTENSION_FILE, e);
        }
    }

    private void saveFileInZipAchive(Object object, String fileName) {
        ZipOutputStream out = null;
        try (File f = new File(directoryToSave + fileName + ".zip")) {
            out = new ZipOutputStream(new FileOutputStream(f));
            ZipEntry entry = new ZipEntry(fileName + EXTENSION_FILE);
            out.putNextEntry(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] data;
        try {
            data = converToByte(object);
        } catch (IOException e) {
            throw new RuntimeException("Exception conver Object to byte[]", e);
        }
        try {
            out.write(data, 0, data.length);
        } catch (IOException e) {
            throw new RuntimeException("Error save archive file: " + directoryToSave + fileName + ".zip");
        }
    }

    private byte[] converToByte(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput output = new ObjectOutputStream(bos)) {
            output.writeObject(object);
            return bos.toByteArray();
        }
    }

    private void presentDirecory() {
        if (!Files.isDirectory(Paths.get(directoryToSave))) {
            try {
                new File(directoryToSave).mkdir();
            } catch (Exception e) {
                new RuntimeException("Error create directory : " + directoryToSave, e);
            }
        }
    }

    private void deleteFile(String fileName) {
        try {
            Files.delete(Paths.get(directoryToSave + fileName + EXTENSION_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Exception from delete file: " + directoryToSave + fileName + EXTENSION_FILE, e);
        }
    }

    private Object readFileNotZipArchive(String fileName) throws FileNotFoundException {
        FileInputStream fileInputStream;
        try (File file = new File(directoryToSave + fileName + EXTENSION_FILE)) {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("File not found :" + directoryToSave + fileName + EXTENSION_FILE);
        }
        ObjectInputStream objectInputStream;
        Object fileObj;
        try {
            objectInputStream = new ObjectInputStream(fileInputStream);
            fileObj = objectInputStream.readObject();
        } catch (IOException e) {
            throw new RuntimeException("Object input stream exception file name :" + file.toString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found exception from read object in file name :" + file.toString());
        }

        return fileObj;
    }

    private void readFileInZipArchive(String fileName) throws FileNotFoundException {
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(directoryToSave + fileName + ".zip");
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
            throw new RuntimeException("Error read file: " + directoryToSave + fileName, e);
        }
    }

    private void extractEntry(final ZipEntry entry, InputStream is) {
        String exractedFile = directoryToSave + entry.getName();
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(exractedFile);

            final byte[] buf = new byte[2048];
            int length;

            while ((length = is.read(buf, 0, buf.length)) >= 0) {
                fos.write(buf, 0, length);
            }
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }
}