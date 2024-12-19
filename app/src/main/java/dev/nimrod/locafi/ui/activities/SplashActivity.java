package dev.nimrod.locafi.ui.activities;


import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.services.MapDataService;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private ShapeableImageView logoImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initializeViews();
        initializeServiceConnection();
        startAnimation();
    }


    private void initializeViews() {
        logoImage = findViewById(R.id.splash_IMG_logo);
    }

    private void initializeServiceConnection() {
        startService(new Intent(this, MapDataService.class));
    }

    private void startAnimation() {
        logoImage.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .alpha(1f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        startMainActivity();
                    }

                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {
                    }
                });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}