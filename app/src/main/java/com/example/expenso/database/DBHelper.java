package com.example.expenso.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "expenso.db";
    private static final int DB_VERSION = 9;
    private static DBHelper instance;

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS Users (user_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, pin_hash TEXT, age INTEGER, profession TEXT, phone TEXT, avatar TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Expenses (expense_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, amount REAL, category TEXT, date TEXT, description TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS SharedExpenses (shared_id INTEGER PRIMARY KEY AUTOINCREMENT, expense_id INTEGER, payer_id INTEGER, owes_to_id INTEGER, amount REAL, status TEXT DEFAULT 'PENDING')");
        db.execSQL("CREATE TABLE IF NOT EXISTS Budgets (budget_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, category TEXT, limit_amount REAL, period TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Categories (category_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Goals (goal_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, name TEXT, target_amount REAL, saved_amount REAL, icon TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple upgrade strategy for now: drop and recreate (in production we would use ALTER TABLE or migrations)
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS Users");
            db.execSQL("DROP TABLE IF EXISTS Expenses");
            db.execSQL("DROP TABLE IF EXISTS SharedExpenses");
            db.execSQL("DROP TABLE IF EXISTS Budgets");
            db.execSQL("DROP TABLE IF EXISTS Categories");
            onCreate(db);
            return;
        }
        
        if (oldVersion < 4) {
             try {
                 db.execSQL("ALTER TABLE Users ADD COLUMN age INTEGER");
                 db.execSQL("ALTER TABLE Users ADD COLUMN profession TEXT");
                 db.execSQL("ALTER TABLE Users ADD COLUMN phone TEXT");
                 db.execSQL("CREATE TABLE IF NOT EXISTS SharedExpenses (shared_id INTEGER PRIMARY KEY AUTOINCREMENT, expense_id INTEGER, payer_id INTEGER, owes_to_id INTEGER, amount REAL)");
             } catch (Exception e) {}
        }
        if (oldVersion < 5) {
             try {
                 db.execSQL("ALTER TABLE SharedExpenses ADD COLUMN status TEXT DEFAULT 'PENDING'");
             } catch (Exception e) {}
        }
        if (oldVersion < 8) { // Handles any versions between 5 and 7
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS Goals (goal_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, name TEXT, target_amount REAL, saved_amount REAL, icon TEXT)");
            } catch (Exception e) {}
        }
        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE Users ADD COLUMN avatar TEXT");
            } catch (Exception e) {}
        }
    }
}
