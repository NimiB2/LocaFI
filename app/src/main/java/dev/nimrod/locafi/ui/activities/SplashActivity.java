package dev.nimrod.locafi.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.nimrod.locafi.R;


@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final long ANIMATION_DURATION = 1500; // 1.5 seconds

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View logo = findViewById(R.id.splash_IMG_logo);

        logo.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(SplashActivity.this, GatewayActivity.class));
                        finish();
                    }
                })
                .start();
    }
}