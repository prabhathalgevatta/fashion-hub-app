package lk.prabhath.fashionhub.activity.model;

import com.google.firebase.firestore.PropertyName;

public class Product {

    private String id;
    private String name;
    private double price;
    private String imageUrl;


    private double rating;
    private int reviewCount;

    public Product() {

    }

    public Product(String id, String name, double price, String imageUrl, double rating, int reviewCount) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }


    @PropertyName("productName")
    public String getName() { return name; }

    @PropertyName("productName")
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    private String category;
    private int stock = 0;

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}