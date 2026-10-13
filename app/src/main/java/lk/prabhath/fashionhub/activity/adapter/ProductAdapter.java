package lk.prabhath.fashionhub.activity.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.database.DatabaseHelper;
import lk.prabhath.fashionhub.activity.model.Product;
import lk.prabhath.fashionhub.util.AuthHelper;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    // --- Search filter ---
    public void setFilteredList(List<Product> filteredList) {
        this.productList = filteredList;
        notifyDataSetChanged(); // Refresh UI
    }
    // ---------------------

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        Context context = holder.itemView.getContext();

        if (product.getName() != null) {
            holder.productName.setText(product.getName());
        } else {
            holder.productName.setText("Unknown Product");
        }

        holder.productPrice.setText(String.format("Rs. %.2f", product.getPrice()));

        // Stock status indicator
        boolean isOutOfStock = product.getStock() <= 0;
        if (holder.badgeOutOfStock != null) {
            if (isOutOfStock) {
                holder.badgeOutOfStock.setVisibility(View.VISIBLE);
                holder.productImage.setAlpha(0.6f);
            } else {
                holder.badgeOutOfStock.setVisibility(View.GONE);
                holder.productImage.setAlpha(1.0f);
            }
        }

        if (holder.productRating != null) {
            holder.productRating.setText("\u2605 " + String.format("%.1f", product.getRating()));
            if (holder.productReviews != null) {
                holder.productReviews.setText("(" + product.getReviewCount() + ")");
            }
        }

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(context)
                    .load(product.getImageUrl())
                    .into(holder.productImage);
        }

        // Wishlist button handling
        if (holder.btnWishlist != null && product.getId() != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            boolean isWishlisted = dbHelper.isInWishlist(product.getId());
            updateWishlistIcon(holder.btnWishlist, isWishlisted);

            holder.btnWishlist.setOnClickListener(v -> {
                if (!AuthHelper.isLoggedIn(context)) {
                    AuthHelper.showLoginPrompt(context, "Please log in to manage your wishlist.");
                    return;
                }

                // Micro-pulse animation
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                ).start();

                boolean currentlyWishlisted = dbHelper.isInWishlist(product.getId());
                if (currentlyWishlisted) {
                    dbHelper.removeFromWishlist(product.getId());
                    updateWishlistIcon(holder.btnWishlist, false);
                    Toast.makeText(context, "Removed from Wishlist", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.addToWishlist(product.getId(), product.getName(), product.getPrice(), product.getImageUrl());
                    updateWishlistIcon(holder.btnWishlist, true);
                    Toast.makeText(context, "Added to Wishlist!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), lk.prabhath.fashionhub.activity.ProductDetailsActivity.class);
            intent.putExtra("product_id", product.getId());
            intent.putExtra("product_name", product.getName());
            intent.putExtra("product_price", product.getPrice());
            intent.putExtra("product_image", product.getImageUrl());

            intent.putExtra("product_rating", product.getRating());
            intent.putExtra("product_reviews", product.getReviewCount());
            intent.putExtra("product_category", product.getCategory());
            intent.putExtra("product_stock", product.getStock());
            intent.putExtra("product_description", product.getDescription());
            v.getContext().startActivity(intent);
        });
    }

    private void updateWishlistIcon(ImageView btnWishlist, boolean isWishlisted) {
        if (isWishlisted) {
            btnWishlist.setImageResource(R.drawable.favorite_24px);
            btnWishlist.setColorFilter(Color.parseColor("#E11D48"));
        } else {
            btnWishlist.setImageResource(R.drawable.ic_nav_heart_round);
            btnWishlist.setColorFilter(Color.parseColor("#1C1917"));
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        ImageView btnWishlist;
        TextView productName, productPrice, productRating, productReviews, badgeOutOfStock;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            btnWishlist = itemView.findViewById(R.id.btn_card_wishlist);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productRating = itemView.findViewById(R.id.product_rating);
            productReviews = itemView.findViewById(R.id.product_reviews);
            badgeOutOfStock = itemView.findViewById(R.id.badge_out_of_stock);
        }
    }
}