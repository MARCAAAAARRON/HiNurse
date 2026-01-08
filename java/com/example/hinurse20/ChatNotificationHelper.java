package com.example.hinurse20;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class to monitor and notify users about new chat messages
 * Works globally, even when ChatActivity is not open
 */
public class ChatNotificationHelper {
    private static final String TAG = "ChatNotification";
    private static final String PREFS_NAME = "chat_notifications";
    private static final String KEY_NOTIFIED_MESSAGES = "notified_messages";
    
    private static ListenerRegistration globalMessagesListener;
    private static Set<String> notifiedMessageIds = new HashSet<>();
    private static String currentUserId;
    private static Context applicationContext; // Store application context for background notifications
    
    /**
     * Start monitoring for new chat messages globally
     * This should be called when app starts (e.g., in MainDashboardActivity or SplashActivity)
     * Works both inside the app and when app is destroyed/closed
     */
    public static void startMonitoring(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot start monitoring");
            return;
        }
        
        // Use application context to persist even when activities are destroyed
        if (context.getApplicationContext() != null) {
            applicationContext = context.getApplicationContext();
        } else {
            applicationContext = context;
        }
        
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "User not logged in, stopping chat monitoring");
            stopMonitoring();
            return;
        }
        
        String newUserId = mAuth.getCurrentUser().getUid();
        if (newUserId == null || newUserId.isEmpty()) {
            Log.d(TAG, "User ID is null or empty, stopping chat monitoring");
            stopMonitoring();
            return;
        }
        
        // If user changed or listener not active, restart monitoring
        if (globalMessagesListener == null || !newUserId.equals(currentUserId)) {
            currentUserId = newUserId;
            
            // Load previously notified message IDs to avoid duplicates
            loadNotifiedMessageIds(applicationContext);
            
            // Stop existing listener if any
            stopMonitoring();
            
            // Start new listener
            startFirestoreListener();
        } else {
            Log.d(TAG, "Chat monitoring already active for user: " + currentUserId);
        }
    }
    
    /**
     * Start the Firestore listener for chat messages
     */
    private static void startFirestoreListener() {
        if (applicationContext == null) {
            Log.e(TAG, "Application context is null, cannot start listener");
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Query for messages where the user is involved
        // Since chatId format is "chat_userId_nurseId" or "chat_nurseId_userId"
        // Firestore doesn't support "contains" queries on strings, so we:
        // 1. Query recent messages (limit 50 for performance)
        // 2. Filter in memory for messages where chatId contains currentUserId
        // 3. Only notify for messages NOT from current user
        // 
        // Note: This approach works without requiring Firestore indexes
        // For better performance with many messages, consider adding a "participants" array field
        try {
            globalMessagesListener = db.collection("chat_messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50) // Limit to recent messages for performance
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "Error listening to messages: " + e.getMessage());
                            return;
                        }
                        
                        // Safety check: ensure currentUserId is valid
                        if (currentUserId == null || currentUserId.isEmpty()) {
                            Log.d(TAG, "Current user ID is null, skipping message processing");
                            return;
                        }
                        
                        if (snapshots == null) {
                            Log.d(TAG, "Snapshots is null");
                            return;
                        }
                        
                        // Check for document changes (new messages)
                        if (snapshots.getDocumentChanges() != null && !snapshots.getDocumentChanges().isEmpty()) {
                            Log.d(TAG, "Detected " + snapshots.getDocumentChanges().size() + " document changes");
                            
                            for (com.google.firebase.firestore.DocumentChange change : snapshots.getDocumentChanges()) {
                                // Only process ADDED documents (new messages)
                                if (change.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                    QueryDocumentSnapshot doc = change.getDocument();
                                    com.example.hinurse20.models.ChatMessage message = 
                                            doc.toObject(com.example.hinurse20.models.ChatMessage.class);
                                    
                                    if (message == null) {
                                        Log.d(TAG, "Message is null, skipping");
                                        continue;
                                    }
                                    
                                    String messageId = message.getMessageId();
                                    if (messageId == null || messageId.isEmpty()) {
                                        // Try to get ID from document
                                        messageId = doc.getId();
                                    }
                                    
                                    String senderId = message.getSenderId();
                                    String chatId = message.getChatId();
                                    
                                    Log.d(TAG, "New message detected - ID: " + messageId + ", Sender: " + senderId + ", ChatId: " + chatId);
                                    
                                    // Skip if already notified
                                    if (messageId != null && notifiedMessageIds.contains(messageId)) {
                                        Log.d(TAG, "Message already notified: " + messageId);
                                        continue;
                                    }
                                    
                                    // Only notify for messages not from current user
                                    if (senderId != null && !senderId.equals(currentUserId)) {
                                        // Check if this chat involves the current user
                                        if (chatId != null && chatId.contains(currentUserId)) {
                                            Log.d(TAG, "Message is for current user, showing notification");
                                            
                                            // This is a message for the current user
                                            String senderName = message.getSenderName();
                                            if (senderName == null || senderName.isEmpty()) {
                                                senderName = "Nurse";
                                            }
                                            
                                            String messageText = message.getMessage();
                                            if (messageText == null || messageText.isEmpty()) {
                                                if ("image".equalsIgnoreCase(message.getMessageType())) {
                                                    messageText = "📷 Sent an image";
                                                } else {
                                                    messageText = "New message";
                                                }
                                            }
                                            
                                            // Show notification using application context
                                            showChatNotification(applicationContext, senderName, messageText, chatId);
                                            
                                            // Mark as notified
                                            if (messageId != null) {
                                                notifiedMessageIds.add(messageId);
                                                saveNotifiedMessageIds(applicationContext);
                                                Log.d(TAG, "Notification sent for message: " + messageId + " from " + senderName);
                                            }
                                        } else {
                                            Log.d(TAG, "ChatId doesn't contain currentUserId. ChatId: " + chatId + ", UserId: " + currentUserId);
                                        }
                                    } else {
                                        Log.d(TAG, "Message is from current user, skipping notification");
                                    }
                                }
                            }
                        } else {
                            // Fallback: check all documents if no changes detected (for initial load)
                            if (!snapshots.isEmpty()) {
                                Log.d(TAG, "No document changes, checking all " + snapshots.size() + " documents");
                                
                                for (QueryDocumentSnapshot doc : snapshots) {
                                    com.example.hinurse20.models.ChatMessage message = 
                                            doc.toObject(com.example.hinurse20.models.ChatMessage.class);
                                    
                                    if (message == null) continue;
                                    
                                    String messageId = message.getMessageId();
                                    if (messageId == null || messageId.isEmpty()) {
                                        messageId = doc.getId();
                                    }
                                    
                                    String senderId = message.getSenderId();
                                    String chatId = message.getChatId();
                                    
                                    // Skip if already notified
                                    if (messageId != null && notifiedMessageIds.contains(messageId)) {
                                        continue;
                                    }
                                    
                                    // Only notify for messages not from current user
                                    if (senderId != null && !senderId.equals(currentUserId)) {
                                        // Check if this chat involves the current user
                                        if (chatId != null && chatId.contains(currentUserId)) {
                                            String senderName = message.getSenderName();
                                            if (senderName == null || senderName.isEmpty()) {
                                                senderName = "Nurse";
                                            }
                                            
                                            String messageText = message.getMessage();
                                            if (messageText == null || messageText.isEmpty()) {
                                                if ("image".equalsIgnoreCase(message.getMessageType())) {
                                                    messageText = "📷 Sent an image";
                                                } else {
                                                    messageText = "New message";
                                                }
                                            }
                                            
                                            // Show notification using application context
                                            showChatNotification(applicationContext, senderName, messageText, chatId);
                                            
                                            // Mark as notified
                                            if (messageId != null) {
                                                notifiedMessageIds.add(messageId);
                                                saveNotifiedMessageIds(applicationContext);
                                                Log.d(TAG, "Notification sent (fallback) for message: " + messageId);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up chat message listener: " + e.getMessage());
            // Fallback: Try without orderBy if timestamp index doesn't exist
            try {
                globalMessagesListener = db.collection("chat_messages")
                        .limit(50)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.e(TAG, "Error listening to messages (fallback): " + e.getMessage());
                                    return;
                                }
                                
                                // Safety check: ensure currentUserId is valid
                                if (currentUserId == null || currentUserId.isEmpty()) {
                                    Log.d(TAG, "Current user ID is null, skipping message processing (fallback)");
                                    return;
                                }
                                
                                if (snapshots == null) {
                                    return;
                                }
                                
                                // Check for document changes (new messages)
                                if (snapshots.getDocumentChanges() != null && !snapshots.getDocumentChanges().isEmpty()) {
                                    Log.d(TAG, "Fallback: Detected " + snapshots.getDocumentChanges().size() + " document changes");
                                    
                                    for (com.google.firebase.firestore.DocumentChange change : snapshots.getDocumentChanges()) {
                                        // Only process ADDED documents (new messages)
                                        if (change.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                            QueryDocumentSnapshot doc = change.getDocument();
                                            com.example.hinurse20.models.ChatMessage message = 
                                                    doc.toObject(com.example.hinurse20.models.ChatMessage.class);
                                            
                                            if (message == null) continue;
                                            
                                            String messageId = message.getMessageId();
                                            if (messageId == null || messageId.isEmpty()) {
                                                messageId = doc.getId();
                                            }
                                            
                                            String senderId = message.getSenderId();
                                            String chatId = message.getChatId();
                                            
                                            Log.d(TAG, "Fallback: New message - ID: " + messageId + ", Sender: " + senderId + ", ChatId: " + chatId);
                                            
                                            // Skip if already notified
                                            if (messageId != null && notifiedMessageIds.contains(messageId)) {
                                                continue;
                                            }
                                            
                                            // Only notify for messages not from current user
                                            if (senderId != null && !senderId.equals(currentUserId)) {
                                                // Check if this chat involves the current user
                                                if (chatId != null && chatId.contains(currentUserId)) {
                                                    String senderName = message.getSenderName();
                                                    if (senderName == null || senderName.isEmpty()) {
                                                        senderName = "Nurse";
                                                    }
                                                    
                                                    String messageText = message.getMessage();
                                                    if (messageText == null || messageText.isEmpty()) {
                                                        if ("image".equalsIgnoreCase(message.getMessageType())) {
                                                            messageText = "📷 Sent an image";
                                                        } else {
                                                            messageText = "New message";
                                                        }
                                                    }
                                                    
                                                    // Show notification using application context
                                                    showChatNotification(applicationContext, senderName, messageText, chatId);
                                                    
                                                    // Mark as notified
                                                    if (messageId != null) {
                                                        notifiedMessageIds.add(messageId);
                                                        saveNotifiedMessageIds(applicationContext);
                                                        Log.d(TAG, "Fallback: Notification sent for message: " + messageId);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        });
            } catch (Exception ex) {
                Log.e(TAG, "Error setting up fallback listener: " + ex.getMessage());
            }
        }
        
        Log.d(TAG, "Started monitoring chat messages for user: " + currentUserId);
        Log.d(TAG, "Listener active: " + (globalMessagesListener != null));
    }
    
    /**
     * Stop monitoring for new messages
     * Note: This should only be called when user logs out or app is fully closed
     */
    public static void stopMonitoring() {
        if (globalMessagesListener != null) {
            globalMessagesListener.remove();
            globalMessagesListener = null;
            Log.d(TAG, "Stopped monitoring chat messages");
        }
        currentUserId = null;
    }
    
    /**
     * Check if monitoring is active
     */
    public static boolean isMonitoring() {
        return globalMessagesListener != null;
    }
    
    /**
     * Show chat notification
     */
    private static void showChatNotification(Context context, String senderName, String message, String chatId) {
        Log.d(TAG, "Attempting to show notification - Sender: " + senderName + ", Message: " + message);
        
        if (!NotificationHelper.shouldShowNotification(context)) {
            Log.w(TAG, "Notifications disabled in app settings");
            return;
        }
        
        if (!NotificationHelper.areNotificationsEnabled(context)) {
            Log.w(TAG, "Notifications disabled at system level");
            return;
        }
        
        Log.d(TAG, "Notification checks passed, showing notification");
        
        // Create intent to open ChatActivity
        Intent intent = new Intent(context, ChatActivity.class);
        // Extract nurseId from chatId if possible
        if (chatId != null && chatId.contains("_")) {
            String[] parts = chatId.replace("chat_", "").split("_");
            if (parts.length >= 2) {
                // Find the nurse ID (the one that's not the current user)
                for (String part : parts) {
                    if (!part.equals(currentUserId) && !part.isEmpty()) {
                        intent.putExtra("nurseId", part);
                        Log.d(TAG, "Opening chat with nurseId: " + part);
                        break;
                    }
                }
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Use unique notification ID based on chatId to group notifications
        int notificationId = chatId != null ? chatId.hashCode() : "chat".hashCode();
        
        try {
            NotificationHelper.showNotification(
                    context,
                    "💬 New message from " + senderName,
                    message,
                    intent,
                    notificationId
            );
            Log.d(TAG, "Notification.showNotification() called successfully");
        } catch (Exception ex) {
            Log.e(TAG, "Error showing notification: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Load previously notified message IDs from SharedPreferences
     */
    private static void loadNotifiedMessageIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String notifiedIdsString = prefs.getString(KEY_NOTIFIED_MESSAGES, "");
        notifiedMessageIds.clear();
        
        if (!notifiedIdsString.isEmpty()) {
            String[] ids = notifiedIdsString.split(",");
            for (String id : ids) {
                if (!id.isEmpty()) {
                    notifiedMessageIds.add(id);
                }
            }
        }
        
        // Limit to last 1000 message IDs to prevent memory issues
        if (notifiedMessageIds.size() > 1000) {
            Set<String> limitedSet = new HashSet<>();
            int count = 0;
            for (String id : notifiedMessageIds) {
                if (count++ >= 1000) break;
                limitedSet.add(id);
            }
            notifiedMessageIds = limitedSet;
        }
    }
    
    /**
     * Save notified message IDs to SharedPreferences
     */
    private static void saveNotifiedMessageIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String id : notifiedMessageIds) {
            if (count++ > 0) sb.append(",");
            sb.append(id);
        }
        prefs.edit().putString(KEY_NOTIFIED_MESSAGES, sb.toString()).apply();
    }
    
    /**
     * Clear notified message IDs (useful for testing)
     */
    public static void clearNotifiedMessages(Context context) {
        notifiedMessageIds.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_NOTIFIED_MESSAGES).apply();
        Log.d(TAG, "Cleared notified message IDs");
    }
    
    /**
     * Mark a message as read (so it won't notify again)
     */
    public static void markMessageAsNotified(String messageId, Context context) {
        if (messageId != null && !messageId.isEmpty()) {
            notifiedMessageIds.add(messageId);
            Context ctx = applicationContext != null ? applicationContext : context;
            if (ctx != null) {
                saveNotifiedMessageIds(ctx);
            }
        }
    }
}

