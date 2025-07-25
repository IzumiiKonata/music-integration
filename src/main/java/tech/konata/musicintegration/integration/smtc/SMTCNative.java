package tech.konata.musicintegration.integration.smtc;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SMTCNative {

    private static final NativeLibrary library;
    private static final Function funcGetMediaInfo, funcFreeMediaInfo;

    static {
        library = NativeLibrary.getInstance(extractNative());

        funcGetMediaInfo = library.getFunction("getMediaInfo");
        funcFreeMediaInfo = library.getFunction("freeMediaInfo");
    }

    // 动态调用返回 MediaInfoC 结构体的函数
    public static MediaInfo getCurrentMediaInfo() {

        if (funcGetMediaInfo == null)
            return null;

        Pointer pointer = funcGetMediaInfo.invokePointer(new Object[0]);

        if (pointer == null) {
            return null;
        }

        try {
            // 创建结构体并从内存读取
            MediaInfoC cInfo = new MediaInfoC(pointer);
            cInfo.read();

            // 转换为 Java 对象
            MediaInfo javaInfo = new MediaInfo();
            javaInfo.title = cInfo.getTitleString();
            javaInfo.artist = cInfo.getArtistString();
            javaInfo.album = cInfo.getAlbumString();

            // 处理缩略图
            if (cInfo.thumbnailData != null && cInfo.thumbnailSize > 0) {
                javaInfo.thumbnailData = cInfo.thumbnailData.getByteArray(0, cInfo.thumbnailSize);
            }

            javaInfo.positionMs = cInfo.positionMs;
            javaInfo.durationMs = cInfo.durationMs;
            javaInfo.playbackRate = cInfo.playbackRate;
            javaInfo.isPlaying = cInfo.isPlaying;

            return javaInfo;
        } finally {
            // 释放内存
            freeMediaInfo(pointer);
        }
    }

    private static void freeMediaInfo(Pointer info) {
        if (funcFreeMediaInfo != null)
            funcFreeMediaInfo.invokeVoid(new Object[]{info});
    }

    public static void close() {
        if (library != null) {
            library.dispose();
        }
    }

    private static String extractNative() {

        File libFile;
        String libFileName = "/SMTCReader.dll";

        try {
            libFile = File.createTempFile("lib", null);
            libFile.deleteOnExit();
            if (!libFile.exists()) {
                throw new IOException();
            }
        } catch (IOException iOException) {
            throw new UnsatisfiedLinkError("Failed to create temp file");
        }

        byte[] arrayOfByte = new byte[2048];
        try {

            InputStream inputStream = SMTCNative.class.getResourceAsStream(libFileName);
            if (inputStream == null) {
                throw new UnsatisfiedLinkError(String.format("Failed to open lib file: %s", libFileName));
            }

            try (FileOutputStream fileOutputStream = new FileOutputStream(libFile)) {
                int size;
                while ((size = inputStream.read(arrayOfByte)) != -1) {
                    fileOutputStream.write(arrayOfByte, 0, size);
                }
            } catch (Throwable throwable) {

                try {
                    inputStream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }

                throw throwable;
            }

        } catch (IOException exception) {
            throw new UnsatisfiedLinkError(String.format("Failed to copy file: %s", exception.getMessage()));
        }

        return libFile.getAbsolutePath();
    }

//    public static native MediaInfo getCurrentMediaInfo();

}