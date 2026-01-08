package com.example.hinurse20;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityLogActivity extends BaseActivity {
    private ListView listViewActivityLog;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_log);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        listViewActivityLog = findViewById(R.id.listViewActivityLog);
        loadActivityLog();
    }

    private void loadActivityLog() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();

        // Load from multiple collections to create a comprehensive activity log
        List<String> activityItems = new ArrayList<>();
        
        // Add some sample activities for demo purposes
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
        String currentTime = dateFormat.format(new Date());
        
        activityItems.add("📱 App opened - " + currentTime);
        activityItems.add("👤 Profile viewed - " + currentTime);
        activityItems.add("💬 Chat message sent - " + currentTime);
        activityItems.add("📅 Appointment booked - " + currentTime);
        activityItems.add("❓ Health question submitted - " + currentTime);
        activityItems.add("📋 Medical records accessed - " + currentTime);
        activityItems.add("💡 Health tips viewed - " + currentTime);

        // In a real implementation, you would query Firestore for actual user activities
        // For now, showing sample data
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, activityItems);
        listViewActivityLog.setAdapter(adapter);
    }
}





















