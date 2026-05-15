package com.example.explit.util;

import android.content.Context;
import android.content.SharedPreferences;

public class CurrencyHelper {

    public static boolean shouldRound(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("explit_prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("round_decimals", false);
    }

    public static double roundIfNeeded(Context context, double amount) {
        if (shouldRound(context)) {
            return Math.floor(amount);
        }
        return amount;
    }
}