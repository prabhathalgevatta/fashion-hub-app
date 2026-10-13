package lk.prabhath.fashionhub.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.MainActivity;
import lk.prabhath.fashionhub.activity.OrdersActivity;

public class AppNotificationHelper {

    public static final String CHANNEL_ORDERS = "orders_channel";
    public static final String CHANNEL_OFFERS = "FashionHub_Offers";

    public static void createNotificationChannels(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // 1. Orders Channel
        NotificationChannel ordersChannel = new NotificationChannel(
                CHANNEL_ORDERS,
                "Order Status Updates",
                NotificationManager.IMPORTANCE_HIGH
        );
        ordersChannel.setDescription("Alerts when your order is confirmed, shipped, or delivered");
        ordersChannel.enableVibration(true);
        ordersChannel.setShowBadge(true);
        manager.createNotificationChannel(ordersChannel);

        // 2. Offers & Banners Channel
        NotificationChannel offersChannel = new NotificationChannel(
                CHANNEL_OFFERS,
                "Promotions & Special Offers",
                NotificationManager.IMPORTANCE_HIGH
        );
        offersChannel.setDescription("Exclusive discounts, flash sales, and seasonal fashion offers");
        offersChannel.enableVibration(true);
        offersChannel.setShowBadge(true);
        manager.createNotificationChannel(offersChannel);
    }

    public static void showOrderStatusNotification(Context context, String orderId, String newStatus, double amount) {
        if (context == null) return;
        createNotificationChannels(context);

        String idSnippet = (orderId != null && orderId.length() >= 8) ? orderId.substring(0, 8) : "FH";

        String title;
        String message;

        if ("Delivered".equalsIgnoreCase(newStatus)) {
            title = "🎉 Order Delivered! #" + idSnippet;
            message = "Your package has been successfully delivered. We hope you love your fashion items!";
        } else if ("Shipped".equalsIgnoreCase(newStatus)) {
            title = "🚚 Order Shipped! #" + idSnippet;
            message = "Great news! Your order is on the way to your delivery address.";
        } else if ("Canceled".equalsIgnoreCase(newStatus) || "Cancelled".equalsIgnoreCase(newStatus)) {
            title = "⚠️ Order Cancelled #" + idSnippet;
            message = "Your order #" + idSnippet + " has been cancelled. Contact support if you have questions.";
        } else if ("Processing".equalsIgnoreCase(newStatus)) {
            title = "⚙️ Order Processing #" + idSnippet;
            message = "We're currently preparing and packing your ordered items.";
        } else {
            title = "📦 Order Status: " + newStatus;
            message = "Your order #" + idSnippet + " status has been updated to: " + newStatus;
        }

        // Tap opens OrdersActivity
        Intent intent = new Intent(context, OrdersActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        // Save to Database
        lk.prabhath.fashionhub.activity.database.DatabaseHelper db = new lk.prabhath.fashionhub.activity.database.DatabaseHelper(context);
        db.addNotification(title, message, "OrdersActivity", lk.prabhath.fashionhub.util.AuthHelper.getUserEmail(context));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ORDERS)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 250, 150, 250})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void showOfferNotification(Context context, String title, String subtitle, String discountText) {
        if (context == null) return;
        createNotificationChannels(context);

        String badge = (discountText != null && !discountText.trim().isEmpty()) ? "[" + discountText + "] " : "";
        String notifTitle = "🔥 Special Offer: " + badge + (title != null ? title : "Flash Sale!");
        String notifBody = (subtitle != null && !subtitle.trim().isEmpty())
                ? subtitle
                : "Exciting new fashion discounts are live now. Tap to explore!";

        // Tap opens MainActivity (Home)
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        // Save to Database
        lk.prabhath.fashionhub.activity.database.DatabaseHelper db = new lk.prabhath.fashionhub.activity.database.DatabaseHelper(context);
        db.addNotification(notifTitle, notifBody, "MainActivity", lk.prabhath.fashionhub.util.AuthHelper.getUserEmail(context));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_OFFERS)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(notifTitle)
                .setContentText(notifBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notifBody + " Limited time offer, don't miss out!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 200, 100, 200})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
