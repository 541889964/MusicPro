package com.music.app.widget;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.animation.*;
import android.widget.TextView;
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
            w.setLayout((int)(dm.widthPixels*0.9f), WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.65f;
            w.setAttributes(lp);
        }
        final View root = findViewById(R.id.dialogRoot);
        final View icon = findViewById(R.id.ivIcon);
        final View title = findViewById(R.id.tvTitle);
        final View divider = findViewById(R.id.divider);
        final View sv = findViewById(R.id.svContent);
        final View btn = findViewById(R.id.btnConfirm);
        final TextView cnt = findViewById(R.id.tvCountdown);
        ((TextView) findViewById(R.id.tvContent)).setText(content);
        root.setAlpha(0f); root.setScaleX(0.82f); root.setScaleY(0.82f); root.setTranslationY(60f);
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
        ObjectAnimator ob = ObjectAnimator.ofFloat(btn, "translationY", 0f, -4f, 0f);
        ob.setDuration(2200);
        ob.setInterpolator(new AccelerateDecelerateInterpolator());
        ob.start();
        final int[] remain = {3};
        cnt.setText("请仔细阅读公告（3s）");
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override public void run() {
                remain[0]--;
                if (remain[0] > 0) {
                    cnt.setText("请仔细阅读公告（" + remain[0] + "s）");
                    cnt.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                cnt.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                            }
                        }).start();
                    h.postDelayed(this, 1000);
                } else {
                    cnt.animate().alpha(0f).setDuration(300).start();
                    cnt.postDelayed(new Runnable() {
                        @Override public void run() { cnt.setVisibility(View.GONE); }
                    }, 320);
                }
            }
        }, 1000);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (remain[0] > 0) { shake(root); return; }
                closeWithStyle(root, icon, title, divider, sv, btn);
            }
        });
    }
    private void shake(View v) {
        ObjectAnimator.ofFloat(v, "translationX", 0f, -12f, 12f, -8f, 8f, -4f, 4f, 0f)
            .setDuration(400).start();
    }
    private void closeWithStyle(final View... views) {
        final View btn = views[views.length-1];
        btn.animate().scaleX(0.85f).scaleY(0.85f).setDuration(90).start();
        btn.postDelayed(new Runnable() {
            @Override public void run() {
                btn.animate().scaleX(1f).scaleY(1f).setDuration(160)
                    .setInterpolator(new OvershootInterpolator(2f)).start();
            }
        }, 90);
        for (int i = 0; i < views.length-1; i++) {
            views[i].animate().alpha(0f).translationY(20f)
                .setStartDelay(40L*i).setDuration(260)
                .setInterpolator(new AccelerateInterpolator()).start();
        }
        views[0].animate().alpha(0f).scaleX(0.86f).scaleY(0.86f)
            .setStartDelay(180).setDuration(320)
            .setInterpolator(new AccelerateInterpolator()).start();
        views[0].postDelayed(new Runnable() {
            @Override public void run() { dismiss(); }
        }, 520);
    }
    @Override public void onBackPressed() {
        View root = findViewById(R.id.dialogRoot);
        if (root != null) shake(root);
    }
}
