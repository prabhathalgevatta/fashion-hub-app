package lk.prabhath.fashionhub.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.adapter.WishlistAdapter;
import lk.prabhath.fashionhub.activity.database.DatabaseHelper;
import lk.prabhath.fashionhub.activity.model.WishlistItem;
import lk.prabhath.fashionhub.util.AuthHelper;

public class WishlistActivity extends AppCompatActivity {

    private RecyclerView recyclerWishlist;
    private WishlistAdapter adapter;
    private List<WishlistItem> wishlist;
    private TextView tvEmpty;
    private View layoutEmpty;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn(this)) {
            Toast.makeText(this, "Please log in to view your wishlist", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(WishlistActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_wishlist);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_wishlist);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerWishlist = findViewById(R.id.recycler_wishlist_items);
        tvEmpty = findViewById(R.id.tv_empty_wishlist);
        layoutEmpty = findViewById(R.id.layout_empty_wishlist);

        View btnExplore = findViewById(R.id.btn_empty_wishlist_explore);
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> finish());
        }

        recyclerWishlist.setLayoutManager(new LinearLayoutManager(this));
        wishlist = new ArrayList<>();
        adapter = new WishlistAdapter(wishlist);
        recyclerWishlist.setAdapter(adapter);

        loadWishlistItems();
    }

    public void loadWishlistItems() {
        wishlist.clear();
        Cursor cursor = dbHelper.getWishlistCursor();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String productId = cursor.getString(cursor.getColumnIndexOrThrow("product_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                String image = cursor.getString(cursor.getColumnIndexOrThrow("image"));

                wishlist.add(new WishlistItem(productId, name, price, image));
            } while (cursor.moveToNext());
            cursor.close();
        }

        adapter.notifyDataSetChanged();

        if (wishlist.isEmpty()) {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerWishlist.setVisibility(View.GONE);
        } else {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            recyclerWishlist.setVisibility(View.VISIBLE);
        }
    }
}
