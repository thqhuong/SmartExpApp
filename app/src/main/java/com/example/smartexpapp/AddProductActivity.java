package com.example.smartexpapp;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.ImageLoader;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AddProductActivity extends BaseActivity {
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private final Calendar selectedDate = Calendar.getInstance();
    private boolean hasSelectedDate;
    private int selectedStorageId = R.id.storageRoom;
    private EditText productNameInput;
    private TextView expiryDateInput;
    private MaterialButton submitButton;
    private TextView titleText;
    private TextView subtitleText;
    private ImageView productPhoto;
    private LinearLayout photoPlaceholder;
    private ImageView editIconOverlay;
    private MaterialButton btnRemovePhoto;
    private FrameLayout photoPreview;

    private String editingProductId;
    private String selectedPhotoPath;

    private final ActivityResultLauncher<String> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        setupChrome(R.id.nav_add);

        productNameInput = findViewById(R.id.productNameInput);
        expiryDateInput = findViewById(R.id.expiryDateInput);
        submitButton = findViewById(R.id.addProductButton);
        titleText = findViewById(R.id.addProductTitle);
        subtitleText = findViewById(R.id.addProductSubtitle);
        productPhoto = findViewById(R.id.productPhoto);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);
        editIconOverlay = findViewById(R.id.editIconOverlay);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);
        photoPreview = findViewById(R.id.photoPreview);

        photoPreview.setOnClickListener(v -> pickPhotoLauncher.launch("image/*"));
        btnRemovePhoto.setOnClickListener(v -> confirmRemovePhoto());

        setupStorageOptions();
        expiryDateInput.setOnClickListener(v -> showDatePicker());
        submitButton.setOnClickListener(v -> submitProduct());

        editingProductId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (editingProductId != null) {
            populateForEdit(editingProductId);
        }
    }

    private void onPhotoPicked(Uri uri) {
        if (uri == null) return;
        String path = saveImageToInternalStorage(uri);
        if (path == null) {
            Toast.makeText(this, "Failed to save photo.", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedPhotoPath = path;
        showPhotoPreview(path);
    }

    private void showPhotoPreview(String path) {
        photoPlaceholder.setVisibility(View.GONE);
        productPhoto.setVisibility(View.VISIBLE);
        editIconOverlay.setVisibility(View.VISIBLE);
        btnRemovePhoto.setVisibility(View.VISIBLE);
        ImageLoader.load(productPhoto, path);
    }

    private void clearPhoto() {
        selectedPhotoPath = null;
        productPhoto.setVisibility(View.GONE);
        productPhoto.setImageBitmap(null);
        editIconOverlay.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.GONE);
        photoPlaceholder.setVisibility(View.VISIBLE);
    }

    private void confirmRemovePhoto() {
        new AlertDialog.Builder(this)
                .setTitle("Remove photo?")
                .setMessage("Are you sure you want to remove this photo?")
                .setPositiveButton("Remove", (dialog, which) -> clearPhoto())
                .setNegativeButton("Cancel", null)
                .show();
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

    private void populateForEdit(String productId) {
        Product product = ProductRepository.getProductById(productId);
        if (product == null) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleText.setText(R.string.edit_product);
        subtitleText.setText("Update the product details.");
        submitButton.setText(R.string.edit_product);
        submitButton.setIcon(null);

        productNameInput.setText(product.getName());

        String imgPath = product.getImageUrl();
        if (imgPath != null && !imgPath.isEmpty()) {
            selectedPhotoPath = imgPath;
            showPhotoPreview(imgPath);
        }

        String storage = product.getStorage();
        if ("Room Temp".equals(storage)) {
            selectStorage(R.id.storageRoom);
        } else if ("Refrigerator".equals(storage)) {
            selectStorage(R.id.storageFridge);
        } else if ("Freeze".equals(storage)) {
            selectStorage(R.id.storageFreezer);
        }

        selectedDate.add(Calendar.DAY_OF_YEAR, product.getDaysUntilExpiry());
        hasSelectedDate = true;
        expiryDateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(selectedDate.getTime()));
        expiryDateInput.setTextColor(getColor(R.color.smart_on_surface));
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

        if (editingProductId != null) {
            Product existing = ProductRepository.getProductById(editingProductId);
            if (existing != null) {
                Product updated = new Product(
                        editingProductId, name, existing.getCategory(), existing.getAmount(),
                        storage, days, icon, selectedPhotoPath, existing.getCreatedAt()
                );
                ProductRepository.updateProduct(updated);
                Toast.makeText(this, R.string.product_updated, Toast.LENGTH_SHORT).show();
            }
        } else {
            ProductRepository.addProduct(new Product(name, "General", "1 pcs", storage, days, icon, selectedPhotoPath));
            Toast.makeText(this, name + " added.", Toast.LENGTH_SHORT).show();
        }

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
