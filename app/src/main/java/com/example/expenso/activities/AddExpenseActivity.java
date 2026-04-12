package com.example.expenso.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Switch;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.expenso.R;
import com.example.expenso.database.ExpenseDao;
import com.example.expenso.models.Expense;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddExpenseActivity extends BaseActivity {

    private EditText amountInput, locationInput, notesInput, peopleCountInput;
    private Button datePickerButton, paymentMethodButton, saveExpenseButton;
    private LinearLayout selectedCategory, splitOptions;
    private TextView selectedCategoryText, splitAmountText;
    private Switch splitSwitch;
    // pinManager provided by BaseActivity
    private ExpenseDao expenseDao;
    private String selectedCategoryName = "";
    private Calendar calendar;
    private boolean isSplitExpense = false;
    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_expense);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        expenseDao = new ExpenseDao(this);
        calendar = Calendar.getInstance();

        initializeViews();
        setupClickListeners();
        setDefaultDate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure date is updated according to real time when user returns to screen
        setDefaultDate();
    }

    private void initializeViews() {
        amountInput = findViewById(R.id.amount_input);
        locationInput = findViewById(R.id.location_input);
        notesInput = findViewById(R.id.notes_input);
        peopleCountInput = findViewById(R.id.people_count_input);
        datePickerButton = findViewById(R.id.date_picker_button);
        paymentMethodButton = findViewById(R.id.payment_method_button);
        saveExpenseButton = findViewById(R.id.btn_save_expense_fixed);
        splitSwitch = findViewById(R.id.split_switch);
        splitOptions = findViewById(R.id.split_options);
        splitAmountText = findViewById(R.id.split_amount_text);

        // Initialize category selection (we'll handle this in the click listeners)
        selectedCategory = findViewById(R.id.category_food); // Default selection
        selectedCategoryName = "Food"; // Default category
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }


    private void setupClickListeners() {
        // Date picker
        datePickerButton.setOnClickListener(v -> showDatePicker());

        // Payment method (placeholder for now)
        paymentMethodButton.setOnClickListener(v -> {
            // TODO: Implement payment method selection
            Toast.makeText(this, "Payment method selection coming soon", Toast.LENGTH_SHORT).show();
        });

        // Split switch
        splitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSplitExpense = isChecked;
            splitOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateSplitAmount();
        });

        // Category selection
        setupCategoryListeners();

        // Save button
        saveExpenseButton.setOnClickListener(v -> saveExpense());

        // Back button
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.navigation_add);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_add) {
                return true;
            } else if (id == R.id.navigation_split) {
                startActivity(new Intent(this, SplitExpenseActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });

        // Text watchers for dynamic split calculation

        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSplitAmount();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        peopleCountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSplitAmount();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupCategoryListeners() {
        // Food category
        findViewById(R.id.category_food).setOnClickListener(v -> selectCategory("Food", (LinearLayout) v));

        // Transport category
        findViewById(R.id.category_transport).setOnClickListener(v -> selectCategory("Transport", (LinearLayout) v));

        // Shopping category
        findViewById(R.id.category_shopping).setOnClickListener(v -> selectCategory("Shopping", (LinearLayout) v));

        // Entertainment category
        findViewById(R.id.category_entertainment)
                .setOnClickListener(v -> selectCategory("Entertainment", (LinearLayout) v));

        // Bills category
        findViewById(R.id.category_bills).setOnClickListener(v -> selectCategory("Bills", (LinearLayout) v));

        // Health category
        findViewById(R.id.category_health).setOnClickListener(v -> selectCategory("Health", (LinearLayout) v));

        // Education category
        findViewById(R.id.category_education).setOnClickListener(v -> selectCategory("Education", (LinearLayout) v));

        // Other category
        findViewById(R.id.category_other).setOnClickListener(v -> selectCategory("Other", (LinearLayout) v));
    }

    private void selectCategory(String categoryName, LinearLayout categoryLayout) {
        // Reset previous selection
        if (selectedCategory != null) {
            selectedCategory.setBackgroundResource(R.drawable.card_background);
        }

        // Set new selection
        selectedCategory = categoryLayout;
        selectedCategoryName = categoryName;
        selectedCategory.setBackgroundResource(R.drawable.gradient_card_background);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateButton();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void updateDateButton() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateString = dateFormat.format(calendar.getTime());
        datePickerButton.setText(dateString);
    }

    private void setDefaultDate() {
        calendar = Calendar.getInstance(); // Refresh to current real-time date
        updateDateButton();
    }

    private void updateSplitAmount() {
        if (!isSplitExpense)
            return;

        String amountStr = amountInput.getText().toString().trim();
        String peopleStr = peopleCountInput.getText().toString().trim();

        if (amountStr.isEmpty() || peopleStr.isEmpty()) {
            splitAmountText.setText("Each person pays: ₹0.00");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            int people = Integer.parseInt(peopleStr);

            if (people <= 0) {
                splitAmountText.setText("Each person pays: ₹0.00");
                return;
            }

            double splitAmount = amount / people;
            splitAmountText.setText(String.format("Each person pays: ₹%.2f", splitAmount));
        } catch (NumberFormatException e) {
            splitAmountText.setText("Each person pays: ₹0.00");
        }
    }

    private void saveExpense() {
        // Validate inputs
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryName.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate split inputs if splitting is enabled
        int peopleCount = 1;
        if (isSplitExpense) {
            String peopleStr = peopleCountInput.getText().toString().trim();
            if (peopleStr.isEmpty()) {
                Toast.makeText(this, "Please enter number of people", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                peopleCount = Integer.parseInt(peopleStr);
                if (peopleCount <= 1) {
                    Toast.makeText(this, "Number of people must be greater than 1", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number of people", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            double amount = Double.parseDouble(amountStr);
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            String description = notesInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();

            // Create expense object
            Expense expense = new Expense(1, amount, selectedCategoryName, date,
                    description.isEmpty() ? location : description);

            // Create a final copy of peopleCount for the lambdas
            final int finalPeopleCount = peopleCount;

            // Save to database off the UI thread
            new Thread(() -> {
                long expenseId = expenseDao.addExpense(expense);
                
                runOnUiThread(() -> {
                    if (expenseId != -1) {
                        // Handle splitting if enabled
                        if (isSplitExpense) {
                            double splitAmount = amount / finalPeopleCount;
                            int payerId = 1; // Current user

                            // Run shared expense inserts in background as well
                            new Thread(() -> {
                                for (int i = 1; i < finalPeopleCount; i++) {
                                    int owesToId = payerId; // They owe to the payer
                                    expenseDao.addSharedExpense((int) expenseId, payerId, owesToId, splitAmount);
                                }
                            }).start();

                            Toast.makeText(this, String.format("Expense split among %d people! Each owes ₹%.2f",
                                    finalPeopleCount, splitAmount), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_LONG).show();
                        }

                        // Clear form
                        clearForm();
                    } else {
                        Toast.makeText(this, "Failed to add expense. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        amountInput.setText("");
        locationInput.setText("");
        notesInput.setText("");
        peopleCountInput.setText("");
        splitSwitch.setChecked(false);
        splitOptions.setVisibility(View.GONE);
        isSplitExpense = false;
        setDefaultDate();
        // Reset to default category
        selectCategory("Food", findViewById(R.id.category_food));
    }
}