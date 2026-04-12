package com.example.expenso.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.expenso.R;
import com.example.expenso.database.ExpenseDao;
import com.example.expenso.utils.PinManager;
import com.example.expenso.utils.ReminderReceiver;
import com.example.expenso.utils.UserProfileManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends BaseActivity {

    private Switch languageToggle;
    private TextView languageName;
    private BottomNavigationView bottomNavigationView;
    private ExpenseDao expenseDao;
    private UserProfileManager profileManager;

    private final ActivityResultLauncher<String> csvPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importCsvData(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        expenseDao = new ExpenseDao(this);
        initializeViews();
        setupClickListeners();
        updateLanguageUI();
    }

    private void initializeViews() {
        languageToggle = findViewById(R.id.language_toggle);
        languageName = findViewById(R.id.language_name);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        profileManager = new UserProfileManager(this);

        // Update profile card info
        if (profileManager.isProfileCompleted()) {
            TextView tvName = findViewById(R.id.tv_profile_name);
            TextView tvProf = findViewById(R.id.tv_profile_profession);
            TextView tvInit = findViewById(R.id.tv_profile_initial);

            String name = profileManager.getName();
            tvName.setText(name);
            tvProf.setText(profileManager.getProfession());
            if (!name.isEmpty()) {
                tvInit.setText(String.valueOf(name.charAt(0)).toUpperCase());
            }
        }
        
        // Update Budget value in Settings UI
        TextView tvBudgetValue = findViewById(R.id.tv_budget_settings_value);
        if (tvBudgetValue != null && expenseDao != null) {
            int userId = new PinManager(this).getCurrentUserId();
            double totalBudget = expenseDao.getTotalBudget(userId);
            if (totalBudget > 0) {
                tvBudgetValue.setText(String.format("₹%.0f/month", totalBudget));
            } else {
                tvBudgetValue.setText("Not set");
            }
        }
    }

    private void setupClickListeners() {
        // Language Toggle
        languageToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String newLang = isChecked ? "en" : "hi";
            if (!pinManager.getLanguage().equals(newLang)) {
                pinManager.saveLanguage(newLang);
                
                // Restart ALL activities to apply language globally
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        // Import Data
        findViewById(R.id.btn_import_data).setOnClickListener(v -> {
            csvPickerLauncher.launch("text/*");
        });

        // Bill Reminder
        findViewById(R.id.btn_bill_reminder).setOnClickListener(v -> setBillReminder());

        // Edit Profile
        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class));
        });

        // Set Budget
        findViewById(R.id.btn_set_budget).setOnClickListener(v -> {
            startActivity(new Intent(this, AddBudgetActivity.class));
        });

        // Security Row
        findViewById(R.id.btn_security).setOnClickListener(v -> {
            // Navigate to Pin Change Activity (to be created)
            startActivity(new Intent(this, ChangePinActivity.class));
        });

        // About Row
        findViewById(R.id.btn_about).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://docs.google.com/document/d/1YourAboutDocURL"));
            startActivity(intent);
        });

        // Logout
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            pinManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation
        bottomNavigationView.setSelectedItemId(R.id.navigation_settings);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_add) {
                startActivity(new Intent(this, AddExpenseActivity.class));
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
                return true;
            }
            return false;
        });
    }


    private void updateLanguageUI() {
        String currentLang = pinManager.getLanguage();
        languageToggle.setChecked(currentLang.equals("en"));
        languageName.setText(currentLang.equals("en") ? "English" : "हिंदी");
    }

    private void importCsvData(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, R.string.csv_import_error, Toast.LENGTH_SHORT).show();
                return;
            }
            try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
                String[] nextLine;
                // Skip header row
                reader.readNext();
                int count = 0;
                int userId = new PinManager(this).getCurrentUserId();
                while ((nextLine = reader.readNext()) != null) {
                    // Expected CSV format: Amount, Category, Date (yyyy-MM-dd), Description
                    if (nextLine.length >= 4) {
                        try {
                            double amount = Double.parseDouble(nextLine[0].trim());
                            String category = nextLine[1].trim();
                            String date = nextLine[2].trim();
                            String description = nextLine[3].trim();
                            expenseDao.addExpense(new com.example.expenso.models.Expense(userId, amount, category, date, description));
                            count++;
                        } catch (NumberFormatException e) {
                            // Skip invalid rows silently
                        }
                    }
                }
                Toast.makeText(this, getString(R.string.csv_import_success) + " (" + count + ")", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.csv_import_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void setBillReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Set for 10 seconds later for demo purposes
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 10);

        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Toast.makeText(this, R.string.reminder_set_success, Toast.LENGTH_SHORT).show();
        }
    }
}
