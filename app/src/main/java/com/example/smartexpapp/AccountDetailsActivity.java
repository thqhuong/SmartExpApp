package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.LocalDataExportRepository;
import com.example.smartexpapp.data.LocalDataResetRepository;
import com.example.smartexpapp.data.SettingsRepository;

public class AccountDetailsActivity extends BaseActivity {
    private EditText displayNameInput;
    private TextView displayNameHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);
        setupChrome(R.id.nav_settings);
        setTopTitle("Account Details");
        useBackButton();
        displayNameInput = findViewById(R.id.displayNameInput);
        displayNameHeader = findViewById(R.id.accountDisplayNameText);
        SettingsRepository.getSettingsAsync(this, settings -> {
            if (displayNameInput != null) {
                displayNameInput.setText(settings.getDisplayName());
            }
            if (displayNameHeader != null) {
                displayNameHeader.setText(settings.getDisplayName());
            }
        }, error -> Toast.makeText(this, "Could not load local profile.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.accountSaveButton).setOnClickListener(v -> saveLocalProfile());
        findViewById(R.id.exportDataButton).setOnClickListener(v -> exportLocalData());
        findViewById(R.id.deleteLocalDataButton).setOnClickListener(v -> confirmDeleteLocalData());
    }

    private void saveLocalProfile() {
        String displayName = displayNameInput == null ? "Local User" : displayNameInput.getText().toString();
        SettingsRepository.setDisplayNameAsync(this, displayName,
                settings -> {
                    if (displayNameInput != null) {
                        displayNameInput.setText(settings.getDisplayName());
                    }
                    if (displayNameHeader != null) {
                        displayNameHeader.setText(settings.getDisplayName());
                    }
                    Toast.makeText(this, "Local profile saved.", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, "Could not save local profile.", Toast.LENGTH_SHORT).show());
    }

    private void exportLocalData() {
        Toast.makeText(this, "Preparing local data export...", Toast.LENGTH_SHORT).show();
        LocalDataExportRepository.exportAsync(this,
                this::shareExport,
                error -> Toast.makeText(this, "Could not export local data.", Toast.LENGTH_SHORT).show());
    }

    private void shareExport(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SmartExpApp local data export");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Export local data"));
    }

    private void confirmDeleteLocalData() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Local Data")
                .setMessage("This removes inventory, scans, recipe cache, agent history, and local profile settings from this device.")
                .setPositiveButton("Delete", (dialog, which) -> deleteLocalData())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteLocalData() {
        Toast.makeText(this, "Deleting local data...", Toast.LENGTH_SHORT).show();
        LocalDataResetRepository.resetAsync(this,
                summary -> {
                    if (displayNameInput != null) {
                        displayNameInput.setText("Local User");
                    }
                    if (displayNameHeader != null) {
                        displayNameHeader.setText("Local User");
                    }
                    Toast.makeText(this, "Local data deleted.", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, "Could not delete local data.", Toast.LENGTH_SHORT).show());
    }
}
