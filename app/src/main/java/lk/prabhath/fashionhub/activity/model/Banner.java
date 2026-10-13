package lk.prabhath.fashionhub.activity.model;

public class Banner {
    private String id;
    private String title;
    private String subtitle;
    private String discountText;
    private String imageUrl;
    private String category;
    private boolean active;

    public Banner() {
        this.active = true;
    }

    public Banner(String id, String title, String subtitle, String discountText, String imageUrl, String category, boolean active) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.discountText = discountText;
        this.imageUrl = imageUrl;
        this.category = category;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getDiscountText() { return discountText; }
    public void setDiscountText(String discountText) { this.discountText = discountText; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
