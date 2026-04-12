package com.example.expenso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
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

        etName       = findViewById(R.id.et_profile_name);
        etAge        = findViewById(R.id.et_profile_age);
        etProfession = findViewById(R.id.et_profile_profession);
        etPhone      = findViewById(R.id.et_profile_phone);
        btnSave      = findViewById(R.id.btn_save_profile);

        // Pre-fill if user is editing existing profile
        if (profileManager.isProfileCompleted()) {
            etName.setText(profileManager.getName());
            etAge.setText(String.valueOf(profileManager.getAge()));
            etProfession.setText(profileManager.getProfession());
            etPhone.setText(profileManager.getPhone());
        }

        btnSave.setOnClickListener(v -> validateAndSave());
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

        new Thread(() -> {
            profileManager.saveProfile(finalName, finalAge, finalProf, finalPhone);
            // Save to DB
            int userId = pinManager.getCurrentUserId();
            userDao.updateUserProfile(userId, finalName, finalAge, finalProf, finalPhone);
            
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
