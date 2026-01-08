package com.example.hinurse20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends BaseActivity {
    private TextView textFullName, textEmail, textEmailInfo, textPhone, textGender, textDateOfBirth, textStudentId;
    private ImageView imageAvatar;
    private ImageButton buttonBack;
    private ImageButton buttonSettings;
    private Button buttonEditProfile;
    private Button buttonLogout;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_read_only);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadUserProfile();
    }

    private void initializeViews() {
        imageAvatar = findViewById(R.id.imageAvatar);
        textFullName = findViewById(R.id.textFullName);
        textEmail = findViewById(R.id.textEmail);
        textEmailInfo = findViewById(R.id.textEmailInfo);
        textPhone = findViewById(R.id.textPhone);
        textGender = findViewById(R.id.textGender);
        textDateOfBirth = findViewById(R.id.textDateOfBirth);
        textStudentId = findViewById(R.id.textStudentId);
        buttonEditProfile = findViewById(R.id.buttonEditProfile);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonBack = findViewById(R.id.buttonBack);
        buttonSettings = findViewById(R.id.buttonSettings);
        progressBar = findViewById(R.id.progressBar);

        if (mAuth.getCurrentUser() != null) {
            textEmail.setText(mAuth.getCurrentUser().getEmail());
        }
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
        
        // Edit Profile button
        if (buttonEditProfile != null) {
            buttonEditProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(UserProfileActivity.this, EditProfileActivity.class);
                    startActivityForResult(intent, 1001);
                }
            });
        }

        // Logout button
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAuth.signOut();
                    Intent intent = new Intent(UserProfileActivity.this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }

        // Settings button
        if (buttonSettings != null) {
            buttonSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(UserProfileActivity.this, SettingsActivity.class));
                }
            });
        }
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String userId = mAuth.getCurrentUser().getUid();
                                String photoUrl = document.getString("photoUrl");
                                String photoBase64 = document.getString("photoBase64");
                                
                                // Check if user has selected an icon
                                Long selectedIconIndex = document.getLong("selectedIconIndex");
                                if (selectedIconIndex != null && selectedIconIndex >= 0 && selectedIconIndex < ProfileIconHelper.getAllProfileIcons().size()) {
                                    // Use selected icon
                                    int iconRes = ProfileIconHelper.getProfileIconByIndex(selectedIconIndex.intValue());
                                    imageAvatar.setImageResource(iconRes);
                                } else if (photoUrl != null && !photoUrl.isEmpty()) {
                                    // Use photo if available
                                    int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                    Glide.with(UserProfileActivity.this)
                                            .load(photoUrl)
                                            .circleCrop()
                                            .placeholder(defaultIcon)
                                            .error(defaultIcon)
                                            .into(imageAvatar);
                                } else if (photoBase64 != null && !photoBase64.isEmpty()) {
                                    try {
                                        byte[] bytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT);
                                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        imageAvatar.setImageBitmap(bmp);
                                    } catch (Exception ignore) {
                                        int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                        imageAvatar.setImageResource(defaultIcon);
                                    }
                                } else {
                                    // Use default icon based on user ID
                                    int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                    imageAvatar.setImageResource(defaultIcon);
                                }

                                String firstName = document.getString("firstName");
                                String middleName = document.getString("middleName");
                                String lastName = document.getString("lastName");
                                
                                // Build full name including middle name
                                StringBuilder fullNameBuilder = new StringBuilder();
                                if (firstName != null && !firstName.isEmpty()) {
                                    fullNameBuilder.append(firstName);
                                }
                                if (middleName != null && !middleName.isEmpty()) {
                                    if (fullNameBuilder.length() > 0) {
                                        fullNameBuilder.append(" ");
                                    }
                                    fullNameBuilder.append(middleName);
                                }
                                if (lastName != null && !lastName.isEmpty()) {
                                    if (fullNameBuilder.length() > 0) {
                                        fullNameBuilder.append(" ");
                                    }
                                    fullNameBuilder.append(lastName);
                                }
                                
                                String fullName = fullNameBuilder.length() > 0 ? fullNameBuilder.toString().trim() : "User";
                                if (textFullName != null) {
                                    textFullName.setText(fullName);
                                    // Set text color to blue
                                    textFullName.setTextColor(getResources().getColor(R.color.blue));
                                }

                                // Set email
                                String email = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "";
                                if (textEmail != null) {
                                    textEmail.setText(email != null && !email.isEmpty() ? email : "Not provided");
                                    // Set text color to blue
                                    textEmail.setTextColor(getResources().getColor(R.color.blue));
                                }
                                // Also set email in Personal Information section
                                if (textEmailInfo != null) {
                                    textEmailInfo.setText(email != null && !email.isEmpty() ? email : "Not provided");
                                    textEmailInfo.setTextColor(getResources().getColor(R.color.purple));
                                }

                                // Set phone
                                String phone = document.getString("phoneNumber");
                                if (textPhone != null) {
                                    textPhone.setText(phone != null && !phone.isEmpty() ? phone : "Not provided");
                                    textPhone.setTextColor(getResources().getColor(R.color.purple));
                                }

                                // Set gender
                                String gender = document.getString("gender");
                                if (textGender != null) {
                                    textGender.setText(gender != null && !gender.isEmpty() ? gender : "Not specified");
                                    textGender.setTextColor(getResources().getColor(R.color.purple));
                                }

                                // Set date of birth
                                if (textDateOfBirth != null) {
                                    Object dobObj = document.get("dateOfBirth");
                                    if (dobObj != null) {
                                        try {
                                            if (dobObj instanceof com.google.firebase.Timestamp) {
                                                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) dobObj;
                                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                                                textDateOfBirth.setText(sdf.format(timestamp.toDate()));
                                            } else if (dobObj instanceof java.util.Date) {
                                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                                                textDateOfBirth.setText(sdf.format((java.util.Date) dobObj));
                                            } else {
                                                textDateOfBirth.setText("Not provided");
                                            }
                                        } catch (Exception e) {
                                            textDateOfBirth.setText("Not provided");
                                        }
                                    } else {
                                        textDateOfBirth.setText("Not provided");
                                    }
                                    textDateOfBirth.setTextColor(getResources().getColor(R.color.purple));
                                }

                                // Set student ID
                                String studentId = document.getString("studentId");
                                if (textStudentId != null) {
                                    textStudentId.setText(studentId != null && !studentId.isEmpty() ? studentId : "Not provided");
                                    textStudentId.setTextColor(getResources().getColor(R.color.purple));
                                }
                            } else {
                                applyPlaceholders();
                                Toast.makeText(UserProfileActivity.this, "No profile found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String msg = (task.getException() != null) ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(UserProfileActivity.this, "Failed to load profile: " + msg,
                                    Toast.LENGTH_LONG).show();
                            applyPlaceholders();
                        }
                    }
                });
    }

    private void applyPlaceholders() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
            if (imageAvatar != null) {
                imageAvatar.setImageResource(defaultIcon);
            }
        }
        if (textFullName != null) {
            textFullName.setText("User");
            textFullName.setTextColor(getResources().getColor(R.color.blue));
        }
        if (textEmail != null) {
            textEmail.setText("Not provided");
            textEmail.setTextColor(getResources().getColor(R.color.blue));
        }
        if (textEmailInfo != null) {
            textEmailInfo.setText("Not provided");
            textEmailInfo.setTextColor(getResources().getColor(R.color.purple));
        }
        if (textPhone != null) {
            textPhone.setText("Not provided");
            textPhone.setTextColor(getResources().getColor(R.color.purple));
        }
        if (textGender != null) {
            textGender.setText("Not specified");
            textGender.setTextColor(getResources().getColor(R.color.purple));
        }
        if (textDateOfBirth != null) {
            textDateOfBirth.setText("Not provided");
            textDateOfBirth.setTextColor(getResources().getColor(R.color.purple));
        }
        if (textStudentId != null) {
            textStudentId.setText("Not provided");
            textStudentId.setTextColor(getResources().getColor(R.color.purple));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            loadUserProfile();
        }
    }
}


