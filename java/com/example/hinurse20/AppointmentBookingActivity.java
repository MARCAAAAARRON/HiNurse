package com.example.hinurse20;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppointmentBookingActivity extends BaseActivity {
    private CalendarView calendarView;
    private LinearLayout containerTimeSlots;
    private EditText editTextReason;
    private AutoCompleteTextView autoCompleteNurse;
    private Button buttonBookAppointment;
    private ImageButton buttonBack;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedDate;
    private String selectedTimeSlot;
    private View selectedTimeSlotView;
    private final List<String> nurseIds = new ArrayList<>();
    private final List<String> nurseNames = new ArrayList<>();
    private ListenerRegistration nursesReg;
    private ListenerRegistration myApptsReg;
    private LinearLayout containerMyAppointments;
    private TextView textNoAppointments;
    private int previousAppointmentCount = 0;
    private boolean isActivityVisible = false;
    private java.util.Map<String, com.example.hinurse20.models.Appointment> previousAppointments = new java.util.HashMap<>();
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_booking);

        // Initialize Firebase with error handling
        try {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            android.util.Log.e("AppointmentBooking", "Firebase initialization error: " + e.getMessage(), e);
            Toast.makeText(this, "Service initialization failed. Please try again.", Toast.LENGTH_LONG).show();
            // Continue anyway - some features might still work
        }

        // Initialize views
        initializeViews();
        setupCalendar();
        setupTimeSlots();
        setupSpinner();
        setupClickListeners();
        
        // Handle Smart Triage priority
        handleSmartTriagePriority();

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);
        
        // Check for today's appointments
        AppointmentReminderHelper.checkTodaysAppointments(this);
        
        // Activity is visible after onCreate completes
        isActivityVisible = true;

        if (db != null) {
        attachMyAppointmentsListener();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        // Check for today's appointments when user returns to appointment screen
        AppointmentReminderHelper.checkTodaysAppointments(this);
    }
    
                    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
    }
    
                    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nursesReg != null) {
            nursesReg.remove();
            nursesReg = null;
        }
        if (myApptsReg != null) {
            myApptsReg.remove();
            myApptsReg = null;
        }
    }

    private void initializeViews() {
        calendarView = findViewById(R.id.calendarView);
        buttonBack = findViewById(R.id.buttonBack);
        containerTimeSlots = findViewById(R.id.containerTimeSlots);
        editTextReason = findViewById(R.id.editTextReason);
        autoCompleteNurse = findViewById(R.id.autoCompleteNurse);
        buttonBookAppointment = findViewById(R.id.buttonBookAppointment);
        progressBar = findViewById(R.id.progressBar);
        containerMyAppointments = findViewById(R.id.containerMyAppointments);
        textNoAppointments = findViewById(R.id.textNoAppointments);
        
        // Setup back button
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }
    }

    private void setupCalendar() {
        if (calendarView == null) return;
        
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
                setupTimeSlots();
                styleSelectedDate(calendarView);
        });

        // Set minimum date to today
        calendarView.setMinDate(System.currentTimeMillis());
        
        // Style the CalendarView header after it is laid out
        calendarView.post(() -> {
            styleCalendarHeader(calendarView);
            styleSelectedDate(calendarView);
            calendarView.postDelayed(() -> {
                styleCalendarHeader(calendarView);
                styleSelectedDate(calendarView);
                calendarView.postDelayed(() -> styleCalendarHeader(calendarView), 200);
                }, 100);
        });

        // Re-apply styling when the calendar scrolls between months
        calendarView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            styleCalendarHeader(calendarView);
            styleSelectedDate(calendarView);
            calendarView.postDelayed(() -> {
                styleCalendarHeader(calendarView);
                styleSelectedDate(calendarView);
                calendarView.postDelayed(() -> styleCalendarHeader(calendarView), 200);
                }, 300);
        });
        
        // Set initial selected date to today
        Calendar today = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.getTime());
    }

    private void setupTimeSlots() {
        if (containerTimeSlots == null) return;
        containerTimeSlots.removeAllViews();
        selectedTimeSlotView = null;
        
        // Generate time slots from 8 AM to 4 PM (hourly intervals)
        LayoutInflater inflater = LayoutInflater.from(this);
        
        for (int hour = 8; hour <= 16; hour++) {
            // Format time as HH:mm
            String time = String.format(Locale.getDefault(), "%02d:00", hour);
            
            View timeSlotView = inflater.inflate(R.layout.item_time_slot, containerTimeSlots, false);
            TextView textTime = timeSlotView.findViewById(R.id.textTime);
            TextView textAvailability = timeSlotView.findViewById(R.id.textAvailability);
            LinearLayout layoutTimeSlot = timeSlotView.findViewById(R.id.layoutTimeSlot);
            
            if (textTime != null) textTime.setText(time);
            if (textAvailability != null) {
                textAvailability.setText("Available");
            textAvailability.setTextColor(getResources().getColor(R.color.text_secondary));
            }
            
            // Store time slot in tag
            timeSlotView.setTag(time);
            
            timeSlotView.setOnClickListener(v -> {
                    // Deselect previous
                    if (selectedTimeSlotView != null) {
                        LinearLayout prevLayout = selectedTimeSlotView.findViewById(R.id.layoutTimeSlot);
                        TextView prevTime = selectedTimeSlotView.findViewById(R.id.textTime);
                        TextView prevAvail = selectedTimeSlotView.findViewById(R.id.textAvailability);
                        
                    if (prevLayout != null) {
                        prevLayout.setBackground(getResources().getDrawable(R.drawable.bg_time_slot));
                    }
                    if (prevTime != null) prevTime.setTextColor(getResources().getColor(R.color.black));
                    if (prevAvail != null) prevAvail.setTextColor(getResources().getColor(R.color.text_secondary));
                    }
                    
                    // Select current
                    selectedTimeSlot = (String) v.getTag();
                    selectedTimeSlotView = v;
                    
                if (layoutTimeSlot != null) {
                    layoutTimeSlot.setBackground(getResources().getDrawable(R.drawable.bg_time_slot_selected));
                }
                if (textTime != null) textTime.setTextColor(getResources().getColor(R.color.white));
                if (textAvailability != null) textAvailability.setTextColor(getResources().getColor(R.color.white));
            });
            
            containerTimeSlots.addView(timeSlotView);
        }
    }

    private void setupSpinner() {
        if (db == null || progressBar == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        nurseIds.clear();
        nurseNames.clear();
        if (nursesReg != null) { 
            nursesReg.remove(); 
            nursesReg = null; 
        }
        
        try {
        nursesReg = db.collection("users")
                .whereEqualTo("role", "nurse")
                    .addSnapshotListener((snapshots, e) -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (e != null) {
                            android.util.Log.e("AppointmentBooking", "Failed to load nurses: " + e.getMessage());
                            String errorMsg = e.getMessage();
                            if (errorMsg != null && (errorMsg.contains("SecurityException") || errorMsg.contains("authentication"))) {
                                Toast.makeText(AppointmentBookingActivity.this, 
                                        "Authentication error. Please check your Firebase configuration.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(AppointmentBookingActivity.this, 
                                        "Failed to load nurses: " + errorMsg, Toast.LENGTH_LONG).show();
                            }
                            return;
                        }
                        nurseIds.clear();
                        nurseNames.clear();
                        // Iterate over DocumentSnapshot and cast when needed
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            // Cast to QueryDocumentSnapshot to access methods
                            if (document instanceof QueryDocumentSnapshot) {
                                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) document;
                                String id = doc.getString("userId");
                                if (id == null) id = doc.getString("uid");
                                if (id == null) id = doc.getId();
                                String name = doc.getString("name");
                            if (name == null || name.trim().isEmpty()) {
                                    String first = doc.getString("firstName");
                                    String last = doc.getString("lastName");
                                name = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                            }
                            if (name == null || name.isEmpty()) name = id;
                            nurseIds.add(id);
                            nurseNames.add(name);
                        }
                        }
                        // Setup AutoCompleteTextView like in MedicalRecordsActivity
                        android.widget.ArrayAdapter<String> nurseAdapter = new android.widget.ArrayAdapter<>(
                                AppointmentBookingActivity.this, android.R.layout.simple_dropdown_item_1line, nurseNames);
                        if (autoCompleteNurse != null) {
                            autoCompleteNurse.setAdapter(nurseAdapter);
                            // Set default selection if saved
                        SharedPreferences prefs = getSharedPreferences("hinurse_prefs", MODE_PRIVATE);
                        String savedNurseId = prefs.getString("default_nurse_id", null);
                            if (savedNurseId != null && nurseIds.contains(savedNurseId)) {
                                int index = nurseIds.indexOf(savedNurseId);
                                if (index >= 0 && index < nurseNames.size()) {
                                    autoCompleteNurse.setText(nurseNames.get(index), false);
                                }
                            }
                        }
                    });
        } catch (SecurityException e) {
            android.util.Log.e("AppointmentBooking", "SecurityException: " + e.getMessage(), e);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Service authentication failed. Please check your app configuration.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("AppointmentBooking", "Unexpected error: " + e.getMessage(), e);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        if (buttonBookAppointment != null) {
            buttonBookAppointment.setOnClickListener(v -> bookAppointment());
        }
    }

    private void bookAppointment() {
        if (mAuth == null || db == null) {
            Toast.makeText(this, "Service not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to book appointments.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedTimeSlot == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editTextReason == null) return;
        String reason = editTextReason.getText().toString().trim();
        if (reason.isEmpty()) {
            editTextReason.setError("Please enter the reason for your appointment");
            editTextReason.requestFocus();
            return;
        }

        if (autoCompleteNurse == null) return;
        String selectedNurseName = autoCompleteNurse.getText().toString().trim();
        if (selectedNurseName.isEmpty()) {
            Toast.makeText(this, "Please select a nurse", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int pos = nurseNames.indexOf(selectedNurseName);
        if (pos < 0 || pos >= nurseIds.size()) {
            Toast.makeText(this, "Please select a valid nurse", Toast.LENGTH_SHORT).show();
            return;
        }
        String nurseId = nurseIds.get(pos);
        String selectedNurse = nurseNames.get(pos);

        SharedPreferences prefs = getSharedPreferences("hinurse_prefs", MODE_PRIVATE);
        prefs.edit().putString("default_nurse_id", nurseId).apply();

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();
        
        // Check if user already has an appointment at this time slot
        checkExistingAppointment(userId, selectedDate, selectedTimeSlot, (hasConflict) -> {
            if (hasConflict) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "You already have an appointment at this time. Please choose a different time slot.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // No conflict, proceed with booking
            proceedWithBooking(userId, nurseId, selectedNurse, reason);
        });
    }

    private void attachMyAppointmentsListener() {
        if (mAuth == null || db == null) return;
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        if (myApptsReg != null) { 
            myApptsReg.remove(); 
            myApptsReg = null; 
        }
        
        try {
            myApptsReg = db.collection("appointments")
                    .whereEqualTo("userId", userId)
                    .orderBy("appointmentDate", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            android.util.Log.e("AppointmentBooking", "Failed to load appointments: " + e.getMessage());
                            String errorMsg = e.getMessage();
                            if (errorMsg != null && (errorMsg.contains("SecurityException") || errorMsg.contains("authentication"))) {
                                Toast.makeText(AppointmentBookingActivity.this, 
                                        "Authentication error. Please check your Firebase configuration.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(AppointmentBookingActivity.this, 
                                        "Failed to load appointments: " + errorMsg, Toast.LENGTH_LONG).show();
                            }
                            return;
                        }
                        renderAppointments(snapshots);
                    });
        } catch (SecurityException e) {
            android.util.Log.e("AppointmentBooking", "SecurityException: " + e.getMessage(), e);
            Toast.makeText(this, "Service authentication failed. Please check your app configuration.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("AppointmentBooking", "Unexpected error: " + e.getMessage(), e);
            Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void renderAppointments(QuerySnapshot snapshots) {
        if (containerMyAppointments == null) return;
        containerMyAppointments.removeAllViews();
        if (snapshots == null || snapshots.isEmpty()) {
            if (textNoAppointments != null) textNoAppointments.setVisibility(View.VISIBLE);
            previousAppointmentCount = 0;
            previousAppointments.clear();
            return;
        } else {
            if (textNoAppointments != null) textNoAppointments.setVisibility(View.GONE);
        }
        
        int currentAppointmentCount = snapshots.size();
        SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        java.util.Map<String, com.example.hinurse20.models.Appointment> currentAppointments = new java.util.HashMap<>();
        
        // Skip notification checks on first load (initial data fetch)
        boolean shouldCheckForChanges = !isFirstLoad && !isActivityVisible;
        
        // Process all appointments and detect changes
        for (DocumentSnapshot document : snapshots.getDocuments()) {
            if (document instanceof QueryDocumentSnapshot) {
                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) document;
                com.example.hinurse20.models.Appointment appt = doc.toObject(com.example.hinurse20.models.Appointment.class);
                if (appt == null || appt.getAppointmentId() == null) continue;
                
                String appointmentId = appt.getAppointmentId();
                currentAppointments.put(appointmentId, appt);
                
                // Check for changes (only notify if activity is not visible and not first load)
                if (shouldCheckForChanges && previousAppointments.containsKey(appointmentId)) {
                    com.example.hinurse20.models.Appointment previousAppt = previousAppointments.get(appointmentId);
                    checkAndNotifyAppointmentChanges(previousAppt, appt, df);
                }
            }
        }
        
        // Check for new appointments (only if count increased, activity is not visible, and not first load)
        if (shouldCheckForChanges && currentAppointmentCount > previousAppointmentCount) {
            // Find newly added appointments
            for (String appointmentId : currentAppointments.keySet()) {
                if (!previousAppointments.containsKey(appointmentId)) {
                    com.example.hinurse20.models.Appointment appt = currentAppointments.get(appointmentId);
                    if (appt != null) {
                        String dateStr = appt.getAppointmentDate() != null ? df.format(appt.getAppointmentDate()) : "";
                        NotificationHelper.showAppointmentNotification(
                                AppointmentBookingActivity.this,
                                "New Appointment",
                                "You have a new appointment with " + (appt.getNurseName() != null ? appt.getNurseName() : "Nurse") 
                                        + " on " + dateStr + " at " + (appt.getTimeSlot() != null ? appt.getTimeSlot() : ""));
                        break; // Only notify once for the first new appointment found
                    }
                }
            }
        }
        
        // Update previous state
        previousAppointmentCount = currentAppointmentCount;
        previousAppointments = new java.util.HashMap<>(currentAppointments);
        isFirstLoad = false; // Mark that initial load is complete
        
        // Render all appointments in Firestore order (already sorted by date DESC)
        for (DocumentSnapshot document : snapshots.getDocuments()) {
            if (document instanceof QueryDocumentSnapshot) {
                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) document;
                com.example.hinurse20.models.Appointment appt = doc.toObject(com.example.hinurse20.models.Appointment.class);
                if (appt == null) continue;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(16, 16, 16, 16);
            
            TextView line1 = new TextView(this);
            String dateStr = appt.getAppointmentDate() != null ? df.format(appt.getAppointmentDate()) : "";
            line1.setText(dateStr + " • " + appt.getTimeSlot());
            line1.setTextColor(getResources().getColor(R.color.dark_blue));
            line1.setTextSize(16);
            
            TextView line2 = new TextView(this);
            line2.setText((appt.getNurseName() != null ? appt.getNurseName() : "") + "  •  " + 
                         (appt.getStatus() != null ? appt.getStatus() : ""));
            line2.setTextColor(getResources().getColor(R.color.purple));
            line2.setTextSize(14);
            
            row.addView(line1);
            row.addView(line2);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

            containerMyAppointments.addView(row);
            containerMyAppointments.addView(divider);
            }
        }
    }
    
    /**
     * Check for appointment changes and notify user accordingly
     */
    private void checkAndNotifyAppointmentChanges(com.example.hinurse20.models.Appointment previous, 
                                                  com.example.hinurse20.models.Appointment current,
                                                  SimpleDateFormat df) {
        if (previous == null || current == null) return;
        
        // Check status change
        String previousStatus = previous.getStatus() != null ? previous.getStatus() : "scheduled";
        String currentStatus = current.getStatus() != null ? current.getStatus() : "scheduled";
        
        if (!previousStatus.equals(currentStatus)) {
            String statusMessage = "Your appointment status changed to " + currentStatus;
            if (current.getNurseName() != null) {
                statusMessage += " with " + current.getNurseName();
            }
            NotificationHelper.showAppointmentStatusNotification(
                    AppointmentBookingActivity.this,
                    "Appointment Status Updated",
                    statusMessage,
                    currentStatus);
            return; // Status change is the most important, so return early
        }
        
        // Check date change
        if (previous.getAppointmentDate() != null && current.getAppointmentDate() != null) {
            if (!previous.getAppointmentDate().equals(current.getAppointmentDate())) {
                String dateStr = df.format(current.getAppointmentDate());
                NotificationHelper.showAppointmentNotification(
                        AppointmentBookingActivity.this,
                        "Appointment Rescheduled",
                        "Your appointment with " + (current.getNurseName() != null ? current.getNurseName() : "Nurse") 
                                + " has been rescheduled to " + dateStr + " at " + (current.getTimeSlot() != null ? current.getTimeSlot() : ""));
                return;
            }
        }
        
        // Check time slot change
        String previousTime = previous.getTimeSlot() != null ? previous.getTimeSlot() : "";
        String currentTime = current.getTimeSlot() != null ? current.getTimeSlot() : "";
        if (!previousTime.equals(currentTime) && !previousTime.isEmpty()) {
            String dateStr = current.getAppointmentDate() != null ? df.format(current.getAppointmentDate()) : "";
            NotificationHelper.showAppointmentNotification(
                    AppointmentBookingActivity.this,
                    "Appointment Time Updated",
                    "Your appointment with " + (current.getNurseName() != null ? current.getNurseName() : "Nurse") 
                            + " on " + dateStr + " has been rescheduled to " + currentTime);
            return;
        }
        
        // Check notes change (if nurse added notes)
        String previousNotes = previous.getNotes() != null ? previous.getNotes() : "";
        String currentNotes = current.getNotes() != null ? current.getNotes() : "";
        if (!previousNotes.equals(currentNotes) && !currentNotes.isEmpty() && previousNotes.isEmpty()) {
            NotificationHelper.showAppointmentNotification(
                    AppointmentBookingActivity.this,
                    "Appointment Update",
                    current.getNurseName() != null ? current.getNurseName() + " added notes to your appointment" : "Notes added to your appointment");
        }
    }

    private void styleCalendarHeader(View root) {
        try {
            final int black = getResources().getColor(R.color.black);
            final android.graphics.Typeface tf = ResourcesCompat.getFont(this, R.font.poppins_bold2);
            walkAndStyle(root, tf, black);
        } catch (Exception ignored) {
        }
    }

    private void walkAndStyle(View v, android.graphics.Typeface tf, int color) {
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            CharSequence t = tv.getText();
            if (t != null) {
                String s = t.toString().trim();
                // Match month-year patterns or arrow symbols
                if (s.matches("[A-Za-z]+\\s+\\d{4}") || 
                    s.matches("[A-Za-z]{3}\\s+\\d{4}") ||
                    s.matches(".*[JFMASOND][a-z]+.*\\d{4}.*") ||
                    s.equals("<") || s.equals(">") || 
                    s.equals("◀") || s.equals("▶") ||
                    (s.length() == 1 && (s.charAt(0) == '<' || s.charAt(0) == '>'))) {
                    if (tf != null) tv.setTypeface(tf);
                    tv.setTextColor(color);
                    return;
                }
                // If text contains a year (4 digits), likely month/year header
                if (s.matches(".*\\d{4}.*")) {
                    if (!s.matches("\\d{1,2}") && !s.matches("\\d{1,2}/\\d{1,2}")) {
                        if (tf != null) tv.setTypeface(tf);
                        tv.setTextColor(color);
                        return;
                    }
                }
            }
            // Style selected date
            try {
                android.graphics.drawable.Drawable bg = tv.getBackground();
                if (bg != null) {
                    if (bg instanceof android.graphics.drawable.GradientDrawable) {
                        tv.setTextColor(getResources().getColor(R.color.white));
                    } else if (bg instanceof android.graphics.drawable.ColorDrawable) {
                        android.graphics.drawable.ColorDrawable cd = (android.graphics.drawable.ColorDrawable) bg;
                        int bgColor = cd.getColor();
                        if (bgColor != 0 && bgColor != 0xFFFFFFFF && bgColor != 0xFFFFFF) {
                            tv.setTextColor(getResources().getColor(R.color.white));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        if (v instanceof android.widget.ImageView || v instanceof ImageButton) {
            try {
                if (v instanceof android.widget.ImageView) {
                    android.widget.ImageView iv = (android.widget.ImageView) v;
                    iv.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
                    iv.setImageTintList(android.content.res.ColorStateList.valueOf(color));
                } else if (v instanceof ImageButton) {
                    ImageButton ib = (ImageButton) v;
                    ib.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
                    ib.setImageTintList(android.content.res.ColorStateList.valueOf(color));
                }
            } catch (Exception ignored) {}
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                walkAndStyle(vg.getChildAt(i), tf, color);
            }
        }
    }
    
    private void styleSelectedDate(CalendarView calendarView) {
        try {
            ViewGroup viewGroup = (ViewGroup) calendarView.getChildAt(0);
            if (viewGroup != null) {
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    if (child instanceof ViewGroup) {
                        walkAndStyleSelectedDate((ViewGroup) child);
                    }
                }
            }
        } catch (Exception ignored) {
            // CalendarView structure may vary by Android version
        }
    }
    
    private void walkAndStyleSelectedDate(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (tv.getBackground() != null) {
                    tv.setTextColor(getResources().getColor(R.color.white));
                }
            } else if (child instanceof ViewGroup) {
                walkAndStyleSelectedDate((ViewGroup) child);
            }
        }
    }

    private void handleSmartTriagePriority() {
        Intent intent = getIntent();
        if (intent != null) {
            String priorityLevel = intent.getStringExtra("priority_level");
            String symptoms = intent.getStringExtra("symptoms");
            String urgency = intent.getStringExtra("urgency");
            
            if (priorityLevel != null && symptoms != null && editTextReason != null) {
                // Update the reason field with Smart Triage information
                String triageReason = "Smart Triage Analysis:\n" +
                        "Priority Level: " + priorityLevel + "\n" +
                        "Symptoms: " + symptoms + "\n" +
                        "Urgency: " + (urgency != null ? urgency : "Standard");
                
                editTextReason.setText(triageReason);
                
                // Show priority-based message
                String priorityMessage = "Based on your symptoms, this appointment has been prioritized.";
                if ("1".equals(priorityLevel) || "2".equals(priorityLevel)) {
                    priorityMessage += " Urgent appointments will be scheduled as soon as possible.";
                }
                Toast.makeText(this, priorityMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    // Callback interface for appointment conflict check
    interface AppointmentConflictCallback {
        void onResult(boolean hasConflict);
    }
    
    // Check if user already has an appointment at the specified date and time
    private void checkExistingAppointment(String userId, String date, String timeSlot, AppointmentConflictCallback callback) {
        // Parse the date string to a Date object for proper comparison
        try {
            Date appointmentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date);
            if (appointmentDate == null) {
                callback.onResult(false);
                return;
            }
            
            db.collection("appointments")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("appointmentDate", appointmentDate)
                    .whereEqualTo("timeSlot", timeSlot)
                    .whereEqualTo("status", "scheduled") // Only check scheduled appointments
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            // If there are any results, it means there's a conflict
                            boolean hasConflict = !task.getResult().isEmpty();
                            callback.onResult(hasConflict);
                        } else {
                            // In case of error, assume no conflict to avoid blocking legitimate bookings
                            android.util.Log.e("AppointmentBooking", "Error checking for existing appointments: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                            callback.onResult(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // In case of error, assume no conflict to avoid blocking legitimate bookings
                        android.util.Log.e("AppointmentBooking", "Failed to check existing appointments: " + e.getMessage(), e);
                        callback.onResult(false);
                    });
        } catch (ParseException e) {
            android.util.Log.e("AppointmentBooking", "Error parsing date for conflict check: " + e.getMessage(), e);
            callback.onResult(false);
        }
    }
    
    // Proceed with booking the appointment after conflict check
    private void proceedWithBooking(String userId, String nurseId, String selectedNurse, String reason) {
        String appointmentId = UUID.randomUUID().toString();
        
        try {
            Date appointmentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate);
            if (appointmentDate == null) {
                throw new ParseException("Failed to parse date", 0);
            }
            
            com.example.hinurse20.models.Appointment appointment = 
                    new com.example.hinurse20.models.Appointment(appointmentId, userId, nurseId, 
                            selectedNurse, appointmentDate, selectedTimeSlot, reason);
            
            db.collection("appointments")
                    .document(appointmentId)
                    .set(appointment)
                    .addOnCompleteListener(task -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(AppointmentBookingActivity.this, 
                                    "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                            
                            // Show notification
                            NotificationHelper.showAppointmentNotification(
                                    AppointmentBookingActivity.this,
                                    "Appointment Booked",
                                    "Your appointment with " + selectedNurse + " on " + selectedDate + " at " + selectedTimeSlot + " has been confirmed.");
                            
                            if (editTextReason != null) editTextReason.setText("");
                            selectedTimeSlotView = null;
                            selectedTimeSlot = null;
                        } else {
                            String msg = (task.getException() != null) 
                                    ? task.getException().getMessage() 
                                    : "Unknown error";
                            Toast.makeText(AppointmentBookingActivity.this, 
                                    "Failed to book appointment: " + msg, Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        android.util.Log.e("AppointmentBooking", "Error booking appointment: " + e.getMessage(), e);
                        if (e instanceof SecurityException) {
                            Toast.makeText(AppointmentBookingActivity.this, 
                                    "Authentication error. Please check your Firebase configuration.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AppointmentBookingActivity.this, 
                                    "Failed to book appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (ParseException e) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error parsing date: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            android.util.Log.e("AppointmentBooking", "Unexpected error: " + e.getMessage(), e);
            Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
