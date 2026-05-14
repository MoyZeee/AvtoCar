package com.example.avto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

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
        void onBookingClick(Booking booking);
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
        return bookingsList != null ? bookingsList.size() : 0;
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
        private MaterialButton cancelButton;
        private MaterialButton completeButton;
        private View actionsLayout;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            carNameTextView = itemView.findViewById(R.id.carNameTextView);
            datesTextView = itemView.findViewById(R.id.datesTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            daysTextView = itemView.findViewById(R.id.daysTextView);
            cancelButton = itemView.findViewById(R.id.cancelButton);
            completeButton = itemView.findViewById(R.id.completeButton);
            actionsLayout = itemView.findViewById(R.id.actionsLayout);

            // Добавляем обработчик клика на всю карточку
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBookingClick(bookingsList.get(position));
                }
            });
        }

        void bind(Booking booking) {
            if (booking == null) return;

            // Базовые данные
            carNameTextView.setText(booking.getCarName());
            datesTextView.setText(booking.getFormattedDates());
            priceTextView.setText(booking.getFormattedTotalPrice());

            int days = booking.getTotalDays();
            daysTextView.setText(days + " " + getDayWord(days));

            // Статус
            String status = booking.getStatus();
            String statusText = booking.getStatusInRussian();
            statusTextView.setText(statusText);

            // Установка цвета и фона статуса
            int backgroundColor;
            int textColor;

            switch (status) {
                case "active":
                case "paid":
                    backgroundColor = R.color.green;
                    textColor = android.R.color.white;
                    break;
                case "pending_payment":
                    backgroundColor = R.color.orange;
                    textColor = android.R.color.white;
                    break;
                case "completed":
                    backgroundColor = R.color.blue;
                    textColor = android.R.color.white;
                    break;
                case "cancelled":
                case "payment_failed":
                    backgroundColor = R.color.red;
                    textColor = android.R.color.white;
                    break;
                default:
                    backgroundColor = R.color.text_secondary;
                    textColor = android.R.color.white;
            }

            try {
                statusTextView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), backgroundColor));
                statusTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
            } catch (Exception e) {
                // Используем дефолтные цвета
                statusTextView.setBackgroundColor(0xFF4CAF50); // зеленый
                statusTextView.setTextColor(0xFFFFFFFF); // белый
            }

            // Управление видимостью кнопок
            actionsLayout.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            completeButton.setVisibility(View.GONE);

            if (status.equals("active") || status.equals("paid") || status.equals("pending_payment")) {
                actionsLayout.setVisibility(View.VISIBLE);

                if (isBeforeStartDate(booking.getStartDate())) {
                    // До начала бронирования - можно только отменить
                    cancelButton.setVisibility(View.VISIBLE);
                    cancelButton.setText("Отменить");
                    cancelButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onCancelClick(booking);
                        }
                    });

                    completeButton.setVisibility(View.GONE);
                } else if (isAfterEndDate(booking.getEndDate())) {
                    // После окончания бронирования - можно завершить
                    completeButton.setVisibility(View.VISIBLE);
                    completeButton.setText("Завершить");
                    completeButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onCompleteClick(booking);
                        }
                    });

                    cancelButton.setVisibility(View.GONE);
                } else {
                    // Во время бронирования - можно досрочно завершить
                    cancelButton.setVisibility(View.VISIBLE);
                    cancelButton.setText("Досрочно завершить");
                    cancelButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onCancelClick(booking);
                        }
                    });

                    completeButton.setVisibility(View.GONE);
                }
            }
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

        private boolean isBeforeStartDate(String startDateStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date startDate = sdf.parse(startDateStr);
                Date today = new Date();

                // Сравниваем только даты, без времени
                SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                int start = Integer.parseInt(dateOnlyFormat.format(startDate));
                int current = Integer.parseInt(dateOnlyFormat.format(today));

                return current < start;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private boolean isAfterEndDate(String endDateStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date endDate = sdf.parse(endDateStr);
                Date today = new Date();

                // Сравниваем только даты, без времени
                SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                int end = Integer.parseInt(dateOnlyFormat.format(endDate));
                int current = Integer.parseInt(dateOnlyFormat.format(today));

                return current > end;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}