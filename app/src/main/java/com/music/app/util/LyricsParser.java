package com.music.app.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsParser {
    public static class Line {
        public long time;
        public String text;
    }

    private static final Pattern P =
        Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\](.*)");

    public static List<Line> parse(String lrc) {
        List<Line> list = new ArrayList<Line>();
        if (lrc == null || lrc.isEmpty()) return list;
        String[] lines = lrc.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            Matcher m = P.matcher(lines[i]);
            while (m.find()) {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                String msStr = m.group(3);
                long ms = 0;
                if (msStr != null) {
                    if (msStr.length() == 1) ms = Long.parseLong(msStr) * 100;
                    else if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                    else ms = Long.parseLong(msStr.substring(0, 3));
                }
                String text = m.group(4).trim();
                if (!text.isEmpty()) {
                    Line l = new Line();
                    l.time = min * 60000 + sec * 1000 + ms;
                    l.text = text;
                    list.add(l);
                }
            }
        }
        Collections.sort(list, new Comparator<Line>() {
            @Override public int compare(Line a, Line b) {
                return Long.compare(a.time, b.time);
            }
        });
        return list;
    }

    public static int findIndex(List<Line> lines, long ms) {
        if (lines == null || lines.isEmpty()) return -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).time <= ms) return i;
        }
        return 0;
    }
}
