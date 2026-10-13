package lk.prabhath.fashionhub.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.location.Address;
import android.location.Geocoder;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import android.preference.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.util.AuthHelper;

public class ProfileActivity extends AppCompatActivity  {

    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;
    private static final int IMAGE_PICK_CODE = 1002;

    private static final String IMGBB_API_KEY = "0b7cb258923a8c4190bc55582d7d816c";

    private ShapeableImageView ivProfilePic;
    private TextInputEditText etName, etEmail, etPhone, etAddress;
    private TextView tvDisplayName, tvDisplayEmail;
    private MaterialButton btnUpdate;
    private NestedScrollView scrollProfileContent;

    // Google Map components
    private MapView mMap;
    private GeoPoint selectedLatLng = null;
    private Marker currentMarker = null;
    private static final GeoPoint DEFAULT_LOCATION = new GeoPoint(6.9271, 79.8612); // Colombo

    private Bitmap currentImageBitmap = null;
    private String currentUserEmail = "";
    private String currentImageUrl = "";

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn(this)) {
            Toast.makeText(this, "Please log in to access your profile", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();

        ivProfilePic = findViewById(R.id.iv_profile_pic);
        etName = findViewById(R.id.et_profile_name);
        etEmail = findViewById(R.id.et_profile_email);
        etPhone = findViewById(R.id.et_profile_phone);
        etAddress = findViewById(R.id.et_profile_address);
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
        tvDisplayName = findViewById(R.id.tv_profile_display_name);
        tvDisplayEmail = findViewById(R.id.tv_profile_display_email);
        scrollProfileContent = findViewById(R.id.scroll_profile_content);
        FloatingActionButton fabCamera = findViewById(R.id.fab_camera);
        btnUpdate = findViewById(R.id.btn_update_profile);

        // Map setup
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        mMap = findViewById(R.id.map_profile);
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true);
        mMap.getController().setZoom(12.0);
        mMap.getController().setCenter(DEFAULT_LOCATION);
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                updateMapMarker(p);
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };
        mMap.getOverlays().add(new MapEventsOverlay(mReceive));


        android.view.View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Quick shortcuts
        android.view.View cardOrders = findViewById(R.id.card_my_orders);
        if (cardOrders != null) {
            cardOrders.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, OrdersActivity.class)));
        }

        android.view.View cardWishlist = findViewById(R.id.card_my_wishlist);
        if (cardWishlist != null) {
            cardWishlist.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, WishlistActivity.class)));
        }

        // Logout buttons
        android.view.View btnTopLogout = findViewById(R.id.btn_profile_logout);
        if (btnTopLogout != null) {
            btnTopLogout.setOnClickListener(v -> showLogoutDialog());
        }

        android.view.View btnRowLogout = findViewById(R.id.btn_logout_row);
        if (btnRowLogout != null) {
            btnRowLogout.setOnClickListener(v -> showLogoutDialog());
        }

        etEmail.setEnabled(false);
        etEmail.setFocusable(false);

        currentUserEmail = AuthHelper.getUserEmail(this);
        loadUserData(currentUserEmail);
        setupPieChart();
        setupAutoThemeSwitch();

        fabCamera.setOnClickListener(v -> showImageOptionDialog());

        btnUpdate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress != null ? etAddress.getText().toString().trim() : "";

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (IMGBB_API_KEY.equals("YOUR_IMGBB_API_KEY_HERE")) {
                Toast.makeText(this, "Enter API Key!", Toast.LENGTH_LONG).show();
                return;
            }

            btnUpdate.setEnabled(false);
            btnUpdate.setText("UPLOADING...");
            uploadImageAndSaveProfile(name, phone, address);
        });
    }

    private void loadUserData(String email) {
        if (tvDisplayEmail != null) {
            tvDisplayEmail.setText(email);
        }

        db.collection("users").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        etName.setText(name);
                        etEmail.setText(email);
                        etPhone.setText(documentSnapshot.getString("phone"));

                        String address = documentSnapshot.getString("address");
                        if (etAddress != null && address != null && !address.isEmpty()) {
                            etAddress.setText(address);
                            geocodeAndShowOnMap(address);
                        }

                        if (tvDisplayName != null && name != null && !name.isEmpty()) {
                            tvDisplayName.setText(name);
                        }

                        currentImageUrl = documentSnapshot.getString("imageUrl");
                        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                            Glide.with(this).load(currentImageUrl).into(ivProfilePic);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Load failed", Toast.LENGTH_SHORT).show());
    }

    private void uploadImageAndSaveProfile(String name, String phone, String address) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String finalImageUrl = currentImageUrl;

                if (currentImageBitmap != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    currentImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                    String postData = "key=" + IMGBB_API_KEY + "&image=" + URLEncoder.encode(base64Image, "UTF-8");

                    URL url = new URL("https://api.imgbb.com/1/upload");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(postData.getBytes());

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                        reader.close();

                        JSONObject jsonObject = new JSONObject(response.toString());
                        finalImageUrl = jsonObject.getJSONObject("data").getString("url");
                    } else {
                        Log.e("ImgBB_Upload", "Error code: " + responseCode);
                        throw new Exception("Server Error");
                    }
                }

                String updatedImageUrl = finalImageUrl;

                runOnUiThread(() -> {
                    db.collection("users").document(currentUserEmail)
                            .update("name", name, "phone", phone, "address", address, "imageUrl", updatedImageUrl)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ProfileActivity.this, "Update Success!", Toast.LENGTH_LONG).show();
                                if (tvDisplayName != null && !name.isEmpty()) {
                                    tvDisplayName.setText(name);
                                }
                                btnUpdate.setEnabled(true);
                                btnUpdate.setText("Save Changes");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ProfileActivity.this, "Update Failed!", Toast.LENGTH_SHORT).show();
                                btnUpdate.setEnabled(true);
                                btnUpdate.setText("Save Changes");
                            });
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ImgBB_Upload", "Error: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText("Save Changes");
                });
            }
        });
    }

    private void updateMapMarker(GeoPoint latLng, String title, boolean moveCamera) {
        if (mMap == null) return;
        mMap.getOverlays().remove(currentMarker);
        currentMarker = new org.osmdroid.views.overlay.Marker(mMap);
        currentMarker.setPosition(latLng);
        currentMarker.setTitle(title);
        currentMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        mMap.getOverlays().add(currentMarker);
        if (moveCamera) mMap.getController().animateTo(latLng);
        mMap.invalidate();
        selectedLatLng = latLng;
        reverseGeocode(latLng);
    }
    
    private void updateMapMarker(GeoPoint latLng) {
        updateMapMarker(latLng, "Selected Location", true);
    }

    private void reverseGeocode(GeoPoint latLng) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String resultAddress = "";
            try {
                Geocoder geocoder = new Geocoder(ProfileActivity.this, Locale.getDefault());
                java.util.List<Address> addresses = geocoder.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i));
                        if (i < address.getMaxAddressLineIndex()) sb.append(", ");
                    }
                    resultAddress = sb.toString();
                }
            } catch (Exception e) {
                Log.e("Geocoder", "Error reverse geocoding", e);
            }

            final String finalAddr = resultAddress;
            runOnUiThread(() -> {
                if (etAddress != null) {
                    if (finalAddr != null && !finalAddr.isEmpty()) {
                        etAddress.setText(finalAddr);
                    } else {
                        etAddress.setText(String.format(Locale.US, "Lat: %.5f, Lng: %.5f", latLng.getLatitude(), latLng.getLongitude()));
                    }
                }
            });
        });
    }

    private void geocodeAndShowOnMap(String address) {
        if (address == null || address.trim().isEmpty()) return;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(ProfileActivity.this, Locale.getDefault());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(address, 1, addresses -> {
                        if (addresses != null && !addresses.isEmpty()) {
                            Address addr = addresses.get(0);
                            GeoPoint latLng = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                            selectedLatLng = latLng;
                            runOnUiThread(() -> updateMapMarker(latLng, "Saved Address", false));
                        }
                    });
                } else {
                    List<Address> addresses = geocoder.getFromLocationName(address, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        GeoPoint latLng = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                        selectedLatLng = latLng;
                        runOnUiThread(() -> updateMapMarker(latLng, "Saved Address", false));
                    }
                }
            } catch (Exception e) {
                Log.e("Geocoder", "Error forward geocoding", e);
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of FashionHub?")
                .setPositiveButton("Log Out", (dialog, which) -> AuthHelper.logout(ProfileActivity.this))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showImageOptionDialog() {
        String[] options = {"Take Photo", "Choose Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) checkPermissionsAndOpenCamera();
            else checkPermissionsAndOpenGallery();
        });
        builder.show();
    }

    private void checkPermissionsAndOpenCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
            } else openCamera();
        } else openCamera();
    }

    // Android 13+ support
    private void checkPermissionsAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_CODE);
            } else {
                openGallery();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
            } else {
                openGallery();
            }
        } else {
            openGallery();
        }
    }

    private void openCamera() {
        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), IMAGE_CAPTURE_CODE);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == IMAGE_CAPTURE_CODE) {
                currentImageBitmap = (Bitmap) data.getExtras().get("data");
                ivProfilePic.setImageBitmap(currentImageBitmap);
            } else if (requestCode == IMAGE_PICK_CODE) {
                try {
                    currentImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    ivProfilePic.setImageBitmap(currentImageBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setupPieChart() {
        com.github.mikephil.charting.charts.PieChart pieChart = findViewById(R.id.pieChart);
        if (pieChart == null) return;
        
        pieChart.setNoDataText("Loading order data...");
        pieChart.setNoDataTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
        pieChart.invalidate();

        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            pieChart.setNoDataText("Please log in to view data.");
            pieChart.invalidate();
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("orders")
            .whereEqualTo("customerEmail", currentUserEmail)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    pieChart.setNoDataText("No orders placed yet.");
                    pieChart.invalidate();
                    return;
                }
                
                java.util.Map<String, Float> categorySpendMap = new java.util.HashMap<>();
                
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    java.util.List<java.util.Map<String, Object>> items = (java.util.List<java.util.Map<String, Object>>) doc.get("items");
                    if (items != null) {
                        for (java.util.Map<String, Object> item : items) {
                            String category = "Uncategorized";
                            if (item.containsKey("category") && item.get("category") != null) {
                                category = item.get("category").toString();
                            }
                            
                            double price = 0.0;
                            if (item.containsKey("price") && item.get("price") != null) {
                                try {
                                    price = Double.parseDouble(item.get("price").toString());
                                } catch (Exception e) {}
                            }
                            
                            int qty = 1;
                            if (item.containsKey("qty") && item.get("qty") != null) {
                                try {
                                    qty = Integer.parseInt(item.get("qty").toString());
                                } catch (Exception e) {}
                            }
                            
                            float lineTotal = (float) (price * qty);
                            
                            categorySpendMap.put(category, categorySpendMap.getOrDefault(category, 0f) + lineTotal);
                        }
                    }
                }
                
                java.util.ArrayList<com.github.mikephil.charting.data.PieEntry> entries = new java.util.ArrayList<>();
                for (java.util.Map.Entry<String, Float> entry : categorySpendMap.entrySet()) {
                    if (entry.getValue() > 0) {
                        entries.add(new com.github.mikephil.charting.data.PieEntry(entry.getValue(), entry.getKey()));
                    }
                }
                
                if (entries.isEmpty()) {
                    pieChart.setNoDataText("No spending data.");
                    pieChart.invalidate();
                    return;
                }
                
                com.github.mikephil.charting.data.PieDataSet dataSet = new com.github.mikephil.charting.data.PieDataSet(entries, "Spending by Category");
                dataSet.setColors(com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS);
                dataSet.setValueTextSize(12f);
                dataSet.setValueTextColor(android.graphics.Color.WHITE);

                com.github.mikephil.charting.data.PieData data = new com.github.mikephil.charting.data.PieData(dataSet);
                pieChart.setData(data);
                pieChart.getDescription().setEnabled(false);
                pieChart.setCenterText("Spending");
                pieChart.setCenterTextColor(androidx.core.content.ContextCompat.getColor(ProfileActivity.this, R.color.text_primary));
                pieChart.setHoleColor(androidx.core.content.ContextCompat.getColor(ProfileActivity.this, R.color.app_card));
                pieChart.getLegend().setTextColor(androidx.core.content.ContextCompat.getColor(ProfileActivity.this, R.color.text_primary));
                pieChart.animateY(1000);
                pieChart.invalidate();
            })
            .addOnFailureListener(e -> {
                pieChart.setNoDataText("Failed to load data.");
                pieChart.invalidate();
            });
    }

    private void setupAutoThemeSwitch() {
        com.google.android.material.switchmaterial.SwitchMaterial switchTheme = findViewById(R.id.switch_auto_theme);
        if (switchTheme == null) return;
        
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isAutoThemeEnabled = prefs.getBoolean("pref_auto_theme", true);
        switchTheme.setChecked(isAutoThemeEnabled);
        
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("pref_auto_theme", isChecked).apply();
            Toast.makeText(this, isChecked ? "Auto Theme Enabled" : "Auto Theme Disabled", Toast.LENGTH_SHORT).show();
        });
    }
}
