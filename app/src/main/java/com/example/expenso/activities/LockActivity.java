package com.example.expenso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.expenso.R;
import com.example.expenso.utils.PinManager;

public class LockActivity extends BaseActivity {

    private String enteredPin = "";
    private TextView[] pinDots = new TextView[4];
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Reuse login layout for consistency

        initViews();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_login_subtitle);
        tvStatus.setText("Device Locked - Enter PIN");

        pinDots[0] = findViewById(R.id.pin_digit_1);
        pinDots[1] = findViewById(R.id.pin_digit_2);
        pinDots[2] = findViewById(R.id.pin_digit_3);
        pinDots[3] = findViewById(R.id.pin_digit_4);

        setupKeypad();
    }

    private void setupKeypad() {
        int[] buttonIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(v -> {
                if (enteredPin.length() < 4) {
                    enteredPin += ((Button) v).getText().toString();
                    updatePinDots();
                }
            });
        }

        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
                updatePinDots();
            }
        });

        findViewById(R.id.btn_enter).setOnClickListener(v -> validatePin());
    }

    private void updatePinDots() {
        for (int i = 0; i < 4; i++) {
            if (i < enteredPin.length()) {
                pinDots[i].setText("●");
                pinDots[i].setBackgroundResource(R.drawable.gradient_card_background);
            } else {
                pinDots[i].setText("");
                pinDots[i].setBackgroundResource(R.drawable.card_background);
            }
        }
        
        if (enteredPin.length() == 4) {
            validatePin();
        }
    }

    private void validatePin() {
        if (pinManager.verifyPin(enteredPin)) {
            PinManager.setAppUnlocked(true);
            finish(); // Go back to whatever activity was being opened
        } else {
            enteredPin = "";
            updatePinDots();
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Disabled to prevent bypass
        // moveTaskToBack(true) would minimize the app
        moveTaskToBack(true);
    }
}
