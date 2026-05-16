package com.example.smartexpapp.data;

import com.example.smartexpapp.model.Product;

import java.util.ArrayList;
import java.util.List;

public final class ProductRepository {
    private static final List<Product> PRODUCTS = new ArrayList<>(SampleData.products());

    private ProductRepository() {
    }

    public static List<Product> getProducts() {
        return new ArrayList<>(PRODUCTS);
    }

    public static void addProduct(Product product) {
        PRODUCTS.add(0, product);
    }
}
