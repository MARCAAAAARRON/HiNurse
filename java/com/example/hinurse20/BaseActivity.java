package com.example.hinurse20;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE);

        // Force medium font size only
        float scale = 1.0f;

        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        if (config.fontScale != scale) {
            config.fontScale = scale;
        }

        // Apply language via AppCompat application locales (AppCompat 1.7+)
        int langIndex = prefs.getInt("language_idx", 0); // 0=System, 1=English, 2=Filipino
        if (langIndex == 0) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            Locale locale;
            if (langIndex == 1) {
                locale = Locale.ENGLISH;
            } else {
                // Filipino; prefer modern "fil" if available, fall back to "tl"
                locale = Locale.forLanguageTag("fil");
                if (locale == null || locale.getLanguage().isEmpty()) {
                    locale = new Locale("tl");
                }
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale));
        }

        Context wrapped = newBase.createConfigurationContext(config);
        super.attachBaseContext(wrapped);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Force light theme only
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
    }
}
