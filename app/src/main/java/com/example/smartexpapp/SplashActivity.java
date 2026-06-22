package com.example.smartexpapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.SettingsRepository;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeSettings();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashContent = findViewById(R.id.splashContent);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in);
        Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.splash_scale_up);

        splashContent.startAnimation(fadeIn);
        findViewById(R.id.splashLogo).startAnimation(scaleUp);

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToMain, SPLASH_DURATION_MS);
    }

    private void navigateToMain() {
        AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(this);
        Class<?> target;
        if (authState.isSignedIn() || authState.isGuest()) {
            target = StatsActivity.class;
        } else {
            target = SignInActivity.class;
        }
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void applyThemeSettings() {
        String languageTag = SettingsRepository.getCachedLanguageTag(this);
        String currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (!languageTag.equals(currentLanguageTag)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
        }

        boolean isNightMode = SettingsRepository.getCachedDarkMode(this);
        int targetMode = isNightMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }
}
