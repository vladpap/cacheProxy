package ru.sbt.cacheproxy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FilesForCache {
    private static final String EXTENSION_FILE = ".cache";
    private final String directoryToSaveFile;

    public FilesForCache(String directoryToSaveFile) {
        this.directoryToSaveFile = directoryToSaveFile;
    }

    protected void saveFile(Object object, String fileName, boolean zip) {

        entryDirectoy();

        if (zip) {

            saveFileInZipAchive(object, fileName);

        } else {

            File file = new File(directoryToSaveFile + fileName + EXTENSION_FILE);
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

    private void saveFileInZipAchive(Object object, String fileName) {
        File f = new File(directoryToSaveFile + fileName + ".zip");
        ZipOutputStream out = null;
        try {
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
            out.closeEntry();

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] converToByte(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput output = new ObjectOutputStream(bos)) {
            output.writeObject(object);
            return bos.toByteArray();
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

    protected Object readFile(String fileName, boolean zip) throws FileNotFoundException {

        if (zip) {
            readFileInZipArchive(fileName);

            Object result = readFileNotZipArchive(fileName);
            deleteFile(fileName);
            return result;

        } else {
            return readFileNotZipArchive(fileName);
        }

    }

    private void readFileInZipArchive(String fileName) throws FileNotFoundException {
        ZipFile zipFile;
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
    }

    private void deleteFile(String fileName) {
        try {
            Files.delete(Paths.get(directoryToSaveFile + fileName + EXTENSION_FILE));
        } catch (IOException e) {
            e.printStackTrace(); // TODO: 22.08.16
        }
    }

    private Object readFileNotZipArchive(String fileName) throws FileNotFoundException {
        File file = new File(directoryToSaveFile + fileName + EXTENSION_FILE);
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
            fileObj = objectInputStream.readObject();
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
            int length;

            while ((length = is.read(buf, 0, buf.length)) >= 0) {
                fos.write(buf, 0, length);
            }

        } catch (IOException ioex) {
            fos.close();
        }

    }
}