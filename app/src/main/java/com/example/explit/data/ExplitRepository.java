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
import com.example.explit.model.SettlementStatus;
import com.example.explit.util.SplitCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExplitRepository {
    private final ExplitDbHelper dbHelper;

    public ExplitRepository(Context context) {
        this.dbHelper = new ExplitDbHelper(context.getApplicationContext());
    }

    public long createGroup(String name, String category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("category", category);
        return db.insert("groups", null, values);
    }

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

    public List<Group> searchGroups(String query) {
        List<Group> groups = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, category FROM groups WHERE name LIKE ? ORDER BY name ASC", new String[]{"%" + query + "%"});
        try {
            while (cursor.moveToNext()) {
                groups.add(new Group(cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
            }
        } finally {
            cursor.close();
        }
        return groups;
    }

    public long addParticipant(long groupId, String name, String nickname) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("name", name);
        values.put("nickname", nickname);
        return db.insert("participants", null, values);
    }

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

    public long createEvent(long groupId, String name, String currency, long paidByParticipantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("name", name);
        values.put("currency", currency);
        values.put("paid_by_participant_id", paidByParticipantId);
        return db.insert("events", null, values);
    }

    public void addEventParticipant(long eventId, long participantId, String name, String nickname) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("event_id", eventId);
        values.put("participant_id", participantId);
        values.put("name", name);
        values.put("nickname", nickname);
        db.insert("event_participants", null, values);
    }

    public List<Participant> getEventParticipants(long eventId) {
        List<Participant> participants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT participant_id, event_id, name, nickname FROM event_participants WHERE event_id=? ORDER BY participant_id ASC", new String[]{String.valueOf(eventId)});
        try {
            while (cursor.moveToNext()) {
                participants.add(new Participant(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3)));
            }
        } finally {
            cursor.close();
        }

        if (participants.isEmpty()) {
            Event event = getEvent(eventId);
            if (event != null) {
                List<Participant> groupParticipants = getParticipants(event.getGroupId());
                db = dbHelper.getWritableDatabase();
                for (Participant p : groupParticipants) {
                    ContentValues values = new ContentValues();
                    values.put("event_id", eventId);
                    values.put("participant_id", p.getId());
                    values.put("name", p.getName());
                    values.put("nickname", p.getNickname());
                    db.insert("event_participants", null, values);
                    participants.add(new Participant(p.getId(), eventId, p.getName(), p.getNickname()));
                }
            }
        }
        return participants;
    }

    public List<Event> getAllEvents() {
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

    public List<Event> getEventsByGroup(long groupId) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, group_id, name, currency, paid_by_participant_id FROM events WHERE group_id=? ORDER BY id DESC", new String[]{String.valueOf(groupId)});
        try {
            while (cursor.moveToNext()) {
                events.add(new Event(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4)));
            }
        } finally {
            cursor.close();
        }
        return events;
    }

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

    public void updateEvent(long eventId, String name, String currency, long paidByParticipantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("currency", currency);
        values.put("paid_by_participant_id", paidByParticipantId);
        db.update("events", values, "id=?", new String[]{String.valueOf(eventId)});
    }

    public List<Receipt> getReceipts(long eventId) {
        List<Receipt> receipts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, event_id, title, tax, tip, service_charge, photo_path FROM receipts WHERE event_id=?", new String[]{String.valueOf(eventId)});
        try {
            while (cursor.moveToNext()) {
                receipts.add(new Receipt(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5), cursor.getString(6)));
            }
        } finally {
            cursor.close();
        }
        return receipts;
    }

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

    public void updateReceiptDetails(long receiptId, double tax, double tip, double serviceCharge) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tax", tax);
        values.put("tip", tip);
        values.put("service_charge", serviceCharge);
        db.update("receipts", values, "id=?", new String[]{String.valueOf(receiptId)});
    }

    public void updateReceiptPhoto(long receiptId, String photoPath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("photo_path", photoPath);
        db.update("receipts", values, "id=?", new String[]{String.valueOf(receiptId)});
    }

    public List<ExpenseItem> getExpenseItemsForEvent(long eventId) {
        List<ExpenseItem> items = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT i.id, i.receipt_id, i.name, i.amount, i.shared, i.payer_participant_id " +
                "FROM expense_items i JOIN receipts r ON i.receipt_id = r.id " +
                "WHERE r.event_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(eventId)});
        try {
            while (cursor.moveToNext()) {
                items.add(new ExpenseItem(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getDouble(3), cursor.getInt(4) == 1, cursor.getLong(5)));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

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

    public void updateExpenseItem(long itemId, String name, double amount, boolean shared, long payerParticipantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("amount", amount);
        values.put("shared", shared ? 1 : 0);
        values.put("payer_participant_id", payerParticipantId);
        db.update("expense_items", values, "id=?", new String[]{String.valueOf(itemId)});
    }

    public void deleteExpenseItem(long itemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("item_assignments", "item_id=?", new String[]{String.valueOf(itemId)});
        db.delete("expense_items", "id=?", new String[]{String.valueOf(itemId)});
    }

    public void replaceAssignments(long itemId, List<ItemAssignment> assignments) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("item_assignments", "item_id=?", new String[]{String.valueOf(itemId)});
            for (ItemAssignment assignment : assignments) {
                ContentValues values = new ContentValues();
                values.put("item_id", itemId);
                values.put("participant_id", assignment.getParticipantId());
                values.put("percent", assignment.getPercent());
                db.insert("item_assignments", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Map<Long, List<ItemAssignment>> getAssignmentsByItem(long eventId) {
        Map<Long, List<ItemAssignment>> map = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT a.item_id, a.participant_id, a.percent " +
                "FROM item_assignments a JOIN expense_items i ON a.item_id = i.id " +
                "JOIN receipts r ON i.receipt_id = r.id " +
                "WHERE r.event_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(eventId)});
        try {
            while (cursor.moveToNext()) {
                long itemId = cursor.getLong(0);
                ItemAssignment assignment = new ItemAssignment(itemId, cursor.getLong(1), cursor.getDouble(2));
                if (!map.containsKey(itemId)) {
                    map.put(itemId, new ArrayList<>());
                }
                map.get(itemId).add(assignment);
            }
        } finally {
            cursor.close();
        }
        return map;
    }

    // ---- Settlement Status ----

    public void saveSettlements(long eventId, List<SplitCalculator.Settlement> settlements) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Map<String, Integer> existingPaid = new HashMap<>();
            Cursor cursor = db.rawQuery("SELECT from_id, to_id, paid FROM settlement_status WHERE event_id=?", new String[]{String.valueOf(eventId)});
            while (cursor.moveToNext()) {
                existingPaid.put(cursor.getLong(0) + "_" + cursor.getLong(1), cursor.getInt(2));
            }
            cursor.close();

            db.delete("settlement_status", "event_id=?", new String[]{String.valueOf(eventId)});
            for (SplitCalculator.Settlement s : settlements) {
                ContentValues values = new ContentValues();
                values.put("event_id", eventId);
                values.put("from_id", s.fromId);
                values.put("to_id", s.toId);
                values.put("amount", s.amount);
                String key = s.fromId + "_" + s.toId;
                values.put("paid", existingPaid.getOrDefault(key, 0));
                db.insert("settlement_status", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void setSettlementPaid(long eventId, long fromId, long toId, boolean paid) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("paid", paid ? 1 : 0);
        db.update("settlement_status", values, "event_id=? AND from_id=? AND to_id=?", new String[]{String.valueOf(eventId), String.valueOf(fromId), String.valueOf(toId)});
    }

    public List<SettlementStatus> getUnpaidSettlements() {
        List<SettlementStatus> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT ss.event_id, ss.from_id, ss.to_id, ss.amount, e.name as event_name, e.group_id, g.name as group_name " +
                "FROM settlement_status ss JOIN events e ON ss.event_id = e.id JOIN groups g ON e.group_id = g.id " +
                "WHERE ss.paid = 0 ORDER BY e.id DESC", null);
        try {
            while (cursor.moveToNext()) {
                list.add(new SettlementStatus(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2), cursor.getDouble(3), cursor.getString(4), cursor.getLong(5), cursor.getString(6)));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public Map<String, Integer> getSettlementPaidStatus(long eventId) {
        Map<String, Integer> map = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT from_id, to_id, paid FROM settlement_status WHERE event_id=?", new String[]{String.valueOf(eventId)});
        try {
            while (cursor.moveToNext()) {
                map.put(cursor.getLong(0) + "_" + cursor.getLong(1), cursor.getInt(2));
            }
        } finally {
            cursor.close();
        }
        return map;
    }

    public boolean hasUnpaidSettlements(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM settlement_status WHERE event_id=? AND paid=0", new String[]{String.valueOf(eventId)});
        try {
            if (cursor.moveToFirst()) return cursor.getInt(0) > 0;
        } finally {
            cursor.close();
        }
        return false;
    }

    public boolean groupHasUnpaidSettlements(long groupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM settlement_status ss JOIN events e ON ss.event_id = e.id WHERE e.group_id=? AND ss.paid=0", new String[]{String.valueOf(groupId)});
        try {
            if (cursor.moveToFirst()) return cursor.getInt(0) > 0;
        } finally {
            cursor.close();
        }
        return false;
    }
}