package com.example.avto;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.badge.BadgeDrawable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CarAdapter.OnBookClickListener {

    private AutoCompleteTextView typeFilter;
    private AutoCompleteTextView priceFilter;
    private ImageView profileButton;
    private ImageView notificationsButton;
    private TextView notificationBadge;
    private BottomNavigationView bottomNavigation;
    private SharedPreferences sharedPreferences;
    private RecyclerView carsRecyclerView;
    private CarAdapter carAdapter;
    private TextView totalCarsText, availableCarsText, avgPriceText;
    private DatabaseHelper dbHelper;
    private List<Car> originalCarList;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("user_email", "");
        Log.d("MainActivity", "onCreate: userEmail = " + userEmail);

        // ИСПРАВЛЕНИЕ: Используем синглтон DatabaseHelper
        dbHelper = DatabaseHelper.getInstance(this);

        // Находим все View
        typeFilter = findViewById(R.id.typeFilter);
        priceFilter = findViewById(R.id.priceFilter);
        profileButton = findViewById(R.id.profileButton);
        notificationsButton = findViewById(R.id.notificationsButton);
        notificationBadge = findViewById(R.id.notificationBadge);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        totalCarsText = findViewById(R.id.totalCarsText);
        availableCarsText = findViewById(R.id.availableCarsText);
        avgPriceText = findViewById(R.id.avgPriceText);

        carsRecyclerView = findViewById(R.id.carsRecyclerView);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        carsRecyclerView.setLayoutManager(layoutManager);

        carsRecyclerView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        carsRecyclerView.setContentDescription(getString(R.string.cars_list_desc));

        // Создаем список машин с актуальными статусами из БД
        originalCarList = createCarList();

        carAdapter = new CarAdapter(this, originalCarList, this);
        carsRecyclerView.setAdapter(carAdapter);

        updateStatistics(originalCarList);

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        notificationsButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Экран уведомлений временно недоступен", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Error opening NotificationsActivity: " + e.getMessage());
            }
        });

        setupBottomNavigation();
        setupTypeFilter();
        setupPriceFilter();

        typeFilter.setLabelFor(R.id.typeFilter);
        priceFilter.setLabelFor(R.id.priceFilter);

        addTestNotificationIfNeeded(userEmail);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
        // ОБНОВЛЯЕМ СПИСОК МАШИН ПРИ ВОЗВРАЩЕНИИ НА ГЛАВНЫЙ ЭКРАН
        refreshCarList();
    }

    private void refreshCarList() {
        List<Car> updatedCars = createCarList();
        carAdapter.updateCars(updatedCars);
        updateStatistics(updatedCars);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                carsRecyclerView.smoothScrollToPosition(0);
                return true;
            } else if (itemId == R.id.nav_bookings) {
                Intent intent = new Intent(this, BookingsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_notifications) {
                try {
                    Intent intent = new Intent(this, NotificationsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Экран уведомлений временно недоступен", Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "Error opening NotificationsActivity: " + e.getMessage());
                }
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bottomNavigation.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                carsRecyclerView.smoothScrollToPosition(0);
            } else if (item.getItemId() == R.id.nav_bookings) {
                typeFilter.setText(getString(R.string.car_type_all), false);
                priceFilter.setText(getString(R.string.price_any), false);
                refreshCarList();
            }
        });

        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        String userEmail = sharedPreferences.getString("user_email", "");
        if (userEmail.isEmpty()) {
            return;
        }

        int unreadCount = dbHelper.getUnreadNotificationsCount(userEmail);

        if (unreadCount > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
            notificationBadge.setText(String.valueOf(unreadCount));
        } else {
            notificationBadge.setVisibility(View.GONE);
        }

        BadgeDrawable badge = bottomNavigation.getOrCreateBadge(R.id.nav_notifications);
        if (unreadCount > 0) {
            badge.setNumber(unreadCount);
            badge.setVisible(true);
        } else {
            badge.setVisible(false);
        }
    }

    private void addTestNotificationIfNeeded(String userEmail) {
        if (userEmail.isEmpty()) {
            return;
        }

        Cursor cursor = dbHelper.getUserNotifications(userEmail);
        boolean hasNotifications = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }

        if (!hasNotifications) {
            dbHelper.addNotification(userEmail,
                    "Добро пожаловать!",
                    "Добро пожаловать в приложение аренды автомобилей. Здесь вы будете получать уведомления о ваших бронированиях.",
                    "system"
            );
            updateNotificationBadge();
        }
    }

    private void setupTypeFilter() {
        String[] carTypes = {getString(R.string.car_type_all), "Седан", "Внедорожник", "Хэтчбек", "Купе"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                carTypes
        );
        typeFilter.setAdapter(typeAdapter);
        typeFilter.setContentDescription(getString(R.string.car_type_filter_desc));

        typeFilter.setOnItemClickListener((parent, view, position, id) -> {
            String selectedType = parent.getItemAtPosition(position).toString();
            applyTypeFilter(selectedType);
        });

        typeFilter.setText(getString(R.string.car_type_all), false);
    }

    private void setupPriceFilter() {
        String[] priceRanges = {
                getString(R.string.price_any),
                "До 3000₽",
                "3000-5000₽",
                "5000-7000₽",
                "7000-10000₽",
                "Самый дешёвый",
                "Самый дорогой"
        };

        ArrayAdapter<String> priceAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                priceRanges
        );
        priceFilter.setAdapter(priceAdapter);
        priceFilter.setContentDescription(getString(R.string.price_filter_desc));

        priceFilter.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPrice = parent.getItemAtPosition(position).toString();
            applyPriceFilter(selectedPrice);
        });

        priceFilter.setText(getString(R.string.price_any), false);
    }

    private void applyTypeFilter(String type) {
        List<Car> allCars = createCarList();
        List<Car> filteredCars = new ArrayList<>();

        if (type.equals(getString(R.string.car_type_all))) {
            filteredCars = allCars;
        } else {
            for (Car car : allCars) {
                if (car.getBodyType().equals(type)) {
                    filteredCars.add(car);
                }
            }
        }

        carAdapter.updateCars(filteredCars);
        updateStatistics(filteredCars);
    }

    private void applyPriceFilter(String priceRange) {
        List<Car> allCars = createCarList();
        List<Car> filteredCars = new ArrayList<>();

        switch (priceRange) {
            case "Любая цена":
                filteredCars = allCars;
                break;

            case "До 3000₽":
                for (Car car : allCars) {
                    if (car.getPricePerDay() <= 3000) {
                        filteredCars.add(car);
                    }
                }
                break;

            case "3000-5000₽":
                for (Car car : allCars) {
                    if (car.getPricePerDay() > 3000 && car.getPricePerDay() <= 5000) {
                        filteredCars.add(car);
                    }
                }
                break;

            case "5000-7000₽":
                for (Car car : allCars) {
                    if (car.getPricePerDay() > 5000 && car.getPricePerDay() <= 7000) {
                        filteredCars.add(car);
                    }
                }
                break;

            case "7000-10000₽":
                for (Car car : allCars) {
                    if (car.getPricePerDay() > 7000 && car.getPricePerDay() <= 10000) {
                        filteredCars.add(car);
                    }
                }
                break;

            case "Самый дешёвый":
                Car cheapest = findCheapestCar(allCars);
                if (cheapest != null) {
                    filteredCars.add(cheapest);
                }
                break;

            case "Самый дорогой":
                Car mostExpensive = findMostExpensiveCar(allCars);
                if (mostExpensive != null) {
                    filteredCars.add(mostExpensive);
                }
                break;
        }

        carAdapter.updateCars(filteredCars);
        updateStatistics(filteredCars);
    }

    private Car findCheapestCar(List<Car> cars) {
        if (cars.isEmpty()) return null;

        Car cheapest = cars.get(0);
        for (Car car : cars) {
            if (car.getPricePerDay() < cheapest.getPricePerDay()) {
                cheapest = car;
            }
        }
        return cheapest;
    }

    private Car findMostExpensiveCar(List<Car> cars) {
        if (cars.isEmpty()) return null;

        Car mostExpensive = cars.get(0);
        for (Car car : cars) {
            if (car.getPricePerDay() > mostExpensive.getPricePerDay()) {
                mostExpensive = car;
            }
        }
        return mostExpensive;
    }

    private List<Car> createCarList() {
        List<Car> cars = new ArrayList<>();

        // Получаем актуальные статусы из БД (базовая доступность - не на ремонте)
        Map<Integer, Boolean> availabilityMap = new HashMap<>();
        try {
            // ИСПРАВЛЕНИЕ: Используем новый метод для базовой доступности
            availabilityMap = dbHelper.getAllCarsBaseAvailability();
            Log.d("MainActivity", "Got base availability map with " + availabilityMap.size() + " entries");

            // Логируем статусы для отладки
            for (Map.Entry<Integer, Boolean> entry : availabilityMap.entrySet()) {
                Log.d("MainActivity", "Car " + entry.getKey() + " base availability: " + entry.getValue());
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error getting availability map: " + e.getMessage());
        }

        // Базовая информация о машинах (постоянная)
        cars.add(new Car(1, "BMW X7", 7800, "Внедорожник", true, "Автомат", "Бензин", 5, R.drawable.car3));
        cars.add(new Car(2, "Porsche 911 Turbo S", 6500, "Купе", true, "Автомат", "Бензин", 2, R.drawable.car2));
        cars.add(new Car(3, "Toyota Land Cruiser", 4000, "Внедорожник", true, "Автомат", "Дизель", 7, R.drawable.car0));
        cars.add(new Car(4, "Audi A6", 5200, "Седан", true, "Автомат", "Бензин", 5, R.drawable.car4));
        cars.add(new Car(5, "Mercedes-Benz C-Class", 5800, "Седан", false, "Автомат", "Бензин", 5, R.drawable.car5));
        cars.add(new Car(6, "Hyundai Solaris", 2800, "Седан", true, "Механика", "Бензин", 5, R.drawable.car6));
        cars.add(new Car(7, "Kia Rio", 2500, "Хэтчбек", true, "Автомат", "Бензин", 5, R.drawable.car7));
        cars.add(new Car(8, "Skoda Octavia", 3200, "Хэтчбек", true, "Автомат", "Бензин", 5, R.drawable.car8));

        // Обновляем базовые статусы из БД (только статус "на ремонте")
        for (Car car : cars) {
            Boolean baseStatus = availabilityMap.get(car.getId());
            if (baseStatus != null) {
                // Только если машина на ремонте, помечаем как недоступную
                if (!baseStatus) {
                    car.setAvailable(false);
                    Log.d("MainActivity", "Car " + car.getId() + " " + car.getName() + " - On repair, not available");
                }
                // Если baseStatus = true (доступна), оставляем текущий статус
            } else {
                Log.d("MainActivity", "No base status in DB for car " + car.getId() + ", using default");
            }
        }

        return cars;
    }

    private void updateStatistics(List<Car> cars) {
        int total = cars.size();
        int available = 0;
        int totalPrice = 0;
        int availableCount = 0;

        for (Car car : cars) {
            if (car.isAvailable()) {
                available++;
                totalPrice += car.getPricePerDay();
                availableCount++;
            }
        }

        totalCarsText.setText(String.valueOf(total));
        availableCarsText.setText(String.valueOf(available));

        totalCarsText.setContentDescription(getString(R.string.total_cars_label) + ": " + total);
        availableCarsText.setContentDescription(getString(R.string.available_cars_label) + ": " + available);

        if (availableCount > 0) {
            int avgPrice = totalPrice / availableCount;
            avgPriceText.setText(String.format(Locale.getDefault(), "%d₽", avgPrice));
            avgPriceText.setContentDescription(getString(R.string.avg_price_label) + ": " + avgPrice + " рублей");
        } else {
            avgPriceText.setText("0₽");
            avgPriceText.setContentDescription(getString(R.string.avg_price_label) + ": 0 рублей");
        }

        Log.d("MainActivity", "Statistics updated - Total: " + total + ", Available: " + available);
    }

    @Override
    public void onBookClick(Car car) {
        Log.d("MainActivity", "onBookClick: Нажата кнопка бронирования для " + car.getName());

        // Проверяем базовую доступность (не на ремонте)
        if (!car.isAvailable()) {
            Toast.makeText(this, "Этот автомобиль в данный момент недоступен для бронирования", Toast.LENGTH_LONG).show();
            return;
        }

        // ИСПРАВЛЕНИЕ: Уже используем синглтон, не создаем новый экземпляр
        String userEmail = sharedPreferences.getString("user_email", "");

        if (!isProfileFilledInDatabase(userEmail)) {
            Log.d("MainActivity", "onBookClick: Профиль не заполнен");
            Toast.makeText(this, "Пожалуйста, заполните свой профиль (паспортные данные и водительское удостоверение) перед бронированием автомобиля", Toast.LENGTH_LONG).show();
            Intent intentToProfile = new Intent(this, ProfileActivity.class);
            startActivity(intentToProfile);
            return;
        }

        Log.d("MainActivity", "onBookClick: Профиль заполнен, открываем экран бронирования");

        // Добавляем уведомление о начале бронирования
        dbHelper.addNotification(userEmail,
                "Начато бронирование",
                "Вы начали бронирование автомобиля " + car.getName(),
                "booking"
        );

        updateNotificationBadge();

        Intent bookingIntent = new Intent(this, BookingActivity.class);
        bookingIntent.putExtra("car", car);
        startActivity(bookingIntent);
    }

    private boolean isProfileFilledInDatabase(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            Log.d("MainActivity", "isProfileFilledInDatabase: userEmail пуст");
            return false;
        }
        Cursor cursor = dbHelper.getUserProfile(userEmail);
        if (cursor != null) {
            boolean hasData = cursor.moveToFirst();
            Log.d("MainActivity", "isProfileFilledInDatabase: hasData = " + hasData);
            if (hasData) {
                int passportSeriesIndex = cursor.getColumnIndex("passport_series");
                int passportNumberIndex = cursor.getColumnIndex("passport_number");
                int licenseNumberIndex = cursor.getColumnIndex("license_number");

                String passportSeries = (passportSeriesIndex != -1) ? cursor.getString(passportSeriesIndex) : null;
                String passportNumber = (passportNumberIndex != -1) ? cursor.getString(passportNumberIndex) : null;
                String licenseNumber = (licenseNumberIndex != -1) ? cursor.getString(licenseNumberIndex) : null;

                boolean filled = passportSeries != null && !passportSeries.isEmpty() &&
                        passportNumber != null && !passportNumber.isEmpty() &&
                        licenseNumber != null && !licenseNumber.isEmpty();
                Log.d("MainActivity", "isProfileFilledInDatabase: filled = " + filled +
                        ", passportSeries = " + passportSeries +
                        ", passportNumber = " + passportNumber +
                        ", licenseNumber = " + licenseNumber);
                cursor.close();
                return filled;
            } else {
                cursor.close();
                return false;
            }
        } else {
            Log.d("MainActivity", "isProfileFilledInDatabase: cursor is null");
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ИСПРАВЛЕНИЕ: Не закрываем базу данных вручную, так как используем синглтон
        // База данных будет закрыта автоматически
    }

    // Геттер для оригинального списка машин (если нужно)
    public List<Car> getOriginalCarList() {
        return originalCarList;
    }
}