package com.example.smartexpapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EditProductDialog {
    private final Product product;
    private final Runnable onUpdated;
    private final Runnable onPickPhoto;

    private final Calendar selectedDate = Calendar.getInstance();
    private int selectedStorageId;
    private EditText nameInput;
    private EditText quantityInput;
    private Spinner unitSpinner;
    private Spinner categorySpinner;
    private EditText customCategoryInput;
    private MaterialButton btnManageCategories;
    private String selectedCategory;
    private TextView expiryDateInput;
    private ImageView productPhoto;
    private LinearLayout photoPlaceholder;
    private ImageView editIconOverlay;
    private MaterialButton btnRemovePhoto;
    private BottomSheetDialog dialog;
    private String currentPhotoPath;
    private final Set<String> pendingCategories = new HashSet<>();

    public EditProductDialog(Product product, Runnable onUpdated, Runnable onPickPhoto) {
        this.product = product;
        this.onUpdated = onUpdated;
        this.onPickPhoto = onPickPhoto;
    }

    public void show(BaseActivity activity) {
        dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_product, null);
        dialog.setContentView(view);

        nameInput = view.findViewById(R.id.editName);
        quantityInput = view.findViewById(R.id.editQuantity);
        unitSpinner = view.findViewById(R.id.editUnitSpinner);
        categorySpinner = view.findViewById(R.id.editCategorySpinner);
        customCategoryInput = view.findViewById(R.id.editCustomCategory);
        btnManageCategories = view.findViewById(R.id.editBtnManageCategories);
        expiryDateInput = view.findViewById(R.id.editExpiryDate);
        productPhoto = view.findViewById(R.id.editProductPhoto);
        photoPlaceholder = view.findViewById(R.id.editPhotoPlaceholder);
        editIconOverlay = view.findViewById(R.id.editIconOverlay);
        btnRemovePhoto = view.findViewById(R.id.editBtnRemovePhoto);

        view.findViewById(R.id.editPhotoPreview).setOnClickListener(v -> {
            if (onPickPhoto != null) onPickPhoto.run();
        });
        btnRemovePhoto.setOnClickListener(v -> confirmRemovePhoto(activity));

        nameInput.setText(product.getName());

        setupUnitSpinner(activity);

        quantityInput.setText(product.getQuantity());

        selectedCategory = product.getCategory();
        setupCategorySpinner(activity);

        String imgPath = product.getImageUrl();
        if (imgPath != null && !imgPath.isEmpty()) {
            currentPhotoPath = imgPath;
            showPhoto();
        }

        String storage = product.getStorage();
        if ("Room Temp".equals(storage)) {
            selectStorage(view, R.id.editStorageRoom);
        } else if ("Refrigerator".equals(storage)) {
            selectStorage(view, R.id.editStorageFridge);
        } else if ("Freeze".equals(storage)) {
            selectStorage(view, R.id.editStorageFreezer);
        }

        selectedDate.add(Calendar.DAY_OF_YEAR, product.getDaysUntilExpiry());
        expiryDateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(selectedDate.getTime()));
        expiryDateInput.setTextColor(activity.getColor(R.color.smart_on_surface));

        view.findViewById(R.id.editStorageRoom).setOnClickListener(v -> selectStorage(view, R.id.editStorageRoom));
        view.findViewById(R.id.editStorageFridge).setOnClickListener(v -> selectStorage(view, R.id.editStorageFridge));
        view.findViewById(R.id.editStorageFreezer).setOnClickListener(v -> selectStorage(view, R.id.editStorageFreezer));

        expiryDateInput.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(
                    activity,
                    (dv, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth, 23, 59, 59);
                        expiryDateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(selectedDate.getTime()));
                        expiryDateInput.setTextColor(activity.getColor(R.color.smart_on_surface));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> confirm(activity));

        dialog.show();
    }

    public void setPhotoPath(String path) {
        currentPhotoPath = path;
        showPhoto();
    }

    public void clearPhoto() {
        currentPhotoPath = null;
        productPhoto.setVisibility(View.GONE);
        productPhoto.setImageBitmap(null);
        editIconOverlay.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.GONE);
        photoPlaceholder.setVisibility(View.VISIBLE);
    }

    private void showPhoto() {
        photoPlaceholder.setVisibility(View.GONE);
        productPhoto.setVisibility(View.VISIBLE);
        editIconOverlay.setVisibility(View.VISIBLE);
        btnRemovePhoto.setVisibility(View.VISIBLE);
        ImageLoader.load(productPhoto, currentPhotoPath);
    }

    private void confirmRemovePhoto(BaseActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Remove photo?")
                .setMessage("Are you sure you want to remove this photo?")
                .setPositiveButton("Remove", (dialog, which) -> clearPhoto())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void selectStorage(View root, int storageId) {
        selectedStorageId = storageId;
        setSelected(root, R.id.editStorageRoom, storageId);
        setSelected(root, R.id.editStorageFridge, storageId);
        setSelected(root, R.id.editStorageFreezer, storageId);
    }

    private void setSelected(View root, int viewId, int selectedId) {
        View option = root.findViewById(viewId);
        if (option != null) {
            option.setSelected(viewId == selectedId);
        }
    }

    private void setupUnitSpinner(BaseActivity activity) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity,
                R.array.unit_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);

        String unit = product.getUnit();
        for (int i = 0; i < unitSpinner.getCount(); i++) {
            if (unitSpinner.getItemAtPosition(i).toString().equals(unit)) {
                unitSpinner.setSelection(i);
                break;
            }
        }
    }

    private void setupCategorySpinner(BaseActivity activity) {
        refreshCategorySpinner(activity);
        btnManageCategories.setOnClickListener(v -> showManageCategoriesDialog(activity));
    }

    private void refreshCategorySpinner(BaseActivity activity) {
        List<Product> allProducts = ProductRepository.getProducts(activity);
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
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
    }

    private void showManageCategoriesDialog(BaseActivity activity) {
        List<Product> all = ProductRepository.getProducts(activity);
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

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad16 = dpToPx(activity, 16);
        int pad8 = dpToPx(activity, 8);
        content.setPadding(pad16, pad8, pad16, pad8);

        int[] editColors = {android.R.drawable.ic_menu_edit, R.color.smart_primary_container};
        int[] deleteColors = {android.R.drawable.ic_menu_delete, R.color.smart_error};

        android.app.AlertDialog[] manageDialogHolder = new android.app.AlertDialog[1];

        for (String cat : allCats) {
            boolean builtin = isBuiltIn(cat);
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(activity, 56)));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            View dot = new View(activity);
            int dotSize = dpToPx(activity, 12);
            dot.setLayoutParams(new LinearLayout.LayoutParams(dotSize, dotSize));
            dot.setBackgroundResource(R.drawable.bg_category_dot);
            dot.getBackground().setTint(activity.getColor(CategoryColorHelper.getColor(cat)));

            TextView name = new TextView(activity);
            name.setText(cat);
            name.setTextSize(16f);
            name.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setTextColor(activity.getColor(builtin ? R.color.smart_secondary : R.color.smart_on_surface));
            name.setPadding(dpToPx(activity, 12), 0, 0, 0);

            ImageButton editBtn = new ImageButton(activity);
            int btnSize = dpToPx(activity, 48);
            editBtn.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
            editBtn.setPadding(pad8, pad8, pad8, pad8);
            editBtn.setImageResource(editColors[0]);
            editBtn.setScaleType(ImageView.ScaleType.CENTER);
            editBtn.setColorFilter(activity.getColor(editColors[1]), android.graphics.PorterDuff.Mode.SRC_IN);
            editBtn.setOnClickListener(v -> {
                if (builtin) {
                    Toast.makeText(activity, "\"" + cat + "\" is a built-in category", Toast.LENGTH_SHORT).show();
                } else {
                    manageDialogHolder[0].dismiss();
                    showRenameCategoryDialog(activity, cat, () -> showManageCategoriesDialog(activity));
                }
            });

            ImageButton deleteBtn = new ImageButton(activity);
            deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
            deleteBtn.setPadding(pad8, pad8, pad8, pad8);
            deleteBtn.setImageResource(deleteColors[0]);
            deleteBtn.setScaleType(ImageView.ScaleType.CENTER);
            deleteBtn.setColorFilter(activity.getColor(deleteColors[1]), android.graphics.PorterDuff.Mode.SRC_IN);
            deleteBtn.setOnClickListener(v -> {
                if (builtin) {
                    Toast.makeText(activity, "\"" + cat + "\" is a built-in category", Toast.LENGTH_SHORT).show();
                } else {
                    manageDialogHolder[0].dismiss();
                    showDeleteCategoryDialog(activity, cat, () -> showManageCategoriesDialog(activity));
                }
            });

            row.addView(dot);
            row.addView(name);
            row.addView(editBtn);
            row.addView(deleteBtn);
            content.addView(row);
        }

        TextView addBtn = new TextView(activity);
        addBtn.setText("+ Add new category");
        addBtn.setTextColor(activity.getColor(R.color.smart_primary_container));
        addBtn.setTextSize(16f);
        addBtn.setPadding(pad16, dpToPx(activity, 12), pad16, dpToPx(activity, 12));
        addBtn.setGravity(android.view.Gravity.CENTER);
        addBtn.setOnClickListener(v -> {
            manageDialogHolder[0].dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final EditText input = new EditText(activity);
            input.setHint("Category name");
            input.setPadding(pad16, dpToPx(activity, 8), pad16, dpToPx(activity, 8));
            final boolean[] handled = {false};
            builder.setTitle("Add new category")
                    .setView(input)
                    .setPositiveButton("Add", (d, w) -> {
                        handled[0] = true;
                        String newCat = input.getText().toString().trim();
                        if (!newCat.isEmpty() && !isBuiltIn(newCat)) {
                            if (pendingCategories.contains(newCat) || categoryExistsInProducts(activity, newCat)) {
                                Toast.makeText(activity, "Category \"" + newCat + "\" already exists", Toast.LENGTH_SHORT).show();
                                showManageCategoriesDialog(activity);
                                return;
                            }
                            pendingCategories.add(newCat);
                            selectedCategory = newCat;
                            refreshCategorySpinner(activity);
                            showManageCategoriesDialog(activity);
                        }
                    })
                    .setNegativeButton("Cancel", (d, w) -> {
                        handled[0] = true;
                        showManageCategoriesDialog(activity);
                    })
                    .setOnDismissListener(d -> {
                        if (!handled[0]) showManageCategoriesDialog(activity);
                    })
                    .show();
        });
        content.addView(addBtn);

        manageDialogHolder[0] = new AlertDialog.Builder(activity)
                .setTitle("Manage Categories")
                .setView(content)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showRenameCategoryDialog(BaseActivity activity, String oldName, Runnable onDone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final EditText input = new EditText(activity);
        input.setText(oldName);
        input.setSelection(oldName.length());
        int pad16 = dpToPx(activity, 16);
        input.setPadding(pad16, dpToPx(activity, 8), pad16, dpToPx(activity, 8));
        final boolean[] handled = {false};
        android.app.AlertDialog dialog = builder.setTitle("Rename category")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    handled[0] = true;
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty() || isBuiltIn(newName) || newName.equals(oldName)) {
                        onDone.run();
                        return;
                    }
                    List<Product> all = ProductRepository.getProducts(activity);
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
                            ProductRepository.updateProduct(activity, updated);
                        }
                    }
                    if (pendingCategories.remove(oldName)) pendingCategories.add(newName);
                    if (selectedCategory.equals(oldName)) selectedCategory = newName;
                    refreshCategorySpinner(activity);
                    onDone.run();
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    handled[0] = true;
                    onDone.run();
                })
                .setOnDismissListener(d -> {
                    if (!handled[0]) onDone.run();
                })
                .show();
    }

    private void showDeleteCategoryDialog(BaseActivity activity, String catToDelete, Runnable onDone) {
        int productCount = 0;
        for (Product p : ProductRepository.getProducts(activity)) {
            if (catToDelete.equals(p.getCategory())) productCount++;
        }
        String message = productCount > 0
                ? "Products in this category will be moved to \"General\"."
                : "Are you sure you want to delete this category?";
        final boolean[] handled = {false};
        new AlertDialog.Builder(activity)
                .setTitle("Delete \"" + catToDelete + "\"?")
                .setMessage(message)
                .setPositiveButton("Delete", (d, w) -> {
                    handled[0] = true;
                    pendingCategories.remove(catToDelete);
                    List<Product> all = ProductRepository.getProducts(activity);
                    for (Product p : all) {
                        if (catToDelete.equals(p.getCategory())) {
                            Product updated = new Product(
                                    p.getId(), p.getName(), "General",
                                    p.getQuantity(), p.getUnit(), p.getStorage(),
                                    p.getStorageLocationId(), p.getExpiryDateMillis(),
                                    p.getBarcode(), p.getStatus(), p.getIconRes(),
                                    p.getImageUrl(), p.getCreatedAt(), System.currentTimeMillis(),
                                    p.getCloudId(), p.getOwnerUserId(),
                                    p.getSyncStatus(), p.getLastSyncedAt()
                            );
                            ProductRepository.updateProduct(activity, updated);
                        }
                    }
                    selectedCategory = "General";
                    refreshCategorySpinner(activity);
                    onDone.run();
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    handled[0] = true;
                    onDone.run();
                })
                .setOnDismissListener(d -> {
                    if (!handled[0]) onDone.run();
                })
                .show();
    }

    private int dpToPx(BaseActivity activity, int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density);
    }

    private boolean isBuiltIn(String category) {
        return "Dairy".equals(category) || "General".equals(category)
                || "Meat".equals(category) || "Pantry".equals(category)
                || "Produce".equals(category) || "Vegetables".equals(category);
    }

    private boolean categoryExistsInProducts(BaseActivity activity, String cat) {
        for (Product p : ProductRepository.getProducts(activity)) {
            if (cat.equals(p.getCategory())) return true;
        }
        return false;
    }

    private void confirm(BaseActivity activity) {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(activity, "Enter a product name.", Toast.LENGTH_SHORT).show();
            return;
        }

        String storage;
        if (selectedStorageId == R.id.editStorageFridge) {
            storage = "Refrigerator";
        } else if (selectedStorageId == R.id.editStorageFreezer) {
            storage = "Freeze";
        } else {
            storage = "Room Temp";
        }

        int icon;
        if ("Freeze".equals(storage)) {
            icon = R.drawable.ic_storage_freeze;
        } else if ("Refrigerator".equals(storage)) {
            icon = R.drawable.ic_storage_fridge;
        } else {
            icon = R.drawable.ic_storage_room;
        }

        String quantity = quantityInput.getText().toString().trim();
        if (quantity.isEmpty()) quantity = "1";
        String unit = unitSpinner.getSelectedItem().toString();

        String imageUrl = currentPhotoPath != null ? currentPhotoPath : product.getImageUrl();
        Product updated = new Product(
                product.getId(),
                name,
                selectedCategory.isEmpty() ? "General" : selectedCategory,
                quantity,
                unit,
                storage,
                product.getStorageLocationId(),
                selectedDate.getTimeInMillis(),
                product.getBarcode(),
                product.getStatus(),
                icon,
                imageUrl,
                product.getCreatedAt(),
                System.currentTimeMillis(),
                product.getCloudId(),
                product.getOwnerUserId(),
                product.getSyncStatus(),
                product.getLastSyncedAt()
        );
        ProductRepository.updateProduct(activity, updated);
        Toast.makeText(activity, R.string.product_updated, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        if (onUpdated != null) {
            onUpdated.run();
        }
    }
}
