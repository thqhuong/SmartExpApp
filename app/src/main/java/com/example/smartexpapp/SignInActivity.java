package com.example.smartexpapp;

import android.content.Intent;
import android.os.Bundle;

public class SignInActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        findViewById(R.id.signInButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, InventoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });
    }
}
