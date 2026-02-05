package com.example.avto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    private List<Booking> bookingsList;
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onCancelClick(Booking booking);
        void onCompleteClick(Booking booking);
        void onEditClick(Booking booking);
    }

    public BookingsAdapter(List<Booking> bookingsList, OnBookingActionListener listener) {
        this.bookingsList = bookingsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingsList.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookingsList.size();
    }

    public void setBookings(List<Booking> bookings) {
        this.bookingsList = bookings;
        notifyDataSetChanged();
    }

    public void updateBookings(List<Booking> newBookings) {
        bookingsList = newBookings;
        notifyDataSetChanged();
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        private TextView carNameTextView;
        private TextView datesTextView;
        private TextView priceTextView;
        private TextView statusTextView;
        private TextView daysTextView;
        private Button cancelButton;
        private Button completeButton;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            carNameTextView = itemView.findViewById(R.id.carNameTextView);
            datesTextView = itemView.findViewById(R.id.datesTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            daysTextView = itemView.findViewById(R.id.daysTextView);
            cancelButton = itemView.findViewById(R.id.cancelButton);
            completeButton = itemView.findViewById(R.id.completeButton);
        }

        void bind(Booking booking) {
            carNameTextView.setText(booking.getCarName());
            datesTextView.setText(booking.getFormattedDates());
            priceTextView.setText(booking.getFormattedTotalPrice());
            daysTextView.setText(booking.getTotalDays() + " дней");

            String status = booking.getStatus();
            statusTextView.setText(booking.getStatusInRussian());

            cancelButton.setVisibility(View.GONE);
            completeButton.setVisibility(View.GONE);

            switch (status) {
                case "active":
                    statusTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green));

                    if (isBeforeStartDate(booking.getStartDate())) {
                        cancelButton.setVisibility(View.VISIBLE);
                        cancelButton.setText("Отменить");
                        cancelButton.setEnabled(true);
                    } else if (isAfterEndDate(booking.getEndDate())) {
                        completeButton.setVisibility(View.VISIBLE);
                        completeButton.setText("Завершить");
                        completeButton.setEnabled(true);
                    } else {
                        cancelButton.setVisibility(View.VISIBLE);
                        cancelButton.setText("Отменить");
                        cancelButton.setEnabled(true);
                    }
                    break;

                case "completed":
                    statusTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.blue));
                    break;

                case "cancelled":
                    statusTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.red));
                    break;
            }

            cancelButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancelClick(booking);
                }
            });

            completeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCompleteClick(booking);
                }
            });
        }

        private boolean isBeforeStartDate(String startDateStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date startDate = sdf.parse(startDateStr);
                Date today = new Date();

                sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                int start = Integer.parseInt(sdf.format(startDate));
                int current = Integer.parseInt(sdf.format(today));

                return current < start;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }

        private boolean isAfterEndDate(String endDateStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date endDate = sdf.parse(endDateStr);
                Date today = new Date();

                sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                int end = Integer.parseInt(sdf.format(endDate));
                int current = Integer.parseInt(sdf.format(today));

                return current > end;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}