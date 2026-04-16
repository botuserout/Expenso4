package com.example.expenso.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * UserProfileManager — stores and retrieves user profile details.
 *
 * Uses a SEPARATE SharedPreferences file ("expenso_profile") so it is
 * completely isolated from PinManager ("expenso_prefs"). No existing
 * code is affected.
 */
public class UserProfileManager {

    private static final String PREFS_NAME        = "expenso_profile";
    private static final String KEY_NAME          = "profile_name";
    private static final String KEY_AGE           = "profile_age";
    private static final String KEY_PROFESSION    = "profile_profession";
    private static final String KEY_PHONE         = "profile_phone";
    private static final String KEY_COMPLETED     = "profile_completed";
    private static final String KEY_AVATAR        = "profile_avatar";

    private final SharedPreferences prefs;

    public UserProfileManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Save ──────────────────────────────────────────────────────────────
    public void saveProfile(String name, int age, String profession, String phone, String avatar) {
        prefs.edit()
                .putString(KEY_NAME, name.trim())
                .putInt(KEY_AGE, age)
                .putString(KEY_PROFESSION, profession.trim())
                .putString(KEY_PHONE, phone.trim())
                .putString(KEY_AVATAR, avatar)
                .putBoolean(KEY_COMPLETED, true)
                .apply();
    }

    // ── Retrieve ──────────────────────────────────────────────────────────
    public String getName() { 
        String name = prefs.getString(KEY_NAME, "User"); 
        if (name != null && name.length() > 30) {
            name = name.substring(0, 30) + "...";
        }
        return name;
    }
    public int    getAge()        { return prefs.getInt(KEY_AGE, 0); }
    public String getProfession() { return prefs.getString(KEY_PROFESSION, ""); }
    public String getPhone()      { return prefs.getString(KEY_PHONE, ""); }
    public String getAvatar()     { return prefs.getString(KEY_AVATAR, "ic_user_profile"); }

    /** Returns true if user has already filled their profile. */
    public boolean isProfileCompleted() {
        return prefs.getBoolean(KEY_COMPLETED, false);
    }

    /** Marks profile as incomplete (used for "Edit Profile" flow reset). */
    public void resetProfileFlag() {
        prefs.edit().putBoolean(KEY_COMPLETED, false).apply();
    }

    /** Clears all profile data (used when creating a new account). */
    public void clearProfile() {
        prefs.edit().clear().apply();
    }
}
