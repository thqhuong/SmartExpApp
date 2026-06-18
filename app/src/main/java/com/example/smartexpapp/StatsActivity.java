package com.example.smartexpapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;

import java.util.List;
import java.util.Map;

public class StatsActivity extends BaseActivity {

    private long currentSinceMillis = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        setupChrome(R.id.nav_stats);
        setTopTitle("Dashboard & Stats");

        RadioGroup dateRangeToggle = findViewById(R.id.dateRangeToggle);
        dateRangeToggle.setOnCheckedChangeListener((group, checkedId) -> {
            long now = System.currentTimeMillis();
            if (checkedId == R.id.btnRangeWeek) {
                currentSinceMillis = now - (7L * 24L * 60L * 60L * 1000L);
            } else if (checkedId == R.id.btnRangeMonth) {
                currentSinceMillis = now - (30L * 24L * 60L * 60L * 1000L);
            } else {
                currentSinceMillis = 0L;
            }
            bindDashboard();
        });

        bindDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadDashboard();
    }

    private void bindDashboard() {
        ProductRepository.getStatsSnapshotAsync(this, currentSinceMillis, snapshot -> {
            ((TextView) findViewById(R.id.urgentCount)).setText(String.valueOf(snapshot.getUrgentCount()));

            ((TextView) findViewById(R.id.consumedCount)).setText(String.valueOf(snapshot.getConsumedActionCount()));
            ((TextView) findViewById(R.id.wastedCount)).setText(String.valueOf(snapshot.getWastedActionCount()));
            ((TextView) findViewById(R.id.donatedCount)).setText(String.valueOf(snapshot.getDonatedActionCount()));
            ((TextView) findViewById(R.id.expiredActionCount))
                    .setText(String.valueOf(snapshot.getExpiredActionCount()));
            ((TextView) findViewById(R.id.activeCount)).setText(String.valueOf(snapshot.getActiveCount()));
            ((TextView) findViewById(R.id.preventedWasteCount))
                    .setText(String.valueOf(snapshot.getPreventedWasteCount()));

            int prevented = snapshot.getPreventedWasteCount();
            int totalOutcomes = prevented + snapshot.getWastedActionCount();
            int percentage = totalOutcomes == 0 ? 0 : Math.round(prevented / (float) totalOutcomes * 100f);

            ProgressBar trendProgress = findViewById(R.id.trendProgress);
            trendProgress.setProgress(percentage);
            ((TextView) findViewById(R.id.trendLabel)).setText(percentage + "% Prevented");

            findViewById(R.id.viewAllInventory)
                    .setOnClickListener(v -> startActivity(new Intent(this, InventoryActivity.class)));

            resetGroup(R.id.expiredList, R.id.sectionExpiredTitle, R.id.sectionExpiredCard);
            resetGroup(R.id.urgentList, R.id.sectionUrgentTitle, R.id.sectionUrgentCard);
            resetGroup(R.id.soonList, R.id.sectionSoonTitle, R.id.sectionSoonCard);
            resetGroup(R.id.safeList, R.id.sectionSafeTitle, R.id.sectionSafeCard);
            bindStorageSummaries(snapshot.getActiveProducts());
            bindGroupedProducts(snapshot.getActiveProducts());
        }, error -> {
            ((TextView) findViewById(R.id.urgentCount)).setText("0");
            ((TextView) findViewById(R.id.activeCount)).setText("0");
            ((TextView) findViewById(R.id.preventedWasteCount)).setText("0");
        });
    }

    private void resetGroup(int listId, int titleId, int cardId) {
        View title = findViewById(titleId);
        View card = findViewById(cardId);
        LinearLayout list = findViewById(listId);
        if (title != null) {
            title.setVisibility(View.GONE);
        }
        if (card != null) {
            card.setVisibility(View.GONE);
            card.setBackgroundResource(R.drawable.bg_glass_card);
        }
        if (list != null) {
            list.removeAllViews();
        }
    }

    private void bindStorageSummaries(List<DashboardState.StorageSummaryEntry> summaries) {
        LinearLayout list = findViewById(R.id.storageSummaryList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        addStorageSummary(inflater, list, products, "Refrigerator", "Refrigerator",
                R.drawable.ic_storage_fridge, R.drawable.bg_storage_icon_fridge, R.color.storage_fridge_icon,
                R.drawable.progress_blue);
        addStorageSummary(inflater, list, products, "Room Temp", "Room Temp",
                R.drawable.ic_storage_room, R.drawable.bg_storage_icon_room, R.color.storage_room_icon,
                R.drawable.progress_orange);
        addStorageSummary(inflater, list, products, "Freezer", "Freeze",
                R.drawable.ic_storage_freeze, R.drawable.bg_storage_icon_freezer, R.color.storage_freezer_icon,
                R.drawable.progress_purple);
    }

    private void addStorageSummary(LayoutInflater inflater, LinearLayout list, List<Product> products,
            String label, String storageValue, int iconRes, int bgRes, int iconColorRes, int progressDrawableRes) {
        int count = 0;
        for (Product product : products) {
            if (storageValue.equals(product.getStorage())) {
                count++;
            }
        }
        int progress = products.isEmpty() ? 0 : Math.round(count / (float) products.size() * 100f);

        View item = inflater.inflate(R.layout.item_storage_summary, list, false);

        View container = item.findViewById(R.id.storageIconContainer);
        if (container != null) {
            container.setBackgroundResource(bgRes);
        }

        ImageView icon = item.findViewById(R.id.storageIcon);
        ViewUtils.setIcon(icon, iconRes, iconColorRes);

        ((TextView) item.findViewById(R.id.storageName)).setText(label);
        ((TextView) item.findViewById(R.id.storageCount))
                .setText(count + " items");

        ProgressBar progressBar = item.findViewById(R.id.storageProgress);
        progressBar.setProgress(summary.getProgressPercent());
        progressBar.setProgressDrawable(
                androidx.appcompat.content.res.AppCompatResources.getDrawable(this, progressDrawableRes));

        list.addView(item);
    }

    private void bindGroupedProducts(Map<String, List<Product>> groups) {
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
        if (products == null || products.isEmpty())
            return;

        findViewById(titleId).setVisibility(View.VISIBLE);
        View cardView = findViewById(cardId);
        cardView.setVisibility(View.VISIBLE);
        if ("Expired".equals(groupName)) {
            cardView.setBackgroundResource(R.drawable.bg_glass_card_expired);
        } else if ("Urgent".equals(groupName)) {
            cardView.setBackgroundResource(R.drawable.bg_glass_card_urgent);
        } else {
            cardView.setBackgroundResource(R.drawable.bg_glass_card);
        }

        LinearLayout list = findViewById(listId);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            View item = inflater.inflate(R.layout.item_dashboard_expiring, list, false);

            ImageView icon = item.findViewById(R.id.productIcon);
            String imgUrl = product.getImageUrl();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                icon.setImageTintList(null);
                ImageLoader.load(icon, imgUrl);
            } else {
                int tintColor = CategoryColorHelper.getColor(this, product.getCategory());
                icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        getColor(tintColor)));
                ViewUtils.setIcon(icon, product.getIconRes(), tintColor);
            }

            View categoryDot = item.findViewById(R.id.categoryDot);
            categoryDot.getBackground().setTint(
                    getColor(CategoryColorHelper.getColor(this, product.getCategory())));
            categoryDot.setVisibility(View.VISIBLE);

            ((TextView) item.findViewById(R.id.productName)).setText(product.getName());
            ((TextView) item.findViewById(R.id.productMeta)).setText(product.getStorage());

            TextView badge = item.findViewById(R.id.expiryBadge);
            badge.setText(product.getDashboardBadge(this));
            if ("Expired".equals(groupName)) {
                badge.setBackgroundResource(R.drawable.bg_error_soft_badge);
                badge.setTextColor(getColor(R.color.smart_error));
            } else if ("Urgent".equals(groupName)) {
                badge.setBackgroundResource(R.drawable.bg_primary_soft_badge);
                badge.setTextColor(getColor(R.color.smart_primary_container));
            } else {
                badge.setBackgroundResource(R.drawable.bg_neutral_badge);
                badge.setTextColor(getColor(R.color.smart_on_surface));
            }

            ((TextView) item.findViewById(R.id.expiryStatus)).setText(product.getAmount());

            list.addView(item);
            if (i < products.size() - 1) {
                item.findViewById(R.id.rowDivider).setVisibility(View.VISIBLE);
            } else {
                item.findViewById(R.id.rowDivider).setVisibility(View.GONE);
            }
        }
    }

    private String getLocalizedStorage(String storage) {
        if ("Refrigerator".equalsIgnoreCase(storage)) {
            return getString(R.string.storage_summary_refrigerator);
        } else if ("Freeze".equalsIgnoreCase(storage) || "Freezer".equalsIgnoreCase(storage)) {
            return getString(R.string.storage_summary_freezer);
        } else if ("Room Temp".equalsIgnoreCase(storage)) {
            return getString(R.string.storage_summary_room_temp);
        }
        return storage;
    }
}
