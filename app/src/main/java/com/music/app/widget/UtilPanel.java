package com.music.app.widget;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

/** 实用功能面板：系统监控 + 快捷开关 + 剪贴板 */
public class UtilPanel {
    private final Context ctx;
    private final TextView txtCpu, txtRam, txtNet, txtClipboard;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean wifiOn = false;
    private boolean btOn = false;
    private boolean torchOn = false;
    private boolean silentOn = false;

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            try { update(); } catch (Throwable ignored) {}
            h.postDelayed(this, 2000);
        }
    };

    public UtilPanel(Context ctx, TextView cpu, TextView ram, TextView net, TextView clip) {
        this.ctx = ctx;
        this.txtCpu = cpu;
        this.txtRam = ram;
        this.txtNet = net;
        this.txtClipboard = clip;
        initStates();
    }

    public void start() {
        h.removeCallbacks(updater);
        h.post(updater);
    }

    public void stop() {
        h.removeCallbacks(updater);
    }

    private void initStates() {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            wifiOn = wm != null && wm.isWifiEnabled();
        } catch (Throwable ignored) {}
        try {
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            btOn = ba != null && ba.isEnabled();
        } catch (Throwable ignored) {}
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            silentOn = am != null && am.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
        } catch (Throwable ignored) {}
    }

    private void update() {
        // CPU 负载（用 /proc/stat 估算）
        try {
            if (txtCpu != null) {
                String cpu = readCpuUsage();
                txtCpu.setText("CPU " + cpu);
                int v = parsePercent(cpu);
                int color = v < 40 ? 0xFF4CD964 : v < 70 ? 0xFFFFCC00 : 0xFFFF3B30;
                txtCpu.setTextColor(color);
            }
        } catch (Throwable ignored) {}

        // RAM 使用率
        try {
            if (txtRam != null) {
                android.app.ActivityManager am = (android.app.ActivityManager)
                    ctx.getSystemService(Context.ACTIVITY_SERVICE);
                android.app.ActivityManager.MemoryInfo mi =
                    new android.app.ActivityManager.MemoryInfo();
                if (am != null) {
                    am.getMemoryInfo(mi);
                    long total = mi.totalMem;
                    long avail = mi.availMem;
                    int used = (int)((total - avail) * 100 / total);
                    txtRam.setText("RAM " + used + "%");
                    int color = used < 60 ? 0xFF4CD964 : used < 85 ? 0xFFFFCC00 : 0xFFFF3B30;
                    txtRam.setTextColor(color);
                }
            }
        } catch (Throwable ignored) {}

        // 网络状态
        try {
            if (txtNet != null) {
                String net = "离线";
                int color = 0xFF8E8E93;
                ConnectivityManager cm = (ConnectivityManager)
                    ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
                if (info != null && info.isConnected()) {
                    if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                        net = "WiFi"; color = 0xFF4CD964;
                    } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                        net = "移动网"; color = 0xFF5AC8FA;
                    } else {
                        net = "已连接"; color = 0xFF4CD964;
                    }
                }
                txtNet.setText(net);
                txtNet.setTextColor(color);
            }
        } catch (Throwable ignored) {}

        // 剪贴板
        try {
            if (txtClipboard != null) {
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip()) {
                    ClipData cd = cm.getPrimaryClip();
                    if (cd != null && cd.getItemCount() > 0) {
                        CharSequence text = cd.getItemAt(0).coerceToText(ctx);
                        if (text != null && text.length() > 0) {
                            String t = text.toString().replace("\n", " ");
                            if (t.length() > 30) t = t.substring(0, 30) + "…";
                            txtClipboard.setText("📋 " + t);
                            return;
                        }
                    }
                }
                txtClipboard.setText("📋 剪贴板为空");
            }
        } catch (Throwable ignored) {}
    }

    private String readCpuUsage() {
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.FileReader("/proc/stat"));
            String line = r.readLine();
            r.close();
            if (line == null) return "0%";
            String[] parts = line.trim().split("\\s+");
            long idle = Long.parseLong(parts[4]);
            long total = 0;
            for (int i = 1; i < parts.length; i++) {
                try { total += Long.parseLong(parts[i]); } catch (Throwable ignored) {}
            }
            if (total == 0) return "0%";
            int usage = (int)((total - idle) * 100 / total);
            // 简单平滑（单次采样波动大）
            usage = Math.min(100, Math.max(0, usage / 3));
            return usage + "%";
        } catch (Throwable t) {
            return "N/A";
        }
    }

    private int parsePercent(String s) {
        try {
            return Integer.parseInt(s.replace("%", "").trim());
        } catch (Throwable t) { return 0; }
    }

    // ============ 快捷开关 ============
    public void toggleWifi() {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return;
            if (Build.VERSION.SDK_INT >= 29) {
                // Android 10+ 需要跳系统设置
                toast("请到系统设置切换 WiFi");
                try {
                    ctx.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Throwable ignored) {}
            } else {
                wm.setWifiEnabled(!wm.isWifiEnabled());
                wifiOn = !wifiOn;
                toast(wifiOn ? "WiFi 已开" : "WiFi 已关");
            }
        } catch (Throwable t) { toast("切换失败"); }
    }

    public void toggleBt() {
        try {
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            if (ba == null) { toast("不支持蓝牙"); return; }
            if (Build.VERSION.SDK_INT >= 33) {
                toast("请到系统设置切换蓝牙");
                try {
                    ctx.startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Throwable ignored) {}
            } else {
                if (btOn) ba.disable(); else ba.enable();
                btOn = !btOn;
                toast(btOn ? "蓝牙已开" : "蓝牙已关");
            }
        } catch (Throwable t) { toast("切换失败"); }
    }

    public void toggleTorch() {
        try {
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) return;
            String[] ids = cm.getCameraIdList();
            if (ids == null || ids.length == 0) return;
            String id = ids[0];
            torchOn = !torchOn;
            cm.setTorchMode(id, torchOn);
            toast(torchOn ? "手电筒已开" : "手电筒已关");
        } catch (Throwable t) { toast("手电筒不可用"); }
    }

    public void toggleSilent() {
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            int cur = am.getRingerMode();
            if (cur == AudioManager.RINGER_MODE_SILENT) {
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                silentOn = false;
                toast("已恢复正常");
            } else {
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                silentOn = true;
                toast("已静音");
            }
        } catch (Throwable t) { toast("切换失败"); }
    }

    public void copyClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData cd = cm.getPrimaryClip();
                if (cd != null && cd.getItemCount() > 0) {
                    CharSequence t = cd.getItemAt(0).coerceToText(ctx);
                    toast("已复制: " + (t != null ? t.toString() : ""));
                    return;
                }
            }
            toast("剪贴板为空");
        } catch (Throwable t) { toast("复制失败"); }
    }

    private void toast(String s) {
        try {
            Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {}
    }
}
