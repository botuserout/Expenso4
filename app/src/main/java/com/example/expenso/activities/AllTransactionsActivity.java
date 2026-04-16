package com.example.expenso.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AllTransactionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private ExpenseDao expenseDao;
    private PinManager pinManager;
    private List<Expense> allExpenses = new ArrayList<>();
    private List<Expense> filteredExpenses = new ArrayList<>();
    private Spinner spinnerCategory, spinnerSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_transactions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        expenseDao = new ExpenseDao(this);
        pinManager = new PinManager(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        setupSpinners();
        setupRecyclerView();
    }

    private void setupSpinners() {
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerSort = findViewById(R.id.spinner_sort);

        // Categories
        String[] categories = {"All Categories", "Food", "Transport", "Shopping", "Entertainment", "Bills", "Education", "Health", "Split"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        // Sort Options
        String[] sortOptions = {"Sort: Newest", "Sort: Oldest", "Amount: High to Low", "Amount: Low to High"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFiltersAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerCategory.setOnItemSelectedListener(filterListener);
        spinnerSort.setOnItemSelectedListener(filterListener);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_all_transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter(filteredExpenses);
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        int userId = pinManager.getCurrentUserId();
        new Thread(() -> {
            allExpenses = expenseDao.getAllExpenses(userId);
            runOnUiThread(this::applyFiltersAndSort);
        }).start();
    }

    private void applyFiltersAndSort() {
        String selectedCategory = spinnerCategory.getSelectedItem().toString();
        int sortPos = spinnerSort.getSelectedItemPosition();

        filteredExpenses.clear();
        
        // 1. Filter
        for (Expense e : allExpenses) {
            if (selectedCategory.equals("All Categories") || e.getCategory().equalsIgnoreCase(selectedCategory)) {
                filteredExpenses.add(e);
            }
        }

        // 2. Sort
        switch (sortPos) {
            case 0: // Newest
                Collections.sort(filteredExpenses, (a, b) -> b.getDate().compareTo(a.getDate()));
                break;
            case 1: // Oldest
                Collections.sort(filteredExpenses, (a, b) -> a.getDate().compareTo(b.getDate()));
                break;
            case 2: // High amount
                Collections.sort(filteredExpenses, (a, b) -> Double.compare(b.getAmount(), a.getAmount()));
                break;
            case 3: // Low amount
                Collections.sort(filteredExpenses, (a, b) -> Double.compare(a.getAmount(), b.getAmount()));
                break;
        }

        adapter.updateExpenses(filteredExpenses);
        
        if (filteredExpenses.isEmpty() && !allExpenses.isEmpty()) {
            Toast.makeText(this, "No transactions found in this category", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}
