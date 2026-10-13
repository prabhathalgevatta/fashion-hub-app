package lk.prabhath.fashionhub.activity;

import android.util.Log;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.FileOutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;







import org.osmdroid.util.GeoPoint;




import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;
import lk.payhere.androidsdk.model.StatusResponse;
import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.database.DatabaseHelper;
import lk.prabhath.fashionhub.activity.receiver.OrderNotificationReceiver;
import lk.prabhath.fashionhub.util.AuthHelper;

public class CheckoutActivity extends AppCompatActivity  {

    private TextInputEditText etName, etPhone, etAddress;
    private double orderTotal = 0.0;
    private FirebaseFirestore db;
    private MaterialButton btnPlaceOrder;

    private String currentOrderId;
    private static final int PAYHERE_REQ_CODE = 11010;

    // Map variables & Flagship Store coordinates (Colombo)
    private static final GeoPoint STORE_LOCATION = new GeoPoint(6.9271, 79.8612);
    private org.osmdroid.views.MapView mMap;
    private GeoPoint selectedLocation = null;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn(this)) {
            Toast.makeText(this, "Please log in to proceed to checkout", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CheckoutActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();
        currentOrderId = UUID.randomUUID().toString();

        View btnBack = findViewById(R.id.btn_checkout_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        etName = findViewById(R.id.et_checkout_name);
        etPhone = findViewById(R.id.et_checkout_phone);
        etAddress = findViewById(R.id.et_checkout_address);
        if (etAddress != null) {
            etAddress.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    String query = etAddress.getText().toString().trim();
                    if (!query.isEmpty()) {
                        geocodeAndShowOnMap(query);
                    }
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(etAddress.getWindowToken(), 0);
                    return true;
                }
                return false;
            });
        }
        
        etAddress.setOnLongClickListener(v -> {
            showSavedAddressesDialog();
            return true;
        });
        
        etAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                Toast.makeText(this, "Hint: Long-press here to load a saved address", Toast.LENGTH_SHORT).show();
            }
        });

        TextView tvTotal = findViewById(R.id.tv_checkout_total);
        btnPlaceOrder = findViewById(R.id.btn_place_order);

        String username = AuthHelper.getUsername(this);
        if (username != null && !username.isEmpty()) {
            etName.setText(username);
        }

        // Autofill contact and address from Firestore if available & verify suspension
        String userEmail = AuthHelper.getUserEmail(this);
        if (!userEmail.isEmpty()) {
            db.collection("users").document(userEmail).get()
                    .addOnSuccessListener(document -> {
                        if (document != null && document.exists()) {
                            String status = document.getString("status");
                            if (status != null && "Suspended".equalsIgnoreCase(status.trim())) {
                                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
                                Toast.makeText(CheckoutActivity.this, "Your account has been suspended by the administrator.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CheckoutActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                                return;
                            }
                            String savedName = document.getString("name");
                            String savedPhone = document.getString("phone");
                            String savedAddress = document.getString("address");

                            if (savedName != null && !savedName.isEmpty() && etName.getText().toString().isEmpty()) {
                                etName.setText(savedName);
                            }
                            if (savedPhone != null && !savedPhone.isEmpty() && etPhone.getText().toString().isEmpty()) {
                                etPhone.setText(savedPhone);
                            }
                            if (savedAddress != null && !savedAddress.isEmpty() && etAddress.getText().toString().isEmpty()) {
                                etAddress.setText(savedAddress);
                            }
                        }
                    });
        }

        orderTotal = getIntent().getDoubleExtra("total_price", 0.0);
        tvTotal.setText(String.format("Rs. %.2f", orderTotal));

        // Init map
        org.osmdroid.config.Configuration.getInstance().load(getApplicationContext(), android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        mMap = findViewById(R.id.map_checkout);
        mMap.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true);
        mMap.getController().setZoom(12.0);
        mMap.getController().setCenter(STORE_LOCATION);
        
        org.osmdroid.views.overlay.Marker storeMarker = new org.osmdroid.views.overlay.Marker(mMap);
        storeMarker.setPosition(STORE_LOCATION);
        storeMarker.setTitle("FashionHub Flagship Store");
        storeMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        mMap.getOverlays().add(storeMarker);

        org.osmdroid.events.MapEventsReceiver mReceive = new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(org.osmdroid.util.GeoPoint p) {
                handleMapSelection(p);
                return true;
            }
            @Override
            public boolean longPressHelper(org.osmdroid.util.GeoPoint p) { return false; }
        };
        mMap.getOverlays().add(new org.osmdroid.views.overlay.MapEventsOverlay(mReceive));

        btnPlaceOrder.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedLocation == null) {
                Toast.makeText(this, "Please tap on the map to select delivery location!", Toast.LENGTH_LONG).show();
                return;
            }

            btnPlaceOrder.setEnabled(false);
            btnPlaceOrder.setText("PROCESSING PAYMENT...");

            startPayHerePayment(name, phone, address);
        });
    }

    private void geocodeAndShowOnMap(String address) {
        if (address == null || address.trim().isEmpty()) return;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(CheckoutActivity.this, java.util.Locale.getDefault());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(address, 1, addresses -> {
                        if (addresses != null && !addresses.isEmpty()) {
                            android.location.Address addr = addresses.get(0);
                            org.osmdroid.util.GeoPoint latLng = new org.osmdroid.util.GeoPoint(addr.getLatitude(), addr.getLongitude());
                            runOnUiThread(() -> handleMapSelection(latLng));
                        }
                    });
                } else {
                    java.util.List<android.location.Address> addresses = geocoder.getFromLocationName(address, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        android.location.Address addr = addresses.get(0);
                        org.osmdroid.util.GeoPoint latLng = new org.osmdroid.util.GeoPoint(addr.getLatitude(), addr.getLongitude());
                        runOnUiThread(() -> handleMapSelection(latLng));
                    }
                }
            } catch (Exception e) {
                Log.e("Geocoder", "Error forward geocoding", e);
            }
        });
    }

    private void handleMapSelection(org.osmdroid.util.GeoPoint latLng) {
        if (mMap == null) return;
        mMap.getOverlays().clear();

        org.osmdroid.events.MapEventsReceiver mReceive = new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(org.osmdroid.util.GeoPoint p) {
                handleMapSelection(p);
                return true;
            }
            @Override
            public boolean longPressHelper(org.osmdroid.util.GeoPoint p) { return false; }
        };
        mMap.getOverlays().add(new org.osmdroid.views.overlay.MapEventsOverlay(mReceive));

        org.osmdroid.views.overlay.Marker storeMarker = new org.osmdroid.views.overlay.Marker(mMap);
        storeMarker.setPosition(STORE_LOCATION);
        storeMarker.setTitle("FashionHub Flagship Store");
        storeMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        mMap.getOverlays().add(storeMarker);

        org.osmdroid.views.overlay.Marker custMarker = new org.osmdroid.views.overlay.Marker(mMap);
        custMarker.setPosition(latLng);
        custMarker.setTitle("Delivery Destination");
        custMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        mMap.getOverlays().add(custMarker);

        selectedLocation = latLng;

        org.osmdroid.views.overlay.Polyline line = new org.osmdroid.views.overlay.Polyline();
        line.addPoint(STORE_LOCATION);
        line.addPoint(latLng);
        line.getOutlinePaint().setColor(android.graphics.Color.parseColor("#BA770C"));
        line.getOutlinePaint().setStrokeWidth(10f);
        mMap.getOverlays().add(line);

        double distanceMeters = STORE_LOCATION.distanceToAsDouble(latLng);
        float distanceKm = (float) (distanceMeters / 1000f);

        View cardDistance = findViewById(R.id.card_delivery_distance);
        TextView tvDistance = findViewById(R.id.tv_distance_info);
        TextView tvEta = findViewById(R.id.tv_eta_info);
        com.google.android.material.button.MaterialButton btnOpenDirections = findViewById(R.id.btn_open_directions);

        if (cardDistance != null) {
            cardDistance.setVisibility(View.VISIBLE);
            tvDistance.setText(String.format("Delivery Distance: %.1f km", distanceKm));
            String eta = distanceKm < 15 ? "Express Delivery: Same Day / 24 Hours" : "Standard Delivery: 2-3 Business Days";
            tvEta.setText("Route: Flagship Store -> Your Location\n" + eta);

            btnOpenDirections.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" +
                        STORE_LOCATION.getLatitude() + "," + STORE_LOCATION.getLongitude() +
                        "&destination=" + latLng.getLatitude() + "," + latLng.getLongitude() +
                        "&travelmode=driving");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Toast.makeText(CheckoutActivity.this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        try {
            org.osmdroid.util.BoundingBox box = org.osmdroid.util.BoundingBox.fromGeoPoints(java.util.Arrays.asList(STORE_LOCATION, latLng));
            mMap.zoomToBoundingBox(box, true);
        } catch (Exception e) {
            mMap.getController().animateTo(latLng);
        }

        mMap.invalidate();
        Toast.makeText(CheckoutActivity.this, String.format("Location Selected! (%.1f km)", distanceKm), Toast.LENGTH_SHORT).show();
    }

    private void startPayHerePayment(String name, String phone, String address) {
        InitRequest req = new InitRequest();
        req.setMerchantId("1222300");
        req.setMerchantSecret("MzgzOTgyNDAyMTI3MDk3MzgxNjUyODYwOTc2NjExNDE4MTUwNDMw");
        req.setCurrency("LKR");
        req.setAmount(orderTotal);
        req.setOrderId(currentOrderId);
        req.setItemsDescription("FashionHub App Order");
        req.getItems().add(new Item(null, "FashionHub Items", 1, orderTotal));

        String userEmail = AuthHelper.getUserEmail(this);
        req.getCustomer().setFirstName(name);
        req.getCustomer().setLastName("");
        req.getCustomer().setEmail(!userEmail.isEmpty() ? userEmail : "customer@gmail.com");
        req.getCustomer().setPhone(phone);
        req.getCustomer().getAddress().setAddress(address);
        req.getCustomer().getAddress().setCity("Colombo");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
        Intent intent = new Intent(this, PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        startActivityForResult(intent, PAYHERE_REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYHERE_REQ_CODE && data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
            PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

            if (resultCode == Activity.RESULT_OK) {
                if (response != null && response.isSuccess()) {
                    Toast.makeText(this, "Payment Success!", Toast.LENGTH_SHORT).show();

                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();

                    placeOrderInFirebase(name, phone, address);
                } else {
                    Toast.makeText(this, "Payment Failed!", Toast.LENGTH_LONG).show();
                    resetButton();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Payment Canceled!", Toast.LENGTH_SHORT).show();
                resetButton();
            }
        }
    }

    private void placeOrderInFirebase(String name, String phone, String address) {
        btnPlaceOrder.setText("SAVING ORDER...");

        // Init DB
        DatabaseHelper dbHelper = new DatabaseHelper(CheckoutActivity.this);

        // Fetch items
        List<Map<String, Object>> itemsListForDb = dbHelper.getCartItemsForFirebase();

        String userEmail = AuthHelper.getUserEmail(CheckoutActivity.this);

        // Generate Tracking Code
        String shortId = (currentOrderId != null && currentOrderId.length() >= 8) ? currentOrderId.substring(0, 8).toUpperCase() : (currentOrderId != null ? currentOrderId.toUpperCase() : "FH" + System.currentTimeMillis());
        String trackingCode = "TRK-" + shortId;

        // Prepare map
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", currentOrderId);
        orderMap.put("trackingCode", trackingCode);
        orderMap.put("customerName", name);
        orderMap.put("customerPhone", phone);
        if (!userEmail.isEmpty()) {
            orderMap.put("userEmail", userEmail);
            orderMap.put("customerEmail", userEmail);
        }
        orderMap.put("deliveryAddress", address);
        orderMap.put("totalAmount", orderTotal);
        orderMap.put("status", "Pending"); // Default status

        // Add metadata
        orderMap.put("items", itemsListForDb); // Cart items
        orderMap.put("timestamp", FieldValue.serverTimestamp()); // Server time

        // Add coordinates
        if (selectedLocation != null) {
            orderMap.put("deliveryLat", selectedLocation.getLatitude());
            orderMap.put("deliveryLng", selectedLocation.getLongitude());
        }

        // Save to Firebase
        db.collection("orders").document(currentOrderId)
                .set(orderMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CheckoutActivity.this, "Order Placed Successfully!", Toast.LENGTH_LONG).show();

                    // 1. Multimedia: Play celebratory sound via MediaPlayer
                    playOrderSuccessSound();
                    
                    // 2. Save Address locally via JSON & Internal Storage (Criteria 4.3 & 5.4)
                    saveAddressToJson(address);

                    // 3. Telephony: Send SMS confirmation to customer's phone with Tracking Code
                    sendOrderConfirmationSMS(phone, currentOrderId, trackingCode, orderTotal);

                    // 3. Receivers & Integration: Send Custom Broadcast for local notification
                    Intent broadcastIntent = new Intent(OrderNotificationReceiver.ACTION_ORDER_PLACED);
                    broadcastIntent.putExtra("orderId", currentOrderId);
                    broadcastIntent.putExtra("trackingCode", trackingCode);
                    broadcastIntent.putExtra("orderTotal", orderTotal);
                    sendBroadcast(broadcastIntent);

                    // 4. Inventory: Deduct ordered quantities from product stock in Firestore
                    deductStockForOrderedItems(itemsListForDb);

                    // Clear local SQLite cart
                    dbHelper.clearCart();

                    // Prompt to view PDF invoice
                    lk.prabhath.fashionhub.activity.model.Order placedOrder = new lk.prabhath.fashionhub.activity.model.Order(
                            currentOrderId, name, orderTotal, "Pending", trackingCode);
                    placedOrder.setCustomerPhone(phone);
                    placedOrder.setCustomerEmail(userEmail);
                    placedOrder.setUserEmail(userEmail);
                    placedOrder.setDeliveryAddress(address);
                    placedOrder.setItems(itemsListForDb);

                    new androidx.appcompat.app.AlertDialog.Builder(CheckoutActivity.this)
                            .setTitle("Order Placed Successfully! 🎉")
                            .setMessage("Thank you for your purchase!\n\nTracking Code: " + trackingCode + "\nTotal Amount: Rs. " + String.format(java.util.Locale.US, "%.2f", orderTotal) + "\n\nWould you like to view your official PDF invoice now?")
                            .setPositiveButton("📄 View Invoice", (dialog, which) -> {
                                lk.prabhath.fashionhub.util.InvoiceGenerator.generateAndOpenInvoice(CheckoutActivity.this, placedOrder);
                                finish();
                            })
                            .setNegativeButton("Done", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CheckoutActivity.this, "Payment done, but failed to save order details online.", Toast.LENGTH_LONG).show();
                    resetButton();
                });
    }

    
    private void deductStockForOrderedItems(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return;
        for (Map<String, Object> item : items) {
            String pId = (String) item.get("productId");
            Object qtyObj = item.get("quantity");
            if (qtyObj == null) qtyObj = item.get("qty");
            int qty = 1;
            if (qtyObj instanceof Number) {
                qty = ((Number) qtyObj).intValue();
            } else if (qtyObj instanceof String) {
                try {
                    qty = Integer.parseInt((String) qtyObj);
                } catch (Exception ignored) {}
            }

            if (pId != null && !pId.isEmpty() && qty > 0) {
                final int deductQty = qty;
                db.collection("products").document(pId)
                        .update("stock", FieldValue.increment(-deductQty))
                        .addOnFailureListener(e -> Log.e("StockUpdate", "Failed to decrement stock for product " + pId, e));
            }
        }
    }

    private void playOrderSuccessSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(CheckoutActivity.this, R.raw.order_success);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendOrderConfirmationSMS(String phone, String orderId, String trackingCode, double total) {
        if (phone == null || phone.trim().isEmpty()) return;
        try {
            String shortId = (orderId != null && orderId.length() >= 8) ? orderId.substring(0, 8).toUpperCase() : "FH";
            String message = String.format("FashionHub: Order #%s confirmed! Total: Rs. %.2f. Your Tracking Code is [%s]. Track your order anytime on FashionHub. Thank you!",
                    shortId, total, trackingCode);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                SmsManager smsManager = SmsManager.getDefault();
                java.util.ArrayList<String> parts = smsManager.divideMessage(message);
                if (parts.size() > 1) {
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
                } else {
                    smsManager.sendTextMessage(phone, null, message, null, null);
                }
                Toast.makeText(this, "Order SMS sent with Tracking Code!", Toast.LENGTH_SHORT).show();
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 102);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetButton() {
        btnPlaceOrder.setEnabled(true);
        btnPlaceOrder.setText("PLACE ORDER");
    }

    private void showSavedAddressesDialog() {
        try {
            java.io.File file = new java.io.File(getFilesDir(), "saved_addresses.json");
            if (!file.exists()) {
                Toast.makeText(this, "No saved addresses yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            java.io.FileInputStream fis = openFileInput("saved_addresses.json");
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();
            String jsonStr = new String(buffer, "UTF-8");
            org.json.JSONArray addressArray = new org.json.JSONArray(jsonStr);
            
            if (addressArray.length() == 0) return;
            
            String[] addresses = new String[addressArray.length()];
            for (int i = 0; i < addressArray.length(); i++) {
                addresses[i] = addressArray.getString(i);
            }
            
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Saved Address")
                .setItems(addresses, (dialog, which) -> {
                    etAddress.setText(addresses[which]);
                })
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveAddressToJson(String newAddress) {
        if (newAddress == null || newAddress.isEmpty()) return;
        try {
            org.json.JSONArray addressArray = new org.json.JSONArray();
            
            // Read existing addresses
            java.io.File file = new java.io.File(getFilesDir(), "saved_addresses.json");
            if (file.exists()) {
                java.io.FileInputStream fis = openFileInput("saved_addresses.json");
                int size = fis.available();
                byte[] buffer = new byte[size];
                fis.read(buffer);
                fis.close();
                String jsonStr = new String(buffer, "UTF-8");
                addressArray = new org.json.JSONArray(jsonStr);
            }

            // Check for duplicates
            boolean exists = false;
            for (int i = 0; i < addressArray.length(); i++) {
                if (addressArray.getString(i).equalsIgnoreCase(newAddress)) {
                    exists = true;
                    break;
                }
            }

            // Add and Save if new
            if (!exists) {
                addressArray.put(newAddress);
                FileOutputStream fos = openFileOutput("saved_addresses.json", MODE_PRIVATE);
                fos.write(addressArray.toString(4).getBytes());
                fos.close();
                android.util.Log.d("EShop", "Address saved to JSON file in Internal Storage.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
