package com.music.app;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
public class SplashActivity extends AppCompatActivity {
    private void goMain() {
        try {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Throwable ignored) {}
        finish();
    }
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try { setContentView(R.layout.activity_splash); }
        catch (Throwable t) { goMain(); return; }
        new Handler().postDelayed(new Runnable() {
            @Override public void run() { goMain(); }
        }, 1500);
    }
}
