package lk.prabhath.fashionhub.activity.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.OrdersActivity;

public class OrderNotificationReceiver extends BroadcastReceiver {

    public static final String ACTION_ORDER_PLACED = "lk.prabhath.fashionhub.ORDER_PLACED";
    private static final String CHANNEL_ID = "orders_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_ORDER_PLACED.equals(intent.getAction())) {
            String orderId = intent.getStringExtra("orderId");
            String trackingCode = intent.getStringExtra("trackingCode");
            double total = intent.getDoubleExtra("orderTotal", 0.0);

            showOrderNotification(context, orderId, trackingCode, total);
        }
    }

    private void showOrderNotification(Context context, String orderId, String trackingCode, double total) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Order Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications about placed and processed orders");
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Tap to open OrdersActivity
        Intent ordersIntent = new Intent(context, OrdersActivity.class);
        ordersIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                ordersIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String shortId = (orderId != null && orderId.length() >= 8) ? orderId.substring(0, 8) : "FH";
        String trackingInfo = (trackingCode != null && !trackingCode.isEmpty()) ? " (" + trackingCode + ")" : "";
        String title = "Order Placed Successfully! 🎉";
        String message = String.format("Your order #%s%s for Rs. %.2f has been confirmed.",
                shortId, trackingInfo, total);

        
        // Save to Database
        lk.prabhath.fashionhub.activity.database.DatabaseHelper db = new lk.prabhath.fashionhub.activity.database.DatabaseHelper(context);
        db.addNotification(title, message, "OrdersActivity", lk.prabhath.fashionhub.util.AuthHelper.getUserEmail(context));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message + " We're preparing your delivery!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
