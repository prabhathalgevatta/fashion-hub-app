package lk.prabhath.fashionhub.activity;

import android.content.Intent;

import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import lk.prabhath.fashionhub.activity.database.DatabaseHelper;
import lk.prabhath.fashionhub.activity.adapter.NotificationAdapter;
import java.util.List;
import java.util.Map;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.onesignal.OneSignal;

import com.bumptech.glide.Glide;
import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.util.AuthHelper;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import lk.prabhath.fashionhub.util.AppNotificationHelper;



    public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NavigationBarView.OnItemSelectedListener {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    private static final String ONESIGNAL_APP_ID = "8789c341-531e-4090-9232-5a39da79efd5";

    // 7.2 Android Sensor Framework
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private boolean isDarkModeSuggested = false;
    private boolean isLightModeSuggested = false;
    private int darkReadings = 0;
    private int lightReadings = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Prevent fragment restoration bugs by clearing saved state
        super.onCreate(null);
        setContentView(R.layout.activity_main);

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // OneSignal Initialization
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        // Init Light Sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.side_navigation_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);




        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Update user details & navigation drawer state
        updateUserUI();
        // Create Notification Channels for Orders and Offers
        AppNotificationHelper.createNotificationChannels(this);

        // Sync OneSignal and setup real-time notification listeners
        syncOneSignalUser();
        setupRealtimeNotificationListeners();



        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(this);
        }

        // Load initial HomeFragment so products load immediately on app launch
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        // Setup Floating Capsule Bottom Navigation Dock
        View dockHome = findViewById(R.id.dock_home);
        View dockCart = findViewById(R.id.dock_cart);
        View dockWishlist = findViewById(R.id.dock_wishlist);
        View dockProfile = findViewById(R.id.dock_profile);

        if (dockHome != null) {
            dockHome.setOnClickListener(v -> {
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                ).start();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            });
        }

        if (dockCart != null) {
            dockCart.setOnClickListener(v -> {
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                ).start();
                if (!AuthHelper.isLoggedIn(MainActivity.this)) {
                    AuthHelper.showLoginPrompt(MainActivity.this, "Please log in to view your cart.");
                    return;
                }
                startActivity(new Intent(MainActivity.this, CartActivity.class));
            });
        }

        if (dockWishlist != null) {
            dockWishlist.setOnClickListener(v -> {
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                ).start();
                if (!AuthHelper.isLoggedIn(MainActivity.this)) {
                    AuthHelper.showLoginPrompt(MainActivity.this, "Please log in to view your wishlist.");
                    return;
                }
                startActivity(new Intent(MainActivity.this, WishlistActivity.class));
            });
        }

        if (dockProfile != null) {
            dockProfile.setOnClickListener(v -> {
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                ).start();
                if (!AuthHelper.isLoggedIn(MainActivity.this)) {
                    AuthHelper.showLoginPrompt(MainActivity.this, "Please log in to access your profile.");
                    return;
                }
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            });
        }

        View btnTopMenu = findViewById(R.id.btn_top_menu);
        if (btnTopMenu != null) {
            btnTopMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }


        View topNotifications = findViewById(R.id.btn_top_notifications);
        if (topNotifications != null) {
            topNotifications.setOnClickListener(v -> {
                showNotificationsPopup(v);
            });
        }

        // --- Topic subscription ---
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("NewProducts")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        System.out.println("Subscribed to NewProducts topic successfully!");
                    }
                });
        // ----------------------------------------------------------------------

        // --- Search bar ---
        EditText searchBar = findViewById(R.id.text_input_search);

        if (searchBar != null) {
            android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(android.view.MotionEvent e) {
                    androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof lk.prabhath.fashionhub.activity.HomeFragment) {
                        ((lk.prabhath.fashionhub.activity.HomeFragment) currentFragment).showAdvanceSearchDialog();
                    }
                    return true;
                }
            });

            searchBar.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return false;
            });

            searchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof HomeFragment) {
                        ((HomeFragment) currentFragment).filterProducts(s.toString());
                    }
                }
            });
        }


        View btnFilter = findViewById(R.id.btn_filter);

        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof HomeFragment) {
                    ((HomeFragment) currentFragment).showAdvanceSearchDialog();
                }
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)){
                    drawerLayout.closeDrawer(GravityCompat.START);
                }else{
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();

        if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home){

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();

        } else if (itemId == R.id.side_nav_profile || itemId == R.id.bottom_nav_profile) {

            if (!AuthHelper.isLoggedIn(MainActivity.this)) {
                AuthHelper.showLoginPrompt(MainActivity.this, "Please log in to access your profile.");
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return false;
            }
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);

        } else if (itemId == R.id.side_nav_orders) {

            if (!AuthHelper.isLoggedIn(MainActivity.this)) {
                AuthHelper.showLoginPrompt(MainActivity.this, "Please log in to view your orders.");
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return false;
            }
            Intent intent = new Intent(MainActivity.this, OrdersActivity.class);
            startActivity(intent);

        } else if (itemId == R.id.side_nav_wishlist) {

            if (!AuthHelper.isLoggedIn(MainActivity.this)) {
                AuthHelper.showLoginPrompt(MainActivity.this, "Please log in to view your wishlist.");
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return false;
            }
            Intent intent = new Intent(MainActivity.this, WishlistActivity.class);
            startActivity(intent);

        } else if (itemId == R.id.side_nav_cart || itemId == R.id.bottom_nav_cart) {

            if (!AuthHelper.isLoggedIn(MainActivity.this)) {
                AuthHelper.showLoginPrompt(MainActivity.this, "Please log in to view your cart.");
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return false;
            }
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            startActivity(intent);

        } else if (itemId == R.id.side_nav_theme) {

            android.content.SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
            boolean currentDark = themePrefs.getBoolean("is_dark_mode", false);
            boolean newDark = !currentDark;
            themePrefs.edit().putBoolean("is_dark_mode", newDark).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    newDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                            : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            );
            android.widget.Toast.makeText(this, newDark ? "Dark Theme Activated" : "Light Theme Activated", android.widget.Toast.LENGTH_SHORT).show();

        } else if (itemId == R.id.side_nav_logout) {

            if (AuthHelper.isLoggedIn(MainActivity.this)) {
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Log Out", (dialog, which) -> {
                            AuthHelper.logout(MainActivity.this);
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }

        }

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        return true;
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float lux = event.values[0];
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            boolean isNight = (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);

            android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
            boolean isAutoThemeEnabled = prefs.getBoolean("pref_auto_theme", true);
            
            if (!isAutoThemeEnabled) return;

            if (lux < 15.0f && !isNight) {
                darkReadings++;
                lightReadings = 0;
            } else if (lux > 40.0f && isNight) {
                lightReadings++;
                darkReadings = 0;
            } else {
                darkReadings = 0;
                lightReadings = 0;
            }

            if (darkReadings >= 3) {
                darkReadings = 0;
                Toast.makeText(MainActivity.this, "Auto-switching to Dark Theme \uD83C\uDF19", Toast.LENGTH_SHORT).show();
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else if (lightReadings >= 3) {
                lightReadings = 0;
                Toast.makeText(MainActivity.this, "Auto-switching to Light Theme \u2600\uFE0F", Toast.LENGTH_SHORT).show();
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        updateUserUI();
        attachUserStatusListener();
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    private void updateUserUI() {
        if (navigationView == null) return;
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        android.widget.TextView navName = headerView.findViewById(R.id.nav_header_username);
        android.widget.TextView navEmail = headerView.findViewById(R.id.nav_header_email);
        android.widget.ImageView navPic = headerView.findViewById(R.id.nav_header_profile_pic);
        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.side_nav_logout);

        if (AuthHelper.isLoggedIn(this)) {
            String loggedInEmail = AuthHelper.getUserEmail(this);
            String savedUsername = AuthHelper.getUsername(this);

            if (navEmail != null) navEmail.setText(loggedInEmail);
            if (navName != null) {
                if (savedUsername != null && !savedUsername.isEmpty()) {
                    navName.setText(savedUsername);
                } else {
                    navName.setText("Loading...");
                }
            }

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(loggedInEmail).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("status");
                            if (status != null && "Suspended".equalsIgnoreCase(status.trim())) {
                                handleSuspendedUser();
                                return;
                            }
                            String name = documentSnapshot.getString("name");
                            String imageUrl = documentSnapshot.getString("imageUrl");
                            if (navName != null) {
                                if (name != null && !name.isEmpty()) {
                                    navName.setText(name);
                                } else {
                                    navName.setText(loggedInEmail.split("@")[0]);
                                }
                            }
                            if (navPic != null && imageUrl != null && !imageUrl.isEmpty() && !isFinishing() && !isDestroyed()) {
                                Glide.with(MainActivity.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.user)
                                        .error(R.drawable.user)
                                        .into(navPic);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (navName != null && (savedUsername == null || savedUsername.isEmpty())) {
                            navName.setText("Customer");
                        }
                    });

            headerView.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            });

            if (logoutItem != null) {
                logoutItem.setTitle(R.string.side_nav_logout_title);
                logoutItem.setIcon(R.drawable.logout_24px);
            }
        } else {
            if (navName != null) navName.setText("Welcome Guest");
            if (navEmail != null) navEmail.setText("Tap to log in");
            if (navPic != null) navPic.setImageResource(R.drawable.user);

            headerView.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            });

            if (logoutItem != null) {
                logoutItem.setTitle("Log In");
                logoutItem.setIcon(R.drawable.login_24px);
            }
        }
    }

    private lk.prabhath.fashionhub.activity.receiver.NetworkChangeReceiver networkReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        // Dynamic broadcast receiver registration
        networkReceiver = new lk.prabhath.fashionhub.activity.receiver.NetworkChangeReceiver();
        registerReceiver(networkReceiver, new android.content.IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        // Request notification permission on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userStatusListenerReg != null) {
            userStatusListenerReg.remove();
            userStatusListenerReg = null;
        }
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
                // Already unregistered
            }
        }
    }

    private ListenerRegistration ordersListenerReg;
    private ListenerRegistration bannersListenerReg;
    private ListenerRegistration userStatusListenerReg;

    private void attachUserStatusListener() {
        if (!AuthHelper.isLoggedIn(this)) return;
        String email = AuthHelper.getUserEmail(this);
        if (email.isEmpty()) return;

        if (userStatusListenerReg != null) {
            userStatusListenerReg.remove();
        }

        userStatusListenerReg = FirebaseFirestore.getInstance()
                .collection("users").document(email)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    String status = snapshot.getString("status");
                    if (status != null && "Suspended".equalsIgnoreCase(status.trim())) {
                        handleSuspendedUser();
                    }
                });
    }

    private void handleSuspendedUser() {
        if (isFinishing() || isDestroyed()) return;

        if (userStatusListenerReg != null) {
            userStatusListenerReg.remove();
            userStatusListenerReg = null;
        }

        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Account Suspended")
                .setMessage("Your FashionHub account has been suspended by the administrator. You have been logged out.")
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void syncOneSignalUser() {
        try {
            String userEmail = AuthHelper.getUserEmail(this);
            if (!userEmail.isEmpty()) {
                OneSignal.setExternalUserId(userEmail);
                OneSignal.sendTag("email", userEmail);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRealtimeNotificationListeners() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String currentUserEmail = AuthHelper.getUserEmail(this);

        // 1. Order Status Updates Listener
        if (!currentUserEmail.isEmpty()) {
            ordersListenerReg = db.collection("orders")
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null || snapshots == null) return;

                        SharedPreferences prefs = getSharedPreferences("OrderStatusTracker", MODE_PRIVATE);
                        boolean isInitialized = prefs.getBoolean("init_" + currentUserEmail, false);
                        SharedPreferences.Editor editor = prefs.edit();

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            QueryDocumentSnapshot doc = dc.getDocument();
                            String orderUser = doc.getString("userEmail");
                            if (orderUser == null) orderUser = doc.getString("customerEmail");

                            if (currentUserEmail.equalsIgnoreCase(orderUser)) {
                                String orderId = doc.getId();
                                String status = doc.getString("status");
                                if (status == null) status = "Pending";

                                double amount = 0.0;
                                if (doc.contains("totalAmount")) {
                                    Double val = doc.getDouble("totalAmount");
                                    if (val != null) amount = val;
                                }

                                String savedStatus = prefs.getString("status_" + orderId, null);

                                if (isInitialized && savedStatus != null && !savedStatus.equalsIgnoreCase(status)) {
                                    // Status changed! Fire instant system notification!
                                    AppNotificationHelper.showOrderStatusNotification(MainActivity.this, orderId, status, amount);
                                }

                                editor.putString("status_" + orderId, status);
                            }
                        }

                        editor.putBoolean("init_" + currentUserEmail, true);
                        editor.apply();
                    });
        }

        // 2. Promotional Offers & Banners Listener
        bannersListenerReg = db.collection("banners")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    SharedPreferences prefs = getSharedPreferences("OfferNotificationTracker", MODE_PRIVATE);
                    boolean isInitialized = prefs.getBoolean("init", false);
                    SharedPreferences.Editor editor = prefs.edit();

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            QueryDocumentSnapshot doc = dc.getDocument();
                            String bannerId = doc.getId();
                            boolean isActive = doc.getBoolean("active") != null ? doc.getBoolean("active") : true;

                            if (isActive) {
                                boolean alreadyNotified = prefs.getBoolean("notified_" + bannerId, false);
                                if (isInitialized && !alreadyNotified) {
                                    String title = doc.getString("title");
                                    String subtitle = doc.getString("subtitle");
                                    String discountText = doc.getString("discountText");

                                    // Fire instant promotional offer notification!
                                    AppNotificationHelper.showOfferNotification(MainActivity.this, title, subtitle, discountText);
                                    editor.putBoolean("notified_" + bannerId, true);
                                } else if (!alreadyNotified) {
                                    editor.putBoolean("notified_" + bannerId, true);
                                }
                            }
                        }
                    }

                    editor.putBoolean("init", true);
                    editor.apply();
                });
    }

    private void showNotificationsDialog() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = AuthHelper.getUserEmail(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notifications & Updates");

        StringBuilder sb = new StringBuilder();

        db.collection("banners").whereEqualTo("active", true).limit(3).get()
                .addOnSuccessListener(bannerSnapshots -> {
                    if (!bannerSnapshots.isEmpty()) {
                        sb.append("🔥 ACTIVE SPECIAL OFFERS:\n");
                        for (QueryDocumentSnapshot doc : bannerSnapshots) {
                            String title = doc.getString("title");
                            String badge = doc.getString("discountText");
                            sb.append("• [").append(badge != null ? badge : "OFFER").append("] ")
                              .append(title != null ? title : "Promo").append("\n");
                        }
                        sb.append("\n");
                    }

                    if (!userEmail.isEmpty()) {
                        db.collection("orders").get().addOnSuccessListener(orderSnapshots -> {
                            boolean hasOrders = false;
                            for (QueryDocumentSnapshot doc : orderSnapshots) {
                                String email = doc.getString("userEmail");
                                if (email == null) email = doc.getString("customerEmail");
                                if (userEmail.equalsIgnoreCase(email)) {
                                    if (!hasOrders) {
                                        sb.append("📦 YOUR RECENT ORDERS:\n");
                                        hasOrders = true;
                                    }
                                    String id = doc.getId().substring(0, Math.min(8, doc.getId().length()));
                                    String status = doc.getString("status");
                                    sb.append("• Order #").append(id).append(" - ").append(status != null ? status : "Pending").append("\n");
                                }
                            }

                            if (sb.length() == 0) {
                                sb.append("No active notifications at this time.");
                            }

                            builder.setMessage(sb.toString());
                            builder.setPositiveButton("Close", (d, w) -> d.dismiss());
                            builder.setNeutralButton("View Orders", (d, w) -> {
                                if (AuthHelper.isLoggedIn(MainActivity.this)) {
                                    startActivity(new Intent(MainActivity.this, OrdersActivity.class));
                                } else {
                                    AuthHelper.showLoginPrompt(MainActivity.this, "Please log in to view your orders.");
                                }
                            });
                            builder.show();
                        });
                    } else {
                        if (sb.length() == 0) {
                            sb.append("No active offers at this time. Log in to track your orders!");
                        }
                        builder.setMessage(sb.toString());
                        builder.setPositiveButton("Close", (d, w) -> d.dismiss());
                        builder.show();
                    }
                })
                .addOnFailureListener(e -> {
                    builder.setMessage("Could not load notifications: " + e.getMessage());
                    builder.setPositiveButton("Close", (d, w) -> d.dismiss());
                    builder.show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListenerReg != null) ordersListenerReg.remove();
        if (bannersListenerReg != null) bannersListenerReg.remove();
    }

    private void showNotificationsPopup(View anchor) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.layout_notifications_popup, null);
        
        PopupWindow popupWindow = new PopupWindow(popupView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                true);
        
        popupWindow.setElevation(10);
        
        DatabaseHelper db = new DatabaseHelper(this);
        List<Map<String, String>> notifications = db.getAllNotifications(lk.prabhath.fashionhub.util.AuthHelper.getUserEmail(this));
        
        ListView listView = popupView.findViewById(R.id.list_notifications);
        NotificationAdapter adapter = new NotificationAdapter(this, notifications);
        listView.setAdapter(adapter);
        
        TextView btnClearAll = popupView.findViewById(R.id.btn_clear_all);
        btnClearAll.setOnClickListener(v -> {
            db.clearAllNotifications(lk.prabhath.fashionhub.util.AuthHelper.getUserEmail(this));
            notifications.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Cleared all notifications", Toast.LENGTH_SHORT).show();
        });
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, String> notif = notifications.get(position);
            String target = notif.get("target_activity");
            if ("OrdersActivity".equals(target)) {
                Intent intent = new Intent(this, OrdersActivity.class);
                startActivity(intent);
            }
            popupWindow.dismiss();
        });

        // Show below the bell icon
        popupWindow.showAsDropDown(anchor, -100, 10);
    }
}
