package com.example.avto;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class BookingsDetailActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextView carNameText;
    private TextView bookingIdText;
    private TextView statusText;
    private TextView startDateText;
    private TextView endDateText;
    private TextView totalDaysText;
    private TextView totalPriceText;
    private TextView pickupLocationText;
    private TextView paymentMethodText;
    private CardView statusCard;

    private DatabaseHelper dbHelper;
    private int bookingId;
    private Booking booking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        getBookingData();
        setupListeners();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        carNameText = findViewById(R.id.carNameText);
        bookingIdText = findViewById(R.id.bookingIdText);
        statusText = findViewById(R.id.statusText);
        startDateText = findViewById(R.id.startDateText);
        endDateText = findViewById(R.id.endDateText);
        totalDaysText = findViewById(R.id.totalDaysText);
        totalPriceText = findViewById(R.id.totalPriceText);
        pickupLocationText = findViewById(R.id.pickupLocationText);
        paymentMethodText = findViewById(R.id.paymentMethodText);
        statusCard = findViewById(R.id.statusCard);
    }

    private void getBookingData() {
        bookingId = getIntent().getIntExtra("booking_id", -1);

        if (bookingId == -1) {
            Toast.makeText(this, "Ошибка: данные не найдены", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new Thread(() -> {
            booking = dbHelper.getBookingById(bookingId);
            runOnUiThread(() -> {
                if (booking != null) {
                    displayBookingDetails();
                } else {
                    Toast.makeText(this, "Бронирование не найдено", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }).start();
    }

    private void displayBookingDetails() {
        carNameText.setText(booking.getCarName());
        bookingIdText.setText("№" + booking.getId());

        // Форматируем даты
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        try {
            String startDate = outputFormat.format(inputFormat.parse(booking.getStartDate()));
            String endDate = outputFormat.format(inputFormat.parse(booking.getEndDate()));
            startDateText.setText(startDate);
            endDateText.setText(endDate);
        } catch (Exception e) {
            startDateText.setText(booking.getStartDate());
            endDateText.setText(booking.getEndDate());
        }

        totalDaysText.setText(booking.getTotalDays() + " " + getDayWord(booking.getTotalDays()));
        totalPriceText.setText(String.format(Locale.getDefault(), "%,d ₽", booking.getTotalPrice()));

        if (booking.getPickupLocation() != null && !booking.getPickupLocation().isEmpty()) {
            pickupLocationText.setText(booking.getPickupLocation());
        } else {
            pickupLocationText.setText("Самовывоз");
        }

        // Устанавливаем метод оплаты
        String paymentMethod = booking.getPaymentMethod();
        if ("credit_card".equals(paymentMethod)) {
            paymentMethodText.setText("Банковская карта");
        } else if ("bitcoin".equals(paymentMethod)) {
            paymentMethodText.setText("Bitcoin");
        } else if ("ethereum".equals(paymentMethod)) {
            paymentMethodText.setText("Ethereum");
        } else if ("usdt".equals(paymentMethod)) {
            paymentMethodText.setText("USDT");
        } else {
            paymentMethodText.setText("Не указан");
        }

        // Устанавливаем статус и цвет
        String status = booking.getStatus();
        switch (status) {
            case "active":
            case "paid":
                statusText.setText("Активно");
                statusCard.setCardBackgroundColor(getColor(R.color.success));
                break;
            case "completed":
                statusText.setText("Завершено");
                statusCard.setCardBackgroundColor(getColor(R.color.gray));
                break;
            case "cancelled":
                statusText.setText("Отменено");
                statusCard.setCardBackgroundColor(getColor(R.color.error_red));
                break;
            default:
                statusText.setText("Ожидает оплаты");
                statusCard.setCardBackgroundColor(getColor(R.color.orange_primary));
                break;
        }
    }

    private String getDayWord(int days) {
        if (days % 10 == 1 && days % 100 != 11) return "день";
        else if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) return "дня";
        else return "дней";
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
    }
}