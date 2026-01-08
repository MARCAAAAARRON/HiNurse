package com.example.hinurse20;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainDashboardActivity extends BaseActivity {
    private static final int PERMISSION_REQUEST_NOTIFICATION = 1001;
    private TextView textViewGreeting;
    private ImageView avatarIcon;
    private GridLayout gridLayoutOptions;
    private CardView cardAskNurse, cardBookAppointment, cardHealthTips, cardMyRecords, cardMyProfile;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();
        setupClickListeners();
        loadUserData();
        
        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_NOTIFICATION);
            }
        }
        
        // Check for today's appointments
        AppointmentReminderHelper.checkTodaysAppointments(this);
        
        // Start monitoring chat messages for notifications
        ChatNotificationHelper.startMonitoring(this);
    }

    private void initializeViews() {
        textViewGreeting = findViewById(R.id.textViewGreeting);
        avatarIcon = findViewById(R.id.avatarIcon);
        gridLayoutOptions = findViewById(R.id.gridLayoutOptions);
        cardAskNurse = findViewById(R.id.cardAskNurse);
        cardBookAppointment = findViewById(R.id.cardBookAppointment);
        cardHealthTips = findViewById(R.id.cardHealthTips);
        cardMyRecords = findViewById(R.id.cardMyRecords);
        cardMyProfile = findViewById(R.id.cardMyProfile);
    }

    private void setupClickListeners() {
        // Avatar -> User profile
        if (avatarIcon != null) {
            avatarIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainDashboardActivity.this, UserProfileActivity.class));
                }
            });
        }

        cardAskNurse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainDashboardActivity.this, NurseSelectionActivity.class));
            }
        });

        cardBookAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainDashboardActivity.this, AppointmentBookingActivity.class));
            }
        });

        cardHealthTips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainDashboardActivity.this, HealthTipsActivity.class));
            }
        });

        cardMyRecords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainDashboardActivity.this, MedicalRecordsActivity.class));
            }
        });

        // Smart Triage option
        findViewById(R.id.cardSmartTriage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainDashboardActivity.this, SmartTriageActivity.class));
            }
        });

        // My Profile option
        if (cardMyProfile != null) {
            cardMyProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainDashboardActivity.this, UserProfileActivity.class));
                }
            });
        }

        // Bottom navigation

        findViewById(R.id.navChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainDashboardActivity.this, NurseSelectionActivity.class));
            }
        });

        findViewById(R.id.navAppointments).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainDashboardActivity.this, AppointmentBookingActivity.class));
            }
        });

        findViewById(R.id.navProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainDashboardActivity.this, UserProfileActivity.class));
            }
        });
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            // Load user profile from Firestore
            db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String firstName = document.getString("firstName");
                                    textViewGreeting.setText("👋 Hi, " + firstName + "!");
                                    
                                    // Load profile icon/picture
                                    String photoUrl = document.getString("photoUrl");
                                    String photoBase64 = document.getString("photoBase64");
                                    
                                    // Check if user has selected an icon
                                    Long selectedIconIndex = document.getLong("selectedIconIndex");
                                    if (selectedIconIndex != null && selectedIconIndex >= 0 && selectedIconIndex < ProfileIconHelper.getAllProfileIcons().size()) {
                                        // Use selected icon
                                        int iconRes = ProfileIconHelper.getProfileIconByIndex(selectedIconIndex.intValue());
                                        avatarIcon.setImageResource(iconRes);
                                    } else if (photoUrl != null && !photoUrl.isEmpty()) {
                                        // Use photo if available
                                        int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                        Glide.with(MainDashboardActivity.this)
                                                .load(photoUrl)
                                                .circleCrop()
                                                .placeholder(defaultIcon)
                                                .error(defaultIcon)
                                                .into(avatarIcon);
                                    } else if (photoBase64 != null && !photoBase64.isEmpty()) {
                                        try {
                                            byte[] bytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT);
                                            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                            avatarIcon.setImageBitmap(bmp);
                                        } catch (Exception ignore) {
                                            int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                            avatarIcon.setImageResource(defaultIcon);
                                        }
                                    } else {
                                        // Use default icon based on user ID
                                        int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                        avatarIcon.setImageResource(defaultIcon);
                                    }
                                } else {
                                    textViewGreeting.setText("👋 Hi, User!");
                                    // Set default icon
                                    int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                    avatarIcon.setImageResource(defaultIcon);
                                }
                            } else {
                                textViewGreeting.setText("👋 Hi, User!");
                                // Set default icon
                                int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                avatarIcon.setImageResource(defaultIcon);
                            }
                        }
                    });

        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data when returning to this activity to reflect icon changes
        loadUserData();
        // Check for today's appointments when user returns to dashboard
        AppointmentReminderHelper.checkTodaysAppointments(this);
        // Ensure chat monitoring is active
        ChatNotificationHelper.startMonitoring(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Keep chat monitoring active even when dashboard is destroyed
        // This allows notifications to work when app is in background or closed
        // Monitoring will persist until app is fully terminated or user logs out
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("MainDashboardActivity", "Notification permission granted");
            } else {
                android.util.Log.d("MainDashboardActivity", "Notification permission denied");
            }
        }
    }

}
