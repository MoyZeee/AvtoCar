package com.example.avto;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BookingsActivity extends AppCompatActivity implements BookingsAdapter.OnBookingActionListener {

    private RecyclerView recyclerView;
    private TextView noBookingsText;
    private BookingsAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<Booking> bookingsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        dbHelper = DatabaseHelper.getInstance(this);

        recyclerView = findViewById(R.id.recyclerViewBookings);
        noBookingsText = findViewById(R.id.noBookingsText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingsAdapter(bookingsList, this);
        recyclerView.setAdapter(adapter);

        loadBookings();
    }

    private void loadBookings() {
        String userEmail = getSharedPreferences("user_profile", MODE_PRIVATE)
                .getString("user_email", "");

        if (!userEmail.isEmpty()) {
            bookingsList = dbHelper.getUserBookings(userEmail);

            if (bookingsList.isEmpty()) {
                noBookingsText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                noBookingsText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.updateBookings(bookingsList);
            }
        }
    }

    @Override
    public void onCancelClick(Booking booking) {
        boolean cancelled = dbHelper.cancelBooking(booking.getId());
        if (cancelled) {
            loadBookings();
        }
    }

    @Override
    public void onCompleteClick(Booking booking) {
        boolean completed = dbHelper.completeBooking(booking.getId());
        if (completed) {
            loadBookings();
        }
    }

    @Override
    public void onEditClick(Booking booking) {
        // Реализуйте при необходимости
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}