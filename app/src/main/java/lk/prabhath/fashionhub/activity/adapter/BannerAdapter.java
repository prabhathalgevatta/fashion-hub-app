package lk.prabhath.fashionhub.activity.adapter;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.model.Banner;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    public interface OnBannerClickListener {
        void onBannerClick(Banner banner);
    }

    private final List<Banner> bannerList;
    private final OnBannerClickListener listener;

    public BannerAdapter(List<Banner> bannerList, OnBannerClickListener listener) {
        this.bannerList = bannerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = bannerList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(banner.getTitle() != null ? banner.getTitle() : "");

        if (banner.getSubtitle() != null && !banner.getSubtitle().trim().isEmpty()) {
            holder.tvSubtitle.setText(banner.getSubtitle());
            holder.tvSubtitle.setVisibility(View.VISIBLE);
        } else {
            holder.tvSubtitle.setVisibility(View.GONE);
        }

        if (banner.getDiscountText() != null && !banner.getDiscountText().trim().isEmpty()) {
            holder.tvBadge.setText(banner.getDiscountText());
            holder.tvBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        // Load image (support both standard URL & Base64 Data URL)
        String imageUrl = banner.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            if (imageUrl.startsWith("data:image") && imageUrl.contains(",")) {
                try {
                    String base64 = imageUrl.substring(imageUrl.indexOf(",") + 1);
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                    Glide.with(context)
                            .asBitmap()
                            .load(bytes)
                            .centerCrop()
                            .into(holder.ivBanner);
                } catch (Exception e) {
                    Glide.with(context).load(imageUrl).centerCrop().into(holder.ivBanner);
                }
            } else {
                Glide.with(context)
                        .load(imageUrl)
                        .centerCrop()
                        .into(holder.ivBanner);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(banner);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;
        TextView tvBadge;
        TextView tvTitle;
        TextView tvSubtitle;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.iv_banner_image);
            tvBadge = itemView.findViewById(R.id.tv_banner_badge);
            tvTitle = itemView.findViewById(R.id.tv_banner_title);
            tvSubtitle = itemView.findViewById(R.id.tv_banner_subtitle);
        }
    }
}
