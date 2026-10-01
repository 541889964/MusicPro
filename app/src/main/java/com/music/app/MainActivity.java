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
import android.view.KeyEvent;
import android.view.View;
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
    private int currentTab = 0;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        try {
            ImageView bg = findViewById(R.id.ivWallpaper);
            Bitmap bm = WallpaperHelper.loadCurrent(this);
            if (bm != null && bg != null) bg.setImageBitmap(bm);
        } catch (Throwable ignored) {}
        try {
            TextView greeting = findViewById(R.id.tvGreeting);
            TextView sub = findViewById(R.id.tvGreetingSub);
            TextView quote = findViewById(R.id.tvDailyQuote);
            if (greeting != null) greeting.setText(WarmGreeting.byTime() + "，" + Prefs.nickname(this));
            if (sub != null) sub.setText(WarmGreeting.bySubTime());
            if (quote != null) quote.setText("\u201C" + WarmGreeting.dailyQuote() + "\u201D");
        } catch (Throwable ignored) {}
        hearts = findViewById(R.id.hearts);
        if (hearts != null) hearts.start(4500);

        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
                catch (Throwable ignored) {}
            }
        });

        final LinearLayout tabLocal = findViewById(R.id.tabLocal);
        final LinearLayout tabOnline = findViewById(R.id.tabOnline);
        final TextView tabLocalText = findViewById(R.id.tabLocalText);
        final TextView tabOnlineText = findViewById(R.id.tabOnlineText);
        final View tabLocalInd = findViewById(R.id.tabLocalIndicator);
        final View tabOnlineInd = findViewById(R.id.tabOnlineIndicator);

        if (tabLocal != null) tabLocal.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentTab = 0;
                if (tabLocalText != null) tabLocalText.setTextColor(0xFFFFFFFF);
                if (tabOnlineText != null) tabOnlineText.setTextColor(0xFFD6CCF5);
                if (tabLocalInd != null) tabLocalInd.setAlpha(1f);
                if (tabOnlineInd != null) tabOnlineInd.setAlpha(0f);
                showing = new ArrayList<Song>(localAll);
                if (etSearch != null) etSearch.setHint("搜索本地音乐");
                refreshList();
            }
        });
        if (tabOnline != null) tabOnline.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentTab = 1;
                if (tabLocalText != null) tabLocalText.setTextColor(0xFFD6CCF5);
                if (tabOnlineText != null) tabOnlineText.setTextColor(0xFFFFFFFF);
                if (tabLocalInd != null) tabLocalInd.setAlpha(0f);
                if (tabOnlineInd != null) tabOnlineInd.setAlpha(1f);
                showing = new ArrayList<Song>(onlineAll);
                if (etSearch != null) etSearch.setHint("搜索网易云音乐");
                refreshList();
            }
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
            });
            rv.setAdapter(adapter);
        }

        View fab = findViewById(R.id.fab);
        if (fab != null) fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (showing.isEmpty()) NiceToast.show(MainActivity.this, "还没有歌曲");
                else openPlayer(0);
            }
        });

        try {
            Intent g = new Intent(this, com.music.app.service.GuardService.class);
            startService(g);
        } catch (Throwable ignored) {}

        requestPermAndScan();
        autoStartIsland();
    }

    private void autoStartIsland() {
        if (Build.VERSION.SDK_INT >= 23
            && !android.provider.Settings.canDrawOverlays(this)) {
            ui.postDelayed(new Runnable() {
                @Override public void run() {
                    try {
                        new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("需要悬浮窗权限")
                            .setMessage("灵动岛需要悬浮窗权限。点「去授权」→ 找到「拾音」→ 打开")
                            .setPositiveButton("去授权", new android.content.DialogInterface.OnClickListener() {
                                @Override public void onClick(android.content.DialogInterface d, int w) {
                                    try {
                                        Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                        i.setData(android.net.Uri.parse("package:" + getPackageName()));
                                        startActivity(i);
                                    } catch (Throwable ignored) {}
                                }
                            })
                            .setNegativeButton("以后再说", null)
                            .show();
                    } catch (Throwable ignored) {}
                }
            }, 500);
            return;
        }
        try {
            Intent svc = new Intent(this, com.music.app.service.IslandService.class);
            startService(svc);
        } catch (Throwable ignored) {}
    }

    private void openPlayer(int idx) {
        if (showing.isEmpty()) return;
        if (idx < 0 || idx >= showing.size()) idx = 0;
        PlayerActivity.queue = new ArrayList<Song>(showing);
        Intent it = new Intent(this, PlayerActivity.class);
        it.putExtra("index", idx);
        startActivity(it);
    }

    private void doSearchOnline(String kw) {
        if (kw == null || kw.trim().isEmpty()) { NiceToast.show(this, "请输入关键词"); return; }
        if (tvEmpty != null) { tvEmpty.setVisibility(View.VISIBLE); tvEmpty.setText("正在搜索…"); }
        NeteaseApi.search(kw, new NeteaseApi.OnSearch() {
            @Override public void onResult(List<Song> songs) {
                onlineAll = songs;
                showing = new ArrayList<Song>(songs);
                refreshList();
            }
        });
    }

    private void requestPermAndScan() {
        String perm;
        if (Build.VERSION.SDK_INT >= 33) perm = "android.permission.READ_MEDIA_AUDIO";
        else perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) scan();
        else ActivityCompat.requestPermissions(this, new String[]{perm}, REQ_PERM);
    }

    @Override public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == REQ_PERM) {
            if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) scan();
            else if (tvEmpty != null) { tvEmpty.setVisibility(View.VISIBLE); tvEmpty.setText("没有权限扫描本地音乐"); }
        }
    }

    private void scan() {
        new Thread(new Runnable() {
            @Override public void run() {
                final List<Song> songs = MusicScanner.scan(MainActivity.this);
                ui.post(new Runnable() {
                    @Override public void run() {
                        localAll = songs;
                        if (currentTab == 0) { showing = new ArrayList<Song>(songs); refreshList(); }
                    }
                });
            }
        }).start();
    }

    private void filter(String q) {
        List<Song> src = currentTab == 0 ? localAll : onlineAll;
        if (q == null || q.trim().isEmpty()) { showing = new ArrayList<Song>(src); }
        else {
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
                tvEmpty.setText(currentTab == 0 ? "还没有本地音乐哦" : "在上方搜索框输入关键词");
            } else tvEmpty.setVisibility(View.GONE);
        }
    }
}
