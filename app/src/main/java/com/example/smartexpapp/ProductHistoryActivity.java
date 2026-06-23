package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;
import com.example.smartexpapp.util.InAppNotificationManager;

import java.util.List;

public class ProductHistoryActivity extends BaseActivity {
    private ProductHistoryViewModel viewModel;
    private ProductHistoryAdapter adapter;
    private RecyclerView productList;
    private LinearLayout emptyState;
    private TextView statsText;
    private View loadingIndicator;
    private ProductRepository repository;

    private Button filterAll;
    private Button filterConsumed;
    private Button filterWasted;
    private Button filterDonated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_history);

        View bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        View archiveButton = findViewById(R.id.topActionArchive);
        if (archiveButton != null) archiveButton.setVisibility(View.GONE);

        TextView title = findViewById(R.id.topTitle);
        if (title != null) title.setText(R.string.app_name_chrome);

        loadingIndicator = findViewById(R.id.loadingIndicator);
        if (loadingIndicator == null) {
            loadingIndicator = new View(this);
            loadingIndicator.setVisibility(View.GONE);
        }

        statsText = findViewById(R.id.statsText);
        productList = findViewById(R.id.productList);
        emptyState = findViewById(R.id.emptyState);

        productList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductHistoryAdapter(this::onRestoreClick);
        productList.setAdapter(adapter);

        filterAll = findViewById(R.id.filterAll);
        filterConsumed = findViewById(R.id.filterConsumed);
        filterWasted = findViewById(R.id.filterWasted);
        filterDonated = findViewById(R.id.filterDonated);

        filterAll.setOnClickListener(v -> setActiveFilter("All", filterAll));
        filterConsumed.setOnClickListener(v -> setActiveFilter("CONSUMED", filterConsumed));
        filterWasted.setOnClickListener(v -> setActiveFilter("WASTED", filterWasted));
        filterDonated.setOnClickListener(v -> setActiveFilter("DONATED", filterDonated));

        repository = ((SmartExpAppApplication) getApplicationContext()).appContainer.getProductRepository();
        ProductHistoryViewModelFactory factory = new ProductHistoryViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(ProductHistoryViewModel.class);

        setupChrome(-1);

        ImageButton menuButton = findViewById(R.id.topActionMenu);
        if (menuButton != null) {
            menuButton.setVisibility(View.VISIBLE);
            menuButton.setImageResource(R.drawable.ic_arrow_back);
            menuButton.setContentDescription(getString(R.string.back));
            menuButton.setOnClickListener(v -> finish());
        }

        View historyContentScroll = findViewById(R.id.historyContentScroll);
        View historyActionContainer = findViewById(R.id.historyActionContainer);
        Button btnClearAllHistory = findViewById(R.id.btnClearAllHistory);
        if (btnClearAllHistory != null) {
            btnClearAllHistory.setOnClickListener(v -> onClearAllClick());
        }

        viewModel.getProducts().observe(this, products -> {
            adapter.submitList(products);
            boolean empty = products == null || products.isEmpty();
            if (historyContentScroll != null) {
                historyContentScroll.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (historyActionContainer != null) {
                historyActionContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getConsumedCount().observe(this, count -> updateStats());
        viewModel.getWastedCount().observe(this, count -> updateStats());
        viewModel.getDonatedCount().observe(this, count -> updateStats());

        viewModel.getIsLoading().observe(this, loading -> {
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.loadHistory();
    }

    private void setActiveFilter(String filter, Button activeButton) {
        viewModel.setFilter(filter);
        Button[] chips = {filterAll, filterConsumed, filterWasted, filterDonated};
        for (Button chip : chips) {
            if (chip == activeButton) {
                if (chip == filterAll) {
                    chip.setBackgroundResource(R.drawable.bg_chip_active_all);
                    chip.setTextColor(getColor(R.color.color_all_active_text));
                } else if (chip == filterConsumed) {
                    chip.setBackgroundResource(R.drawable.bg_chip_active_consumed);
                    chip.setTextColor(getColor(R.color.color_consumed_active_text));
                } else if (chip == filterWasted) {
                    chip.setBackgroundResource(R.drawable.bg_chip_active_wasted);
                    chip.setTextColor(getColor(R.color.color_wasted_active_text));
                } else if (chip == filterDonated) {
                    chip.setBackgroundResource(R.drawable.bg_chip_active_donated);
                    chip.setTextColor(getColor(R.color.color_donated_active_text));
                }
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_inactive);
                chip.setTextColor(getColor(R.color.smart_secondary));
            }
        }
    }

    private void updateStats() {
        Integer consumed = viewModel.getConsumedCount().getValue();
        Integer wasted = viewModel.getWastedCount().getValue();
        Integer donated = viewModel.getDonatedCount().getValue();
        int c = consumed != null ? consumed : 0;
        int w = wasted != null ? wasted : 0;
        int d = donated != null ? donated : 0;
        statsText.setText(getString(R.string.stats_format, c, w, d));
    }

    private String getLocalizedStatusLabel(String status) {
        if (ProductStatus.CONSUMED.equals(status)) {
            return getString(R.string.action_mark_consumed);
        } else if (ProductStatus.WASTED.equals(status)) {
            return getString(R.string.action_mark_wasted);
        } else if (ProductStatus.DONATED.equals(status)) {
            return getString(R.string.action_mark_donated);
        } else {
            return getString(R.string.badge_deleted);
        }
    }

    private void onRestoreClick(Product product) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_restore_confirm)
                .create();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.6f);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
        }
        dialog.show();

        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText(R.string.restore_title);
        ((TextView) dialog.findViewById(R.id.dialogMessage)).setText(
                getString(R.string.restore_message_format, product.getName()));

        dialog.findViewById(R.id.dialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.restoreProduct(product.getId(),
                    success -> {
                        if (success) {
                            runOnUiThread(() -> {
                                String prevStatus = product.getStatus();
                                InAppNotificationManager.showUndo(this,
                                        getString(R.string.restored_format, product.getName()),
                                        R.drawable.ic_check_circle,
                                        R.drawable.bg_action_icon_circle,
                                        R.color.smart_primary,
                                        () -> {
                                            ProductRepository.Callback<Boolean> callback = result -> {
                                                if (result) {
                                                    runOnUiThread(() -> {
                                                        showSuccessNotification(getString(R.string.mark_status_snackbar_format, product.getName(), getLocalizedStatusLabel(prevStatus)));
                                                        viewModel.loadHistory();
                                                    });
                                                }
                                            };
                                            ProductRepository.ErrorCallback err = e -> {
                                                runOnUiThread(() -> {
                                                    showErrorNotification(getString(R.string.error_load));
                                                });
                                            };
                                            if (ProductStatus.CONSUMED.equals(prevStatus)) {
                                                repository.markConsumedAsync(product.getId(), "Undo restore", callback, err);
                                            } else if (ProductStatus.WASTED.equals(prevStatus)) {
                                                repository.markWastedAsync(product.getId(), "Undo restore", callback, err);
                                            } else if (ProductStatus.DONATED.equals(prevStatus)) {
                                                repository.markDonatedAsync(product.getId(), "Undo restore", callback, err);
                                            } else if (ProductStatus.DELETED.equals(prevStatus)) {
                                                repository.markDeletedAsync(product.getId(), "Undo restore", callback, err);
                                            }
                                        });
                                viewModel.loadHistory();
                            });
                        }
                    },
                    error -> {
                        runOnUiThread(() -> {
                            showErrorNotification(getString(R.string.restore_error));
                        });
                    });
        });

        dialog.findViewById(R.id.dialogCancel).setOnClickListener(v -> dialog.dismiss());
    }

    private void onClearAllClick() {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_clear_history_confirm)
                .create();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.6f);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
        }
        dialog.show();

        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText(R.string.clear_history_title);
        ((TextView) dialog.findViewById(R.id.dialogMessage)).setText(R.string.clear_history_message);

        dialog.findViewById(R.id.dialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.VISIBLE);
            }
            viewModel.clearAllHistory(
                    success -> {
                        runOnUiThread(() -> {
                            if (loadingIndicator != null) {
                                loadingIndicator.setVisibility(View.GONE);
                            }
                            if (success) {
                                showSuccessNotification(getString(R.string.delete_local_data_done));
                                viewModel.loadHistory();
                            } else {
                                showErrorNotification(getString(R.string.delete_local_data_error));
                            }
                        });
                    },
                    error -> {
                        runOnUiThread(() -> {
                            if (loadingIndicator != null) {
                                loadingIndicator.setVisibility(View.GONE);
                            }
                            showErrorNotification(getString(R.string.delete_local_data_error));
                        });
                    });
        });

        dialog.findViewById(R.id.dialogCancel).setOnClickListener(v -> dialog.dismiss());
    }
}
