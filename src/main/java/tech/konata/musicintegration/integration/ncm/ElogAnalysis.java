package tech.konata.musicintegration.integration.ncm;

import lombok.experimental.UtilityClass;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author IzumiiKonata
 * Date: 2025/7/1 20:09
 */
@UtilityClass
public class ElogAnalysis {

    public String decode(byte[] dataArray) {
        int[] bytesArr = new int[dataArray.length];
        for (int i = 0; i < dataArray.length; i++) {
            bytesArr[i] = dataArray[i] & 0xFF; // 转换为无符号整数
        }

        // 解码字节数组
        int[] decodedBytes = new int[bytesArr.length];
        for (int i = 0; i < bytesArr.length; i++) {
            int byteValue = bytesArr[i];
            int hexDigit = (byteValue / 16 ^ ((byteValue % 16) + 8)) % 16;
            decodedBytes[i] = hexDigit * 16 + (byteValue / 64) * 4 + (~(byteValue / 16) & 3);
        }

        // 将解码后的整数数组转换为 byte 数组
        byte[] decodedBuffer = new byte[decodedBytes.length];
        for (int i = 0; i < decodedBytes.length; i++) {
            decodedBuffer[i] = (byte) decodedBytes[i];
        }

        return new String(decodedBuffer, StandardCharsets.UTF_8);
    }

    public static LogHeader getHeader(String row) {
        Pattern pattern = Pattern.compile(
                "^\\[(\\d+):(\\d+):(\\d{4}/\\d{6}:\\d+):([A-Z]+):([a-zA-Z0-9._-]+)\\((\\d+)\\)\\]\\s+\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\]"
        );

        Matcher matcher = pattern.matcher(row);
        if (!matcher.find()) {
            return null;
        }

        String pid = matcher.group(1);
        String tid = matcher.group(2);
        String timestamp = matcher.group(3);
        String type = matcher.group(4);
        String src = matcher.group(5);
        String lr = matcher.group(6);
        String datetime = matcher.group(7);

        String[] timestampParts = timestamp.split(":");
        if (timestampParts.length < 2) {
            return null;
        }

        long startupTime = Long.parseLong(timestampParts[1]);

        // 获取系统运行时间（毫秒）
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long currentTime = System.currentTimeMillis();
        long bootTime = currentTime - uptime;
        long realLogTime = startupTime + bootTime;

        return new LogHeader(pid, tid, realLogTime, type, src, lr, datetime);
    }

    public static Types.PlayingStatusType getType(String row) {
        if (ElogMatches.EXIT.matches(row)) {
            return Types.PlayingStatusType.EXIT;
        } else if (ElogMatches.SET_PLAYING.matches(row)) {
            return Types.PlayingStatusType.SET_PLAYING;
        } else if (ElogMatches.SET_PLAYING_POSITION.matches(row)) {
            return Types.PlayingStatusType.SET_PLAYING_POSITION;
        } else if (ElogMatches.SET_PLAYING_STATUS.matches(row)) {
            return Types.PlayingStatusType.SET_PLAYING_STATUS;
        } else if (ElogMatches.PRECACHE_NEXT.matches(row)) {
            return Types.PlayingStatusType.PRECACHE_NEXT;
        }
        return null;
    }

    public static class LogHeader {
        public final String pid;
        public final String tid;
        public final long timestamp;
        public final String type;
        public final String src;
        public final String lr;
        public final String datetime;

        public LogHeader(String pid, String tid, long timestamp, String type, String src, String lr, String datetime) {
            this.pid = pid;
            this.tid = tid;
            this.timestamp = timestamp;
            this.type = type;
            this.src = src;
            this.lr = lr;
            this.datetime = datetime;
        }
    }

}
