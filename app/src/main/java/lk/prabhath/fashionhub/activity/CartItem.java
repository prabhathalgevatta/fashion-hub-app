package lk.prabhath.fashionhub.activity;
// Required import
import com.google.firebase.firestore.Exclude;

public class CartItem {

    // Document ID
    private String documentId;

    // Product details
    private String productName;
    private double productPrice;
    private String productImage; // Image URL
    private String selectedSize;
    private String selectedColor;
    private int quantity;

    // Review info
    private double rating;
    private int reviewCount;

    // Empty constructor
    public CartItem() {
    }

    // Data constructor
    public CartItem(String productName, double productPrice, String productImage, String selectedSize, int quantity) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.productImage = productImage;
        this.selectedSize = selectedSize;
        this.selectedColor = "Default";
        this.quantity = quantity;
    }

    public CartItem(String productName, double productPrice, String productImage, String selectedSize, String selectedColor, int quantity) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.productImage = productImage;
        this.selectedSize = selectedSize;
        this.selectedColor = selectedColor;
        this.quantity = quantity;
    }

    // Getters Setters

    // Handle ID
    @Exclude // Exclude DB
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // Product name
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    // Product price
    public double getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(double productPrice) {
        this.productPrice = productPrice;
    }

    // Image URL
    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    // Selected size
    public String getSelectedSize() {
        return selectedSize;
    }

    public void setSelectedSize(String selectedSize) {
        this.selectedSize = selectedSize;
    }

    // Quantity count
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // Rating info
    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    // Review count
    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(String selectedColor) {
        this.selectedColor = selectedColor;
    }
}