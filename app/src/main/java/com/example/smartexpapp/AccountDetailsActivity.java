package com.example.smartexpapp;

import android.os.Bundle;
import android.widget.Toast;

public class AccountDetailsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);
        setupChrome(R.id.nav_settings);
        setTopTitle("Account");
        useBackButton();
        findViewById(R.id.accountSaveButton).setOnClickListener(
                v -> Toast.makeText(this, "Profile changes saved locally.", Toast.LENGTH_SHORT).show()
        );
    }
}
