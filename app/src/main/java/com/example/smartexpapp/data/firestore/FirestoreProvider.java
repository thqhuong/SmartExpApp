package com.example.smartexpapp.data.firestore;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public final class FirestoreProvider {
    private static final String PLACEHOLDER_PROJECT_ID = "placeholder-project-id";
    private static volatile boolean settingsApplied;

    private FirestoreProvider() {
    }

    public static boolean isConfigured(Context context) {
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            FirebaseOptions options = app.getOptions();
            String projectId = options == null ? null : options.getProjectId();
            return projectId != null && !projectId.trim().isEmpty() && !PLACEHOLDER_PROJECT_ID.equals(projectId);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public static FirebaseFirestore getInstance(Context context) {
        if (!isConfigured(context)) {
            return null;
        }

        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            if (!settingsApplied) {
                synchronized (FirestoreProvider.class) {
                    if (!settingsApplied) {
                        try {
                            firestore.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                                    .setPersistenceEnabled(true)
                                    .build());
                        } catch (IllegalStateException ignored) {
                            // Firestore may already be initialized by an earlier auth/settings callback.
                        }
                        settingsApplied = true;
                    }
                }
            }
            return firestore;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
