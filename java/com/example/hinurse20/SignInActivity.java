package com.example.hinurse20;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends BaseActivity {
    private EditText editTextEmail, editTextPassword;
    private Button buttonSignIn, buttonSignUp, buttonGoogleSignIn;
    private TextView textViewForgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Initialize prefs for storing auth hints
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn);
        progressBar = findViewById(R.id.progressBar);

        // Configure Google Sign In
        String clientId = getString(R.string.default_web_client_id);
        // Validate client ID
        if (clientId == null || clientId.isEmpty() || clientId.contains("xxxxxxxx")) {
            // Client ID is not properly configured
            buttonGoogleSignIn.setEnabled(false);
            buttonGoogleSignIn.setAlpha(0.5f);
        } else {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(clientId)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        }

        // Set click listeners
        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToSignUp();
            }
        });

        buttonGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String clientId = getString(R.string.default_web_client_id);
                if (clientId == null || clientId.isEmpty() || clientId.contains("xxxxxxxx")) {
                    Toast.makeText(SignInActivity.this, 
                            "Google Sign-In is not configured. Please set up OAuth client ID in Firebase Console and update strings.xml", 
                            Toast.LENGTH_LONG).show();
                } else {
                    signInWithGoogle();
                }
            }
        });

        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainDashboard();
        }
    }

    private void signIn() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Persist current session hints for local auth
                            if (prefs != null) {
                                prefs.edit()
                                        .putString("email", email)
                                        .putString("password", password)
                                        .apply();
                            }
                            navigateToMainDashboard();
                        } else {
                            Toast.makeText(SignInActivity.this, "Authentication failed: " + 
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    private void navigateToMainDashboard() {
        Intent intent = new Intent(SignInActivity.this, MainDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void signInWithGoogle() {
        if (mGoogleSignInClient == null) {
            Toast.makeText(this, "Google Sign-In is not properly configured", Toast.LENGTH_LONG).show();
            return;
        }
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google sign in failed: Unable to get account information", Toast.LENGTH_LONG).show();
                }
            } catch (ApiException e) {
                String errorMessage = "Google sign in failed: ";
                int errorCode = e.getStatusCode();
                if (errorCode == 10) {
                    // DEVELOPER_ERROR - usually means OAuth client ID is not configured correctly
                    errorMessage = "Configuration error: Please check your OAuth client ID in Firebase Console. Error code: " + errorCode;
                } else if (errorCode == 12500) {
                    errorMessage = "Sign in was cancelled";
                } else if (errorCode == 7) {
                    errorMessage = "Network error: Please check your internet connection";
                } else {
                    errorMessage += e.getMessage() + " (Error code: " + errorCode + ")";
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Create user document if it doesn't exist
                            createUserDocumentIfNeeded(user);
                            // Persist current session hints for Google auth
                            if (prefs != null && user != null) {
                                prefs.edit()
                                        .putString("email", user.getEmail() != null ? user.getEmail() : "")
                                        .remove("password")
                                        .apply();
                            }
                            navigateToMainDashboard();
                        } else {
                            Toast.makeText(SignInActivity.this, "Authentication failed: " + 
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void createUserDocumentIfNeeded(FirebaseUser user) {
        String userId = user.getUid();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().exists()) {
                            // Create user document with Google account info
                            com.example.hinurse20.models.User newUser = new com.example.hinurse20.models.User();
                            newUser.setUserId(userId);
                            newUser.setEmail(user.getEmail());
                            newUser.setFirstName(user.getDisplayName() != null ? user.getDisplayName().split(" ")[0] : "");
                            newUser.setLastName(user.getDisplayName() != null && user.getDisplayName().split(" ").length > 1 ? user.getDisplayName().split(" ")[1] : "");
                            newUser.setCreatedAt(new java.util.Date());
                            newUser.setUpdatedAt(new java.util.Date());
                            
                            db.collection("users").document(userId).set(newUser);
                        }
                    }
                });
    }

    private void showForgotPasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        
        // Create the input field
        final EditText input = new EditText(this);
        input.setHint("Enter your email address");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        // Pre-fill with email if already entered
        String email = editTextEmail.getText().toString().trim();
        if (!email.isEmpty()) {
            input.setText(email);
        }
        
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton("Reset", new android.content.DialogInterface.OnClickListener() { 
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String email = input.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(email)) {
                    Toast.makeText(SignInActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(SignInActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                resetPassword(email);
            }
        });
        builder.setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }

    private void resetPassword(String email) {
        progressBar.setVisibility(View.VISIBLE);
        
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        
                        if (task.isSuccessful()) {
                            Toast.makeText(SignInActivity.this, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                            android.util.Log.d("SignInActivity", "Password reset email sent to " + email);
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(SignInActivity.this, "Failed to send reset email: " + errorMessage, Toast.LENGTH_LONG).show();
                            android.util.Log.e("SignInActivity", "Failed to send password reset email", task.getException());
                        }
                    }
                });
    }
}
