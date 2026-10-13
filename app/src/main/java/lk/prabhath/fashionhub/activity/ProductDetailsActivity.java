package lk.prabhath.fashionhub.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.MotionEvent;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.database.DatabaseHelper;
import lk.prabhath.fashionhub.util.AuthHelper;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;
    private String productId;
    private String selectedColor = "Charcoal Black";
    private int selectedQuantity = 1;
    private double currentUnitPrice = 0.0;
    private int currentAvailableStock = 0;
    private double currentProductRating = 0.0;
    private int currentProductReviews = 0;
    private com.google.firebase.firestore.ListenerRegistration productStockListenerReg;
    
    
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        // 1. Find views
        TextView nameText = findViewById(R.id.detail_name);
        TextView priceText = findViewById(R.id.detail_price);
        ViewPager2 imagePager = findViewById(R.id.detail_image_pager);
        imagePager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        imagePager.setOffscreenPageLimit(1);

        // Find new views
        TextView descText = findViewById(R.id.detail_description);
        RatingBar ratingBar = findViewById(R.id.detail_rating_bar);
        TextView reviewCountText = findViewById(R.id.detail_review_count);

        // 2. Get details
        productId = getIntent().getStringExtra("product_id");
        String productName = getIntent().getStringExtra("product_name");
        double productPrice = getIntent().getDoubleExtra("product_price", 0.0);
        String productImage = getIntent().getStringExtra("product_image");
        String productDesc = getIntent().getStringExtra("product_description");
        double productRating = getIntent().getDoubleExtra("product_rating", 0.0);
        int productReviews = getIntent().getIntExtra("product_reviews", 0);
        currentProductRating = productRating;
        currentProductReviews = productReviews;
        int productStock = getIntent().getIntExtra("product_stock", 0);
        String productCategory = getIntent().getStringExtra("product_category");

        // Zoom is handled by PhotoView automatically.

        TextView headerPriceText = findViewById(R.id.detail_header_price);
        if (headerPriceText != null) headerPriceText.setText(String.format("Rs. %.2f", productPrice));
        updateStockDisplay(findViewById(R.id.btn_add_to_cart));

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnCartTop = findViewById(R.id.btn_cart_top);
        if (btnCartTop != null) {
            btnCartTop.setOnClickListener(v -> {
                if (currentAvailableStock <= 0) {
                    Toast.makeText(ProductDetailsActivity.this, "Sorry, this product is currently out of stock!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!AuthHelper.isLoggedIn(ProductDetailsActivity.this)) {
                    AuthHelper.showLoginPrompt(ProductDetailsActivity.this, "Please log in to view your cart.");
                    return;
                }
                startActivity(new Intent(ProductDetailsActivity.this, CartActivity.class));
            });
        }

        // 3. Set details immediately without network delay
        if (productName != null) nameText.setText(productName);
        priceText.setText(String.format("Rs. %.2f", productPrice));
        List<String> imageUrls = new ArrayList<>();
        addImageUrl(imageUrls, productImage);
        imagePager.setAdapter(new ProductImagePagerAdapter(imageUrls, imagePager));

        if (productDesc != null && !productDesc.isEmpty()) {
            descText.setText(productDesc);
        } else {
            java.util.Map<String, Object> cached = dbHelper.getCachedProduct(productId);
            if (cached != null) {
                String cDesc = (String) cached.get("description");
                if (cDesc != null && !cDesc.isEmpty()) {
                    descText.setText(cDesc);
                }
            }
        }

        if (productRating > 0) {
            ratingBar.setRating((float) productRating);
        }
        if (productReviews > 0) {
            reviewCountText.setText("(" + productReviews + " Reviews)");
        } else {
            reviewCountText.setText("(No reviews yet)");
        }

        currentAvailableStock = productStock;
        MaterialButton btnAddToCartImmediate = findViewById(R.id.btn_add_to_cart);
        updateStockDisplay(btnAddToCartImmediate);

        // Real-time stock listener
        if (productId != null) {
            productStockListenerReg = db.collection("products").document(productId)
                    .addSnapshotListener((doc, err) -> {
                        if (err != null || doc == null || !doc.exists()) return;
                        Long sVal = null;
                        try {
                            sVal = doc.getLong("stock");
                        } catch (Exception e) {
                            try {
                                Double d = doc.getDouble("stock");
                                if (d != null) sVal = d.longValue();
                            } catch (Exception ignored) {}
                        }
                        currentAvailableStock = (sVal != null) ? sVal.intValue() : 0;
                        updateStockDisplay(findViewById(R.id.btn_add_to_cart));
                    });
        }

        // 4. Fetch latest data & Cache in background
        if (productId != null) {
            dbHelper.saveRecentlyViewedProduct(productId, productName, productPrice, productImage, productCategory, productDesc, productRating, productReviews);

            db.collection("products").document(productId).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String desc = document.getString("description");
                            if (desc != null && !desc.isEmpty()) {
                                descText.setText(desc);
                            }

                            Double rating = document.getDouble("rating");
                            Long reviewCount = document.getLong("reviewCount");
                            String category = document.getString("category");
                            List<String> firestoreImages = getImageUrls(document);
                            if (!firestoreImages.isEmpty()) {
                                imagePager.setAdapter(new ProductImagePagerAdapter(firestoreImages, imagePager));
                            }

                            if (rating != null) ratingBar.setRating(rating.floatValue());
                            if (reviewCount != null) reviewCountText.setText("(" + reviewCount + " Reviews)");

                            Long stockVal = null;
                            try {
                                stockVal = document.getLong("stock");
                            } catch (Exception e) {
                                try {
                                    Double d = document.getDouble("stock");
                                    if (d != null) stockVal = d.longValue();
                                } catch (Exception ignored) {}
                            }
                            currentAvailableStock = (stockVal != null) ? stockVal.intValue() : 0;
                            MaterialButton btnAddToCartRef = findViewById(R.id.btn_add_to_cart);
                            updateStockDisplay(btnAddToCartRef);

                            dbHelper.saveRecentlyViewedProduct(productId, productName, productPrice, productImage,
                                    category, desc, rating != null ? rating : productRating, reviewCount != null ? reviewCount.intValue() : productReviews);
                        }
                    });
        }

        // 5. Save one rating per logged-in user
        if (ratingBar != null) {
            ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
                if (fromUser && productId != null) {
                    if (rating < 1) {
                        bar.setRating(0);
                        return;
                    }
                    if (!AuthHelper.isLoggedIn(ProductDetailsActivity.this)) {
                        bar.setRating((float) productRating);
                        AuthHelper.showLoginPrompt(ProductDetailsActivity.this, "Please log in to rate this product.");
                        return;
                    }

                    com.google.firebase.firestore.DocumentReference docRef = db.collection("products").document(productId);
                    String userId = AuthHelper.getUserEmail(ProductDetailsActivity.this)
                            .replaceAll("[^A-Za-z0-9_.@-]", "_");
                    com.google.firebase.firestore.DocumentReference userRatingRef =
                            docRef.collection("ratings").document(userId);
                    db.runTransaction(transaction -> {
                        com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(docRef);
                        com.google.firebase.firestore.DocumentSnapshot previousRating =
                                transaction.get(userRatingRef);
                        if (previousRating.exists()) {
                            return -1.0;
                        }

                        Double currentRating = snapshot.getDouble("rating");
                        if (currentRating == null) currentRating = 0.0;
                        Long currentReviewCount = snapshot.getLong("reviewCount");
                        if (currentReviewCount == null) currentReviewCount = 0L;

                        double newAvgRating = ((currentRating * currentReviewCount) + rating) / (currentReviewCount + 1);
                        long newReviewCount = currentReviewCount + 1;

                        transaction.update(docRef, "rating", newAvgRating);
                        transaction.update(docRef, "reviewCount", newReviewCount);
                        java.util.Map<String, Object> ratingData = new java.util.HashMap<>();
                        ratingData.put("rating", rating);
                        ratingData.put("userEmail", AuthHelper.getUserEmail(ProductDetailsActivity.this));
                        ratingData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        transaction.set(userRatingRef, ratingData);

                        return newAvgRating;
                    }).addOnSuccessListener(newAvg -> {
                        if (newAvg < 0) {
                            ratingBar.setRating((float) currentProductRating);
                            Toast.makeText(this, "You have already rated this product.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ratingBar.setRating(newAvg.floatValue());
                        currentProductRating = newAvg;
                        currentProductReviews++;
                        reviewCountText.setText("(" + currentProductReviews + " Reviews)");
                        Toast.makeText(this, "Thank you for rating!", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        ratingBar.setRating((float) productRating);
                        Toast.makeText(this, "Failed to save rating. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }

        // 6. Call & SMS Telephony button
        MaterialButton btnCallStore = findViewById(R.id.btn_call_store);
        if (btnCallStore != null) {
            btnCallStore.setOnClickListener(v -> {
                String storeNumber = "0712345678";
                String[] options = {"📞 Call Store Support", "💬 Send SMS Inquiry"};

                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Contact Store Support")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                // Telephony: Call dialer
                                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                                dialIntent.setData(Uri.parse("tel:" + storeNumber));
                                startActivity(dialIntent);
                            } else {
                                // Telephony: Send SMS
                                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                smsIntent.setData(Uri.parse("smsto:" + storeNumber));
                                String message = "Hello FashionHub, I would like to inquire about: " +
                                        (productName != null ? productName : "this product") +
                                        " (Price: Rs. " + String.format("%.2f", productPrice) + ")";
                                smsIntent.putExtra("sms_body", message);
                                startActivity(smsIntent);
                            }
                        })
                        .show();
            });
        }

        // Wishlist & Share buttons
        View btnShare = findViewById(R.id.btn_share);
        ImageView btnFavorite = findViewById(R.id.btn_favorite);

        if (btnFavorite != null) {
            if (productId != null) {
                boolean isWishlisted = dbHelper.isInWishlist(productId);
                updateFavoriteIcon(btnFavorite, isWishlisted);
            }

            btnFavorite.setOnClickListener(v -> {
                if (productId == null) return;
                if (!AuthHelper.isLoggedIn(ProductDetailsActivity.this)) {
                    AuthHelper.showLoginPrompt(ProductDetailsActivity.this, "Please log in to manage your wishlist.");
                    return;
                }
                // Micro-animation: Heart beat pulse effect
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                ).start();

                boolean currentlyWishlisted = dbHelper.isInWishlist(productId);
                if (currentlyWishlisted) {
                    dbHelper.removeFromWishlist(productId);
                    updateFavoriteIcon(btnFavorite, false);
                    Toast.makeText(ProductDetailsActivity.this, "Removed from Wishlist", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.addToWishlist(productId, productName, productPrice, productImage);
                    updateFavoriteIcon(btnFavorite, true);
                    Toast.makeText(ProductDetailsActivity.this, "Added to Wishlist!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareBody = "Check out this product on FashionHub!\n" + productName + "\nPrice: Rs. " + String.format("%.2f", productPrice);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, productName != null ? productName : "FashionHub Product");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(shareIntent, "Share Product Via"));
            });
        }

        // Color selection handling
        TextView tvSelectedColorName = findViewById(R.id.tv_selected_color_name);
        View btnColor1 = findViewById(R.id.btn_color_1);
        View btnColor2 = findViewById(R.id.btn_color_2);
        View btnColor3 = findViewById(R.id.btn_color_3);
        View btnColor4 = findViewById(R.id.btn_color_4);

        View.OnClickListener colorClickListener = v -> {
            if (btnColor1 != null) btnColor1.setBackgroundResource(R.drawable.bg_color_ring_inactive);
            if (btnColor2 != null) btnColor2.setBackgroundResource(R.drawable.bg_color_ring_inactive);
            if (btnColor3 != null) btnColor3.setBackgroundResource(R.drawable.bg_color_ring_inactive);
            if (btnColor4 != null) btnColor4.setBackgroundResource(R.drawable.bg_color_ring_inactive);

            v.setBackgroundResource(R.drawable.bg_color_ring_active);

            int id = v.getId();
            if (id == R.id.btn_color_1) {
                selectedColor = "Charcoal Black";
            } else if (id == R.id.btn_color_2) {
                selectedColor = "Warm Camel";
            } else if (id == R.id.btn_color_3) {
                selectedColor = "Desert Sand";
            } else if (id == R.id.btn_color_4) {
                selectedColor = "Olive Sage";
            }

            if (tvSelectedColorName != null) {
                tvSelectedColorName.setText(selectedColor);
            }
        };

        if (btnColor1 != null) btnColor1.setOnClickListener(colorClickListener);
        if (btnColor2 != null) btnColor2.setOnClickListener(colorClickListener);
        if (btnColor3 != null) btnColor3.setOnClickListener(colorClickListener);
        if (btnColor4 != null) btnColor4.setOnClickListener(colorClickListener);

        // Quantity counter handling
        TextView tvProductQty = findViewById(R.id.tv_product_qty);
        View btnQtyMinus = findViewById(R.id.btn_qty_minus);
        View btnQtyPlus = findViewById(R.id.btn_qty_plus);
        TextView detailPriceBottom = findViewById(R.id.detail_price);
        currentUnitPrice = productPrice;

        if (btnQtyMinus != null && tvProductQty != null) {
            btnQtyMinus.setOnClickListener(v -> {
                if (selectedQuantity > 1) {
                    selectedQuantity--;
                    tvProductQty.setText(String.valueOf(selectedQuantity));
                    if (detailPriceBottom != null) {
                        detailPriceBottom.setText(String.format("Rs. %.2f", currentUnitPrice * selectedQuantity));
                    }
                }
            });
        }

        if (btnQtyPlus != null && tvProductQty != null) {
            btnQtyPlus.setOnClickListener(v -> {
                if (selectedQuantity < Math.min(99, currentAvailableStock)) {
                    selectedQuantity++;
                    tvProductQty.setText(String.valueOf(selectedQuantity));
                    if (detailPriceBottom != null) {
                        detailPriceBottom.setText(String.format("Rs. %.2f", currentUnitPrice * selectedQuantity));
                    }
                }
            });
        }

        // Cart button
        RadioGroup radioGroupSize = findViewById(R.id.radio_group_size);
        MaterialButton btnAddToCart = findViewById(R.id.btn_add_to_cart);

        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                // Button feedback animation
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                ).start();

                if (!AuthHelper.isLoggedIn(ProductDetailsActivity.this)) {
                    AuthHelper.showLoginPrompt(ProductDetailsActivity.this, "Please log in to add products to your cart.");
                    return;
                }

                String selectedSize = "M"; // default fallback
                if (radioGroupSize != null) {
                    int selectedId = radioGroupSize.getCheckedRadioButtonId();
                    if (selectedId != -1) {
                        RadioButton selectedRadioButton = findViewById(selectedId);
                        if (selectedRadioButton != null) {
                            selectedSize = selectedRadioButton.getText().toString();
                        }
                    }
                }

                // Insert or increment quantity in cart
                boolean isInserted = dbHelper.addToCart(productId, productName, productPrice, productImage, selectedSize, selectedColor, selectedQuantity, productCategory);

                if (isInserted) {
                    Toast.makeText(ProductDetailsActivity.this, "Added (" + selectedQuantity + ") " + selectedColor + " to Cart!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProductDetailsActivity.this, "Failed to Add to Cart!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    
    private void updateStockDisplay(MaterialButton btnAddToCart) {
        TextView tvStockBadge = findViewById(R.id.tv_stock_badge);
        if (tvStockBadge != null) {
            if (currentAvailableStock <= 0) {
                tvStockBadge.setVisibility(android.view.View.VISIBLE);
                tvStockBadge.setText("OUT OF STOCK");
                tvStockBadge.setTextColor(android.graphics.Color.parseColor("#E11D48"));
                tvStockBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFE4E6")));
            } else if (currentAvailableStock <= 5) {
                tvStockBadge.setVisibility(android.view.View.VISIBLE);
                tvStockBadge.setText("ONLY " + currentAvailableStock + " LEFT IN STOCK");
                tvStockBadge.setTextColor(android.graphics.Color.parseColor("#D97706"));
                tvStockBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF3C7")));
            } else {
                tvStockBadge.setVisibility(android.view.View.VISIBLE);
                tvStockBadge.setText("IN STOCK (" + currentAvailableStock + ")");
                tvStockBadge.setTextColor(android.graphics.Color.parseColor("#059669"));
                tvStockBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D1FAE5")));
            }
        }

        if (btnAddToCart == null) return;
        btnAddToCart.setBackgroundResource(R.drawable.bg_button_get_started);
        if (currentAvailableStock <= 0) {
            btnAddToCart.setText("OUT OF STOCK");
            btnAddToCart.setEnabled(false);
            btnAddToCart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#8C857B")));
        } else if (currentAvailableStock <= 5) {
            btnAddToCart.setText("ADD TO CART (" + currentAvailableStock + " LEFT)");
            btnAddToCart.setEnabled(true);
            btnAddToCart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BA770C")));
        } else {
            btnAddToCart.setText("ADD TO CART");
            btnAddToCart.setEnabled(true);
            btnAddToCart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BA770C")));
        }
    }

    private void updateFavoriteIcon(ImageView btnFavorite, boolean isWishlisted) {
        if (btnFavorite == null) return;
        if (isWishlisted) {
            btnFavorite.setImageResource(R.drawable.favorite_24px);
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#E11D48"));
        } else {
            btnFavorite.setImageResource(R.drawable.favorite_24px);
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#8C857B"));
        }
    }

    private void loadProductFromCache(String id, TextView nameText, TextView priceText, ImageView imageView,
                                      TextView descText, RatingBar ratingBar, TextView reviewCountText) {
        java.util.Map<String, Object> cached = dbHelper.getCachedProduct(id);
        if (cached != null) {
            String name = (String) cached.get("name");
            Double price = (Double) cached.get("price");
            String image = (String) cached.get("image");
            String desc = (String) cached.get("description");
            Double rating = (Double) cached.get("rating");
            Integer reviewCount = (Integer) cached.get("reviewCount");

            if (name != null && !name.isEmpty()) {
                nameText.setText(name);
            }
            if (price != null && price > 0) {
                priceText.setText(String.format("Rs. %.2f", price));
                TextView headerPriceText = findViewById(R.id.detail_header_price);
                if (headerPriceText != null) headerPriceText.setText(String.format("Rs. %.2f", price));
            }
            if (image != null && !image.isEmpty() && !isFinishing() && !isDestroyed()) {
                Glide.with(this).load(image).into(imageView);
            }
            if (desc != null && !desc.isEmpty()) {
                descText.setText(desc);
            } else {
                descText.setText("No description available (Offline Mode).");
            }
            if (rating != null && rating > 0) {
                ratingBar.setRating(rating.floatValue());
            }
            if (reviewCount != null && reviewCount > 0) {
                reviewCountText.setText("(" + reviewCount + " Reviews)");
            }
            Toast.makeText(this, "Displaying cached offline product details.", Toast.LENGTH_SHORT).show();
        }
    }

    private static void addImageUrl(List<String> imageUrls, String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrls.contains(imageUrl.trim())) {
            imageUrls.add(imageUrl.trim());
        }
    }

    private static List<String> getImageUrls(com.google.firebase.firestore.DocumentSnapshot document) {
        List<String> imageUrls = new ArrayList<>();
        addImageUrl(imageUrls, document.getString("imageUrl"));
        addImageUrl(imageUrls, document.getString("image"));
        Object imageList = document.get("imageUrls");
        if (!(imageList instanceof List)) {
            imageList = document.get("images");
        }
        if (imageList instanceof List) {
            for (Object image : (List<?>) imageList) {
                if (image instanceof String) {
                    addImageUrl(imageUrls, (String) image);
                }
            }
        }
        return imageUrls;
    }

    private class ProductImagePagerAdapter extends RecyclerView.Adapter<ProductImagePagerAdapter.ImageViewHolder> {
        private final List<String> imageUrls;
        private final ViewPager2 imagePager;

        ProductImagePagerAdapter(List<String> imageUrls, ViewPager2 imagePager) {
            this.imageUrls = imageUrls;
            this.imagePager = imagePager;
        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            PhotoView imageView = new PhotoView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setMaximumScale(3.5f);
            imageView.setMediumScale(2.0f);
            imageView.setMinimumScale(1.0f);
            imageView.setZoomTransitionDuration(200);
            imageView.setAllowParentInterceptOnEdge(true);
            imageView.setZoomable(true);
            imageView.setOnTouchListener((view, event) -> {
                PhotoView photoView = (PhotoView) view;
                boolean zoomed = photoView.getScale() > photoView.getMinimumScale() + 0.01f;

                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    imagePager.setUserInputEnabled(true);
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN
                        || event.getPointerCount() > 1 || zoomed) {
                    imagePager.setUserInputEnabled(false);
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    imagePager.setUserInputEnabled(true);
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    imagePager.setUserInputEnabled(true);
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            holder.imageView.setScale(1.0f, false);
            Glide.with(ProductDetailsActivity.this)
                    .load(imageUrls.get(position))
                    .fitCenter()
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            final PhotoView imageView;

            ImageViewHolder(PhotoView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productStockListenerReg != null) {
            productStockListenerReg.remove();
            productStockListenerReg = null;
        }
    }
}