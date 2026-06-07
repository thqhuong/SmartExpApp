package com.example.smartexpapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;

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
        bindGroupedProducts(products);
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

    private void bindGroupedProducts(List<Product> products) {
        Map<String, List<Product>> groups = new HashMap<>();
        groups.put("Expired", new ArrayList<>());
        groups.put("Urgent", new ArrayList<>());
        groups.put("Soon", new ArrayList<>());
        groups.put("Safe", new ArrayList<>());

        for (Product product : products) {
            groups.get(product.getGroup()).add(product);
        }

        bindGroup("Expired", R.id.expiredList, R.id.sectionExpiredTitle, R.id.sectionExpiredCard,
                groups.get("Expired"), R.color.smart_error);
        bindGroup("Urgent", R.id.urgentList, R.id.sectionUrgentTitle, R.id.sectionUrgentCard,
                groups.get("Urgent"), R.color.smart_primary_container);
        bindGroup("Soon", R.id.soonList, R.id.sectionSoonTitle, R.id.sectionSoonCard,
                groups.get("Soon"), R.color.smart_on_surface);
        bindGroup("Safe", R.id.safeList, R.id.sectionSafeTitle, R.id.sectionSafeCard,
                groups.get("Safe"), R.color.smart_on_surface);
    }

    private void bindGroup(String groupName, int listId, int titleId, int cardId,
                           List<Product> products, int badgeColorRes) {
        if (products.isEmpty()) return;

        findViewById(titleId).setVisibility(View.VISIBLE);
        findViewById(cardId).setVisibility(View.VISIBLE);

        LinearLayout list = findViewById(listId);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Product product : products) {
            View item = inflater.inflate(R.layout.item_dashboard_expiring, list, false);

            ImageView icon = item.findViewById(R.id.productIcon);
            String imgUrl = product.getImageUrl();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                icon.setImageTintList(null);
                ImageLoader.load(icon, imgUrl);
            } else {
                icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        getColor(R.color.smart_secondary)));
                ViewUtils.setIcon(icon, product.getIconRes(), R.color.smart_secondary);
            }

            View categoryDot = item.findViewById(R.id.categoryDot);
            categoryDot.getBackground().setTint(getColor(CategoryColorHelper.getColor(product.getCategory())));
            categoryDot.setVisibility(View.VISIBLE);

            ((TextView) item.findViewById(R.id.productName)).setText(product.getName());
            ((TextView) item.findViewById(R.id.productMeta)).setText(
                    product.getStorage() + " \u2022 " + product.getAmount());

            TextView badge = item.findViewById(R.id.expiryBadge);
            badge.setText(product.getDashboardBadge());
            if ("Expired".equals(groupName) || "Urgent".equals(groupName)) {
                badge.setBackgroundResource(R.drawable.bg_error_badge);
                badge.setTextColor(getColor(badgeColorRes));
            }

            ((TextView) item.findViewById(R.id.expiryStatus)).setText(product.getExpiryStatus());

            list.addView(item);
            if (products.indexOf(product) < products.size() - 1) {
                item.findViewById(R.id.rowDivider).setVisibility(View.VISIBLE);
            }
        }
    }
}
