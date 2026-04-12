package com.example.expenso.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expenso.R;
import com.example.expenso.adapters.GoalAdapter;
import com.example.expenso.database.GoalDao;
import com.example.expenso.models.Goal;
import com.example.expenso.utils.PinManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class GoalsActivity extends BaseActivity {

    private RecyclerView rvGoals;
    private GoalAdapter goalAdapter;
    private GoalDao goalDao;
    private List<Goal> goalList;
    private PinManager pinManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        pinManager = new PinManager(this);
        goalDao = new GoalDao(this);
        
        setupToolbar();
        setupRecyclerView();
        
        ExtendedFloatingActionButton fabAddGoal = findViewById(R.id.fab_add_goal);
        fabAddGoal.setOnClickListener(v -> showAddGoalDialog());

        loadGoals();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvGoals = findViewById(R.id.rv_goals);
        rvGoals.setLayoutManager(new LinearLayoutManager(this));
        goalList = new ArrayList<>();
        goalAdapter = new GoalAdapter(this, goalList);
        rvGoals.setAdapter(goalAdapter);
    }

    private void loadGoals() {
        int userId = pinManager.getCurrentUserId();
        new Thread(() -> {
            List<Goal> goals = goalDao.getAllGoals(userId);
            runOnUiThread(() -> {
                goalList = goals;
                goalAdapter.updateGoals(goals);
            });
        }).start();
    }

    private void showAddGoalDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_goal, null);
        TextInputEditText etName = dialogView.findViewById(R.id.et_goal_name);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_target_amount);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_goal_type);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    int selectedId = rgType.getCheckedRadioButtonId();

                    if (name.isEmpty() || amountStr.isEmpty() || selectedId == -1) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount = Double.parseDouble(amountStr);
                    RadioButton rb = dialogView.findViewById(selectedId);
                    String icon = (String) rb.getTag();

                    Goal newGoal = new Goal(pinManager.getCurrentUserId(), name, amount, 0, icon);
                    new Thread(() -> {
                        goalDao.addGoal(newGoal);
                        runOnUiThread(this::loadGoals);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
