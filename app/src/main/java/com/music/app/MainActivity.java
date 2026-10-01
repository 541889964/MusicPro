package com.music.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.animation.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.*;
import com.music.app.adapter.SongAdapter;
import com.music.app.model.Song;
import com.music.app.util.*;
import com.music.app.widget.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERM = 1001;

    private FloatingHeartsView hearts;
    private RecyclerView rv;
    private SongAdapter adapter;
    private List<Song> allSongs = new ArrayList<>();
    private List<Song> filtered = new ArrayList<>();
    private TextView tvEmpty, tvCount;
    private EditText etSearch;
    private long lastClick = 0;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);

        ImageView bgImg = findViewById(R.id.ivWallpaper);
        Bitmap bm = WallpaperHelper.loadUser(this);
        if (bm != null && bgImg != null) bgImg.setImageBitmap(bm);

        TextView greeting = findViewById(R.id.tvGreeting);
        TextView sub = findViewById(R.id.tvGreetingSub);
        TextView quote = findViewById(R.id.tvDailyQuote);
        if (greeting != null)
            greeting.setText(WarmGreeting.byTime() + "，" + Prefs.nick(this));
        if (sub != null)
            sub.setText(WarmGreeting.subByTime());
        if (quote != null)
            quote.setText("\u201C" + WarmGreeting.dailyQuote(this) + "\u201D");

        hearts = findViewById(R.id.hearts);
        if (hearts != null) hearts.start(4500);

        // 顶栏滑入
        View appBar = findViewById(R.id.appBar);
        View greetingCard = findViewById(R.id.greetingCard);
        View searchBar = findViewById(R.id.etSearch);
        View tabs = findViewById(R.id.tabLayout);
        View fab = findViewById(R.id.fab);

        if (appBar != null) {
            appBar.setTranslationY(-180f); appBar.setAlpha(0f);
            appBar.animate().translationY(0f).alpha(1f).setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
        if (greetingCard != null) {
            greetingCard.setAlpha(0f); greetingCard.setTranslationY(-20f);
            greetingCard.animate().alpha(1f).translationY(0f)
                .setStartDelay(200).setDuration(500).start();
        }
        if (searchBar != null) {
            searchBar.setAlpha(0f); searchBar.setScaleX(0.92f);
            searchBar.animate().alpha(1f).scaleX(1f)
                .setStartDelay(350).setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.2f)).start();
        }
        if (tabs != null) {
            tabs.setAlpha(0f); tabs.setTranslationY(30f);
            tabs.animate().alpha(1f).translationY(0f)
                .setStartDelay(500).setDuration(450).start();
        }
        if (fab != null) {
            fab.setScaleX(0f); fab.setScaleY(0f);
            fab.animate().scaleX(1f).scaleY(1f)
                .setStartDelay(700).setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.8f)).start();
            ObjectAnimator bob = ObjectAnimator.ofFloat(fab, "translationY", 0f, -8f, 0f);
            bob.setDuration(2600);
            bob.setInterpolator(new AccelerateDecelerateInterpolator());
            bob.setRepeatCount(ObjectAnimator.INFINITE);
            bob.start();
            fab.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (allSongs.isEmpty()) {
                        NiceToast.show(MainActivity.this, "还没有扫描到音乐哦");
                    } else {
                        startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                    }
                }
            });
        }

        View logo = findViewById(R.id.ivAppLogo);
        if (logo != null) {
            logo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    NiceToast.love(MainActivity.this, WarmGreeting.randomCheer());
                    if (hearts != null) hearts.start(2800);
                    return true;
                }
            });
            logo.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    long now = System.currentTimeMillis();
                    if (now - lastClick < 400) {
                        NiceToast.love(MainActivity.this, WarmGreeting.randomCheer());
                        if (hearts != null) hearts.start(2200);
                    }
                    lastClick = now;
                }
            });
        }

        // 搜索框
        etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    filter(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent e) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        hideKeyboard();
                        return true;
                    }
                    return false;
                }
            });
        }

        // 列表
        rv = findViewById(R.id.rvSongs);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvCount = findViewById(R.id.tvCount);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SongAdapter(new SongAdapter.OnItemClick() {
                @Override public void onClick(Song s, int pos) {
                    Intent it = new Intent(MainActivity.this, PlayerActivity.class);
                    it.putExtra("index", pos);
                    startActivity(it);
                }
            });
            rv.setAdapter(adapter);
        }

        // 公告
        if (getIntent().getBooleanExtra("show_announcement", false)
                && !Prefs.annShown(this)) {
            ui.postDelayed(new Runnable() {
                @Override public void run() {
                    final AnnouncementDialog d =
                        new AnnouncementDialog(MainActivity.this, getText());
                    d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                        @Override public void onDismiss(android.content.DialogInterface di) {
                            Prefs.setAnnShown(MainActivity.this, true);
                            if (hearts != null) {
                                hearts.postDelayed(new Runnable() {
                                    @Override public void run() { hearts.start(3200); }
                                }, 300);
                            }
                        }
                    });
                    d.show();
                }
            }, 500);
        }

        if (Prefs.openCount(this) == 10) {
            ui.postDelayed(new Runnable() {
                @Override public void run() {
                    NiceToast.love(MainActivity.this, "谢谢你第 10 次打开我 ♡");
                }
            }, 2200);
        }

        // 权限 → 扫描
        requestPermissionsAndScan();
    }

    private void requestPermissionsAndScan() {
        String perm;
        if (Build.VERSION.SDK_INT >= 33) perm = "android.permission.READ_MEDIA_AUDIO";
        else perm = Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, perm)
                == PackageManager.PERMISSION_GRANTED) {
            scanAsync();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{perm}, REQ_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == REQ_PERM) {
            if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
                scanAsync();
            } else {
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("没有权限扫描本地音乐\n请在设置中开启存储权限");
                }
            }
        }
    }

    private void scanAsync() {
        new Thread(new Runnable() {
            @Override public void run() {
                final List<Song> songs = MusicScanner.scan(MainActivity.this);
                ui.post(new Runnable() {
                    @Override public void run() {
                        allSongs = songs;
                        filtered = new ArrayList<>(songs);
                        if (adapter != null) adapter.setData(filtered);
                        if (tvCount != null)
                            tvCount.setText("共 " + songs.size() + " 首");
                        if (tvEmpty != null) {
                            if (songs.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                tvEmpty.setText("还没有扫描到本地音乐\n去下载一些歌曲再来吧 ♡");
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void filter(String q) {
        if (q == null || q.trim().isEmpty()) {
            filtered = new ArrayList<>(allSongs);
        } else {
            String key = q.toLowerCase().trim();
            filtered = new ArrayList<>();
            for (Song s : allSongs) {
                if (s.title.toLowerCase().contains(key)
                    || s.artist.toLowerCase().contains(key)
                    || s.album.toLowerCase().contains(key)) {
                    filtered.add(s);
                }
            }
        }
        if (adapter != null) adapter.setData(filtered);
        if (tvCount != null)
            tvCount.setText("共 " + filtered.size() + " 首");
        if (tvEmpty != null) {
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            if (filtered.isEmpty()) tvEmpty.setText("没有找到匹配的歌曲 ♡");
        }
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private String getText() {
        return "欢迎回来，亲爱的你 🌸\n\n"
            + "【写给你的第一句话】\n"
            + "很高兴在音乐的世界里遇见你。希望这款小小的应用，能陪你度过每一个或忙碌、"
            + "或闲暇、或开心、或有点小失落的时刻。当你打开它，希望所有的烦恼都能被旋律轻轻带走。"
            + "记得：无论今天过得怎么样，你已经做得很好了。天天开心，事事顺遂。\n\n"
            + "【关于 MUSIC·Pro】\n"
            + "这是一款为音乐爱好者准备的随身播放器。自动扫描设备中所有音频文件，"
            + "支持 MP3、FLAC、OGG、WAV、M4A、AAC、APE、WMA 等 20+ 音频格式，"
            + "让你无需切换应用即可畅听全部音乐。\n\n"
            + "【核心功能】\n"
            + "1. 本地扫描 —— 一键扫描设备中的所有音频文件\n"
            + "2. 实时搜索 —— 输入关键词即刻过滤歌名/歌手/专辑\n"
            + "3. 专业播放 —— ExoPlayer 引擎，支持后台播放与通知栏控制\n"
            + "4. 迷你播放器 —— 底部常驻，随时切换\n"
            + "5. 展开播放页 —— 全屏专辑封面 + 进度条 + 控制按钮\n"
            + "6. 播放队列 —— 自动按列表顺序播放\n"
            + "7. 多语言 —— 支持 13 种语言界面\n"
            + "8. 深色模式 —— 跟随系统自动切换\n\n"
            + "【暖心细节】\n"
            + "• 根据时间自动问候（早/午/晚/深夜）\n"
            + "• 每日一句温暖话语（80 条轮播，每天不同）\n"
            + "• 进入时彩色爱心漂浮动画\n"
            + "• 开启动画含粒子效果\n"
            + "• 长按或双击顶部 Logo 有惊喜彩蛋\n"
            + "• 第 10 次打开会有特别感谢\n"
            + "• 彩色圆角 Toast 提示\n"
            + "• 玻璃拟态问候卡片\n\n"
            + "【使用须知】\n"
            + "1. 本软件仅供学习交流与技术研究，不得用于商业用途。\n"
            + "2. 所有音乐资源版权归原作者及唱片公司所有。\n"
            + "3. 使用本软件即表示你已知晓并同意上述条款。\n\n"
            + "【隐私承诺】\n"
            + "我们尊重并保护你的隐私。本软件不会收集任何个人信息、设备标识、位置数据。"
            + "本地音乐扫描仅读取音频文件基础元数据，不上传任何内容到服务器。\n\n"
            + "【版本信息】\n"
            + "当前版本：8.0.0 完整版\n"
            + "适配系统：Android 5.0 及以上\n\n"
            + "【最后想对你说】\n"
            + "生活或许偶尔会有不易，但请你相信，一切都会好起来的。累了就休息，饿了就吃饭，"
            + "难过的时候就听首歌，开心的时候就大声笑。你值得被这个世界温柔以待。\n\n"
            + "记得多喝水，照顾好自己。\n"
            + "天天开心，岁岁平安。\n\n"
            + "—— MUSIC·Pro 敬上 ♡";
    }
}
