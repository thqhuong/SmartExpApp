package com.example.smartexpapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class HelpSupportActivity extends BaseActivity {
    private String diagnostics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);
        setupChrome(R.id.nav_settings);
        setTopTitle(getString(R.string.support_title));
        useBackButton();

        diagnostics = buildDiagnostics();
        TextView diagnosticsText = findViewById(R.id.supportDiagnosticsText);
        if (diagnosticsText != null) {
            diagnosticsText.setText(diagnostics);
        }

        findViewById(R.id.emailSupportButton).setOnClickListener(v ->
                openEmail(getString(R.string.support_email_subject), getString(R.string.support_email_body, diagnostics))
        );
        findViewById(R.id.reportBugButton).setOnClickListener(v ->
                openEmail(getString(R.string.support_bug_subject), getString(R.string.support_bug_body, diagnostics))
        );
    }

    private void openEmail(String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.support_email_chooser)));
        } catch (ActivityNotFoundException error) {
            showErrorNotification(getString(R.string.support_no_email_app));
        }
    }

    private String buildDiagnostics() {
        return getString(
                R.string.support_diagnostics_format,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT
        );
    }
}
