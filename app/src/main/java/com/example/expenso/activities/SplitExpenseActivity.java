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
import android.widget.RadioGroup;
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

    private RadioGroup rgWhoPaid;
    private RadioButton rbIPaid, rbSomeoneElsePaid;
    private View payerNameContainer;
    private EditText etPayerName;

    private RecyclerView rvOwedToYou, rvYouOwe;
    private SharedExpenseAdapter adapterOwedToYou, adapterYouOwe;

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

        rgWhoPaid           = findViewById(R.id.rg_who_paid);
        rbIPaid             = findViewById(R.id.rb_i_paid);
        rbSomeoneElsePaid   = findViewById(R.id.rb_someone_else_paid);
        payerNameContainer  = findViewById(R.id.payer_name_container);
        etPayerName         = findViewById(R.id.et_payer_name);

        p1Share            = findViewById(R.id.participant_1_share);
        p2Share            = findViewById(R.id.participant_2_share);
        p3Share            = findViewById(R.id.participant_3_share);
        p4Share            = findViewById(R.id.participant_4_share);

        p2Name             = findViewById(R.id.participant_2_name);
        p3Name             = findViewById(R.id.participant_3_name);
        p4Name             = findViewById(R.id.participant_4_name);
        
        // p1Share is always the user
        p1Share.setEnabled(false); 

        // Shared History Views
        rvOwedToYou = findViewById(R.id.rv_owed_to_you);
        rvYouOwe = findViewById(R.id.rv_you_owe);
        rvOwedToYou.setLayoutManager(new LinearLayoutManager(this));
        rvYouOwe.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadSharedHistory() {
        int userId = pinManager.getCurrentUserId();
        List<SharedExpense> allShared = expenseDao.getSharedExpenses(userId);
        
        java.util.List<SharedExpense> owedToYouList = new java.util.ArrayList<>();
        java.util.List<SharedExpense> youOweList = new java.util.ArrayList<>();
        
        for (SharedExpense se : allShared) {
            if (se.getOwesToId() == userId && se.getPayerId() != userId) {
                owedToYouList.add(se);
            } else if (se.getPayerId() == userId && se.getOwesToId() != userId) {
                youOweList.add(se);
            }
        }

        if (adapterOwedToYou == null) {
            adapterOwedToYou = new SharedExpenseAdapter(owedToYouList, userId, this::settleExpense);
            rvOwedToYou.setAdapter(adapterOwedToYou);
        } else {
            adapterOwedToYou.updateList(owedToYouList);
        }

        if (adapterYouOwe == null) {
            adapterYouOwe = new SharedExpenseAdapter(youOweList, userId, this::settleExpense);
            rvYouOwe.setAdapter(adapterYouOwe);
        } else {
            adapterYouOwe.updateList(youOweList);
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

        rgWhoPaid.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_someone_else_paid) {
                payerNameContainer.setVisibility(View.VISIBLE);
            } else {
                payerNameContainer.setVisibility(View.GONE);
            }
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

        boolean iPaid = rbIPaid.isChecked();
        String externalPayerName = etPayerName.getText().toString().trim();

        if (!iPaid && externalPayerName.isEmpty()) {
            Toast.makeText(this, "Please enter who paid", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    Toast.makeText(this, "Total of shares must equal total", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Enter valid amounts", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            double equalShare = totalAmount / peopleCount;
            for (int i = 0; i < peopleCount; i++) shares[i] = equalShare;
        }

        saveSplitToDatabase(totalAmount, shares, iPaid, externalPayerName);
    }

    private void saveSplitToDatabase(double totalAmount, double[] shares, boolean iPaid, String externalPayerName) {
        int currentUserId = pinManager.getCurrentUserId();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // 1. Add Main Expense (The person who paid gets the expense record)
        int initialPayerId = iPaid ? currentUserId : -2; // -2 placeholder for external payer to be resolved
        
        new Thread(() -> {
            int actualPayerId = initialPayerId;
            if (!iPaid) {
                actualPayerId = userDao.getUserIdByName(externalPayerName);
                if (actualPayerId == -1) actualPayerId = userDao.addFriendPlaceholder(externalPayerName);
            }

            final int finalPayerId = actualPayerId;
            Expense mainExpense = new Expense(finalPayerId, totalAmount, "Split", date, iPaid ? "You paid group" : externalPayerName + " paid group");
            long expenseId = expenseDao.addExpense(mainExpense);
            
            if (expenseId != -1) {
                String[] names = new String[4];
                names[1] = p2Name.getText().toString().trim();
                names[2] = p3Name.getText().toString().trim();
                names[3] = p4Name.getText().toString().trim();

                for (int i = 0; i < peopleCount; i++) {
                    // Logic:
                    // If I PAID: Friends owe me. Payer = FriendID, OwesTo = MyID, Amount = Friend's Share
                    // If OTHERS PAID: I owe them. Payer = MyID, OwesTo = FriendID, Amount = My Share

                    if (iPaid) {
                        if (i == 0) continue; // My share is already in my expense
                        String friendName = names[i].isEmpty() ? "Friend " + i : names[i];
                        int friendId = userDao.getUserIdByName(friendName);
                        if (friendId == -1) friendId = userDao.addFriendPlaceholder(friendName);
                        
                        expenseDao.addSharedExpense((int) expenseId, friendId, currentUserId, shares[i]);
                    } else {
                        // Someone else paid.
                        int creditorId = finalPayerId;
                        if (i == 0) {
                            // My share: I (Payer) owe to the creditor (OwesTo)
                            expenseDao.addSharedExpense((int) expenseId, currentUserId, creditorId, shares[i]);
                        } else {
                            // Their share: They owe to the creditor
                            String friendName = names[i].isEmpty() ? "Friend " + i : names[i];
                            int debtorId = userDao.getUserIdByName(friendName);
                            if (debtorId == -1) debtorId = userDao.addFriendPlaceholder(friendName);
                            
                            if (debtorId != creditorId) {
                                expenseDao.addSharedExpense((int) expenseId, debtorId, creditorId, shares[i]);
                            }
                        }
                    }
                }
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Split Saved!", Toast.LENGTH_LONG).show();
                    loadSharedHistory();
                    totalAmountInput.setText("");
                    etPayerName.setText("");
                });
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
