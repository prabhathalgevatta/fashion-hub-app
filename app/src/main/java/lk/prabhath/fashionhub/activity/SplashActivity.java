package lk.prabhath.fashionhub.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import lk.prabhath.fashionhub.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDark = themePrefs.getBoolean("is_dark_mode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View topContainer = findViewById(R.id.top_container);
        View ivHero = findViewById(R.id.iv_splash_hero);
        View bottomContainer = findViewById(R.id.bottom_container);
        View btnGetStarted = findViewById(R.id.btn_get_started);
        View btnSkip = findViewById(R.id.btn_skip);

        // Entrance animations
        if (topContainer != null) {
            android.view.animation.Animation slideDown = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_down);
            topContainer.startAnimation(slideDown);
        }

        if (ivHero != null) {
            android.view.animation.Animation scaleUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_up);
            ivHero.startAnimation(scaleUp);
        }

        if (bottomContainer != null) {
            android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up);
            bottomContainer.startAnimation(slideUp);
        }

        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                finish();
            });
        }

        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> {
                // Button click feedback micro-animation
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction(() -> {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }).start();
                }).start();
            });
        }
    }
}