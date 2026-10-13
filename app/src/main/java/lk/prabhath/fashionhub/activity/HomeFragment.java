package lk.prabhath.fashionhub.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.adapter.BannerAdapter;
import lk.prabhath.fashionhub.activity.adapter.CategoryAdapter;
import lk.prabhath.fashionhub.activity.adapter.ProductAdapter;
import lk.prabhath.fashionhub.activity.model.Banner;
import lk.prabhath.fashionhub.activity.model.Product;

public class HomeFragment extends Fragment {

    // Product variables
    private RecyclerView recyclerPopularItems;
    private ProductAdapter productAdapter;
    private List<Product> productList;
    private List<Product> originalProductList;

    // Category variables
    private RecyclerView recyclerCategories;
    private CategoryAdapter categoryAdapter;
    private List<String> categoryList;

    // Banner variables
    private View bannerContainer;
    private ViewPager2 viewPagerBanners;
    private LinearLayout layoutBannerDots;
    private BannerAdapter bannerAdapter;
    private List<Banner> bannerList;
    private Handler bannerSliderHandler;
    private Runnable bannerSliderRunnable;
    private ListenerRegistration bannerListenerRegistration;
    private ListenerRegistration productListenerRegistration;

    // Filter variables
    private String currentSearchText = "";
    private double currentMinPrice = 0.0;
    private double currentMaxPrice = Double.MAX_VALUE;
    private float currentMinRating = 0f;
    private int currentSortOption = 0; // 0=Default, 1=LowHigh, 2=HighLow, 3=TopRated

    // Selected category
    private String currentSelectedCategory = "";

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Setup products
        recyclerPopularItems = view.findViewById(R.id.recycler_popular_items);
        recyclerPopularItems.setLayoutManager(new GridLayoutManager(getContext(), 2));

        productList = new ArrayList<>();
        originalProductList = new ArrayList<>();
        productAdapter = new ProductAdapter(productList);
        recyclerPopularItems.setAdapter(productAdapter);

        // 2. Setup categories
        recyclerCategories = view.findViewById(R.id.recycler_categories);
        recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        categoryList = new ArrayList<>();

        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            filterByCategory(category);
        });
        recyclerCategories.setAdapter(categoryAdapter);

        // 3. Setup Banners Slider
        bannerContainer = view.findViewById(R.id.banner_container);
        viewPagerBanners = view.findViewById(R.id.view_pager_banners);
        layoutBannerDots = view.findViewById(R.id.layout_banner_dots);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (viewPagerBanners != null) {
            bannerList = new ArrayList<>();
            bannerAdapter = new BannerAdapter(bannerList, banner -> {
                if (banner.getCategory() != null && !banner.getCategory().isEmpty() && !banner.getCategory().equalsIgnoreCase("All")) {
                    filterByCategory(banner.getCategory());
                    Toast.makeText(getContext(), "Showing category: " + banner.getCategory(), Toast.LENGTH_SHORT).show();
                }
            });
            viewPagerBanners.setAdapter(bannerAdapter);

            viewPagerBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateBannerDots(position);
                }
            });

            // Real-time Live Banners listener
            setupRealtimeBanners(db);
            setupBannerAutoScroll();
        }

        lk.prabhath.fashionhub.activity.database.DatabaseHelper dbHelper = new lk.prabhath.fashionhub.activity.database.DatabaseHelper(getContext());

        // Real-time Live Products Listener with accurate Stock status
        productListenerRegistration = db.collection("products")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        List<Product> cached = dbHelper.getCachedProducts();
                        if (cached != null && !cached.isEmpty()) {
                            productList.clear();
                            originalProductList.clear();
                            categoryList.clear();
                            categoryList.add("All");
                            for (Product p : cached) {
                                productList.add(p);
                                originalProductList.add(p);
                                String cat = p.getCategory();
                                if (cat != null && !cat.isEmpty() && !categoryList.contains(cat)) {
                                    categoryList.add(cat);
                                }
                            }
                            applyAdvancedFilters();
                            categoryAdapter.notifyDataSetChanged();
                        }
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        productList.clear();
                        originalProductList.clear();

                        categoryList.clear();
                        categoryList.add("All");

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Product product = document.toObject(Product.class);
                            product.setId(document.getId());

                            // Parse Stock accurately (default 0 if missing or undefined)
                            Long stockVal = null;
                            try {
                                stockVal = document.getLong("stock");
                            } catch (Exception e) {
                                try {
                                    Double d = document.getDouble("stock");
                                    if (d != null) stockVal = d.longValue();
                                } catch (Exception ignored) {}
                            }
                            product.setStock(stockVal != null ? stockVal.intValue() : 0);

                            if (product.getName() == null || product.getName().isEmpty()) {
                                String n = document.getString("name");
                                if (n == null) n = document.getString("productName");
                                if (n != null) product.setName(n);
                            }

                            if (product.getDescription() == null || product.getDescription().isEmpty()) {
                                String desc = document.getString("description");
                                if (desc == null) desc = document.getString("desc");
                                if (desc != null) product.setDescription(desc);
                            }
                            productList.add(product);
                            originalProductList.add(product);

                            String cat = product.getCategory();
                            if (cat != null && !cat.isEmpty() && !categoryList.contains(cat)) {
                                categoryList.add(cat);
                            }
                        }

                        applyAdvancedFilters();
                        categoryAdapter.notifyDataSetChanged();
                        dbHelper.cacheProducts(productList);
                    }
                });

        return view;
    }

    // Real-time Banners Listener (Updates LIVE the second admin publishes in admin panel)
    private void setupRealtimeBanners(FirebaseFirestore db) {
        bannerListenerRegistration = db.collection("banners")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (querySnapshot != null) {
                        bannerList.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            try {
                                Banner banner = doc.toObject(Banner.class);
                                banner.setId(doc.getId());
                                if (doc.contains("active") && Boolean.FALSE.equals(doc.getBoolean("active"))) {
                                    continue; // Skip inactive banners
                                }
                                bannerList.add(banner);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        bannerAdapter.notifyDataSetChanged();

                        if (bannerList.isEmpty()) {
                            if (bannerContainer != null) bannerContainer.setVisibility(View.GONE);
                        } else {
                            if (bannerContainer != null) {
                                bannerContainer.setVisibility(View.VISIBLE);
                                setupBannerDots(bannerList.size());
                                if (viewPagerBanners != null) {
                                    updateBannerDots(viewPagerBanners.getCurrentItem());
                                }
                            }
                        }
                    }
                });
    }

    // Auto-scroll banners every 4 seconds
    private void setupBannerAutoScroll() {
        bannerSliderHandler = new Handler(Looper.getMainLooper());
        bannerSliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (viewPagerBanners != null && bannerList != null && bannerList.size() > 1) {
                    int nextItem = (viewPagerBanners.getCurrentItem() + 1) % bannerList.size();
                    viewPagerBanners.setCurrentItem(nextItem, true);
                }
                bannerSliderHandler.postDelayed(this, 4000);
            }
        };
        bannerSliderHandler.postDelayed(bannerSliderRunnable, 4000);
    }

    // Dynamic Dot Indicator setup
    private void setupBannerDots(int count) {
        if (layoutBannerDots == null || getContext() == null) return;
        layoutBannerDots.removeAllViews();

        if (count <= 1) return; // No dots needed for single banner

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.bg_circle_icon);
            dot.setAlpha(0.4f);
            layoutBannerDots.addView(dot);
        }
    }

    private void updateBannerDots(int position) {
        if (layoutBannerDots == null) return;
        int count = layoutBannerDots.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = layoutBannerDots.getChildAt(i);
            if (child != null) {
                if (i == position) {
                    child.setAlpha(1.0f);
                    child.setScaleX(1.3f);
                    child.setScaleY(1.3f);
                } else {
                    child.setAlpha(0.4f);
                    child.setScaleX(1.0f);
                    child.setScaleY(1.0f);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerSliderHandler != null && bannerSliderRunnable != null) {
            bannerSliderHandler.removeCallbacks(bannerSliderRunnable);
            bannerSliderHandler.postDelayed(bannerSliderRunnable, 4000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bannerSliderHandler != null && bannerSliderRunnable != null) {
            bannerSliderHandler.removeCallbacks(bannerSliderRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productListenerRegistration != null) {
            productListenerRegistration.remove();
        }
        if (bannerListenerRegistration != null) {
            bannerListenerRegistration.remove();
        }
        if (bannerSliderHandler != null && bannerSliderRunnable != null) {
            bannerSliderHandler.removeCallbacks(bannerSliderRunnable);
        }
    }

    // Normal search
    public void filterProducts(String text) {
        this.currentSearchText = text;
        applyAdvancedFilters();
    }

    // Category filter
    public void filterByCategory(String categoryName) {
        if (categoryName.equalsIgnoreCase("All")) {
            this.currentSelectedCategory = "";
        } else {
            this.currentSelectedCategory = categoryName;
        }
        applyAdvancedFilters();
    }

    // Advanced search
    public void showAdvanceSearchDialog() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_advance_search, null);
        bottomSheetDialog.setContentView(sheetView);

        com.google.android.material.slider.RangeSlider rsPriceRange = sheetView.findViewById(R.id.rs_price_range);
        TextView tvMinLabel = sheetView.findViewById(R.id.tv_min_price_label);
        TextView tvMaxLabel = sheetView.findViewById(R.id.tv_max_price_label);
        
        RadioButton rbSortNew = sheetView.findViewById(R.id.rb_sort_new);
        RadioButton rbLowHigh = sheetView.findViewById(R.id.rb_sort_low_high);
        RadioButton rbHighLow = sheetView.findViewById(R.id.rb_sort_high_low);
        RadioButton rbTopRated = sheetView.findViewById(R.id.rb_sort_rating);

        RadioButton rbCatAll = sheetView.findViewById(R.id.rb_cat_all);
        RadioButton rbCatMen = sheetView.findViewById(R.id.rb_cat_men);
        RadioButton rbCatWomen = sheetView.findViewById(R.id.rb_cat_women);
        RadioButton rbCatKids = sheetView.findViewById(R.id.rb_cat_kids);

        float minVal = (float) (currentMinPrice > 0 ? currentMinPrice : 0f);
        float maxVal = (float) (currentMaxPrice < 20000.0 ? currentMaxPrice : 20000f);
        if (minVal > 20000f) minVal = 20000f;
        if (maxVal > 20000f) maxVal = 20000f;
        if (minVal > maxVal) minVal = maxVal;
        
        rsPriceRange.setValues(minVal, maxVal);
        tvMinLabel.setText("Rs. " + (int) minVal);
        if (maxVal >= 20000f) tvMaxLabel.setText("Rs. 20000+");
        else tvMaxLabel.setText("Rs. " + (int) maxVal);

        rsPriceRange.addOnChangeListener((slider, value, fromUser) -> {
            java.util.List<Float> values = slider.getValues();
            int currentMin = values.get(0).intValue();
            int currentMax = values.get(1).intValue();
            tvMinLabel.setText("Rs. " + currentMin);
            if (currentMax >= 20000) tvMaxLabel.setText("Rs. 20000+");
            else tvMaxLabel.setText("Rs. " + currentMax);
        });

        if (currentSortOption == 1) rbLowHigh.setChecked(true);
        else if (currentSortOption == 2) rbHighLow.setChecked(true);
        else if (currentSortOption == 3) rbTopRated.setChecked(true);
        else rbSortNew.setChecked(true);

        if (currentSelectedCategory.equalsIgnoreCase("Men")) rbCatMen.setChecked(true);
        else if (currentSelectedCategory.equalsIgnoreCase("Women")) rbCatWomen.setChecked(true);
        else if (currentSelectedCategory.equalsIgnoreCase("Kids")) rbCatKids.setChecked(true);
        else rbCatAll.setChecked(true);

        sheetView.findViewById(R.id.btn_apply_filter).setOnClickListener(v -> {
            java.util.List<Float> values = rsPriceRange.getValues();
            float selMin = values.get(0);
            float selMax = values.get(1);

            currentMinPrice = selMin;
            currentMaxPrice = selMax >= 20000f ? Double.MAX_VALUE : selMax;

            if (rbLowHigh.isChecked()) currentSortOption = 1;
            else if (rbHighLow.isChecked()) currentSortOption = 2;
            else if (rbTopRated.isChecked()) currentSortOption = 3;
            else currentSortOption = 0;

            if (rbCatMen.isChecked()) currentSelectedCategory = "Men";
            else if (rbCatWomen.isChecked()) currentSelectedCategory = "Women";
            else if (rbCatKids.isChecked()) currentSelectedCategory = "Kids";
            else currentSelectedCategory = "";

            applyAdvancedFilters();
            bottomSheetDialog.dismiss();
        });

        sheetView.findViewById(R.id.btn_reset_filter).setOnClickListener(v -> {
            currentMinPrice = 0.0;
            currentMaxPrice = Double.MAX_VALUE;
            currentSortOption = 0;
            currentSelectedCategory = "";
            applyAdvancedFilters();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    // Apply filters
    private void applyAdvancedFilters() {
        if (originalProductList == null || productAdapter == null) return;

        List<Product> filteredList = new ArrayList<>();

        for (Product item : originalProductList) {
            boolean matchesName = true;
            boolean matchesPrice = true;
            boolean matchesRating = true;
            boolean matchesCategory = true;

            if (currentSearchText != null && !currentSearchText.isEmpty() && item.getName() != null) {
                matchesName = item.getName().toLowerCase().contains(currentSearchText.toLowerCase());
            }

            if (item.getPrice() < currentMinPrice || item.getPrice() > currentMaxPrice) {
                matchesPrice = false;
            }

            if (item.getRating() < currentMinRating) {
                matchesRating = false;
            }

            if (!currentSelectedCategory.isEmpty()) {
                if (item.getCategory() == null || !item.getCategory().equalsIgnoreCase(currentSelectedCategory)) {
                    matchesCategory = false;
                }
            }

            if (matchesName && matchesPrice && matchesRating && matchesCategory) {
                filteredList.add(item);
            }
        }

        if (currentSortOption == 1) {
            Collections.sort(filteredList, (p1, p2) -> Double.compare(p1.getPrice(), p2.getPrice()));
        } else if (currentSortOption == 2) {
            Collections.sort(filteredList, (p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()));
        } else if (currentSortOption == 3) {
            Collections.sort(filteredList, (p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));
        }

        productAdapter.setFilteredList(filteredList);

        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "No products", Toast.LENGTH_SHORT).show();
        }
    }
}
