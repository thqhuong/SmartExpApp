package com.example.smartexpapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.smartexpapp.util.NotificationHelper;

public class ExpiryBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String productName = intent.getStringExtra("product_name");
        String category = intent.getStringExtra("product_category");
        NotificationHelper.showNotification(context, productName, category);
    }
}
