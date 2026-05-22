package com.example.smartexpapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AddProductActivity extends BaseActivity {
    private final Calendar selectedDate = Calendar.getInstance();
    private boolean hasSelectedDate;
    private int selectedStorageId = R.id.storageRoom;
    private EditText productNameInput;
    private TextView expiryDateInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        setupChrome(R.id.nav_add);

        productNameInput = findViewById(R.id.productNameInput);
        expiryDateInput = findViewById(R.id.expiryDateInput);
        setupStorageOptions();

        expiryDateInput.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.addProductButton).setOnClickListener(v -> submitProduct());
    }

    private void setupStorageOptions() {
        findViewById(R.id.storageRoom).setOnClickListener(v -> selectStorage(R.id.storageRoom));
        findViewById(R.id.storageFridge).setOnClickListener(v -> selectStorage(R.id.storageFridge));
        findViewById(R.id.storageFreezer).setOnClickListener(v -> selectStorage(R.id.storageFreezer));
        selectStorage(R.id.storageRoom);
    }

    private void selectStorage(int storageId) {
        selectedStorageId = storageId;
        setStorageSelected(R.id.storageRoom, storageId);
        setStorageSelected(R.id.storageFridge, storageId);
        setStorageSelected(R.id.storageFreezer, storageId);
    }

    private void setStorageSelected(int viewId, int selectedId) {
        View option = findViewById(viewId);
        if (option != null) {
            option.setSelected(viewId == selectedId);
        }
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth, 23, 59, 59);
                    hasSelectedDate = true;
                    expiryDateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(selectedDate.getTime()));
                    expiryDateInput.setTextColor(getColor(R.color.smart_on_surface));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void submitProduct() {
        String name = productNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a product name.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasSelectedDate) {
            Toast.makeText(this, "Select an expiry date.", Toast.LENGTH_SHORT).show();
            return;
        }

        String storage = selectedStorage();
        int days = daysUntilExpiry();
        int icon = iconForStorage(storage);

        // Future OCR can replace this manual field mapping without changing the screen flow.
        ProductRepository.addProduct(new Product(name, "General", "1 pcs", storage, days, icon));
        Toast.makeText(this, name + " added.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, InventoryActivity.class));
        overridePendingTransition(0, 0);
    }

    private String selectedStorage() {
        if (selectedStorageId == R.id.storageFridge) {
            return "Refrigerator";
        }
        if (selectedStorageId == R.id.storageFreezer) {
            return "Freeze";
        }
        return "Room Temp";
    }

    private int daysUntilExpiry() {
        long diff = selectedDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        return Math.max(0, (int) Math.ceil(diff / (double) TimeUnit.DAYS.toMillis(1)));
    }

    private int iconForStorage(String storage) {
        if ("Freeze".equals(storage)) {
            return R.drawable.ic_storage_freeze;
        }
        if ("Refrigerator".equals(storage)) {
            return R.drawable.ic_storage_fridge;
        }
        return R.drawable.ic_storage_room;
    }
}
