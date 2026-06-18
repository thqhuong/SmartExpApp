package com.example.smartexpapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;

import java.util.List;

public class ProductHistoryAdapter extends ListAdapter<Product, ProductHistoryAdapter.ViewHolder> {

    public interface OnRestoreListener {
        void onRestoreClick(Product product);
    }

    private final OnRestoreListener restoreListener;

    public ProductHistoryAdapter(OnRestoreListener restoreListener) {
        super(new DiffUtil.ItemCallback<Product>() {
            @Override
            public boolean areItemsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
                return oldItem.getStatus().equals(newItem.getStatus())
                        && oldItem.getName().equals(newItem.getName());
            }
        });
        this.restoreListener = restoreListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = getItem(position);
        holder.bind(product);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productIcon;
        private final TextView productName;
        private final TextView productMeta;
        private final TextView actionInfo;
    private final TextView expiryStatus;
    private final ProgressBar expiryProgress;
    private final Button btnRestore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productIcon = itemView.findViewById(R.id.productIcon);
            productName = itemView.findViewById(R.id.productName);
            productMeta = itemView.findViewById(R.id.productMeta);
            actionInfo = itemView.findViewById(R.id.actionInfo);
            expiryStatus = itemView.findViewById(R.id.expiryStatus);
            expiryProgress = itemView.findViewById(R.id.expiryProgress);
            btnRestore = itemView.findViewById(R.id.btnRestore);
        }

        void bind(Product product) {
            productName.setText(product.getName());
            productMeta.setText(product.getStorage() + " \u2022 " + product.getAmount());

            String imgUrl = product.getImageUrl();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                productIcon.setImageTintList(null);
                ImageLoader.load(productIcon, imgUrl);
            } else {
                int tintColor = CategoryColorHelper.getColor(itemView.getContext(), product.getCategory());
                productIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(tintColor)));
                ViewUtils.setIcon(productIcon, product.getIconRes(), tintColor);
            }

            expiryStatus.setText(product.getExpiryStatus());

            int progress = product.getExpiryProgress();
            expiryProgress.setProgress(progress);

            String status = product.getStatus();

            if ("CONSUMED".equals(status)) {
                actionInfo.setText("Consumed");
            } else if ("WASTED".equals(status)) {
                actionInfo.setText("Wasted");
            } else if ("DONATED".equals(status)) {
                actionInfo.setText("Donated");
            } else {
                actionInfo.setText(status);
            }

            expiryProgress.setProgressDrawable(
                    androidx.appcompat.content.res.AppCompatResources.getDrawable(
                            itemView.getContext(), R.drawable.progress_gray));

            btnRestore.setOnClickListener(v -> restoreListener.onRestoreClick(product));
        }
    }
}
