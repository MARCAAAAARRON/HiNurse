package com.example.hinurse20;

import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.hinurse20.adapters.MedicalRecordAdapter;

public class MedicalRecordsActivity extends BaseActivity {
    private RecyclerView recyclerViewRecords;
    private AutoCompleteTextView autoCompleteNurse;
    private TextView textViewEmpty;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private MedicalRecordAdapter adapter;
    private List<MedicalRecordAdapter.MedicalRecordItem> allRecords = new ArrayList<>();
    private Map<String, String> nurseMap = new HashMap<>(); // nurseId -> nurseName
    private String selectedNurseId = null;
    private ListenerRegistration medicalRecordsListener;
    private int previousRecordCount = 0;
    private boolean isActivityVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_records);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        
        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);
        
        // Activity is visible after onCreate completes
        isActivityVisible = true;
        
        loadNurses();
        loadMedicalRecords();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }
        
        recyclerViewRecords = findViewById(R.id.recyclerViewRecords);
        autoCompleteNurse = findViewById(R.id.autoCompleteNurse);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        // Setup RecyclerView
        recyclerViewRecords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicalRecordAdapter();
        adapter.setOnRecordClickListener(record -> showRecordDetails(record));
        recyclerViewRecords.setAdapter(adapter);
    }

    private void loadNurses() {
        // Load all nurses from Firestore
        db.collection("users")
                .whereEqualTo("role", "nurse")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> nurseNames = new ArrayList<>();
                        nurseNames.add("All Nurses"); // First option to show all records
                        nurseMap.put("", ""); // Empty ID for "All Nurses"

                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String nurseId = doc.getId();
                            String name = doc.getString("name");
                            if (name == null || name.trim().isEmpty()) {
                                String first = doc.getString("firstName");
                                String last = doc.getString("lastName");
                                name = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                            }
                            if (name == null || name.isEmpty()) {
                                name = "Nurse";
                            }
                            nurseNames.add(name);
                            nurseMap.put(name, nurseId);
                        }

                        // Setup AutoCompleteTextView
                        android.widget.ArrayAdapter<String> nurseAdapter = new android.widget.ArrayAdapter<>(
                                this, android.R.layout.simple_dropdown_item_1line, nurseNames);
                        autoCompleteNurse.setAdapter(nurseAdapter);
                        autoCompleteNurse.setText("All Nurses", false);
                        autoCompleteNurse.setOnItemClickListener((parent, view, position, id) -> {
                            String selectedNurseName = nurseNames.get(position);
                            selectedNurseId = nurseMap.get(selectedNurseName);
                            filterRecordsByNurse();
                        });
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        previousRecordCount = allRecords != null ? allRecords.size() : 0;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (medicalRecordsListener != null) {
            medicalRecordsListener.remove();
            medicalRecordsListener = null;
        }
    }

    private void loadMedicalRecords() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to view medical records.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        // Remove existing listener if any
        if (medicalRecordsListener != null) {
            medicalRecordsListener.remove();
        }

        // Use snapshot listener for real-time updates
        medicalRecordsListener = db.collection("medicalRecords")
                .whereEqualTo("patientId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            String msg = e.getMessage();
                            Toast.makeText(MedicalRecordsActivity.this, "Failed to load medical records: " + msg, Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        // Load all records
                        loadRecordsFromSnapshot(snapshots);
                    }
                });
    }
    
    private void loadRecordsFromSnapshot(QuerySnapshot snapshots) {
        if (snapshots != null) {
            int currentRecordCount = snapshots.size();
            
            // Check for new records (only if count increased and activity is not visible)
            if (currentRecordCount > previousRecordCount && !isActivityVisible) {
                // Find the newest record
                if (!snapshots.isEmpty()) {
                    // Documents are typically ordered by creation, but we'll check the last added
                    DocumentSnapshot newestDoc = null;
                    Date newestDate = null;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Object dateObj = doc.get("date");
                        Date docDate = null;
                        if (dateObj instanceof Date) {
                            docDate = (Date) dateObj;
                        } else if (dateObj instanceof com.google.firebase.Timestamp) {
                            docDate = ((com.google.firebase.Timestamp) dateObj).toDate();
                        }
                        if (newestDate == null || (docDate != null && docDate.after(newestDate))) {
                            newestDate = docDate;
                            newestDoc = doc;
                        }
                    }
                    
                    if (newestDoc != null) {
                        String nurseName = newestDoc.getString("nurseName");
                        if (nurseName == null || nurseName.isEmpty()) {
                            nurseName = newestDoc.getString("doctorName");
                        }
                        if (nurseName == null || nurseName.isEmpty()) {
                            nurseName = "Nurse";
                        }
                        String type = newestDoc.getString("type");
                        if (type == null || type.isEmpty()) {
                            type = "Medical Record";
                        }
                        NotificationHelper.showMedicalRecordNotification(
                                MedicalRecordsActivity.this,
                                "New Medical Record",
                                "A new " + type + " from " + nurseName + " has been added to your records.");
                    }
                }
            }
            
            previousRecordCount = currentRecordCount;
            
            allRecords.clear();
            
            for (DocumentSnapshot document : snapshots.getDocuments()) {
                String recordId = document.getId();
                String nurseName = document.getString("nurseName");
                String doctorName = document.getString("doctorName");
                String doctorId = document.getString("doctorId");
                String type = document.getString("type");
                String diagnosis = document.getString("diagnosis");
                String treatment = document.getString("treatment");
                String status = document.getString("status");
                String priority = document.getString("priority");
                String notes = document.getString("notes");
                String patientId = document.getString("patientId");
                String patientName = document.getString("patientName");

                // Get date field
                Date recordDate = null;
                Object dateObj = document.get("date");
                if (dateObj instanceof Date) {
                    recordDate = (Date) dateObj;
                } else if (dateObj instanceof Timestamp) {
                    recordDate = ((Timestamp) dateObj).toDate();
                }

                if (recordDate == null) {
                    Object updatedAtObj = document.get("updatedAt");
                    if (updatedAtObj instanceof Date) {
                        recordDate = (Date) updatedAtObj;
                    } else if (updatedAtObj instanceof Timestamp) {
                        recordDate = ((Timestamp) updatedAtObj).toDate();
                    }
                }

                if (recordDate == null) {
                    Object createdAtObj = document.get("createdAt");
                    if (createdAtObj instanceof Date) {
                        recordDate = (Date) createdAtObj;
                    } else if (createdAtObj instanceof Timestamp) {
                        recordDate = ((Timestamp) createdAtObj).toDate();
                    }
                }

                // If no nurse name but we have doctorId, use doctorName
                if ((nurseName == null || nurseName.isEmpty()) && doctorName != null && !doctorName.isEmpty()) {
                    nurseName = doctorName;
                }

                MedicalRecordAdapter.MedicalRecordItem record = new MedicalRecordAdapter.MedicalRecordItem(
                        recordId, nurseName, doctorName, doctorId, recordDate != null ? recordDate : new Date(0),
                        type, diagnosis, treatment, status, priority, notes, patientId, patientName);
                allRecords.add(record);
            }

            // Sort by date descending
            Collections.sort(allRecords, (a, b) -> b.getRecordDate().compareTo(a.getRecordDate()));

            filterRecordsByNurse();
        }
    }

    private void filterRecordsByNurse() {
        List<MedicalRecordAdapter.MedicalRecordItem> filteredRecords = new ArrayList<>();

        if (selectedNurseId == null || selectedNurseId.isEmpty()) {
            // Show all records
            filteredRecords.addAll(allRecords);
        } else {
            // Filter by selected nurse - need to match by name since we don't store doctorId in the item
            String selectedNurseName = null;
            for (Map.Entry<String, String> entry : nurseMap.entrySet()) {
                if (entry.getValue().equals(selectedNurseId)) {
                    selectedNurseName = entry.getKey();
                    break;
                }
            }

            if (selectedNurseName != null) {
                for (MedicalRecordAdapter.MedicalRecordItem record : allRecords) {
                    String recordNurseName = record.getNurseName();
                    String recordDoctorName = record.getDoctorName();
                    String recordDoctorId = record.getDoctorId();
                    
                    // Check if record matches selected nurse by ID (most reliable) or by name
                    boolean matches = false;
                    if (recordDoctorId != null && recordDoctorId.equals(selectedNurseId)) {
                        matches = true;
                    } else if ((recordNurseName != null && recordNurseName.equalsIgnoreCase(selectedNurseName)) ||
                               (recordDoctorName != null && recordDoctorName.equalsIgnoreCase(selectedNurseName))) {
                        matches = true;
                    }
                    
                    if (matches) {
                        filteredRecords.add(record);
                    }
                }
            }
        }

        adapter.setRecords(filteredRecords);
        
        // Show/hide empty message
        if (filteredRecords.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            recyclerViewRecords.setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            recyclerViewRecords.setVisibility(View.VISIBLE);
        }
    }

    private void showRecordDetails(MedicalRecordAdapter.MedicalRecordItem record) {
        // Create a dialog or bottom sheet to show full record details
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_medical_record_details, null);
        
        TextView textNurseName = dialogView.findViewById(R.id.textNurseName);
        TextView textDate = dialogView.findViewById(R.id.textDate);
        TextView textType = dialogView.findViewById(R.id.textType);
        TextView textDiagnosis = dialogView.findViewById(R.id.textDiagnosis);
        TextView textTreatment = dialogView.findViewById(R.id.textTreatment);
        TextView textStatus = dialogView.findViewById(R.id.textStatus);
        TextView textPriority = dialogView.findViewById(R.id.textPriority);
        TextView textNotes = dialogView.findViewById(R.id.textNotes);

        // Set values
        String nurseName = record.getNurseName();
        if (nurseName == null || nurseName.isEmpty()) {
            nurseName = record.getDoctorName();
        }
        if (nurseName == null || nurseName.isEmpty()) {
            nurseName = "Unknown";
        }
        textNurseName.setText(nurseName);

        if (record.getRecordDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            textDate.setText(sdf.format(record.getRecordDate()));
        } else {
            textDate.setText("N/A");
        }

        textType.setText(record.getType() != null ? record.getType() : "N/A");
        textDiagnosis.setText(record.getDiagnosis() != null ? record.getDiagnosis() : "N/A");
        textTreatment.setText(record.getTreatment() != null ? record.getTreatment() : "N/A");
        textStatus.setText(record.getStatus() != null ? record.getStatus() : "N/A");
        textPriority.setText(record.getPriority() != null ? record.getPriority() : "N/A");
        textNotes.setText(record.getNotes() != null && !record.getNotes().isEmpty() ? record.getNotes() : "No notes");

        builder.setView(dialogView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }
}
