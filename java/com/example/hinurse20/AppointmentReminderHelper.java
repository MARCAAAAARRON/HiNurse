package com.example.hinurse20;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class to check and notify users about appointments scheduled for today
 */
public class AppointmentReminderHelper {
    private static final String TAG = "AppointmentReminder";
    private static final String PREFS_NAME = "appointment_reminders";
    private static final String KEY_LAST_CHECK_DATE = "last_check_date";
    
    /**
     * Check for today's appointments and notify user if found
     * This should be called when app starts or when appointment activity opens
     */
    public static void checkTodaysAppointments(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }
        
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "User not logged in, skipping appointment check");
            return;
        }
        
        // Check if we already notified today
        if (alreadyNotifiedToday(context)) {
            Log.d(TAG, "Already notified today, skipping check");
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        
        // Query Firestore for appointments with status "scheduled"
        // We'll filter for today's appointments in memory and exclude past/completed appointments
        db.collection("appointments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "scheduled")  // Only get scheduled appointments (excludes completed, cancelled)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            // Get today's date for comparison
                            Date today = new Date();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            
                            // Create Calendar instance for date comparison (time set to 00:00:00)
                            java.util.Calendar todayCalendar = java.util.Calendar.getInstance();
                            todayCalendar.setTime(today);
                            todayCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
                            todayCalendar.set(java.util.Calendar.MINUTE, 0);
                            todayCalendar.set(java.util.Calendar.SECOND, 0);
                            todayCalendar.set(java.util.Calendar.MILLISECOND, 0);
                            Date todayStart = todayCalendar.getTime();
                            
                            for (QueryDocumentSnapshot doc : snapshot) {
                                com.example.hinurse20.models.Appointment appointment = 
                                        doc.toObject(com.example.hinurse20.models.Appointment.class);
                                
                                // Validate appointment: must be scheduled and for today (not past, not completed)
                                if (isValidForTodayNotification(appointment, todayStart)) {
                                    // Appointment is valid for today - show notification
                                    showTodaysAppointmentNotification(context, appointment);
                                    // Mark that we've notified today
                                    markAsNotifiedToday(context);
                                    break; // Only notify once per day
                                }
                            }
                        } else {
                            Log.d(TAG, "No scheduled appointments found");
                        }
                    } else {
                        Log.e(TAG, "Error checking appointments: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }
    
    /**
     * Show notification for today's appointment
     */
    private static void showTodaysAppointmentNotification(Context context, 
                                                          com.example.hinurse20.models.Appointment appointment) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String timeSlot = appointment.getTimeSlot();
        String nurseName = appointment.getNurseName() != null ? appointment.getNurseName() : "your nurse";
        
        String message;
        if (timeSlot != null && !timeSlot.isEmpty()) {
            message = "You have an appointment with " + nurseName + " at " + timeSlot + " today!";
        } else {
            message = "You have an appointment with " + nurseName + " today!";
        }
        
        NotificationHelper.showAppointmentNotification(
                context,
                "📅 Appointment Today!",
                message
        );
        
        Log.d(TAG, "Notification sent for today's appointment: " + appointment.getAppointmentId());
    }
    
    /**
     * Check if we already notified the user today
     */
    private static boolean alreadyNotifiedToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastCheckDate = prefs.getString(KEY_LAST_CHECK_DATE, "");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        return today.equals(lastCheckDate);
    }
    
    /**
     * Mark that we've notified the user today
     */
    private static void markAsNotifiedToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        prefs.edit().putString(KEY_LAST_CHECK_DATE, today).apply();
    }
    
    /**
     * Reset the notification flag (useful for testing or if you want to allow multiple notifications per day)
     */
    public static void resetNotificationFlag(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_LAST_CHECK_DATE).apply();
    }
    
    /**
     * Check if appointment is valid for today's notification
     * @param appointment The appointment to check
     * @param todayStart Today's date at 00:00:00
     * @return true if appointment is scheduled for today, false otherwise
     */
    private static boolean isValidForTodayNotification(com.example.hinurse20.models.Appointment appointment, 
                                                       Date todayStart) {
        if (appointment == null || appointment.getAppointmentDate() == null) {
            return false;
        }
        
        // Check status - must be "scheduled" (not completed, not cancelled)
        String status = appointment.getStatus();
        if (status == null || !status.equalsIgnoreCase("scheduled")) {
            return false;
        }
        
        // Normalize appointment date to start of day for comparison
        Date appointmentDate = appointment.getAppointmentDate();
        java.util.Calendar appointmentCalendar = java.util.Calendar.getInstance();
        appointmentCalendar.setTime(appointmentDate);
        appointmentCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        appointmentCalendar.set(java.util.Calendar.MINUTE, 0);
        appointmentCalendar.set(java.util.Calendar.SECOND, 0);
        appointmentCalendar.set(java.util.Calendar.MILLISECOND, 0);
        Date appointmentStart = appointmentCalendar.getTime();
        
        // Only return true if appointment is exactly today (not past, not future)
        return appointmentStart.equals(todayStart);
    }
}

