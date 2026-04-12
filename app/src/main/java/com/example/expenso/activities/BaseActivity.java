package com.example.expenso.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.expenso.utils.NotificationHelper;
import com.example.expenso.utils.PinManager;
import com.example.expenso.workers.SmartRemindersWorker;

import android.content.Intent;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * BaseActivity — Handles global language settings and common UI logic.
 * All activities should extend this for consistent language support.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected PinManager pinManager;
    private static int activeActivities = 0; // Simple foreground detection

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pinManager = new PinManager(this);
        // Apply saved locale before onCreate
        applyLocale();
        super.onCreate(savedInstanceState);

        // Setup Smart Notifications
        NotificationHelper.createNotificationChannel(this);
        schedulePeriodicReminders();
    }

    private void schedulePeriodicReminders() {
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                SmartRemindersWorker.class, 24, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "expenso_daily_reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        activeActivities++;
    }

    @Override
    protected void onStop() {
        super.onStop();
        activeActivities--;
        if (activeActivities == 0) {
            // App might have gone to background. 
            // We wait a moment to see if another activity starts (which would increment it back to 1).
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (activeActivities == 0) {
                    PinManager.setAppUnlocked(false);
                }
            }, 700); // 700ms grace period for transitions
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if lock enforcement is needed
        if (pinManager.isPinSetup() && !PinManager.isAppUnlocked()) {
            // Avoid loop if already on Lock or Login screen
            if (!(this instanceof LockActivity || this instanceof LoginActivity)) {
                Intent intent = new Intent(this, LockActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }

    private void applyLocale() {
        String lang = pinManager.getLanguage();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private Context updateBaseContextLocale(Context context) {
        PinManager tempManager = new PinManager(context);
        String lang = tempManager.getLanguage();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }
}
