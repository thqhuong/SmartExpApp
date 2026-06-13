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
import com.example.smartexpapp.notifications.ReminderScheduler;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventoryActivity extends BaseActivity {
    private static final String[] STORAGE_KEYS = {"All", "Room Temp", "Refrigerator", "Freeze"};
    private static final String TAG = "InventoryActivity";
    public static final String EXTRA_FILTER = "com.example.smartexpapp.extra.FILTER";
    public static final String EXTRA_RESET_FILTERS = "com.example.smartexpapp.extra.RESET_FILTERS";
    public static final String FILTER_EXPIRING_SOON = "ExpiringSoon";

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
    private List<Product> latestProducts = new ArrayList<>();

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
        applyLaunchFilter(getIntent());

        showLoading();
        renderProducts();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyLaunchFilter(intent);
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
                switch (position) {
                    case 1:
                        currentFilter = "StillGood";
                        break;
                    case 2:
                        currentFilter = "Expired";
                        break;
                    default:
                        currentFilter = "All";
                        break;
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
                if (position >= 0 && position < STORAGE_KEYS.length) {
                    currentStorage = STORAGE_KEYS[position];
                } else {
                    currentStorage = "All";
                }
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
                switch (position) {
                    case 1:
                        currentSort = "name";
                        break;
                    case 2:
                        currentSort = "newest";
                        break;
                    default:
                        currentSort = "oldest";
                        break;
                }
                renderProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void applyLaunchFilter(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_RESET_FILTERS, false)) {
            resetInventoryFilters();
            return;
        }
        if (intent == null || !FILTER_EXPIRING_SOON.equals(intent.getStringExtra(EXTRA_FILTER))) {
            return;
        }
        currentFilter = FILTER_EXPIRING_SOON;
            Toast.makeText(this, R.string.reminder_filter_toast, Toast.LENGTH_SHORT).show();
    }

    private void resetInventoryFilters() {
        currentSearch = "";
        currentFilter = "All";
        currentStorage = "All";
        currentSort = "oldest";

        if (searchInput != null && searchInput.length() > 0) {
            searchInput.setText("");
        }
        if (expirySpinner != null && expirySpinner.getSelectedItemPosition() != 0) {
            expirySpinner.setSelection(0);
        }
        if (storageSpinner != null && storageSpinner.getSelectedItemPosition() != 0) {
            storageSpinner.setSelection(0);
        }
        if (sortSpinner != null && sortSpinner.getSelectedItemPosition() != 0) {
            sortSpinner.setSelection(0);
        }
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
        ProductRepository.searchAsync(this, currentSearch, products -> {
            latestProducts = new ArrayList<>(products);
            List<Product> filtered = filterProducts(products);
            List<Product> sorted = sortProducts(filtered);
            bindProducts(sorted);
            hideLoading();
        }, e -> {
            Log.e(TAG, "Failed to render products", e);
            hideLoading();
            productList.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            errorState.setVisibility(View.VISIBLE);
        });
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
        if (FILTER_EXPIRING_SOON.equals(currentFilter)) {
            return product.isExpiringSoon();
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
            } else if (FILTER_EXPIRING_SOON.equals(currentFilter)) {
                emptyTitle.setText(R.string.empty_expiring_soon);
                emptyDesc.setText(R.string.empty_expiring_soon_desc);
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
        categoryDot.getBackground().setTint(getColor(CategoryColorHelper.getColor(this, product.getCategory())));
        categoryDot.setVisibility(View.VISIBLE);

        ImageView icon = item.findViewById(R.id.productIcon);
        String imgUrl = product.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            icon.setImageTintList(null);
            ImageLoader.load(icon, imgUrl);
        } else {
            int tintColor = CategoryColorHelper.getColor(this, product.getCategory());
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(tintColor)));
            ViewUtils.setIcon(icon, product.getIconRes(), tintColor);
        }
        ((TextView) item.findViewById(R.id.productName)).setText(product.getName());
        ((TextView) item.findViewById(R.id.productMeta)).setText(getString(R.string.product_meta_format, product.getCategory(), product.getAmount()));
        expiryStatus.setText(product.getExpiryStatus());
        progress.setProgress(product.getExpiryProgress());

        float density = getResources().getDisplayMetrics().density;
        if (product.isExpired()) {
            card.setCardBackgroundColor(getColor(R.color.smart_glass_error));
            card.setStrokeColor(getColor(R.color.smart_glass_error_stroke));
            card.setStrokeWidth((int) (1.5f * density));
            urgentBadge.setVisibility(View.VISIBLE);
            urgentBadge.setBackgroundResource(R.drawable.bg_error_soft_badge);
            urgentBadge.setText(R.string.status_expired);
            urgentBadge.setTextColor(getColor(R.color.smart_error));
            expiryStatus.setTextColor(getColor(R.color.smart_error));
            progress.setProgressDrawable(AppCompatResources.getDrawable(this, R.drawable.progress_orange));
        } else if (product.isExpiringSoon()) {
            card.setCardBackgroundColor(getColor(R.color.smart_glass_urgent));
            card.setStrokeColor(getColor(R.color.smart_glass_urgent_stroke));
            card.setStrokeWidth((int) (1.5f * density));
            urgentBadge.setVisibility(View.VISIBLE);
            urgentBadge.setBackgroundResource(R.drawable.bg_primary_soft_badge);
            urgentBadge.setText(R.string.status_expiring);
            urgentBadge.setTextColor(getColor(R.color.smart_primary_container));
            expiryStatus.setTextColor(getColor(R.color.smart_primary_container));
            progress.setProgressDrawable(AppCompatResources.getDrawable(this, R.drawable.progress_orange));
        } else {
            card.setCardBackgroundColor(getColor(R.color.smart_glass_surface));
            card.setStrokeColor(getColor(R.color.smart_glass_stroke));
            card.setStrokeWidth((int) (1.0f * density));
            urgentBadge.setVisibility(View.GONE);
            expiryStatus.setTextColor(getColor(R.color.smart_on_surface));
            progress.setProgressDrawable(AppCompatResources.getDrawable(this, R.drawable.progress_gray));
        }

        card.setOnClickListener(v -> showProductActions(product));

        deleteBtn.setOnClickListener(v -> confirmDelete(product));
    }

    private void showProductActions(Product product) {
        String[] actions = {"Edit", "Mark consumed", "Mark wasted", "Mark donated", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle(product.getName())
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openEditDialog(product);
                            break;
                        case 1:
                            markConsumed(product);
                            break;
                        case 2:
                            markWasted(product);
                            break;
                        case 3:
                            markDonated(product);
                            break;
                        case 4:
                            confirmDelete(product);
                            break;
                        default:
                            break;
                    }
                })
                .show();
    }

    private void openEditDialog(Product product) {
        new EditProductDialog(product, this::renderProducts, latestProducts).show(this);
    }

    private void confirmDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, product.getName()))
                .setPositiveButton(R.string.delete_confirm, (dialog, which) -> {
                    ProductRepository.deleteProductAsync(this, product.getId(), deleted -> {
                        if (Boolean.TRUE.equals(deleted)) {
                            Toast.makeText(this, R.string.product_deleted, Toast.LENGTH_SHORT).show();
                            ReminderScheduler.runSoon(this);
                            renderProducts();
                        }
                    }, error -> {
                        Log.e(TAG, "Failed to delete product", error);
                        Toast.makeText(this, R.string.error_load, Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void markConsumed(Product product) {
        ProductRepository.markConsumedAsync(this, product.getId(), "Marked from inventory",
                updated -> onStatusUpdated(updated, product.getName() + " marked consumed."),
                error -> onStatusUpdateFailed(error));
    }

    private void markWasted(Product product) {
        ProductRepository.markWastedAsync(this, product.getId(), "Marked from inventory",
                updated -> onStatusUpdated(updated, product.getName() + " marked wasted."),
                error -> onStatusUpdateFailed(error));
    }

    private void markDonated(Product product) {
        ProductRepository.markDonatedAsync(this, product.getId(), "Marked from inventory",
                updated -> onStatusUpdated(updated, product.getName() + " marked donated."),
                error -> onStatusUpdateFailed(error));
    }

    private void onStatusUpdated(Boolean updated, String message) {
        if (Boolean.TRUE.equals(updated)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            ReminderScheduler.runSoon(this);
            renderProducts();
        }
    }

    private void onStatusUpdateFailed(Exception error) {
        Log.e(TAG, "Failed to update product status", error);
        Toast.makeText(this, R.string.error_load, Toast.LENGTH_SHORT).show();
    }
}
