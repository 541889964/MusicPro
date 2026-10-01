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
    private ImageView bgWallpaper;
    private int currentTab = 0;
    private long lastClick = 0;
    private int wpIndex = 0;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable wpRunnable;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(R.layout.activity_main);
            bgWallpaper = findViewById(R.id.ivWallpaper);
            applyWallpaper();

            TextView greeting = findViewById(R.id.tvGreeting);
            TextView sub = findViewById(R.id.tvGreetingSub);
            TextView quote = findViewById(R.id.tvDailyQuote);
            if (greeting != null) greeting.setText(WarmGreeting.byTime() + "，" + Prefs.nickname(this));
            if (sub != null) sub.setText(WarmGreeting.byTime());
            if (quote != null) quote.setText("\u201C" + WarmGreeting.dailyQuote(this) + "\u201D");

            hearts = findViewById(R.id.hearts);
            if (hearts != null) hearts.start(4500);

            View appBar = findViewById(R.id.appBar);
            View greetingCard = findViewById(R.id.greetingCard);
            View fab = findViewById(R.id.fab);
            if (appBar != null) {
                appBar.setTranslationY(-180f); appBar.setAlpha(0f);
                appBar.animate().translationY(0f).alpha(1f).setDuration(600)
                    .setInterpolator(new DecelerateInterpolator()).start();
            }
            if (greetingCard != null) {
                greetingCard.setAlpha(0f); greetingCard.setTranslationY(-20f);
                greetingCard.animate().alpha(1f).translationY(0f).setStartDelay(200).setDuration(500).start();
            }
            if (fab != null) {
                fab.setScaleX(0f); fab.setScaleY(0f);
                fab.animate().scaleX(1f).scaleY(1f).setStartDelay(700).setDuration(500)
                    .setInterpolator(new OvershootInterpolator(1.8f)).start();
                ObjectAnimator bob = ObjectAnimator.ofFloat(fab, "translationY", 0f, -8f, 0f);
                bob.setDuration(2600);
                bob.setInterpolator(new AccelerateDecelerateInterpolator());
                bob.setRepeatCount(ObjectAnimator.INFINITE);
                bob.start();
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (showing.isEmpty()) NiceToast.show(MainActivity.this, "还没有歌曲");
                        else openPlayer(0);
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

            // ★ 唯一按钮：设置
            View btnSettings = findViewById(R.id.btnSettings);
            if (btnSettings != null) {
                btnSettings.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try {
                            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        } catch (Throwable t) {
                            NiceToast.show(MainActivity.this, "打开设置失败: " + t.getMessage());
                        }
                    }
                });
            }

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
                    @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
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
                rv.setHasFixedSize(true);
                rv.setItemAnimator(null);
                rv.setLayoutManager(new LinearLayoutManager(this));
                adapter = new SongAdapter(new SongAdapter.OnItemClick() {
                    @Override public void onClick(Song s, int pos) { openPlayer(pos); }
                }, new SongAdapter.OnItemLongClick() {
                    @Override public void onLongClick(Song s, int pos) { showItemMenu(s); }
                });
                rv.setAdapter(adapter);
            }

            if (getIntent().getBooleanExtra("show_announcement", false) && !Prefs.annShown(this)) {
                ui.postDelayed(new Runnable() {
                    @Override public void run() {
                        try {
                            final AnnouncementDialog d = new AnnouncementDialog(MainActivity.this, getText());
                            d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                                @Override public void onDismiss(android.content.DialogInterface di) {
                                    Prefs.setAnnShown(MainActivity.this, true);
                                }
                            });
                            d.show();
                        } catch (Throwable ignored) {}
                    }
                }, 500);
            }
            requestPermAndScan();
        } catch (Throwable t) {
            android.util.Log.e("Music", "onCreate fail", t);
            NiceToast.show(this, "初始化失败: " + t.getMessage());
        }
    }

    private void applyWallpaper() {
        if (bgWallpaper == null) return;
        try {
            String name = Prefs.wallpaper(this);
            Bitmap bm;
            if (name != null && !name.isEmpty()) bm = WallpaperHelper.load(this, name);
            else bm = WallpaperHelper.at(this, wpIndex);
            if (bm != null) {
                bgWallpaper.setAlpha(0f);
                bgWallpaper.setImageBitmap(bm);
                bgWallpaper.animate().alpha(1f).setDuration(800).start();
            }
        } catch (Throwable ignored) {}
    }

    @Override protected void onResume() {
        super.onResume();
        try {
            if (bgWallpaper != null) {
                String name = Prefs.wallpaper(this);
                if (name != null && !name.isEmpty()) {
                    Bitmap bm = WallpaperHelper.load(this, name);
                    if (bm != null) {
                        bgWallpaper.setAlpha(0f);
                        bgWallpaper.setImageBitmap(bm);
                        bgWallpaper.animate().alpha(1f).setDuration(600).start();
                    }
                }
            }
        } catch (Throwable ignored) {}
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
        if (tabLocalIndicator != null) tabLocalIndicator.animate().alpha(tab == 0 ? 1f : 0f).setDuration(220).start();
        if (tabOnlineIndicator != null) tabOnlineIndicator.animate().alpha(tab == 1 ? 1f : 0f).setDuration(220).start();
        if (tab == 0) {
            showing = new ArrayList<Song>(localAll);
            if (etSearch != null) etSearch.setHint("搜索本地音乐");
        } else {
            showing = new ArrayList<Song>(onlineAll);
            if (etSearch != null) etSearch.setHint("搜索网易云音乐");
        }
        refreshList();
    }

    private void showItemMenu(final Song s) {
        if (!s.isOnline) { NiceToast.show(this, "本地音乐：" + s.title); return; }
        final android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
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
                if (url == null || url.isEmpty()) { NiceToast.show(MainActivity.this, "无法获取地址"); return; }
                String safeName = s.title.replaceAll("[\\\\/:*?\"<>|]", "_") + " - " + s.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
                com.music.app.util.DownloadUtil.download(url, safeName + ".mp3",
                    new com.music.app.util.DownloadUtil.Callback() {
                        @Override public void onDone(boolean ok, String path) {
                            NiceToast.show(MainActivity.this, ok ? "✓ 已下载" : "下载失败");
                        }
                    });
            }
        });
    }

    private void downloadLyrics(final Song s) {
        NiceToast.show(this, "正在获取歌词…");
        NeteaseApi.getLyrics(s.id, new NeteaseApi.OnLyrics() {
            @Override public void onResult(String lrc) {
                if (lrc == null || lrc.isEmpty()) { NiceToast.show(MainActivity.this, "没有歌词"); return; }
                String safeName = s.title.replaceAll("[\\\\/:*?\"<>|]", "_") + " - " + s.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
                com.music.app.util.DownloadUtil.saveLyrics(lrc, safeName + ".lrc",
                    new com.music.app.util.DownloadUtil.Callback() {
                        @Override public void onDone(boolean ok, String path) {
                            NiceToast.show(MainActivity.this, ok ? "✓ 歌词已保存" : "保存失败");
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void doSearchOnline(final String kw) {
        if (kw == null || kw.trim().isEmpty()) { NiceToast.show(this, "请输入关键词"); return; }
        if (tvEmpty != null) { tvEmpty.setVisibility(View.VISIBLE); tvEmpty.setText("正在搜索…"); }
        if (tvCount != null) tvCount.setText("搜索中…");
        NeteaseApi.search(kw, new NeteaseApi.OnSearch() {
            @Override public void onResult(List<Song> songs) {
                onlineAll = songs;
                showing = new ArrayList<Song>(songs);
                refreshList();
                if (songs.isEmpty()) NiceToast.show(MainActivity.this, "没有结果");
                else NiceToast.show(MainActivity.this, "找到 " + songs.size() + " 首");
            }
        });
    }

    private void requestPermAndScan() {
        String perm;
        if (Build.VERSION.SDK_INT >= 33) perm = "android.permission.READ_MEDIA_AUDIO";
        else perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) scanAsync();
        else ActivityCompat.requestPermissions(this, new String[]{perm}, REQ_PERM);
    }

    @Override public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == REQ_PERM) {
            if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) scanAsync();
            else if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("没有权限扫描本地音乐");
            }
        }
    }

    private void scanAsync() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final List<Song> songs = MusicScanner.scan(MainActivity.this);
                    ui.post(new Runnable() {
                        @Override public void run() {
                            localAll = songs;
                            if (currentTab == 0) {
                                showing = new ArrayList<Song>(songs);
                                refreshList();
                            }
                        }
                    });
                } catch (Throwable ignored) {}
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
                    || s.album.toLowerCase().contains(key)) showing.add(s);
            }
        }
        refreshList();
    }

    private void refreshList() {
        if (adapter != null) adapter.setData(showing);
        if (tvCount != null) tvCount.setText("共 " + showing.size() + " 首");
        if (tvEmpty != null) {
            if (showing.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(currentTab == 0
                    ? "还没有本地音乐哦"
                    : "在上方搜索框输入关键词");
            } else tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override protected void onDestroy() {
        if (wpRunnable != null) ui.removeCallbacks(wpRunnable);
        super.onDestroy();
    }

    private String getText() {
        return "欢迎使用 拾音\n\n" +
            "很高兴在音乐的世界里遇见你。\n" +
            "天天开心，事事顺遂。\n\n" +
            "—— 拾音 敬上 ♡";
    }
}
