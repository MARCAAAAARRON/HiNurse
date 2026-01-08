package com.example.hinurse20;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    private static final String CHANNEL_ID = "hinurse_notifications";
    private static final String CHANNEL_NAME = "HiNurse Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for appointments, messages, and medical records";
    
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager == null) {
                android.util.Log.e("NotificationHelper", "NotificationManager is null");
                return;
            }
            
            // Check if channel already exists
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (existingChannel != null) {
                android.util.Log.d("NotificationHelper", "Notification channel already exists");
                return;
            }
            
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.enableLights(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            
            notificationManager.createNotificationChannel(channel);
            android.util.Log.d("NotificationHelper", "Notification channel created: " + CHANNEL_NAME);
        }
    }
    
    public static boolean areNotificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                return notificationManager.areNotificationsEnabled();
            }
        }
        return true; // Default to true for older Android versions
    }
    
    public static boolean shouldShowNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("notifications", true); // Default to true
    }
    
    public static void showNotification(Context context, String title, String message, Intent intent, int notificationId) {
        if (!shouldShowNotification(context)) {
            android.util.Log.d("NotificationHelper", "Notifications disabled in app settings");
            return;
        }
        
        if (!areNotificationsEnabled(context)) {
            android.util.Log.d("NotificationHelper", "Notifications disabled at system level");
            return;
        }
        
        // Ensure notification channel is created
        createNotificationChannel(context);
        
        PendingIntent pendingIntent = null;
        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setShowWhen(true);
        
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }
        
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
    
    public static void showChatNotification(Context context, String senderName, String message) {
        if (context == null) {
            android.util.Log.e("NotificationHelper", "Context is null, cannot show notification");
            return;
        }
        Intent intent = new Intent(context, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // You can add extras like nurseId if needed
        int notificationId = "chat".hashCode();
        showNotification(context, "New message from " + senderName, message, intent, notificationId);
    }
    
    public static void showAppointmentNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, AppointmentBookingActivity.class);
        int notificationId = "appointment".hashCode();
        showNotification(context, title, message, intent, notificationId);
    }
    
    /**
     * Show appointment status change notification with priority based on status
     */
    public static void showAppointmentStatusNotification(Context context, String title, String message, String status) {
        if (!shouldShowNotification(context)) {
            android.util.Log.d("NotificationHelper", "Notifications disabled in app settings");
            return;
        }
        
        if (!areNotificationsEnabled(context)) {
            android.util.Log.d("NotificationHelper", "Notifications disabled at system level");
            return;
        }
        
        // Ensure notification channel is created
        createNotificationChannel(context);
        
        Intent intent = new Intent(context, AppointmentBookingActivity.class);
        int notificationId = ("appointment_status_" + status).hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Set priority based on status
        int priority = NotificationCompat.PRIORITY_DEFAULT;
        if ("cancelled".equalsIgnoreCase(status)) {
            priority = NotificationCompat.PRIORITY_HIGH;
        } else if ("completed".equalsIgnoreCase(status)) {
            priority = NotificationCompat.PRIORITY_LOW;
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setContentIntent(pendingIntent);
        
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
    
    public static void showMedicalRecordNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, MedicalRecordsActivity.class);
        int notificationId = "medical_record".hashCode();
        showNotification(context, title, message, intent, notificationId);
    }
}

