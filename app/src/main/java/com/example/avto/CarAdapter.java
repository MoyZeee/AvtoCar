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
    private OnFavoriteClickListener onFavoriteClickListener;
    private Set<Integer> favorites = new HashSet<>();

    public interface OnBookClickListener {
        void onBookClick(Car car);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(int carId, boolean isFavorite);
    }

    public CarAdapter(Context context, List<Car> cars, OnBookClickListener bookListener, OnFavoriteClickListener favoriteListener) {
        this.context = context;
        this.cars = cars;
        this.onBookClickListener = bookListener;
        this.onFavoriteClickListener = favoriteListener;
    }

    public void setFavorites(Set<Integer> favoritesSet) {
        this.favorites.clear();
        if (favoritesSet != null) {
            this.favorites.addAll(favoritesSet);
        }
        notifyDataSetChanged();
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

        holder.carImage.setImageResource(car.getImageResId());
        holder.carTitle.setText(car.getName());
        holder.carDetails.setText(car.getDescription());
        holder.carPrice.setText(car.getFormattedPrice());

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

        holder.cardView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        holder.carTitle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        holder.carDetails.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        holder.carPrice.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        holder.statusBadge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        boolean isFavorite = favorites.contains(car.getId());
        updateFavoriteButton(holder.favoriteButton, car.getId(), isFavorite);
        holder.favoriteButton.setContentDescription(isFavorite ? "Убрать из избранного" : "Добавить в избранное");

        holder.favoriteButton.setOnClickListener(v -> {
            boolean wasFavorite = favorites.contains(car.getId());
            boolean newFavoriteState = !wasFavorite;

            if (newFavoriteState) {
                favorites.add(car.getId());
            } else {
                favorites.remove(car.getId());
            }
            updateFavoriteButton(holder.favoriteButton, car.getId(), newFavoriteState);
            holder.favoriteButton.setContentDescription(newFavoriteState ? "Убрать из избранного" : "Добавить в избранное");

            if (onFavoriteClickListener != null) {
                onFavoriteClickListener.onFavoriteClick(car.getId(), newFavoriteState);
            }
        });

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