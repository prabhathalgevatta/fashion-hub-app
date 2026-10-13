package lk.prabhath.fashionhub.activity.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import lk.prabhath.fashionhub.R;

public class NotificationAdapter extends BaseAdapter {

    private Context context;
    private List<Map<String, String>> notifications;

    public NotificationAdapter(Context context, List<Map<String, String>> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    @Override
    public int getCount() {
        return notifications.size();
    }

    @Override
    public Object getItem(int position) {
        return notifications.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        }

        Map<String, String> notif = notifications.get(position);

        TextView tvTitle = convertView.findViewById(R.id.tv_notif_title);
        TextView tvMessage = convertView.findViewById(R.id.tv_notif_message);
        TextView tvTime = convertView.findViewById(R.id.tv_notif_time);

        tvTitle.setText(notif.get("title"));
        tvMessage.setText(notif.get("message"));
        tvTime.setText(notif.get("timestamp"));

        return convertView;
    }
}
