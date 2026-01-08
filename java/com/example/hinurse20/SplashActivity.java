package com.example.hinurse20;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.hinurse20.services.CloudinaryService;

public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds
    private static final int PERMISSION_REQUEST_NOTIFICATION = 1001;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize Cloudinary
        CloudinaryService.initialize(this);
        
        // Create notification channel early
        NotificationHelper.createNotificationChannel(this);
        
        // Start chat monitoring early (works even if user not logged in yet)
        // Will be restarted in MainDashboardActivity when user is confirmed logged in
        if (mAuth.getCurrentUser() != null) {
            ChatNotificationHelper.startMonitoring(this);
        }
        
        // Delay navigation to allow splash screen to be visible
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToNextActivity();
            }
        }, SPLASH_DELAY);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("SplashActivity", "Notification permission granted");
            } else {
                android.util.Log.d("SplashActivity", "Notification permission denied");
            }
        }
    }

    private void navigateToNextActivity() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        Intent intent;
        if (currentUser != null) {
            // User is already signed in, go to main dashboard
            intent = new Intent(SplashActivity.this, MainDashboardActivity.class);
        } else {
            // User is not signed in, go to sign in activity
            intent = new Intent(SplashActivity.this, SignInActivity.class);
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
