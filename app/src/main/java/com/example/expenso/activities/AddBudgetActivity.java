package com.example.expenso.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.expenso.R;
import com.example.expenso.database.ExpenseDao;
import com.example.expenso.models.Budget;
import com.example.expenso.utils.PinManager;

/**
 * AddBudgetActivity — Redesigned for setting a single monthly budget.
 * Category and period selection removed for a cleaner, professional experience.
 */
public class AddBudgetActivity extends BaseActivity {

    private EditText amountInput;
    private Button saveBudgetButton;
    private ImageButton backButton;

    private ExpenseDao expenseDao;
    private PinManager pinManager;
    private final String BUDGET_CATEGORY = "Total"; // Unified category for total budget
    private final String BUDGET_PERIOD = "monthly";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_budget);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        expenseDao = new ExpenseDao(this);
        pinManager = new PinManager(this);

        initializeViews();
        setupClickListeners();
        loadExistingBudget();
    }

    private void initializeViews() {
        amountInput = findViewById(R.id.amount_input);
        saveBudgetButton = findViewById(R.id.save_budget_button);
        backButton = findViewById(R.id.back_button);
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Save button
        saveBudgetButton.setOnClickListener(v -> saveBudget());
    }

    private void loadExistingBudget() {
        new Thread(() -> {
            int userId = pinManager.getCurrentUserId();
            Budget budget = expenseDao.getBudgetByCategory(userId, BUDGET_CATEGORY);
            if (budget != null) {
                runOnUiThread(() -> {
                    String val = String.valueOf((int) budget.getLimitAmount());
                    amountInput.setText(val);
                });
            }
        }).start();
    }

    private void saveBudget() {
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter your monthly budget", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount > 0", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentUserId = pinManager.getCurrentUserId();
            Budget budget = new Budget(currentUserId, BUDGET_CATEGORY, amount, BUDGET_PERIOD);

            new Thread(() -> {
                // Check if exists to Update or Insert
                if (expenseDao.getBudgetByCategory(currentUserId, BUDGET_CATEGORY) != null) {
                    expenseDao.updateBudget(budget);
                } else {
                    expenseDao.addBudget(budget);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Monthly budget saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Navigate back
                });
            }).start();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount entered", Toast.LENGTH_SHORT).show();
        }
    }
}
