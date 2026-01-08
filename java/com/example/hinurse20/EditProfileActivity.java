package com.example.hinurse20;

import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends BaseActivity {
    private EditText editTextFirstName, editTextMiddleName, editTextLastName, editTextPhone, editTextEmail, editTextDateOfBirth, editTextStudentId;
    private android.widget.AutoCompleteTextView spinnerGender;
    private ImageButton buttonBack;
    private ImageButton buttonSave;
    private Button buttonChangeIcon;
    private ImageView imageAvatarEdit;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private java.util.Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadUserProfile();
    }

    private void initializeViews() {
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextMiddleName = findViewById(R.id.editTextMiddleName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextDateOfBirth = findViewById(R.id.editTextDateOfBirth);
        editTextStudentId = findViewById(R.id.editTextStudentId);
        spinnerGender = findViewById(R.id.spinnerGender);
        buttonBack = findViewById(R.id.buttonBack);
        buttonSave = findViewById(R.id.buttonSave);
        buttonChangeIcon = findViewById(R.id.buttonChangeIcon);
        progressBar = findViewById(R.id.progressBar);
        imageAvatarEdit = findViewById(R.id.imageAvatarEdit);
        calendar = java.util.Calendar.getInstance();
        
        // Setup gender dropdown
        if (spinnerGender != null) {
            String[] genderOptions = {"Male", "Female", "Other", "Prefer not to say"};
            android.widget.ArrayAdapter<String> genderAdapter = new android.widget.ArrayAdapter<>(this, 
                    android.R.layout.simple_dropdown_item_1line, genderOptions);
            spinnerGender.setAdapter(genderAdapter);
        }
        
        // Set email (read-only)
        if (editTextEmail != null && mAuth.getCurrentUser() != null) {
            editTextEmail.setText(mAuth.getCurrentUser().getEmail());
            // Set email text color to blue
            editTextEmail.setTextColor(getResources().getColor(R.color.blue));
        }
        
        // Ensure all EditText fields have visible text colors
        if (editTextFirstName != null) {
            editTextFirstName.setTextColor(getResources().getColor(R.color.purple));
        }
        if (editTextMiddleName != null) {
            editTextMiddleName.setTextColor(getResources().getColor(R.color.purple));
        }
        if (editTextLastName != null) {
            editTextLastName.setTextColor(getResources().getColor(R.color.purple));
        }
        if (editTextPhone != null) {
            editTextPhone.setTextColor(getResources().getColor(R.color.purple));
        }
        if (spinnerGender != null) {
            spinnerGender.setTextColor(getResources().getColor(R.color.purple));
        }
        if (editTextStudentId != null) {
            editTextStudentId.setTextColor(getResources().getColor(R.color.purple));
        }
        
        // Setup date of birth picker
        if (editTextDateOfBirth != null) {
            editTextDateOfBirth.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePickerDialog();
                }
            });
            editTextDateOfBirth.setTextColor(getResources().getColor(R.color.purple));
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
        
        // Save button (checkmark in header)
        if (buttonSave != null) {
            buttonSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveUserProfile();
                }
            });
        }

        // Change Icon button
        if (buttonChangeIcon != null) {
            buttonChangeIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Navigate to IconSelectionActivity
                    Intent intent = new Intent(EditProfileActivity.this, IconSelectionActivity.class);
                    startActivityForResult(intent, 2002);
                }
            });
        }
    }

    private void loadUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String userId = mAuth.getCurrentUser().getUid();
                                String photoUrl = document.getString("photoUrl");
                                
                                // Check if user has selected an icon
                                Long selectedIconIndex = document.getLong("selectedIconIndex");
                                if (selectedIconIndex != null && selectedIconIndex >= 0 && selectedIconIndex < ProfileIconHelper.getAllProfileIcons().size()) {
                                    // Use selected icon
                                    int iconRes = ProfileIconHelper.getProfileIconByIndex(selectedIconIndex.intValue());
                                    imageAvatarEdit.setImageResource(iconRes);
                                } else if (photoUrl != null && !photoUrl.isEmpty()) {
                                    // Use photo if available
                                    int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                    Glide.with(EditProfileActivity.this)
                                            .load(photoUrl)
                                            .circleCrop()
                                            .placeholder(defaultIcon)
                                            .error(defaultIcon)
                                            .into(imageAvatarEdit);
                                } else {
                                    // Use default icon based on user ID
                                    int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                                    imageAvatarEdit.setImageResource(defaultIcon);
                                }
                                if (editTextFirstName != null) {
                                    String firstName = document.getString("firstName");
                                    editTextFirstName.setText(firstName != null ? firstName : "");
                                    // Set text color to purple
                                    editTextFirstName.setTextColor(getResources().getColor(R.color.purple));
                                }
                                if (editTextMiddleName != null) {
                                    String middleName = document.getString("middleName");
                                    editTextMiddleName.setText(middleName != null ? middleName : "");
                                    // Set text color to purple
                                    editTextMiddleName.setTextColor(getResources().getColor(R.color.purple));
                                }
                                if (editTextLastName != null) {
                                    String lastName = document.getString("lastName");
                                    editTextLastName.setText(lastName != null ? lastName : "");
                                    // Set text color to purple
                                    editTextLastName.setTextColor(getResources().getColor(R.color.purple));
                                }
                                if (editTextPhone != null) {
                                    String phone = document.getString("phoneNumber");
                                    editTextPhone.setText(phone != null ? phone : "");
                                    // Set text color to purple
                                    editTextPhone.setTextColor(getResources().getColor(R.color.purple));
                                }
                                if (spinnerGender != null) {
                                    String gender = document.getString("gender");
                                    if (gender != null && !gender.isEmpty()) {
                                        spinnerGender.setText(gender, false);
                                        // Set text color to purple
                                        spinnerGender.setTextColor(getResources().getColor(R.color.purple));
                                    }
                                }
                                // Set email text color to blue
                                if (editTextEmail != null && mAuth.getCurrentUser() != null) {
                                    editTextEmail.setText(mAuth.getCurrentUser().getEmail());
                                    editTextEmail.setTextColor(getResources().getColor(R.color.blue));
                                }
                                
                                // Set date of birth
                                if (editTextDateOfBirth != null) {
                                    Object dobObj = document.get("dateOfBirth");
                                    if (dobObj != null) {
                                        try {
                                            java.util.Date dob = null;
                                            if (dobObj instanceof com.google.firebase.Timestamp) {
                                                dob = ((com.google.firebase.Timestamp) dobObj).toDate();
                                            } else if (dobObj instanceof java.util.Date) {
                                                dob = (java.util.Date) dobObj;
                                            }
                                            if (dob != null) {
                                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                                                editTextDateOfBirth.setText(sdf.format(dob));
                                                calendar.setTime(dob);
                                            }
                                        } catch (Exception e) {
                                            editTextDateOfBirth.setText("");
                                        }
                                    }
                                    editTextDateOfBirth.setTextColor(getResources().getColor(R.color.purple));
                                }
                                
                                // Set student ID
                                String studentId = document.getString("studentId");
                                if (editTextStudentId != null) {
                                    editTextStudentId.setText(studentId != null ? studentId : "");
                                    editTextStudentId.setTextColor(getResources().getColor(R.color.purple));
                                }
                            }
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Failed to load profile",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showDatePickerDialog() {
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                this,
                new android.app.DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(java.util.Calendar.YEAR, year);
                        calendar.set(java.util.Calendar.MONTH, month);
                        calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                        editTextDateOfBirth.setText(sdf.format(calendar.getTime()));
                    }
                },
                year, month, day
        );
        
        // Set maximum date to today (can't select future dates)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveUserProfile() {
        String firstName = editTextFirstName != null ? editTextFirstName.getText().toString().trim() : "";
        String middleName = editTextMiddleName != null ? editTextMiddleName.getText().toString().trim() : "";
        String lastName = editTextLastName != null ? editTextLastName.getText().toString().trim() : "";
        String phone = editTextPhone != null ? editTextPhone.getText().toString().trim() : "";
        String gender = spinnerGender != null ? spinnerGender.getText().toString().trim() : "";
        String studentId = editTextStudentId != null ? editTextStudentId.getText().toString().trim() : "";

        if (firstName.isEmpty()) {
            if (editTextFirstName != null) {
                editTextFirstName.setError("First name is required");
                editTextFirstName.requestFocus();
            }
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();

        // Update Firestore document directly
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        if (!middleName.isEmpty()) {
            updates.put("middleName", middleName);
        }
        updates.put("lastName", lastName);
        updates.put("phoneNumber", phone);
        
        // Update fullName field with first + middle + last name
        StringBuilder fullNameBuilder = new StringBuilder();
        fullNameBuilder.append(firstName);
        if (!middleName.isEmpty()) {
            fullNameBuilder.append(" ").append(middleName);
        }
        if (!lastName.isEmpty()) {
            fullNameBuilder.append(" ").append(lastName);
        }
        updates.put("fullName", fullNameBuilder.toString().trim());
        if (!gender.isEmpty()) {
            updates.put("gender", gender);
        }
        if (!studentId.isEmpty()) {
            updates.put("studentId", studentId);
        }
        // Save date of birth if calendar is set
        if (calendar != null && editTextDateOfBirth != null && !editTextDateOfBirth.getText().toString().trim().isEmpty()) {
            updates.put("dateOfBirth", calendar.getTime());
        }
        updates.put("updatedAt", new Date());

        db.collection("users").document(userId)
                .update(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(EditProfileActivity.this, "Profile updated successfully!",
                                    Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Failed to update profile",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private static final int REQ_PICK_IMAGE = 2001;

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        } else if (requestCode == 2002 && resultCode == RESULT_OK && data != null) {
            // Icon selection result
            int iconIndex = data.getIntExtra("iconIndex", -1);
            if (iconIndex >= 0 && iconIndex < ProfileIconHelper.getAllProfileIcons().size()) {
                int iconRes = ProfileIconHelper.getProfileIconByIndex(iconIndex);
                imageAvatarEdit.setImageResource(iconRes);
                // Profile icon is already saved by IconSelectionActivity
                Toast.makeText(this, "Icon updated successfully!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImage(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);
        String userId = mAuth.getCurrentUser().getUid();
        // Explicitly target the correct Storage bucket to avoid 404 upload session errors
        StorageReference storageRef = FirebaseStorage.getInstance("gs://hinurse-3c1ee.appspot.com").getReference()
                .child("profile_images/" + userId + ".jpg");

        storageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    int defaultIcon = ProfileIconHelper.getProfileIconForUser(userId);
                    
                    if (task.isSuccessful()) {
                        Uri downloadUri = (Uri) task.getResult();
                        if (downloadUri != null) {
                            String url = downloadUri.toString();
                            Glide.with(EditProfileActivity.this)
                                    .load(url)
                                    .circleCrop()
                                    .placeholder(defaultIcon)
                                    .error(defaultIcon)
                                    .into(imageAvatarEdit);
                            // Save photoUrl to user doc
                            db.collection("users").document(userId).update("photoUrl", url);
                        }
                    } else {
                        String msg = (task.getException() != null) ? task.getException().getMessage() : "Unknown error";
                        // Fallback: store compressed Base64 in Firestore (no Storage required)
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true);
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                            db.collection("users").document(userId).update("photoBase64", base64);
                            Glide.with(EditProfileActivity.this)
                                    .load(imageUri)
                                    .circleCrop()
                                    .placeholder(defaultIcon)
                                    .error(defaultIcon)
                                    .into(imageAvatarEdit);
                            Toast.makeText(EditProfileActivity.this, "Saved avatar without Storage.", Toast.LENGTH_SHORT).show();
                        } catch (Exception ex) {
                            imageAvatarEdit.setImageResource(defaultIcon);
                            Toast.makeText(EditProfileActivity.this, "Failed to save image locally.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
