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
        public Line(long t, String x) { time = t; text = x; }
    }

    private static final Pattern P = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?\\]");

    public static List<Line> parse(String lrc) {
        List<Line> out = new ArrayList<Line>();
        if (lrc == null || lrc.isEmpty()) return out;
        try {
            String[] lines = lrc.split("\n");
            for (String raw : lines) {
                if (raw == null) continue;
                Matcher m = P.matcher(raw);
                List<Long> times = new ArrayList<Long>();
                int lastEnd = 0;
                while (m.find()) {
                    long mm = parseLong(m.group(1));
                    long ss = parseLong(m.group(2));
                    long ms = 0;
                    if (m.group(3) != null) {
                        String g = m.group(3);
                        if (g.length() == 2) ms = parseLong(g) * 10;
                        else ms = parseLong(g);
                    }
                    times.add(mm * 60000 + ss * 1000 + ms);
                    lastEnd = m.end();
                }
                String text = raw.substring(lastEnd).trim();
                if (text.isEmpty()) continue;
                for (Long t : times) out.add(new Line(t, text));
            }
            Collections.sort(out, new Comparator<Line>() {
                @Override public int compare(Line a, Line b) {
                    return a.time < b.time ? -1 : (a.time > b.time ? 1 : 0);
                }
            });
        } catch (Throwable ignored) {}
        return out;
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Throwable t) { return 0; }
    }

    public static int findIndex(List<Line> lines, long posMs) {
        if (lines == null || lines.isEmpty()) return -1;
        int lo = 0, hi = lines.size() - 1, ans = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).time <= posMs) { ans = mid; lo = mid + 1; }
            else hi = mid - 1;
        }
        return ans;
    }
}
