package lk.prabhath.fashionhub.activity.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.prabhath.fashionhub.activity.database.DatabaseHelper;
import lk.prabhath.fashionhub.util.AuthHelper;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";
    private static boolean wasConnected = false;

    public interface NetworkChangeListener {
        void onNetworkChanged(boolean isConnected);
    }

    private static NetworkChangeListener listener;

    public static void setNetworkChangeListener(NetworkChangeListener l) {
        listener = l;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = isNetworkAvailable(context);

        if (listener != null) {
            listener.onNetworkChanged(isConnected);
        }

        if (!isConnected) {
            wasConnected = false;
            Toast.makeText(context, "No Internet Connection! Offline mode active.", Toast.LENGTH_SHORT).show();
        } else {
            // Internet restored or active
            if (!wasConnected) {
                wasConnected = true;
                syncOfflineDataToServer(context);
            }
        }
    }

    /**
     * Automatically syncs locally cached cart and wishlist items to Firestore when back online
     */
    private void syncOfflineDataToServer(Context context) {
        if (!AuthHelper.isLoggedIn(context)) {
            return;
        }

        String userEmail = AuthHelper.getUserEmail(context);
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return;
        }

        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            List<Map<String, Object>> cartItems = dbHelper.getCartItemsForFirebase();
            List<Map<String, Object>> wishlistItems = dbHelper.getWishlistItemsForFirebase();

            Map<String, Object> syncData = new HashMap<>();
            syncData.put("offlineCartSync", cartItems);
            syncData.put("offlineWishlistSync", wishlistItems);
            syncData.put("lastSyncedAt", Timestamp.now());

            FirebaseFirestore.getInstance().collection("users").document(userEmail)
                    .set(syncData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Offline data successfully synced to Firebase for: " + userEmail);
                        Toast.makeText(context, "Online: Offline Cart & Wishlist synced to server!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to sync offline data to Firebase: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error during offline sync: " + e.getMessage(), e);
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
}
