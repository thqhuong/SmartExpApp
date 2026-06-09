package com.example.smartexpapp;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddProductActivity extends BaseActivity {
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private final Calendar selectedDate = Calendar.getInstance();
    private boolean hasSelectedDate;
    private int selectedStorageId = R.id.storageRoom;
    private EditText productNameInput;
    private EditText quantityInput;
    private Spinner unitSpinner;
    private Spinner categorySpinner;
    private EditText customCategoryInput;
    private MaterialButton btnManageCategories;
    private String selectedCategory = "General";
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
    private final Set<String> pendingCategories = new HashSet<>();

    private final ActivityResultLauncher<String> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        setupChrome(R.id.nav_add);

        productNameInput = findViewById(R.id.productNameInput);
        quantityInput = findViewById(R.id.quantityInput);
        unitSpinner = findViewById(R.id.unitSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);
        customCategoryInput = findViewById(R.id.customCategoryInput);
        btnManageCategories = findViewById(R.id.btnManageCategories);
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

        setupUnitSpinner();
        setupCategorySpinner();
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

    private void setupUnitSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.unit_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);

        if (editingProductId != null) {
            Product product = ProductRepository.getProductById(this, editingProductId);
            if (product != null) {
                String unit = product.getUnit();
                for (int i = 0; i < unitSpinner.getCount(); i++) {
                    if (unitSpinner.getItemAtPosition(i).toString().equals(unit)) {
                        unitSpinner.setSelection(i);
                        break;
                    }
                }
                quantityInput.setText(product.getQuantity());
            }
        } else {
            quantityInput.setText("1");
        }
    }

    private void setupCategorySpinner() {
        refreshCategorySpinner();
        btnManageCategories.setOnClickListener(v -> showManageCategoriesDialog());
    }

    private void refreshCategorySpinner() {
        List<Product> allProducts = ProductRepository.getProducts(this);
        Set<String> existingCustom = new HashSet<>();
        existingCustom.addAll(pendingCategories);
        for (Product p : allProducts) {
            String cat = p.getCategory();
            if (cat != null && !isBuiltIn(cat) && !cat.trim().isEmpty()) {
                existingCustom.add(cat);
            }
        }

        List<String> items = new ArrayList<>();
        items.add("Dairy"); items.add("General"); items.add("Meat");
        items.add("Pantry"); items.add("Produce"); items.add("Vegetables");
        items.addAll(existingCustom);
        Collections.sort(items);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        int pos = items.indexOf(selectedCategory);
        if (pos >= 0) categorySpinner.setSelection(pos);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (editingProductId != null) {
            Product product = ProductRepository.getProductById(this, editingProductId);
            if (product != null) {
                selectedCategory = product.getCategory();
                int p = items.indexOf(selectedCategory);
                if (p >= 0) {
                    categorySpinner.setSelection(p);
                }
            }
        }
    }

    private void showManageCategoriesDialog() {
        List<Product> all = ProductRepository.getProducts(this);
        Set<String> customSet = new HashSet<>();
        customSet.addAll(pendingCategories);
        for (Product p : all) {
            String cat = p.getCategory();
            if (cat != null && !isBuiltIn(cat) && !cat.trim().isEmpty()) {
                customSet.add(cat);
            }
        }

        List<String> allCats = new ArrayList<>();
        allCats.add("Dairy"); allCats.add("General"); allCats.add("Meat");
        allCats.add("Pantry"); allCats.add("Produce"); allCats.add("Vegetables");
        allCats.addAll(customSet);
        Collections.sort(allCats);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad16 = dpToPx(16);
        int pad8 = dpToPx(8);
        content.setPadding(pad16, pad8, pad16, pad8);

        int[] editColors = {android.R.drawable.ic_menu_edit, R.color.smart_primary_container};
        int[] deleteColors = {android.R.drawable.ic_menu_delete, R.color.smart_error};

        android.app.AlertDialog[] manageDialogHolder = new android.app.AlertDialog[1];

        for (String cat : allCats) {
            boolean builtin = isBuiltIn(cat);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(56)));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            View dot = new View(this);
            int dotSize = dpToPx(12);
            dot.setLayoutParams(new LinearLayout.LayoutParams(dotSize, dotSize));
            dot.setBackgroundResource(R.drawable.bg_category_dot);
            dot.getBackground().setTint(getColor(CategoryColorHelper.getColor(cat)));

            TextView name = new TextView(this);
            name.setText(cat);
            name.setTextSize(16f);
            name.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setTextColor(getColor(builtin ? R.color.smart_secondary : R.color.smart_on_surface));
            name.setPadding(dpToPx(12), 0, 0, 0);

            row.addView(dot);
            row.addView(name);

            if (!builtin) {
                ImageButton editBtn = new ImageButton(this);
                int btnSize = dpToPx(48);
                editBtn.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
                editBtn.setPadding(pad8, pad8, pad8, pad8);
                editBtn.setImageResource(editColors[0]);
                editBtn.setScaleType(ImageView.ScaleType.CENTER);
                editBtn.setColorFilter(getColor(editColors[1]), android.graphics.PorterDuff.Mode.SRC_IN);
                editBtn.setOnClickListener(v -> {
                    manageDialogHolder[0].dismiss();
                    showRenameCategoryDialog(cat, () -> showManageCategoriesDialog());
                });

                ImageButton deleteBtn = new ImageButton(this);
                deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
                deleteBtn.setPadding(pad8, pad8, pad8, pad8);
                deleteBtn.setImageResource(deleteColors[0]);
                deleteBtn.setScaleType(ImageView.ScaleType.CENTER);
                deleteBtn.setColorFilter(getColor(deleteColors[1]), android.graphics.PorterDuff.Mode.SRC_IN);
                deleteBtn.setOnClickListener(v -> {
                    manageDialogHolder[0].dismiss();
                    showDeleteCategoryDialog(cat, () -> showManageCategoriesDialog());
                });

                row.addView(editBtn);
                row.addView(deleteBtn);
            }
            content.addView(row);
        }

        TextView addBtn = new TextView(this);
        addBtn.setText("+ Add new category");
        addBtn.setTextColor(getColor(R.color.smart_primary_container));
        addBtn.setTextSize(16f);
        addBtn.setPadding(pad16, dpToPx(12), pad16, dpToPx(12));
        addBtn.setGravity(android.view.Gravity.CENTER);
        addBtn.setOnClickListener(v -> {
            manageDialogHolder[0].dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final EditText input = new EditText(this);
            input.setHint("Category name");
            input.setPadding(pad16, dpToPx(8), pad16, dpToPx(8));
            builder.setTitle("Add new category")
                    .setView(input)
                    .setPositiveButton("Add", (d, w) -> {
                        String newCat = input.getText().toString().trim();
                        if (!newCat.isEmpty() && !isBuiltIn(newCat)) {
                            pendingCategories.add(newCat);
                            selectedCategory = newCat;
                            refreshCategorySpinner();
                            showManageCategoriesDialog();
                        }
                    })
                    .setNegativeButton("Cancel", (d, w) -> showManageCategoriesDialog())
                    .show();
        });
        content.addView(addBtn);

        manageDialogHolder[0] = new AlertDialog.Builder(this)
                .setTitle("Manage Categories")
                .setView(content)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showRenameCategoryDialog(String oldName, Runnable onDone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setText(oldName);
        input.setSelection(oldName.length());
        int pad16 = dpToPx(16);
        input.setPadding(pad16, dpToPx(8), pad16, dpToPx(8));
        builder.setTitle("Rename category")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty() || isBuiltIn(newName) || newName.equals(oldName)) {
                        onDone.run();
                        return;
                    }
                    List<Product> all = ProductRepository.getProducts(this);
                    for (Product p : all) {
                        if (oldName.equals(p.getCategory())) {
                            Product updated = new Product(
                                    p.getId(), p.getName(), newName,
                                    p.getQuantity(), p.getUnit(), p.getStorage(),
                                    p.getStorageLocationId(), p.getExpiryDateMillis(),
                                    p.getBarcode(), p.getStatus(), p.getIconRes(),
                                    p.getImageUrl(), p.getCreatedAt(), System.currentTimeMillis(),
                                    p.getCloudId(), p.getOwnerUserId(),
                                    p.getSyncStatus(), p.getLastSyncedAt()
                            );
                            ProductRepository.updateProduct(this, updated);
                        }
                    }
                    if (selectedCategory.equals(oldName)) selectedCategory = newName;
                    refreshCategorySpinner();
                    onDone.run();
                })
                .setNegativeButton("Cancel", (d, w) -> onDone.run())
                .setOnDismissListener(d -> onDone.run())
                .show();
    }

    private void showDeleteCategoryDialog(String catToDelete, Runnable onDone) {
        new AlertDialog.Builder(this)
                .setTitle("Delete \"" + catToDelete + "\"?")
                .setMessage("Products in this category will be moved to \"General\".")
                .setPositiveButton("Delete", (d, w) -> {
                    pendingCategories.remove(catToDelete);
                    List<Product> all = ProductRepository.getProducts(this);
                    for (Product p : all) {
                        if (catToDelete.equals(p.getCategory()) && !p.getId().equals(editingProductId)) {
                            Product updated = new Product(
                                    p.getId(), p.getName(), "General",
                                    p.getQuantity(), p.getUnit(), p.getStorage(),
                                    p.getStorageLocationId(), p.getExpiryDateMillis(),
                                    p.getBarcode(), p.getStatus(), p.getIconRes(),
                                    p.getImageUrl(), p.getCreatedAt(), System.currentTimeMillis(),
                                    p.getCloudId(), p.getOwnerUserId(),
                                    p.getSyncStatus(), p.getLastSyncedAt()
                            );
                            ProductRepository.updateProduct(this, updated);
                        }
                    }
                    selectedCategory = "General";
                    refreshCategorySpinner();
                    onDone.run();
                })
                .setNegativeButton("Cancel", (d, w) -> onDone.run())
                .setOnDismissListener(d -> onDone.run())
                .show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private boolean isBuiltIn(String category) {
        return "Dairy".equals(category) || "General".equals(category)
                || "Meat".equals(category) || "Pantry".equals(category)
                || "Produce".equals(category) || "Vegetables".equals(category);
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
                    selectedDate.set(Calendar.MILLISECOND, 999);
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
        Product product = ProductRepository.getProductById(this, productId);
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

        selectedCategory = product.getCategory();

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

        selectedDate.setTimeInMillis(product.getExpiryDateMillis());
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
        int icon = iconForStorage(storage);

        String category = selectedCategory.isEmpty() ? "General" : selectedCategory;

        String quantity = quantityInput.getText().toString().trim();
        if (quantity.isEmpty()) quantity = "1";
        String unit = unitSpinner.getSelectedItem().toString();

        if (editingProductId != null) {
            Product existing = ProductRepository.getProductById(this, editingProductId);
            if (existing != null) {
                Product updated = new Product(
                        editingProductId,
                        name,
                        category,
                        quantity,
                        unit,
                        storage,
                        null,
                        selectedDate.getTimeInMillis(),
                        existing.getBarcode(),
                        existing.getStatus(),
                        icon,
                        selectedPhotoPath,
                        existing.getCreatedAt(),
                        System.currentTimeMillis(),
                        existing.getCloudId(),
                        existing.getOwnerUserId(),
                        existing.getSyncStatus(),
                        existing.getLastSyncedAt()
                );
                ProductRepository.updateProduct(this, updated);
                Toast.makeText(this, R.string.product_updated, Toast.LENGTH_SHORT).show();
            }
        } else {
            ProductRepository.addProduct(this, new Product(name, category, quantity, unit, storage, selectedDate.getTimeInMillis(), icon, selectedPhotoPath));
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
