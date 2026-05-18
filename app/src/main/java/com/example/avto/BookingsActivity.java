package com.example.avto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookingsActivity extends AppCompatActivity implements BookingsAdapter.OnBookingActionListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private TextView emptyTitle;
    private TextView emptySubtitle;
    private MaterialButton btnGoToMain;
    private BookingsAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();
    private ImageView backButton;

    private TextView tabAll, tabActive, tabCompleted;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupListeners();
        loadBookings();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        recyclerView = findViewById(R.id.recyclerViewBookings);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptySubtitle = findViewById(R.id.emptySubtitle);
        btnGoToMain = findViewById(R.id.btnGoToMain);

        tabAll = findViewById(R.id.tabAll);
        tabActive = findViewById(R.id.tabActive);
        tabCompleted = findViewById(R.id.tabCompleted);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        backButton.setOnClickListener(v -> finish());
        btnGoToMain.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Устанавливаем активный таб по умолчанию
        setActiveTab(tabAll);
    }

    private void setupListeners() {
        tabAll.setOnClickListener(v -> {
            setActiveTab(tabAll);
            currentFilter = "all";
            filterBookings();
        });

        tabActive.setOnClickListener(v -> {
            setActiveTab(tabActive);
            currentFilter = "active";
            filterBookings();
        });

        tabCompleted.setOnClickListener(v -> {
            setActiveTab(tabCompleted);
            currentFilter = "completed";
            filterBookings();
        });
    }

    private void setActiveTab(TextView activeTab) {
        resetTabStyle(tabAll);
        resetTabStyle(tabActive);
        resetTabStyle(tabCompleted);

        activeTab.setBackgroundResource(R.drawable.tab_selected);
        activeTab.setTextColor(getColor(R.color.white));
        activeTab.setTextSize(14);
        activeTab.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void resetTabStyle(TextView tab) {
        tab.setBackgroundResource(R.drawable.tab_unselected);
        tab.setTextColor(getColor(R.color.text_secondary));
        tab.setTextSize(14);
        tab.setTypeface(Typeface.DEFAULT);
    }

    private void loadBookings() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("user_email", "");

        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new Thread(() -> {
            allBookings.clear();
            allBookings.addAll(dbHelper.getUserBookings(userEmail));

            // Сортируем по дате (сначала новые)
            allBookings.sort((b1, b2) -> b2.getBookingDate().compareTo(b1.getBookingDate()));

            runOnUiThread(() -> {
                filterBookings();
            });
        }).start();
    }

    private boolean isBookingActive(Booking booking) {
        if ("cancelled".equals(booking.getStatus()) || "completed".equals(booking.getStatus())) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(Calendar.getInstance().getTime());

        // Бронирование активно, если дата окончания еще не прошла
        return booking.getEndDate().compareTo(today) >= 0;
    }

    private void filterBookings() {
        filteredBookings.clear();

        for (Booking booking : allBookings) {
            switch (currentFilter) {
                case "all":
                    filteredBookings.add(booking);
                    break;
                case "active":
                    if (isBookingActive(booking)) {
                        filteredBookings.add(booking);
                    }
                    break;
                case "completed":
                    if ("completed".equals(booking.getStatus())) {
                        filteredBookings.add(booking);
                    }
                    break;
            }
        }

        if (filteredBookings.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);

            if (currentFilter.equals("active")) {
                emptyTitle.setText("Нет активных бронирований");
                emptySubtitle.setText("У вас пока нет активных бронирований.\nВыберите автомобиль и начните аренду!");
            } else if (currentFilter.equals("completed")) {
                emptyTitle.setText("Нет завершённых бронирований");
                emptySubtitle.setText("У вас пока нет завершённых бронирований.\nСовершите первую аренду!");
            } else {
                emptyTitle.setText("Нет бронирований");
                emptySubtitle.setText("У вас пока нет бронирований.\nНачните аренду прямо сейчас!");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            adapter = new BookingsAdapter(filteredBookings, this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onBookingClick(Booking booking) {
        // Открываем детали бронирования
        Intent intent = new Intent(this, BookingsDetailActivity.class);
        intent.putExtra("booking_id", booking.getId());
        intent.putExtra("car_name", booking.getCarName());
        intent.putExtra("start_date", booking.getStartDate());
        intent.putExtra("end_date", booking.getEndDate());
        intent.putExtra("total_price", booking.getTotalPrice());
        intent.putExtra("status", booking.getStatus());
        startActivity(intent);
    }

    @Override
    public void onCancelClick(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Отмена бронирования")
                .setMessage("Вы уверены, что хотите отменить бронирование автомобиля " + booking.getCarName() + "?")
                .setPositiveButton("Отменить", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = dbHelper.cancelBooking(booking.getId());
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Бронирование отменено", Toast.LENGTH_SHORT).show();
                                loadBookings();
                            } else {
                                Toast.makeText(this, "Ошибка при отмене", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Назад", null)
                .show();
    }

    @Override
    public void onCompleteClick(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Завершение бронирования")
                .setMessage("Подтвердите завершение аренды автомобиля " + booking.getCarName() + "?")
                .setPositiveButton("Завершить", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = dbHelper.completeBooking(booking.getId());
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Бронирование завершено", Toast.LENGTH_SHORT).show();
                                loadBookings();
                            } else {
                                Toast.makeText(this, "Ошибка при завершении", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Назад", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}