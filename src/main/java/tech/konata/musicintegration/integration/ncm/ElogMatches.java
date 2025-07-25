package tech.konata.musicintegration.integration.ncm;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElogMatches {
    public static final MatchRule EXIT = new MatchRule(
            row -> row.contains("【app】,{\"actionId\":\"exitApp\"}"),
            row -> true
    );
    public static final MatchRule PRECACHE_NEXT = new MatchRule(
            row -> row.contains("【playing】,\"预请求下一首\""),
            row -> {
                Pattern pattern = Pattern.compile("\"[0-9]*\"$");
                Matcher matcher = pattern.matcher(row);
                if (matcher.find()) {
                    String group = matcher.group();
                    try {
                        return Long.parseLong(group.substring(1, group.length() - 1));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return null;
            }
    );
    public static final MatchRule SET_PLAYING_POSITION = new MatchRule(
            row -> row.contains("【playing】,\"setPlayingPosition\""),
            row -> {
                Pattern pattern = Pattern.compile("【playing】,\"setPlayingPosition\",(\\d+\\.\\d+)");
                Matcher matcher = pattern.matcher(row);
                if (matcher.find()) {
                    try {
                        return Double.parseDouble(matcher.group(1));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return null;
            }
    );
    public static final MatchRule SET_PLAYING_STATUS = new MatchRule(
            row -> row.contains("【playing】,\"native播放state\""),
            row -> {
                Pattern pattern = Pattern.compile("【playing】,\"native播放state\",(\\d+),");
                Matcher matcher = pattern.matcher(row);
                if (matcher.find()) {
                    try {
                        return Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return null;
            }
    );
    private static final Gson gson = new Gson();
    public static final MatchRule SET_PLAYING = new MatchRule(
            row -> row.contains("【playing】,\"setPlaying\""),
            row -> {
                Pattern pattern = Pattern.compile("\\{.*}$");
                Matcher matcher = pattern.matcher(row);
                if (matcher.find()) {
                    try {
                        return gson.fromJson(matcher.group(), Types.PlayingStatus.class);
                    } catch (JsonSyntaxException e) {
                        System.out.println(matcher.group());
                        e.printStackTrace();
                        return null;
                    }
                }
                return null;
            }
    );

    public static class MatchRule {
        private final Rule rule;
        private final ArgsParser<?> argsParser;

        public MatchRule(Rule rule, ArgsParser<?> argsParser) {
            this.rule = rule;
            this.argsParser = argsParser;
        }

        public boolean matches(String row) {
            return rule.test(row);
        }

        @SuppressWarnings("unchecked")
        public <T> T parseArgs(String row) {
            return (T) argsParser.parse(row);
        }

        public interface Rule {
            boolean test(String row);
        }

        public interface ArgsParser<T> {
            T parse(String row);
        }
    }
}