package com.example.expenso.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.expenso.models.Budget;
import com.example.expenso.models.Expense;
import com.example.expenso.models.SharedExpense;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ExpenseDao {
    private static final String TAG = "ExpenseDao";
    private DBHelper dbHelper;

    public ExpenseDao(Context context) {
        dbHelper = DBHelper.getInstance(context);
    }

    public long addExpense(Expense expense) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", expense.getUserId());
        values.put("amount", expense.getAmount());
        values.put("category", expense.getCategory());
        values.put("date", expense.getDate());
        values.put("description", expense.getDescription());
        
        long result = db.insert("Expenses", null, values);
        if (result == -1) Log.e(TAG, "Failed to insert expense");
        return result;
    }

    public List<Expense> getAllExpenses(int userId) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query("Expenses", null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, "date DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Expense expense = new Expense();
                    expense.setExpenseId(cursor.getInt(cursor.getColumnIndexOrThrow("expense_id")));
                    expense.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
                    expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("amount")));
                    expense.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
                    expense.setDate(cursor.getString(cursor.getColumnIndexOrThrow("date")));
                    expense.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                    expenses.add(expense);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return expenses;
    }

    public double getTotalExpenses(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM Expenses WHERE user_id = ?", new String[]{String.valueOf(userId)});
        double total = 0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        if (cursor != null) cursor.close();
        return total;
    }

    public double getAverageExpense(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT AVG(amount) FROM Expenses WHERE user_id = ?", new String[]{String.valueOf(userId)});
        double average = 0;
        if (cursor != null && cursor.moveToFirst()) {
            average = cursor.getDouble(0);
        }
        if (cursor != null) cursor.close();
        return average;
    }

    public Map<String, Double> getCategorySpending(int userId) {
        Map<String, Double> categorySpending = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT category, SUM(amount) FROM Expenses WHERE user_id = ? GROUP BY category", 
                new String[]{String.valueOf(userId)});
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                categorySpending.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return categorySpending;
    }

    public Map<String, Double> getMonthlySpending(int userId) {
        Map<String, Double> monthlySpending = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // date format: yyyy-MM-dd, we want to group by yyyy-MM
        Cursor cursor = db.rawQuery("SELECT strftime('%Y-%m', date) as month, SUM(amount) " +
                        "FROM Expenses WHERE user_id = ? GROUP BY month ORDER BY month ASC", 
                new String[]{String.valueOf(userId)});
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                monthlySpending.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return monthlySpending;
    }

    // --- Shared Expenses ---
    public long addSharedExpense(int expenseId, int payerId, int owesToId, double amount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("expense_id", expenseId);
        values.put("payer_id", payerId);
        values.put("owes_to_id", owesToId);
        values.put("amount", amount);
        values.put("status", "PENDING"); // Default status
        long result = db.insert("SharedExpenses", null, values);
        if (result == -1) Log.e(TAG, "Failed to insert shared expense");
        return result;
    }

    public List<SharedExpense> getSharedExpenses(int userId) {
        List<SharedExpense> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Complex query to get shared expenses and JOIN with Users to get the person's name
        // We look for rows where the logged-in user is either the payer or the owed person
        String query = "SELECT s.*, u.name as person_name FROM SharedExpenses s " +
                      "LEFT JOIN Users u ON (s.payer_id = u.user_id AND s.owes_to_id = ?) " +
                      "OR (s.owes_to_id = u.user_id AND s.payer_id = ?) " +
                      "WHERE s.payer_id = ? OR s.owes_to_id = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(userId), String.valueOf(userId), String.valueOf(userId)});
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                SharedExpense se = new SharedExpense();
                se.setSharedId(cursor.getInt(cursor.getColumnIndexOrThrow("shared_id")));
                se.setExpenseId(cursor.getInt(cursor.getColumnIndexOrThrow("expense_id")));
                se.setPayerId(cursor.getInt(cursor.getColumnIndexOrThrow("payer_id")));
                se.setOwesToId(cursor.getInt(cursor.getColumnIndexOrThrow("owes_to_id")));
                se.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("amount")));
                se.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                se.setPersonName(cursor.getString(cursor.getColumnIndexOrThrow("person_name")));
                list.add(se);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public void settleSharedExpense(int sharedId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", "PAID");
        db.update("SharedExpenses", values, "shared_id = ?", new String[]{String.valueOf(sharedId)});
    }

    public double getOwedAmount(int userId) {
        // Amount others owe to this user (Must be PENDING)
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM SharedExpenses WHERE owes_to_id = ? AND payer_id != ? AND status = 'PENDING'", 
                new String[]{String.valueOf(userId), String.valueOf(userId)});
        double amount = 0;
        if (cursor != null && cursor.moveToFirst()) {
            amount = cursor.getDouble(0);
        }
        if (cursor != null) cursor.close();
        return amount;
    }

    public double getOweAmount(int userId) {
        // Amount this user owes to others (Must be PENDING)
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM SharedExpenses WHERE payer_id = ? AND owes_to_id != ? AND status = 'PENDING'", 
                new String[]{String.valueOf(userId), String.valueOf(userId)});
        double amount = 0;
        if (cursor != null && cursor.moveToFirst()) {
            amount = cursor.getDouble(0);
        }
        if (cursor != null) cursor.close();
        return amount;
    }

    // --- Budget Methods ---
    public long addBudget(Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", budget.getUserId());
        values.put("category", budget.getCategory());
        values.put("limit_amount", budget.getLimitAmount());
        values.put("period", budget.getPeriod());
        long result = db.insert("Budgets", null, values);
        if (result == -1) Log.e(TAG, "Failed to insert budget");
        return result;
    }

    public void updateBudget(Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("limit_amount", budget.getLimitAmount());
        values.put("period", budget.getPeriod());
        db.update("Budgets", values, "user_id = ? AND category = ?", 
                new String[]{String.valueOf(budget.getUserId()), budget.getCategory()});
    }

    public double getTotalBudget(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(limit_amount) FROM Budgets WHERE user_id = ?", new String[]{String.valueOf(userId)});
        double total = 0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        if (cursor != null) cursor.close();
        return total;
    }

    public Budget getBudgetByCategory(int userId, String category) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Budget budget = null;
        Cursor cursor = db.query("Budgets", null, "user_id = ? AND category = ?", 
                new String[]{String.valueOf(userId), category}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            budget = new Budget();
            budget.setBudgetId(cursor.getInt(cursor.getColumnIndexOrThrow("budget_id")));
            budget.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
            budget.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
            budget.setLimitAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("limit_amount")));
            budget.setPeriod(cursor.getString(cursor.getColumnIndexOrThrow("period")));
            cursor.close();
        }
        return budget;
    }
}
