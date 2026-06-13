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
        setTopTitle(getString(R.string.account_details_title));
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
        }, error -> Toast.makeText(this, R.string.profile_load_error, Toast.LENGTH_SHORT).show());
        findViewById(R.id.accountSaveButton).setOnClickListener(v -> saveLocalProfile());
        findViewById(R.id.exportDataButton).setOnClickListener(v -> exportLocalData());
        findViewById(R.id.deleteLocalDataButton).setOnClickListener(v -> confirmDeleteLocalData());
    }

    private void saveLocalProfile() {
        String displayName = displayNameInput == null ? getString(R.string.profile_local_user) : displayNameInput.getText().toString();
        SettingsRepository.setDisplayNameAsync(this, displayName,
                settings -> {
                    if (displayNameInput != null) {
                        displayNameInput.setText(settings.getDisplayName());
                    }
                    if (displayNameHeader != null) {
                        displayNameHeader.setText(settings.getDisplayName());
                    }
                    Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, R.string.profile_save_error, Toast.LENGTH_SHORT).show());
    }

    private void exportLocalData() {
        Toast.makeText(this, R.string.export_preparing, Toast.LENGTH_SHORT).show();
        LocalDataExportRepository.exportAsync(this,
                this::shareExport,
                error -> Toast.makeText(this, R.string.export_error, Toast.LENGTH_SHORT).show());
    }

    private void shareExport(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_subject));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_chooser_title)));
    }

    private void confirmDeleteLocalData() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_local_data_title)
                .setMessage(R.string.delete_local_data_message)
                .setPositiveButton(R.string.delete_confirm, (dialog, which) -> deleteLocalData())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteLocalData() {
        Toast.makeText(this, R.string.delete_local_data_progress, Toast.LENGTH_SHORT).show();
        LocalDataResetRepository.resetAsync(this,
                summary -> {
                    if (displayNameInput != null) {
                        displayNameInput.setText(R.string.profile_local_user);
                    }
                    if (displayNameHeader != null) {
                        displayNameHeader.setText(R.string.profile_local_user);
                    }
                    Toast.makeText(this, R.string.delete_local_data_done, Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, R.string.delete_local_data_error, Toast.LENGTH_SHORT).show());
    }
}
