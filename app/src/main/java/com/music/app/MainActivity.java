package com.music.app;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
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
import java.util.*;
public class MainActivity extends AppCompatActivity {
    private static final int REQ_PERM = 1001;
    private static final int REQ_OVERLAY = 5001;
    private RecyclerView rv;
    private SongAdapter adapter;
    private final List<Song> showing = new ArrayList<>();
    private TextView tvCount, tvEmpty;
    private EditText etSearch;
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(R.layout.activity_main);
            rv = findViewById(R.id.rvSongs);
            tvCount = findViewById(R.id.tvCount);
            tvEmpty = findViewById(R.id.tvEmpty);
            etSearch = findViewById(R.id.etSearch);
            if (rv != null) {
                rv.setLayoutManager(new LinearLayoutManager(this));
                adapter = new SongAdapter((song, pos) -> openPlayer(pos));
                rv.setAdapter(adapter);
            }
            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
                    public void onTextChanged(CharSequence c, int a, int b, int d) { filter(c.toString()); }
                    public void afterTextChanged(Editable e) {}
                });
            }
            View settings = findViewById(R.id.btnSettings);
            if (settings != null) settings.setOnClickListener(v -> {
                try { startActivity(new Intent(this, SettingsActivity.class)); }
                catch (Throwable ignored) {}
            });
            requestPermAndScan();
            checkOverlayAndStart();
        } catch (Throwable t) {
            android.util.Log.e("Music", "Main", t);
        }
    }
    private void requestPermAndScan() {
        String perm = Build.VERSION.SDK_INT >= 33
            ? "android.permission.READ_MEDIA_AUDIO"
            : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) scanAsync();
        else ActivityCompat.requestPermissions(this, new String[]{perm}, REQ_PERM);
    }
    @Override public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == REQ_PERM && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) scanAsync();
    }
    private void scanAsync() {
        new Thread(() -> {
            List<Song> songs = MusicScanner.scan(this);
            runOnUiThread(() -> {
                showing.clear();
                showing.addAll(songs);
                refresh();
            });
        }).start();
    }
    private void filter(String q) {
        if (q == null || q.trim().isEmpty()) {
            showing.clear();
            showing.addAll(MusicScanner.lastList);
        } else {
            String k = q.toLowerCase().trim();
            showing.clear();
            for (Song s : MusicScanner.lastList) {
                if (s.title.toLowerCase().contains(k)
                    || s.artist.toLowerCase().contains(k)) showing.add(s);
            }
        }
        refresh();
    }
    private void refresh() {
        if (adapter != null) adapter.setData(showing);
        if (tvCount != null) tvCount.setText("共 " + showing.size() + " 首");
        if (tvEmpty != null) tvEmpty.setVisibility(showing.isEmpty() ? View.VISIBLE : View.GONE);
    }
    private void openPlayer(int idx) {
        try {
            PlayerActivity.queue.clear();
            PlayerActivity.queue.addAll(showing);
            Intent it = new Intent(this, PlayerActivity.class);
            it.putExtra("index", idx);
            startActivity(it);
        } catch (Throwable ignored) {}
    }
    private void checkOverlayAndStart() {
        try {
            if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) {
                startIsland();
                return;
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("悬浮窗权限")
                        .setMessage("灵动岛需要悬浮窗权限。点\"去授权\"找到「拾音」打开开关。")
                        .setCancelable(false)
                        .setPositiveButton("去授权", (d, w) -> {
                            try {
                                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                i.setData(android.net.Uri.parse("package:" + getPackageName()));
                                startActivityForResult(i, REQ_OVERLAY);
                            } catch (Throwable ignored) {}
                        }).show();
                } catch (Throwable ignored) {}
            }, 800);
        } catch (Throwable ignored) {}
    }
    @Override protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) startIsland();
        }
    }
    private void startIsland() {
        try {
            Intent svc = new Intent(this, com.music.app.service.IslandService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
        } catch (Throwable ignored) {}
    }
}
