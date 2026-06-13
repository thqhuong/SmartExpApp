package com.example.smartexpapp;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartexpapp.data.ProductRepository;

public class InventoryViewModelFactory implements ViewModelProvider.Factory {
    private final ProductRepository productRepository;

    public InventoryViewModelFactory(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(InventoryViewModel.class)) {
            return (T) new InventoryViewModel(productRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
