package com.example.smartexpapp;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.Product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventoryViewModel extends ViewModel {
    public enum EmptyState {
        NONE,
        EMPTY_INVENTORY,
        EMPTY_SEARCH,
        EMPTY_EXPIRING_SOON
    }

    private final ProductRepository productRepository;

    private final MutableLiveData<List<Product>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isError = new MutableLiveData<>(false);
    private final MutableLiveData<EmptyState> emptyState = new MutableLiveData<>(EmptyState.NONE);

    private String currentSearch = "";
    private String currentFilter = "All";
    private String currentStorage = "All";
    private String currentSort = "oldest";
    private int expiringSoonDays = Product.DEFAULT_EXPIRING_SOON_DAYS;
    private List<Product> latestProducts = new ArrayList<>();

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    public InventoryViewModel(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public LiveData<List<Product>> getProducts() {
        return products;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsError() {
        return isError;
    }

    public LiveData<EmptyState> getEmptyState() {
        return emptyState;
    }

    public String getCurrentSearch() {
        return currentSearch;
    }

    public String getCurrentFilter() {
        return currentFilter;
    }

    public String getCurrentStorage() {
        return currentStorage;
    }

    public String getCurrentSort() {
        return currentSort;
    }

    public void loadProducts() {
        isLoading.setValue(true);
        isError.setValue(false);

        productRepository.searchAsync(currentSearch, result -> {
            latestProducts = new ArrayList<>(result);
            applyFiltersAndSort();
            isLoading.setValue(false);
        }, e -> {
            isError.setValue(true);
            isLoading.setValue(false);
        });
    }

    public void setSearchQuery(String query) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> {
            currentSearch = query != null ? query : "";
            loadProducts();
        };
        searchHandler.postDelayed(searchRunnable, 300);
    }

    public void setExpiryFilter(String filter) {
        this.currentFilter = filter != null ? filter : "All";
        applyFiltersAndSort();
    }

    public void setStorageFilter(String filter) {
        this.currentStorage = filter != null ? filter : "All";
        applyFiltersAndSort();
    }

    public void setSortOrder(String sort) {
        this.currentSort = sort != null ? sort : "oldest";
        applyFiltersAndSort();
    }

    public void setExpiringSoonDays(int days) {
        this.expiringSoonDays = days;
        applyFiltersAndSort();
    }

    public int getExpiringSoonDays() {
        return expiringSoonDays;
    }

    public void applyLaunchFilter(String filter) {
        this.currentFilter = filter != null ? filter : "All";
    }

    public void resetFilters() {
        this.currentSearch = "";
        this.currentFilter = "All";
        this.currentStorage = "All";
        this.currentSort = "oldest";
        loadProducts();
    }

    public void deleteProduct(String id, ProductRepository.Callback<Boolean> callback, ProductRepository.ErrorCallback errorCallback) {
        productRepository.deleteProductAsync(id, callback, errorCallback);
    }

    public void softDeleteProduct(String id, String note, ProductRepository.Callback<Boolean> callback, ProductRepository.ErrorCallback errorCallback) {
        productRepository.markDeletedAsync(id, note, callback, errorCallback);
    }

    public void markConsumed(String id, String note, ProductRepository.Callback<Boolean> callback, ProductRepository.ErrorCallback errorCallback) {
        productRepository.markConsumedAsync(id, note, callback, errorCallback);
    }

    public void markWasted(String id, String note, ProductRepository.Callback<Boolean> callback, ProductRepository.ErrorCallback errorCallback) {
        productRepository.markWastedAsync(id, note, callback, errorCallback);
    }

    public void markDonated(String id, String note, ProductRepository.Callback<Boolean> callback, ProductRepository.ErrorCallback errorCallback) {
        productRepository.markDonatedAsync(id, note, callback, errorCallback);
    }

    public void revertStatus(String id, String note, ProductRepository.Callback<Boolean> callback, ProductRepository.ErrorCallback errorCallback) {
        productRepository.markActiveAsync(id, note, callback, errorCallback);
    }

    private void applyFiltersAndSort() {
        List<Product> filtered = new ArrayList<>();
        for (Product product : latestProducts) {
            if (matchesFilter(product) && matchesStorage(product)) {
                filtered.add(product);
            }
        }

        // Sort
        switch (currentSort) {
            case "name":
                filtered.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            case "newest":
                filtered.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            default: // oldest / expiry date
                filtered.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
                break;
        }

        products.setValue(filtered);

        // Update Empty State
        if (filtered.isEmpty()) {
            if (currentSearch != null && !currentSearch.trim().isEmpty()) {
                emptyState.setValue(EmptyState.EMPTY_SEARCH);
            } else if ("ExpiringSoon".equals(currentFilter)) {
                emptyState.setValue(EmptyState.EMPTY_EXPIRING_SOON);
            } else {
                emptyState.setValue(EmptyState.EMPTY_INVENTORY);
            }
        } else {
            emptyState.setValue(EmptyState.NONE);
        }
    }

    private boolean matchesStorage(Product product) {
        if ("All".equals(currentStorage)) {
            return true;
        }
        String selectedId = LocalDataContract.storageIdForName(currentStorage);
        String productId = LocalDataContract.storageIdForName(product.getStorage());
        return selectedId.equals(productId);
    }

    private boolean matchesFilter(Product product) {
        if ("All".equals(currentFilter)) {
            return true;
        }
        if ("StillGood".equals(currentFilter)) {
            return !product.isExpired();
        }
        if ("ExpiringSoon".equals(currentFilter)) {
            return product.isExpiringSoon(expiringSoonDays);
        }
        return product.isExpired();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
