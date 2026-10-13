package lk.prabhath.fashionhub.activity.adapter;

import android.content.Context;
import android.content.DialogInterface;
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

import java.util.List;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.CartActivity;
import lk.prabhath.fashionhub.activity.CartItem;
import lk.prabhath.fashionhub.activity.database.DatabaseHelper;


public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartList;

    public CartAdapter(List<CartItem> cartList) {
        this.cartList = cartList;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartList.get(position);
        Context context = holder.itemView.getContext();

        // Set details
        holder.itemName.setText(cartItem.getProductName());
        String colorInfo = (cartItem.getSelectedColor() != null && !cartItem.getSelectedColor().isEmpty() && !cartItem.getSelectedColor().equalsIgnoreCase("Default"))
                ? " • " + cartItem.getSelectedColor() : "";
        holder.itemSize.setText("Size: " + cartItem.getSelectedSize() + colorInfo);
        holder.itemQty.setText("Qty: " + cartItem.getQuantity());

        holder.itemPrice.setText(String.format("Rs. %.2f", cartItem.getProductPrice()));

        if (cartItem.getProductImage() != null && !cartItem.getProductImage().isEmpty()) {
            Glide.with(context).load(cartItem.getProductImage()).into(holder.itemImage);
        }

        // Handle delete
        holder.btnDelete.setOnClickListener(v -> {
            // Confirm deletion
            new AlertDialog.Builder(context)
                    .setTitle("Remove Item")
                    .setMessage("Are you sure you want to remove this item from the cart?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseHelper dbHelper = new DatabaseHelper(context);
                        boolean isDeleted = dbHelper.deleteCartItem(cartItem.getDocumentId());

                        if (isDeleted) {
                            Toast.makeText(context, "Item Removed!", Toast.LENGTH_SHORT).show();
                            // Refresh cart
                            if (context instanceof CartActivity) {
                                ((CartActivity) context).loadCartItems();
                            }
                        } else {
                            Toast.makeText(context, "Failed to remove item!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage, btnDelete;
        TextView itemName, itemPrice, itemSize, itemQty;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.cart_item_image);
            itemName = itemView.findViewById(R.id.cart_item_title);
            itemPrice = itemView.findViewById(R.id.cart_item_price);
            btnDelete = itemView.findViewById(R.id.cart_item_delete);

            // Bind views
            itemSize = itemView.findViewById(R.id.cart_item_size);
            itemQty = itemView.findViewById(R.id.cart_item_qty);
        }
    }
}