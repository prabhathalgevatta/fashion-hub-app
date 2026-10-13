package lk.prabhath.fashionhub.activity.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import lk.prabhath.fashionhub.R;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<String> categoryList;
    private int selectedPosition = 0; // Default selection
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public CategoryAdapter(List<String> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categoryList.get(position);
        holder.btnCategory.setText(category);

        holder.btnCategory.setBackgroundTintList(null);
        if (selectedPosition == position) {
            holder.btnCategory.setBackgroundResource(R.drawable.bg_category_pill_active);
            holder.btnCategory.setTextColor(android.graphics.Color.WHITE);
        } else {
            holder.btnCategory.setBackgroundResource(R.drawable.bg_category_pill_inactive);
            holder.btnCategory.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        }

        holder.btnCategory.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position;

            // Update UI
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            // Notify listener
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        MaterialButton btnCategory;
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            btnCategory = itemView.findViewById(R.id.btn_category_item);
        }
    }
}