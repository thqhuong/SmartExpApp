package com.example.smartexpapp;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InventoryAdapter extends ListAdapter<Product, InventoryAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onDeleteClick(Product product);
    }

    public interface OnProductLongClickListener {
        boolean onProductLongClick(Product product);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(Set<String> selectedIds);
    }

    private static final DiffUtil.ItemCallback<Product> DIFF_CALLBACK = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getCategory().equals(newItem.getCategory())
                    && oldItem.getAmount().equals(newItem.getAmount())
                    && oldItem.getExpiryDateMillis() == newItem.getExpiryDateMillis()
                    && oldItem.getDaysUntilExpiry() == newItem.getDaysUntilExpiry()
                    && oldItem.getStorage().equals(newItem.getStorage())
                    && oldItem.getIconRes() == newItem.getIconRes()
                    && (oldItem.getImageUrl() == null ? newItem.getImageUrl() == null : oldItem.getImageUrl().equals(newItem.getImageUrl()));
        }
    };

    private final OnProductClickListener clickListener;
    @Nullable private OnProductLongClickListener longClickListener;
    @Nullable private OnSelectionChangedListener selectionChangedListener;
    @Nullable private Set<String> selectedIds;

    public InventoryAdapter(OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.clickListener = listener;
    }

    public void setOnProductLongClickListener(@Nullable OnProductLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnSelectionChangedListener(@Nullable OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void setSelectedIds(@Nullable Set<String> ids) {
        this.selectedIds = ids != null ? new HashSet<>(ids) : null;
        notifyItemRangeChanged(0, getItemCount());
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedIds != null ? selectedIds : new HashSet<String>());
        }
    }

    public void selectAll() {
        if (selectedIds == null) return;
        for (int i = 0; i < getItemCount(); i++) {
            selectedIds.add(getItem(i).getId());
        }
        notifyItemRangeChanged(0, getItemCount());
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedIds);
        }
    }

    @Nullable
    public Set<String> getSelectedIds() {
        return selectedIds != null ? Collections.unmodifiableSet(selectedIds) : null;
    }

    private boolean isSelected(String id) {
        return selectedIds != null && selectedIds.contains(id);
    }

    public boolean isInSelectionMode() {
        return selectedIds != null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_product, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final TextView urgentBadge;
        private final TextView expiryStatus;
        private final ProgressBar progress;
        private final View deleteBtn;
        private final ImageView btnCheckbox;
        private final View categoryDot;
        private final ImageView icon;
        private final TextView productName;
        private final TextView productMeta;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.productCard);
            urgentBadge = itemView.findViewById(R.id.urgentBadge);
            expiryStatus = itemView.findViewById(R.id.expiryStatus);
            progress = itemView.findViewById(R.id.expiryProgress);
            deleteBtn = itemView.findViewById(R.id.btnDelete);
            btnCheckbox = itemView.findViewById(R.id.btnCheckbox);
            categoryDot = itemView.findViewById(R.id.categoryDot);
            icon = itemView.findViewById(R.id.productIcon);
            productName = itemView.findViewById(R.id.productName);
            productMeta = itemView.findViewById(R.id.productMeta);
        }

        void bind(Product product) {
            String productId = product.getId();
            boolean selected = isSelected(productId);
            boolean selectionMode = isInSelectionMode();

            card.setOnClickListener(v -> {
                if (selectionMode) {
                    toggleSelection(productId);
                } else {
                    clickListener.onProductClick(product);
                }
            });
            deleteBtn.setOnClickListener(v -> clickListener.onDeleteClick(product));
            card.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    card.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    return longClickListener.onProductLongClick(product);
                }
                return false;
            });

            categoryDot.getBackground().setTint(
                    card.getContext().getColor(CategoryColorHelper.getColor(card.getContext(), product.getCategory()))
            );
            categoryDot.setVisibility(View.VISIBLE);

            String imgUrl = product.getImageUrl();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                icon.setImageTintList(null);
                ImageLoader.load(icon, imgUrl);
            } else {
                int tintColor = CategoryColorHelper.getColor(card.getContext(), product.getCategory());
                icon.setImageTintList(ColorStateList.valueOf(card.getContext().getColor(tintColor)));
                ViewUtils.setIcon(icon, product.getIconRes(), tintColor);
            }

            productName.setText(product.getName());
            productMeta.setText(card.getContext().getString(R.string.product_meta_format,
                    CategoryColorHelper.getLocalizedCategory(card.getContext(), product.getCategory()), product.getAmount()));
            expiryStatus.setText(product.getExpiryStatus(card.getContext()));
            progress.setProgress(product.getExpiryProgress());

            float density = card.getResources().getDisplayMetrics().density;
            if (product.isExpired()) {
                card.setCardBackgroundColor(card.getContext().getColor(R.color.smart_glass_error));
                card.setStrokeColor(card.getContext().getColor(R.color.smart_glass_error_stroke));
                card.setStrokeWidth((int) (1.5f * density));
                urgentBadge.setVisibility(View.VISIBLE);
                urgentBadge.setBackgroundResource(R.drawable.bg_error_soft_badge);
                urgentBadge.setText(R.string.status_expired);
                urgentBadge.setTextColor(card.getContext().getColor(R.color.smart_error));
                expiryStatus.setTextColor(card.getContext().getColor(R.color.smart_error));
                progress.setProgressDrawable(AppCompatResources.getDrawable(card.getContext(), R.drawable.progress_orange));
            } else if (product.isExpiringSoon()) {
                card.setCardBackgroundColor(card.getContext().getColor(R.color.smart_glass_urgent));
                card.setStrokeColor(card.getContext().getColor(R.color.smart_glass_urgent_stroke));
                card.setStrokeWidth((int) (1.5f * density));
                urgentBadge.setVisibility(View.VISIBLE);
                urgentBadge.setBackgroundResource(R.drawable.bg_primary_soft_badge);
                urgentBadge.setText(R.string.status_expiring);
                urgentBadge.setTextColor(card.getContext().getColor(R.color.smart_primary_container));
                expiryStatus.setTextColor(card.getContext().getColor(R.color.smart_primary_container));
                progress.setProgressDrawable(AppCompatResources.getDrawable(card.getContext(), R.drawable.progress_orange));
            } else {
                card.setCardBackgroundColor(card.getContext().getColor(R.color.smart_glass_surface));
                card.setStrokeColor(card.getContext().getColor(R.color.smart_glass_stroke));
                card.setStrokeWidth((int) (1.0f * density));
                urgentBadge.setVisibility(View.GONE);
                expiryStatus.setTextColor(card.getContext().getColor(R.color.smart_on_surface));
                progress.setProgressDrawable(AppCompatResources.getDrawable(card.getContext(), R.drawable.progress_gray));
            }

            if (selectionMode) {
                deleteBtn.setVisibility(View.GONE);
                btnCheckbox.setVisibility(View.VISIBLE);
                btnCheckbox.setImageResource(selected ? R.drawable.ic_check_circle : R.drawable.ic_circle_outline);
                btnCheckbox.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(card.getContext(), selected ? R.color.smart_primary : R.color.smart_muted)));
                if (selected) {
                    card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.smart_primary));
                    card.setStrokeWidth((int) (2f * density));
                    card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.smart_primary_soft));
                } else {
                    card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.smart_glass_stroke));
                    card.setStrokeWidth((int) (1.0f * density));
                }
            } else {
                deleteBtn.setVisibility(View.VISIBLE);
                btnCheckbox.setVisibility(View.GONE);
            }
        }

        private void toggleSelection(String productId) {
            if (selectedIds == null) return;
            boolean wasSelected = selectedIds.contains(productId);
            if (wasSelected) {
                selectedIds.remove(productId);
            } else {
                selectedIds.add(productId);
            }
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(selectedIds);
            }
            notifyItemChanged(getAbsoluteAdapterPosition());
        }
    }
}