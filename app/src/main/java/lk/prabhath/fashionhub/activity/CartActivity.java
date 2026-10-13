package lk.prabhath.fashionhub.activity;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.adapter.CartAdapter;
import lk.prabhath.fashionhub.activity.database.DatabaseHelper;
import lk.prabhath.fashionhub.util.AuthHelper;

// Implements listener
public class CartActivity extends AppCompatActivity implements SensorEventListener {

    private RecyclerView recyclerCartItems;
    private CartAdapter cartAdapter;
    private List<CartItem> cartList;
    private TextView tvTotalPrice;
    private View scrollCartContent;
    private View bottomCheckout;
    private View layoutEmptyCart;
    private double totalPrice = 0.0;

    private DatabaseHelper dbHelper;

    // Sensor variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn(this)) {
            Toast.makeText(this, "Please log in to access your cart", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CartActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_cart);

        dbHelper = new DatabaseHelper(this);

        // Back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar_cart);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Find views
        recyclerCartItems = findViewById(R.id.recycler_cart_items);
        tvTotalPrice = findViewById(R.id.tv_cart_total_price);
        scrollCartContent = findViewById(R.id.scroll_cart_content);
        bottomCheckout = findViewById(R.id.bottom_checkout_container);
        layoutEmptyCart = findViewById(R.id.layout_empty_cart);

        View btnExplore = findViewById(R.id.btn_empty_cart_explore);
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> finish());
        }

        // Setup list
        recyclerCartItems.setLayoutManager(new LinearLayoutManager(this));

        // Setup adapter
        cartList = new ArrayList<>();
        cartAdapter = new CartAdapter(cartList);
        recyclerCartItems.setAdapter(cartAdapter);

        // Init sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        acceleration = 10f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        // Load data
        loadCartItems();

        // Checkout button
        com.google.android.material.button.MaterialButton btnCheckout = findViewById(R.id.btn_checkout);

        btnCheckout.setOnClickListener(v -> {
            if (totalPrice > 0) {
                android.content.Intent intent = new android.content.Intent(CartActivity.this, CheckoutActivity.class);
                intent.putExtra("total_price", totalPrice);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(CartActivity.this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Register sensor
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // Unregister sensor
    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // Shake detection
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        lastAcceleration = currentAcceleration;
        currentAcceleration = (float) Math.sqrt((double) (x * x + y * y + z * z));
        float delta = currentAcceleration - lastAcceleration;
        acceleration = acceleration * 0.9f + delta;

        // Shake threshold
        if (acceleration > 12) {
            clearCartWithShake();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    // Clear cart
    private void clearCartWithShake() {
        // Check empty
        if (cartList == null || cartList.isEmpty()) {
            return;
        }

        dbHelper.clearCart(); // Clear DB
        loadCartItems();      // Clear UI

        // Vibrate phone
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(500);
        }

        Toast.makeText(this, "Cart Cleared by Shake!", Toast.LENGTH_SHORT).show();
    }

    public void loadCartItems() {
        cartList.clear();
        totalPrice = 0.0;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cart", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                String image = cursor.getString(cursor.getColumnIndexOrThrow("image"));
                String size = cursor.getString(cursor.getColumnIndexOrThrow("size"));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                String color = "Default";
                int colorIndex = cursor.getColumnIndex("color");
                if (colorIndex != -1) {
                    String c = cursor.getString(colorIndex);
                    if (c != null && !c.isEmpty()) color = c;
                }

                CartItem cartItem = new CartItem(name, price, image, size, color, qty);
                cartItem.setDocumentId(String.valueOf(id));

                cartList.add(cartItem);
                totalPrice += (price * qty);

            } while (cursor.moveToNext());
        }
        cursor.close();

        cartAdapter.notifyDataSetChanged();
        tvTotalPrice.setText(String.format("Rs. %.2f", totalPrice));

        if (cartList.isEmpty()) {
            if (layoutEmptyCart != null) layoutEmptyCart.setVisibility(View.VISIBLE);
            if (scrollCartContent != null) scrollCartContent.setVisibility(View.GONE);
            if (bottomCheckout != null) bottomCheckout.setVisibility(View.GONE);
        } else {
            if (layoutEmptyCart != null) layoutEmptyCart.setVisibility(View.GONE);
            if (scrollCartContent != null) scrollCartContent.setVisibility(View.VISIBLE);
            if (bottomCheckout != null) bottomCheckout.setVisibility(View.VISIBLE);
        }
    }
}