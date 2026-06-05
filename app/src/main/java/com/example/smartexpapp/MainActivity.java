package com.example.smartexpapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupChrome(R.id.nav_stats);
        bindDashboard();
    }

    private void bindDashboard() {
        List<Product> products = ProductRepository.getProducts(this);
        int urgentCount = 0;
        for (Product product : products) {
            if (product.isExpiringSoon()) {
                urgentCount++;
            }
        }

        ((TextView) findViewById(R.id.urgentCount)).setText(String.valueOf(urgentCount));
        ((TextView) findViewById(R.id.totalTracked)).setText(String.valueOf(products.size()));
        findViewById(R.id.viewAllInventory).setOnClickListener(v -> startActivity(new Intent(this, InventoryActivity.class)));

        bindStorageSummaries(products);
        bindExpiringSoon(products);
    }

    private void bindStorageSummaries(List<Product> products) {
        LinearLayout list = findViewById(R.id.storageSummaryList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        addStorageSummary(inflater, list, products, "Refrigerator", "Refrigerator", android.R.drawable.ic_menu_upload, true);
        addStorageSummary(inflater, list, products, "Room Temp", "Room Temp", android.R.drawable.ic_menu_agenda, false);
        addStorageSummary(inflater, list, products, "Freezer", "Freeze", android.R.drawable.ic_menu_compass, false);
    }

    private void addStorageSummary(LayoutInflater inflater, LinearLayout list, List<Product> products, String label, String storageValue, int iconRes, boolean primaryProgress) {
        int count = 0;
        for (Product product : products) {
            if (storageValue.equals(product.getStorage())) {
                count++;
            }
        }
        int progress = products.isEmpty() ? 0 : Math.round(count / (float) products.size() * 100f);

        View item = inflater.inflate(R.layout.item_storage_summary, list, false);
        ViewUtils.setIcon(item.findViewById(R.id.storageIcon), iconRes, R.color.smart_secondary);
        ((TextView) item.findViewById(R.id.storageName)).setText(label);
        ((TextView) item.findViewById(R.id.storageCount)).setText(count + " items");
        ProgressBar progressBar = item.findViewById(R.id.storageProgress);
        progressBar.setProgress(progress);
        if (primaryProgress) {
            progressBar.setProgressDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.progress_orange));
        }
        list.addView(item);
    }

    private void bindExpiringSoon(List<Product> products) {
        LinearLayout list = findViewById(R.id.dashboardExpiringList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        List<Product> sorted = new ArrayList<>(products);
        sorted.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
        int visibleCount = Math.min(3, sorted.size());
        for (int i = 0; i < visibleCount; i++) {
            Product product = sorted.get(i);
            View item = inflater.inflate(R.layout.item_dashboard_expiring, list, false);
            ViewUtils.setIcon(item.findViewById(R.id.productIcon), product.getIconRes(), product.isExpiringSoon() ? R.color.smart_primary_container : R.color.smart_secondary);
            ImageLoader.load(item.findViewById(R.id.productIcon), product.getImageUrl());
            ((TextView) item.findViewById(R.id.productName)).setText(product.getName());
            ((TextView) item.findViewById(R.id.productMeta)).setText(product.getStorage() + " • " + product.getAmount());

            TextView badge = item.findViewById(R.id.expiryBadge);
            badge.setText(product.getDashboardBadge());
            if (product.isExpiringSoon()) {
                badge.setBackgroundResource(R.drawable.bg_error_badge);
                badge.setTextColor(getColor(R.color.smart_primary_container));
            }
            ((TextView) item.findViewById(R.id.expiryStatus)).setText(product.getExpiryStatus());
            item.findViewById(R.id.rowDivider).setVisibility(i == visibleCount - 1 ? View.GONE : View.VISIBLE);
            list.addView(item);
        }
    }
}
