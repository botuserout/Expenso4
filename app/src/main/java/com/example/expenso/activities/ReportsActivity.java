package com.example.expenso.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.expenso.R;
import com.example.expenso.database.ExpenseDao;
import com.example.expenso.utils.PinManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Map;

/**
 * ReportsActivity — Advanced Data-Driven Reporting Screen.
 * Visualizes spending via Pie and Bar charts using real SQLite data.
 */
public class ReportsActivity extends BaseActivity {

    private static final String TAG = "ReportsActivity";
    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvTotalSpent, tvTopCategory;
    private ExpenseDao expenseDao;
    private PinManager pinManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        expenseDao = new ExpenseDao(this);
        pinManager = new PinManager(this);

        initializeViews();
        setupBottomNav();
        loadReportData();
    }

    private void initializeViews() {
        pieChart = findViewById(R.id.pie_chart);
        barChart = findViewById(R.id.bar_chart);
        tvTotalSpent = findViewById(R.id.tv_total_spent);
        tvTopCategory = findViewById(R.id.tv_top_category);
    }

    private void loadReportData() {
        new Thread(() -> {
            int userId = pinManager.getCurrentUserId();
            double totalSpentInDb = expenseDao.getTotalExpenses(userId);
            double oweAmount = expenseDao.getOweAmount(userId);
            double totalRealSpent = totalSpentInDb + oweAmount;

            Map<String, Double> categoryData = expenseDao.getCategorySpending(userId);
            Map<String, Double> monthlyData = expenseDao.getMonthlySpending(userId);

            runOnUiThread(() -> {
                displaySummary(totalRealSpent, categoryData);
                setupPieChart(categoryData);
                setupBarChart(monthlyData);
            });
        }).start();
    }

    private void displaySummary(double total, Map<String, Double> categoryMap) {
        tvTotalSpent.setText(String.format("₹%.0f", total));

        String topCat = "None";
        double maxAmt = 0;
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            if (entry.getValue() > maxAmt) {
                maxAmt = entry.getValue();
                topCat = entry.getKey();
            }
        }
        tvTopCategory.setText(topCat);
    }

    private void setupPieChart(Map<String, Double> categoryMap) {
        if (categoryMap.isEmpty()) {
            pieChart.setNoDataText("Add expenses to see distribution");
            pieChart.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Categories");
        pieChart.setHoleRadius(40f);
        pieChart.animateY(1000);

        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setEnabled(true);

        pieChart.invalidate();
    }

    private void setupBarChart(Map<String, Double> monthlyMap) {
        if (monthlyMap.isEmpty()) {
            barChart.setNoDataText("Not enough data for monthly trends");
            barChart.invalidate();
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Double> entry : monthlyMap.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Spending per Month");
        dataSet.setColor(getResources().getColor(R.color.primary_purple));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.animateY(1000);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        barChart.invalidate();
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.navigation_reports);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.navigation_add) {
                startActivity(new Intent(this, AddExpenseActivity.class));
                return true;
            } else if (id == R.id.navigation_split) {
                startActivity(new Intent(this, SplitExpenseActivity.class));
                return true;
            } else if (id == R.id.navigation_reports) {
                return true;
            } else if (id == R.id.navigation_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }
}
