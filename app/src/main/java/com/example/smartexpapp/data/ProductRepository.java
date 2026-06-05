package com.example.smartexpapp.data;

import com.example.smartexpapp.model.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProductRepository {
    private static final Map<String, Product> PRODUCTS = new LinkedHashMap<>();

    static {
        for (Product p : SampleData.products()) {
            PRODUCTS.put(p.getId(), p);
        }
    }

    private ProductRepository() {
    }

    public static List<Product> getProducts() {
        return new ArrayList<>(PRODUCTS.values());
    }

    public static Product getProductById(String id) {
        return PRODUCTS.get(id);
    }

    public static void addProduct(Product product) {
        PRODUCTS.put(product.getId(), product);
    }

    public static void updateProduct(Product product) {
        PRODUCTS.put(product.getId(), product);
    }

    public static void deleteProduct(String id) {
        PRODUCTS.remove(id);
    }

    public static List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getProducts();
        }
        String lower = query.toLowerCase().trim();
        List<Product> result = new ArrayList<>();
        for (Product p : PRODUCTS.values()) {
            if (p.getName().toLowerCase().contains(lower)) {
                result.add(p);
            }
        }
        return result;
    }
}
