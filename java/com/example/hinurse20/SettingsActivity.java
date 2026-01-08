package com.example.hinurse20;

import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;



    private Switch switchNotifications;
    private Spinner spinnerReminderFrequency;

    private TextView textAppVersion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        switchNotifications = findViewById(R.id.switchNotifications);
        spinnerReminderFrequency = findViewById(R.id.spinnerReminderFrequency);

        textAppVersion = findViewById(R.id.textAppVersion);

        Button buttonChangePassword = findViewById(R.id.buttonChangePassword);
        Button buttonLogout = findViewById(R.id.buttonLogout);
        Button buttonViewProfile = findViewById(R.id.buttonViewProfile);
        Button buttonClearCache = findViewById(R.id.buttonClearCache);
        Button buttonPermissions = findViewById(R.id.buttonPermissions);
        Button buttonDataExport = findViewById(R.id.buttonDataExport);
        Button buttonDataDelete = findViewById(R.id.buttonDataDelete);
        Button buttonDeveloperInfo = findViewById(R.id.buttonDeveloperInfo);
        Button buttonOpenSourceLicenses = findViewById(R.id.buttonOpenSourceLicenses);
        
        android.widget.ImageButton buttonBack = findViewById(R.id.buttonBack);
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }

        setupAdapters();
        loadState();
        wireListeners();
        populateVersion();

        buttonLogout.setOnClickListener(v -> {
            // Sign out of Firebase auth session
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

            // Sign out from Google (if used) and then navigate to SignIn
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build());

            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent intent = new Intent(this, SignInActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finishAffinity();
            });
        });

        buttonViewProfile.setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));

        buttonClearCache.setOnClickListener(v -> Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show());

        buttonPermissions.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        });

        buttonDataExport.setOnClickListener(v -> Toast.makeText(this, "Data export requested", Toast.LENGTH_SHORT).show());

        buttonDataDelete.setOnClickListener(v -> Toast.makeText(this, "Data delete requested", Toast.LENGTH_SHORT).show());

        buttonDeveloperInfo.setOnClickListener(v -> Toast.makeText(this, "Developer info", Toast.LENGTH_SHORT).show());

        buttonOpenSourceLicenses.setOnClickListener(v -> Toast.makeText(this, "Open-source licenses", Toast.LENGTH_SHORT).show());
    }

    private void setupAdapters() {
        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Never", "Daily", "Weekly"});
        reminderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReminderFrequency.setAdapter(reminderAdapter);
    }

    private void loadState() {
        boolean notifications = prefs.getBoolean("notifications", true);
        switchNotifications.setChecked(notifications);

        int reminderIndex = prefs.getInt("reminder_idx", 1);
        spinnerReminderFrequency.setSelection(Math.max(0, Math.min(reminderIndex, 2)));
    }

    private void wireListeners() {
        // Set up change password button first to ensure it's not overridden
        Button buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));

        // Set up other listeners
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("notifications", isChecked).apply());

        spinnerReminderFrequency.setOnItemSelectedListener(new SimpleOnItemSelected(prefs, "reminder_idx"));
    }

    private void populateVersion() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            textAppVersion.setText("App version: " + version);
        } catch (Exception e) {
            textAppVersion.setText("App version: -");
        }
    }

    private static class SimpleOnItemSelected implements android.widget.AdapterView.OnItemSelectedListener {
        private final SharedPreferences prefs;
        private final String key;
        SimpleOnItemSelected(SharedPreferences prefs, String key) { this.prefs = prefs; this.key = key; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            prefs.edit().putInt(key, position).apply();
        }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }
}
