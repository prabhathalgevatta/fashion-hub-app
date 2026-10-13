package lk.prabhath.fashionhub.activity.adapter;

import android.content.Context;
import android.content.Intent;
import lk.prabhath.fashionhub.activity.ProductDetailsActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.WishlistActivity;
import lk.prabhath.fashionhub.activity.database.DatabaseHelper;
import lk.prabhath.fashionhub.activity.model.WishlistItem;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {

    private List<WishlistItem> wishlist;
    private DatabaseHelper dbHelper;

    public WishlistAdapter(List<WishlistItem> wishlist) {
        this.wishlist = wishlist;
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wishlist, parent, false);
        dbHelper = new DatabaseHelper(parent.getContext());
        return new WishlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        WishlistItem item = wishlist.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(item.getName());
        holder.tvPrice.setText(String.format("Rs. %.2f", item.getPrice()));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context).load(item.getImageUrl()).into(holder.ivImage);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailsActivity.class);
            intent.putExtra("product_id", item.getProductId());
            intent.putExtra("product_name", item.getName());
            intent.putExtra("product_price", item.getPrice());
            intent.putExtra("product_image", item.getImageUrl());
            context.startActivity(intent);
        });

        holder.btnAddToCart.setOnClickListener(v -> {
            boolean added = dbHelper.addToCart(item.getProductId(), item.getName(), item.getPrice(), item.getImageUrl(), "M", 1);
            if (added) {
                Toast.makeText(context, "Added to cart!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to add to cart.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Remove Item")
                    .setMessage("Remove this item from your wishlist?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        boolean deleted = dbHelper.removeFromWishlist(item.getProductId());
                        if (deleted) {
                            if (context instanceof WishlistActivity) {
                                ((WishlistActivity) context).loadWishlistItems();
                            }
                            Toast.makeText(context, "Item removed from wishlist", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return wishlist != null ? wishlist.size() : 0;
    }

    public static class WishlistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, btnDelete;
        TextView tvTitle, tvPrice;
        MaterialButton btnAddToCart;

        public WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.wishlist_item_image);
            btnDelete = itemView.findViewById(R.id.wishlist_item_delete);
            tvTitle = itemView.findViewById(R.id.wishlist_item_title);
            tvPrice = itemView.findViewById(R.id.wishlist_item_price);
            btnAddToCart = itemView.findViewById(R.id.btn_wishlist_add_to_cart);
        }
    }
}
