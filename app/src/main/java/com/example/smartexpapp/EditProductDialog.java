package com.example.smartexpapp;

import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

    private final Calendar selectedDate = Calendar.getInstance();
    private int selectedStorageId;
    private EditText nameInput;
    private EditText quantityInput;
    private Spinner unitSpinner;
    private Spinner categorySpinner;
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

    public EditProductDialog(Product product, Runnable onUpdated) {
        this.product = product;
        this.onUpdated = onUpdated;
    }

    public void show(BaseActivity activity) {
        dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_product, null);
        dialog.setContentView(view);

        nameInput = view.findViewById(R.id.editName);
        quantityInput = view.findViewById(R.id.editQuantity);
        unitSpinner = view.findViewById(R.id.editUnitSpinner);
        categorySpinner = view.findViewById(R.id.editCategorySpinner);
        btnManageCategories = view.findViewById(R.id.editBtnManageCategories);
        expiryDateInput = view.findViewById(R.id.editExpiryDate);
        productPhoto = view.findViewById(R.id.editProductPhoto);
        photoPlaceholder = view.findViewById(R.id.editPhotoPlaceholder);
        editIconOverlay = view.findViewById(R.id.editIconOverlay);
        btnRemovePhoto = view.findViewById(R.id.editBtnRemovePhoto);

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
            selectStorage(R.id.editStorageRoom);
        } else if ("Refrigerator".equals(storage)) {
            selectStorage(R.id.editStorageFridge);
        } else if ("Freeze".equals(storage)) {
            selectStorage(R.id.editStorageFreezer);
        }

        selectedDate.setTimeInMillis(product.getExpiryDateMillis());
        expiryDateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(selectedDate.getTime()));
        expiryDateInput.setTextColor(activity.getColor(R.color.smart_on_surface));

        view.findViewById(R.id.editPhotoPreview).setOnClickListener(v -> {
            Toast.makeText(activity, R.string.edit_photo_in_add_screen, Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.editStorageRoom).setOnClickListener(v -> selectStorage(R.id.editStorageRoom));
        view.findViewById(R.id.editStorageFridge).setOnClickListener(v -> selectStorage(R.id.editStorageFridge));
        view.findViewById(R.id.editStorageFreezer).setOnClickListener(v -> selectStorage(R.id.editStorageFreezer));

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

        btnRemovePhoto.setOnClickListener(v -> clearPhoto());

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> confirm(activity));

        dialog.show();
    }

    private void showPhoto() {
        photoPlaceholder.setVisibility(View.GONE);
        productPhoto.setVisibility(View.VISIBLE);
        editIconOverlay.setVisibility(View.VISIBLE);
        btnRemovePhoto.setVisibility(View.VISIBLE);
        ImageLoader.load(productPhoto, currentPhotoPath);
    }

    private void clearPhoto() {
        currentPhotoPath = null;
        productPhoto.setVisibility(View.GONE);
        productPhoto.setImageBitmap(null);
        editIconOverlay.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.GONE);
        photoPlaceholder.setVisibility(View.VISIBLE);
    }

    private void selectStorage(int storageId) {
        selectedStorageId = storageId;
        setSelected(R.id.editStorageRoom, storageId);
        setSelected(R.id.editStorageFridge, storageId);
        setSelected(R.id.editStorageFreezer, storageId);
    }

    private void setSelected(int viewId, int selectedId) {
        if (dialog == null) return;
        View option = dialog.findViewById(viewId);
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
        items.add(activity.getString(R.string.cat_dairy));
        items.add(activity.getString(R.string.cat_general));
        items.add(activity.getString(R.string.cat_meat));
        items.add(activity.getString(R.string.cat_pantry));
        items.add(activity.getString(R.string.cat_produce));
        items.add(activity.getString(R.string.cat_vegetables));
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
        allCats.add(activity.getString(R.string.cat_dairy));
        allCats.add(activity.getString(R.string.cat_general));
        allCats.add(activity.getString(R.string.cat_meat));
        allCats.add(activity.getString(R.string.cat_pantry));
        allCats.add(activity.getString(R.string.cat_produce));
        allCats.add(activity.getString(R.string.cat_vegetables));
        allCats.addAll(customSet);
        Collections.sort(allCats);

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_manage_categories, null);
        LinearLayout categoryList = content.findViewById(R.id.categoryList);
        MaterialButton btnAdd = content.findViewById(R.id.btnAddCategory);

        LayoutInflater inflater = LayoutInflater.from(activity);
        for (String cat : allCats) {
            boolean builtin = isBuiltIn(cat);
            View row = inflater.inflate(R.layout.item_manage_category, categoryList, false);

            View dot = row.findViewById(R.id.categoryDot);
            dot.getBackground().setTint(activity.getColor(CategoryColorHelper.getColor(cat)));

            TextView name = row.findViewById(R.id.categoryName);
            name.setText(cat);
            name.setTextColor(activity.getColor(builtin ? R.color.smart_secondary : R.color.smart_on_surface));

            MaterialButton editBtn = row.findViewById(R.id.btnEditCategory);
            MaterialButton deleteBtn = row.findViewById(R.id.btnDeleteCategory);

            if (builtin) {
                editBtn.setVisibility(View.GONE);
                deleteBtn.setVisibility(View.GONE);
            } else {
                editBtn.setVisibility(View.VISIBLE);
                deleteBtn.setVisibility(View.VISIBLE);
                editBtn.setOnClickListener(v -> {
                    showRenameCategoryDialog(activity, cat, () -> showManageCategoriesDialog(activity));
                });
                deleteBtn.setOnClickListener(v -> {
                    showDeleteCategoryDialog(activity, cat, () -> showManageCategoriesDialog(activity));
                });
            }

            categoryList.addView(row);
        }

        btnAdd.setOnClickListener(v -> {
            View inputLayout = inflater.inflate(R.layout.dialog_edit_text, null);
            EditText input = inputLayout.findViewById(android.R.id.edit);
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.category_add_title)
                    .setView(inputLayout)
                    .setPositiveButton(R.string.add_label, (d, w) -> {
                        String newCat = input.getText().toString().trim();
                        if (!newCat.isEmpty() && !isBuiltIn(newCat)) {
                            if (pendingCategories.contains(newCat) || categoryExistsInProducts(activity, newCat)) {
                                Toast.makeText(activity, activity.getString(R.string.category_already_exists_format, newCat), Toast.LENGTH_SHORT).show();
                                showManageCategoriesDialog(activity);
                                return;
                            }
                            pendingCategories.add(newCat);
                            selectedCategory = newCat;
                            refreshCategorySpinner(activity);
                            showManageCategoriesDialog(activity);
                        }
                    })
                    .setNegativeButton(R.string.cancel, (d, w) -> showManageCategoriesDialog(activity))
                    .setOnDismissListener(d -> showManageCategoriesDialog(activity))
                    .show();
        });

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.manage_categories_title)
                .setView(content)
                .setPositiveButton(R.string.close_label, null)
                .show();
    }

    private void showRenameCategoryDialog(BaseActivity activity, String oldName, Runnable onDone) {
        View inputLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null);
        EditText input = inputLayout.findViewById(android.R.id.edit);
        input.setText(oldName);
        input.setSelection(oldName.length());
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.rename_category_title)
                .setView(inputLayout)
                .setPositiveButton(R.string.rename_label, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty() || isBuiltIn(newName) || newName.equals(oldName)) {
                        onDone.run();
                        return;
                    }
                    List<Product> all = ProductRepository.getProducts(activity);
                    for (Product p : all) {
                        if (oldName.equals(p.getCategory())) {
                            Product updated = copyWithCategory(p, newName);
                            ProductRepository.updateProduct(activity, updated);
                        }
                    }
                    if (pendingCategories.remove(oldName)) pendingCategories.add(newName);
                    if (selectedCategory.equals(oldName)) selectedCategory = newName;
                    refreshCategorySpinner(activity);
                    onDone.run();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> onDone.run())
                .setOnDismissListener(d -> onDone.run())
                .show();
    }

    private void showDeleteCategoryDialog(BaseActivity activity, String catToDelete, Runnable onDone) {
        int productCount = 0;
        for (Product p : ProductRepository.getProducts(activity)) {
            if (catToDelete.equals(p.getCategory())) productCount++;
        }
        String message = productCount > 0
                ? activity.getString(R.string.category_delete_message_with_products)
                : activity.getString(R.string.category_delete_message_empty);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.delete_category_title_format, catToDelete))
                .setMessage(message)
                .setPositiveButton(R.string.delete_confirm, (d, w) -> {
                    pendingCategories.remove(catToDelete);
                    List<Product> all = ProductRepository.getProducts(activity);
                    for (Product p : all) {
                        if (catToDelete.equals(p.getCategory())) {
                            Product updated = copyWithCategory(p, activity.getString(R.string.cat_general));
                            ProductRepository.updateProduct(activity, updated);
                        }
                    }
                    selectedCategory = activity.getString(R.string.cat_general);
                    refreshCategorySpinner(activity);
                    onDone.run();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> onDone.run())
                .setOnDismissListener(d -> onDone.run())
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

    private Product copyWithCategory(Product product, String category) {
        return new Product(
                product.getId(), product.getName(), category,
                product.getQuantity(), product.getUnit(), product.getStorage(),
                product.getStorageLocationId(), product.getExpiryDateMillis(),
                product.getBarcode(), product.getStatus(), product.getIconRes(),
                product.getImageUrl(), product.getCreatedAt(), System.currentTimeMillis(),
                product.getCloudId(), product.getOwnerUserId(),
                product.getSyncStatus(), product.getLastSyncedAt()
        );
    }

    private void confirm(BaseActivity activity) {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(activity, R.string.enter_product_name, Toast.LENGTH_SHORT).show();
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
                selectedCategory.isEmpty() ? activity.getString(R.string.cat_general) : selectedCategory,
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
