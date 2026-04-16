package com.example.expenso.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.expenso.models.User;

public class UserDao {
    private DBHelper dbHelper;

    public UserDao(Context context) {
        dbHelper = DBHelper.getInstance(context);
    }

    public int addUser(String name, String pinHash) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("pin_hash", pinHash);
        return (int) db.insert("Users", null, values);
    }

    public int getUserIdByPin(String pinHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("Users", new String[]{"user_id"}, "pin_hash = ?", 
                new String[]{pinHash}, null, null, null);
        
        int userId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(0);
            cursor.close();
        }
        return userId;
    }

    public void updateUserProfile(int userId, String name, int age, String profession, String phone, String avatar) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("age", age);
        values.put("profession", profession);
        values.put("phone", phone);
        values.put("avatar", avatar);
        db.update("Users", values, "user_id = ?", new String[]{String.valueOf(userId)});
    }

    public boolean updateUserPin(int userId, String newPinHash) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pin_hash", newPinHash);
        int rows = db.update("Users", values, "user_id = ?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public boolean checkUserExists() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT count(*) FROM Users", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }

    public int getUserIdByName(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("Users", new String[]{"user_id"}, "name = ?", 
                new String[]{name}, null, null, null);
        int userId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(0);
            cursor.close();
        }
        return userId;
    }

    public User getUserDetails(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("Users", null, "user_id = ?", 
                new String[]{String.valueOf(userId)}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            User user = new User();
            user.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            user.setAge(cursor.getInt(cursor.getColumnIndexOrThrow("age")));
            user.setProfession(cursor.getString(cursor.getColumnIndexOrThrow("profession")));
            user.setPhone(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
            user.setAvatar(cursor.getString(cursor.getColumnIndexOrThrow("avatar")));
            cursor.close();
            return user;
        }
        return null;
    }

    public int addFriendPlaceholder(String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        // Placeholder values for a non-authenticated user (friend)
        values.put("pin_hash", "FRIEND_PLACEHOLDER");
        return (int) db.insert("Users", null, values);
    }
}
