package lk.prabhath.fashionhub.activity.database;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    public void addNotification(String title, String message, String targetActivity, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("target_activity", targetActivity);
        values.put("user_email", userEmail != null ? userEmail : "global");
        db.insert("notifications", null, values);
        db.close();
    }

    public java.util.List<java.util.Map<String, String>> getAllNotifications(String userEmail) {
        java.util.List<java.util.Map<String, String>> notifications = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notifications WHERE user_email = ? OR user_email = 'global' ORDER BY id DESC", new String[]{userEmail != null ? userEmail : ""});
        if (cursor.moveToFirst()) {
            do {
                java.util.Map<String, String> notif = new java.util.HashMap<>();
                notif.put("id", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("id"))));
                notif.put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")));
                notif.put("message", cursor.getString(cursor.getColumnIndexOrThrow("message")));
                notif.put("target_activity", cursor.getString(cursor.getColumnIndexOrThrow("target_activity")));
                notif.put("timestamp", cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
                notifications.add(notif);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notifications;
    }

    public void clearAllNotifications(String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM notifications WHERE user_email = '" + (userEmail != null ? userEmail : "") + "' OR user_email = 'global'");
        db.close();
    }

    public void deleteNotification(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("notifications", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }


    private static final String DATABASE_NAME = "FashionHubLocal.db";
    private static final int DATABASE_VERSION = 8;

    private static final String TABLE_CART = "cart";
    private static final String TABLE_WISHLIST = "wishlist";
    private static final String TABLE_CACHED_PRODUCTS = "cached_products";

    // Cart columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PRODUCT_ID = "product_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_IMAGE = "image";
    private static final String COLUMN_SIZE = "size";
    private static final String COLUMN_COLOR = "color";
    private static final String COLUMN_QTY = "quantity";

    // Cached Products columns
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_RATING = "rating";
    private static final String COLUMN_REVIEW_COUNT = "review_count";
    private static final String COLUMN_VIEWED_AT = "viewed_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableCart = "CREATE TABLE " + TABLE_CART + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PRODUCT_ID + " TEXT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PRICE + " REAL, " +
                COLUMN_IMAGE + " TEXT, " +
                COLUMN_SIZE + " TEXT, " +
                COLUMN_COLOR + " TEXT, " +
                COLUMN_QTY + " INTEGER, " +
                COLUMN_CATEGORY + " TEXT)";
        db.execSQL(createTableCart);

        String createTableWishlist = "CREATE TABLE IF NOT EXISTS " + TABLE_WISHLIST + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PRODUCT_ID + " TEXT UNIQUE, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PRICE + " REAL, " +
                COLUMN_IMAGE + " TEXT)";
        db.execSQL(createTableWishlist);

        String createTableCachedProducts = "CREATE TABLE IF NOT EXISTS " + TABLE_CACHED_PRODUCTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PRODUCT_ID + " TEXT UNIQUE, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PRICE + " REAL, " +
                COLUMN_IMAGE + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_RATING + " REAL, " +
                COLUMN_REVIEW_COUNT + " INTEGER, " +
                COLUMN_VIEWED_AT + " INTEGER)";
        db.execSQL(createTableCachedProducts);

        db.execSQL("CREATE TABLE IF NOT EXISTS notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "message TEXT, " +
                "target_activity TEXT, " +
                "user_email TEXT DEFAULT 'global', " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String createTableWishlist = "CREATE TABLE IF NOT EXISTS " + TABLE_WISHLIST + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PRODUCT_ID + " TEXT UNIQUE, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_PRICE + " REAL, " +
                    COLUMN_IMAGE + " TEXT)";
            db.execSQL(createTableWishlist);
        }
        if (oldVersion < 3) {
            String createTableCachedProducts = "CREATE TABLE IF NOT EXISTS " + TABLE_CACHED_PRODUCTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PRODUCT_ID + " TEXT UNIQUE, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_PRICE + " REAL, " +
                    COLUMN_IMAGE + " TEXT, " +
                    COLUMN_CATEGORY + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_RATING + " REAL, " +
                    COLUMN_REVIEW_COUNT + " INTEGER, " +
                    COLUMN_VIEWED_AT + " INTEGER)";
            db.execSQL(createTableCachedProducts);
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_CART + " ADD COLUMN " + COLUMN_COLOR + " TEXT DEFAULT 'Default'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_CART + " ADD COLUMN " + COLUMN_CATEGORY + " TEXT DEFAULT 'Uncategorized'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "message TEXT, " +
                "target_activity TEXT, " +
                "user_email TEXT DEFAULT 'global', " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE notifications ADD COLUMN user_email TEXT DEFAULT 'global'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Add cart with color, quantity, and category
    public boolean addToCart(String productId, String name, double price, String image, String size, String color, int qty, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        String safeColor = (color != null && !color.trim().isEmpty()) ? color.trim() : "Default";

        // Check existing matching product_id, size and color
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CART + " WHERE " + COLUMN_PRODUCT_ID + "=? AND " + COLUMN_SIZE + "=? AND (" + COLUMN_COLOR + "=? OR (" + COLUMN_COLOR + " IS NULL AND ?='Default'))",
                new String[]{productId, size, safeColor, safeColor});

        if (cursor != null && cursor.getCount() > 0) {
            // Update quantity
            cursor.moveToFirst();
            int currentQty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QTY));
            int cartId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            ContentValues values = new ContentValues();
            values.put(COLUMN_QTY, currentQty + qty);

            long result = db.update(TABLE_CART, values, COLUMN_ID + "=?", new String[]{String.valueOf(cartId)});
            cursor.close();
            return result != -1;
        } else {
            if (cursor != null) cursor.close();
            // Insert new
            ContentValues values = new ContentValues();
            values.put(COLUMN_PRODUCT_ID, productId);
            values.put(COLUMN_NAME, name);
            values.put(COLUMN_PRICE, price);
            values.put(COLUMN_IMAGE, image);
            values.put(COLUMN_SIZE, size);
            values.put(COLUMN_COLOR, safeColor);
            values.put(COLUMN_QTY, qty);
            values.put(COLUMN_CATEGORY, category != null && !category.isEmpty() ? category : "Uncategorized");

            long result = db.insert(TABLE_CART, null, values);
            return result != -1;
        }
    }

    // Overload for backward compatibility
    public boolean addToCart(String productId, String name, double price, String image, String size, String color, int qty) {
        return addToCart(productId, name, price, image, size, color, qty, "Uncategorized");
    }

    // Overload for backward compatibility
    public boolean addToCart(String productId, String name, double price, String image, String size, int qty) {
        return addToCart(productId, name, price, image, size, "Default", qty);
    }


    public boolean deleteCartItem(String id) {
        SQLiteDatabase db = this.getWritableDatabase();

        long result = db.delete(TABLE_CART, COLUMN_ID + "=?", new String[]{id});
        return result != -1;
    }


    public void clearCart() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_CART);
    }

    // Firebase items
    public List<Map<String, Object>> getCartItemsForFirebase() {
        List<Map<String, Object>> itemList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CART, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> itemMap = new HashMap<>();

                // Map fields
                itemMap.put("productName", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                itemMap.put("price", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)));
                itemMap.put("qty", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QTY)));
                itemMap.put("size", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SIZE)));
                itemMap.put("imageUrl", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE)));
                itemMap.put("category", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));

                itemList.add(itemMap);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return itemList;
    }

    // Wishlist operations
    public boolean addToWishlist(String productId, String name, double price, String image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_ID, productId);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PRICE, price);
        values.put(COLUMN_IMAGE, image);

        long result = db.insertWithOnConflict(TABLE_WISHLIST, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public boolean removeFromWishlist(String productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete(TABLE_WISHLIST, COLUMN_PRODUCT_ID + "=?", new String[]{productId});
        return result != -1;
    }

    public boolean isInWishlist(String productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_WISHLIST + " WHERE " + COLUMN_PRODUCT_ID + "=?", new String[]{productId});
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    public Cursor getWishlistCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WISHLIST, null);
    }

    // Cache catalog products for offline browsing
    public void cacheProducts(List<lk.prabhath.fashionhub.activity.model.Product> products) {
        if (products == null || products.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (lk.prabhath.fashionhub.activity.model.Product p : products) {
                if (p == null || p.getId() == null) continue;
                ContentValues values = new ContentValues();
                values.put(COLUMN_PRODUCT_ID, p.getId());
                values.put(COLUMN_NAME, p.getName());
                values.put(COLUMN_PRICE, p.getPrice());
                values.put(COLUMN_IMAGE, p.getImageUrl());
                values.put(COLUMN_CATEGORY, p.getCategory());
                values.put(COLUMN_RATING, p.getRating());
                values.put(COLUMN_REVIEW_COUNT, p.getReviewCount());
                if (p.getDescription() != null) values.put(COLUMN_DESCRIPTION, p.getDescription());
                db.insertWithOnConflict(TABLE_CACHED_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    // Get cached catalog products when offline
    public List<lk.prabhath.fashionhub.activity.model.Product> getCachedProducts() {
        List<lk.prabhath.fashionhub.activity.model.Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CACHED_PRODUCTS + " ORDER BY " + COLUMN_ID + " DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE));
                String image = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                double rating = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_RATING));
                int reviewCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REVIEW_COUNT));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));

                lk.prabhath.fashionhub.activity.model.Product p = new lk.prabhath.fashionhub.activity.model.Product(id, name, price, image, rating, reviewCount);
                p.setCategory(category);
                p.setDescription(description);
                list.add(p);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    // Save product to recently viewed cache
    public void saveRecentlyViewedProduct(String productId, String name, double price, String image, String category, String description, double rating, int reviewCount) {
        if (productId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_ID, productId);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PRICE, price);
        values.put(COLUMN_IMAGE, image);
        if (category != null) values.put(COLUMN_CATEGORY, category);
        if (description != null) values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_RATING, rating);
        values.put(COLUMN_REVIEW_COUNT, reviewCount);
        values.put(COLUMN_VIEWED_AT, System.currentTimeMillis());

        db.insertWithOnConflict(TABLE_CACHED_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Get single cached product
    public Map<String, Object> getCachedProduct(String productId) {
        if (productId == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CACHED_PRODUCTS + " WHERE " + COLUMN_PRODUCT_ID + "=?", new String[]{productId});
        Map<String, Object> map = null;
        if (cursor != null && cursor.moveToFirst()) {
            map = new HashMap<>();
            map.put("name", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            map.put("price", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)));
            map.put("image", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE)));
            map.put("category", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
            map.put("description", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
            map.put("rating", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_RATING)));
            map.put("reviewCount", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REVIEW_COUNT)));
            cursor.close();
        }
        return map;
    }

    // Wishlist items formatted for online sync
    public List<Map<String, Object>> getWishlistItemsForFirebase() {
        List<Map<String, Object>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_WISHLIST, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID)));
                map.put("name", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                map.put("price", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)));
                map.put("image", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE)));
                list.add(map);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }
}