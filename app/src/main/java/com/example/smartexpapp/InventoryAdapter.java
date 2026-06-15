package com.example.smartexpapp;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.card.MaterialCardView;

public class InventoryAdapter extends ListAdapter<Product, InventoryAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onDeleteClick(Product product);
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

    private final OnProductClickListener listener;

    public InventoryAdapter(OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
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
            categoryDot = itemView.findViewById(R.id.categoryDot);
            icon = itemView.findViewById(R.id.productIcon);
            productName = itemView.findViewById(R.id.productName);
            productMeta = itemView.findViewById(R.id.productMeta);
        }

        void bind(Product product) {
            card.setOnClickListener(v -> listener.onProductClick(product));
            deleteBtn.setOnClickListener(v -> listener.onDeleteClick(product));

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
        }
    }
}