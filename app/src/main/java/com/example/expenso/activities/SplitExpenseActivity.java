package com.example.expenso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenso.R;
import com.example.expenso.adapters.SharedExpenseAdapter;
import com.example.expenso.database.ExpenseDao;
import com.example.expenso.database.UserDao;
import com.example.expenso.models.Expense;
import com.example.expenso.models.SharedExpense;
import com.example.expenso.utils.PinManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SplitExpenseActivity extends BaseActivity {

    private EditText totalAmountInput;
    private RadioButton splitEquallyButton, splitCustomButton;
    private TextView sharePerPerson, splitResultText, splitTotalDisplay, peopleCountText;
    private EditText p1Share, p2Share, p3Share, p4Share;
    private EditText p2Name, p3Name, p4Name;
    private Button btnDecrease, btnIncrease, btnSplitConfirm;

    private RecyclerView rvSharedHistory;
    private SharedExpenseAdapter sharedAdapter;

    private int peopleCount = 2;
    private ExpenseDao expenseDao;
    private UserDao userDao;
    private PinManager pinManager;
    private boolean isCustomSplit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_expense);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        expenseDao = new ExpenseDao(this);
        userDao = new UserDao(this);
        pinManager = new PinManager(this);
        initViews();
        setupListeners();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSharedHistory();
        updateCalculations();
    }

    private void initViews() {
        totalAmountInput   = findViewById(R.id.total_amount_input);
        splitEquallyButton = findViewById(R.id.split_equally);
        splitCustomButton  = findViewById(R.id.split_custom);
        sharePerPerson     = findViewById(R.id.share_per_person);
        splitResultText    = findViewById(R.id.split_result_text);
        splitTotalDisplay  = findViewById(R.id.split_total_display);
        peopleCountText    = findViewById(R.id.people_count_text);
        btnDecrease        = findViewById(R.id.btn_decrease);
        btnIncrease        = findViewById(R.id.btn_increase);
        btnSplitConfirm    = findViewById(R.id.btn_split_confirm);

        p1Share            = findViewById(R.id.participant_1_share);
        p2Share            = findViewById(R.id.participant_2_share);
        p3Share            = findViewById(R.id.participant_3_share);
        p4Share            = findViewById(R.id.participant_4_share);

        p2Name             = findViewById(R.id.participant_2_name);
        p3Name             = findViewById(R.id.participant_3_name);
        p4Name             = findViewById(R.id.participant_4_name);
        
        // p1Share is always the user - keep it disabled for equal split
        p1Share.setEnabled(false); 

        // Shared History
        rvSharedHistory = findViewById(R.id.rv_shared_history);
        rvSharedHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadSharedHistory() {
        int userId = pinManager.getCurrentUserId();
        List<SharedExpense> list = expenseDao.getSharedExpenses(userId);
        if (sharedAdapter == null) {
            sharedAdapter = new SharedExpenseAdapter(list, this::settleExpense);
            rvSharedHistory.setAdapter(sharedAdapter);
        } else {
            sharedAdapter.updateList(list);
        }
    }

    private void settleExpense(SharedExpense expense) {
        expenseDao.settleSharedExpense(expense.getSharedId());
        Toast.makeText(this, "Settled!", Toast.LENGTH_SHORT).show();
        loadSharedHistory();
    }

    private void setupListeners() {
        totalAmountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isCustomSplit) updateCalculations();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnDecrease.setOnClickListener(v -> {
            if (peopleCount > 2) {
                peopleCount--;
                peopleCountText.setText(String.valueOf(peopleCount));
                updateCalculations();
            }
        });

        btnIncrease.setOnClickListener(v -> {
            if (peopleCount < 4) {
                peopleCount++;
                peopleCountText.setText(String.valueOf(peopleCount));
                updateCalculations();
            } else {
                Toast.makeText(this, "Max 4 participants", Toast.LENGTH_SHORT).show();
            }
        });

        splitEquallyButton.setOnClickListener(v -> {
            isCustomSplit = false;
            splitEquallyButton.setChecked(true);
            splitCustomButton.setChecked(false);
            toggleManualFields(false);
            updateCalculations();
        });

        splitCustomButton.setOnClickListener(v -> {
            isCustomSplit = true;
            splitCustomButton.setChecked(true);
            splitEquallyButton.setChecked(false);
            toggleManualFields(true);
        });

        btnSplitConfirm.setOnClickListener(v -> handleSplitConfirmation());
    }

    private void toggleManualFields(boolean enable) {
        p1Share.setEnabled(enable);
        p2Share.setEnabled(enable);
        p3Share.setEnabled(enable);
        p4Share.setEnabled(enable);
        
        if (enable) {
            sharePerPerson.setText("Manual Mode");
        }
    }

    private void handleSplitConfirmation() {
        String amountStr = totalAmountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter total amount", Toast.LENGTH_SHORT).show();
            return;
        }
        double totalAmount = Double.parseDouble(amountStr);

        double[] shares = new double[peopleCount];
        if (isCustomSplit) {
            double sum = 0;
            try {
                shares[0] = Double.parseDouble(p1Share.getText().toString());
                shares[1] = Double.parseDouble(p2Share.getText().toString());
                if (peopleCount >= 3) shares[2] = Double.parseDouble(p3Share.getText().toString());
                if (peopleCount >= 4) shares[3] = Double.parseDouble(p4Share.getText().toString());

                for (double s : shares) sum += s;

                if (Math.abs(sum - totalAmount) > 0.01) {
                    Toast.makeText(this, "Total of shares (₹" + sum + ") must equal total (₹" + totalAmount + ")", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Please enter valid amounts for all participants", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            double equalShare = totalAmount / peopleCount;
            for (int i = 0; i < peopleCount; i++) shares[i] = equalShare;
        }

        saveSplitToDatabase(totalAmount, shares);
    }

    private void saveSplitToDatabase(double totalAmount, double[] shares) {
        int currentUserId = pinManager.getCurrentUserId();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // 1. Add Main Expense
        Expense mainExpense = new Expense(currentUserId, totalAmount, "Split", date, "Group Split");
        
        new Thread(() -> {
            long expenseId = expenseDao.addExpense(mainExpense);
            if (expenseId != -1) {
                // Get friend names from EditTexts
                String[] names = new String[4];
                names[1] = p2Name.getText().toString().trim();
                names[2] = p3Name.getText().toString().trim();
                names[3] = p4Name.getText().toString().trim();

                for (int i = 1; i < peopleCount; i++) {
                    String friendName = names[i].isEmpty() ? "Friend " + i : names[i];
                    
                    // Get or Create Friend ID by Name
                    int friendId = userDao.getUserIdByName(friendName);
                    if (friendId == -1) {
                        friendId = userDao.addFriendPlaceholder(friendName);
                    }
                    
                    expenseDao.addSharedExpense((int) expenseId, friendId, currentUserId, shares[i]);
                }
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Split Expense Saved Successfully!", Toast.LENGTH_LONG).show();
                    loadSharedHistory(); // Refresh the list
                    // Clear fields
                    totalAmountInput.setText("");
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateCalculations() {
        String amountStr = totalAmountInput.getText().toString().trim();
        double total = amountStr.isEmpty() ? 0 : Double.parseDouble(amountStr);
        double share = peopleCount > 0 ? total / peopleCount : 0;
        String shareStr = String.format("%.2f", share);

        sharePerPerson.setText(String.format("₹%.2f each", share));
        splitResultText.setText(String.format("₹%.2f", share));
        splitTotalDisplay.setText(String.format("÷ %d", peopleCount));

        p1Share.setText(shareStr);
        p2Share.setText(peopleCount >= 2 ? shareStr : "0.00");
        p3Share.setText(peopleCount >= 3 ? shareStr : "0.00");
        p4Share.setText(peopleCount >= 4 ? shareStr : "0.00");
        
        // Toggle visibility of rows based on count
        ((View)findViewById(R.id.participant_3_name).getParent().getParent()).setVisibility(peopleCount >= 3 ? View.VISIBLE : View.GONE);
        ((View)findViewById(R.id.participant_4_name).getParent().getParent()).setVisibility(peopleCount >= 4 ? View.VISIBLE : View.GONE);
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.navigation_split);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_add) {
                startActivity(new Intent(this, AddExpenseActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_split) {
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
    }
}
