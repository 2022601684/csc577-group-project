package com.example.groupproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "MemoryGame.db";
    private static final int DATABASE_VERSION = 2; // Increment version

    // Table: Users
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    // Table: Nicknames (Now with score)
    private static final String TABLE_NICKNAMES = "nicknames";
    private static final String COLUMN_NICKNAME_ID = "id";
    private static final String COLUMN_NICKNAME = "nickname";
    private static final String COLUMN_USER_ID_FK = "user_id"; // Foreign key linking to users table
    private static final String COLUMN_SCORE = "score"; // New column for storing score

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createUsersTable);

        // Create Nicknames Table with Score Column
        String createNicknamesTable = "CREATE TABLE " + TABLE_NICKNAMES + " (" +
                COLUMN_NICKNAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NICKNAME + " TEXT, " +
                COLUMN_USER_ID_FK + " INTEGER, " +
                COLUMN_SCORE + " INTEGER DEFAULT 0, " + // Default score is 0
                "FOREIGN KEY(" + COLUMN_USER_ID_FK + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createNicknamesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Upgrade database by adding the score column to nicknames table if not already added
            String addScoreColumn = "ALTER TABLE " + TABLE_NICKNAMES + " ADD COLUMN " + COLUMN_SCORE + " INTEGER DEFAULT 0";
            db.execSQL(addScoreColumn);
        }
    }

    // ============================== USER AUTHENTICATION ==============================

    // Register a new user
    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1; // If insert fails, it returns -1
    }

    // Login User (Checks if credentials are correct)
    public boolean loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " +
                COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?", new String[]{username, password});

        boolean success = cursor.getCount() > 0;
        cursor.close();
        return success;
    }

    // Get User ID by Username (Used for CRUD operations on nicknames)
    public int getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_USER_ID + " FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ?", new String[]{username});

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(0);
            cursor.close();
            return userId;
        } else {
            cursor.close();
            return -1; // User not found
        }
    }

    // ============================== CRUD OPERATIONS FOR NICKNAMES ==============================

    // Add a nickname
    public boolean addNickname(String nickname, int userId) {
        SQLiteDatabase db = null;
        boolean result = false;

        try {
            db = this.getWritableDatabase();

            // Check if the database is open for writing
            if (db != null) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_NICKNAME, nickname);
                values.put(COLUMN_USER_ID_FK, userId);

                // Insert the nickname into the table
                long insertResult = db.insert(TABLE_NICKNAMES, null, values);

                // Check if the insert was successful
                if (insertResult != -1) {
                    result = true;
                    Log.d("Database", "Nickname added successfully. Insert ID: " + insertResult);
                } else {
                    Log.e("Database", "Failed to add nickname.");
                }
            }
        } catch (Exception e) {
            Log.e("Database", "Error adding nickname", e);
        } finally {
            if (db != null) {
                db.close(); // Ensure the database is closed after the operation
            }
        }

        return result;
    }


    // Get all nicknames for a user
    public Cursor getAllNicknames(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NICKNAMES + " WHERE " + COLUMN_USER_ID_FK + " = ?", new String[]{String.valueOf(userId)});

        // Debugging log to check cursor data
        if (cursor != null) {
            Log.d("DatabaseHelper", "Found " + cursor.getCount() + " records for user " + userId);
        } else {
            Log.e("DatabaseHelper", "Cursor is null or query failed");
        }

        return cursor;
    }

    // Update a nickname
    public boolean updateNickname(int nicknameId, String newNickname) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NICKNAME, newNickname);

        int rowsAffected = db.update(TABLE_NICKNAMES, values, COLUMN_NICKNAME_ID + " = ?", new String[]{String.valueOf(nicknameId)});
        return rowsAffected > 0;
    }

    // Delete a nickname
    public boolean deleteNickname(int nicknameId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_NICKNAMES, COLUMN_NICKNAME_ID + " = ?", new String[]{String.valueOf(nicknameId)});
        return rowsDeleted > 0;
    }

    // ============================== SCORE OPERATIONS ==============================

    // Set or update score for a nickname
    public boolean setScore(int nicknameId, int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCORE, score);

        int rowsAffected = db.update(TABLE_NICKNAMES, values, COLUMN_NICKNAME_ID + " = ?", new String[]{String.valueOf(nicknameId)});
        return rowsAffected > 0;
    }

    // Get the score of a specific nickname
    public int getScore(int nicknameId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_SCORE + " FROM " + TABLE_NICKNAMES + " WHERE " + COLUMN_NICKNAME_ID + " = ?", new String[]{String.valueOf(nicknameId)});

        if (cursor.moveToFirst()) {
            int score = cursor.getInt(0);
            cursor.close();
            return score;
        } else {
            cursor.close();
            return 0; // Default score if no record is found
        }
    }
}