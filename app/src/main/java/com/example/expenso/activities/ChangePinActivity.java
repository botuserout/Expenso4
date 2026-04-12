package com.example.expenso.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.expenso.R;

public class ChangePinActivity extends BaseActivity {

    private EditText etOldPin, etNewPin, etConfirmNewPin;
    private Button btnUpdatePin;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_pin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etOldPin = findViewById(R.id.et_old_pin);
        etNewPin = findViewById(R.id.et_new_pin);
        etConfirmNewPin = findViewById(R.id.et_confirm_new_pin);
        btnUpdatePin = findViewById(R.id.btn_update_pin);
        btnBack = findViewById(R.id.back_button);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnUpdatePin.setOnClickListener(v -> {
            String oldPin = etOldPin.getText().toString().trim();
            String newPin = etNewPin.getText().toString().trim();
            String confirmPin = etConfirmNewPin.getText().toString().trim();

            if (oldPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pinManager.verifyPin(oldPin)) {
                Toast.makeText(this, "Old PIN is incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPin.length() < 4) {
                Toast.makeText(this, "New PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPin.equals(confirmPin)) {
                Toast.makeText(this, "New PINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pinManager.updatePin(newPin)) {
                Toast.makeText(this, "PIN updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update PIN", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
