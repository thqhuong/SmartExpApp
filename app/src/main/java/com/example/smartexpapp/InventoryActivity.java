package com.example.smartexpapp;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InventoryActivity extends BaseActivity {
    private LinearLayout productList;
    private LinearLayout emptyState;
    private TextView emptyTitle;
    private TextView emptyDesc;
    private TextView errorState;
    private ProgressBar loadingIndicator;
    private EditText searchInput;
    private ChipGroup expiryFilterGroup;
    private ChipGroup categoryFilterGroup;
    private Chip sortDate;
    private Chip sortName;
    private Chip sortNewest;

    private String currentSearch = "";
    private String currentFilter = "All";
    private String currentCategory = "All";
    private String currentSort = "date";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private EditProductDialog currentEditDialog;

    private final ActivityResultLauncher<String> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoPicked);
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        setupChrome(R.id.nav_inventory);

        productList = findViewById(R.id.productList);
        emptyState = findViewById(R.id.emptyState);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptyDesc = findViewById(R.id.emptyDesc);
        errorState = findViewById(R.id.errorState);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        searchInput = findViewById(R.id.searchInput);
        expiryFilterGroup = findViewById(R.id.expiryFilterGroup);
        categoryFilterGroup = findViewById(R.id.categoryFilterGroup);
        sortDate = findViewById(R.id.sortDate);
        sortName = findViewById(R.id.sortName);
        sortNewest = findViewById(R.id.sortNewest);

        setupSearch();
        setupFilters();
        setupCategoryFilter();
        setupSort();

        showLoading();
        renderProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderProducts();
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    currentSearch = s.toString();
                    renderProducts();
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilters() {
        expiryFilterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.filterAll : checkedIds.get(0);
            if (checkedId == R.id.filterAll) {
                currentFilter = "All";
            } else if (checkedId == R.id.filterExpiring) {
                currentFilter = "Expiring";
            } else if (checkedId == R.id.filterExpired) {
                currentFilter = "Expired";
            } else if (checkedId == R.id.filterRoom) {
                currentFilter = "Room Temp";
            } else if (checkedId == R.id.filterCool) {
                currentFilter = "Cool";
            } else if (checkedId == R.id.filterFrozen) {
                currentFilter = "Frozen";
            }
            renderProducts();
        });
    }

    private void setupCategoryFilter() {
        categoryFilterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.filterCatAll : checkedIds.get(0);
            if (checkedId == R.id.filterCatAll) {
                currentCategory = "All";
            } else if (checkedId == R.id.filterCatDairy) {
                currentCategory = "Dairy";
            } else if (checkedId == R.id.filterCatProduce) {
                currentCategory = "Produce";
            } else if (checkedId == R.id.filterCatPantry) {
                currentCategory = "Pantry";
            } else if (checkedId == R.id.filterCatVegetables) {
                currentCategory = "Vegetables";
            } else if (checkedId == R.id.filterCatGeneral) {
                currentCategory = "General";
            }
            renderProducts();
        });
    }

    private void setupSort() {
        View.OnClickListener sortListener = v -> {
            if (v == sortDate) {
                currentSort = "date";
                sortDate.setChecked(true);
                sortName.setChecked(false);
                sortNewest.setChecked(false);
            } else if (v == sortName) {
                currentSort = "name";
                sortDate.setChecked(false);
                sortName.setChecked(true);
                sortNewest.setChecked(false);
            } else if (v == sortNewest) {
                currentSort = "newest";
                sortDate.setChecked(false);
                sortName.setChecked(false);
                sortNewest.setChecked(true);
            }
            renderProducts();
        };
        sortDate.setOnClickListener(sortListener);
        sortName.setOnClickListener(sortListener);
        sortNewest.setOnClickListener(sortListener);
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        productList.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        productList.setVisibility(View.VISIBLE);
    }

    private void renderProducts() {
        try {
            List<Product> products = ProductRepository.search(this, currentSearch);
            products = filterProducts(products);
            products = sortProducts(products);
            bindProducts(products);
            hideLoading();
        } catch (Exception e) {
            hideLoading();
            productList.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            errorState.setVisibility(View.VISIBLE);
        }
    }

    private List<Product> filterProducts(List<Product> products) {
        List<Product> filtered = new ArrayList<>();
        for (Product product : products) {
            if (matchesFilter(product) && matchesCategory(product)) {
                filtered.add(product);
            }
        }
        return filtered;
    }

    private boolean matchesCategory(Product product) {
        return "All".equals(currentCategory) || currentCategory.equals(product.getCategory());
    }

    private boolean matchesFilter(Product product) {
        if ("All".equals(currentFilter)) {
            return true;
        }
        switch (currentFilter) {
            case "Expiring":
                return product.isExpiringSoon();
            case "Expired":
                return product.isExpired();
            case "Room Temp":
                return "Room Temp".equals(product.getStorage());
            case "Cool":
                return "Refrigerator".equals(product.getStorage());
            case "Frozen":
                return "Freeze".equals(product.getStorage());
            default:
                return true;
        }
    }

    private List<Product> sortProducts(List<Product> products) {
        List<Product> sorted = new ArrayList<>(products);
        switch (currentSort) {
            case "name":
                Collections.sort(sorted, Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            case "newest":
                Collections.sort(sorted, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            default:
                Collections.sort(sorted, Comparator.comparingInt(Product::getDaysUntilExpiry));
                break;
        }
        return sorted;
    }

    private void bindProducts(List<Product> products) {
        productList.removeAllViews();

        if (products.isEmpty()) {
            productList.setVisibility(View.GONE);
            errorState.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);

            if (!currentSearch.isEmpty()) {
                emptyTitle.setText(R.string.empty_search);
                emptyDesc.setText(R.string.empty_search_desc);
            } else {
                emptyTitle.setText(R.string.empty_inventory);
                emptyDesc.setText(R.string.empty_inventory_desc);
            }
            return;
        }

        productList.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Product product : products) {
            View item = inflater.inflate(R.layout.item_inventory_product, productList, false);
            bindProductCard(item, product);
            ViewUtils.setBottomMargin(item, 12);
            productList.addView(item);
        }
    }

    private void bindProductCard(View item, Product product) {
        MaterialCardView card = item.findViewById(R.id.productCard);
        TextView urgentBadge = item.findViewById(R.id.urgentBadge);
        TextView expiryStatus = item.findViewById(R.id.expiryStatus);
        ProgressBar progress = item.findViewById(R.id.expiryProgress);
        View deleteBtn = item.findViewById(R.id.btnDelete);

        ViewUtils.setIcon(item.findViewById(R.id.productIcon), product.getIconRes(),
                product.isExpiringSoon() || product.isExpired() ? R.color.smart_primary_container : R.color.smart_secondary);
        ImageLoader.load(item.findViewById(R.id.productIcon), product.getImageUrl());
        ((TextView) item.findViewById(R.id.productName)).setText(product.getName());
        ((TextView) item.findViewById(R.id.productMeta)).setText(product.getCategory() + " \u2022 " + product.getAmount());
        expiryStatus.setText(product.getExpiryStatus());
        progress.setProgress(product.getExpiryProgress());

        if (product.isExpired()) {
            card.setStrokeColor(getColor(R.color.smart_error));
            urgentBadge.setVisibility(View.VISIBLE);
            urgentBadge.setBackgroundResource(R.drawable.bg_error_badge);
            urgentBadge.setText("EXPIRED");
            urgentBadge.setTextColor(getColor(R.color.smart_error));
            expiryStatus.setTextColor(getColor(R.color.smart_error));
            progress.setProgressDrawable(AppCompatResources.getDrawable(this, R.drawable.progress_orange));
        } else if (product.isExpiringSoon()) {
            card.setStrokeColor(getColor(R.color.smart_primary_container));
            urgentBadge.setVisibility(View.VISIBLE);
            urgentBadge.setBackgroundResource(R.drawable.bg_primary_badge);
            urgentBadge.setText("EXPIRING SOON");
            urgentBadge.setTextColor(getColor(R.color.smart_on_primary));
            expiryStatus.setTextColor(getColor(R.color.smart_primary_container));
            progress.setProgressDrawable(AppCompatResources.getDrawable(this, R.drawable.progress_orange));
        }

        card.setOnClickListener(v -> openEditDialog(product));

        deleteBtn.setOnClickListener(v -> confirmDelete(product));
    }

    private void openEditDialog(Product product) {
        currentEditDialog = new EditProductDialog(product,
                this::renderProducts,
                () -> pickPhotoLauncher.launch("image/*")
        );
        currentEditDialog.show(this);
    }

    private void confirmDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, product.getName()))
                .setPositiveButton(R.string.delete_confirm, (dialog, which) -> {
                    ProductRepository.deleteProduct(this, product.getId());
                    Toast.makeText(this, R.string.product_deleted, Toast.LENGTH_SHORT).show();
                    renderProducts();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onPhotoPicked(Uri uri) {
        if (uri == null || currentEditDialog == null) return;
        String path = saveImageToInternalStorage(uri);
        if (path != null) {
            currentEditDialog.setPhotoPath(path);
        }
    }
}
