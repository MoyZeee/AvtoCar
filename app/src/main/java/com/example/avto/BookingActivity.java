package com.example.avto;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingActivity extends AppCompatActivity {

    private static final String TAG = "BookingActivity";
    private static final String PREFS_NAME = "booking_prefs";
    private static final int REQUEST_ADD_PAYMENT = 1;

    private Car car;
    private String userEmail;
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;

    // UI элементы
    private ImageView carImageView;
    private TextView carNameTextView, carDetailsTextView, carPriceTextView;
    private TextView startDateTextView, endDateTextView;
    private TextView totalDaysTextView, totalPriceTextView;
    private TextView pickupLocationTextView, locationDetailsTextView;
    private TextView cardNumberTextView, deliveryPriceTextView, deliveryFeeTextView;
    private TextView cryptoInfoTextView, cryptoRateTextView;
    private EditText deliveryAddressEditText;
    private Button confirmBookingButton, addCardButton, connectWalletButton;
    private ImageButton changeLocationButton;
    private CardView selfPickupCardView, deliveryCardView;
    private LinearLayout locationsContainer, cryptoRateContainer;
    private LinearLayout selfPickupOption, deliveryOptionLayout;
    private LinearLayout cardPaymentOption, bitcoinPaymentOption, ethereumPaymentOption, usdtPaymentOption;

    private CheckBox selfPickupCheckBox, deliveryCheckBox;
    private CheckBox creditCardCheckBox, bitcoinCheckBox, ethereumCheckBox, usdtCheckBox;

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private String selectedLocation = "Симферополь, ул. Тверская, 10";
    private String locationDetails = "Парковка у бизнес-центра, этаж B2";
    private String selectedPaymentMethod = "credit_card";
    private String selectedDeliveryType = "self_pickup";
    private String savedCardNumber = "";
    private String deliveryAddress = "";

    private int carPricePerDayValue = 0;
    private int deliveryFee = 1500;
    private int totalDays = 1;

    // Курсы криптовалют
    private double btcRate = 2500000.0;
    private double ethRate = 150000.0;
    private double usdtRate = 100.0;

    // Список доступных пунктов выдачи
    private List<PickupLocation> pickupLocations = new ArrayList<>();

    // Для работы с потоками
    private ExecutorService executorService;
    private Handler mainHandler;
    private android.app.ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        initPickupLocations();
        loadData();
        setupListeners();

        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        } else {
            loadUserPaymentMethods();
        }

        updateDeliveryViews();
    }

    private void initViews() {
        carImageView = findViewById(R.id.carImageView);
        carNameTextView = findViewById(R.id.carNameTextView);
        carDetailsTextView = findViewById(R.id.carDetailsTextView);
        carPriceTextView = findViewById(R.id.carPriceTextView);
        startDateTextView = findViewById(R.id.startDateTextView);
        endDateTextView = findViewById(R.id.endDateTextView);
        totalDaysTextView = findViewById(R.id.totalDaysTextView);
        totalPriceTextView = findViewById(R.id.totalPriceTextView);
        pickupLocationTextView = findViewById(R.id.pickupLocationTextView);
        locationDetailsTextView = findViewById(R.id.locationDetailsTextView);
        cardNumberTextView = findViewById(R.id.cardNumberTextView);
        deliveryPriceTextView = findViewById(R.id.deliveryPriceTextView);
        deliveryFeeTextView = findViewById(R.id.deliveryFeeTextView);
        cryptoInfoTextView = findViewById(R.id.cryptoInfoTextView);
        cryptoRateTextView = findViewById(R.id.cryptoRateTextView);
        deliveryAddressEditText = findViewById(R.id.deliveryAddressEditText);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);
        addCardButton = findViewById(R.id.addCardButton);
        connectWalletButton = findViewById(R.id.connectWalletButton);
        changeLocationButton = findViewById(R.id.changeLocationButton);
        selfPickupCardView = findViewById(R.id.selfPickupCardView);
        deliveryCardView = findViewById(R.id.deliveryCardView);
        locationsContainer = findViewById(R.id.locationsContainer);
        cryptoRateContainer = findViewById(R.id.cryptoRateContainer);

        // CheckBox для способа получения
        selfPickupCheckBox = findViewById(R.id.selfPickupCheckBox);
        deliveryCheckBox = findViewById(R.id.deliveryCheckBox);

        // CheckBox для способа оплаты
        creditCardCheckBox = findViewById(R.id.creditCardCheckBox);
        bitcoinCheckBox = findViewById(R.id.bitcoinCheckBox);
        ethereumCheckBox = findViewById(R.id.ethereumCheckBox);
        usdtCheckBox = findViewById(R.id.usdtCheckBox);

        // LinearLayout для кликов
        selfPickupOption = findViewById(R.id.selfPickupOption);
        deliveryOptionLayout = findViewById(R.id.deliveryOptionLayout);
        cardPaymentOption = findViewById(R.id.cardPaymentOption);
        bitcoinPaymentOption = findViewById(R.id.bitcoinPaymentOption);
        ethereumPaymentOption = findViewById(R.id.ethereumPaymentOption);
        usdtPaymentOption = findViewById(R.id.usdtPaymentOption);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        dbHelper = DatabaseHelper.getInstance(this);

        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);

        if (deliveryPriceTextView != null) {
            deliveryPriceTextView.setText(String.format(Locale.getDefault(), "+%,d ₽", deliveryFee));
        }
        if (deliveryFeeTextView != null) {
            deliveryFeeTextView.setText(String.format(Locale.getDefault(), "%,d ₽", deliveryFee));
        }

        progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Пожалуйста, подождите...");
        progressDialog.setCancelable(false);
    }

    private void initPickupLocations() {
        pickupLocations.clear();
        pickupLocations.add(new PickupLocation("Симферополь, ул. Тверская, 10", "Парковка у бизнес-центра, этаж B2", "09:00 - 22:00", 5.0, true));
        pickupLocations.add(new PickupLocation("Симферополь, аэропорт", "Терминал D, парковка №3, сектор A", "Круглосуточно", 25.0, true));
        pickupLocations.add(new PickupLocation("Симферополь, Киевский вокзал", "Наземная парковка, секция A, места 45-60", "06:00 - 00:00", 3.5, true));
        pickupLocations.add(new PickupLocation("Симферополь, ТЦ Авиапарк", "Подземный паркинг, уровень -2, секция 3", "10:00 - 23:00", 8.0, true));
        pickupLocations.add(new PickupLocation("Симферополь, ул. Арбат, 15", "Исторический центр, охраняемая парковка", "08:00 - 23:00", 4.0, true));
    }

    private void loadData() {
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

        userEmail = sharedPreferences.getString("user_email", "");
        if (userEmail == null || userEmail.isEmpty()) {
            SharedPreferences userPrefs = getSharedPreferences("user_profile", MODE_PRIVATE);
            userEmail = userPrefs.getString("user_email", "");
        }

        setCarImage(car.getId(), car.getName());

        if (carNameTextView != null) carNameTextView.setText(car.getName());
        if (carDetailsTextView != null) carDetailsTextView.setText(car.getDescription());
        if (carPriceTextView != null) carPriceTextView.setText(car.getFormattedPrice() + "/день");

        carPricePerDayValue = car.getPricePerDay();

        updateDateTextViews();
        calculateTotal();
        updateLocationViews();
        populateOtherLocations();
    }

    private void setCarImage(int carId, String carName) {
        int imageResId = getCarImageResource(carId, carName);
        if (imageResId != 0 && carImageView != null) {
            carImageView.setImageResource(imageResId);
        }
    }

    private int getCarImageResource(int carId, String carName) {
        switch (carId) {
            case 1: return R.drawable.car3;
            case 2: return R.drawable.car2;
            case 3: return R.drawable.car0;
            case 4: return R.drawable.car4;
            case 5: return R.drawable.car5;
            case 6: return R.drawable.car6;
            case 7: return R.drawable.car7;
            case 8: return R.drawable.car8;
            default: return R.drawable.car_placeholder;
        }
    }

    private void setupListeners() {
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        LinearLayout startDateLayout = findViewById(R.id.startDateLayout);
        LinearLayout endDateLayout = findViewById(R.id.endDateLayout);

        if (startDateLayout != null) startDateLayout.setOnClickListener(v -> showDatePicker(true));
        if (endDateLayout != null) endDateLayout.setOnClickListener(v -> showDatePicker(false));

        if (changeLocationButton != null) changeLocationButton.setOnClickListener(v -> showLocationPicker());
        if (addCardButton != null) addCardButton.setOnClickListener(v -> addPaymentMethod());
        if (connectWalletButton != null) connectWalletButton.setOnClickListener(v -> connectWallet());

        // ===== ОБРАБОТКА СПОСОБА ПОЛУЧЕНИЯ С CHECKBOX =====
        if (selfPickupCheckBox != null) {
            selfPickupCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (deliveryCheckBox != null && deliveryCheckBox.isChecked()) {
                        deliveryCheckBox.setChecked(false);
                    }
                    selectedDeliveryType = "self_pickup";
                    updateDeliveryViews();
                    calculateTotal();
                    Toast.makeText(BookingActivity.this, "Выбран самовывоз", Toast.LENGTH_SHORT).show();
                } else {
                    if (deliveryCheckBox != null && !deliveryCheckBox.isChecked()) {
                        deliveryCheckBox.setChecked(true);
                    }
                }
            });
        }

        if (deliveryCheckBox != null) {
            deliveryCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (selfPickupCheckBox != null && selfPickupCheckBox.isChecked()) {
                        selfPickupCheckBox.setChecked(false);
                    }
                    selectedDeliveryType = "delivery";
                    updateDeliveryViews();
                    calculateTotal();
                    Toast.makeText(BookingActivity.this, "Выбрана доставка", Toast.LENGTH_SHORT).show();
                } else {
                    if (selfPickupCheckBox != null && !selfPickupCheckBox.isChecked()) {
                        selfPickupCheckBox.setChecked(true);
                    }
                }
            });
        }

        // Обработка кликов по всей строке для способа получения
        if (selfPickupOption != null) {
            selfPickupOption.setOnClickListener(v -> {
                if (selfPickupCheckBox != null) {
                    selfPickupCheckBox.setChecked(true);
                }
            });
        }

        if (deliveryOptionLayout != null) {
            deliveryOptionLayout.setOnClickListener(v -> {
                if (deliveryCheckBox != null) {
                    deliveryCheckBox.setChecked(true);
                }
            });
        }

        // ===== ОБРАБОТКА СПОСОБА ОПЛАТЫ С CHECKBOX =====
        if (creditCardCheckBox != null) {
            creditCardCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (bitcoinCheckBox != null) bitcoinCheckBox.setChecked(false);
                    if (ethereumCheckBox != null) ethereumCheckBox.setChecked(false);
                    if (usdtCheckBox != null) usdtCheckBox.setChecked(false);
                    selectedPaymentMethod = "credit_card";
                    hideCryptoInfo();
                    Log.d(TAG, "Выбран способ оплаты: Банковская карта");
                    calculateTotal();
                } else if (!isChecked && !isAnyPaymentChecked()) {
                    creditCardCheckBox.setChecked(true);
                }
            });
        }

        if (bitcoinCheckBox != null) {
            bitcoinCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (creditCardCheckBox != null) creditCardCheckBox.setChecked(false);
                    if (ethereumCheckBox != null) ethereumCheckBox.setChecked(false);
                    if (usdtCheckBox != null) usdtCheckBox.setChecked(false);
                    selectedPaymentMethod = "bitcoin";
                    showCryptoInfo("BTC", btcRate);
                    Log.d(TAG, "Выбран способ оплаты: Bitcoin");
                    calculateTotal();
                } else if (!isChecked && !isAnyPaymentChecked()) {
                    bitcoinCheckBox.setChecked(true);
                }
            });
        }

        if (ethereumCheckBox != null) {
            ethereumCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (creditCardCheckBox != null) creditCardCheckBox.setChecked(false);
                    if (bitcoinCheckBox != null) bitcoinCheckBox.setChecked(false);
                    if (usdtCheckBox != null) usdtCheckBox.setChecked(false);
                    selectedPaymentMethod = "ethereum";
                    showCryptoInfo("ETH", ethRate);
                    Log.d(TAG, "Выбран способ оплаты: Ethereum");
                    calculateTotal();
                } else if (!isChecked && !isAnyPaymentChecked()) {
                    ethereumCheckBox.setChecked(true);
                }
            });
        }

        if (usdtCheckBox != null) {
            usdtCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (creditCardCheckBox != null) creditCardCheckBox.setChecked(false);
                    if (bitcoinCheckBox != null) bitcoinCheckBox.setChecked(false);
                    if (ethereumCheckBox != null) ethereumCheckBox.setChecked(false);
                    selectedPaymentMethod = "usdt";
                    showCryptoInfo("USDT", usdtRate);
                    Log.d(TAG, "Выбран способ оплаты: USDT");
                    calculateTotal();
                } else if (!isChecked && !isAnyPaymentChecked()) {
                    usdtCheckBox.setChecked(true);
                }
            });
        }

        // Обработка кликов по всей строке для оплаты
        if (cardPaymentOption != null) {
            cardPaymentOption.setOnClickListener(v -> {
                if (creditCardCheckBox != null) creditCardCheckBox.setChecked(true);
            });
        }

        if (bitcoinPaymentOption != null) {
            bitcoinPaymentOption.setOnClickListener(v -> {
                if (bitcoinCheckBox != null) bitcoinCheckBox.setChecked(true);
            });
        }

        if (ethereumPaymentOption != null) {
            ethereumPaymentOption.setOnClickListener(v -> {
                if (ethereumCheckBox != null) ethereumCheckBox.setChecked(true);
            });
        }

        if (usdtPaymentOption != null) {
            usdtPaymentOption.setOnClickListener(v -> {
                if (usdtCheckBox != null) usdtCheckBox.setChecked(true);
            });
        }

        if (deliveryAddressEditText != null) {
            deliveryAddressEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    deliveryAddress = s.toString();
                    saveDeliveryAddress();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (confirmBookingButton != null) confirmBookingButton.setOnClickListener(v -> confirmBooking());
    }

    private boolean isAnyPaymentChecked() {
        return (creditCardCheckBox != null && creditCardCheckBox.isChecked()) ||
                (bitcoinCheckBox != null && bitcoinCheckBox.isChecked()) ||
                (ethereumCheckBox != null && ethereumCheckBox.isChecked()) ||
                (usdtCheckBox != null && usdtCheckBox.isChecked());
    }

    private void showCryptoInfo(String cryptoCode, double rate) {
        if (cryptoInfoTextView != null) cryptoInfoTextView.setVisibility(View.VISIBLE);
        if (cryptoRateContainer != null) cryptoRateContainer.setVisibility(View.VISIBLE);

        int totalPrice = calculateTotalPrice();
        double cryptoAmount = totalPrice / rate;

        String formattedAmount;
        if (cryptoCode.equals("BTC")) {
            formattedAmount = String.format(Locale.US, "%.6f", cryptoAmount);
        } else if (cryptoCode.equals("ETH")) {
            formattedAmount = String.format(Locale.US, "%.4f", cryptoAmount);
        } else {
            formattedAmount = String.format(Locale.US, "%.2f", cryptoAmount);
        }

        if (cryptoRateTextView != null) {
            cryptoRateTextView.setText(String.format(Locale.getDefault(), "1 %s ≈ %,.0f ₽\nСумма: %s %s",
                    cryptoCode, rate, formattedAmount, cryptoCode));
        }
    }

    private void hideCryptoInfo() {
        if (cryptoInfoTextView != null) cryptoInfoTextView.setVisibility(View.GONE);
        if (cryptoRateContainer != null) cryptoRateContainer.setVisibility(View.GONE);
    }

    private void updateDeliveryViews() {
        Log.d(TAG, "updateDeliveryViews: " + selectedDeliveryType);

        if (selectedDeliveryType.equals("self_pickup")) {
            if (selfPickupCardView != null) {
                selfPickupCardView.setVisibility(View.VISIBLE);
            }
            if (deliveryCardView != null) {
                deliveryCardView.setVisibility(View.GONE);
            }
            if (selfPickupCheckBox != null && !selfPickupCheckBox.isChecked()) {
                selfPickupCheckBox.setChecked(true);
            }
        } else {
            if (selfPickupCardView != null) {
                selfPickupCardView.setVisibility(View.GONE);
            }
            if (deliveryCardView != null) {
                deliveryCardView.setVisibility(View.VISIBLE);
            }
            if (deliveryCheckBox != null && !deliveryCheckBox.isChecked()) {
                deliveryCheckBox.setChecked(true);
            }

            String savedAddress = getSavedDeliveryAddress();
            if (!savedAddress.isEmpty() && deliveryAddressEditText != null &&
                    deliveryAddressEditText.getText().toString().isEmpty()) {
                deliveryAddressEditText.setText(savedAddress);
                deliveryAddress = savedAddress;
            }
        }
        saveDeliveryType();
    }

    private void saveDeliveryType() {
        sharedPreferences.edit().putString("delivery_type", selectedDeliveryType).apply();
    }

    private void saveDeliveryAddress() {
        if (deliveryAddress != null && !deliveryAddress.isEmpty()) {
            sharedPreferences.edit().putString("delivery_address", deliveryAddress).apply();
        }
    }

    private String getSavedDeliveryAddress() {
        return sharedPreferences.getString("delivery_address", "");
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDateCalendar : endDateCalendar;

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    if (isStartDate) {
                        if (selectedDate.before(Calendar.getInstance())) {
                            Toast.makeText(BookingActivity.this, "Дата начала не может быть раньше сегодняшнего дня", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startDateCalendar.set(selectedYear, selectedMonth, selectedDay);
                        if (endDateCalendar.before(startDateCalendar)) {
                            endDateCalendar.setTime(startDateCalendar.getTime());
                            endDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    } else {
                        if (selectedDate.before(startDateCalendar)) {
                            Toast.makeText(BookingActivity.this, "Дата окончания не может быть раньше даты начала", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        endDateCalendar.set(selectedYear, selectedMonth, selectedDay);
                    }
                    updateDateTextViews();
                    calculateTotal();
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        if (isStartDate) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        } else {
            datePickerDialog.getDatePicker().setMinDate(startDateCalendar.getTimeInMillis());
        }
        datePickerDialog.show();
    }

    private void showLocationPicker() {
        String[] locationArray = new String[pickupLocations.size()];
        for (int i = 0; i < pickupLocations.size(); i++) {
            PickupLocation loc = pickupLocations.get(i);
            locationArray[i] = loc.getName() + "\n" + loc.getDetails() + "\nВремя работы: " + loc.getWorkingHours();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Выберите пункт самовывоза")
                .setItems(locationArray, (dialog, which) -> {
                    PickupLocation selectedLoc = pickupLocations.get(which);
                    selectedLocation = selectedLoc.getName();
                    locationDetails = selectedLoc.getDetails();
                    updateLocationViews();
                    saveSelectedLocation();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveSelectedLocation() {
        sharedPreferences.edit()
                .putString("pickup_location", selectedLocation)
                .putString("location_details", locationDetails)
                .apply();
    }

    private void populateOtherLocations() {
        if (locationsContainer == null) return;
        locationsContainer.removeAllViews();

        for (int i = 1; i < Math.min(pickupLocations.size(), 4); i++) {
            addLocationView(pickupLocations.get(i));
        }
    }

    private void addLocationView(PickupLocation location) {
        View locationView = LayoutInflater.from(this).inflate(R.layout.item_location, locationsContainer, false);

        TextView nameTextView = locationView.findViewById(R.id.locationNameTextView);
        TextView detailsTextView = locationView.findViewById(R.id.locationDetailsTextView);
        TextView distanceTextView = locationView.findViewById(R.id.locationDistanceTextView);
        TextView hoursTextView = locationView.findViewById(R.id.locationHoursTextView);

        nameTextView.setText(location.getName());
        detailsTextView.setText(location.getDetails());
        distanceTextView.setText(String.format(Locale.getDefault(), "%.1f км", location.getDistance()));
        hoursTextView.setText(location.getWorkingHours());

        locationView.setOnClickListener(v -> {
            selectedLocation = location.getName();
            locationDetails = location.getDetails();
            updateLocationViews();
            saveSelectedLocation();
        });

        locationsContainer.addView(locationView);
    }

    private void updateLocationViews() {
        if (pickupLocationTextView != null) pickupLocationTextView.setText(selectedLocation);
        if (locationDetailsTextView != null) locationDetailsTextView.setText(locationDetails);
    }

    private void updateDateTextViews() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        if (startDateTextView != null) startDateTextView.setText(sdf.format(startDateCalendar.getTime()));
        if (endDateTextView != null) endDateTextView.setText(sdf.format(endDateCalendar.getTime()));
    }

    private int calculateTotalPrice() {
        long diff = endDateCalendar.getTimeInMillis() - startDateCalendar.getTimeInMillis();
        totalDays = (int) (diff / (1000 * 60 * 60 * 24));
        if (totalDays < 1) totalDays = 1;

        int totalPrice = totalDays * carPricePerDayValue;
        if (selectedDeliveryType.equals("delivery")) {
            totalPrice += deliveryFee;
        }
        return totalPrice;
    }

    private void calculateTotal() {
        int totalPrice = calculateTotalPrice();
        if (totalDaysTextView != null) totalDaysTextView.setText(totalDays + " " + getDayWord(totalDays));
        if (totalPriceTextView != null) totalPriceTextView.setText(String.format(Locale.getDefault(), "%,d ₽", totalPrice));
    }

    private void loadUserPaymentMethods() {
        if (userEmail == null || userEmail.isEmpty()) return;

        Cursor cursor = null;
        try {
            cursor = dbHelper.getUserPaymentMethods(userEmail);
            if (cursor != null && cursor.moveToFirst()) {
                int cardNumberIndex = cursor.getColumnIndex("card_last_four");
                if (cardNumberIndex >= 0) {
                    savedCardNumber = cursor.getString(cardNumberIndex);
                    runOnUiThread(() -> {
                        if (cardNumberTextView != null) {
                            cardNumberTextView.setText("**** " + savedCardNumber);
                        }
                    });
                }
            }
            final boolean isCardEmpty = savedCardNumber.isEmpty();
            runOnUiThread(() -> {
                if (isCardEmpty && cardNumberTextView != null) {
                    cardNumberTextView.setText("Нет карт");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading payment methods", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }
    }

    private void addPaymentMethod() {
        Intent intent = new Intent(this, AddPaymentMethodActivity.class);
        startActivityForResult(intent, REQUEST_ADD_PAYMENT);
    }

    private void connectWallet() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Подключение кошелька")
                .setMessage("Выберите способ подключения крипто-кошелька:")
                .setItems(new String[]{"MetaMask", "Trust Wallet", "Binance Chain", "Другой кошелек"}, (dialog, which) -> {
                    Toast.makeText(BookingActivity.this, "Кошелек подключен", Toast.LENGTH_SHORT).show();
                    if (bitcoinCheckBox != null) bitcoinCheckBox.setChecked(true);
                    selectedPaymentMethod = "bitcoin";
                    showCryptoInfo("BTC", btcRate);
                    calculateTotal();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PAYMENT && resultCode == RESULT_OK) {
            loadUserPaymentMethods();
            Toast.makeText(this, "Способ оплаты добавлен", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmBooking() {
        if (!isProfileFilled()) {
            showProfileWarning();
            return;
        }

        if (selectedDeliveryType.equals("delivery")) {
            String address = deliveryAddressEditText != null ? deliveryAddressEditText.getText().toString().trim() : "";
            if (address.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, укажите адрес доставки", Toast.LENGTH_SHORT).show();
                return;
            }
            deliveryAddress = address;
        }

        if (selectedPaymentMethod.equals("credit_card") && (savedCardNumber == null || savedCardNumber.isEmpty())) {
            Toast.makeText(this, "Пожалуйста, добавьте банковскую карту", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final String startDate = sdf.format(startDateCalendar.getTime());
        final String endDate = sdf.format(endDateCalendar.getTime());
        final int totalPrice = calculateTotalPrice();
        final String bookingDate = sdf.format(Calendar.getInstance().getTime());

        Log.d(TAG, "=== ДАННЫЕ ДЛЯ БРОНИРОВАНИЯ ===");
        Log.d(TAG, "Автомобиль: " + car.getName() + " (ID: " + car.getId() + ")");
        Log.d(TAG, "Даты: " + startDate + " - " + endDate);
        Log.d(TAG, "Количество дней: " + totalDays);
        Log.d(TAG, "Способ получения: " + (selectedDeliveryType.equals("self_pickup") ? "Самовывоз" : "Доставка"));
        Log.d(TAG, "Способ оплаты: " + selectedPaymentMethod);
        Log.d(TAG, "Итоговая сумма: " + totalPrice + " ₽");

        final int carId = car.getId();
        final String userEmailFinal = userEmail;
        final String carName = car.getName();
        final int pricePerDay = car.getPricePerDay();
        final int totalDaysFinal = totalDays;
        final String selectedLocationFinal = selectedLocation;
        final String locationDetailsFinal = locationDetails;
        final String selectedPaymentMethodFinal = selectedPaymentMethod;
        final String selectedDeliveryTypeFinal = selectedDeliveryType;
        final String deliveryAddressFinal = deliveryAddress;
        final int deliveryFeeFinal = selectedDeliveryType.equals("delivery") ? deliveryFee : 0;

        final Booking booking = new Booking(
                carId, userEmailFinal, carName, pricePerDay,
                startDate, endDate, totalDaysFinal, totalPrice,
                "pending_payment", bookingDate, selectedLocationFinal, locationDetailsFinal,
                selectedPaymentMethodFinal, selectedDeliveryTypeFinal, deliveryAddressFinal, deliveryFeeFinal
        );

        showLoadingDialog();

        executorService.execute(() -> {
            final boolean isBooked = dbHelper.isCarBooked(carId, startDate, endDate);

            if (isBooked) {
                mainHandler.post(() -> {
                    hideLoadingDialog();
                    Toast.makeText(BookingActivity.this, "Автомобиль уже забронирован", Toast.LENGTH_LONG).show();
                });
                return;
            }

            final long bookingId = dbHelper.addBooking(booking);

            mainHandler.post(() -> {
                hideLoadingDialog();
                if (bookingId != -1) {
                    if (selectedPaymentMethodFinal.equals("credit_card")) {
                        processCardPayment(bookingId, totalPrice);
                    } else {
                        processCryptoPayment(bookingId, totalPrice);
                    }
                } else {
                    Toast.makeText(BookingActivity.this, "Ошибка при создании бронирования", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void processCardPayment(final long bookingId, final int amount) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Оплата картой")
                .setMessage("Оплатить " + String.format(Locale.getDefault(), "%,d ₽", amount) + " с карты **** " + savedCardNumber)
                .setPositiveButton("Оплатить", (dialog, which) -> {
                    showLoadingDialog();
                    executorService.execute(() -> {
                        try {
                            Thread.sleep(2000);
                            dbHelper.updateBookingStatus(bookingId, "paid");
                            mainHandler.post(() -> {
                                hideLoadingDialog();
                                showSuccessDialog(bookingId);
                            });
                        } catch (InterruptedException e) {
                            mainHandler.post(() -> {
                                hideLoadingDialog();
                                Toast.makeText(BookingActivity.this, "Ошибка оплаты", Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    executorService.execute(() -> dbHelper.updateBookingStatus(bookingId, "cancelled"));
                    mainHandler.post(() -> Toast.makeText(BookingActivity.this, "Оплата отменена", Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    private void processCryptoPayment(final long bookingId, final int amount) {
        double cryptoAmount = 0;
        String cryptoCode = "";
        switch (selectedPaymentMethod) {
            case "bitcoin":
                cryptoAmount = amount / btcRate;
                cryptoCode = "BTC";
                break;
            case "ethereum":
                cryptoAmount = amount / ethRate;
                cryptoCode = "ETH";
                break;
            case "usdt":
                cryptoAmount = amount / usdtRate;
                cryptoCode = "USDT";
                break;
        }

        final String finalCryptoCode = cryptoCode;
        final String formattedAmount = String.format(Locale.US,
                cryptoCode.equals("BTC") ? "%.6f" : cryptoCode.equals("ETH") ? "%.4f" : "%.2f", cryptoAmount);
        final String cryptoAddress = getCryptoAddress(cryptoCode);
        final String network = getNetworkForCrypto(cryptoCode);

        // Создаем строку для QR-кода
        String qrData = "";
        switch (cryptoCode) {
            case "BTC":
                qrData = "bitcoin:" + cryptoAddress + "?amount=" + formattedAmount;
                break;
            case "ETH":
                qrData = "ethereum:" + cryptoAddress + "?value=" + formattedAmount;
                break;
            case "USDT":
                qrData = cryptoAddress;
                break;
        }

        final String finalQrData = qrData;

        // Показываем диалог с QR-кодом
        showCryptoPaymentDialog(bookingId, formattedAmount, finalCryptoCode, cryptoAddress, network, finalQrData, amount);
    }

    private void showCryptoPaymentDialog(final long bookingId, final String amount,
                                         final String cryptoCode, final String address,
                                         final String network, final String qrData, final int fiatAmount) {

        // Создаем диалог
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crypto_payment, null);

        TextView cryptoAmountTextView = dialogView.findViewById(R.id.cryptoAmountTextView);
        TextView cryptoFiatTextView = dialogView.findViewById(R.id.cryptoFiatTextView);
        TextView cryptoAddressTextView = dialogView.findViewById(R.id.cryptoAddressTextView);
        ImageView qrCodeImageView = dialogView.findViewById(R.id.qrCodeImageView);
        ImageButton copyAddressButton = dialogView.findViewById(R.id.copyAddressButton);
        Button cancelPaymentButton = dialogView.findViewById(R.id.cancelPaymentButton);
        Button iHavePaidButton = dialogView.findViewById(R.id.iHavePaidButton);

        // Устанавливаем значения
        cryptoAmountTextView.setText(String.format(Locale.getDefault(), "%s %s", amount, cryptoCode));
        cryptoFiatTextView.setText(String.format(Locale.getDefault(), "≈ %,.0f ₽", (double) fiatAmount));
        cryptoAddressTextView.setText(address);

        // Показываем загрузку для QR-кода
        qrCodeImageView.setImageResource(R.drawable.ic_qr_code);

        // Генерируем QR-код в фоновом потоке
        executorService.execute(() -> {
            try {
                Bitmap qrBitmap = generateQRCode(qrData, 400, 400);

                mainHandler.post(() -> {
                    if (qrBitmap != null) {
                        qrCodeImageView.setImageBitmap(qrBitmap);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error generating QR code", e);
            }
        });

        // Копирование адреса
        copyAddressButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String copyText = cryptoCode + ":" + amount + "\n" + address;
            clipboard.setPrimaryClip(ClipData.newPlainText("Crypto Payment", copyText));
            Toast.makeText(BookingActivity.this, "Данные для оплаты скопированы", Toast.LENGTH_SHORT).show();
        });

        // Создаем диалог
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();

        // Обработка кнопки "Я оплатил"
        iHavePaidButton.setOnClickListener(v -> {
            dialog.dismiss();
            showPaymentConfirmationDialog(bookingId);
        });

        // Обработка кнопки "Отмена"
        cancelPaymentButton.setOnClickListener(v -> {
            dialog.dismiss();
            new MaterialAlertDialogBuilder(BookingActivity.this)
                    .setTitle("Отмена бронирования")
                    .setMessage("Вы уверены, что хотите отменить бронирование?")
                    .setPositiveButton("Да, отменить", (dialogInterface, which) -> {
                        executorService.execute(() -> dbHelper.updateBookingStatus(bookingId, "cancelled"));
                        mainHandler.post(() -> {
                            Toast.makeText(BookingActivity.this, "Бронирование отменено", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    })
                    .setNegativeButton("Нет, остаться", null)
                    .show();
        });

        dialog.show();
    }

    private void showPaymentConfirmationDialog(final long bookingId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Подтверждение оплаты")
                .setMessage("Вы подтверждаете, что отправили криптовалюту?\n\nСтатус платежа будет проверен и бронирование подтвердится автоматически.")
                .setPositiveButton("Да, подтверждаю", (dialog, which) -> {
                    showLoadingDialog();
                    executorService.execute(() -> {
                        try {
                            // Симулируем проверку платежа
                            Thread.sleep(3000);
                            dbHelper.updateBookingStatus(bookingId, "paid");

                            mainHandler.post(() -> {
                                hideLoadingDialog();
                                showSuccessDialog(bookingId);
                            });
                        } catch (InterruptedException e) {
                            mainHandler.post(() -> {
                                hideLoadingDialog();
                                Toast.makeText(BookingActivity.this, "Ошибка проверки платежа", Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .setCancelable(false)
                .show();
    }

    private Bitmap generateQRCode(String data, int width, int height) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            Map<com.google.zxing.EncodeHintType, Object> hints = new HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "QR generation error", e);
            return null;
        }
    }

    private String getCryptoAddress(String cryptoCode) {
        switch (cryptoCode) {
            case "BTC": return "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";
            case "ETH": return "0x742d35Cc6634C0532925a3b844Bc9e8F5C3b3b1a";
            case "USDT": return "0xdAC17F958D2ee523a2206206994597C13D831ec7";
            default: return "";
        }
    }

    private String getNetworkForCrypto(String cryptoCode) {
        switch (cryptoCode) {
            case "BTC": return "Bitcoin Network";
            case "ETH": return "Ethereum Network (ERC20)";
            case "USDT": return "Tron Network (TRC20)";
            default: return "Main Network";
        }
    }

    private void showSuccessDialog(final long bookingId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Успешно!")
                .setMessage("Бронирование #" + bookingId + " подтверждено!")
                .setPositiveButton("Перейти к бронированиям", (dialog, which) -> {
                    startActivity(new Intent(BookingActivity.this, BookingsActivity.class));
                    finish();
                })
                .setNegativeButton("На главную", (dialog, which) -> {
                    startActivity(new Intent(BookingActivity.this, MainActivity.class));
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showProfileWarning() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Необходимо заполнить профиль")
                .setMessage("Для бронирования заполните паспортные данные и водительское удостоверение")
                .setPositiveButton("Заполнить", (dialog, which) -> startActivity(new Intent(BookingActivity.this, ProfileActivity.class)))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private boolean isProfileFilled() {
        if (userEmail == null || userEmail.isEmpty()) return false;
        Cursor cursor = null;
        try {
            cursor = dbHelper.getUserProfile(userEmail);
            if (cursor != null && cursor.moveToFirst()) {
                int passportSeriesIndex = cursor.getColumnIndex("passport_series");
                int passportNumberIndex = cursor.getColumnIndex("passport_number");
                int licenseNumberIndex = cursor.getColumnIndex("license_number");

                String passportSeries = (passportSeriesIndex >= 0) ? cursor.getString(passportSeriesIndex) : null;
                String passportNumber = (passportNumberIndex >= 0) ? cursor.getString(passportNumberIndex) : null;
                String licenseNumber = (licenseNumberIndex >= 0) ? cursor.getString(licenseNumberIndex) : null;

                return passportSeries != null && !passportSeries.isEmpty() &&
                        passportNumber != null && !passportNumber.isEmpty() &&
                        licenseNumber != null && !licenseNumber.isEmpty();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking profile", e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private String getDayWord(int days) {
        if (days % 10 == 1 && days % 100 != 11) return "день";
        else if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) return "дня";
        else return "дней";
    }

    private void showLoadingDialog() {
        runOnUiThread(() -> { if (progressDialog != null && !progressDialog.isShowing()) progressDialog.show(); });
    }

    private void hideLoadingDialog() {
        runOnUiThread(() -> { if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss(); });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selected_location", selectedLocation);
        outState.putString("location_details", locationDetails);
        outState.putString("selected_payment_method", selectedPaymentMethod);
        outState.putString("selected_delivery_type", selectedDeliveryType);
        outState.putLong("start_date", startDateCalendar.getTimeInMillis());
        outState.putLong("end_date", endDateCalendar.getTimeInMillis());
        outState.putString("delivery_address", deliveryAddress);
        outState.putBoolean("self_pickup_checked", selfPickupCheckBox != null && selfPickupCheckBox.isChecked());
        outState.putBoolean("delivery_checked", deliveryCheckBox != null && deliveryCheckBox.isChecked());
        outState.putBoolean("credit_card_checked", creditCardCheckBox != null && creditCardCheckBox.isChecked());
        outState.putBoolean("bitcoin_checked", bitcoinCheckBox != null && bitcoinCheckBox.isChecked());
        outState.putBoolean("ethereum_checked", ethereumCheckBox != null && ethereumCheckBox.isChecked());
        outState.putBoolean("usdt_checked", usdtCheckBox != null && usdtCheckBox.isChecked());
    }

    private void restoreSavedState(Bundle savedInstanceState) {
        selectedLocation = savedInstanceState.getString("selected_location", selectedLocation);
        locationDetails = savedInstanceState.getString("location_details", locationDetails);
        selectedPaymentMethod = savedInstanceState.getString("selected_payment_method", selectedPaymentMethod);
        selectedDeliveryType = savedInstanceState.getString("selected_delivery_type", selectedDeliveryType);
        deliveryAddress = savedInstanceState.getString("delivery_address", "");

        long startDate = savedInstanceState.getLong("start_date");
        long endDate = savedInstanceState.getLong("end_date");
        if (startDate > 0) startDateCalendar.setTimeInMillis(startDate);
        if (endDate > 0) endDateCalendar.setTimeInMillis(endDate);

        updateDateTextViews();
        updateLocationViews();

        boolean selfPickupChecked = savedInstanceState.getBoolean("self_pickup_checked", selectedDeliveryType.equals("self_pickup"));
        boolean deliveryChecked = savedInstanceState.getBoolean("delivery_checked", selectedDeliveryType.equals("delivery"));

        if (selfPickupCheckBox != null) selfPickupCheckBox.setChecked(selfPickupChecked);
        if (deliveryCheckBox != null) deliveryCheckBox.setChecked(deliveryChecked);

        boolean creditCardChecked = savedInstanceState.getBoolean("credit_card_checked", selectedPaymentMethod.equals("credit_card"));
        boolean bitcoinChecked = savedInstanceState.getBoolean("bitcoin_checked", selectedPaymentMethod.equals("bitcoin"));
        boolean ethereumChecked = savedInstanceState.getBoolean("ethereum_checked", selectedPaymentMethod.equals("ethereum"));
        boolean usdtChecked = savedInstanceState.getBoolean("usdt_checked", selectedPaymentMethod.equals("usdt"));

        if (creditCardCheckBox != null) creditCardCheckBox.setChecked(creditCardChecked);
        if (bitcoinCheckBox != null) bitcoinCheckBox.setChecked(bitcoinChecked);
        if (ethereumCheckBox != null) ethereumCheckBox.setChecked(ethereumChecked);
        if (usdtCheckBox != null) usdtCheckBox.setChecked(usdtChecked);

        if (deliveryAddress != null && !deliveryAddress.isEmpty() && deliveryAddressEditText != null) {
            deliveryAddressEditText.setText(deliveryAddress);
        }
        loadUserPaymentMethods();
        updateDeliveryViews();

        if (selectedPaymentMethod.equals("credit_card")) {
            hideCryptoInfo();
        } else if (selectedPaymentMethod.equals("bitcoin")) {
            showCryptoInfo("BTC", btcRate);
        } else if (selectedPaymentMethod.equals("ethereum")) {
            showCryptoInfo("ETH", ethRate);
        } else if (selectedPaymentMethod.equals("usdt")) {
            showCryptoInfo("USDT", usdtRate);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) executorService.shutdown();
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    static class PickupLocation {
        String name, details, workingHours;
        double distance;
        boolean available;

        public PickupLocation(String name, String details, String workingHours, double distance, boolean available) {
            this.name = name;
            this.details = details;
            this.workingHours = workingHours;
            this.distance = distance;
            this.available = available;
        }

        public String getName() { return name; }
        public String getDetails() { return details; }
        public String getWorkingHours() { return workingHours; }
        public double getDistance() { return distance; }
    }
}