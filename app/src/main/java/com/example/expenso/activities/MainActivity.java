package com.example.expenso.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenso.R;
import com.example.expenso.adapters.ExpenseAdapter;
import com.example.expenso.database.ExpenseDao;
import com.example.expenso.models.Expense;
import com.example.expenso.utils.PinManager;
import com.example.expenso.utils.UserProfileManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends BaseActivity {

    private UserProfileManager userProfileManager;
    private ExpenseDao expenseDao;
    private ExpenseAdapter expenseAdapter;
    private RecyclerView expensesRecyclerView;
    private LinearLayout budgetWarningsContainer, budgetWarningCard;
    private TextView totalBalanceText, incomeText, expensesText, savingsText, tvGreeting,
            tvOwed, tvOwe;
    
    // Meter Views
    private ProgressBar circularProgressBar;
    private TextView tvMeterPercent, tvMeterDetail, tvMeterRemaining;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // pinManager initialized in BaseActivity
        userProfileManager = new UserProfileManager(this);

        // AUTH FLOW:
        // - No PIN setup yet → go to Login (setup mode)
        // - PIN setup but no active session → go to Login (login mode)
        // - Active session → show dashboard
        if (!pinManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        expenseDao = new ExpenseDao(this);
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadDashboardData(); // single background call for both expenses + balance
    }

    private void initializeViews() {
        totalBalanceText = findViewById(R.id.total_balance);
        incomeText = findViewById(R.id.income_amount);
        expensesText = findViewById(R.id.expenses_amount);
        savingsText = findViewById(R.id.savings_amount);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvOwed = findViewById(R.id.tv_owed_amount);
        tvOwe = findViewById(R.id.tv_owe_amount);
        
        // Meter
        circularProgressBar = findViewById(R.id.budget_circular_progress);
        tvMeterPercent = findViewById(R.id.tv_meter_percent);
        tvMeterDetail = findViewById(R.id.tv_meter_detail);
        tvMeterRemaining = findViewById(R.id.tv_meter_remaining);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Update greeting with user's name
        if (userProfileManager.isProfileCompleted()) {
            tvGreeting.setText("Hello, " + userProfileManager.getName() + " 👋");
        }
    }

    private void setupRecyclerView() {
        expensesRecyclerView = findViewById(R.id.expenses_recycler_view);
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        expenseAdapter = new ExpenseAdapter(null);
        expensesRecyclerView.setAdapter(expenseAdapter);
    }

    private void setupClickListeners() {
        // FAB — add expense
        FloatingActionButton fabAddExpense = findViewById(R.id.fab_add_expense);
        fabAddExpense.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));

        // Bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                return true;
            } else if (id == R.id.navigation_add) {
                startActivity(new Intent(this, AddExpenseActivity.class));
                return true;
            } else if (id == R.id.navigation_split) {
                startActivity(new Intent(this, SplitExpenseActivity.class));
                return true;
            } else if (id == R.id.navigation_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                return true;
            } else if (id == R.id.navigation_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * Loads expenses and balance summary on a background thread,
     * then posts results to the UI thread — avoids blocking main thread.
     */
    private void loadDashboardData() {
        int userId = pinManager.getCurrentUserId();
        new Thread(() -> {
            List<Expense> expenses = expenseDao.getAllExpenses(userId);
            double totalExpenses = expenseDao.getTotalExpenses(userId);
            double owedAmount = expenseDao.getOwedAmount(userId);
            double oweAmount = expenseDao.getOweAmount(userId);
            
            // Real spending = expenses user paid + debt user owes to others
            double realSpending = totalExpenses + oweAmount;
            
            double totalBudget = expenseDao.getTotalBudget(userId);
            double income = totalBudget; 

            runOnUiThread(() -> {
                expenseAdapter.updateExpenses(expenses);
                
                double balance = income - realSpending;
                double savings = balance > 0 ? balance * 0.4 : 0;

                if (totalBudget <= 0) {
                    totalBalanceText.setText("Set Budget");
                    totalBalanceText.setTextSize(24);
                    totalBalanceText.setOnClickListener(v -> startActivity(new Intent(this, AddBudgetActivity.class)));
                    incomeText.setText("₹0.00");
                } else {
                    totalBalanceText.setText(String.format("₹%.2f", balance));
                    totalBalanceText.setOnClickListener(null); // Remove listener if budget set
                    incomeText.setText(String.format("₹%.2f", income));
                }
                
                expensesText.setText(String.format("₹%.2f", realSpending));
                savingsText.setText(String.format("₹%.2f", savings));

                tvOwed.setText(String.format("₹%.0f", owedAmount));
                tvOwe.setText(String.format("₹%.0f", oweAmount));

                // Update Budget Meter
                updateBudgetMeter(realSpending, totalBudget);
            });
        }).start();
    }

    private void updateBudgetMeter(double spent, double limit) {
        if (limit <= 0) {
            tvMeterPercent.setText("0%");
            tvMeterDetail.setText("Set Budget");
            tvMeterRemaining.setText("Remaining: ₹0");
            circularProgressBar.setProgress(0);
            return;
        }

        int percent = (int) ((spent / limit) * 100);
        tvMeterPercent.setText(percent + "%");
        tvMeterDetail.setText(String.format("₹%.0f / ₹%.0f", spent, limit));
        
        double remaining = Math.max(0, limit - spent);
        tvMeterRemaining.setText(String.format("Remaining: ₹%.0f", remaining));

        // Color Logic
        int colorRes;
        if (percent < 70) {
            colorRes = R.color.status_safe;
        } else if (percent < 90) {
            colorRes = R.color.status_warning;
        } else if (percent <= 100) {
            colorRes = R.color.status_critical;
        } else {
            colorRes = R.color.status_exceeded;
        }
        
        int color = ContextCompat.getColor(this, colorRes);
        circularProgressBar.setProgressTintList(ColorStateList.valueOf(color));
        tvMeterPercent.setTextColor(color);

        // Animate
        ObjectAnimator animation = ObjectAnimator.ofInt(circularProgressBar, "progress", 0, Math.min(100, percent));
        animation.setDuration(800);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning from Add Expense
        if (expenseDao != null) {
            loadDashboardData();
        }
        if (userProfileManager != null && userProfileManager.isProfileCompleted()) {
            tvGreeting.setText("Hello, " + userProfileManager.getName() + " 👋");
        }
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }
}