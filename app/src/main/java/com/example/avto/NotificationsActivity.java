package com.example.avto;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private DatabaseHelper dbHelper;
    private ImageView backArrow;
    private TextView emptyText;
    private ImageView clearAllButton;
    private TextView notificationsTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        dbHelper = DatabaseHelper.getInstance(this);
        backArrow = findViewById(R.id.backArrow);
        emptyText = findViewById(R.id.emptyText);
        recyclerView = findViewById(R.id.recyclerView);
        clearAllButton = findViewById(R.id.clearAllButton);
        notificationsTitle = findViewById(R.id.notificationsTitle);

        backArrow.setOnClickListener(v -> finish());

        // Кнопка очистки всех уведомлений
        if (clearAllButton != null) {
            clearAllButton.setOnClickListener(v -> showClearAllDialog());
        }

        setupRecyclerView();
        loadNotifications();
        updateTitle();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationsAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        try {
            String userEmail = getSharedPreferences("user_profile", MODE_PRIVATE)
                    .getString("user_email", "");

            if (userEmail.isEmpty()) {
                Toast.makeText(this, "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Cursor cursor = dbHelper.getUserNotifications(userEmail);
            List<NotificationItem> notifications = new ArrayList<>();

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("id"));
                    @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex("title"));
                    @SuppressLint("Range") String message = cursor.getString(cursor.getColumnIndex("message"));
                    @SuppressLint("Range") String type = cursor.getString(cursor.getColumnIndex("type"));
                    @SuppressLint("Range") int isRead = cursor.getInt(cursor.getColumnIndex("is_read"));
                    @SuppressLint("Range") String timestamp = cursor.getString(cursor.getColumnIndex("timestamp"));

                    notifications.add(new NotificationItem(id, title, message, type, isRead == 1, timestamp));
                } while (cursor.moveToNext());
                cursor.close();
            }

            updateUI(notifications);
        } catch (Exception e) {
            Log.e("NotificationsActivity", "Error loading notifications: " + e.getMessage());
            Toast.makeText(this, "Ошибка загрузки уведомлений", Toast.LENGTH_SHORT).show();
            showEmptyState();
        }
    }

    private void updateUI(List<NotificationItem> notifications) {
        if (notifications.isEmpty()) {
            showEmptyState();
        } else {
            showNotificationsList(notifications);
        }
    }

    private void showEmptyState() {
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        if (clearAllButton != null) {
            clearAllButton.setVisibility(View.GONE);
        }
    }

    private void showNotificationsList(List<NotificationItem> notifications) {
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        if (clearAllButton != null) {
            clearAllButton.setVisibility(View.VISIBLE);
        }
        adapter.setNotifications(notifications);
    }

    private void showClearAllDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Очистить все уведомления")
                .setMessage("Вы уверены, что хотите удалить все уведомления?")
                .setPositiveButton("Удалить", (dialog, which) -> clearAllNotifications())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void clearAllNotifications() {
        String userEmail = getSharedPreferences("user_profile", MODE_PRIVATE)
                .getString("user_email", "");

        if (!userEmail.isEmpty()) {
            boolean success = dbHelper.deleteAllUserNotifications(userEmail);
            if (success) {
                Toast.makeText(this, "Все уведомления удалены", Toast.LENGTH_SHORT).show();
                adapter.setNotifications(new ArrayList<>());
                showEmptyState();
                updateTitle();
            } else {
                Toast.makeText(this, "Ошибка при удалении уведомлений", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateTitle() {
        String userEmail = getSharedPreferences("user_profile", MODE_PRIVATE)
                .getString("user_email", "");

        if (!userEmail.isEmpty() && notificationsTitle != null) {
            int unreadCount = dbHelper.getUnreadNotificationsCount(userEmail);
            String title = "Уведомления";
            if (unreadCount > 0) {
                title += " (" + unreadCount + " непрочитанных)";
            }
            notificationsTitle.setText(title);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
        updateTitle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // НЕ ЗАКРЫВАЕМ БД здесь!
    }

    // Класс для элемента уведомления
    public static class NotificationItem {
        public int id;
        public String title;
        public String message;
        public String type;
        public boolean isRead;
        public String timestamp;

        public NotificationItem(int id, String title, String message, String type, boolean isRead, String timestamp) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
            this.isRead = isRead;
            this.timestamp = timestamp;
        }
    }

    // Адаптер для RecyclerView
    public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
        private List<NotificationItem> notifications = new ArrayList<>();

        public void setNotifications(List<NotificationItem> notifications) {
            this.notifications = notifications;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            NotificationItem notification = notifications.get(position);
            holder.bind(notification);
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleText;
            TextView messageText;
            TextView timeText;
            View unreadIndicator;

            public ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.notificationTitle);
                messageText = itemView.findViewById(R.id.notificationMessage);
                timeText = itemView.findViewById(R.id.notificationTime);
                unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            }

            public void bind(NotificationItem notification) {
                titleText.setText(notification.title);
                messageText.setText(notification.message);

                // Форматируем дату
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    Date date = inputFormat.parse(notification.timestamp);
                    timeText.setText(outputFormat.format(date));
                } catch (Exception e) {
                    timeText.setText(notification.timestamp);
                }

                // Визуальное отображение прочитанных/непрочитанных
                if (notification.isRead) {
                    itemView.setAlpha(0.7f);
                    if (unreadIndicator != null) {
                        unreadIndicator.setVisibility(View.GONE);
                    }
                    itemView.setOnClickListener(null);
                } else {
                    itemView.setAlpha(1.0f);
                    if (unreadIndicator != null) {
                        unreadIndicator.setVisibility(View.VISIBLE);
                    }

                    // Помечаем как прочитанное при клике
                    itemView.setOnClickListener(v -> {
                        boolean success = dbHelper.markNotificationAsRead(notification.id);
                        if (success) {
                            notification.isRead = true;
                            notifyItemChanged(getAdapterPosition());
                            updateTitle();
                        }
                    });
                }

                // Долгий клик для удаления отдельного уведомления
                itemView.setOnLongClickListener(v -> {
                    showDeleteDialog(notification);
                    return true;
                });
            }
        }
    }

    private void showDeleteDialog(NotificationItem notification) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удалить уведомление")
                .setMessage("Вы уверены, что хотите удалить это уведомление?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteNotification(notification))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteNotification(NotificationItem notification) {
        boolean success = dbHelper.deleteNotification(notification.id);
        if (success) {
            Toast.makeText(this, "Уведомление удалено", Toast.LENGTH_SHORT).show();
            loadNotifications();
            updateTitle();
        } else {
            Toast.makeText(this, "Ошибка при удалении уведомления", Toast.LENGTH_SHORT).show();
        }
    }
}