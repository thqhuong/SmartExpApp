package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventoryActivity extends BaseActivity {
    private static final String TAG = "InventoryActivity";

    private LinearLayout productList;
    private LinearLayout emptyState;
    private TextView emptyTitle;
    private TextView emptyDesc;
    private TextView errorState;
    private ProgressBar loadingIndicator;
    private EditText searchInput;
    private Spinner expirySpinner;
    private Spinner storageSpinner;
    private Spinner sortSpinner;

    private String currentSearch = "";
    private String currentFilter = "All";
    private String currentStorage = "All";
    private String currentSort = "oldest";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
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
        expirySpinner = findViewById(R.id.expirySpinner);
        storageSpinner = findViewById(R.id.storageSpinner);
        sortSpinner = findViewById(R.id.sortSpinner);

        setupSearch();
        setupSpinners();

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

    private void setupSpinners() {
        ArrayAdapter<CharSequence> expiryAdapter = ArrayAdapter.createFromResource(this,
                R.array.expiry_filter_options, android.R.layout.simple_spinner_item);
        expiryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        expirySpinner.setAdapter(expiryAdapter);
        expirySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if ("Still good".equals(selected)) {
                    currentFilter = "StillGood";
                } else if ("Expired".equals(selected)) {
                    currentFilter = "Expired";
                } else {
                    currentFilter = "All";
                }
                renderProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<CharSequence> storageAdapter = ArrayAdapter.createFromResource(this,
                R.array.storage_filter_options, android.R.layout.simple_spinner_item);
        storageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storageSpinner.setAdapter(storageAdapter);
        storageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStorage = parent.getItemAtPosition(position).toString();
                renderProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentStorage = "All";
                renderProducts();
            }
        });

        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if ("Name".equals(selected)) {
                    currentSort = "name";
                } else if ("Newest".equals(selected)) {
                    currentSort = "newest";
                } else {
                    currentSort = "oldest";
                }
                renderProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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
            Log.e(TAG, "Failed to render products", e);
            hideLoading();
            productList.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            errorState.setVisibility(View.VISIBLE);
        }
    }

    private List<Product> filterProducts(List<Product> products) {
        List<Product> filtered = new ArrayList<>();
        for (Product product : products) {
            if (matchesFilter(product) && matchesStorage(product)) {
                filtered.add(product);
            }
        }
        return filtered;
    }

    private boolean matchesStorage(Product product) {
        return "All".equals(currentStorage) || currentStorage.equals(product.getStorage());
    }

    private boolean matchesFilter(Product product) {
        if ("All".equals(currentFilter)) {
            return true;
        }
        if ("StillGood".equals(currentFilter)) {
            return !product.isExpired();
        }
        return product.isExpired();
    }

    private List<Product> sortProducts(List<Product> products) {
        List<Product> sorted = new ArrayList<>(products);
        switch (currentSort) {
            case "name":
                sorted.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            case "newest":
                sorted.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            default:
                sorted.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
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

        View categoryDot = item.findViewById(R.id.categoryDot);
        categoryDot.getBackground().setTint(getColor(CategoryColorHelper.getColor(product.getCategory())));
        categoryDot.setVisibility(View.VISIBLE);

        ImageView icon = item.findViewById(R.id.productIcon);
        String imgUrl = product.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            icon.setImageTintList(null);
            ImageLoader.load(icon, imgUrl);
        } else {
            int tintColor = product.isExpiringSoon() || product.isExpired()
                    ? R.color.smart_primary_container : R.color.smart_secondary;
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(tintColor)));
            ViewUtils.setIcon(icon, product.getIconRes(), tintColor);
        }
        ((TextView) item.findViewById(R.id.productName)).setText(product.getName());
        ((TextView) item.findViewById(R.id.productMeta)).setText(getString(R.string.product_meta_format, product.getCategory(), product.getAmount()));
        expiryStatus.setText(product.getExpiryStatus());
        progress.setProgress(product.getExpiryProgress());

        if (product.isExpired()) {
            card.setStrokeColor(getColor(R.color.smart_error));
            urgentBadge.setVisibility(View.VISIBLE);
            urgentBadge.setBackgroundResource(R.drawable.bg_error_soft_badge);
            urgentBadge.setText(R.string.status_expired);
            urgentBadge.setTextColor(getColor(R.color.smart_error));
            expiryStatus.setTextColor(getColor(R.color.smart_error));
            progress.setProgressDrawable(AppCompatResources.getDrawable(this, R.drawable.progress_orange));
        } else if (product.isExpiringSoon()) {
            card.setStrokeColor(getColor(R.color.smart_primary_container));
            urgentBadge.setVisibility(View.VISIBLE);
            urgentBadge.setBackgroundResource(R.drawable.bg_primary_soft_badge);
            urgentBadge.setText(R.string.status_expiring);
            urgentBadge.setTextColor(getColor(R.color.smart_primary_container));
            expiryStatus.setTextColor(getColor(R.color.smart_primary_container));
            progress.setProgressDrawable(AppCompatResources.getDrawable(this, R.drawable.progress_orange));
        }

        card.setOnClickListener(v -> openEditDialog(product));

        deleteBtn.setOnClickListener(v -> confirmDelete(product));
    }

    private void openEditDialog(Product product) {
        Intent intent = new Intent(this, AddProductActivity.class);
        intent.putExtra(AddProductActivity.EXTRA_PRODUCT_ID, product.getId());
        startActivity(intent);
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
}
