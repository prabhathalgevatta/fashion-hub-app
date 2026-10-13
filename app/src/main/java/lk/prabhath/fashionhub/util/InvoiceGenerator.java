package lk.prabhath.fashionhub.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.prabhath.fashionhub.activity.model.Order;

public class InvoiceGenerator {

    /**
     * Generates a PDF invoice for the given order and launches an intent to open/view it.
     * If the order has no items populated, it fetches the full order details from Firestore first.
     */
    public static void generateAndOpenInvoice(Context context, Order order) {
        if (context == null || order == null) return;

        if (order.getItems() == null || order.getItems().isEmpty()) {
            if (order.getOrderId() != null && !order.getOrderId().isEmpty()) {
                Toast.makeText(context, "Preparing invoice...", Toast.LENGTH_SHORT).show();
                FirebaseFirestore.getInstance().collection("orders").document(order.getOrderId())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                Order fullOrder = doc.toObject(Order.class);
                                if (fullOrder != null) {
                                    if (fullOrder.getOrderId() == null) fullOrder.setOrderId(doc.getId());
                                    createAndLaunchPdf(context, fullOrder);
                                    return;
                                }
                            }
                            createAndLaunchPdf(context, order);
                        })
                        .addOnFailureListener(e -> createAndLaunchPdf(context, order));
                return;
            }
        }

        createAndLaunchPdf(context, order);
    }

    private static void createAndLaunchPdf(Context context, Order order) {
        try {
            File pdfFile = createInvoicePdfFile(context, order);
            if (pdfFile != null && pdfFile.exists()) {
                Toast.makeText(context, "Invoice saved: " + pdfFile.getName(), Toast.LENGTH_SHORT).show();
                openPdf(context, pdfFile);
            } else {
                Toast.makeText(context, "Failed to create invoice PDF.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating invoice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates the PDF document and writes it to the app's external files documents folder.
     */
    public static File createInvoicePdfFile(Context context, Order order) throws Exception {
        PdfDocument document = new PdfDocument();
        // A4 page: 595 x 842 points
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        String shortId = (order.getOrderId() != null && order.getOrderId().length() >= 8)
                ? order.getOrderId().substring(0, 8).toUpperCase()
                : (order.getOrderId() != null ? order.getOrderId() : "FH" + System.currentTimeMillis());

        // 1. Header Banner
        paint.setColor(Color.parseColor("#1E1B4B")); // Deep Luxury Indigo
        canvas.drawRect(0, 0, 595, 95, paint);

        // Accent gold stripe
        paint.setColor(Color.parseColor("#F59E0B"));
        canvas.drawRect(0, 95, 595, 98, paint);

        // Brand Name
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("FASHIONHUB", 40, 50, paint);

        paint.setTextSize(9);
        paint.setColor(Color.parseColor("#CBD5E1"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("PREMIUM APPAREL & LIFESTYLE STORE", 40, 68, paint);

        // Header Right - TAX INVOICE
        paint.setColor(Color.parseColor("#F59E0B"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("TAX INVOICE", 440, 48, paint);

        String dateStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        paint.setColor(Color.WHITE);
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Date: " + dateStr, 440, 68, paint);

        // 2. Customer & Order Metadata Block (y = 120 to 195)
        // Left Column: Customer details
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("BILLED TO / DELIVER TO:", 40, 122, paint);

        String custName = (order.getCustomerName() != null && !order.getCustomerName().trim().isEmpty())
                ? order.getCustomerName().trim() : "Valued Customer";
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(custName, 40, 139, paint);

        paint.setTextSize(9);
        paint.setColor(Color.parseColor("#334155"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        int custY = 153;
        if (order.getCustomerPhone() != null && !order.getCustomerPhone().trim().isEmpty()) {
            canvas.drawText("Phone: " + order.getCustomerPhone().trim(), 40, custY, paint);
            custY += 13;
        }
        String email = (order.getUserEmail() != null && !order.getUserEmail().isEmpty())
                ? order.getUserEmail()
                : ((order.getCustomerEmail() != null) ? order.getCustomerEmail() : "");
        if (!email.trim().isEmpty()) {
            canvas.drawText("Email: " + email.trim(), 40, custY, paint);
            custY += 13;
        }

        String address = (order.getDeliveryAddress() != null && !order.getDeliveryAddress().trim().isEmpty())
                ? order.getDeliveryAddress().trim() : "Standard Home Delivery";
        if (address.length() > 40) {
            canvas.drawText("Address: " + address.substring(0, 38) + "...", 40, custY, paint);
        } else {
            canvas.drawText("Address: " + address, 40, custY, paint);
        }

        // Right Column: Order details
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("ORDER DETAILS:", 340, 122, paint);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Order ID: #" + shortId, 340, 139, paint);

        paint.setTextSize(9);
        paint.setColor(Color.parseColor("#334155"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        String tracking = (order.getTrackingCode() != null && !order.getTrackingCode().isEmpty())
                ? order.getTrackingCode() : "TRK-" + shortId;
        canvas.drawText("Tracking: " + tracking, 340, 153, paint);

        String status = (order.getStatus() != null) ? order.getStatus() : "Pending";
        canvas.drawText("Order Status: " + status, 340, 166, paint);
        canvas.drawText("Payment: Confirmed (Paid Online)", 340, 179, paint);

        // 3. Items Table Header (y = 215)
        paint.setColor(Color.parseColor("#F1F5F9"));
        RectF headerRect = new RectF(40, 215, 555, 239);
        canvas.drawRoundRect(headerRect, 4, 4, paint);

        paint.setColor(Color.parseColor("#334155"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("#", 48, 231, paint);
        canvas.drawText("ITEM DESCRIPTION", 75, 231, paint);
        canvas.drawText("SIZE", 290, 231, paint);
        canvas.drawText("QTY", 345, 231, paint);
        canvas.drawText("UNIT PRICE (RS)", 395, 231, paint);
        canvas.drawText("TOTAL (RS)", 485, 231, paint);

        // 4. Items Table Rows
        int rowY = 258;
        List<Map<String, Object>> items = order.getItems();

        if (items != null && !items.isEmpty()) {
            int index = 1;
            for (Map<String, Object> item : items) {
                String name = item.containsKey("productName") ? String.valueOf(item.get("productName")) : "Fashion Item";
                if (name.length() > 32) name = name.substring(0, 30) + "...";
                String size = item.containsKey("size") ? String.valueOf(item.get("size")) : "M";

                int qty = 1;
                if (item.containsKey("qty") && item.get("qty") instanceof Number) {
                    qty = ((Number) item.get("qty")).intValue();
                }

                double unitPrice = 0.0;
                if (item.containsKey("price") && item.get("price") instanceof Number) {
                    unitPrice = ((Number) item.get("price")).doubleValue();
                }
                double lineTotal = unitPrice * qty;

                paint.setColor(Color.parseColor("#1E293B"));
                paint.setTextSize(9);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

                canvas.drawText(String.valueOf(index), 48, rowY, paint);
                canvas.drawText(name, 75, rowY, paint);
                canvas.drawText(size, 290, rowY, paint);
                canvas.drawText(String.valueOf(qty), 350, rowY, paint);
                canvas.drawText(String.format(Locale.US, "%.2f", unitPrice), 395, rowY, paint);
                canvas.drawText(String.format(Locale.US, "%.2f", lineTotal), 485, rowY, paint);

                // Row underline
                paint.setColor(Color.parseColor("#E2E8F0"));
                canvas.drawLine(40, rowY + 7, 555, rowY + 7, paint);

                rowY += 24;
                index++;
                if (rowY > 620) break; // Keep within single page
            }
        } else {
            // Fallback single line if item collection details were not embedded
            paint.setColor(Color.parseColor("#1E293B"));
            paint.setTextSize(9);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            canvas.drawText("1", 48, rowY, paint);
            canvas.drawText("Apparel Collection (Order #" + shortId + ")", 75, rowY, paint);
            canvas.drawText("Std", 290, rowY, paint);
            canvas.drawText("1", 350, rowY, paint);
            canvas.drawText(String.format(Locale.US, "%.2f", order.getTotalAmount()), 395, rowY, paint);
            canvas.drawText(String.format(Locale.US, "%.2f", order.getTotalAmount()), 485, rowY, paint);

            paint.setColor(Color.parseColor("#E2E8F0"));
            canvas.drawLine(40, rowY + 7, 555, rowY + 7, paint);
            rowY += 24;
        }

        // 5. Summary / Totals Block (Right aligned)
        int summaryY = Math.max(rowY + 16, 360);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(9.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Subtotal:", 370, summaryY, paint);

        paint.setColor(Color.parseColor("#1E293B"));
        canvas.drawText(String.format(Locale.US, "Rs. %.2f", order.getTotalAmount()), 470, summaryY, paint);

        summaryY += 18;
        paint.setColor(Color.parseColor("#64748B"));
        canvas.drawText("Delivery (Express):", 370, summaryY, paint);
        paint.setColor(Color.parseColor("#059669"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("FREE", 470, summaryY, paint);

        // Grand Total Pill
        summaryY += 12;
        RectF totalPill = new RectF(350, summaryY, 555, summaryY + 34);
        paint.setColor(Color.parseColor("#1E1B4B"));
        canvas.drawRoundRect(totalPill, 6, 6, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("GRAND TOTAL:", 365, summaryY + 21, paint);

        paint.setColor(Color.parseColor("#FBBF24"));
        paint.setTextSize(12);
        canvas.drawText(String.format(Locale.US, "Rs. %.2f", order.getTotalAmount()), 465, summaryY + 21, paint);

        // 6. Footer (Bottom of page)
        paint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawLine(40, 740, 555, 740, paint);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        drawCenteredText(canvas, paint, "Thank you for shopping with FashionHub!", 297, 760);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        drawCenteredText(canvas, paint, "Exchanges accepted within 7 days with this invoice and original garment tags intact.", 297, 776);
        drawCenteredText(canvas, paint, "Customer Care: +94 71 234 5678  •  Email: support@fashionhub.lk  •  Colombo, Sri Lanka", 297, 792);

        document.finishPage(page);

        // Save file
        File docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docsDir != null && !docsDir.exists()) {
            docsDir.mkdirs();
        }
        File pdfFile = new File(docsDir, "FashionHub_Invoice_" + shortId + ".pdf");
        FileOutputStream fos = new FileOutputStream(pdfFile);
        document.writeTo(fos);
        document.close();
        fos.close();

        return pdfFile;
    }

    private static void drawCenteredText(Canvas canvas, Paint paint, String text, float cx, float y) {
        float textWidth = paint.measureText(text);
        canvas.drawText(text, cx - (textWidth / 2f), y, paint);
    }

    /**
     * Opens the PDF file using an external viewer application via FileProvider.
     */
    public static void openPdf(Context context, File pdfFile) {
        try {
            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent chooser = Intent.createChooser(intent, "Open Invoice With");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No PDF viewer app found. Saved to Documents: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to open invoice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
