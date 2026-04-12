package com.example.expenso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.expenso.R;
import com.example.expenso.utils.PinManager;
import com.example.expenso.utils.UserProfileManager;

public class LoginActivity extends BaseActivity {

    private TextView[] pinDigits;
    private StringBuilder enteredPin;
    // REMOVED: private PinManager pinManager; (Shadowing cause of NPE)

    // True = first-time setup (no PIN stored yet), False = returning user login
    private boolean isSetupMode;

    // During setup, after first PIN is entered we store it and ask for confirmation
    private String firstPinEntry = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // super.onCreate(savedInstanceState) must be called first to initialize pinManager in BaseActivity
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        enteredPin = new StringBuilder();

        // Determine mode: first launch (no PIN) → setup; returning user → login
        // Now using protected pinManager from BaseActivity
        isSetupMode = !pinManager.isPinSetup();

        initializeViews();
        setupKeypadListeners();
        updateModeUI();
    }

    private void initializeViews() {
        pinDigits = new TextView[4];
        pinDigits[0] = findViewById(R.id.pin_digit_1);
        pinDigits[1] = findViewById(R.id.pin_digit_2);
        pinDigits[2] = findViewById(R.id.pin_digit_3);
        pinDigits[3] = findViewById(R.id.pin_digit_4);

        // "Forgot PIN?" → reset PIN (only shown in login mode)
        TextView forgotPin = findViewById(R.id.tv_forgot_pin);
        forgotPin.setOnClickListener(v -> {
            pinManager.clearPin();
            isSetupMode = true;
            firstPinEntry = null;
            enteredPin.setLength(0);
            updatePinDisplay();
            updateModeUI();
            Toast.makeText(this, "PIN reset. Please create a new PIN.", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateModeUI() {
        TextView subtitle = findViewById(R.id.tv_login_subtitle);
        TextView forgotPin = findViewById(R.id.tv_forgot_pin);
        if (isSetupMode) {
            subtitle.setText(firstPinEntry == null
                    ? "Create a 4-digit PIN to secure your account"
                    : "Confirm your PIN");
            forgotPin.setVisibility(View.GONE);
        } else {
            subtitle.setText("Enter your PIN to continue");
            forgotPin.setVisibility(View.VISIBLE);
        }
    }

    private void setupKeypadListeners() {
        int[] buttonIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = v -> {
            if (enteredPin.length() < 4) {
                Button button = (Button) v;
                enteredPin.append(button.getText().toString());
                updatePinDisplay();
            }
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        // Backspace
        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                enteredPin.deleteCharAt(enteredPin.length() - 1);
                updatePinDisplay();
            }
        });

        // Confirm / Enter
        findViewById(R.id.btn_enter).setOnClickListener(v -> {
            if (enteredPin.length() == 4) {
                if (isSetupMode) {
                    handleSetupPin();
                } else {
                    verifyPin();
                }
            } else {
                Toast.makeText(this, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** First-time PIN setup: collect first entry, then confirm */
    private void handleSetupPin() {
        if (firstPinEntry == null) {
            // First entry — store it and ask to confirm
            firstPinEntry = enteredPin.toString();
            enteredPin.setLength(0);
            updatePinDisplay();
            updateModeUI();
        } else {
            // Confirmation entry
            if (firstPinEntry.equals(enteredPin.toString())) {
                // PINs match — save and go to dashboard
                pinManager.savePin("User", firstPinEntry);
                goToDashboard();
            } else {
                // Mismatch — reset and start over
                Toast.makeText(this, "PINs do not match. Try again.", Toast.LENGTH_SHORT).show();
                firstPinEntry = null;
                enteredPin.setLength(0);
                updatePinDisplay();
                updateModeUI();
            }
        }
    }

    /** Returning user: verify PIN and open dashboard */
    private void verifyPin() {
        String pin = enteredPin.toString();
        if (pinManager.verifyPin(pin)) {
            // Updated to use the correct session handling in PinManager
            int userId = pinManager.getUserIdByPin(pin);
            pinManager.saveLoginSession(userId, pinManager.getUserName());
            goToDashboard();
        } else {
            Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show();
            enteredPin.setLength(0);
            updatePinDisplay();
        }
    }

    private void goToDashboard() {
        PinManager.setAppUnlocked(true); // Unlock session after successful login/setup
        UserProfileManager profileManager = new UserProfileManager(this);
        Intent intent;
        if (!profileManager.isProfileCompleted()) {
            // First time — collect user details before showing dashboard
            intent = new Intent(this, UserProfileActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updatePinDisplay() {
        for (int i = 0; i < 4; i++) {
            pinDigits[i].setText(i < enteredPin.length() ? "•" : "");
        }
    }
}
