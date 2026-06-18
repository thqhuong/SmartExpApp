package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.LocalDataExportRepository;
import com.example.smartexpapp.data.LocalDataResetRepository;
import com.example.smartexpapp.data.SettingsRepository;
import com.google.android.material.button.MaterialButton;

public class AccountDetailsActivity extends BaseActivity {
    private EditText displayNameInput;
    private TextView displayNameHeader;
    private TextView accountStatusText;
    private TextView accountAuthStateText;
    private EditText accountModeValue;
    private MaterialButton accountAuthButton;
    private MaterialButton accountSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);
        setupChrome(R.id.nav_settings);
        setTopTitle(getString(R.string.account_details_title));
        useBackButton();
        displayNameInput = findViewById(R.id.displayNameInput);
        displayNameHeader = findViewById(R.id.accountDisplayNameText);
        accountStatusText = findViewById(R.id.accountStatusText);
        accountAuthStateText = findViewById(R.id.accountAuthStateText);
        accountModeValue = findViewById(R.id.accountModeValue);
        accountAuthButton = findViewById(R.id.accountAuthButton);
        accountSaveButton = findViewById(R.id.accountSaveButton);
        bindAccountState();
        findViewById(R.id.exportDataButton).setOnClickListener(v -> exportLocalData());
        findViewById(R.id.deleteLocalDataButton).setOnClickListener(v -> confirmDeleteLocalData());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindAccountState();
    }

    private void bindAccountState() {
        AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(this);
        if (authState.isSignedIn()) {
            bindSignedInAccount(authState);
        } else {
            bindLocalAccount(authState);
        }
    }

    private void bindSignedInAccount(AuthStateRepository.AuthState authState) {
        String identity = authState.getBestDisplayName(getString(R.string.profile_local_user));
        if (displayNameInput != null) {
            displayNameInput.setEnabled(false);
            displayNameInput.setText(identity);
        }
        if (displayNameHeader != null) {
            displayNameHeader.setText(identity);
        }
        if (accountStatusText != null) {
            accountStatusText.setText(R.string.account_signed_in_device);
        }
        if (accountModeValue != null) {
            accountModeValue.setText(R.string.signed_in_mode);
        }
        if (accountAuthStateText != null) {
            accountAuthStateText.setText(getString(R.string.auth_signed_in_format, signedInIdentity(authState)));
        }
        if (accountAuthButton != null) {
            accountAuthButton.setVisibility(android.view.View.VISIBLE);
            accountAuthButton.setText(R.string.sign_out_label);
            accountAuthButton.setIconResource(R.drawable.ic_close);
            accountAuthButton.setOnClickListener(v -> {
                AuthStateRepository.signOut(this);
                navigateToSignIn();
            });
        }
        if (accountSaveButton != null) {
            accountSaveButton.setVisibility(android.view.View.GONE);
        }
    }

    private void bindLocalAccount(AuthStateRepository.AuthState authState) {
        if (displayNameInput != null) {
            displayNameInput.setEnabled(true);
        }
        if (accountStatusText != null) {
            accountStatusText.setText(authState.isGuest()
                    ? R.string.account_guest_device
                    : R.string.account_stored_only_device);
        }
        if (accountModeValue != null) {
            accountModeValue.setText(authState.isGuest()
                    ? R.string.guest_mode_label
                    : R.string.no_sign_in_required);
        }
        if (accountAuthStateText != null) {
            accountAuthStateText.setText(authState.isGuest()
                    ? R.string.auth_guest_local
                    : R.string.auth_local_only);
        }
        if (accountAuthButton != null) {
            accountAuthButton.setVisibility(android.view.View.VISIBLE);
            accountAuthButton.setText(R.string.sign_in_label);
            accountAuthButton.setIconResource(R.drawable.ic_info);
            accountAuthButton.setOnClickListener(v -> navigateToSignIn());
        }
        if (accountSaveButton != null) {
            accountSaveButton.setVisibility(android.view.View.VISIBLE);
            accountSaveButton.setOnClickListener(v -> saveLocalProfile());
        }
        SettingsRepository.getSettingsAsync(this, settings -> {
            if (displayNameInput != null) {
                displayNameInput.setText(settings.getDisplayName());
            }
            if (displayNameHeader != null) {
                displayNameHeader.setText(settings.getDisplayName());
            }
        }, error -> Toast.makeText(this, R.string.profile_load_error, Toast.LENGTH_SHORT).show());
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
                    bindAccountState();
                    Toast.makeText(this, R.string.delete_local_data_done, Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, R.string.delete_local_data_error, Toast.LENGTH_SHORT).show());
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String signedInIdentity(AuthStateRepository.AuthState authState) {
        if (authState.getEmail() != null && !authState.getEmail().trim().isEmpty()) {
            return authState.getEmail().trim();
        }
        return authState.getBestDisplayName(getString(R.string.profile_local_user));
    }
}
