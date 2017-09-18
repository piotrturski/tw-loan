package net.piotrturski.tw.infrastructure;

public class JsonUtils {
    public static String toJson(String input) {
        return input.replaceAll("'","\"");
    }
}
