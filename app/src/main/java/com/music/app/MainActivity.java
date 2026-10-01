package com.music.app;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.util.NiceToast;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import com.music.app.util.WarmGreeting;
import com.music.app.widget.AnnouncementDialog;
import com.music.app.widget.FloatingHeartsView;

public class MainActivity extends AppCompatActivity {

    private FloatingHeartsView hearts;
    private long lastClick = 0;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);

        ImageView bgImg = findViewById(R.id.ivWallpaper);
        Bitmap bm = WallpaperHelper.loadUser(this);
        if (bm != null) bgImg.setImageBitmap(bm);

        TextView greeting = findViewById(R.id.tvGreeting);
        TextView sub      = findViewById(R.id.tvGreetingSub);
        greeting.setText(WarmGreeting.byTime() + "，" + Prefs.nickname(this));
        sub.setText(WarmGreeting.subByTime());

        TextView quote = findViewById(R.id.tvDailyQuote);
        quote.setText("\u201C" + WarmGreeting.dailyQuote(this) + "\u201D");

        hearts = findViewById(R.id.hearts);
        hearts.start(4500);

        View appBar = findViewById(R.id.appBar);
        View greetingCard = findViewById(R.id.greetingCard);
        View search = findViewById(R.id.searchBar);
        View tabs   = findViewById(R.id.tabLayout);
        View fab    = findViewById(R.id.fab);

        appBar.setTranslationY(-180f); appBar.setAlpha(0f);
        appBar.animate().translationY(0f).alpha(1f)
            .setDuration(600).setInterpolator(new DecelerateInterpolator()).start();

        greetingCard.setAlpha(0f); greetingCard.setTranslationY(-20f);
        greetingCard.animate().alpha(1f).translationY(0f)
            .setStartDelay(200).setDuration(500)
            .setInterpolator(new DecelerateInterpolator()).start();

        search.setAlpha(0f); search.setScaleX(0.92f);
        search.animate().alpha(1f).scaleX(1f)
            .setStartDelay(350).setDuration(500)
            .setInterpolator(new OvershootInterpolator(1.2f)).start();

        tabs.setAlpha(0f); tabs.setTranslationY(30f);
        tabs.animate().alpha(1f).translationY(0f)
            .setStartDelay(500).setDuration(450).start();

        fab.setScaleX(0f); fab.setScaleY(0f);
        fab.animate().scaleX(1f).scaleY(1f)
            .setStartDelay(700).setDuration(500)
            .setInterpolator(new OvershootInterpolator(1.8f)).start();
        ObjectAnimator.ofFloat(fab, "translationY", 0f, -8f, 0f)
            .setDuration(2600)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setRepeatCount(ObjectAnimator.INFINITE)
            .start();

        View logo = findViewById(R.id.ivAppLogo);
        logo.setOnLongClickListener(v -> {
            NiceToast.love(this, WarmGreeting.randomCheer());
            hearts.start(2800);
            return true;
        });
        logo.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastClick < 400) {
                NiceToast.love(this, WarmGreeting.randomCheer());
                hearts.start(2200);
            }
            lastClick = now;
        });

        if (WarmGreeting.isBirthday(this)) {
            findViewById(R.id.root).postDelayed(() ->
                NiceToast.show(this, "🎂 生日快乐！祝你天天开心"), 1500);
        }
        if (Prefs.openCount(this) == 10) {
            findViewById(R.id.root).postDelayed(() ->
                NiceToast.love(this, "谢谢你第 10 次打开我 ♡"), 2200);
        }

        if (getIntent().getBooleanExtra("show_announcement", false)
                && !Prefs.announcementShown(this)) {
            findViewById(R.id.root).postDelayed(() -> {
                AnnouncementDialog d =
                    new AnnouncementDialog(this, getAnnouncementText());
                d.setOnDismissListener(di -> {
                    Prefs.setAnnouncementShown(this, true);
                    hearts.postDelayed(() -> hearts.start(3200), 300);
                });
                d.show();
            }, 500);
        }
    }

    private String getAnnouncementText() {
        return "欢迎回来，亲爱的你 🌸\n\n"
        + "【写给第一次见面的你】\n"
        + "很高兴在音乐的世界里遇见你。希望这款小小的应用，能陪你度过每一个或忙碌、"
        + "或闲暇、或开心、或有点小失落的时刻。音乐是时间的艺术，也是情感的载体。"
        + "当你打开它，希望所有的烦恼都能被旋律轻轻带走。记得：无论今天过得怎么样，"
        + "你已经做得很好了。天天开心，事事顺遂。\n\n"
        + "【关于 MUSIC·Pro】\n"
        + "这是一款集在线搜索、无损下载、本地管理、专业播放、音频工坊于一体的全能音乐"
        + "播放器。依托网易云官方接口，海量正版曲库随你检索；内置 FFmpeg 音频引擎，可在"
        + "手机上完成裁剪、变速、升降调、格式转换等专业操作；搭载 TensorFlow Lite 智能分类"
        + "模型，自动识别节奏与情绪，为你生成个性化歌单。本地音乐模块支持 MP3、FLAC、OGG、"
        + "WAV、M4A、AAC、APE、WMA 等 20+ 音频格式，让你无需切换应用即可畅听全部音乐。\n\n"
        + "【核心功能】\n"
        + "1. 在线搜索 —— 关键词模糊匹配，支持歌名、歌手、专辑、歌词内容搜索。\n"
        + "2. 无损下载 —— 支持 128k/320k/FLAC 三档音质，断点续传、并发加速。\n"
        + "3. 歌词同步 —— 下载 LRC 格式歌词，含原文、翻译、罗马音。\n"
        + "4. 本地扫描 —— MediaStore 高速扫描，四种分类浏览。\n"
        + "5. 音频工坊 —— 可视化裁剪、变速、升降调、批量格式转换。\n"
        + "6. 10 段均衡器 —— 12 套预设，支持自定义曲线。\n"
        + "7. 实时频谱 —— 5 种可视化样式随音乐律动。\n"
        + "8. 智能分类 —— 自动生成「运动」「放松」「学习」等场景歌单。\n"
        + "9. 睡眠定时 —— 15/30/60/90 分钟或「播放完当前歌曲」自动暂停。\n"
        + "10. 多主题 —— 8 套精美皮肤 + Material You 动态取色。\n\n"
        + "【暖心细节】\n"
        + "• 根据时间自动问候（早/午/晚/深夜）\n"
        + "• 每日一句温暖话语（120 条轮播，每天不同）\n"
        + "• 进入时彩色爱心漂浮动画\n"
        + "• 开启动画含粒子效果\n"
        + "• 长按或双击顶部 Logo 有惊喜彩蛋\n"
        + "• 生日当天自动祝福\n"
        + "• 第 10 次打开会有特别感谢\n"
        + "• 播放时背景呼吸光晕\n"
        + "• 彩色圆角 Toast 提示\n"
        + "• 玻璃拟态问候卡片\n\n"
        + "【使用须知】\n"
        + "1. 本软件仅供学习交流与技术研究，不得用于商业用途。\n"
        + "2. 所有音乐资源版权归原作者及唱片公司所有，请在下载后 24 小时内删除。\n"
        + "3. 请勿将本软件用于任何违反当地法律法规的场景。\n"
        + "4. 使用本软件即表示你已知晓并同意上述条款。\n\n"
        + "【隐私承诺】\n"
        + "我们尊重并保护你的隐私。本软件不会收集任何个人信息、设备标识、位置数据或"
        + "使用习惯。本地音乐扫描仅读取音频文件基础元数据，不上传任何内容到服务器。"
        + "所有下载记录、搜索历史、播放统计均存储于本地设备，可随时一键清除。\n\n"
        + "【版本信息】\n"
        + "当前版本：6.0.0 极致版\n"
        + "适配系统：Android 5.0 及以上\n"
        + "打包体积：约 120 MB（含完整解码器、FFmpeg 引擎、AI 模型、多语言、"
        + "10 张高清壁纸、4 段助眠白噪音与字体）\n\n"
        + "【最后想对你说】\n"
        + "写完这段文字的时候，窗外的风正好吹过来。我想告诉你：生活或许偶尔会有不易，"
        + "但请你相信，一切都会好起来的。累了就休息，饿了就吃饭，难过的时候就听首歌，"
        + "开心的时候就大声笑。你值得被这个世界温柔以待。\n\n"
        + "记得多喝水，照顾好自己。\n"
        + "愿你三冬暖，愿你春不寒，愿你天黑有灯，下雨有伞。\n"
        + "天天开心，岁岁平安。\n\n"
        + "—— MUSIC·Pro 敬上 ♡";
    }
}
