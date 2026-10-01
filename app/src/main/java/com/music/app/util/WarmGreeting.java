package com.music.app.util;
import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WarmGreeting {
    public static String byTime() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5  && h < 9)  return "早上好呀 ☀️";
        if (h >= 9  && h < 12) return "上午好呀 🌸";
        if (h >= 12 && h < 14) return "中午好，吃饭了吗 🍱";
        if (h >= 14 && h < 18) return "下午好，喝口水吧 ☕";
        if (h >= 18 && h < 22) return "晚上好呀 🌆";
        return "夜深了，早点休息 🌙";
    }
    public static String subByTime() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5  && h < 12) return "新的一天，从一首好歌开始";
        if (h >= 12 && h < 18) return "让音乐陪你度过下午";
        if (h >= 18 && h < 22) return "夜晚的歌，格外温柔";
        return "夜深了，放首轻音乐吧";
    }
    public static String dailyQuote(Context c) {
        List<String> all = readAll(c);
        if (all.isEmpty()) return "今天也要开心呀";
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        return all.get(day % all.size());
    }
    public static String randomCheer() {
        String[] a = {
            "天天开心","你最棒了","一切都会好的","记得对自己好一点",
            "今天也要元气满满哦","你笑起来真好看","喝口水休息一下吧","星星也在为你闪烁",
            "愿你所想皆如愿","慢慢来，不着急","抱抱你","你值得被爱","好梦即将来临",
            "生活甜甜的","你是被爱的呀","谢谢你坚持到现在"
        };
        return a[(int)(Math.random()*a.length)];
    }
    private static List<String> readAll(Context c) {
        List<String> o = new ArrayList<>();
        try (InputStream in = c.getAssets().open("quotes/daily.txt");
             BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) o.add(line);
            }
        } catch (Throwable ignored) {}
        return o;
    }
}
