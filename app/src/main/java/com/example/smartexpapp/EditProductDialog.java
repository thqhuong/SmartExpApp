package com.example.smartexpapp;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartexpapp.data.CategoryRepository;
import com.example.smartexpapp.data.LocalImageRepository;
import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ProductQuantityValidator;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EditProductDialog {
    private final Product product;
    private final Runnable onUpdated;
    private final List<Product> productsSnapshot;

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
    private android.app.Dialog manageDialog;
    private String currentPhotoPath;
    private String originalPhotoPath;

    public EditProductDialog(Product product, Runnable onUpdated) {
        this(product, onUpdated, Collections.emptyList());
    }

    public EditProductDialog(Product product, Runnable onUpdated, List<Product> productsSnapshot) {
        this.product = product;
        this.onUpdated = onUpdated;
        this.productsSnapshot = productsSnapshot == null ? Collections.emptyList() : new ArrayList<>(productsSnapshot);
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
        originalPhotoPath = imgPath;
        if (imgPath != null && !imgPath.isEmpty()) {
            currentPhotoPath = imgPath;
            showPhoto();
        }

        String storageId = LocalDataContract.storageIdForName(product.getStorage());
        if (LocalDataContract.STORAGE_ROOM_TEMP_ID.equals(storageId)) {
            selectStorage(R.id.editStorageRoom);
        } else if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageId)) {
            selectStorage(R.id.editStorageFridge);
        } else if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageId)) {
            selectStorage(R.id.editStorageFreezer);
        }

        selectedDate.setTimeInMillis(product.getExpiryDateMillis());
        expiryDateInput.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.getTime()));
        expiryDateInput.setTextColor(activity.getColor(R.color.smart_on_surface));

        view.findViewById(R.id.editPhotoPreview).setOnClickListener(v ->
                activity.pickProductPhoto(uri -> onPhotoPicked(activity, uri)));

        view.findViewById(R.id.editStorageRoom).setOnClickListener(v -> selectStorage(R.id.editStorageRoom));
        view.findViewById(R.id.editStorageFridge).setOnClickListener(v -> selectStorage(R.id.editStorageFridge));
        view.findViewById(R.id.editStorageFreezer).setOnClickListener(v -> selectStorage(R.id.editStorageFreezer));

        expiryDateInput.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(
                    activity,
                    (dv, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth, 23, 59, 59);
                        expiryDateInput.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.getTime()));
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

        View scroll = view.findViewById(R.id.editScroll);
        View buttonBar = view.findViewById(R.id.editButtonBar);
        int scrollPadTop = scroll.getPaddingTop();
        int buttonPadBottom = buttonBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (insetView, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            scroll.setPadding(scroll.getPaddingLeft(), scrollPadTop + bars.top,
                    scroll.getPaddingRight(), scroll.getPaddingBottom());
            buttonBar.setPadding(buttonBar.getPaddingLeft(), buttonBar.getPaddingTop(),
                    buttonBar.getPaddingRight(), buttonPadBottom + bars.bottom);
            return windowInsets;
        });
        dialog.setOnShowListener(d -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                ViewGroup.LayoutParams params = sheet.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                sheet.setLayoutParams(params);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

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

    private void onPhotoPicked(BaseActivity activity, Uri uri) {
        if (uri == null) {
            return;
        }
        String path = activity.saveImageToInternalStorage(uri);
        if (path == null) {
            Toast.makeText(activity, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentPhotoPath != null && !currentPhotoPath.equals(originalPhotoPath)) {
            LocalImageRepository.deleteProductImage(activity.getApplicationContext(), currentPhotoPath);
        }
        currentPhotoPath = path;
        showPhoto();
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
        CategoryRepository.getDisplayCategoriesAsync(activity,
                items -> bindCategorySpinner(activity, items),
                error -> Toast.makeText(activity, R.string.category_load_error, Toast.LENGTH_SHORT).show());
    }

    private void bindCategorySpinner(BaseActivity activity, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        String canonicalSelected = canonicalCategory(activity, selectedCategory);
        int pos = -1;
        for (int i = 0; i < items.size(); i++) {
            if (canonicalCategory(activity, items.get(i)).equals(canonicalSelected)) {
                pos = i;
                break;
            }
        }
        if (pos >= 0) categorySpinner.setSelection(pos);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String display = parent.getItemAtPosition(position).toString();
                selectedCategory = canonicalCategory(activity, display);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showManageCategoriesDialog(BaseActivity activity) {
        CategoryRepository.getDisplayCategoriesAsync(activity,
                allCats -> showManageCategoriesDialog(activity, allCats),
                error -> Toast.makeText(activity, R.string.category_load_error, Toast.LENGTH_SHORT).show());
    }

    private void showManageCategoriesDialog(BaseActivity activity, List<String> allCats) {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_manage_categories, null);
        LinearLayout categoryList = content.findViewById(R.id.categoryList);
        MaterialButton btnAdd = content.findViewById(R.id.btnAddCategory);

        LayoutInflater inflater = LayoutInflater.from(activity);
        for (String cat : allCats) {
            String canonicalCat = canonicalCategory(activity, cat);
            boolean builtin = CategoryColorHelper.isBuiltInCanonical(canonicalCat);
            View row = inflater.inflate(R.layout.item_manage_category, categoryList, false);

            View dot = row.findViewById(R.id.categoryDot);
            dot.getBackground().setTint(activity.getColor(CategoryColorHelper.getColor(activity, cat)));

            TextView name = row.findViewById(R.id.categoryName);
            name.setText(cat);
            name.setTextColor(activity.getColor(builtin ? R.color.smart_secondary : R.color.smart_on_surface));

            ImageButton gear = row.findViewById(R.id.btnGear);
            gear.setOnClickListener(v -> showCategoryActionDialog(activity, canonicalCat));

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
                        String canonicalNew = canonicalCategory(activity, newCat);
                        if (!newCat.isEmpty()) {
                            CategoryRepository.addCategoryAsync(activity, canonicalNew, added -> {
                                if (!added) {
                                    Toast.makeText(activity, activity.getString(R.string.category_already_exists_format, newCat), Toast.LENGTH_SHORT).show();
                                    showManageCategoriesDialog(activity);
                                    return;
                                }
                                selectedCategory = canonicalNew;
                                refreshCategorySpinner(activity);
                                showManageCategoriesDialog(activity);
                                showCategorySnackbar(activity, activity.getString(R.string.category_add_success_format, newCat),
                                        android.R.drawable.ic_menu_add, R.color.smart_primary_container);
                            }, error -> Toast.makeText(activity, R.string.category_load_error, Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        if (manageDialog != null && manageDialog.isShowing()) {
            manageDialog.dismiss();
        }
        manageDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.manage_categories_title)
                .setView(content)
                .setPositiveButton(R.string.close_label, null)
                .show();
    }

    private void showCategoryActionDialog(BaseActivity activity, String canonicalCat) {
        View actionView = LayoutInflater.from(activity).inflate(R.layout.dialog_category_action, null);
        int usedCount = countProductsForCategory(activity, canonicalCat);
        String displayName = CategoryColorHelper.getLocalizedCategory(activity, canonicalCat);

        android.app.Dialog actionDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(displayName)
                .setView(actionView)
                .show();

        actionView.findViewById(R.id.actionEdit).setOnClickListener(v -> {
            actionDialog.dismiss();
            showRenameCategoryDialog(activity, canonicalCat, () -> {
                if (manageDialog != null) manageDialog.dismiss();
                showManageCategoriesDialog(activity);
            });
        });
        actionView.findViewById(R.id.actionDelete).setOnClickListener(v -> {
            actionDialog.dismiss();
            showDeleteCategoryDialog(activity, canonicalCat, usedCount, () -> {
                if (manageDialog != null) manageDialog.dismiss();
                showManageCategoriesDialog(activity);
            });
        });
    }

    private void showRenameCategoryDialog(BaseActivity activity, String oldName, Runnable onDone) {
        View inputLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null);
        EditText input = inputLayout.findViewById(android.R.id.edit);
        String displayOld = CategoryColorHelper.getLocalizedCategory(activity, oldName);
        input.setText(displayOld);
        input.setSelection(displayOld.length());
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.rename_category_title)
                .setView(inputLayout)
                .setPositiveButton(R.string.rename_label, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    String canonicalNew = canonicalCategory(activity, newName);
                    if (newName.isEmpty() || canonicalNew.equals(oldName)) {
                        return;
                    }
                    CategoryRepository.renameCategoryAsync(activity, oldName, canonicalNew, ignored -> {
                        if (canonicalCategory(activity, selectedCategory).equals(oldName)) selectedCategory = canonicalNew;
                        refreshCategorySpinner(activity);
                        onDone.run();
                        showCategorySnackbar(activity, activity.getString(R.string.category_rename_success_format, newName),
                                R.drawable.ic_edit, R.color.smart_primary_container);
                    }, error -> {
                        Toast.makeText(activity, R.string.category_rename_error, Toast.LENGTH_SHORT).show();
                        onDone.run();
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteCategoryDialog(BaseActivity activity, String catToDelete, int productCount, Runnable onDone) {
        String displayDelete = CategoryColorHelper.getLocalizedCategory(activity, catToDelete);
        if (productCount > 0) {
            showCannotDeleteDialog(activity, displayDelete, catToDelete, productCount);
            return;
        }
        View confirmView = LayoutInflater.from(activity).inflate(R.layout.dialog_delete_confirm, null);
        android.app.Dialog dlg = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        dlg.setContentView(confirmView);
        dlg.show();
        if (dlg.getWindow() != null) {
            android.view.WindowManager.LayoutParams p = dlg.getWindow().getAttributes();
            p.gravity = android.view.Gravity.CENTER;
            p.dimAmount = 0.5f;
            p.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            p.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            dlg.getWindow().setAttributes(p);
            dlg.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        ((TextView) confirmView.findViewById(R.id.dialogTitle)).setText(
                activity.getString(R.string.delete_category_title_format, displayDelete));
        ((TextView) confirmView.findViewById(R.id.dialogMessage)).setText(
                R.string.category_delete_message_empty);

        confirmView.findViewById(R.id.dialogConfirm).setOnClickListener(v -> {
            dlg.dismiss();
            CategoryRepository.deleteCategoryAsync(activity, catToDelete, success -> {
                if (!success) {
                    Toast.makeText(activity, R.string.category_delete_blocked_title, Toast.LENGTH_SHORT).show();
                    return;
                }
                CategoryRepository.getDisplayCategoriesAsync(activity, active -> {
                    if (canonicalCategory(activity, selectedCategory).equals(catToDelete)) {
                        selectedCategory = active.isEmpty() ? "General" : canonicalCategory(activity, active.get(0));
                    }
                    refreshCategorySpinner(activity);
                    onDone.run();

                    Snackbar snackbar = Snackbar.make(activity.findViewById(R.id.root),
                            activity.getString(R.string.category_delete_success_format, displayDelete),
                            Snackbar.LENGTH_SHORT);
                    snackbar.getView().setBackgroundResource(R.drawable.bg_undo_bar);
                    snackbar.setActionTextColor(activity.getColor(R.color.smart_primary));
                    snackbar.setAction(R.string.undo_delete_label, v2 -> {
                        CategoryRepository.addCategoryAsync(activity, catToDelete, unused -> {
                            selectedCategory = catToDelete;
                            refreshCategorySpinner(activity);
                        }, error -> {});
                    });
                    snackbar.show();
                }, error -> {
                    refreshCategorySpinner(activity);
                    onDone.run();
                });
            }, error -> Toast.makeText(activity, R.string.category_load_error, Toast.LENGTH_SHORT).show());
        });
        confirmView.findViewById(R.id.dialogCancel).setOnClickListener(v -> dlg.dismiss());
    }

    private void showCannotDeleteDialog(BaseActivity activity, String displayName, String canonicalCat, int count) {
        View confirmView = LayoutInflater.from(activity).inflate(R.layout.dialog_delete_confirm, null);
        android.app.Dialog dlg = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        dlg.setContentView(confirmView);
        dlg.show();
        if (dlg.getWindow() != null) {
            android.view.WindowManager.LayoutParams p = dlg.getWindow().getAttributes();
            p.gravity = android.view.Gravity.CENTER;
            p.dimAmount = 0.5f;
            p.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            p.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            dlg.getWindow().setAttributes(p);
            dlg.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        ((TextView) confirmView.findViewById(R.id.dialogTitle)).setText(R.string.category_delete_blocked_title);
        ((TextView) confirmView.findViewById(R.id.dialogMessage)).setText(
                activity.getString(R.string.category_delete_blocked_message_format, count));

        Button confirmBtn = confirmView.findViewById(R.id.dialogConfirm);
        confirmBtn.setText(R.string.got_it);
        confirmBtn.setOnClickListener(v -> dlg.dismiss());
        confirmView.findViewById(R.id.dialogCancel).setVisibility(View.GONE);
    }

    private int countProductsForCategory(BaseActivity activity, String canonicalCat) {
        int count = 0;
        for (Product p : productsSnapshot) {
            if (canonicalCat.equals(canonicalCategory(activity, p.getCategory()))) count++;
        }
        return count;
    }

    private void showCategorySnackbar(BaseActivity activity, String message, int iconRes, int tintRes) {
        Snackbar snackbar = Snackbar.make(activity.findViewById(R.id.root), message, Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundResource(R.drawable.bg_undo_bar);
        snackbar.show();
    }

    private int dpToPx(BaseActivity activity, int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density);
    }

    private String canonicalCategory(BaseActivity activity, String category) {
        return CategoryColorHelper.toCanonical(activity, category);
    }

    private boolean categoryExistsInProducts(BaseActivity activity, String cat) {
        String canonicalCat = canonicalCategory(activity, cat);
        for (Product p : productsSnapshot) {
            if (canonicalCat.equals(canonicalCategory(activity, p.getCategory()))) return true;
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

        String storage = selectedStorage();
        int icon;
        String storageId = LocalDataContract.storageIdForName(storage);
        if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageId)) {
            icon = R.drawable.ic_storage_freeze;
        } else if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageId)) {
            icon = R.drawable.ic_storage_fridge;
        } else {
            icon = R.drawable.ic_storage_room;
        }

        String quantity = ProductQuantityValidator.normalize(quantityInput.getText().toString());
        if (quantity == null) {
            quantityInput.setError(activity.getString(R.string.quantity_invalid));
            quantityInput.requestFocus();
            Toast.makeText(activity, R.string.quantity_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        String unit = unitSpinner.getSelectedItem().toString();

        String imageUrl = currentPhotoPath != null ? currentPhotoPath : product.getImageUrl();
        Product updated = new Product(
                product.getId(),
                name,
                selectedCategory.isEmpty() ? "General" : canonicalCategory(activity, selectedCategory),
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
        ProductRepository.updateProductAsync(activity, updated, saved -> {
            Toast.makeText(activity, R.string.product_updated, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onUpdated != null) {
                onUpdated.run();
            }
        }, error -> Toast.makeText(activity, R.string.error_load, Toast.LENGTH_SHORT).show());
    }

    private String selectedStorage() {
        if (selectedStorageId == R.id.editStorageFridge) {
            return LocalDataContract.STORAGE_REFRIGERATOR_NAME;
        }
        if (selectedStorageId == R.id.editStorageFreezer) {
            return LocalDataContract.STORAGE_FREEZE_NAME;
        }
        return LocalDataContract.STORAGE_ROOM_TEMP_NAME;
    }
}
