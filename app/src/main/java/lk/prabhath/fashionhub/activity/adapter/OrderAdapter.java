package lk.prabhath.fashionhub.activity.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import lk.prabhath.fashionhub.R;
import lk.prabhath.fashionhub.activity.model.Order;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private FirebaseFirestore db;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        Context context = holder.itemView.getContext();

        String shortId = (order.getOrderId() != null && order.getOrderId().length() >= 8)
                ? order.getOrderId().substring(0, 8).toUpperCase() : order.getOrderId();
        String trackingInfo = (order.getTrackingCode() != null && !order.getTrackingCode().isEmpty())
                ? " • " + order.getTrackingCode() : "";
        holder.tvOrderId.setText("Order #" + shortId + trackingInfo);
        holder.tvTotal.setText(String.format("Rs. %.2f", order.getTotalAmount()));

        String status = order.getStatus() != null ? order.getStatus() : "Pending";
        holder.tvStatus.setText(status);

        if ("Canceled".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_canceled);
            holder.tvStatus.setTextColor(Color.parseColor("#DC2626"));
            holder.btnCancel.setVisibility(View.GONE);
        } else if ("Delivered".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_delivered);
            holder.tvStatus.setTextColor(Color.parseColor("#059669"));
            holder.btnCancel.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            holder.btnCancel.setVisibility(View.VISIBLE);
        }

        if (holder.btnInvoice != null) {
            holder.btnInvoice.setOnClickListener(v -> {
                lk.prabhath.fashionhub.util.InvoiceGenerator.generateAndOpenInvoice(context, order);
            });
        }

        holder.btnCancel.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Cancel Order")
                    .setMessage("Are you sure you want to cancel this order?")
                    .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                        db.collection("orders").document(order.getOrderId())
                                .update("status", "Canceled")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Order Canceled!", Toast.LENGTH_SHORT).show();
                                    order.setStatus("Canceled");
                                    notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Failed to cancel", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() { return orderList.size(); }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvTotal, tvStatus;
        MaterialButton btnCancel, btnInvoice;
        ImageView ivProductImage;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvTotal = itemView.findViewById(R.id.tv_order_total);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            btnCancel = itemView.findViewById(R.id.btn_cancel_order);
            btnInvoice = itemView.findViewById(R.id.btn_download_invoice);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
        }
    }
}