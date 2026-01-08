package com.example.hinurse20;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.hinurse20.services.GeminiAiStudioClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SmartTriageActivity extends BaseActivity {
    private AutoCompleteTextView autoCompleteSymptoms;
    private Button buttonAddSymptom, buttonAnalyze, buttonBookAppointment, buttonReset;
    private LinearLayout layoutSymptomsList, layoutAnalysisResults;
    private TextView textViewAnalysis, textViewRecommendation, textViewSelfCareTips, textViewDisclaimer;
    private ProgressBar progressBar;
    private Switch switchAiEnhancements;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private List<String> selectedSymptoms;
    private com.example.hinurse20.models.SymptomAnalysis currentAnalysis;
    private GeminiAiStudioClient aiClient = new GeminiAiStudioClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_triage);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();
        setupAutoComplete();
        setupClickListeners();
        selectedSymptoms = new ArrayList<>();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }
        
        autoCompleteSymptoms = findViewById(R.id.autoCompleteSymptoms);
        buttonAddSymptom = findViewById(R.id.buttonAddSymptom);
        buttonAnalyze = findViewById(R.id.buttonAnalyze);
        buttonBookAppointment = findViewById(R.id.buttonBookAppointment);
        buttonReset = findViewById(R.id.buttonReset);
        layoutSymptomsList = findViewById(R.id.layoutSymptomsList);
        layoutAnalysisResults = findViewById(R.id.layoutAnalysisResults);
        textViewAnalysis = findViewById(R.id.textViewAnalysis);
        textViewRecommendation = findViewById(R.id.textViewRecommendation);
        textViewSelfCareTips = findViewById(R.id.textViewSelfCareTips);
        textViewDisclaimer = findViewById(R.id.textViewDisclaimer);
        progressBar = findViewById(R.id.progressBar);
        switchAiEnhancements = findViewById(R.id.switchAiEnhancements);

        if (switchAiEnhancements != null) {
            SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            boolean enabled = prefs.getBoolean("ai_enhancements_enabled", false);
            switchAiEnhancements.setChecked(enabled);
            switchAiEnhancements.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("ai_enhancements_enabled", isChecked).apply();
                if (isChecked) {
                    Toast.makeText(SmartTriageActivity.this, "AI enhancements enabled (beta)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SmartTriageActivity.this, "AI enhancements disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) return false;
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void setupAutoComplete() {
        List<String> availableSymptoms = com.example.hinurse20.services.SmartTriageService.getAvailableSymptoms();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, availableSymptoms);
        autoCompleteSymptoms.setAdapter(adapter);
        autoCompleteSymptoms.setThreshold(1);
    }

    private void setupClickListeners() {
        buttonAddSymptom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSymptom();
            }
        });

        buttonAnalyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzeSymptoms();
            }
        });

        buttonBookAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookAppointment();
            }
        });

        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetForm();
            }
        });
    }

    private void addSymptom() {
        String symptom = autoCompleteSymptoms.getText().toString().trim();
        if (symptom.isEmpty()) {
            autoCompleteSymptoms.setError("Please enter a symptom");
            autoCompleteSymptoms.requestFocus();
            return;
        }

        if (selectedSymptoms.contains(symptom.toLowerCase())) {
            Toast.makeText(this, "Symptom already added", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedSymptoms.add(symptom.toLowerCase());
        updateSymptomsList();
        autoCompleteSymptoms.setText("");
    }

    private void updateSymptomsList() {
        layoutSymptomsList.removeAllViews();
        
        for (String symptom : selectedSymptoms) {
            final String symptomToRemove = symptom; // Need final for inner class
            
            // Create horizontal layout for each symptom item
            LinearLayout symptomItemLayout = new LinearLayout(this);
            symptomItemLayout.setOrientation(LinearLayout.HORIZONTAL);
            symptomItemLayout.setPadding(16, 12, 16, 12);
            symptomItemLayout.setBackground(getResources().getDrawable(R.drawable.edit_text_background));
            
            // Set margin between items
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, 8);
            symptomItemLayout.setLayoutParams(layoutParams);
            
            // Symptom text view - capitalize first letter for display
            String displaySymptom = symptom.length() > 0 
                    ? symptom.substring(0, 1).toUpperCase() + symptom.substring(1) 
                    : symptom;
            TextView symptomView = new TextView(this);
            symptomView.setText("• " + displaySymptom);
            symptomView.setTextSize(16);
            symptomView.setTextColor(getResources().getColor(R.color.purple));
            symptomView.setPadding(0, 0, 12, 0);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            symptomView.setLayoutParams(textParams);
            
            // Remove button (X icon)
            ImageButton removeButton = new ImageButton(this);
            removeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            removeButton.setBackground(null);
            removeButton.setColorFilter(getResources().getColor(R.color.purple));
            removeButton.setContentDescription("Remove " + symptom);
            removeButton.setPadding(8, 8, 8, 8);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            removeButton.setLayoutParams(buttonParams);
            
            // Set click listener to remove this specific symptom
            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeSymptom(symptomToRemove);
                }
            });
            
            // Add views to layout
            symptomItemLayout.addView(symptomView);
            symptomItemLayout.addView(removeButton);
            
            // Add the symptom item layout to the symptoms list
            layoutSymptomsList.addView(symptomItemLayout);
        }
    }
    
    /**
     * Remove a specific symptom from the list
     */
    private void removeSymptom(String symptom) {
        if (selectedSymptoms.remove(symptom)) {
            updateSymptomsList();
            
            // Capitalize first letter for display
            String displaySymptom = symptom.substring(0, 1).toUpperCase() + symptom.substring(1);
            Toast.makeText(this, "Removed: " + displaySymptom, Toast.LENGTH_SHORT).show();
            
            // If symptoms list is empty and analysis was done, hide results
            if (selectedSymptoms.isEmpty()) {
                if (layoutAnalysisResults != null) {
                    layoutAnalysisResults.setVisibility(View.GONE);
                }
                currentAnalysis = null;
            }
        }
    }

    private void analyzeSymptoms() {
        if (selectedSymptoms.isEmpty()) {
            Toast.makeText(this, "Please add at least one symptom", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutAnalysisResults.setVisibility(View.GONE);
        boolean useAi = (switchAiEnhancements != null && switchAiEnhancements.isChecked());
        if (useAi) {
            if (!isOnline()) {
                Toast.makeText(this, "AI unavailable, using local analysis (offline)", Toast.LENGTH_SHORT).show();
                currentAnalysis = com.example.hinurse20.services.SmartTriageService.analyzeSymptoms(selectedSymptoms);
                saveAnalysisToFirestore();
                displayAnalysisResults();
                progressBar.setVisibility(View.GONE);
                layoutAnalysisResults.setVisibility(View.VISIBLE);
                return;
            }
            if (BuildConfig.AI_STUDIO_API_KEY == null || BuildConfig.AI_STUDIO_API_KEY.isEmpty()) {
                Toast.makeText(this, "AI key missing; using local analysis", Toast.LENGTH_SHORT).show();
                currentAnalysis = com.example.hinurse20.services.SmartTriageService.analyzeSymptoms(selectedSymptoms);
                saveAnalysisToFirestore();
                displayAnalysisResults();
                progressBar.setVisibility(View.GONE);
                layoutAnalysisResults.setVisibility(View.VISIBLE);
                return;
            }
            aiClient.assess(
                    BuildConfig.AI_STUDIO_API_KEY,
                    null, // age
                    null, // sex
                    selectedSymptoms,
                    null, // onset
                    null, // vitals
                    null, // meds
                    null, // conditions
                    null, // notes
                    new GeminiAiStudioClient.CallbackResult() {
                        @Override
                        public void onSuccess(com.example.hinurse20.models.SymptomAnalysis analysis) {
                            runOnUiThread(() -> {
                                currentAnalysis = analysis;
                                saveAnalysisToFirestore();
                                displayAnalysisResults();
                                progressBar.setVisibility(View.GONE);
                                layoutAnalysisResults.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                String msg = e != null && e.getMessage() != null ? (": " + e.getMessage()) : "";
                                Toast.makeText(SmartTriageActivity.this, "AI unavailable, using local analysis" + msg, Toast.LENGTH_SHORT).show();
                                currentAnalysis = com.example.hinurse20.services.SmartTriageService.analyzeSymptoms(selectedSymptoms);
                                saveAnalysisToFirestore();
                                displayAnalysisResults();
                                progressBar.setVisibility(View.GONE);
                                layoutAnalysisResults.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onInfo(String message) {
                            runOnUiThread(() -> Toast.makeText(SmartTriageActivity.this, message, Toast.LENGTH_SHORT).show());
                        }
                    }
            );
        } else {
            currentAnalysis = com.example.hinurse20.services.SmartTriageService.analyzeSymptoms(selectedSymptoms);
            saveAnalysisToFirestore();
            displayAnalysisResults();
            progressBar.setVisibility(View.GONE);
            layoutAnalysisResults.setVisibility(View.VISIBLE);
        }
    }

    private void saveAnalysisToFirestore() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        String analysisId = UUID.randomUUID().toString();
        
        currentAnalysis.setAnalysisId(analysisId);
        currentAnalysis.setUserId(userId);

        db.collection("symptom_analyses")
                .document(analysisId)
                .set(currentAnalysis)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SmartTriageActivity.this, "Analysis saved", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void displayAnalysisResults() {
        if (currentAnalysis == null) return;

        // Display severity and urgency
        String analysisText = "Severity: " + currentAnalysis.getSeverityDisplayText() + "\n" +
                "Urgency: " + currentAnalysis.getUrgencyDisplayText() + "\n" +
                "Priority Level: " + currentAnalysis.getPriorityLevel();
        textViewAnalysis.setText(analysisText);

        // Display recommendation
        String recommendationText = "Recommendation: " + currentAnalysis.getRecommendationDisplayText() + "\n" +
                "Suggested Timeframe: " + currentAnalysis.getSuggestedTimeframe();
        textViewRecommendation.setText(recommendationText);

        // Display self-care tips
        if (currentAnalysis.getSelfCareTips() != null && !currentAnalysis.getSelfCareTips().isEmpty()) {
            textViewSelfCareTips.setText("Self-Care Tips:\n• " + currentAnalysis.getSelfCareTips());
        } else {
            textViewSelfCareTips.setText("No specific self-care tips available.");
        }

        // Display AI recommendation disclaimer
        if (textViewDisclaimer != null) {
            textViewDisclaimer.setText("⚠️ AI Recommendation Notice:\nThis is an AI-generated recommendation based on your symptoms. " +
                    "Please consult with a medical professional for accurate diagnosis and treatment.");
            textViewDisclaimer.setVisibility(View.VISIBLE);
        }

        // Show/hide appointment booking button
        if (currentAnalysis.isRequiresAppointment()) {
            buttonBookAppointment.setVisibility(View.VISIBLE);
        } else {
            buttonBookAppointment.setVisibility(View.GONE);
        }
    }

    private void bookAppointment() {
        if (currentAnalysis == null) {
            Toast.makeText(this, "Please analyze symptoms first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(SmartTriageActivity.this, AppointmentBookingActivity.class);
        intent.putExtra("priority_level", currentAnalysis.getPriorityLevel());
        intent.putExtra("symptoms", String.join(", ", selectedSymptoms));
        intent.putExtra("urgency", currentAnalysis.getUrgency());
        startActivity(intent);
    }

    /**
     * Reset the form to initial state
     * Note: Individual symptoms can be removed using the X button on each symptom item
     */
    private void resetForm() {
        // Clear selected symptoms
        selectedSymptoms.clear();
        
        // Clear symptom input field
        if (autoCompleteSymptoms != null) {
            autoCompleteSymptoms.setText("");
            autoCompleteSymptoms.setError(null);
        }
        
        // Clear symptoms list view
        if (layoutSymptomsList != null) {
            layoutSymptomsList.removeAllViews();
        }
        
        // Hide analysis results
        if (layoutAnalysisResults != null) {
            layoutAnalysisResults.setVisibility(View.GONE);
        }
        
        // Reset current analysis
        currentAnalysis = null;
        
        // Reset analysis text views to default
        if (textViewAnalysis != null) {
            textViewAnalysis.setText("Analysis results will appear here...");
        }
        if (textViewRecommendation != null) {
            textViewRecommendation.setText("Recommendation will appear here...");
        }
        if (textViewSelfCareTips != null) {
            textViewSelfCareTips.setText("Self-care tips will appear here...");
        }
        if (textViewDisclaimer != null) {
            textViewDisclaimer.setVisibility(View.GONE);
        }
        
        // Hide book appointment button
        if (buttonBookAppointment != null) {
            buttonBookAppointment.setVisibility(View.GONE);
        }
        
        // Hide progress bar
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        
        // Show confirmation message
        Toast.makeText(this, "Form reset successfully", Toast.LENGTH_SHORT).show();
    }
}









