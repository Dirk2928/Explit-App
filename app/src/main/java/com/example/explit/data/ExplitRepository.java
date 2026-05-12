package com.example.explit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.explit.model.Event;
import com.example.explit.model.ExpenseItem;
import com.example.explit.model.Group;
import com.example.explit.model.ItemAssignment;
import com.example.explit.model.Participant;
import com.example.explit.model.Receipt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExplitRepository {
    private final ExplitDbHelper dbHelper;

    // ---------------
    // ExplitRepository
    public ExplitRepository(Context context) {
        this.dbHelper = new ExplitDbHelper(context.getApplicationContext());
    }

    // ---------------
    // createGroup
    public long createGroup(String name, String category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("category", category);
        return db.insert("groups", null, values);
    }

    // ---------------
    // getGroups
    public List<Group> getGroups() {
        List<Group> groups = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, category FROM groups ORDER BY id DESC", null);
        try {
            while (cursor.moveToNext()) {
                groups.add(new Group(cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
            }
        } finally {
            cursor.close();
        }
        return groups;
    }

    // ---------------
    // addParticipant
    public long addParticipant(long groupId, String name, String nickname) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("name", name);
        values.put("nickname", nickname);
        return db.insert("participants", null, values);
    }

    // ---------------
    // getParticipants
    public List<Participant> getParticipants(long groupId) {
        List<Participant> participants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, group_id, name, nickname FROM participants WHERE group_id=? ORDER BY id ASC", new String[]{String.valueOf(groupId)});
        try {
            while (cursor.moveToNext()) {
                participants.add(new Participant(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3)));
            }
        } finally {
            cursor.close();
        }
        return participants;
    }

    // ---------------
    // createEvent
    public long createEvent(long groupId, String name, String currency, long paidByParticipantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("name", name);
        values.put("currency", currency);
        values.put("paid_by_participant_id", paidByParticipantId);
        return db.insert("events", null, values);
    }

    // ---------------
    // updateEvent
    public void updateEvent(long eventId, String name, String currency, long paidByParticipantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("currency", currency);
        values.put("paid_by_participant_id", paidByParticipantId);
        db.update("events", values, "id=?", new String[]{String.valueOf(eventId)});
    }

    // ---------------
    // getEvent
    public Event getEvent(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, group_id, name, currency, paid_by_participant_id FROM events WHERE id=?", new String[]{String.valueOf(eventId)});
        try {
            if (cursor.moveToFirst()) {
                return new Event(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4));
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    // ---------------
    // getEvents
    public List<Event> getEvents() {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, group_id, name, currency, paid_by_participant_id FROM events ORDER BY id DESC", null);
        try {
            while (cursor.moveToNext()) {
                events.add(new Event(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4)));
            }
        } finally {
            cursor.close();
        }
        return events;
    }

    // ---------------
    // addReceipt
    public long addReceipt(long eventId, String title, double tax, double tip, double serviceCharge, String photoPath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("event_id", eventId);
        values.put("title", title);
        values.put("tax", tax);
        values.put("tip", tip);
        values.put("service_charge", serviceCharge);
        values.put("photo_path", photoPath);
        return db.insert("receipts", null, values);
    }

    // ---------------
    // getReceipts
    public List<Receipt> getReceipts(long eventId) {
        List<Receipt> receipts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, event_id, title, tax, tip, service_charge, photo_path FROM receipts WHERE event_id=? ORDER BY id ASC", new String[]{String.valueOf(eventId)});
        try {
            while (cursor.moveToNext()) {
                receipts.add(new Receipt(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5), cursor.getString(6)));
            }
        } finally {
            cursor.close();
        }
        return receipts;
    }

    // ---------------
    // addExpenseItem
    public long addExpenseItem(long receiptId, String name, double amount, boolean shared, long payerParticipantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("receipt_id", receiptId);
        values.put("name", name);
        values.put("amount", amount);
        values.put("shared", shared ? 1 : 0);
        values.put("payer_participant_id", payerParticipantId);
        return db.insert("expense_items", null, values);
    }

    // ---------------
    // updateExpenseItem
    public void updateExpenseItem(long itemId, String name, double amount, boolean shared, long payerParticipantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("amount", amount);
        values.put("shared", shared ? 1 : 0);
        values.put("payer_participant_id", payerParticipantId);
        db.update("expense_items", values, "id=?", new String[]{String.valueOf(itemId)});
    }

    // ---------------
    // deleteExpenseItem
    public void deleteExpenseItem(long itemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("item_assignments", "item_id=?", new String[]{String.valueOf(itemId)});
        db.delete("expense_items", "id=?", new String[]{String.valueOf(itemId)});
    }

    // ---------------
    // getExpenseItemsForEvent
    public List<ExpenseItem> getExpenseItemsForEvent(long eventId) {
        List<ExpenseItem> items = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT ei.id, ei.receipt_id, ei.name, ei.amount, ei.shared, ei.payer_participant_id " +
                        "FROM expense_items ei INNER JOIN receipts r ON r.id = ei.receipt_id " +
                        "WHERE r.event_id=? ORDER BY ei.id ASC",
                new String[]{String.valueOf(eventId)}
        );
        try {
            while (cursor.moveToNext()) {
                items.add(new ExpenseItem(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getDouble(3), cursor.getInt(4) == 1, cursor.getLong(5)));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    // ---------------
    // replaceAssignments
    public void replaceAssignments(long itemId, List<ItemAssignment> assignments) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("item_assignments", "item_id=?", new String[]{String.valueOf(itemId)});
        for (ItemAssignment assignment : assignments) {
            ContentValues values = new ContentValues();
            values.put("item_id", itemId);
            values.put("participant_id", assignment.getParticipantId());
            if (assignment.getPercent() == null) {
                values.putNull("percent");
            } else {
                values.put("percent", assignment.getPercent());
            }
            db.insert("item_assignments", null, values);
        }
    }

    // ---------------
    // getAssignmentsByItem
    public Map<Long, List<ItemAssignment>> getAssignmentsByItem(long eventId) {
        Map<Long, List<ItemAssignment>> map = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT ia.item_id, ia.participant_id, ia.percent FROM item_assignments ia " +
                        "INNER JOIN expense_items ei ON ei.id = ia.item_id " +
                        "INNER JOIN receipts r ON r.id = ei.receipt_id WHERE r.event_id=?",
                new String[]{String.valueOf(eventId)}
        );
        try {
            while (cursor.moveToNext()) {
                long itemId = cursor.getLong(0);
                long participantId = cursor.getLong(1);
                Double percent = cursor.isNull(2) ? null : cursor.getDouble(2);
                map.computeIfAbsent(itemId, ignored -> new ArrayList<>()).add(new ItemAssignment(itemId, participantId, percent));
            }
        } finally {
            cursor.close();
        }
        return map;
    }

    // ---------------
    // getEventIdForReceipt
    public long getEventIdForReceipt(long receiptId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT event_id FROM receipts WHERE id=?", new String[]{String.valueOf(receiptId)});
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }
        return -1L;
    }
}
