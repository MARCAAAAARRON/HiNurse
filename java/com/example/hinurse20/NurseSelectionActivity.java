package com.example.hinurse20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NurseSelectionActivity extends BaseActivity {
    private RecyclerView recyclerViewNurses;
    private ProgressBar progressBar;
    private TextView textViewEmpty;
    private ImageButton buttonBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private com.example.hinurse20.adapters.NurseAdapter adapter;
    private ListenerRegistration nursesReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_selection);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        recyclerViewNurses = findViewById(R.id.recyclerViewNurses);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        buttonBack = findViewById(R.id.buttonBack);

        // Setup RecyclerView
        adapter = new com.example.hinurse20.adapters.NurseAdapter();
        recyclerViewNurses.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNurses.setAdapter(adapter);

        // Setup click listener
        adapter.setOnNurseClickListener(new com.example.hinurse20.adapters.NurseAdapter.OnNurseClickListener() {
            @Override
            public void onNurseClick(String nurseId, String nurseName) {
                // Navigate to ChatActivity with nurse ID
                Intent intent = new Intent(NurseSelectionActivity.this, ChatActivity.class);
                intent.putExtra("nurseId", nurseId);
                intent.putExtra("nurseName", nurseName);
                startActivity(intent);
            }
        });

        // Setup back button
        if (buttonBack != null) {
            buttonBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // Check authentication
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to view nurses.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load nurses
        loadNurses();
    }

    private void loadNurses() {
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);

        if (nursesReg != null) {
            nursesReg.remove();
            nursesReg = null;
        }

        nursesReg = db.collection("users")
                .whereEqualTo("role", "nurse")
                .addSnapshotListener(new EventListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onEvent(com.google.firebase.firestore.QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        progressBar.setVisibility(View.GONE);
                        
                        if (e != null) {
                            String errorMessage = "Failed to load nurses: " + e.getMessage();
                            android.util.Log.e("NurseSelection", errorMessage, e);
                            Toast.makeText(NurseSelectionActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            textViewEmpty.setVisibility(View.VISIBLE);
                            return;
                        }

                        List<com.example.hinurse20.adapters.NurseAdapter.NurseItem> nurseList = new ArrayList<>();

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String id = doc.getString("userId");
                            if (id == null) id = doc.getString("uid");
                            if (id == null) id = doc.getId();

                            String name = doc.getString("name");
                            if (name == null || name.trim().isEmpty()) {
                                String first = doc.getString("firstName");
                                String last = doc.getString("lastName");
                                name = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                            }
                            if (name == null || name.isEmpty()) {
                                name = "Nurse";
                            }

                            String photoUrl = doc.getString("photoUrl");
                            String photoBase64 = doc.getString("photoBase64");
                            
                            // Default to online status (can be enhanced later with presence tracking)
                            boolean isOnline = true;

                            nurseList.add(new com.example.hinurse20.adapters.NurseAdapter.NurseItem(
                                    id, name, photoUrl, photoBase64, isOnline));
                        }

                        adapter.setNurses(nurseList);

                        if (nurseList.isEmpty()) {
                            textViewEmpty.setVisibility(View.VISIBLE);
                        } else {
                            textViewEmpty.setVisibility(View.GONE);
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (nursesReg != null) {
            nursesReg.remove();
            nursesReg = null;
        }
    }
}














