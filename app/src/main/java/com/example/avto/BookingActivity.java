package com.example.avto;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private Car car;
    private String userEmail;
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private TextView carNameTextView;
    private TextView carDetailsTextView;
    private TextView carPriceTextView;
    private TextView startDateTextView;
    private TextView endDateTextView;
    private TextView totalDaysTextView;
    private TextView totalPriceTextView;
    private Button confirmBookingButton;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        Intent intent = getIntent();
        car = (Car) intent.getSerializableExtra("car");
        if (car == null) {
            Toast.makeText(this, "Ошибка: данные автомобиля не получены", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!car.isAvailable()) {
            Toast.makeText(this, "Этот автомобиль в данный момент недоступен для бронирования", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);
        userEmail = sharedPreferences.getString("user_email", "");
        dbHelper = DatabaseHelper.getInstance(this);

        carNameTextView = findViewById(R.id.carNameTextView);
        carDetailsTextView = findViewById(R.id.carDetailsTextView);
        carPriceTextView = findViewById(R.id.carPriceTextView);
        startDateTextView = findViewById(R.id.startDateTextView);
        endDateTextView = findViewById(R.id.endDateTextView);
        totalDaysTextView = findViewById(R.id.totalDaysTextView);
        totalPriceTextView = findViewById(R.id.totalPriceTextView);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        carNameTextView.setText(car.getName());
        carDetailsTextView.setText(car.getDescription());
        carPriceTextView.setText(car.getFormattedPrice() + "/день");

        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);

        updateDateTextViews();
        calculateTotal();

        startDateTextView.setOnClickListener(v -> showDatePicker(true));
        endDateTextView.setOnClickListener(v -> showDatePicker(false));

        confirmBookingButton.setOnClickListener(v -> confirmBooking());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDateCalendar : endDateCalendar;
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    updateDateTextViews();
                    calculateTotal();

                    if (!isStartDate && endDateCalendar.before(startDateCalendar)) {
                        endDateCalendar.setTime(startDateCalendar.getTime());
                        endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
                        updateDateTextViews();
                        calculateTotal();
                        Toast.makeText(this, "Дата окончания не может быть раньше даты начала", Toast.LENGTH_SHORT).show();
                    }
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDateTextViews() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        startDateTextView.setText(sdf.format(startDateCalendar.getTime()));
        endDateTextView.setText(sdf.format(endDateCalendar.getTime()));
    }

    private void calculateTotal() {
        long diff = endDateCalendar.getTimeInMillis() - startDateCalendar.getTimeInMillis();
        int days = (int) (diff / (1000 * 60 * 60 * 24));

        if (days < 1) days = 1;

        int totalPrice = days * car.getPricePerDay();

        totalDaysTextView.setText(days + " " + getDayWord(days));
        totalPriceTextView.setText(String.format("%,d ₽", totalPrice));
    }

    private String getDayWord(int days) {
        if (days % 10 == 1 && days % 100 != 11) {
            return "день";
        } else if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) {
            return "дня";
        } else {
            return "дней";
        }
    }

    private void confirmBooking() {
        if (!isProfileFilled()) {
            Toast.makeText(this, "Пожалуйста, заполните паспортные данные и водительское удостоверение в профиле", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(startDateCalendar.getTime());
        String endDate = sdf.format(endDateCalendar.getTime());

        if (dbHelper.isCarBooked(car.getId(), startDate, endDate)) {
            Toast.makeText(this, "Автомобиль уже забронирован на выбранные даты", Toast.LENGTH_LONG).show();
            return;
        }

        long diff = endDateCalendar.getTimeInMillis() - startDateCalendar.getTimeInMillis();
        int totalDays = (int) (diff / (1000 * 60 * 60 * 24));
        if (totalDays < 1) totalDays = 1;
        int totalPrice = totalDays * car.getPricePerDay();

        String bookingDate = sdf.format(Calendar.getInstance().getTime());

        Booking booking = new Booking(car.getId(), userEmail, car.getName(), car.getPricePerDay(), startDate, endDate, totalDays, totalPrice, "active", bookingDate);

        long bookingId = dbHelper.addBooking(booking);

        if (bookingId != -1) {
            Toast.makeText(this, "Бронирование успешно создано!", Toast.LENGTH_LONG).show();
            Log.d("BookingActivity", "Booking created with ID: " + bookingId + " for car: " + car.getId());

            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);

            Intent bookingsIntent = new Intent(this, BookingsActivity.class);
            startActivity(bookingsIntent);
            finish();
        } else {
            Toast.makeText(this, "Ошибка при создании бронирования", Toast.LENGTH_SHORT).show();
            Log.e("BookingActivity", "Failed to create booking for car: " + car.getId());
        }
    }

    private boolean isProfileFilled() {
        if (userEmail == null || userEmail.isEmpty()) {
            return false;
        }

        Cursor cursor = dbHelper.getUserProfile(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            int passportSeriesIndex = cursor.getColumnIndex("passport_series");
            int passportNumberIndex = cursor.getColumnIndex("passport_number");
            int licenseNumberIndex = cursor.getColumnIndex("license_number");

            String passportSeries = (passportSeriesIndex != -1) ? cursor.getString(passportSeriesIndex) : null;
            String passportNumber = (passportNumberIndex != -1) ? cursor.getString(passportNumberIndex) : null;
            String licenseNumber = (licenseNumberIndex != -1) ? cursor.getString(licenseNumberIndex) : null;

            cursor.close();

            return passportSeries != null && !passportSeries.isEmpty() &&
                    passportNumber != null && !passportNumber.isEmpty() &&
                    licenseNumber != null && !licenseNumber.isEmpty();
        }

        if (cursor != null) {
            cursor.close();
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}