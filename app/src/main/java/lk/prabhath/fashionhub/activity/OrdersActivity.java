package lk.prabhath.fashionhub.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.adapter.OrderAdapter;
import lk.prabhath.fashionhub.activity.model.Order;
import lk.prabhath.fashionhub.util.AuthHelper;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private FirebaseFirestore db;
    private android.view.View layoutEmptyOrders;
    private android.widget.TextView tvOrdersBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn(this)) {
            Toast.makeText(this, "Please log in to view your orders", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(OrdersActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_orders);

        android.view.View btnBack = findViewById(R.id.btn_orders_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvOrdersBadge = findViewById(R.id.tv_orders_badge);
        layoutEmptyOrders = findViewById(R.id.layout_empty_orders);

        android.view.View btnStartShopping = findViewById(R.id.btn_start_shopping);
        if (btnStartShopping != null) {
            btnStartShopping.setOnClickListener(v -> {
                Intent intent = new Intent(OrdersActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        recyclerOrders = findViewById(R.id.recycler_orders);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList);
        recyclerOrders.setAdapter(orderAdapter);

        db = FirebaseFirestore.getInstance();

        loadOrders();
    }

    private void loadOrders() {
        String userEmail = AuthHelper.getUserEmail(this);
        Query query;
        if (!userEmail.isEmpty()) {
            query = db.collection("orders").whereEqualTo("userEmail", userEmail);
        } else {
            query = db.collection("orders");
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        orderList.add(order);
                    }
                    orderAdapter.notifyDataSetChanged();

                    if (orderList.isEmpty()) {
                        if (layoutEmptyOrders != null) layoutEmptyOrders.setVisibility(android.view.View.VISIBLE);
                        if (recyclerOrders != null) recyclerOrders.setVisibility(android.view.View.GONE);
                        if (tvOrdersBadge != null) tvOrdersBadge.setText("0 Orders");
                    } else {
                        if (layoutEmptyOrders != null) layoutEmptyOrders.setVisibility(android.view.View.GONE);
                        if (recyclerOrders != null) recyclerOrders.setVisibility(android.view.View.VISIBLE);
                        if (tvOrdersBadge != null) tvOrdersBadge.setText(orderList.size() + (orderList.size() == 1 ? " Order" : " Orders"));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                    if (orderList.isEmpty()) {
                        if (layoutEmptyOrders != null) layoutEmptyOrders.setVisibility(android.view.View.VISIBLE);
                        if (recyclerOrders != null) recyclerOrders.setVisibility(android.view.View.GONE);
                    }
                });
    }
}