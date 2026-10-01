package com.music.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.music.app.adapter.SongAdapter;
import com.music.app.model.Song;
import com.music.app.util.MusicScanner;
import com.music.app.util.NeteaseApi;
import com.music.app.util.NiceToast;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import com.music.app.util.WarmGreeting;
import com.music.app.widget.AnnouncementDialog;
import com.music.app.widget.FloatingHeartsView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERM = 1001;

    private FloatingHeartsView hearts;
    private RecyclerView rv;
    private SongAdapter adapter;
    private List<Song> localAll = new ArrayList<Song>();
    private List<Song> onlineAll = new ArrayList<Song>();
    private List<Song> showing = new ArrayList<Song>();
    private TextView tvEmpty, tvCount;
    private EditText etSearch;
    private LinearLayout tabLocal, tabOnline;
    private TextView tabLocalText, tabOnlineText;
    private View tabLocalIndicator, tabOnlineIndicator;
    private int currentTab = 0; // 0 本地，1 在线
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
            greeting.setText(WarmGreeting.byTime() + "，" + Prefs.nickname(this));
        if (sub != null) sub.setText(WarmGreeting.subByTime());
        if (quote != null)
            quote.setText("\u201C" + WarmGreeting.dailyQuote(this) + "\u201D");

        hearts = findViewById(R.id.hearts);
        if (hearts != null) hearts.start(4500);

        View appBar = findViewById(R.id.appBar);
        View greetingCard = findViewById(R.id.greetingCard);
        View fab = findViewById(R.id.fab);

        if (appBar != null) {
            appBar.setTranslationY(-180f);
            appBar.setAlpha(0f);
            appBar.animate().translationY(0f).alpha(1f)
                .setDuration(600).setInterpolator(new DecelerateInterpolator()).start();
        }
        if (greetingCard != null) {
            greetingCard.setAlpha(0f);
            greetingCard.setTranslationY(-20f);
            greetingCard.animate().alpha(1f).translationY(0f)
                .setStartDelay(200).setDuration(500).start();
        }
        if (fab != null) {
            fab.setScaleX(0f);
            fab.setScaleY(0f);
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
                    if (showing.isEmpty()) {
                        NiceToast.show(MainActivity.this, "还没有歌曲哦");
                    } else {
                        openPlayer(0);
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

        // Tab 切换
        tabLocal = findViewById(R.id.tabLocal);
        tabOnline = findViewById(R.id.tabOnline);
        tabLocalText = findViewById(R.id.tabLocalText);
        tabOnlineText = findViewById(R.id.tabOnlineText);
        tabLocalIndicator = findViewById(R.id.tabLocalIndicator);
        tabOnlineIndicator = findViewById(R.id.tabOnlineIndicator);

        if (tabLocal != null) tabLocal.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(0); }
        });
        if (tabOnline != null) tabOnline.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(1); }
        });

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
                        if (currentTab == 1) doSearchOnline(etSearch.getText().toString());
                        return true;
                    }
                    return false;
                }
            });
        }

        rv = findViewById(R.id.rvSongs);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvCount = findViewById(R.id.tvCount);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SongAdapter(new SongAdapter.OnItemClick() {
                @Override public void onClick(Song s, int pos) {
                    openPlayer(pos);
                }
            }, new SongAdapter.OnItemLongClick() {
                @Override public void onLongClick(Song s, int pos) {
                    showItemMenu(s);
                }
            });
            rv.setAdapter(adapter);
        }

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

        requestPermAndScan();
    }

    private void switchTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        if (tabLocalText != null) {
            tabLocalText.setTextColor(tab == 0 ? 0xFFFFFFFF : 0xFFD6CCF5);
            tabLocalText.setTextSize(tab == 0 ? 15 : 14);
        }
        if (tabOnlineText != null) {
            tabOnlineText.setTextColor(tab == 1 ? 0xFFFFFFFF : 0xFFD6CCF5);
            tabOnlineText.setTextSize(tab == 1 ? 15 : 14);
        }
        if (tabLocalIndicator != null) {
            tabLocalIndicator.animate().alpha(tab == 0 ? 1f : 0f).setDuration(220).start();
        }
        if (tabOnlineIndicator != null) {
            tabOnlineIndicator.animate().alpha(tab == 1 ? 1f : 0f).setDuration(220).start();
        }
        if (tab == 0) {
            showing = new ArrayList<Song>(localAll);
            if (etSearch != null) etSearch.setHint("搜索本地音乐");
        } else {
            showing = new ArrayList<Song>(onlineAll);
            if (etSearch != null) etSearch.setHint("搜索网易云音乐");
        }
        refreshList(true);
    }

    private void showItemMenu(final Song s) {
        if (!s.isOnline) {
            NiceToast.show(this, "本地音乐：" + s.title);
            return;
        }
        final android.app.AlertDialog.Builder b =
            new android.app.AlertDialog.Builder(this);
        b.setTitle(s.title + "\n" + s.artist);
        String[] items = {"下载音乐", "下载歌词", "播放"};
        b.setItems(items, new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                if (which == 0) downloadSong(s);
                else if (which == 1) downloadLyrics(s);
                else openPlayer(showing.indexOf(s));
            }
        });
        b.show();
    }

    private void downloadSong(final Song s) {
        NiceToast.show(this, "开始下载：" + s.title);
        NeteaseApi.getPlayUrl(s.id, new NeteaseApi.OnUrl() {
            @Override public void onResult(String url) {
                if (url == null || url.isEmpty()) {
                    NiceToast.show(MainActivity.this, "无法获取下载地址");
                    return;
                }
                String safeName = s.title.replaceAll("[\\\\/:*?\"<>|]", "_")
                    + " - " + s.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
                com.music.app.util.DownloadUtil.download(url, safeName + ".mp3",
                    new com.music.app.util.DownloadUtil.Callback() {
                        @Override public void onDone(boolean ok, String path) {
                            NiceToast.show(MainActivity.this,
                                ok ? "✓ 已下载到 /sogou/" : "下载失败：" + path);
                        }
                    });
            }
        });
    }

    private void downloadLyrics(final Song s) {
        NiceToast.show(this, "正在获取歌词…");
        NeteaseApi.getLyrics(s.id, new NeteaseApi.OnLyrics() {
            @Override public void onResult(String lrc) {
                if (lrc == null || lrc.isEmpty()) {
                    NiceToast.show(MainActivity.this, "没有获取到歌词");
                    return;
                }
                String safeName = s.title.replaceAll("[\\\\/:*?\"<>|]", "_")
                    + " - " + s.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
                com.music.app.util.DownloadUtil.saveLyrics(lrc, safeName + ".lrc",
                    new com.music.app.util.DownloadUtil.Callback() {
                        @Override public void onDone(boolean ok, String path) {
                            NiceToast.show(MainActivity.this,
                                ok ? "✓ 歌词已保存到 /sogou/" : "保存失败");
                        }
                    });
            }
        });
    }

    private void openPlayer(int idx) {
        if (showing.isEmpty()) { NiceToast.show(this, "列表为空"); return; }
        if (idx < 0 || idx >= showing.size()) idx = 0;
        PlayerActivity.queue = new ArrayList<Song>(showing);
        Intent it = new Intent(this, PlayerActivity.class);
        it.putExtra("index", idx);
        startActivity(it);
        overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);
    }

    private void doSearchOnline(final String kw) {
        if (kw == null || kw.trim().isEmpty()) {
            NiceToast.show(this, "请输入关键词");
            return;
        }
        if (tvEmpty != null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("正在搜索…");
        }
        if (tvCount != null) tvCount.setText("搜索中…");
        NeteaseApi.search(kw, new NeteaseApi.OnSearch() {
            @Override public void onResult(List<Song> songs) {
                onlineAll = songs;
                showing = new ArrayList<Song>(songs);
                refreshList(true);
                if (songs.isEmpty()) {
                    NiceToast.show(MainActivity.this, "没有搜索到结果");
                } else {
                    NiceToast.show(MainActivity.this, "找到 " + songs.size() + " 首");
                }
            }
        });
    }

    private void requestPermAndScan() {
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
                    tvEmpty.setText("没有权限扫描本地音乐\n在设置中开启存储权限即可");
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
                        localAll = songs;
                        if (currentTab == 0) {
                            showing = new ArrayList<Song>(songs);
                            refreshList(true);
                        }
                    }
                });
            }
        }).start();
    }

    private void filter(String q) {
        List<Song> src = currentTab == 0 ? localAll : onlineAll;
        if (q == null || q.trim().isEmpty()) {
            showing = new ArrayList<Song>(src);
        } else {
            String key = q.toLowerCase().trim();
            showing = new ArrayList<Song>();
            for (int i = 0; i < src.size(); i++) {
                Song s = src.get(i);
                if (s.title.toLowerCase().contains(key)
                    || s.artist.toLowerCase().contains(key)
                    || s.album.toLowerCase().contains(key)) {
                    showing.add(s);
                }
            }
        }
        refreshList(false);
        if (currentTab == 1 && !showing.isEmpty()) {
            // 在线 Tab 边输入边搜索效果
        }
    }

    private void refreshList(boolean animate) {
        if (adapter != null) adapter.setData(showing);
        if (tvCount != null) tvCount.setText("共 " + showing.size() + " 首");
        if (tvEmpty != null) {
            if (showing.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(currentTab == 0
                    ? "还没有本地音乐哦\n可以从在线 Tab 搜索并下载 ♡"
                    : "在上方搜索框输入关键词\n从网易云搜索音乐 ♪");
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        }
    }

    private String getText() {
        return "欢迎回来，亲爱的你 🌸\n\n"
            + "【写给你的第一句话】\n"
            + "很高兴在音乐的世界里遇见你。希望这款小小的应用，能陪你度过每一个或忙碌、"
            + "或闲暇、或开心、或有点小失落的时刻。天天开心，事事顺遂。\n\n"
            + "【v10.0 新功能】\n"
            + "• 网易云官方接口搜索 —— 输入关键词即刻搜索全网音乐\n"
            + "• 在线播放 —— 点击即播，支持边下边播\n"
            + "• 歌词同步 —— 自动获取 LRC 歌词，逐行滚动高亮\n"
            + "• 一键下载 —— 长按歌曲可下载音乐到 /sogou/\n"
            + "• 歌词下载 —— 长按可下载 LRC 歌词文件\n"
            + "• 本地扫描 —— 自动扫描设备中的音乐\n"
            + "• 液态玻璃 UI —— 20 项玻璃拟态细节\n"
            + "• 丝滑过渡 —— 页面切换、列表滚动、点击反馈全面调优\n\n"
            + "【使用小贴士】\n"
            + "1. 顶部两个 Tab 切换本地音乐 / 在线搜索\n"
            + "2. 在在线 Tab 输入关键词，回车搜索\n"
            + "3. 长按歌曲弹出菜单：下载音乐 / 下载歌词 / 播放\n"
            + "4. 下载的音乐和歌词都在 /storage/emulated/0/sogou/\n"
            + "5. 播放页会显示歌词，随时间逐行滚动\n\n"
            + "【使用须知】\n"
            + "1. 本软件仅供学习交流，不得用于商业用途。\n"
            + "2. 所有音乐资源版权归原作者及唱片公司所有。\n"
            + "3. 使用本软件即表示你已知晓并同意上述条款。\n\n"
            + "【版本信息】\n"
            + "当前版本：10.0.0\n"
            + "适配系统：Android 5.0 及以上\n\n"
            + "生活或许偶尔会有不易，但请你相信，一切都会好起来的。\n"
            + "记得多喝水，照顾好自己。\n"
            + "天天开心，岁岁平安。\n\n"
            + "—— MUSIC·Pro 敬上 ♡";
    }
}
