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

import java.util.Date;

public class SignUpActivity extends BaseActivity {
    private EditText editTextFirstName, editTextLastName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonSignUp, buttonBackToSignIn, buttonGoogleSignUp;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Initialize prefs for storing auth hints
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Initialize views
        initializeViews();

        // Configure Google Sign In
        String clientId = getString(R.string.default_web_client_id);
        // Validate client ID
        if (clientId == null || clientId.isEmpty() || clientId.contains("xxxxxxxx")) {
            // Client ID is not properly configured
            buttonGoogleSignUp.setEnabled(false);
            buttonGoogleSignUp.setAlpha(0.5f);
        } else {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(clientId)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        }

        setupClickListeners();
    }

    private void initializeViews() {
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        buttonBackToSignIn = findViewById(R.id.buttonBackToSignIn);
        buttonGoogleSignUp = findViewById(R.id.buttonGoogleSignUp);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });

        buttonBackToSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToSignIn();
            }
        });

        buttonGoogleSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String clientId = getString(R.string.default_web_client_id);
                if (clientId == null || clientId.isEmpty() || clientId.contains("xxxxxxxx")) {
                    Toast.makeText(SignUpActivity.this, 
                            "Google Sign-Up is not configured. Please set up OAuth client ID in Firebase Console and update strings.xml", 
                            Toast.LENGTH_LONG).show();
                } else {
                    signUpWithGoogle();
                }
            }
        });
    }

    private void signUp() {
        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(firstName)) {
            editTextFirstName.setError("First name is required");
            editTextFirstName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            editTextLastName.setError("Last name is required");
            editTextLastName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
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

        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.setError("Please confirm your password");
            editTextConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            editTextConfirmPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Create user profile in Firestore
                            createUserProfile(firstName, lastName, email);
                        } else {
                            String errorMessage = "Registration failed: " + task.getException().getMessage();
                            Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void createUserProfile(String firstName, String lastName, String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Create a comprehensive user profile
            com.example.hinurse20.models.User userProfile = new com.example.hinurse20.models.User();
            userProfile.setUserId(user.getUid());
            userProfile.setEmail(email);
            userProfile.setFirstName(firstName);
            userProfile.setLastName(lastName);
            userProfile.setCreatedAt(new Date());
            userProfile.setUpdatedAt(new Date());
            userProfile.setRole("patient");

            db.collection("users").document(user.getUid())
                    .set(userProfile)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(SignUpActivity.this, "Account created successfully!", 
                                        Toast.LENGTH_SHORT).show();
                                navigateToMainDashboard();
                            } else {
                                Toast.makeText(SignUpActivity.this, "Failed to create user profile", 
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMainDashboard() {
        Intent intent = new Intent(SignUpActivity.this, MainDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void signUpWithGoogle() {
        if (mGoogleSignInClient == null) {
            Toast.makeText(this, "Google Sign-Up is not properly configured", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, "Google sign up failed: Unable to get account information", Toast.LENGTH_LONG).show();
                }
            } catch (ApiException e) {
                String errorMessage = "Google sign up failed: ";
                int errorCode = e.getStatusCode();
                if (errorCode == 10) {
                    // DEVELOPER_ERROR - usually means OAuth client ID is not configured correctly
                    errorMessage = "Configuration error: Please check your OAuth client ID in Firebase Console. Error code: " + errorCode;
                } else if (errorCode == 12500) {
                    errorMessage = "Sign up was cancelled";
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
                            // Persist current session hints for Google auth so settings can detect it
                            if (prefs != null && user != null) {
                                prefs.edit()
                                        .putString("email", user.getEmail() != null ? user.getEmail() : "")
                                        .remove("password")
                                        .apply();
                            }
                            navigateToMainDashboard();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Authentication failed: " + 
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
                            newUser.setRole("patient");
                            
                            db.collection("users").document(userId).set(newUser);
                        }
                    }
                });
    }
}



