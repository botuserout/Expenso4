package com.example.expenso.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

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
import com.example.expenso.database.GoalDao;
import com.example.expenso.models.Expense;
import com.example.expenso.models.Goal;
import com.example.expenso.utils.PinManager;
import com.example.expenso.utils.UserProfileManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends BaseActivity {

    private UserProfileManager userProfileManager;
    private ExpenseDao expenseDao;
    private ExpenseAdapter expenseAdapter;
    private RecyclerView expensesRecyclerView;
    private LinearLayout budgetWarningsContainer, budgetWarningCard;
    private TextView totalBalanceText, incomeText, expensesText, savingsText, tvGreeting,
            tvOwed, tvOwe;
    private ImageView ivDashboardAvatar;
    
    // Meter Views
    private ProgressBar circularProgressBar;
    private TextView tvMeterPercent, tvMeterDetail, tvMeterRemaining;

    // Trend Views
    private LineChart balanceTrendChart;
    private TextView tvTodayBalance, tvBalanceDiff;

    // Goals Preview
    private TextView tvTopGoalName;
    private ProgressBar pbTopGoal;
    private LinearLayout cardGoalsPreview;

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
        ivDashboardAvatar = findViewById(R.id.iv_dashboard_avatar);
        tvOwed = findViewById(R.id.tv_owed_amount);
        tvOwe = findViewById(R.id.tv_owe_amount);
        
        findViewById(R.id.layout_profile_header).setOnClickListener(v -> 
                startActivity(new Intent(this, UserProfileActivity.class)));
        
        // Meter
        circularProgressBar = findViewById(R.id.budget_circular_progress);
        tvMeterPercent = findViewById(R.id.tv_meter_percent);
        tvMeterDetail = findViewById(R.id.tv_meter_detail);
        tvMeterRemaining = findViewById(R.id.tv_meter_remaining);

        // Trend
        balanceTrendChart = findViewById(R.id.balance_trend_chart);
        tvTodayBalance = findViewById(R.id.tv_today_balance);
        tvBalanceDiff = findViewById(R.id.tv_balance_diff);

        // Goals Preview
        tvTopGoalName = findViewById(R.id.tv_top_goal_name);
        pbTopGoal = findViewById(R.id.pb_top_goal);
        cardGoalsPreview = findViewById(R.id.card_goals_preview);
        cardGoalsPreview.setOnClickListener(v -> startActivity(new Intent(this, GoalsActivity.class)));

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Update greeting with user's name
        if (userProfileManager.isProfileCompleted()) {
            tvGreeting.setText("Hello, " + userProfileManager.getName() + " 👋");
        }
    }

    private void setupRecyclerView() {
        expensesRecyclerView = findViewById(R.id.expenses_recycler_view);
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        expensesRecyclerView.setNestedScrollingEnabled(false); // Smooth scroll in NestedScrollView
        expenseAdapter = new ExpenseAdapter(null);
        expensesRecyclerView.setAdapter(expenseAdapter);
    }

    private void setupClickListeners() {
        // "See All" — show all transactions
        findViewById(R.id.btn_see_all).setOnClickListener(v -> 
                startActivity(new Intent(this, AllTransactionsActivity.class)));

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
            List<Expense> expenses = expenseDao.getRecentExpenses(userId, 5);
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
                
                // Fetch Daily Spending and setup Chart
                new Thread(() -> {
                    Map<String, Double> dailyData = expenseDao.getDailySpending(userId);
                    runOnUiThread(() -> setupBalanceTrend(totalBudget, dailyData));
                }).start();

                // Fetch Top Goal Preview
                new Thread(() -> {
                    GoalDao goalDao = new GoalDao(this);
                    List<Goal> goals = goalDao.getAllGoals(userId);
                    if (!goals.isEmpty()) {
                        Goal topGoal = goals.get(0);
                        runOnUiThread(() -> {
                            tvTopGoalName.setText(topGoal.getIcon() + " " + topGoal.getName() + ": " + topGoal.getProgress() + "%");
                            pbTopGoal.setProgress(topGoal.getProgress());
                        });
                    }
                }).start();
            });
        }).start();
    }

    private void setupBalanceTrend(double totalBudget, Map<String, Double> dailySpending) {
        if (dailySpending.isEmpty() || totalBudget <= 0) {
            balanceTrendChart.setNoDataText("Add expenses to see trend");
            balanceTrendChart.invalidate();
            return;
        }

        List<Entry> entries = new java.util.ArrayList<>();
        final List<String> dateList = new java.util.ArrayList<>();
        
        double cumulativeSpent = 0;
        int index = 0;
        double currentBalance = totalBudget;
        double yesterdayBalance = totalBudget;

        for (Map.Entry<String, Double> entry : dailySpending.entrySet()) {
            yesterdayBalance = currentBalance;
            cumulativeSpent += entry.getValue();
            currentBalance = totalBudget - cumulativeSpent;
            
            entries.add(new Entry(index, (float) currentBalance));
            
            // Format date to dd/MM
            String date = entry.getKey();
            try {
                java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(date);
                dateList.add(new java.text.SimpleDateFormat("dd/MM").format(d));
            } catch (Exception e) {
                dateList.add(date);
            }
            index++;
        }

        tvTodayBalance.setText(String.format("Today: ₹%.0f", currentBalance));
        tvTodayBalance.setTextColor(ContextCompat.getColor(this, currentBalance >= 0 ? R.color.income_green : R.color.expense_red));
        
        // Calculate Trend Delta
        if (dailySpending.size() >= 1) {
            // If only one day, compare with totalBudget (the starting point)
            double baseline = (dailySpending.size() > 1) ? yesterdayBalance : totalBudget;
            double diff = currentBalance - baseline;
            
            if (baseline != 0) {
                double percent = (Math.abs(diff) / baseline) * 100;
                String sign = diff >= 0 ? "▲ +" : "▼ -";
                tvBalanceDiff.setText(String.format("%s%.1f%% vs previous", sign, percent));
                tvBalanceDiff.setTextColor(ContextCompat.getColor(this, diff >= 0 ? R.color.income_green : R.color.expense_red));
            } else {
                tvBalanceDiff.setText("Balance initialized");
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Remaining Balance");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary_color));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_color));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curves
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_light));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        balanceTrendChart.setData(lineData);
        
        // Customizing Axis
        XAxis xAxis = balanceTrendChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                if (i >= 0 && i < dateList.size()) return dateList.get(i);
                return "";
            }
        });

        // Fix for single point visibility
        balanceTrendChart.getAxisLeft().setAxisMinimum((float) (currentBalance - 1000));
        balanceTrendChart.getAxisLeft().setAxisMaximum((float) (Math.max(totalBudget, currentBalance) + 1000));

        balanceTrendChart.getAxisRight().setEnabled(false);
        balanceTrendChart.getAxisLeft().setDrawGridLines(false);
        balanceTrendChart.getDescription().setEnabled(false);
        balanceTrendChart.getLegend().setEnabled(false);
        balanceTrendChart.animateX(1000);
        balanceTrendChart.invalidate();
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
            
            // Load Avatar
            String avatar = userProfileManager.getAvatar();
            int resId = getResources().getIdentifier(avatar, "drawable", getPackageName());
            if (resId != 0) {
                ivDashboardAvatar.setImageResource(resId);
                ivDashboardAvatar.setPadding(0,0,0,0);
            }
        }
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }
}