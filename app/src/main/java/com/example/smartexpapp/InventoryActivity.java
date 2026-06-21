package com.example.smartexpapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.local.LocalDataContract;
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
    private static final String[] STORAGE_KEYS = {
            "All",
            LocalDataContract.STORAGE_ROOM_TEMP_NAME,
            LocalDataContract.STORAGE_REFRIGERATOR_NAME,
            LocalDataContract.STORAGE_FREEZE_NAME
    };
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
    private TextView expiryFilterValue;
    private TextView storageFilterValue;
    private TextView sortFilterValue;
    private LinearLayout expiryFilterDropdown;
    private LinearLayout storageFilterDropdown;
    private LinearLayout sortFilterDropdown;
    private ImageView expiryFilterChevron;
    private ImageView storageFilterChevron;
    private ImageView sortFilterChevron;

    private List<Product> latestProducts = new ArrayList<>();
    private InventoryViewModel viewModel;
    private View undoBarView;
    private Handler undoHandler;

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
        expiryFilterValue = findViewById(R.id.expiryFilterValue);
        storageFilterValue = findViewById(R.id.storageFilterValue);
        sortFilterValue = findViewById(R.id.sortFilterValue);
        expiryFilterDropdown = findViewById(R.id.expiryFilterDropdown);
        storageFilterDropdown = findViewById(R.id.storageFilterDropdown);
        sortFilterDropdown = findViewById(R.id.sortFilterDropdown);
        expiryFilterChevron = findViewById(R.id.expiryFilterChevron);
        storageFilterChevron = findViewById(R.id.storageFilterChevron);
        sortFilterChevron = findViewById(R.id.sortFilterChevron);

        AppContainer appContainer = ((SmartExpApplication) getApplicationContext()).appContainer;
        InventoryViewModelFactory factory = new InventoryViewModelFactory(appContainer.getProductRepository());
        viewModel = new ViewModelProvider(this, factory).get(InventoryViewModel.class);

        setupSearch();
        setupFilters();
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

    private void setupFilters() {
        findViewById(R.id.expiryFilterRow).setOnClickListener(v -> {
            closeDropdown(storageFilterDropdown, storageFilterChevron);
            closeDropdown(sortFilterDropdown, sortFilterChevron);
            toggleDropdown(expiryFilterDropdown, expiryFilterChevron);
        });
        findViewById(R.id.storageFilterRow).setOnClickListener(v -> {
            closeDropdown(expiryFilterDropdown, expiryFilterChevron);
            closeDropdown(sortFilterDropdown, sortFilterChevron);
            toggleDropdown(storageFilterDropdown, storageFilterChevron);
        });
        findViewById(R.id.sortFilterRow).setOnClickListener(v -> {
            closeDropdown(expiryFilterDropdown, expiryFilterChevron);
            closeDropdown(storageFilterDropdown, storageFilterChevron);
            toggleDropdown(sortFilterDropdown, sortFilterChevron);
        });

        populateDropdown(expiryFilterDropdown, R.array.expiry_filter_options, new DropdownCallback() {
            @Override
            public void onSelected(int index, String label) {
                String filter;
                switch (index) {
                    case 1: filter = "StillGood"; break;
                    case 2: filter = "Expired"; break;
                    default: filter = "All"; break;
                }
                expiryFilterValue.setText(label);
                viewModel.setExpiryFilter(filter);
                toggleDropdown(expiryFilterDropdown, expiryFilterChevron);
            }
        });

        populateDropdown(storageFilterDropdown, R.array.storage_filter_options, new DropdownCallback() {
            @Override
            public void onSelected(int index, String label) {
                String storage = (index >= 0 && index < STORAGE_KEYS.length) ? STORAGE_KEYS[index] : "All";
                storageFilterValue.setText(label);
                viewModel.setStorageFilter(storage);
                toggleDropdown(storageFilterDropdown, storageFilterChevron);
            }
        });

        populateDropdown(sortFilterDropdown, R.array.sort_options, new DropdownCallback() {
            @Override
            public void onSelected(int index, String label) {
                String sort;
                switch (index) {
                    case 1: sort = "name"; break;
                    case 2: sort = "newest"; break;
                    default: sort = "oldest"; break;
                }
                sortFilterValue.setText(label);
                viewModel.setSortOrder(sort);
                toggleDropdown(sortFilterDropdown, sortFilterChevron);
            }
        });

        expiryFilterValue.setText(getResources().getStringArray(R.array.expiry_filter_options)[0]);
        storageFilterValue.setText(getResources().getStringArray(R.array.storage_filter_options)[0]);
        sortFilterValue.setText(getResources().getStringArray(R.array.sort_options)[0]);
    }

    private void toggleDropdown(LinearLayout dropdown, ImageView chevron) {
        boolean isOpen = dropdown.getVisibility() == View.VISIBLE;
        if (isOpen) {
            hideDropdown(dropdown, chevron);
        } else {
            showDropdown(dropdown, chevron);
        }
    }

    private void showDropdown(LinearLayout dropdown, ImageView chevron) {
        dropdown.setAlpha(0f);
        dropdown.setTranslationY(-8f);
        dropdown.setVisibility(View.VISIBLE);
        dropdown.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start();
        chevron.animate().rotation(180f).setDuration(200).start();
    }

    private void hideDropdown(LinearLayout dropdown, ImageView chevron) {
        dropdown.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(150)
                .withEndAction(() -> {
                    dropdown.setVisibility(View.GONE);
                    dropdown.setTranslationY(0f);
                })
                .start();
        chevron.animate().rotation(0f).setDuration(200).start();
    }

    private void closeDropdown(LinearLayout dropdown, ImageView chevron) {
        if (dropdown.getVisibility() == View.VISIBLE) {
            hideDropdown(dropdown, chevron);
        }
    }

    private void populateDropdown(LinearLayout container, int arrayResId, DropdownCallback callback) {
        String[] options = getResources().getStringArray(arrayResId);
        container.removeAllViews();
        for (int i = 0; i < options.length; i++) {
            View option = getLayoutInflater().inflate(R.layout.item_filter_option, container, false);
            TextView label = option.findViewById(R.id.filterOptionLabel);
            ImageView radio = option.findViewById(R.id.filterOptionRadio);
            label.setText(options[i]);
            final int pos = i;
            option.setOnClickListener(v -> {
                updateRadioSelection(container, pos);
                callback.onSelected(pos, options[pos]);
            });
            container.addView(option);
        }
        updateRadioSelection(container, 0);
    }

    private void updateRadioSelection(LinearLayout container, int selectedIndex) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View option = container.getChildAt(i);
            ImageView radio = option.findViewById(R.id.filterOptionRadio);
            radio.setImageResource(i == selectedIndex
                    ? R.drawable.ic_radio_button_checked
                    : R.drawable.ic_radio_button_unchecked);
        }
    }

    interface DropdownCallback {
        void onSelected(int index, String label);
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
        String[] expiryOptions = getResources().getStringArray(R.array.expiry_filter_options);
        String[] storageOptions = getResources().getStringArray(R.array.storage_filter_options);
        String[] sortOptions = getResources().getStringArray(R.array.sort_options);
        expiryFilterValue.setText(expiryOptions[0]);
        storageFilterValue.setText(storageOptions[0]);
        sortFilterValue.setText(sortOptions[0]);
        updateRadioSelection(expiryFilterDropdown, 0);
        updateRadioSelection(storageFilterDropdown, 0);
        updateRadioSelection(sortFilterDropdown, 0);
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
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_product_actions,
                findViewById(android.R.id.content), false);
        dialog.setContentView(view);

        TextView name = view.findViewById(R.id.bsProductName);
        name.setText(product.getName());

        TextView meta = view.findViewById(R.id.bsProductMeta);
        meta.setText(getString(R.string.product_meta_format, product.getStorage(), product.getAmount()));

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
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_mark_status)
                .create();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.6f);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
        }
        dialog.show();

        ((TextView) dialog.findViewById(R.id.dialogProductName)).setText(product.getName());

        int iconRes;
        int iconTintRes;
        String upperLabel;
        boolean showWarning = false;

        switch (action) {
            case "wasted":
                iconRes = R.drawable.ic_close;
                iconTintRes = R.color.smart_error;
                upperLabel = "MARK WASTED";
                showWarning = true;
                break;
            case "donated":
                iconRes = R.drawable.ic_favorite_filled;
                iconTintRes = R.color.smart_notification_red;
                upperLabel = "MARK DONATED";
                break;
            default:
                iconRes = R.drawable.ic_check_circle;
                iconTintRes = R.color.smart_primary;
                upperLabel = "MARK CONSUMED";
                break;
        }

        ((TextView) dialog.findViewById(R.id.dialogActionLabelUpper)).setText(upperLabel);
        ImageView icon = dialog.findViewById(R.id.dialogActionIcon);
        icon.setImageResource(iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                getColor(iconTintRes)));

        if (showWarning) {
            dialog.findViewById(R.id.dialogWarningContainer).setVisibility(View.VISIBLE);
            ((TextView) dialog.findViewById(R.id.dialogWarning)).setText(R.string.mark_wasted_confirm_text);
        } else {
            dialog.findViewById(R.id.dialogWarningContainer).setVisibility(View.GONE);
        }

        dialog.findViewById(R.id.dialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            EditText noteInput = dialog.findViewById(R.id.dialogNoteInput);
            String note = noteInput.getText().toString().trim();
            executeMarkAction(product, action, note);
        });

        dialog.findViewById(R.id.dialogCancel).setOnClickListener(v -> dialog.dismiss());
    }

    private void executeMarkAction(Product product, String action, String note) {
        int iconRes;
        int iconBgRes;
        int iconTintRes;
        String statusLabel;

        switch (action) {
            case "wasted":
                iconRes = R.drawable.ic_close;
                iconBgRes = R.drawable.bg_action_icon_circle_delete;
                iconTintRes = R.color.smart_error;
                statusLabel = getString(R.string.action_mark_wasted);
                break;
            case "donated":
                iconRes = R.drawable.ic_favorite_filled;
                iconBgRes = R.drawable.bg_action_icon_circle;
                iconTintRes = R.color.smart_notification_red;
                statusLabel = getString(R.string.action_mark_donated);
                break;
            default:
                iconRes = R.drawable.ic_check_circle;
                iconBgRes = R.drawable.bg_action_icon_circle;
                iconTintRes = R.color.smart_primary;
                statusLabel = getString(R.string.action_mark_consumed);
                break;
        }

        switch (action) {
            case "wasted":
                viewModel.markWasted(product.getId(), note,
                        updated -> onMarkSuccess(updated, product, iconRes, iconBgRes, iconTintRes, statusLabel),
                        this::onStatusUpdateFailed);
                break;
            case "donated":
                viewModel.markDonated(product.getId(), note,
                        updated -> onMarkSuccess(updated, product, iconRes, iconBgRes, iconTintRes, statusLabel),
                        this::onStatusUpdateFailed);
                break;
            default:
                viewModel.markConsumed(product.getId(), note,
                        updated -> onMarkSuccess(updated, product, iconRes, iconBgRes, iconTintRes, statusLabel),
                        this::onStatusUpdateFailed);
                break;
        }
    }

    private void onMarkSuccess(Boolean updated, Product product,
                               int iconRes, int iconBgRes, int iconTintRes, String statusLabel) {
        if (Boolean.TRUE.equals(updated)) {
            ReminderScheduler.runSoon(this);
            viewModel.loadProducts();
            String message = getString(R.string.mark_status_snackbar_format, product.getName(), statusLabel);
            showUndoBar(product, iconRes, iconBgRes, iconTintRes, message, () -> undoMark(product));
        }
    }

    private void undoMark(Product product) {
        viewModel.revertStatus(product.getId(), "Reverted from undo bar",
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

    private void showUndoBar(Product product, int iconRes, int iconBgRes, int iconTintRes,
                             String message, Runnable undoAction) {
        ViewGroup root = findViewById(R.id.root);
        if (root == null) return;

        if (undoBarView != null) {
            root.removeView(undoBarView);
            undoBarView = null;
        }
        if (undoHandler != null) {
            undoHandler.removeCallbacksAndMessages(null);
        }

        undoBarView = LayoutInflater.from(this).inflate(R.layout.dialog_undo_delete, root, false);

        FrameLayout iconContainer = undoBarView.findViewById(R.id.undoIconContainer);
        iconContainer.setBackgroundResource(iconBgRes);

        ImageView icon = undoBarView.findViewById(R.id.undoIcon);
        icon.setImageResource(iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                getColor(iconTintRes)));

        ((TextView) undoBarView.findViewById(R.id.undoMessage)).setText(message);

        undoBarView.findViewById(R.id.undoAction).setOnClickListener(v -> {
            dismissUndoBar();
            undoAction.run();
        });

        root.addView(undoBarView, root.getChildCount() - 1);

        undoBarView.setTranslationY(200f);
        undoBarView.setAlpha(0f);
        undoBarView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(350)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .start();

        undoHandler = new Handler(Looper.getMainLooper());
        undoHandler.postDelayed(this::dismissUndoBar, 5000);
    }

    private void dismissUndoBar() {
        if (undoHandler != null) {
            undoHandler.removeCallbacksAndMessages(null);
        }
        if (undoBarView != null) {
            undoBarView.animate()
                    .translationY(200f)
                    .alpha(0f)
                    .setDuration(250)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        ViewGroup root = findViewById(R.id.root);
                        if (root != null && undoBarView != null) {
                            root.removeView(undoBarView);
                        }
                        undoBarView = null;
                    })
                    .start();
        }
    }

    private void openEditDialog(Product product) {
        new EditProductDialog(product, viewModel::loadProducts, latestProducts).show(this);
    }

    private void confirmDelete(Product product) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_delete_confirm)
                .create();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.6f);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
        }
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
                    showUndoBar(product,
                            R.drawable.ic_delete,
                            R.drawable.bg_action_icon_circle_delete,
                            R.color.smart_error,
                            getString(R.string.undo_delete_format, product.getName()),
                            () -> undoMark(product));
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
