package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.LocalImageRepository;
import com.example.smartexpapp.data.firestore.FirestoreProvider;
import com.example.smartexpapp.data.firestore.ProductSyncRepository;
import com.example.smartexpapp.data.firestore.SyncStatusRepository;
import com.example.smartexpapp.data.firestore.UserDataSyncRepository;
import com.example.smartexpapp.data.LocalDataExportRepository;
import com.example.smartexpapp.data.LocalDataResetRepository;
import com.example.smartexpapp.data.SettingsRepository;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.util.ImageLoader;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.io.File;

public class AccountDetailsActivity extends BaseActivity {
    private EditText displayNameInput;
    private ImageView accountAvatarImage;
    private TextView displayNameHeader;
    private TextView accountStatusText;
    private TextView accountSyncStateText;
    private MaterialButton accountAuthButton;
    private MaterialButton accountSaveButton;
    private MaterialButton retrySyncButton;
    private MaterialButton chooseAvatarButton;
    private MaterialButton removeAvatarButton;
    private String profileAvatarPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);
        setupChrome(R.id.nav_settings);
        setTopTitle(getString(R.string.account_details_title));
        useBackButton();
        displayNameInput = findViewById(R.id.displayNameInput);
        accountAvatarImage = findViewById(R.id.accountAvatarImage);
        displayNameHeader = findViewById(R.id.accountDisplayNameText);
        accountStatusText = findViewById(R.id.accountStatusText);
        accountSyncStateText = findViewById(R.id.accountSyncStateText);
        accountAuthButton = findViewById(R.id.accountAuthButton);
        accountSaveButton = findViewById(R.id.accountSaveButton);
        retrySyncButton = findViewById(R.id.retrySyncButton);
        chooseAvatarButton = findViewById(R.id.chooseAvatarButton);
        removeAvatarButton = findViewById(R.id.removeAvatarButton);
        bindAccountState();
        if (chooseAvatarButton != null) {
            chooseAvatarButton.setOnClickListener(v -> pickImage(this::onAvatarPicked));
        }
        if (removeAvatarButton != null) {
            removeAvatarButton.setOnClickListener(v -> confirmRemoveAvatar());
        }
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
        bindAvatar();
        bindSyncState(authState);
    }

    private void bindSignedInAccount(AuthStateRepository.AuthState authState) {
        String identity = authState.getBestDisplayName(getString(R.string.profile_local_user));
        if (displayNameInput != null) {
            displayNameInput.setEnabled(true);
            displayNameInput.setText(identity);
        }
        if (displayNameHeader != null) {
            displayNameHeader.setText(identity);
        }
        if (accountStatusText != null) {
            accountStatusText.setText(R.string.account_signed_in_device);
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
            accountSaveButton.setVisibility(android.view.View.VISIBLE);
            accountSaveButton.setOnClickListener(v -> saveProfile());
        }
        SettingsRepository.getSettingsAsync(this, settings -> {
            profileAvatarPath = settings.getProfileAvatarPath();
            bindAvatar();
        }, error -> Toast.makeText(this, R.string.profile_load_error, Toast.LENGTH_SHORT).show());
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
        if (accountAuthButton != null) {
            accountAuthButton.setVisibility(android.view.View.VISIBLE);
            accountAuthButton.setText(R.string.sign_in_label);
            accountAuthButton.setIconResource(R.drawable.ic_info);
            accountAuthButton.setOnClickListener(v -> navigateToSignIn());
        }
        if (accountSaveButton != null) {
            accountSaveButton.setVisibility(android.view.View.VISIBLE);
            accountSaveButton.setOnClickListener(v -> saveProfile());
        }
        SettingsRepository.getSettingsAsync(this, settings -> {
            if (displayNameInput != null) {
                displayNameInput.setText(settings.getDisplayName());
            }
            if (displayNameHeader != null) {
                displayNameHeader.setText(settings.getDisplayName());
            }
            profileAvatarPath = settings.getProfileAvatarPath();
            bindAvatar();
        }, error -> Toast.makeText(this, R.string.profile_load_error, Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        String displayName = displayNameInput == null ? "" : displayNameInput.getText().toString().trim();
        if (displayName.isEmpty()) {
            Toast.makeText(this, R.string.display_name_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }

        AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(this);
        if (authState.isSignedIn()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build();
                user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveLocalAndRefresh(displayName);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                        Toast.makeText(this, getString(R.string.profile_save_error) + ": " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                saveLocalAndRefresh(displayName);
            }
        } else {
            saveLocalAndRefresh(displayName);
        }
    }

    private void saveLocalAndRefresh(String displayName) {
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

    private void bindAvatar() {
        if (accountAvatarImage == null) {
            return;
        }
        if (!hasUsableAvatar(profileAvatarPath)) {
            accountAvatarImage.setImageResource(R.drawable.account_avatar);
            if (removeAvatarButton != null) {
                removeAvatarButton.setVisibility(View.GONE);
            }
            if (chooseAvatarButton != null) {
                chooseAvatarButton.setText(R.string.choose_avatar);
            }
            return;
        }
        ImageLoader.load(accountAvatarImage, profileAvatarPath);
        if (removeAvatarButton != null) {
            removeAvatarButton.setVisibility(View.VISIBLE);
        }
        if (chooseAvatarButton != null) {
            chooseAvatarButton.setText(R.string.change_avatar);
        }
    }

    private boolean hasUsableAvatar(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        String filePath = path.startsWith("file://") ? path.substring(7) : path;
        return new File(filePath).isFile();
    }

    private void onAvatarPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        String previousPath = profileAvatarPath;
        String path = saveImageToInternalStorage(uri);
        if (path == null) {
            Toast.makeText(this, R.string.avatar_save_error, Toast.LENGTH_SHORT).show();
            return;
        }
        SettingsRepository.setProfileAvatarPathAsync(this, path, settings -> {
            profileAvatarPath = settings.getProfileAvatarPath();
            if (previousPath != null && !previousPath.equals(profileAvatarPath)) {
                LocalImageRepository.deleteProductImage(getApplicationContext(), previousPath);
            }
            bindAvatar();
            Toast.makeText(this, R.string.avatar_saved, Toast.LENGTH_SHORT).show();
        }, error -> {
            LocalImageRepository.deleteProductImage(getApplicationContext(), path);
            Toast.makeText(this, R.string.avatar_save_error, Toast.LENGTH_SHORT).show();
        });
    }

    private void confirmRemoveAvatar() {
        if (profileAvatarPath == null || profileAvatarPath.trim().isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_avatar_title)
                .setMessage(R.string.remove_avatar_message)
                .setPositiveButton(R.string.remove_label, (dialog, which) -> removeAvatar())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void removeAvatar() {
        String previousPath = profileAvatarPath;
        SettingsRepository.setProfileAvatarPathAsync(this, null, settings -> {
            profileAvatarPath = settings.getProfileAvatarPath();
            LocalImageRepository.deleteProductImage(getApplicationContext(), previousPath);
            bindAvatar();
            Toast.makeText(this, R.string.avatar_removed, Toast.LENGTH_SHORT).show();
        }, error -> Toast.makeText(this, R.string.avatar_remove_error, Toast.LENGTH_SHORT).show());
    }

    private void bindSyncState(AuthStateRepository.AuthState authState) {
        if (accountSyncStateText == null || retrySyncButton == null) {
            return;
        }
        if (!authState.isSignedIn()) {
            accountSyncStateText.setText(authState.isGuest()
                    ? R.string.sync_status_guest_local
                    : R.string.sync_status_local_only);
            retrySyncButton.setVisibility(android.view.View.GONE);
            SyncStatusRepository.markLocalOnly(this);
            return;
        }
        if (!FirestoreProvider.isConfigured(this)) {
            accountSyncStateText.setText(R.string.sync_status_not_configured);
            retrySyncButton.setVisibility(android.view.View.GONE);
            return;
        }
        accountSyncStateText.setText(syncStatusLabel(SyncStatusRepository.getStatus(this)));
        retrySyncButton.setVisibility(android.view.View.VISIBLE);
        retrySyncButton.setText(R.string.retry_sync_label);
        retrySyncButton.setIconResource(R.drawable.ic_check_circle);
        retrySyncButton.setOnClickListener(v -> retryCloudSync());
    }

    private int syncStatusLabel(String status) {
        if (SyncStatusRepository.STATUS_SYNCING.equals(status)) {
            return R.string.sync_status_syncing;
        }
        if (SyncStatusRepository.STATUS_SYNCED.equals(status)) {
            return R.string.sync_status_synced;
        }
        if (SyncStatusRepository.STATUS_ERROR.equals(status)) {
            return R.string.sync_status_error;
        }
        if (SyncStatusRepository.STATUS_SYNC_ENABLED.equals(status)) {
            return R.string.sync_status_enabled;
        }
        return R.string.sync_status_local_only;
    }

    private void retryCloudSync() {
        AppDatabase database = AppDatabase.getInstance(this);
        SyncStatusRepository.markSyncing(this);
        if (accountSyncStateText != null) {
            accountSyncStateText.setText(R.string.sync_status_syncing);
        }
        ProductSyncRepository.initialSyncAsync(this, database,
                ignored -> {
                    UserDataSyncRepository.syncUserDataAsync(this, database);
                    bindAccountState();
                },
                error -> {
                    UserDataSyncRepository.syncUserDataAsync(this, database);
                    bindAccountState();
                });
        Toast.makeText(this, R.string.retry_sync_started, Toast.LENGTH_SHORT).show();
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
