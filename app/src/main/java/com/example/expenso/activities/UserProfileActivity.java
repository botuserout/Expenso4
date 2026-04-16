package com.example.expenso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.expenso.R;
import com.example.expenso.database.UserDao;
import com.example.expenso.utils.PinManager;
import com.example.expenso.utils.UserProfileManager;

/**
 * UserProfileActivity — collects user details (Name, Age, Profession, Phone).
 *
 * Shown automatically after first PIN setup when profile is not yet complete.
 * Also reachable from Settings > Edit Profile to update details.
 *
 * Does NOT touch any existing auth, database, or navigation logic.
 */
public class UserProfileActivity extends BaseActivity {

    private EditText etName, etAge, etProfession, etPhone;
    private Button btnSave;
    private ImageView ivProfileHeader;
    private TextView tvProfileHeaderName;
    private String selectedAvatar = "ic_user_profile";
    private UserProfileManager profileManager;
    private UserDao userDao;
    private PinManager pinManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        profileManager = new UserProfileManager(this);
        userDao = new UserDao(this);
        pinManager = new PinManager(this);

        ivProfileHeader = findViewById(R.id.iv_profile_header);
        tvProfileHeaderName = findViewById(R.id.tv_profile_header_name);
        etName       = findViewById(R.id.et_profile_name);
        etAge        = findViewById(R.id.et_profile_age);
        etProfession = findViewById(R.id.et_profile_profession);
        etPhone      = findViewById(R.id.et_profile_phone);
        btnSave      = findViewById(R.id.btn_save_profile);

        setupAvatarSelection();

        // Pre-fill if user is editing existing profile
        if (profileManager.isProfileCompleted()) {
            String name = profileManager.getName();
            etName.setText(name);
            etAge.setText(String.valueOf(profileManager.getAge()));
            etProfession.setText(profileManager.getProfession());
            etPhone.setText(profileManager.getPhone());
            
            selectedAvatar = profileManager.getAvatar();
            updateSelectedAvatarUI();
            
            tvProfileHeaderName.setText(name);
        }

        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void setupAvatarSelection() {
        int[] avatarIds = {
            R.id.avatar_luffy, R.id.avatar_zoro, R.id.avatar_nami,
            R.id.avatar_sanji, R.id.avatar_robin, R.id.avatar_brook
        };
        String[] avatarNames = {"luffy", "zoro", "nami", "sanji", "robin", "brook"};

        for (int i = 0; i < avatarIds.length; i++) {
            final String avatarName = avatarNames[i];
            findViewById(avatarIds[i]).setOnClickListener(v -> {
                selectedAvatar = avatarName;
                updateSelectedAvatarUI();
            });
        }
    }

    private void updateSelectedAvatarUI() {
        int[] avatarIds = {
            R.id.avatar_luffy, R.id.avatar_zoro, R.id.avatar_nami,
            R.id.avatar_sanji, R.id.avatar_robin, R.id.avatar_brook
        };
        String[] avatarNames = {"luffy", "zoro", "nami", "sanji", "robin", "brook"};

        for (int i = 0; i < avatarIds.length; i++) {
            ImageView iv = findViewById(avatarIds[i]);
            if (avatarNames[i].equals(selectedAvatar)) {
                iv.setBackgroundResource(R.drawable.category_active_bg);
            } else {
                iv.setBackgroundResource(R.drawable.input_background);
            }
        }

        // Update header
        int resId = getResources().getIdentifier(selectedAvatar, "drawable", getPackageName());
        if (resId != 0) {
            ivProfileHeader.setImageResource(resId);
            ivProfileHeader.setPadding(0, 0, 0, 0); // Remove padding for character icons
            
            // Comprehensive tint clearing
            ivProfileHeader.setColorFilter(null);
            ivProfileHeader.setImageTintList(null); 
            
            // Update Header Name (capitalize first letter)
            String capitalized = selectedAvatar.substring(0, 1).toUpperCase() + selectedAvatar.substring(1);
            tvProfileHeaderName.setText(capitalized);
        }
    }

    /**
     * Validates all fields then saves profile off the UI thread.
     * Shows field-level errors via setError() + toast for phone.
     */
    private void validateAndSave() {
        String name       = etName.getText().toString().trim();
        String ageStr     = etAge.getText().toString().trim();
        String profession = etProfession.getText().toString().trim();
        String phone      = etPhone.getText().toString().trim();

        // ── Validation ────────────────────────────────────────────────────
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name cannot be empty");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(ageStr)) {
            etAge.setError("Age is required");
            etAge.requestFocus();
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age <= 0 || age > 120) {
                etAge.setError("Enter a valid age (1–120)");
                etAge.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAge.setError("Enter a valid number");
            etAge.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(profession)) {
            etProfession.setError("Profession cannot be empty");
            etProfession.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return;
        }
        if (phone.length() != 10) {
            etPhone.setError("Must be exactly 10 digits");
            etPhone.requestFocus();
            Toast.makeText(this, "Phone number must be 10 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Save off UI thread ────────────────────────────────────────────
        final String finalName   = name;
        final int    finalAge    = age;
        final String finalProf   = profession;
        final String finalPhone  = phone;
        final String finalAvatar = selectedAvatar;

        new Thread(() -> {
            profileManager.saveProfile(finalName, finalAge, finalProf, finalPhone, finalAvatar);
            // Save to DB
            int userId = pinManager.getCurrentUserId();
            userDao.updateUserProfile(userId, finalName, finalAge, finalProf, finalPhone, finalAvatar);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "Profile saved! Welcome, " + finalName + " 👋",
                        Toast.LENGTH_SHORT).show();
                goToDashboard();
            });
        }).start();
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
