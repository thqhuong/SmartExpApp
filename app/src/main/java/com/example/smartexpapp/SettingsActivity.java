package com.example.smartexpapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.SampleData;
import com.example.smartexpapp.model.SettingItem;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;

import java.util.List;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupChrome(R.id.nav_settings);
        ImageLoader.load(findViewById(R.id.profileImage), "https://images.unsplash.com/photo-1599566150163-29194dcaad36?auto=format&fit=crop&q=80&w=200");
        bindSettings();
        findViewById(R.id.signOutButton).setOnClickListener(v -> Toast.makeText(this, "Sign out placeholder", Toast.LENGTH_SHORT).show());
    }

    private void bindSettings() {
        LinearLayout list = findViewById(R.id.settingsList);
        LayoutInflater inflater = LayoutInflater.from(this);
        List<SettingItem> settings = SampleData.settings();

        for (int i = 0; i < settings.size(); i++) {
            SettingItem item = settings.get(i);
            View row = inflater.inflate(R.layout.item_setting_row, list, false);
            ViewUtils.setIcon(row.findViewById(R.id.settingIcon), item.getIconRes(), R.color.smart_secondary);
            ((TextView) row.findViewById(R.id.settingTitle)).setText(item.getTitle());
            ((TextView) row.findViewById(R.id.settingSubtitle)).setText(item.getSubtitle());

            row.findViewById(R.id.settingSwitch).setVisibility(item.isSwitchRow() ? View.VISIBLE : View.GONE);
            ((ImageView) row.findViewById(R.id.settingChevron)).setVisibility(item.isSwitchRow() ? View.GONE : View.VISIBLE);
            row.findViewById(R.id.settingDivider).setVisibility(i == settings.size() - 1 ? View.GONE : View.VISIBLE);
            list.addView(row);
        }
    }
}
