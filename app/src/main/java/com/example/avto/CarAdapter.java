package com.example.avto;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.ViewHolder> {

    private Context context;
    private List<Car> cars;
    private OnBookClickListener onBookClickListener;
    private Set<Integer> favorites = new HashSet<>();

    public interface OnBookClickListener {
        void onBookClick(Car car);
    }

    public CarAdapter(Context context, List<Car> cars, OnBookClickListener listener) {
        this.context = context;
        this.cars = cars;
        this.onBookClickListener = listener;
    }

    public List<Car> getCars() {
        return cars;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Car car = cars.get(position);

        // Устанавливаем изображение автомобиля
        holder.carImage.setImageResource(car.getImageResId());

        // Устанавливаем данные
        holder.carTitle.setText(car.getName());
        holder.carDetails.setText(car.getDescription());
        holder.carPrice.setText(car.getFormattedPrice());

        // Устанавливаем статус и цвет бейджа
        if (car.isAvailable()) {
            holder.statusBadge.setText("ДОСТУПНО");
            holder.statusBadge.setBackgroundResource(R.drawable.badge_available);
            holder.bookButton.setEnabled(true);
            holder.bookButton.setAlpha(1.0f);
            holder.bookButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.primary_dark)
            );
            holder.bookButton.setText("Забронировать");
            holder.bookButton.setContentDescription("Забронировать " + car.getName());
        } else {
            if (car.isOnRepair()) {
                holder.statusBadge.setText("НА РЕМОНТЕ");
                holder.statusBadge.setBackgroundResource(R.drawable.badge_maintenance);
                holder.bookButton.setText("На ремонте");
            } else {
                holder.statusBadge.setText("ЗАБРОНИРОВАН");
                holder.statusBadge.setBackgroundResource(R.drawable.badge_booked);
                holder.bookButton.setText("Забронирован");
            }
            holder.bookButton.setEnabled(false);
            holder.bookButton.setAlpha(0.5f);
            holder.bookButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.gray_500)
            );
            holder.bookButton.setContentDescription(car.getName() + " недоступен для бронирования");
        }

        // Устанавливаем доступность для элементов
        holder.cardView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        holder.carTitle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        holder.carDetails.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        holder.carPrice.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        holder.statusBadge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        // Настройка кнопки избранного
        boolean isFavorite = favorites.contains(car.getId());
        updateFavoriteButton(holder.favoriteButton, car.getId(), isFavorite);
        holder.favoriteButton.setContentDescription(isFavorite ? "Убрать из избранного" : "Добавить в избранное");

        // Обработчик клика на кнопку избранного
        holder.favoriteButton.setOnClickListener(v -> {
            boolean wasFavorite = favorites.contains(car.getId());
            if (wasFavorite) {
                favorites.remove(car.getId());
            } else {
                favorites.add(car.getId());
            }
            updateFavoriteButton(holder.favoriteButton, car.getId(), !wasFavorite);
            holder.favoriteButton.setContentDescription(!wasFavorite ? "Убрать из избранного" : "Добавить в избранное");
        });

        // Обработчик клика на кнопку бронирования
        holder.bookButton.setOnClickListener(v -> {
            if (car.isAvailable() && onBookClickListener != null) {
                onBookClickListener.onBookClick(car);
            } else if (!car.isAvailable() && !car.isOnRepair()) {
                android.widget.Toast.makeText(context,
                        "Автомобиль уже забронирован на выбранные даты",
                        android.widget.Toast.LENGTH_SHORT).show();
            } else if (car.isOnRepair()) {
                android.widget.Toast.makeText(context,
                        "Автомобиль на ремонте и временно недоступен",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Установка контент-описания для карточки
        String speakableText = car.getName() + ", " + car.getPricePerDay() + " рублей в день, "
                + car.getBodyType() + ", " + (car.isAvailable() ? "доступен" : "недоступен")
                + ", " + car.getTransmission() + ", " + car.getFuelType() + ", " + car.getSeats() + " мест";
        holder.cardView.setContentDescription(speakableText);
    }

    @Override
    public int getItemCount() {
        return cars.size();
    }

    private void updateFavoriteButton(ImageView button, int carId, boolean isFavorite) {
        if (isFavorite) {
            button.setImageResource(R.drawable.ic_favorite);
            button.setColorFilter(ContextCompat.getColor(context, R.color.error_red));
        } else {
            button.setImageResource(R.drawable.ic_favorite_border);
            button.setColorFilter(ContextCompat.getColor(context, R.color.primary));
        }
    }

    public void updateCars(List<Car> newCars) {
        this.cars = newCars;
        notifyDataSetChanged();
    }

    // Класс ViewHolder
    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView carImage;
        TextView statusBadge;
        TextView carTitle;
        ImageView favoriteButton;
        TextView carDetails;
        TextView carPrice;
        MaterialButton bookButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            carImage = itemView.findViewById(R.id.carImage);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            carTitle = itemView.findViewById(R.id.carTitle);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            carDetails = itemView.findViewById(R.id.carDetails);
            carPrice = itemView.findViewById(R.id.carPrice);
            bookButton = itemView.findViewById(R.id.bookButton);
        }
    }
}