package com.example.smartexpapp;

import android.os.Bundle;

public class HelpSupportActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);
        setupChrome(R.id.nav_settings);
        setTopTitle("Support");
        useBackButton();
    }
}
