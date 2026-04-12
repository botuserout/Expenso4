package com.example.expenso.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.expenso.models.Goal;
import java.util.ArrayList;
import java.util.List;

public class GoalDao {
    private DBHelper dbHelper;

    public GoalDao(Context context) {
        dbHelper = DBHelper.getInstance(context);
    }

    public long addGoal(Goal goal) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", goal.getUserId());
        values.put("name", goal.getName());
        values.put("target_amount", goal.getTargetAmount());
        values.put("saved_amount", goal.getSavedAmount());
        values.put("icon", goal.getIcon());
        return db.insert("Goals", null, values);
    }

    public List<Goal> getAllGoals(int userId) {
        List<Goal> goals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("Goals", null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, "goal_id DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Goal goal = new Goal();
                goal.setGoalId(cursor.getInt(cursor.getColumnIndexOrThrow("goal_id")));
                goal.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
                goal.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                goal.setTargetAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("target_amount")));
                goal.setSavedAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("saved_amount")));
                goal.setIcon(cursor.getString(cursor.getColumnIndexOrThrow("icon")));
                goals.add(goal);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return goals;
    }

    public boolean updateSavedAmount(int goalId, double addAmount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE Goals SET saved_amount = saved_amount + ? WHERE goal_id = ?", new Object[]{addAmount, goalId});
        return true;
    }

    public boolean deleteGoal(int goalId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("Goals", "goal_id = ?", new String[]{String.valueOf(goalId)}) > 0;
    }
}
