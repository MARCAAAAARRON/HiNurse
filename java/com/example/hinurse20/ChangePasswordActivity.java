package com.example.hinurse20;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText editCurrentPassword, editNewPassword, editConfirmPassword;
    private TextInputLayout currentPasswordLayout, newPasswordLayout, confirmPasswordLayout;
    private SharedPreferences prefs;
    private LinearLayout passwordFormLayout;
    private TextView googleSignInMessage;
    private Button buttonChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Initialize views
        initializeViews();
        
        // Check authentication method
        checkAuthenticationMethod();
    }
    
    private void checkAuthenticationMethod() {
        // Get the current user's email stored by the app
        String currentUserEmail = prefs.getString("email", "");
        boolean hasLocalPassword = prefs.contains("password") && !TextUtils.isEmpty(prefs.getString("password", ""));

        // Get any cached Google account from the device
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        boolean googleMatchesCurrentUser = false;
        if (account != null && account.getEmail() != null && !TextUtils.isEmpty(currentUserEmail)) {
            googleMatchesCurrentUser = account.getEmail().equalsIgnoreCase(currentUserEmail);
        }

        // Decide which UI to show
        if (googleMatchesCurrentUser) {
            showGoogleSignInMessage(account.getEmail());
        } else if (hasLocalPassword) {
            setupPasswordChangeForm();
        } else {
            // Fallback to local form if uncertain
            setupPasswordChangeForm();
        }
    }

    private void initializeViews() {
        // Find all views
        passwordFormLayout = findViewById(R.id.passwordFormLayout);
        googleSignInMessage = findViewById(R.id.googleSignInMessage);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        
        // Set up back button
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }
    
    private void setupPasswordChangeForm() {
        // Show password form and hide Google message
        passwordFormLayout.setVisibility(View.VISIBLE);
        googleSignInMessage.setVisibility(View.GONE);
        
        // Initialize password fields
        editCurrentPassword = findViewById(R.id.editCurrentPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        
        currentPasswordLayout = findViewById(R.id.currentPasswordLayout);
        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        
        // Set up the change password button
        setupTextWatchers();
        buttonChangePassword.setOnClickListener(v -> attemptChangePassword());
    }
    
    private void showGoogleSignInMessage(String email) {
        // Hide password form and show Google message
        passwordFormLayout.setVisibility(View.GONE);
        googleSignInMessage.setVisibility(View.VISIBLE);
        
        // Update the message with the user's email
        String message = String.format("You're signed in with Google (%s).\n\nTo change your password, please update it in your Google account settings.", email);
        googleSignInMessage.setText(message);
        
        // Change button to open Google account settings
        buttonChangePassword.setText("Open Google Account Settings");
        buttonChangePassword.setOnClickListener(v -> {
            // Open Google account settings in browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                android.net.Uri.parse("https://myaccount.google.com/security"));
            startActivity(browserIntent);
        });
    }

    private void setupTextWatchers() {
        // Clear errors when user types
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentPasswordLayout.isErrorEnabled()) currentPasswordLayout.setError(null);
                if (newPasswordLayout.isErrorEnabled()) newPasswordLayout.setError(null);
                if (confirmPasswordLayout.isErrorEnabled()) confirmPasswordLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editCurrentPassword.addTextChangedListener(clearErrorWatcher);
        editNewPassword.addTextChangedListener(clearErrorWatcher);
        editConfirmPassword.addTextChangedListener(clearErrorWatcher);
    }

    // Method removed as it's now handled in setupPasswordChangeForm() and showGoogleSignInMessage()

    private void attemptChangePassword() {
        // Reset errors
        currentPasswordLayout.setError(null);
        newPasswordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        String currentPassword = editCurrentPassword.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        boolean hasError = false;
        View focusView = null;

        // Validate current password
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordLayout.setError(getString(R.string.error_field_required));
            focusView = editCurrentPassword;
            hasError = true;
        } else if (!isPasswordValid(currentPassword)) {
            currentPasswordLayout.setError(getString(R.string.error_invalid_password));
            focusView = editCurrentPassword;
            hasError = true;
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordLayout.setError(getString(R.string.error_field_required));
            focusView = hasError ? focusView : editNewPassword;
            hasError = true;
        } else if (!isPasswordValid(newPassword)) {
            newPasswordLayout.setError(getString(R.string.error_invalid_password));
            focusView = hasError ? focusView : editNewPassword;
            hasError = true;
        } else if (newPassword.equals(currentPassword)) {
            newPasswordLayout.setError(getString(R.string.error_same_password));
            focusView = hasError ? focusView : editNewPassword;
            hasError = true;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_field_required));
            focusView = hasError ? focusView : editConfirmPassword;
            hasError = true;
        } else if (!confirmPassword.equals(newPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_password_mismatch));
            focusView = hasError ? focusView : editConfirmPassword;
            hasError = true;
        }

        if (hasError) {
            focusView.requestFocus();
            return;
        }

        // TODO: Replace with your actual authentication logic
        String savedPassword = prefs.getString("password", "");
        if (!savedPassword.equals(currentPassword)) {
            currentPasswordLayout.setError(getString(R.string.error_incorrect_password));
            editCurrentPassword.requestFocus();
            return;
        }

        // Save new password and show success message
        prefs.edit()
                .putString("password", newPassword)
                .apply();

        showSuccessDialog();
    }

    private boolean isPasswordValid(String password) {
        // Add your password validation logic here
        // For example: at least 6 characters, contains letter and number
        return password.length() >= 6;
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.password_changed)
                .setMessage(R.string.password_changed_successfully)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Clear fields and go back to settings
                    editCurrentPassword.setText("");
                    editNewPassword.setText("");
                    editConfirmPassword.setText("");
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
