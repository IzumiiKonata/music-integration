package tech.konata.musicintegration.integration.ncm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author IzumiiKonata
 * Date: 2025/7/1 20:03
 */
public class ElogListener {

    private long fileSize = 0;
    private final String filePath;
    private final int watchInterval = 100; // 毫秒
    private ScheduledExecutorService scheduler;
    private final List<LineListener> lineListeners = new ArrayList<>();
    public ElogListener() {
        this.filePath = NCMConstants.CLOUDMUSIC_ELOG_PATH.getAbsolutePath();
    }

    public void addLineListener(LineListener listener) {
        lineListeners.add(listener);
    }

    public void removeLineListener(LineListener listener) {
        lineListeners.remove(listener);
    }

    private void emitLine(String line) {
        for (LineListener listener : lineListeners) {
            listener.onLine(line);
        }
    }

    private void watchListener() {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }

            long currentSize = file.length();

            if (currentSize < this.fileSize) {
                this.fileSize = 0;
                return;
            }

            if (currentSize == this.fileSize) {
                return;
            }

            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(this.fileSize);

                byte[] buffer = new byte[(int) (currentSize - this.fileSize)];
                raf.readFully(buffer);

                // 解码数据
                String decodedData = ElogAnalysis.decode(buffer);
                String[] lines = decodedData.split("\n");

                emitLines(lines);
            }

            this.fileSize = currentSize;

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public String[] start() throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File does not exist: " + filePath);
        }

        byte[] buffer = Files.readAllBytes(path);
        String decodedData = ElogAnalysis.decode(buffer);
        String[] lines = decodedData.split("\n");

        this.fileSize = buffer.length;

        // 启动定时监听
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::watchListener,
                watchInterval,
                watchInterval,
                TimeUnit.MILLISECONDS
        );

        return lines;
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
//                Thread.currentThread().interrupt();
            }
        }
    }

    private void emitLines(String[] lines) {
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                emitLine(line);
            }
        }
    }

    public interface LineListener {
        void onLine(String line);
    }
}
