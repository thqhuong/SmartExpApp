package com.example.smartexpapp;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartexpapp.data.ProductRepository;

public class DashboardViewModelFactory implements ViewModelProvider.Factory {
    private final ProductRepository productRepository;

    public DashboardViewModelFactory(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DashboardViewModel.class)) {
            return (T) new DashboardViewModel(productRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
