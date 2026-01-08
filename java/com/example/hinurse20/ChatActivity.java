package com.example.hinurse20;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import android.net.Uri;
import android.content.SharedPreferences;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.Intent;
import android.provider.MediaStore;
import android.os.Environment;
import androidx.core.content.FileProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Date;
import com.example.hinurse20.services.CloudinaryService;

public class ChatActivity extends BaseActivity {
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonAttach;
    private ImageButton buttonBack;
    private ImageButton buttonVideoCall;
    private ImageButton buttonVoiceCall;
    private ImageButton buttonMicrophone;
    private ImageView imageProfile;
    private TextView textViewContactName;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private List<com.example.hinurse20.models.ChatMessage> messagesList;
    private com.example.hinurse20.adapters.ChatAdapter adapter;
    private String chatId;
    private String nurseId;
    private StorageReference chatImagesRef;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ListenerRegistration messagesReg;
    private ListenerRegistration nurseProfileReg;
    private int previousMessageCount = 0;
    private boolean isActivityVisible = false;
    private String currentPhotoPath;
    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private static final int PERMISSION_REQUEST_STORAGE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase with error handling
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            android.util.Log.e("ChatActivity", "Firebase initialization error: " + e.getMessage(), e);
            Toast.makeText(this, "Service initialization failed. Please try again.", Toast.LENGTH_LONG).show();
            // Continue anyway - some features might still work
        }

        // Initialize views
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonAttach = findViewById(R.id.buttonAttach);
        buttonBack = findViewById(R.id.buttonBack);
        buttonVideoCall = findViewById(R.id.buttonVideoCall);
        buttonVoiceCall = findViewById(R.id.buttonVoiceCall);
        buttonMicrophone = findViewById(R.id.buttonMicrophone);
        imageProfile = findViewById(R.id.imageProfile);
        textViewContactName = findViewById(R.id.textViewContactName);
        
        messagesList = new ArrayList<>();
        adapter = new com.example.hinurse20.adapters.ChatAdapter();
        adapter.setOnMessageDeleteListener(new com.example.hinurse20.adapters.ChatAdapter.OnMessageDeleteListener() {
            @Override
            public void onDeleteMessage(com.example.hinurse20.models.ChatMessage message, int position) {
                deleteMessage(message, position);
            }
        });
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(adapter);

        // Generate or get chat ID
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to use chat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get nurse ID from intent, then shared preferences, or load first available
        String currentUserId = mAuth.getCurrentUser().getUid();
        String nurseIdFromIntent = getIntent().getStringExtra("nurseId");
        SharedPreferences prefs = getSharedPreferences("hinurse_prefs", MODE_PRIVATE);
        String savedNurseId = prefs.getString("default_nurse_id", null);
        
        // Priority: Intent > SharedPreferences > Load first available
        if (nurseIdFromIntent != null && !nurseIdFromIntent.isEmpty()) {
            initChatWithNurse(currentUserId, nurseIdFromIntent);
        } else if (savedNurseId != null && !savedNurseId.isEmpty()) {
            initChatWithNurse(currentUserId, savedNurseId);
        } else {
            loadFirstAvailableNurse(currentUserId);
        }

        // Register gallery picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadAndSendImage(uri);
            }
        });

        // Register camera launcher
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                if (currentPhotoPath != null) {
                    File photoFile = new File(currentPhotoPath);
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.example.hinurse20.fileprovider",
                            photoFile);
                    uploadAndSendImage(photoURI);
                }
            }
        });

        setupClickListeners();
        
        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);
        
        // Activity is visible after onCreate completes
        isActivityVisible = true;
    }

    private void setupClickListeners() {
        // Back button
        if (buttonBack != null) {
            buttonBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        
        // Send button
        if (buttonSend != null) {
            buttonSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessage();
                }
            });
        }
        
        // Send on Enter/IME action
        if (editTextMessage != null) {
            editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                }
                return false;
            });
        }
        
        // Attach button
        if (buttonAttach != null) {
            buttonAttach.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showImageSourceDialog();
                }
            });
        }
        
        // Video call button (placeholder - can be implemented later)
        if (buttonVideoCall != null) {
            buttonVideoCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(ChatActivity.this, "Video call feature coming soon", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Voice call button (placeholder - can be implemented later)
        if (buttonVoiceCall != null) {
            buttonVoiceCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(ChatActivity.this, "Voice call feature coming soon", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Microphone button (placeholder - can be implemented later)
        if (buttonMicrophone != null) {
            buttonMicrophone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(ChatActivity.this, "Voice message feature coming soon", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        if (chatId == null) {
            Toast.makeText(this, "Chat is initializing. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to send messages.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Store the message text before clearing
        final String messageToSend = messageText;
        
        // Clear text field immediately and disable input while sending
        editTextMessage.setText("");
        editTextMessage.setEnabled(false);
        buttonSend.setEnabled(false);
        buttonAttach.setEnabled(false);
        editTextMessage.setHint("Sending...");
        
        String userId = mAuth.getCurrentUser().getUid();
        String messageId = UUID.randomUUID().toString();

        com.example.hinurse20.models.ChatMessage message = 
                new com.example.hinurse20.models.ChatMessage(messageId, chatId, userId, 
                        "You", messageToSend, true);

        db.collection("chat_messages")
                .document(messageId)
                .set(message)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Always re-enable UI elements regardless of success/failure
                        editTextMessage.setEnabled(true);
                        buttonSend.setEnabled(true);
                        buttonAttach.setEnabled(true);
                        editTextMessage.setHint("Type a message");
                        
                        if (task.isSuccessful()) {
                            // Mark as sent with timestamp
                            db.collection("chat_messages")
                                    .document(messageId)
                                    .update("status", "sent", "sentAt", new Date())
                                    .addOnFailureListener(e -> android.util.Log.e("ChatActivity", "Failed to update sent status: " + e.getMessage()));
                            loadMessages();
                        } else {
                            String msg = (task.getException() != null) ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(ChatActivity.this, "Failed to send message: " + msg, 
                                    Toast.LENGTH_LONG).show();
                            // Restore the message text to the input field so user doesn't lose it
                            editTextMessage.setText(messageToSend);
                        }
                    }
                });
    }

    private void uploadAndSendImage(@NonNull Uri uri) {
        if (chatId == null) {
            Toast.makeText(this, "Chat is initializing. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to send images.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable text input while uploading but keep it visible
        editTextMessage.setEnabled(false);
        buttonSend.setEnabled(false);
        buttonAttach.setEnabled(false);
        editTextMessage.setHint("Uploading image...");
        
        String userId = mAuth.getCurrentUser().getUid();
        String messageId = UUID.randomUUID().toString();
        
        Toast.makeText(this, "Uploading image to Cloudinary...", Toast.LENGTH_SHORT).show();
        
        // Upload to Cloudinary
        CloudinaryService cloudinaryService = CloudinaryService.getInstance();
        cloudinaryService.uploadImage(uri, messageId, new CloudinaryService.CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                // Create chat message with Cloudinary URL
                com.example.hinurse20.models.ChatMessage imageMsg = new com.example.hinurse20.models.ChatMessage(
                        messageId, chatId, userId, "You", "", true);
                imageMsg.setMessageType("image");
                imageMsg.setImageUrl(imageUrl);
                imageMsg.setStatus("sending");
                imageMsg.setTimestamp(new Date());
                
                // Save to Firestore
                db.collection("chat_messages")
                        .document(messageId)
                        .set(imageMsg)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                // Always re-enable UI elements regardless of success/failure
                                editTextMessage.setEnabled(true);
                                buttonSend.setEnabled(true);
                                buttonAttach.setEnabled(true);
                                editTextMessage.setHint("Type a message");
                                
                                if (task.isSuccessful()) {
                                    // Mark as sent
                                    db.collection("chat_messages")
                                            .document(messageId)
                                            .update("status", "sent", "sentAt", new Date())
                                            .addOnFailureListener(e -> android.util.Log.e("ChatActivity", "Failed to update sent status: " + e.getMessage()));
                                    loadMessages();
                                    android.util.Log.d("ChatActivity", "Image sent successfully: " + imageUrl);
                                } else {
                                    String msg = (task.getException() != null) ? task.getException().getMessage() : "Unknown error";
                                    Toast.makeText(ChatActivity.this, "Failed to save image message: " + msg,
                                            Toast.LENGTH_LONG).show();
                                    android.util.Log.e("ChatActivity", "Failed to save image: " + msg, task.getException());
                                }
                            }
                        });
            }

            @Override
            public void onError(String error) {
                // Always re-enable UI elements if upload fails
                editTextMessage.setEnabled(true);
                buttonSend.setEnabled(true);
                buttonAttach.setEnabled(true);
                editTextMessage.setHint("Type a message");
                
                Toast.makeText(ChatActivity.this, "Upload failed: " + error, Toast.LENGTH_LONG).show();
                android.util.Log.e("ChatActivity", "Cloudinary upload error: " + error);
            }

            @Override
            public void onProgress(long bytes, long totalBytes) {
                int progress = (int) (bytes * 100 / totalBytes);
                android.util.Log.d("ChatActivity", "Upload progress: " + progress + "%");
            }
        });
    }

    private void deleteMessage(com.example.hinurse20.models.ChatMessage message, int position) {
        // Confirm deletion with user
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // First, remove from UI to give immediate feedback
                    adapter.markPendingDeletion(message.getMessageId());
                    adapter.removeItem(position);
                    
                    // If this is an image message, delete from Cloudinary first
                    if ("image".equalsIgnoreCase(message.getMessageType()) && message.getImageUrl() != null) {
                        deleteImageFromCloudinary(message.getImageUrl());
                    }
                    
                    // Delete from Firestore
                    db.collection("chat_messages")
                            .document(message.getMessageId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("ChatActivity", "Message deleted successfully");
                                Toast.makeText(ChatActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
                                // Remove pending deletion status
                                adapter.removePendingDeletion(message.getMessageId());
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("ChatActivity", "Error deleting message: " + e.getMessage(), e);
                                Toast.makeText(ChatActivity.this, "Error deleting message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                // Remove pending deletion status
                                adapter.removePendingDeletion(message.getMessageId());
                                // If deletion fails, reload messages to restore the deleted item
                                loadMessages();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImageFromCloudinary(String imageUrl) {
        // Extract public ID from Cloudinary URL
        // Example URL: https://res.cloudinary.com/dlvgoszbo/image/upload/v1732082659/hinurse/chat/uuid.jpg
        try {
            if (imageUrl != null && imageUrl.contains("cloudinary.com")) {
                // Extract the public ID part (everything after the last slash, before the extension)
                String[] parts = imageUrl.split("/");
                if (parts.length > 0) {
                    String fileName = parts[parts.length - 1]; // e.g., "uuid.jpg"
                    String publicId = "hinurse/chat/" + fileName.split("\\.")[0]; // e.g., "hinurse/chat/uuid"
                    
                    // Delete from Cloudinary using the service
                    CloudinaryService cloudinaryService = CloudinaryService.getInstance();
                    cloudinaryService.deleteImage(publicId, new CloudinaryService.CloudinaryDeleteCallback() {
                        @Override
                        public void onSuccess() {
                            android.util.Log.d("ChatActivity", "Cloudinary image deleted successfully: " + publicId);
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.e("ChatActivity", "Error deleting Cloudinary image: " + error);
                        }
                    });
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ChatActivity", "Error parsing Cloudinary URL for deletion: " + e.getMessage(), e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("ChatActivity", "Camera permission granted");
                // Permission granted, proceed with camera
                dispatchTakePictureIntent();
            } else {
                android.util.Log.d("ChatActivity", "Camera permission denied");
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showImageSourceDialog() {
        // Always show both options
        String[] options = {"Gallery", "Camera"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Gallery
                        pickImageLauncher.launch("image/*");
                    } else {
                        // Camera - check permission and request if needed
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                                == PackageManager.PERMISSION_GRANTED) {
                            dispatchTakePictureIntent();
                        } else {
                            // Request camera permission
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA},
                                    PERMISSION_REQUEST_CAMERA);
                        }
                    }
                })
                .show();
    }

    private void dispatchTakePictureIntent() {
        // Check for camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
            return;
        }
        
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                android.util.Log.e("ChatActivity", "Error creating image file", ex);
                Toast.makeText(this, "Error creating image file: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.hinurse20.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void loadMessages() {
        attachMessagesListener();
    }
    
    private void attachMessagesListener() {
        if (chatId == null) return;
        if (messagesReg != null) { messagesReg.remove(); messagesReg = null; }
        
        messagesReg = db.collection("chat_messages")
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(ChatActivity.this, "Live load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        int currentMessageCount = snapshots != null ? snapshots.size() : 0;
                        
                        // Check for new messages (notify if count increased and activity is not visible or in background)
                        // Only check if we have a previous count (skip first load)
                        // Note: Global listener (ChatNotificationHelper) also monitors, but this handles immediate notifications
                        // when user is in the app but not viewing this specific chat
                        if (previousMessageCount > 0 && currentMessageCount > previousMessageCount && !isActivityVisible) {
                            // Find the newest message that's not from current user
                            String currentUserId = mAuth != null && mAuth.getCurrentUser() != null 
                                    ? mAuth.getCurrentUser().getUid() : null;
                            
                            if (currentUserId != null && snapshots != null && !snapshots.isEmpty()) {
                                // Get the last document (newest message)
                                DocumentSnapshot lastDoc = snapshots.getDocuments().get(snapshots.size() - 1);
                                String senderId = lastDoc.getString("senderId");
                                
                                // Only notify if message is from someone else
                                if (senderId != null && !senderId.equals(currentUserId)) {
                                    String senderName = lastDoc.getString("senderName");
                                    if (senderName == null || senderName.isEmpty()) {
                                        senderName = textViewContactName != null 
                                                ? textViewContactName.getText().toString() 
                                                : "Nurse";
                                    }
                                    if (senderName == null || senderName.isEmpty()) {
                                        senderName = "Nurse";
                                    }
                                    String messageText = lastDoc.getString("message");
                                    if (messageText == null || messageText.isEmpty()) {
                                        messageText = "New message";
                                    }
                                    
                                    // Show notification - use application context for background safety
                                    try {
                                        String messageId = lastDoc.getId();
                                        if (messageId == null || messageId.isEmpty()) {
                                            messageId = lastDoc.getString("messageId");
                                        }
                                        
                                        NotificationHelper.showChatNotification(
                                                ChatActivity.this.getApplicationContext(), 
                                                senderName, 
                                                messageText);
                                        
                                        // Mark as notified to prevent duplicate from global listener
                                        if (messageId != null && !messageId.isEmpty()) {
                                            ChatNotificationHelper.markMessageAsNotified(messageId, ChatActivity.this);
                                        }
                                        
                                        android.util.Log.d("ChatActivity", "Notification shown for new message from " + senderName);
                                    } catch (Exception ex) {
                                        android.util.Log.e("ChatActivity", "Error showing notification: " + ex.getMessage(), ex);
                                    }
                                }
                            }
                        }
                        
                        previousMessageCount = currentMessageCount;
                        
                        messagesList.clear();
                        String currentUserId = mAuth != null && mAuth.getCurrentUser() != null 
                                ? mAuth.getCurrentUser().getUid() : null;
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            com.example.hinurse20.models.ChatMessage message = document.toObject(com.example.hinurse20.models.ChatMessage.class);
                            messagesList.add(message);
                            // Mark incoming messages as read when visible
                            if (isActivityVisible && currentUserId != null) {
                                String senderId = document.getString("senderId");
                                Boolean isRead = document.getBoolean("isRead");
                                if (senderId != null && !senderId.equals(currentUserId)) {
                                    if (isRead == null || !isRead || (document.getString("status") != null && !"read".equalsIgnoreCase(document.getString("status")))) {
                                        db.collection("chat_messages")
                                                .document(document.getId())
                                                .update("isRead", true, "status", "read", "readAt", new Date())
                                                .addOnFailureListener(err -> android.util.Log.e("ChatActivity", "Failed to mark read: " + err.getMessage()));
                                    }
                                }
                            }
                        }
                        adapter.setItems(messagesList);
                        if (adapter.getItemCount() > 0) {
                            recyclerViewMessages.scrollToPosition(adapter.getItemCount() - 1);
                        }
                    }
                });
    }

    private void loadFirstAvailableNurse(String currentUserId) {
        if (db == null) {
            Toast.makeText(this, "Database not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            db.collection("users")
                    .whereEqualTo("role", "nurse")
                    .limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                                DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                                String id = doc.getString("userId");
                                if (id == null) id = doc.getString("uid");
                                if (id == null) id = doc.getId();
                                if (id != null && !id.isEmpty()) {
                                    initChatWithNurse(currentUserId, id);
                                }
                            } else {
                                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                android.util.Log.e("ChatActivity", "Failed to load nurses: " + errorMsg);
                                Toast.makeText(ChatActivity.this, "No nurses available", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ChatActivity", "Error loading nurses: " + e.getMessage(), e);
                        if (e instanceof SecurityException) {
                            Toast.makeText(ChatActivity.this, "Authentication error. Please check your Firebase configuration.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ChatActivity.this, "Failed to load nurses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (SecurityException e) {
            android.util.Log.e("ChatActivity", "SecurityException: " + e.getMessage(), e);
            Toast.makeText(this, "Service authentication failed. Please check your app configuration.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("ChatActivity", "Unexpected error: " + e.getMessage(), e);
            Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadNurseProfile(String nurseId) {
        if (nurseProfileReg != null) {
            nurseProfileReg.remove();
            nurseProfileReg = null;
        }
        
        nurseProfileReg = db.collection("users").document(nurseId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(DocumentSnapshot document, FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        
                        if (document != null && document.exists()) {
                            // Load profile picture
                            String photoUrl = document.getString("photoUrl");
                            String photoBase64 = document.getString("photoBase64");
                            
                            // Check if nurse has selected an icon
                            Long selectedIconIndex = document.getLong("selectedIconIndex");
                            if (selectedIconIndex != null && selectedIconIndex >= 0 && selectedIconIndex < ProfileIconHelper.getAllProfileIcons().size()) {
                                // Use selected icon
                                int iconRes = ProfileIconHelper.getProfileIconByIndex(selectedIconIndex.intValue());
                                imageProfile.setImageResource(iconRes);
                            } else if (photoUrl != null && !photoUrl.isEmpty()) {
                                // Use photo if available
                                int defaultIcon = ProfileIconHelper.getProfileIconForUser(nurseId);
                                Glide.with(ChatActivity.this)
                                        .load(photoUrl)
                                        .circleCrop()
                                        .placeholder(defaultIcon)
                                        .error(defaultIcon)
                                        .into(imageProfile);
                            } else if (photoBase64 != null && !photoBase64.isEmpty()) {
                                try {
                                    byte[] bytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT);
                                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    imageProfile.setImageBitmap(bmp);
                                    // Make it circular
                                    imageProfile.setClipToOutline(true);
                                } catch (Exception ex) {
                                    int defaultIcon = ProfileIconHelper.getProfileIconForUser(nurseId);
                                    imageProfile.setImageResource(defaultIcon);
                                }
                            } else {
                                // Use default icon based on nurse ID
                                int defaultIcon = ProfileIconHelper.getProfileIconForUser(nurseId);
                                imageProfile.setImageResource(defaultIcon);
                            }
                            
                            // Load name
                            String name = document.getString("name");
                            if (name == null || name.trim().isEmpty()) {
                                String first = document.getString("firstName");
                                String last = document.getString("lastName");
                                name = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                            }
                            if (name == null || name.isEmpty()) {
                                name = "Nurse";
                            }
                            textViewContactName.setText(name);
                        }
                    }
                });
    }

    private void initChatWithNurse(String currentUserId, String pickedNurseId) {
        this.nurseId = pickedNurseId;
        String combined = currentUserId.compareTo(pickedNurseId) < 0
                ? currentUserId + "_" + pickedNurseId
                : pickedNurseId + "_" + currentUserId;
        chatId = "chat_" + combined;
        chatImagesRef = FirebaseStorage.getInstance().getReference().child("chat_images").child(chatId);
        SharedPreferences prefs = getSharedPreferences("hinurse_prefs", MODE_PRIVATE);
        prefs.edit().putString("default_nurse_id", pickedNurseId).apply();
        
        // Load nurse profile information
        loadNurseProfile(pickedNurseId);
        
        attachMessagesListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        previousMessageCount = messagesList != null ? messagesList.size() : 0;
        
        // Mark all current messages as notified since user is viewing them
        if (messagesList != null && !messagesList.isEmpty()) {
            for (com.example.hinurse20.models.ChatMessage message : messagesList) {
                if (message != null && message.getMessageId() != null) {
                    ChatNotificationHelper.markMessageAsNotified(message.getMessageId(), this);
                }
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
        // Keep listener active for background notifications
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Keep listener active for background notifications
        // Only remove in onDestroy()
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listeners when activity is destroyed
        if (messagesReg != null) { 
            messagesReg.remove(); 
            messagesReg = null; 
        }
        if (nurseProfileReg != null) { 
            nurseProfileReg.remove(); 
            nurseProfileReg = null; 
        }
    }
}




