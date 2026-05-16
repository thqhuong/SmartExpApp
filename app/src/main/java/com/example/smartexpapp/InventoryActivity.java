package com.example.smartexpapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventoryActivity extends BaseActivity {
    private LinearLayout productList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        setupChrome(R.id.nav_inventory);

        productList = findViewById(R.id.productList);
        ChipGroup filters = findViewById(R.id.filterGroup);
        filters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.filterAll : checkedIds.get(0);
            renderProducts(filterFor(checkedId));
        });
        renderProducts("All");
    }

    private String filterFor(int checkedId) {
        if (checkedId == R.id.filterRoom) {
            return "Room Temp";
        }
        if (checkedId == R.id.filterCool) {
            return "Cool";
        }
        if (checkedId == R.id.filterFrozen) {
            return "Frozen";
        }
        return "All";
    }

    private void renderProducts(String filter) {
        productList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        List<Product> sorted = new ArrayList<>(ProductRepository.getProducts());
        sorted.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
        for (Product product : sorted) {
            if (!matches(product, filter)) {
                continue;
            }
            View item = inflater.inflate(R.layout.item_inventory_product, productList, false);
            bindProductCard(item, product);
            ViewUtils.setBottomMargin(item, 12);
            productList.addView(item);
        }
    }

    private boolean matches(Product product, String filter) {
        if ("All".equals(filter)) {
            return true;
        }
        if ("Cool".equals(filter)) {
            return "Refrigerator".equals(product.getStorage());
        }
        if ("Frozen".equals(filter)) {
            return "Freeze".equals(product.getStorage());
        }
        return "Room Temp".equals(product.getStorage());
    }

    private void bindProductCard(View item, Product product) {
        MaterialCardView card = item.findViewById(R.id.productCard);
        TextView urgentBadge = item.findViewById(R.id.urgentBadge);
        TextView expiryStatus = item.findViewById(R.id.expiryStatus);
        ProgressBar progress = item.findViewById(R.id.expiryProgress);

        ViewUtils.setIcon(item.findViewById(R.id.productIcon), product.getIconRes(), product.isExpiringSoon() ? R.color.smart_primary_container : R.color.smart_secondary);
        ImageLoader.load(item.findViewById(R.id.productIcon), product.getImageUrl());
        ((TextView) item.findViewById(R.id.productName)).setText(product.getName());
        ((TextView) item.findViewById(R.id.productMeta)).setText(product.getCategory() + " • " + product.getAmount());
        expiryStatus.setText(product.getExpiryStatus());
        progress.setProgress(product.getExpiryProgress());

        if (product.isExpiringSoon()) {
            card.setStrokeColor(getColor(R.color.smart_primary_container));
            urgentBadge.setVisibility(View.VISIBLE);
            expiryStatus.setTextColor(getColor(R.color.smart_primary_container));
            progress.setProgressDrawable(AppCompatResources.getDrawable(this, R.drawable.progress_orange));
        }
    }
}
