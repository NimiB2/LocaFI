package dev.nimrod.locafi;

import android.app.Application;
import com.google.firebase.FirebaseApp;

import dev.nimrod.locafi.models.User;

public class LocaFiApp extends Application {
    private static User currentUser;

    @Override
    public void onCreate() {
        super.onCreate();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        // Initialize with a default user ID for now
        currentUser = new User("default_user", "default@example.com");
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
}