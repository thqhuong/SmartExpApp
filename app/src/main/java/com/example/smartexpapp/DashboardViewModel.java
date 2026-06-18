package com.example.smartexpapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardViewModel extends ViewModel {
    private final ProductRepository productRepository;
    private final MutableLiveData<DashboardState> uiState = new MutableLiveData<>(DashboardState.loading());

    public DashboardViewModel(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public LiveData<DashboardState> getUiState() {
        return uiState;
    }

    public void loadDashboard() {
        uiState.setValue(DashboardState.loading());

        productRepository.getDashboardSnapshotAsync(snapshot -> {
            List<Product> products = snapshot.getActiveProducts();
            DashboardState state = new DashboardState(
                    snapshot.getUrgentCount(),
                    snapshot.getTotalTracked(),
                    snapshot.getWastePreventedCount(),
                    buildStorageSummaries(products),
                    groupProducts(products),
                    false,
                    false
            );
            uiState.postValue(state);
        }, error -> {
            uiState.postValue(DashboardState.error());
        });
    }

    static List<DashboardState.StorageSummaryEntry> buildStorageSummaries(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Refrigerator", 0);
        counts.put("Room Temp", 0);
        counts.put("Freeze", 0);

        for (Product product : products) {
            String storage = product.getStorage();
            if (counts.containsKey(storage)) {
                counts.put(storage, counts.get(storage) + 1);
            }
        }

        int total = products.size();
        List<DashboardState.StorageSummaryEntry> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int count = entry.getValue();
            int progress = Math.round(count / (float) total * 100f);
            result.add(new DashboardState.StorageSummaryEntry(entry.getKey(), count, progress));
        }
        return result;
    }

    static Map<String, List<Product>> groupProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<Product>> groups = new LinkedHashMap<>();
        groups.put("Expired", new ArrayList<>());
        groups.put("Urgent", new ArrayList<>());
        groups.put("Soon", new ArrayList<>());
        groups.put("Safe", new ArrayList<>());

        for (Product product : products) {
            String group = product.getGroup();
            List<Product> list = groups.get(group);
            if (list != null) {
                list.add(product);
            }
        }
        return groups;
    }
}
