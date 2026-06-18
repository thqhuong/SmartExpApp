package com.example.smartexpapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductHistoryViewModel extends ViewModel {
    private final ProductRepository productRepository;
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> consumedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> wastedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> donatedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private String currentFilter = "All";

    public ProductHistoryViewModel(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<Integer> getConsumedCount() { return consumedCount; }
    public LiveData<Integer> getWastedCount() { return wastedCount; }
    public LiveData<Integer> getDonatedCount() { return donatedCount; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadHistory() {
        isLoading.setValue(true);
        productRepository.getAllProductsAsync(allProducts -> {
            List<Product> inactive = new ArrayList<>();
            int consumed = 0, wasted = 0, donated = 0;
            for (Product p : allProducts) {
                String status = p.getStatus();
                if (Product.SYNC_STATUS_LOCAL.equals(status)) continue;
                if ("CONSUMED".equals(status)) {
                    inactive.add(p);
                    consumed++;
                } else if ("WASTED".equals(status)) {
                    inactive.add(p);
                    wasted++;
                } else if ("DONATED".equals(status)) {
                    inactive.add(p);
                    donated++;
                }
            }
            consumedCount.setValue(consumed);
            wastedCount.setValue(wasted);
            donatedCount.setValue(donated);
            applyFilter(inactive);
            isLoading.setValue(false);
        }, e -> isLoading.setValue(false));
    }

    public void setFilter(String filter) {
        currentFilter = filter;
        loadHistory();
    }

    public void restoreProduct(String id, ProductRepository.Callback<Boolean> callback, ProductRepository.ErrorCallback errorCallback) {
        productRepository.markActiveAsync(id, "Restored from history", callback, errorCallback);
    }

    private void applyFilter(List<Product> all) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : all) {
            String status = p.getStatus();
            if ("All".equals(currentFilter)) {
                filtered.add(p);
            } else if ("CONSUMED".equals(currentFilter) && "CONSUMED".equals(status)) {
                filtered.add(p);
            } else if ("WASTED".equals(currentFilter) && "WASTED".equals(status)) {
                filtered.add(p);
            } else if ("DONATED".equals(currentFilter) && "DONATED".equals(status)) {
                filtered.add(p);
            }
        }
        products.setValue(filtered);
    }
}
