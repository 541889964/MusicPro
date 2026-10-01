package com.music.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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
import com.music.app.service.IslandService;
import com.music.app.util.MusicScanner;
import com.music.app.util.NiceToast;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import com.music.app.util.WarmGreeting;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ = 1001;
    private RecyclerView rv;
    private SongAdapter adapter;
    private List<Song> all = new ArrayList<Song>();
    private List<Song> showing = new ArrayList<Song>();
    private TextView tvEmpty, tvCount;
    private EditText etSearch;
    private View miniPlayer;
    private ImageView miniCover;
    private TextView miniTitle, miniArtist, miniPlay;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(R.layout.activity_main);
            Prefs.incOpen(this);
            ImageView bg = findViewById(R.id.ivWallpaper);
            Bitmap bm = WallpaperHelper.loadCurrent(this);
            if (bm != null) bg.setImageBitmap(bm);

            TextView greeting = findViewById(R.id.tvGreeting);
            TextView sub = findViewById(R.id.tvSubGreeting);
            TextView quote = findViewById(R.id.tvDailyQuote);
            if (greeting != null) greeting.setText(WarmGreeting.byTime());
            if (sub != null) sub.setText(WarmGreeting.dailyQuote());
            if (quote != null) quote.setText("♡ " + WarmGreeting.dailyQuote());

            findViewById(R.id.btnLocal).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (etSearch != null) { etSearch.setText(""); etSearch.clearFocus(); }
                }
            });
            findViewById(R.id.btnOnline).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    NiceToast.show(MainActivity.this, "在线搜索开发中");
                }
            });
            findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            });

            etSearch = findViewById(R.id.etSearch);
            if (etSearch != null) etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });

            rv = findViewById(R.id.rvSongs);
            tvEmpty = findViewById(R.id.tvEmpty);
            tvCount = findViewById(R.id.tvCount);
            if (rv != null) {
                rv.setLayoutManager(new LinearLayoutManager(this));
                adapter = new SongAdapter(new SongAdapter.OnItemClick() {
                    @Override public void onClick(Song s, int pos) { openPlayer(pos); }
                });
                rv.setAdapter(adapter);
            }

            miniPlayer = findViewById(R.id.miniPlayer);
            miniCover = findViewById(R.id.miniCover);
            miniTitle = findViewById(R.id.miniTitle);
            miniArtist = findViewById(R.id.miniArtist);
            miniPlay = findViewById(R.id.miniPlay);
            if (miniPlay != null) miniPlay.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { com.music.app.service.MusicService.toggle(MainActivity.this); }
            });
            if (miniPlayer != null) miniPlayer.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                }
            });

            // 请求悬浮窗权限
            if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
                ui.postDelayed(new Runnable() {
                    @Override public void run() {
                        try {
                            new android.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("开启悬浮窗")
                                .setMessage("灵动岛需要悬浮窗权限才能显示")
                                .setPositiveButton("去开启", new android.content.DialogInterface.OnClickListener() {
                                    @Override public void onClick(android.content.DialogInterface d, int w) {
                                        try {
                                            Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                            i.setData(android.net.Uri.parse("package:" + getPackageName()));
                                            startActivity(i);
                                        } catch (Throwable ignored) {}
                                    }
                                })
                                .setNegativeButton("稍后", null).show();
                        } catch (Throwable ignored) {}
                    }
                }, 800);
            }

            requestPermAndScan();
            startIsland();
            ui.postDelayed(updateMini, 500);
        } catch (Throwable t) {
            NiceToast.show(this, "初始化失败");
        }
    }

    private final Runnable updateMini = new Runnable() {
        @Override public void run() {
            try {
                if (miniPlayer != null) {
                    if (com.music.app.service.MusicService.isPlaying() || !com.music.app.service.MusicService.sharedQueue.isEmpty()) {
                        if (com.music.app.service.MusicService.sharedPlayer != null) {
                            int idx = com.music.app.service.MusicService.getIndex();
                            List<Song> q = com.music.app.service.MusicService.sharedQueue;
                            if (q != null && idx >= 0 && idx < q.size()) {
                                Song s = q.get(idx);
                                miniPlayer.setVisibility(View.VISIBLE);
                                miniTitle.setText(s.title);
                                miniArtist.setText(s.artist);
                                String name = WallpaperHelper.forSong(MainActivity.this, s.id);
                                Bitmap bm = WallpaperHelper.loadSmall(MainActivity.this, name);
                                if (bm != null) miniCover.setImageBitmap(bm);
                                miniPlay.setText(com.music.app.service.MusicService.isPlaying() ? "⏸" : "▶");
                            }
                        }
                    } else {
                        miniPlayer.setVisibility(View.GONE);
                    }
                }
            } catch (Throwable ignored) {}
            ui.postDelayed(this, 500);
        }
    };

    private void startIsland() {
        try {
            if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) return;
            startService(new Intent(this, IslandService.class));
        } catch (Throwable ignored) {}
    }

    private void filter(String q) {
        if (q == null || q.trim().isEmpty()) showing = new ArrayList<Song>(all);
        else {
            String k = q.toLowerCase().trim();
            showing = new ArrayList<Song>();
            for (Song s : all) {
                if (s.title.toLowerCase().contains(k)
                    || s.artist.toLowerCase().contains(k)
                    || s.album.toLowerCase().contains(k)) showing.add(s);
            }
        }
        if (adapter != null) adapter.setData(showing);
        if (tvCount != null) tvCount.setText("共 " + showing.size() + " 首");
        if (tvEmpty != null) {
            tvEmpty.setVisibility(showing.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText("没有找到匹配的歌曲");
        }
    }

    private void requestPermAndScan() {
        String perm = Build.VERSION.SDK_INT >= 33
            ? "android.permission.READ_MEDIA_AUDIO"
            : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) scan();
        else ActivityCompat.requestPermissions(this, new String[]{perm}, REQ);
    }

    @Override public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == REQ) {
            if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) scan();
            else if (tvEmpty != null) tvEmpty.setText("没有权限扫描本地音乐");
        }
    }

    private void scan() {
        new Thread(new Runnable() {
            @Override public void run() {
                final List<Song> songs = MusicScanner.scan(MainActivity.this);
                ui.post(new Runnable() {
                    @Override public void run() {
                        all = songs;
                        showing = new ArrayList<Song>(songs);
                        if (adapter != null) adapter.setData(showing);
                        if (tvCount != null) tvCount.setText("共 " + songs.size() + " 首");
                        if (tvEmpty != null) {
                            if (songs.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                tvEmpty.setText("还没有本地音乐");
                            } else tvEmpty.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }).start();
    }

    private void openPlayer(int idx) {
        if (showing.isEmpty()) return;
        if (idx < 0 || idx >= showing.size()) idx = 0;
        PlayerActivity.queue = new ArrayList<Song>(showing);
        PlayerActivity.currentIndex = idx;
        com.music.app.service.MusicService.sharedQueue = new ArrayList<Song>(showing);
        com.music.app.service.MusicService.sharedIndex = idx;
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Override protected void onDestroy() {
        ui.removeCallbacks(updateMini);
        super.onDestroy();
    }
}
