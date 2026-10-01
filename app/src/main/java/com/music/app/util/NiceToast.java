package com.music.app.util;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;
public class NiceToast {
    public static void show(Context c, String msg) {
        Toast t = new Toast(c);
        TextView tv = new TextView(c);
        tv.setText(msg); tv.setTextColor(Color.WHITE); tv.setTextSize(14);
        tv.setPadding(48, 32, 48, 32);
        GradientDrawable d = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF9B6BFF, 0xFFFF6B9D});
        d.setCornerRadius(40f);
        tv.setBackground(d); tv.setElevation(12f);
        t.setView(tv); t.setDuration(Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0); t.show();
        View v = t.getView();
        if (v != null) {
            v.setAlpha(0f); v.setScaleX(0.85f); v.setScaleY(0.85f);
            v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280)
                .setInterpolator(new OvershootInterpolator(1.3f)).start();
        }
    }
}
