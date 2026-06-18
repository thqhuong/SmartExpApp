package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.notifications.ReminderScheduler;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

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

        AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(this);
        if (!authState.isSignedIn() && !authState.isGuest()) {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_inventory);
        setupChrome(R.id.nav_inventory);

        View archiveButton = findViewById(R.id.topActionArchive);
        if (archiveButton != null) {
            archiveButton.setVisibility(View.VISIBLE);
            archiveButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductHistoryActivity.class);
                startActivity(intent);
            });
        }

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
        storageAdapter        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        sortAdapter        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_product_actions, null);
        dialog.setContentView(view);

        TextView name = view.findViewById(R.id.bsProductName);
        name.setText(product.getName());

        TextView meta = view.findViewById(R.id.bsProductMeta);
        meta.setText(product.getStorage() + " \u2022 " + product.getAmount());

        ImageView icon = view.findViewById(R.id.bsProductImage);
        ImageView placeholder = view.findViewById(R.id.bsProductPlaceholder);
        String imgUrl = product.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            placeholder.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
            icon.setImageTintList(null);
            ImageLoader.load(icon, imgUrl);
        } else {
            icon.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            int tintColor = CategoryColorHelper.getColor(this, product.getCategory());
            placeholder.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getColor(tintColor)));
            ViewUtils.setIcon(placeholder, product.getIconRes(), tintColor);
        }

        view.findViewById(R.id.bsActionEdit).setOnClickListener(v -> {
            dialog.dismiss();
            openEditDialog(product);
        });
        view.findViewById(R.id.bsActionConsumed).setOnClickListener(v -> {
            dialog.dismiss();
            showMarkDialog(product, "consumed");
        });
        view.findViewById(R.id.bsActionWasted).setOnClickListener(v -> {
            dialog.dismiss();
            showMarkDialog(product, "wasted");
        });
        view.findViewById(R.id.bsActionDonated).setOnClickListener(v -> {
            dialog.dismiss();
            showMarkDialog(product, "donated");
        });
        view.findViewById(R.id.bsActionDelete).setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(product);
        });

        dialog.show();
    }

    private void showMarkDialog(Product product, String action) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_mark_status, null);
        ((TextView) view.findViewById(R.id.dialogProductName)).setText(product.getName());

        int actionLabelRes;
        String statusKey;
        switch (action) {
            case "wasted":
                actionLabelRes = R.string.action_mark_wasted;
                statusKey = "wasted";
                view.findViewById(R.id.dialogWarning).setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.dialogWarning)).setText(R.string.mark_wasted_confirm_text);
                break;
            case "donated":
                actionLabelRes = R.string.action_mark_donated;
                statusKey = "donated";
                break;
            default:
                actionLabelRes = R.string.action_mark_consumed;
                statusKey = "consumed";
                break;
        }
        ((TextView) view.findViewById(R.id.dialogActionLabel)).setText(actionLabelRes);

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.mark_status_confirm, (dialog, which) -> {
                    EditText noteInput = view.findViewById(R.id.dialogNoteInput);
                    String note = noteInput.getText().toString().trim();
                    executeMarkAction(product, statusKey, note);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void executeMarkAction(Product product, String action, String note) {
        switch (action) {
            case "wasted":
                viewModel.markWasted(product.getId(), note,
                        updated -> onStatusSuccess(updated, product, getString(R.string.toast_mark_wasted_format, product.getName())),
                        this::onStatusUpdateFailed);
                break;
            case "donated":
                viewModel.markDonated(product.getId(), note,
                        updated -> onStatusSuccess(updated, product, getString(R.string.toast_mark_donated_format, product.getName())),
                        this::onStatusUpdateFailed);
                break;
            default:
                viewModel.markConsumed(product.getId(), note,
                        updated -> onStatusSuccess(updated, product, getString(R.string.toast_mark_consumed_format, product.getName())),
                        this::onStatusUpdateFailed);
                break;
        }
    }

    private void onStatusSuccess(Boolean updated, Product product, String toastMessage) {
        if (Boolean.TRUE.equals(updated)) {
            ReminderScheduler.runSoon(this);
            viewModel.loadProducts();

            String snackAction = actionLabel(product, toastMessage);
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.mark_status_snackbar_format, product.getName(), snackAction),
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.mark_undo, v -> undoMark(product))
                    .show();
        }
    }

    private String actionLabel(Product product, String toastMessage) {
        if (toastMessage.contains(getString(R.string.action_mark_wasted).toLowerCase()))
            return getString(R.string.action_mark_wasted);
        if (toastMessage.contains(getString(R.string.action_mark_donated).toLowerCase()))
            return getString(R.string.action_mark_donated);
        return getString(R.string.action_mark_consumed);
    }

    private void undoMark(Product product) {
        viewModel.revertStatus(product.getId(), "Reverted from snackbar undo",
                reverted -> {
                    if (Boolean.TRUE.equals(reverted)) {
                        Toast.makeText(this,
                                getString(R.string.mark_undone_format, product.getName()),
                                Toast.LENGTH_SHORT).show();
                        ReminderScheduler.runSoon(this);
                        viewModel.loadProducts();
                    }
                }, this::onStatusUpdateFailed);
    }

    private void openEditDialog(Product product) {
        new EditProductDialog(product, viewModel::loadProducts, latestProducts).show(this);
    }

    private void confirmDelete(Product product) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_delete_confirm)
                .create();
        dialog.getWindow().setDimAmount(0.6f);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                android.graphics.Color.TRANSPARENT));
        dialog.show();

        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText(R.string.delete_title);
        ((TextView) dialog.findViewById(R.id.dialogMessage)).setText(
                getString(R.string.delete_message, product.getName()));

        dialog.findViewById(R.id.dialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.softDeleteProduct(product.getId(), "Deleted from inventory", deleted -> {
                if (Boolean.TRUE.equals(deleted)) {
                    ReminderScheduler.runSoon(this);
                    viewModel.loadProducts();
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.product_deleted) + ": " + product.getName(),
                            Snackbar.LENGTH_LONG)
                            .setAction(R.string.mark_undo, v2 -> undoMark(product))
                            .show();
                }
            }, error -> {
                Log.e(TAG, "Failed to delete product", error);
                Toast.makeText(this, R.string.error_load, Toast.LENGTH_SHORT).show();
            });
        });

        dialog.findViewById(R.id.dialogCancel).setOnClickListener(v -> dialog.dismiss());
    }

    private void onStatusUpdateFailed(Exception error) {
        Log.e(TAG, "Failed to update product status", error);
        Toast.makeText(this, R.string.error_load, Toast.LENGTH_SHORT).show();
    }
}
