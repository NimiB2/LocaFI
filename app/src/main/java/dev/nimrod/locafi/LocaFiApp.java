package dev.nimrod.locafi;

import android.app.Application;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;

import java.util.UUID;

import dev.nimrod.locafi.models.User;

public class LocaFiApp extends Application {
    private static User currentUser;

    @Override
    public void onCreate() {
        super.onCreate();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        loadOrGenerateUserId();
    }

    private void loadOrGenerateUserId() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId == null) {
            // Generate a new random user ID
            userId = UUID.randomUUID().toString();
            prefs.edit().putString("userId", userId).apply();
        }

        currentUser = new User(userId);
    }


    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
}