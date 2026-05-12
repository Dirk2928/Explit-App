package com.example.explit.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ExplitDbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "explit.db";
    public static final int DB_VERSION = 1;

    // ---------------
    // ExplitDbHelper
    public ExplitDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ---------------
    // onCreate
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE groups (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT NOT NULL)");
        db.execSQL("CREATE TABLE participants (id INTEGER PRIMARY KEY AUTOINCREMENT, group_id INTEGER NOT NULL, name TEXT NOT NULL, nickname TEXT)");
        db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, group_id INTEGER NOT NULL, name TEXT NOT NULL, currency TEXT NOT NULL, paid_by_participant_id INTEGER DEFAULT -1)");
        db.execSQL("CREATE TABLE receipts (id INTEGER PRIMARY KEY AUTOINCREMENT, event_id INTEGER NOT NULL, title TEXT NOT NULL, tax REAL NOT NULL DEFAULT 0, tip REAL NOT NULL DEFAULT 0, service_charge REAL NOT NULL DEFAULT 0, photo_path TEXT)");
        db.execSQL("CREATE TABLE expense_items (id INTEGER PRIMARY KEY AUTOINCREMENT, receipt_id INTEGER NOT NULL, name TEXT NOT NULL, amount REAL NOT NULL, shared INTEGER NOT NULL DEFAULT 0, payer_participant_id INTEGER DEFAULT -1)");
        db.execSQL("CREATE TABLE item_assignments (item_id INTEGER NOT NULL, participant_id INTEGER NOT NULL, percent REAL, PRIMARY KEY (item_id, participant_id))");
    }

    // ---------------
    // onUpgrade
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS item_assignments");
        db.execSQL("DROP TABLE IF EXISTS expense_items");
        db.execSQL("DROP TABLE IF EXISTS receipts");
        db.execSQL("DROP TABLE IF EXISTS events");
        db.execSQL("DROP TABLE IF EXISTS participants");
        db.execSQL("DROP TABLE IF EXISTS groups");
        onCreate(db);
    }
}
