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
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class ProductHistoryActivity extends BaseActivity {
    private ProductHistoryViewModel viewModel;
    private ProductHistoryAdapter adapter;
    private RecyclerView productList;
    private LinearLayout emptyState;
    private TextView statsText;
    private View loadingIndicator;

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

        View searchButton = findViewById(R.id.topActionSearch);
        if (searchButton != null) searchButton.setVisibility(View.GONE);

        TextView title = findViewById(R.id.topTitle);
        if (title != null) title.setText(R.string.history_title);

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

        ProductRepository repository = ((SmartExpApplication) getApplicationContext()).appContainer.getProductRepository();
        ProductHistoryViewModelFactory factory = new ProductHistoryViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(ProductHistoryViewModel.class);

        setupChrome(-1);

        ImageButton menuButton = findViewById(R.id.topActionMenu);
        if (menuButton != null) {
            menuButton.setImageResource(R.drawable.ic_arrow_back);
            menuButton.setContentDescription(getString(R.string.back));
            menuButton.setOnClickListener(v -> finish());
        }

        viewModel.getProducts().observe(this, products -> {
            adapter.submitList(products);
            boolean empty = products == null || products.isEmpty();
            productList.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
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
                chip.setBackgroundResource(R.drawable.bg_chip_active);
                chip.setTextColor(getColor(R.color.smart_on_primary));
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

    private void onRestoreClick(Product product) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_title)
                .setMessage(getString(R.string.restore_message_format, product.getName()))
                .setPositiveButton(R.string.restore_confirm, (dialog, which) -> {
                    viewModel.restoreProduct(product.getId(),
                            success -> {
                                if (success) {
                                    runOnUiThread(() -> {
                                        Snackbar.make(productList,
                                                getString(R.string.restored_format, product.getName()),
                                                Snackbar.LENGTH_LONG).show();
                                        viewModel.loadHistory();
                                    });
                                }
                            },
                            error -> {
                                runOnUiThread(() -> {
                                    Snackbar.make(productList,
                                            getString(R.string.restore_error),
                                            Snackbar.LENGTH_LONG).show();
                                });
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
