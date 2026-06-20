package com.example.smartexpapp;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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

import com.example.smartexpapp.data.AgentRepository;
import com.example.smartexpapp.data.CategoryRepository;
import com.example.smartexpapp.data.LocalImageRepository;
import com.example.smartexpapp.data.OcrCaptureRepository;
import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.data.SettingsRepository;
import com.example.smartexpapp.data.local.ExpiryScanEntity;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductDraft;
import com.example.smartexpapp.notifications.ReminderScheduler;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.DateParser;
import com.example.smartexpapp.util.DateParser.DateCandidate;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ProductQuantityValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class AddProductActivity extends BaseActivity {
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private final Calendar selectedDate = Calendar.getInstance();
    private boolean hasSelectedDate;
    private int selectedStorageId = R.id.storageRoom;
    private EditText productNameInput;
    private EditText quantityInput;
    private Spinner unitSpinner;
    private Spinner categorySpinner;
    private MaterialButton btnManageCategories;
    private String selectedCategory = "General";
    private TextView expiryDateInput;
    private MaterialButton submitButton;
    private MaterialButton skipSmartDraftButton;
    private TextView titleText;
    private TextView subtitleText;
    private ImageView productPhoto;
    private LinearLayout photoPlaceholder;
    private ImageView editIconOverlay;
    private android.widget.ImageButton btnOcrScan;
    private MaterialButton btnRemovePhoto;
    private FrameLayout photoPreview;

    private String editingProductId;
    private String selectedPhotoPath;
    private String originalPhotoPath;
    private Uri pendingOcrCaptureUri;
    private ExpiryScanEntity pendingExpiryScan;
    private boolean userSelectedStorage;
    private boolean saveCompleted;
    private android.app.Dialog manageDialog;
    private final List<ProductDraft> pendingSmartDrafts = new ArrayList<>();
    private int smartDraftTotalCount;
    private int smartDraftCurrentIndex;

    private final ActivityResultLauncher<String> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoPicked);

    private final ActivityResultLauncher<Uri> ocrPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), this::onOcrPhotoCaptured);

    private final ActivityResultLauncher<String> ocrGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onOcrGalleryImagePicked);

    private final ActivityResultLauncher<Intent> smartAddSpeechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    showSmartAddTextDialog();
                    return;
                }
                ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches == null || matches.isEmpty()) {
                    showSmartAddTextDialog();
                    return;
                }
                parseSmartAddDraft(matches.get(0));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        setupChrome(R.id.nav_add);

        productNameInput = findViewById(R.id.productNameInput);
        quantityInput = findViewById(R.id.quantityInput);
        unitSpinner = findViewById(R.id.unitSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);
        btnManageCategories = findViewById(R.id.btnManageCategories);
        expiryDateInput = findViewById(R.id.expiryDateInput);
        submitButton = findViewById(R.id.addProductButton);
        skipSmartDraftButton = findViewById(R.id.skipSmartDraftButton);
        titleText = findViewById(R.id.addProductTitle);
        subtitleText = findViewById(R.id.addProductSubtitle);
        productPhoto = findViewById(R.id.productPhoto);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);
        editIconOverlay = findViewById(R.id.editIconOverlay);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);
        photoPreview = findViewById(R.id.photoPreview);
        btnOcrScan = findViewById(R.id.btnOcrScan);

        findViewById(R.id.btnLaunchSmartVoice).setOnClickListener(v -> startSmartAddVoice());
        findViewById(R.id.btnTypeSmartAdd).setOnClickListener(v -> showSmartAddTextDialog());

        photoPreview.setOnClickListener(v -> pickPhotoLauncher.launch("image/*"));
        btnOcrScan.setOnClickListener(v -> showOcrSourceDialog());
        btnRemovePhoto.setOnClickListener(v -> confirmRemovePhoto());

        editingProductId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);

        setupUnitSpinner();
        setupCategorySpinner();
        setupStorageOptions();
        expiryDateInput.setOnClickListener(v -> showDatePicker());
        submitButton.setOnClickListener(v -> submitProduct());
        skipSmartDraftButton.setOnClickListener(v -> skipCurrentSmartDraft());

        if (editingProductId != null) {
            populateForEdit(editingProductId);
        }
    }

    @Override
    protected void onDestroy() {
        if (!isChangingConfigurations() && !saveCompleted) {
            deleteUnsavedSelectedPhoto(selectedPhotoPath);
        }
        super.onDestroy();
    }

    private void startSmartAddVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.smart_add_voice_prompt));
        if (intent.resolveActivity(getPackageManager()) == null) {
            showSmartAddTextDialog();
            return;
        }
        smartAddSpeechLauncher.launch(intent);
    }

    private void showSmartAddTextDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.smart_add_hint);
        input.setSingleLine(false);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        new AlertDialog.Builder(this)
                .setTitle(R.string.smart_add_dialog_title)
                .setMessage(R.string.smart_add_dialog_message)
                .setView(input)
                .setPositiveButton(R.string.parse_label, (dialog, which) ->
                        parseSmartAddDraft(input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void parseSmartAddDraft(String input) {
        Toast.makeText(this, R.string.smart_add_preparing, Toast.LENGTH_SHORT).show();
        AgentRepository.parseProductDraftsAsync(this, input, this::showSmartDraftConfirmation);
    }

    private void showSmartDraftConfirmation(List<ProductDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            Toast.makeText(this, R.string.smart_add_no_details, Toast.LENGTH_SHORT).show();
            return;
        }
        if (drafts.size() == 1) {
            showSingleSmartDraftConfirmation(drafts.get(0));
            return;
        }

        StringBuilder message = new StringBuilder(getString(R.string.smart_add_batch_intro_format, drafts.size()));
        for (int i = 0; i < drafts.size(); i++) {
            ProductDraft draft = drafts.get(i);
            message.append("\n")
                    .append(i + 1)
                    .append(". ")
                    .append(draft.getName())
                    .append(" - ")
                    .append(draft.getQuantity())
                    .append(" ")
                    .append(draft.getUnit());
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.smart_add_batch_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.start_review, (dialog, which) -> applySmartDraftBatch(drafts))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showSingleSmartDraftConfirmation(ProductDraft draft) {
        String expiry = draft.hasExpiryDate()
                ? new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new java.util.Date(draft.getExpiryDateMillis()))
                : getString(R.string.not_detected);
        String message = getString(
                R.string.smart_add_draft_summary_format,
                draft.getName(),
                draft.getQuantity(),
                draft.getUnit(),
                draft.getCategory(),
                draft.getStorage(),
                expiry,
                draft.getSourceText()
        );
        new AlertDialog.Builder(this)
                .setTitle(R.string.smart_add_draft_title)
                .setMessage(message)
                .setPositiveButton(R.string.fill_form, (dialog, which) -> applySmartDraft(draft))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applySmartDraft(ProductDraft draft) {
        pendingSmartDrafts.clear();
        smartDraftTotalCount = 0;
        smartDraftCurrentIndex = 0;
        resetFormForSmartDraft();
        applyDraftFields(draft, true);
    }

    private void applySmartDraftBatch(List<ProductDraft> drafts) {
        pendingSmartDrafts.clear();
        smartDraftTotalCount = drafts.size();
        smartDraftCurrentIndex = 1;
        if (drafts.size() > 1) {
            pendingSmartDrafts.addAll(drafts.subList(1, drafts.size()));
        }
        resetFormForSmartDraft();
        applyDraftFields(drafts.get(0), true);
        Toast.makeText(this, getString(R.string.smart_add_review_first_format, drafts.size()), Toast.LENGTH_LONG).show();
    }

    private void applyDraftFields(ProductDraft draft, boolean includeExpiry) {
        productNameInput.setText(draft.getName());
        quantityInput.setText(draft.getQuantity());
        selectSpinnerValue(unitSpinner, draft.getUnit());

        selectedCategory = canonicalCategory(draft.getCategory());
        if (!CategoryRepository.isBuiltInCanonical(selectedCategory)) {
            CategoryRepository.addCategoryAsync(this, selectedCategory, ignored -> refreshCategorySpinner(), error -> refreshCategorySpinner());
        } else {
            refreshCategorySpinner();
        }

        String storageId = LocalDataContract.storageIdForName(draft.getStorage());
        if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageId)) {
            selectStorage(R.id.storageFridge);
        } else if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageId)) {
            selectStorage(R.id.storageFreezer);
        } else {
            selectStorage(R.id.storageRoom);
        }

        if (includeExpiry && draft.hasExpiryDate()) {
            selectedDate.setTimeInMillis(draft.getExpiryDateMillis());
            hasSelectedDate = true;
            expiryDateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(selectedDate.getTime()));
            expiryDateInput.setTextColor(getColor(R.color.smart_on_surface));
        } else if (includeExpiry) {
            Toast.makeText(this, R.string.expiry_not_detected, Toast.LENGTH_LONG).show();
        }
    }

    private void selectSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equalsIgnoreCase(spinner.getItemAtPosition(i).toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void showOcrSourceDialog() {
        String[] sources = {getString(R.string.ocr_source_camera), getString(R.string.ocr_source_gallery)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.scan_expiry_text)
                .setItems(sources, (dialog, which) -> {
                    if (which == 0) {
                        launchOcrCamera();
                    } else {
                        ocrGalleryLauncher.launch("image/*");
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void launchOcrCamera() {
        try {
            pendingOcrCaptureUri = createOcrCaptureUri();
            ocrPhotoLauncher.launch(pendingOcrCaptureUri);
        } catch (Exception error) {
            Toast.makeText(this, R.string.ocr_camera_error, Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createOcrCaptureUri() throws IOException {
        return OcrCaptureRepository.createCaptureUri(this);
    }

    private void onOcrPhotoCaptured(Boolean captured) {
        if (!Boolean.TRUE.equals(captured) || pendingOcrCaptureUri == null) {
            pendingOcrCaptureUri = null;
            return;
        }
        Uri uri = pendingOcrCaptureUri;
        pendingOcrCaptureUri = null;
        processOcrImageUri(uri);
    }

    private void onOcrGalleryImagePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        processOcrImageUri(uri);
    }

    private void processOcrImageUri(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            
            Toast.makeText(this, R.string.ocr_scanning_dates, Toast.LENGTH_SHORT).show();
            
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String rawText = visionText.getText();
                        if (rawText == null || rawText.trim().isEmpty()) {
                            Toast.makeText(this, R.string.ocr_no_text, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        handleOcrText(rawText);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, getString(R.string.ocr_failed_format, e.getMessage()), Toast.LENGTH_SHORT).show();
                    })
                    .addOnCompleteListener(task -> {
                        recognizer.close();
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.image_process_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleOcrText(String rawText) {
        Toast.makeText(this, R.string.ocr_preparing_draft, Toast.LENGTH_SHORT).show();
        List<DateCandidate> detectedDates = DateParser.extractDateCandidates(rawText);
        AgentRepository.parseProductDraftAsync(this, rawText, draft -> {
            if (detectedDates.isEmpty()) {
                showOcrDraftWithoutDateDialog(draft, rawText);
            } else {
                showDetectedDatesDialog(detectedDates, rawText, draft);
            }
        });
    }

    private void showDetectedDatesDialog(List<DateCandidate> dates, String rawText, ProductDraft draft) {
        String[] dateStrings = new String[dates.size()];
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        for (int i = 0; i < dates.size(); i++) {
            DateCandidate candidate = dates.get(i);
            dateStrings[i] = sdf.format(new java.util.Date(candidate.getDateMillis()))
                    + " - " + candidate.getSnippet();
        }

        final int[] selectedIndex = {0};

        new AlertDialog.Builder(this)
                .setTitle(R.string.ocr_scan_review_title)
                .setMessage(ocrDraftSummary(draft) + "\n\n" + getString(R.string.ocr_scan_review_message))
                .setSingleChoiceItems(dateStrings, selectedIndex[0], (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setPositiveButton(R.string.fill_form, (dialog, which) -> {
                    DateCandidate candidate = dates.get(selectedIndex[0]);
                    applyDetectedDate(candidate, rawText);
                    applyDraftFields(draft, false);
                })
                .setNeutralButton(R.string.date_only, (dialog, which) -> {
                    applyDetectedDate(dates.get(selectedIndex[0]), rawText);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showOcrDraftWithoutDateDialog(ProductDraft draft, String rawText) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ocr_draft_review_title)
                .setMessage(ocrDraftSummary(draft) + "\n\n" + getString(R.string.ocr_draft_review_message))
                .setPositiveButton(R.string.fill_details, (dialog, which) -> {
                    applyDraftFields(draft, false);
                    pendingExpiryScan = new ExpiryScanEntity();
                    pendingExpiryScan.id = UUID.randomUUID().toString();
                    pendingExpiryScan.rawText = rawText;
                    pendingExpiryScan.confidence = 0.25f;
                    pendingExpiryScan.scannedAt = System.currentTimeMillis();
                    Toast.makeText(this, R.string.select_expiry_before_saving, Toast.LENGTH_LONG).show();
                })
                .setNeutralButton(R.string.select_date, (dialog, which) -> showDatePicker(rawText))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String ocrDraftSummary(ProductDraft draft) {
        return getString(
                R.string.ocr_draft_summary_format,
                draft.getName(),
                draft.getQuantity(),
                draft.getUnit(),
                draft.getCategory(),
                draft.getStorage()
        );
    }

    private void applyDetectedDate(DateCandidate candidate, String rawText) {
        selectedDate.setTimeInMillis(candidate.getDateMillis());
        selectedDate.set(Calendar.HOUR_OF_DAY, 23);
        selectedDate.set(Calendar.MINUTE, 59);
        selectedDate.set(Calendar.SECOND, 59);
        selectedDate.set(Calendar.MILLISECOND, 999);
        hasSelectedDate = true;
        expiryDateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(selectedDate.getTime()));
        expiryDateInput.setTextColor(getColor(R.color.smart_on_surface));

        pendingExpiryScan = new ExpiryScanEntity();
        pendingExpiryScan.id = UUID.randomUUID().toString();
        pendingExpiryScan.rawText = rawText;
        pendingExpiryScan.detectedDateMillis = selectedDate.getTimeInMillis();
        pendingExpiryScan.confidence = candidate.getConfidence();
        pendingExpiryScan.scannedAt = System.currentTimeMillis();
    }

    private void onPhotoPicked(Uri uri) {
        if (uri == null) return;
        String path = saveImageToInternalStorage(uri);
        if (path == null) {
            Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        deleteUnsavedSelectedPhoto(selectedPhotoPath);
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
        deleteUnsavedSelectedPhoto(selectedPhotoPath);
        clearPhotoPreviewOnly();
    }

    private void clearPhotoPreviewOnly() {
        selectedPhotoPath = null;
        productPhoto.setVisibility(View.GONE);
        productPhoto.setImageBitmap(null);
        editIconOverlay.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.GONE);
        photoPlaceholder.setVisibility(View.VISIBLE);
    }

    private void confirmRemovePhoto() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_photo_title)
                .setMessage(R.string.remove_photo_message)
                .setPositiveButton(R.string.remove_label, (dialog, which) -> clearPhoto())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteUnsavedSelectedPhoto(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        if (editingProductId == null || !path.equals(originalPhotoPath)) {
            LocalImageRepository.deleteProductImage(getApplicationContext(), path);
        }
    }

    private void setupUnitSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.unit_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);
        if (editingProductId == null) {
            quantityInput.setText(R.string.quantity_default);
        }
    }

    private void setupCategorySpinner() {
        refreshCategorySpinner();
        btnManageCategories.setOnClickListener(v -> showManageCategoriesDialog());
    }

    private void refreshCategorySpinner() {
        CategoryRepository.getDisplayCategoriesAsync(this, this::refreshCategorySpinner, error -> {
            Toast.makeText(this, R.string.category_load_error, Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshCategorySpinner(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        String canonicalSelected = canonicalCategory(selectedCategory);
        int pos = -1;
        for (int i = 0; i < items.size(); i++) {
            if (canonicalCategory(items.get(i)).equals(canonicalSelected)) {
                pos = i;
                break;
            }
        }
        if (pos >= 0) categorySpinner.setSelection(pos);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String display = parent.getItemAtPosition(position).toString();
                selectedCategory = canonicalCategory(display);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showManageCategoriesDialog() {
        ProductRepository.getProductsAsync(this, products ->
                CategoryRepository.getDisplayCategoriesAsync(this,
                        categories -> showManageCategoriesDialog(products, categories),
                        error -> Toast.makeText(this, R.string.category_load_error, Toast.LENGTH_SHORT).show()),
                error -> Toast.makeText(this, R.string.category_load_error, Toast.LENGTH_SHORT).show());
    }

    private void showManageCategoriesDialog(List<Product> all, List<String> allCats) {
        View content = getLayoutInflater().inflate(R.layout.dialog_manage_categories, null);
        LinearLayout categoryList = content.findViewById(R.id.categoryList);
        MaterialButton btnAdd = content.findViewById(R.id.btnAddCategory);

        for (String cat : allCats) {
            String canonicalCat = canonicalCategory(cat);
            boolean builtin = CategoryColorHelper.isBuiltInCanonical(canonicalCat);
            View row = getLayoutInflater().inflate(R.layout.item_manage_category, categoryList, false);

            View dot = row.findViewById(R.id.categoryDot);
            dot.getBackground().setTint(getColor(CategoryColorHelper.getColor(this, cat)));

            TextView name = row.findViewById(R.id.categoryName);
            name.setText(cat);
            name.setTextColor(getColor(builtin ? R.color.smart_secondary : R.color.smart_on_surface));

            MaterialButton editBtn = row.findViewById(R.id.btnEditCategory);
            MaterialButton deleteBtn = row.findViewById(R.id.btnDeleteCategory);

            editBtn.setVisibility(View.VISIBLE);
            editBtn.setOnClickListener(v -> showRenameCategoryDialog(canonicalCat, () -> showManageCategoriesDialog()));

            boolean used = isCategoryUsed(canonicalCat, all);
            if (used) {
                deleteBtn.setVisibility(View.GONE);
            } else {
                deleteBtn.setVisibility(View.VISIBLE);
                deleteBtn.setOnClickListener(v -> showDeleteCategoryDialog(canonicalCat, () -> showManageCategoriesDialog()));
            }

            categoryList.addView(row);
        }

        btnAdd.setOnClickListener(v -> {
            View inputLayout = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
            EditText input = inputLayout.findViewById(android.R.id.edit);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.category_add_title)
                    .setView(inputLayout)
                    .setPositiveButton(R.string.add_label, (d, w) -> {
                        String newCat = input.getText().toString().trim();
                        String canonicalNew = canonicalCategory(newCat);
                        if (!newCat.isEmpty()) {
                            CategoryRepository.addCategoryAsync(this, canonicalNew, added -> {
                                if (!added) {
                                    Toast.makeText(this, getString(R.string.category_already_exists_format, newCat), Toast.LENGTH_SHORT).show();
                                    showManageCategoriesDialog();
                                    return;
                                }
                                selectedCategory = canonicalNew;
                                refreshCategorySpinner();
                                showManageCategoriesDialog();
                            }, error -> Toast.makeText(this, R.string.category_load_error, Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        if (manageDialog != null && manageDialog.isShowing()) {
            manageDialog.dismiss();
        }
        manageDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.manage_categories_title)
                .setView(content)
                .setPositiveButton(R.string.close_label, null)
                .show();
    }

    private void showRenameCategoryDialog(String oldName, Runnable onDone) {
        View inputLayout = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        EditText input = inputLayout.findViewById(android.R.id.edit);
        String displayOld = CategoryColorHelper.getLocalizedCategory(this, oldName);
        input.setText(displayOld);
        input.setSelection(displayOld.length());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename_category_title)
                .setView(inputLayout)
                .setPositiveButton(R.string.rename_label, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    String canonicalNew = canonicalCategory(newName);
                    if (newName.isEmpty() || canonicalNew.equals(oldName)) {
                        return;
                    }
                    ProductRepository.getProductsAsync(this, all -> {
                        CategoryRepository.renameCategoryAsync(this, oldName, canonicalNew, ignored -> {
                        if (canonicalCategory(selectedCategory).equals(oldName)) selectedCategory = canonicalNew;
                        refreshCategorySpinner();
                        onDone.run();
                        }, error -> {
                            Toast.makeText(this, R.string.category_rename_error, Toast.LENGTH_SHORT).show();
                            onDone.run();
                        });
                    }, error -> {
                        Toast.makeText(this, R.string.category_rename_error, Toast.LENGTH_SHORT).show();
                        onDone.run();
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteCategoryDialog(String catToDelete, Runnable onDone) {
        String displayDelete = CategoryColorHelper.getLocalizedCategory(this, catToDelete);
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_category_title_format, displayDelete))
                .setMessage(R.string.category_delete_message_empty)
                .setPositiveButton(R.string.delete_confirm, (d, w) -> {
                    CategoryRepository.deleteCategoryAsync(this, catToDelete, ignored -> {
                        CategoryRepository.getDisplayCategoriesAsync(this, active -> {
                            if (canonicalCategory(selectedCategory).equals(catToDelete)) {
                                String fallback = active.isEmpty() ? "General" : canonicalCategory(active.get(0));
                                selectedCategory = fallback;
                            }
                            refreshCategorySpinner();
                            onDone.run();
                        }, error -> {
                            refreshCategorySpinner();
                            onDone.run();
                        });
                    }, error -> Toast.makeText(this, R.string.category_load_error, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private String canonicalCategory(String category) {
        return CategoryColorHelper.toCanonical(this, category);
    }

    private boolean isCategoryUsed(String canonicalCat, List<Product> allProducts) {
        for (Product p : allProducts) {
            if (canonicalCat.equals(canonicalCategory(p.getCategory()))) {
                return true;
            }
        }
        return false;
    }

    private boolean categoryExistsInProducts(List<Product> allProducts, String cat) {
        String canonicalCat = canonicalCategory(cat);
        for (Product p : allProducts) {
            if (canonicalCat.equals(canonicalCategory(p.getCategory()))) {
                return true;
            }
        }
        return false;
    }

    private void setupStorageOptions() {
        findViewById(R.id.storageRoom).setOnClickListener(v -> selectStorage(R.id.storageRoom, true));
        findViewById(R.id.storageFridge).setOnClickListener(v -> selectStorage(R.id.storageFridge, true));
        findViewById(R.id.storageFreezer).setOnClickListener(v -> selectStorage(R.id.storageFreezer, true));
        selectStorage(R.id.storageRoom);
        if (editingProductId == null) {
            SettingsRepository.getSettingsAsync(this, settings -> {
                if (!userSelectedStorage && editingProductId == null) {
                    selectStorage(storageViewIdForLocationId(settings.getDefaultStorageLocationId()));
                }
            }, error -> {
            });
        }
    }

    private void selectStorage(int storageId) {
        selectStorage(storageId, false);
    }

    private void selectStorage(int storageId, boolean userInitiated) {
        if (userInitiated) {
            userSelectedStorage = true;
        }
        selectedStorageId = storageId;
        setStorageSelected(R.id.storageRoom, storageId);
        setStorageSelected(R.id.storageFridge, storageId);
        setStorageSelected(R.id.storageFreezer, storageId);
    }

    private int storageViewIdForLocationId(String storageLocationId) {
        if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageLocationId)) {
            return R.id.storageFridge;
        }
        if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageLocationId)) {
            return R.id.storageFreezer;
        }
        return R.id.storageRoom;
    }

    private void setStorageSelected(int viewId, int selectedId) {
        View option = findViewById(viewId);
        if (option != null) {
            option.setSelected(viewId == selectedId);
        }
    }

    private void showDatePicker() {
        showDatePicker(null);
    }

    private void showDatePicker(String rawOcrText) {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth, 23, 59, 59);
                    selectedDate.set(Calendar.MILLISECOND, 999);
                    hasSelectedDate = true;
                    expiryDateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(selectedDate.getTime()));
                    expiryDateInput.setTextColor(getColor(R.color.smart_on_surface));
                    if (rawOcrText != null) {
                        pendingExpiryScan = new ExpiryScanEntity();
                        pendingExpiryScan.id = UUID.randomUUID().toString();
                        pendingExpiryScan.rawText = rawOcrText;
                        pendingExpiryScan.detectedDateMillis = selectedDate.getTimeInMillis();
                        pendingExpiryScan.confidence = 0.5f;
                        pendingExpiryScan.scannedAt = System.currentTimeMillis();
                    }
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void populateForEdit(String productId) {
        ProductRepository.getProductByIdAsync(this, productId, product -> {
            if (product == null) {
                Toast.makeText(this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            populateProductFields(product);
        }, error -> {
            Toast.makeText(this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void populateProductFields(Product product) {
        titleText.setText(R.string.edit_product);
        subtitleText.setText(R.string.edit_product_subtitle);
        submitButton.setText(R.string.edit_product);
        submitButton.setIcon(null);

        productNameInput.setText(product.getName());
        quantityInput.setText(product.getQuantity());
        selectSpinnerValue(unitSpinner, product.getUnit());

        selectedCategory = product.getCategory() == null || product.getCategory().trim().isEmpty()
                ? "General"
                : canonicalCategory(product.getCategory());
        refreshCategorySpinner();

        String imgPath = product.getImageUrl();
        if (imgPath != null && !imgPath.isEmpty()) {
            originalPhotoPath = imgPath;
            selectedPhotoPath = imgPath;
            showPhotoPreview(imgPath);
        }

        String storageId = LocalDataContract.storageIdForName(product.getStorage());
        if (LocalDataContract.STORAGE_ROOM_TEMP_ID.equals(storageId)) {
            selectStorage(R.id.storageRoom);
        } else if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageId)) {
            selectStorage(R.id.storageFridge);
        } else if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageId)) {
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
            Toast.makeText(this, R.string.enter_product_name, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasSelectedDate) {
            Toast.makeText(this, R.string.select_expiry_before_saving, Toast.LENGTH_SHORT).show();
            return;
        }

        String storage = selectedStorage();
        int icon = iconForStorage(storage);

        String category = selectedCategory == null || selectedCategory.isEmpty() ? "General" : canonicalCategory(selectedCategory);

        String quantity = ProductQuantityValidator.normalize(quantityInput.getText().toString());
        if (quantity == null) {
            quantityInput.setError(getString(R.string.quantity_invalid));
            quantityInput.requestFocus();
            Toast.makeText(this, R.string.quantity_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        final String finalQuantity = quantity;
        String unit = unitSpinner.getSelectedItem().toString();
        submitButton.setEnabled(false);
        skipSmartDraftButton.setEnabled(false);
        if (editingProductId != null) {
            ProductRepository.getProductByIdAsync(this, editingProductId, existing -> {
                if (existing == null) {
                    submitButton.setEnabled(true);
                    skipSmartDraftButton.setEnabled(true);
                    Toast.makeText(this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                Product updatedProduct = new Product(
                        editingProductId,
                        name,
                        category,
                        finalQuantity,
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
                ProductRepository.updateProductAsync(this, updatedProduct, updated -> {
                    if (Boolean.TRUE.equals(updated)) {
                        afterProductSaved(updatedProduct, getString(R.string.product_updated));
                    } else {
                        submitButton.setEnabled(true);
                        skipSmartDraftButton.setEnabled(true);
                        Toast.makeText(this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
                    }
                }, error -> showSaveFailed());
            }, error -> showSaveFailed());
        } else {
            Product finalProductToSave = new Product(name, category, finalQuantity, unit, storage, selectedDate.getTimeInMillis(), icon, selectedPhotoPath);
            ProductRepository.addProductAsync(this, finalProductToSave,
                    ignored -> afterProductSaved(finalProductToSave, getString(R.string.product_added_format, name)),
                    error -> showSaveFailed());
        }
    }

    private void afterProductSaved(Product product, String toastMessage) {
        if (pendingExpiryScan != null) {
            ExpiryScanEntity scan = pendingExpiryScan;
            pendingExpiryScan = null;
            scan.productId = product.getId();
            scan.createdAt = System.currentTimeMillis();
            scan.updatedAt = System.currentTimeMillis();
            ProductRepository.insertExpiryScanAsync(this, scan,
                    ignored -> finishProductSave(toastMessage),
                    error -> {
                        Toast.makeText(this, R.string.ocr_save_warning, Toast.LENGTH_SHORT).show();
                        finishProductSave(toastMessage);
                    });
            return;
        }
        finishProductSave(toastMessage);
    }

    private void finishProductSave(String toastMessage) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        ReminderScheduler.scheduleDaily(this);
        ReminderScheduler.runSoon(this);

        if (moveToNextSmartDraft(true)) {
            return;
        }

        clearSmartDraftBatch();
        saveCompleted = true;
        Intent inventoryIntent = new Intent(this, InventoryActivity.class);
        inventoryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        inventoryIntent.putExtra(InventoryActivity.EXTRA_RESET_FILTERS, true);
        startActivity(inventoryIntent);
        overridePendingTransition(0, 0);
        finish();
    }

    private boolean moveToNextSmartDraft(boolean showToast) {
        if (pendingSmartDrafts.isEmpty()) {
            return false;
        }
        ProductDraft next = pendingSmartDrafts.remove(0);
        smartDraftCurrentIndex++;
        resetFormForSmartDraft();
        applyDraftFields(next, true);
        updateSmartDraftActions();
        if (showToast) {
            Toast.makeText(this, R.string.smart_add_next_ready, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private void skipCurrentSmartDraft() {
        deleteUnsavedSelectedPhoto(selectedPhotoPath);
        if (moveToNextSmartDraft(false)) {
            Toast.makeText(this, R.string.smart_add_item_skipped, Toast.LENGTH_SHORT).show();
            return;
        }
        clearSmartDraftBatch();
        Toast.makeText(this, R.string.smart_add_item_skipped, Toast.LENGTH_SHORT).show();
        saveCompleted = true;
        Intent inventoryIntent = new Intent(this, InventoryActivity.class);
        inventoryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        inventoryIntent.putExtra(InventoryActivity.EXTRA_RESET_FILTERS, true);
        startActivity(inventoryIntent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void clearSmartDraftBatch() {
        pendingSmartDrafts.clear();
        smartDraftTotalCount = 0;
        smartDraftCurrentIndex = 0;
        updateSmartDraftActions();
    }

    private void resetFormForSmartDraft() {
        editingProductId = null;
        pendingExpiryScan = null;
        originalPhotoPath = null;
        clearPhotoPreviewOnly();
        productNameInput.setText("");
        quantityInput.setText(R.string.quantity_default);
        selectSpinnerValue(unitSpinner, "pcs");
        selectedCategory = "General";
        refreshCategorySpinner();
        selectStorage(R.id.storageRoom);
        hasSelectedDate = false;
        expiryDateInput.setText(R.string.select_expiry_date);
        expiryDateInput.setTextColor(getColor(R.color.smart_secondary));
        titleText.setText(R.string.add_product);
        subtitleText.setText(R.string.add_product_subtitle);
        submitButton.setEnabled(true);
        submitButton.setIconResource(android.R.drawable.ic_menu_add);
        skipSmartDraftButton.setEnabled(true);
        updateSmartDraftActions();
    }

    private void updateSmartDraftActions() {
        if (smartDraftTotalCount > 1 && smartDraftCurrentIndex > 0) {
            skipSmartDraftButton.setVisibility(View.VISIBLE);
            submitButton.setText(getString(R.string.add_product_progress_format, smartDraftCurrentIndex, smartDraftTotalCount));
        } else {
            skipSmartDraftButton.setVisibility(View.GONE);
            submitButton.setText(editingProductId == null ? R.string.add_product : R.string.edit_product);
        }
    }

    private void showSaveFailed() {
        submitButton.setEnabled(true);
        skipSmartDraftButton.setEnabled(true);
        Toast.makeText(this, R.string.product_save_error, Toast.LENGTH_SHORT).show();
    }

    private String selectedStorage() {
        if (selectedStorageId == R.id.storageFridge) {
            return LocalDataContract.STORAGE_REFRIGERATOR_NAME;
        }
        if (selectedStorageId == R.id.storageFreezer) {
            return LocalDataContract.STORAGE_FREEZE_NAME;
        }
        return LocalDataContract.STORAGE_ROOM_TEMP_NAME;
    }

    private int iconForStorage(String storage) {
        String storageId = LocalDataContract.storageIdForName(storage);
        if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageId)) {
            return R.drawable.ic_storage_freeze;
        }
        if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageId)) {
            return R.drawable.ic_storage_fridge;
        }
        return R.drawable.ic_storage_room;
    }
}
