package com.example.expenso.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.example.expenso.database.UserDao;
import com.example.expenso.models.User;

public class PinManager {
    private static final String PREFS_NAME = "expenso_secure_prefs";
    private static final String PIN_KEY = "user_pin";
    private static final String PIN_SETUP_KEY = "pin_setup_complete";
    private static final String SESSION_KEY = "user_logged_in";
    private static final String USER_NAME_KEY = "user_name";
    private static final String USER_ID_KEY = "current_user_id";
    private static final String LANGUAGE_KEY = "app_language";

    private SharedPreferences prefs;
    private final UserDao userDao;
    private static boolean isAppUnlocked = false; // In-memory session

    public PinManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to regular SharedPreferences if encryption fails
            prefs = context.getSharedPreferences("expenso_prefs", Context.MODE_PRIVATE);
        }
        userDao = new UserDao(context);
    }

    public void savePin(String name, String pin) {
        // Save to SQLite Database first to get the generated ID
        int userId = userDao.addUser(name, pin);
        
        // Save to SharedPreferences
        prefs.edit()
                .putInt(USER_ID_KEY, userId)
                .putString(PIN_KEY, pin)
                .putString(USER_NAME_KEY, name)
                .putBoolean(PIN_SETUP_KEY, true)
                .apply();
    }

    public boolean updatePin(String newPin) {
        int userId = getCurrentUserId();
        if (userId != -1) {
            boolean dbUpdated = userDao.updateUserPin(userId, newPin);
            if (dbUpdated) {
                prefs.edit().putString(PIN_KEY, newPin).apply();
                return true;
            }
        }
        return false;
    }

    public boolean verifyPin(String enteredPin) {
        // Query database to see if any user has this PIN
        return userDao.getUserIdByPin(enteredPin) != -1;
    }

    public int getUserIdByPin(String pin) {
        return userDao.getUserIdByPin(pin);
    }

    public boolean isPinSetup() {
        // App is set up if at least one user exists in the DB
        return userDao.checkUserExists();
    }

    public void saveLoginSession(int userId, Context context) {
        // Fetch User Details from DB to refresh UserProfileManager Cache
        User user = userDao.getUserDetails(userId);
        if (user != null) {
            UserProfileManager upm = new UserProfileManager(context);
            upm.saveProfile(user.getName(), user.getAge(), user.getProfession(), user.getPhone(), user.getAvatar());
        }

        prefs.edit()
                .putBoolean(SESSION_KEY, true)
                .putInt(USER_ID_KEY, userId)
                .putString(USER_NAME_KEY, user != null ? user.getName() : "User")
                .apply();
    }

    public boolean isLoggedIn() {
        return isPinSetup() && prefs.getBoolean(SESSION_KEY, false);
    }

    public int getCurrentUserId() {
        return prefs.getInt(USER_ID_KEY, -1);
    }

    public String getUserName() {
        return prefs.getString(USER_NAME_KEY, "User");
    }

    public void logout() {
        prefs.edit()
                .putBoolean(SESSION_KEY, false)
                .putInt(USER_ID_KEY, -1)
                .apply();
    }

    public void clearPin() {
        prefs.edit()
                .remove(PIN_KEY)
                .remove(USER_NAME_KEY)
                .putBoolean(PIN_SETUP_KEY, false)
                .putBoolean(SESSION_KEY, false)
                .apply();
    }

    public void saveLanguage(String lang) {
        prefs.edit().putString(LANGUAGE_KEY, lang).apply();
    }

    public String getLanguage() {
        return prefs.getString(LANGUAGE_KEY, "en"); // Default to English
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean("notifications_enabled", true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply();
    }

    public static boolean isAppUnlocked() {
        return isAppUnlocked;
    }

    public static void setAppUnlocked(boolean unlocked) {
        isAppUnlocked = unlocked;
    }
}
