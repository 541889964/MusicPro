package com.music.app.widget;

import android.animation.*;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.animation.*;
import android.widget.TextView;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.music.app.R;

public class AnnouncementDialog extends Dialog {

    public AnnouncementDialog(Context c, String content) {
        super(c, R.style.CustomDialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_announcement);

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics dm = c.getResources().getDisplayMetrics();
            w.setLayout((int)(dm.widthPixels * 0.9f),
                        WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.65f;
            w.setAttributes(lp);
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    lp.blurBehindRadius = 40;
                    w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                } catch (Throwable ignored) {}
            }
        }

        final View root    = findViewById(R.id.dialogRoot);
        final View icon    = findViewById(R.id.ivIcon);
        final View title   = findViewById(R.id.tvTitle);
        final View divider = findViewById(R.id.divider);
        final View sv      = findViewById(R.id.svContent);
        final View btn     = findViewById(R.id.btnConfirm);
        final TextView cnt = findViewById(R.id.tvCountdown);
        ((TextView) findViewById(R.id.tvContent)).setText(content);

        root.setAlpha(0f); root.setScaleX(0.82f); root.setScaleY(0.82f);
        root.setTranslationY(60f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .setDuration(520).setInterpolator(new OvershootInterpolator(1.15f)).start();

        icon.setAlpha(0f); icon.setScaleX(0.5f); icon.setScaleY(0.5f);
        icon.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setStartDelay(180).setDuration(600)
            .setInterpolator(new OvershootInterpolator(2f)).start();

        title.setAlpha(0f); title.setTranslationX(-30f);
        title.animate().alpha(1f).translationX(0f)
            .setStartDelay(320).setDuration(450)
            .setInterpolator(new DecelerateInterpolator()).start();

        divider.setScaleX(0f);
        divider.animate().scaleX(1f).setStartDelay(420).setDuration(420)
            .setInterpolator(new OvershootInterpolator(1.4f)).start();

        sv.setAlpha(0f); sv.setTranslationY(24f);
        sv.animate().alpha(1f).translationY(0f)
            .setStartDelay(500).setDuration(500)
            .setInterpolator(new DecelerateInterpolator()).start();

        btn.setAlpha(0f); btn.setTranslationY(30f);
        btn.animate().alpha(1f).translationY(0f)
            .setStartDelay(650).setDuration(480)
            .setInterpolator(new OvershootInterpolator(1.2f)).start();

        ObjectAnimator.ofFloat(btn, "translationY", 0f, -4f, 0f)
            .setDuration(2200)
            .setInterpolator(new AccelerateDecelerateInterpolator()).start();

        final int[] remain = {3};
        cnt.setText("请仔细阅读公告（3s）");
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override public void run() {
                remain[0]--;
                if (remain[0] > 0) {
                    cnt.setText("请仔细阅读公告（" + remain[0] + "s）");
                    YoYo.with(Techniques.Pulse).duration(300).playOn(cnt);
                    h.postDelayed(this, 1000);
                } else {
                    cnt.animate().alpha(0f).setDuration(300)
                        .withEndAction(() -> cnt.setVisibility(View.GONE)).start();
                }
            }
        }, 1000);

        btn.setOnClickListener(v -> {
            if (remain[0] > 0) {
                YoYo.with(Techniques.Shake).duration(400).playOn(root);
                return;
            }
            closeWithStyle(root, icon, title, divider, sv, btn);
        });
    }

    private void closeWithStyle(View... views) {
        View btn = views[views.length - 1];
        btn.animate().scaleX(0.85f).scaleY(0.85f).setDuration(90)
           .withEndAction(() -> btn.animate().scaleX(1f).scaleY(1f)
                .setDuration(160)
                .setInterpolator(new OvershootInterpolator(2f)).start())
           .start();
        for (int i = 0; i < views.length - 1; i++) {
            views[i].animate().alpha(0f).translationY(20f)
                .setStartDelay(40L * i).setDuration(260)
                .setInterpolator(new AccelerateInterpolator()).start();
        }
        views[0].animate().alpha(0f).scaleX(0.86f).scaleY(0.86f)
            .setStartDelay(180).setDuration(320)
            .setInterpolator(new AccelerateInterpolator())
            .withEndAction(this::dismiss).start();
    }

    @Override public void onBackPressed() {
        View root = findViewById(R.id.dialogRoot);
        if (root != null) YoYo.with(Techniques.Shake).duration(400).playOn(root);
    }
}
