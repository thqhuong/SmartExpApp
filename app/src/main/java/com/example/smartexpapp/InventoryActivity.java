package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.notifications.ReminderScheduler;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends BaseActivity {
    private static final String[] STORAGE_KEYS = { "All", "Room Temp", "Refrigerator", "Freeze" };
    private static final String TAG = "InventoryActivity";
    public static final String EXTRA_FILTER = "com.example.smartexpapp.extra.FILTER";
    public static final String EXTRA_RESET_FILTERS = "com.example.smartexpapp.extra.RESET_FILTERS";
    public static final String FILTER_EXPIRING_SOON = "ExpiringSoon";

    private RecyclerView productList;
    private InventoryAdapter adapter;
    private LinearLayout emptyState;
    private TextView emptyTitle;
    private TextView emptyDesc;
    private TextView errorState;
    private ProgressBar loadingIndicator;
    private EditText searchInput;
    private Spinner expirySpinner;
    private Spinner storageSpinner;
    private Spinner sortSpinner;

    private List<Product> latestProducts = new ArrayList<>();
    private InventoryViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Authentication and guest mode check
        boolean isAuthenticated = false;
        try {
            if (!com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                isAuthenticated = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Firebase Auth state", e);
        }

        boolean guestModeEnabled = getSharedPreferences("auth_prefs", MODE_PRIVATE).getBoolean("guest_mode_enabled",
                false);
        if (!isAuthenticated && !guestModeEnabled) {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_inventory);
        setupChrome(R.id.nav_inventory);

        productList = findViewById(R.id.productList);
        productList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(new InventoryAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                showProductActions(product);
            }

            @Override
            public void onDeleteClick(Product product) {
                confirmDelete(product);
            }
        });
        productList.setAdapter(adapter);

        emptyState = findViewById(R.id.emptyState);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptyDesc = findViewById(R.id.emptyDesc);
        errorState = findViewById(R.id.errorState);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        searchInput = findViewById(R.id.searchInput);
        expirySpinner = findViewById(R.id.expirySpinner);
        storageSpinner = findViewById(R.id.storageSpinner);
        sortSpinner = findViewById(R.id.sortSpinner);

        AppContainer appContainer = ((SmartExpApplication) getApplicationContext()).appContainer;
        InventoryViewModelFactory factory = new InventoryViewModelFactory(appContainer.getProductRepository());
        viewModel = new ViewModelProvider(this, factory).get(InventoryViewModel.class);

        setupSearch();
        setupSpinners();
        applyLaunchFilter(getIntent());

        setupObservers();

        viewModel.loadProducts();
    }

    private void setupObservers() {
        viewModel.getProducts().observe(this, products -> {
            latestProducts = products;
            bindProducts(products);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                showLoading();
            } else {
                hideLoading();
            }
        });

        viewModel.getIsError().observe(this, isError -> {
            if (Boolean.TRUE.equals(isError)) {
                hideLoading();
                productList.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
                errorState.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyLaunchFilter(intent);
        viewModel.loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadProducts();
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
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
                String filter;
                switch (position) {
                    case 1:
                        filter = "StillGood";
                        break;
                    case 2:
                        filter = "Expired";
                        break;
                    default:
                        filter = "All";
                        break;
                }
                viewModel.setExpiryFilter(filter);
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
                String storage = (position >= 0 && position < STORAGE_KEYS.length) ? STORAGE_KEYS[position] : "All";
                viewModel.setStorageFilter(storage);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                viewModel.setStorageFilter("All");
            }
        });

        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sort;
                switch (position) {
                    case 1:
                        sort = "name";
                        break;
                    case 2:
                        sort = "newest";
                        break;
                    default:
                        sort = "oldest";
                        break;
                }
                viewModel.setSortOrder(sort);
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
        viewModel.applyLaunchFilter(FILTER_EXPIRING_SOON);
        Toast.makeText(this, R.string.reminder_filter_toast, Toast.LENGTH_SHORT).show();
    }

    private void resetInventoryFilters() {
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
        viewModel.resetFilters();
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        productList.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
    }

    private void bindProducts(List<Product> products) {
        if (products.isEmpty()) {
            productList.setVisibility(View.GONE);
            errorState.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);

            String currentSearch = viewModel.getCurrentSearch();
            String currentFilter = viewModel.getCurrentFilter();

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
        } else {
            productList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            errorState.setVisibility(View.GONE);
        }

        adapter.submitList(products);
    }

    private void showProductActions(Product product) {
        String[] actions = {
                getString(R.string.action_edit),
                getString(R.string.action_mark_consumed),
                getString(R.string.action_mark_wasted),
                getString(R.string.action_mark_donated),
                getString(R.string.action_delete)
        };
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
        new EditProductDialog(product, viewModel::loadProducts, latestProducts).show(this);
    }

    private void confirmDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, product.getName()))
                .setPositiveButton(R.string.delete_confirm,
                        (dialog, which) -> viewModel.deleteProduct(product.getId(), deleted -> {
                            if (Boolean.TRUE.equals(deleted)) {
                                Toast.makeText(this, R.string.product_deleted, Toast.LENGTH_SHORT).show();
                                ReminderScheduler.runSoon(this);
                                viewModel.loadProducts();
                            }
                        }, error -> {
                            Log.e(TAG, "Failed to delete product", error);
                            Toast.makeText(this, R.string.error_load, Toast.LENGTH_SHORT).show();
                        }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void markConsumed(Product product) {
        viewModel.markConsumed(product.getId(), getString(R.string.note_marked_from_inventory),
                updated -> onStatusUpdated(updated, getString(R.string.toast_mark_consumed_format, product.getName())),
                this::onStatusUpdateFailed);
    }

    private void markWasted(Product product) {
        viewModel.markWasted(product.getId(), getString(R.string.note_marked_from_inventory),
                updated -> onStatusUpdated(updated, getString(R.string.toast_mark_wasted_format, product.getName())),
                this::onStatusUpdateFailed);
    }

    private void markDonated(Product product) {
        viewModel.markDonated(product.getId(), getString(R.string.note_marked_from_inventory),
                updated -> onStatusUpdated(updated, getString(R.string.toast_mark_donated_format, product.getName())),
                this::onStatusUpdateFailed);
    }

    private void onStatusUpdated(Boolean updated, String message) {
        if (Boolean.TRUE.equals(updated)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            ReminderScheduler.runSoon(this);
            viewModel.loadProducts();
        }
    }

    private void onStatusUpdateFailed(Exception error) {
        Log.e(TAG, "Failed to update product status", error);
        Toast.makeText(this, R.string.error_load, Toast.LENGTH_SHORT).show();
    }
}
