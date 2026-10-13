package lk.prabhath.fashionhub.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import lk.prabhath.fashionhub.activity.LoginActivity;

public class AuthHelper {

    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";

    public static boolean isLoggedIn(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        String email = prefs.getString(KEY_EMAIL, "");
        return loggedIn && !email.trim().isEmpty();
    }

    public static String getUserEmail(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, "");
    }

    public static String getUsername(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "");
    }

    public static void logout(Context context) {
        if (context == null) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public static void showLoginPrompt(Context context, String message) {
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle("Login Required")
                .setMessage(message != null ? message : "Please log in to continue.")
                .setPositiveButton("Log In", (dialog, which) -> {
                    Intent intent = new Intent(context, LoginActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
