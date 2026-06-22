package com.example.smartexpapp.model;

public class ProductDraft {
    private final String name;
    private final String category;
    private final String quantity;
    private final String unit;
    private final String storage;
    private final long expiryDateMillis;
    private final String sourceText;
    private final boolean hasExpiryDate;

    public ProductDraft(String name, String category, String quantity, String unit,
                        String storage, long expiryDateMillis, String sourceText,
                        boolean hasExpiryDate) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.storage = storage;
        this.expiryDateMillis = expiryDateMillis;
        this.sourceText = sourceText;
        this.hasExpiryDate = hasExpiryDate;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getStorage() {
        return storage;
    }

    public long getExpiryDateMillis() {
        return expiryDateMillis;
    }

    public String getSourceText() {
        return sourceText;
    }

    public boolean hasExpiryDate() {
        return hasExpiryDate;
    }
}
