package com.example.smartexpapp;

import android.content.Context;
import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.data.local.AppDatabase;

public class AppContainer {
    private final AppDatabase database;
    private final ProductRepository productRepository;

    public AppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        this.database = AppDatabase.getInstance(appContext);
        this.productRepository = new ProductRepository(appContext, database);
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public ProductRepository getProductRepository() {
        return productRepository;
    }
}
