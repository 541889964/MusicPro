package com.music.app.util;
import java.util.Calendar;
public class WarmGreeting {
    public static String byTime() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 9) return "早上好呀 ☀️";
        if (h >= 9 && h < 12) return "上午好呀 🌸";
        if (h >= 12 && h < 14) return "中午好，吃饭了吗 🍱";
        if (h >= 14 && h < 18) return "下午好，喝口水吧 ☕";
        if (h >= 18 && h < 22) return "晚上好呀 🌆";
        return "夜深了，早点休息 🌙";
    }
    public static String bySubTime() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 12) return "新的一天，从一首好歌开始";
        if (h >= 12 && h < 18) return "让音乐陪你度过下午";
        if (h >= 18 && h < 22) return "夜晚的歌，格外温柔";
        return "夜深了，放首轻音乐吧";
    }
    public static String dailyQuote() {
        String[] q = {"今天也要开心呀","你笑起来真好看","慢慢来，一切都来得及",
            "记得喝水，照顾好自己","你值得被温柔以待","今天的你，也很棒",
            "累了就休息，没有人会怪你","生活明朗，万物可爱","愿你成为自己的太阳",
            "慢慢走，沿途都是风景","星光不问赶路人","愿你被这世界温柔以待",
            "天天开心，岁岁平安","你的努力，时间会看见","一杯热茶，一首好歌"};
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        return q[day % q.length];
    }
}
