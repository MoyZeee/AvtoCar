package com.example.avto;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "users.db";
    private static final int DATABASE_VERSION = 11; // Обновлено с 10 на 11
    private static DatabaseHelper instance;

    // Константы для таблицы пользователей
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_FULL_NAME = "full_name";
    private static final String COLUMN_PASSPORT_SERIES = "passport_series";
    private static final String COLUMN_PASSPORT_NUMBER = "passport_number";
    private static final String COLUMN_LICENSE_NUMBER = "license_number";
    private static final String COLUMN_PROFILE_PHOTO = "profile_photo";

    // Константы для таблицы бронирований
    private static final String TABLE_BOOKINGS = "bookings";
    private static final String COLUMN_BOOKING_ID = "booking_id";
    private static final String COLUMN_BOOKING_CAR_ID = "car_id";
    private static final String COLUMN_BOOKING_USER_EMAIL = "user_email";
    private static final String COLUMN_BOOKING_CAR_NAME = "car_name";
    private static final String COLUMN_BOOKING_PRICE_PER_DAY = "price_per_day";
    private static final String COLUMN_BOOKING_START_DATE = "start_date";
    private static final String COLUMN_BOOKING_END_DATE = "end_date";
    private static final String COLUMN_BOOKING_TOTAL_DAYS = "total_days";
    private static final String COLUMN_BOOKING_TOTAL_PRICE = "total_price";
    private static final String COLUMN_BOOKING_STATUS = "status";
    private static final String COLUMN_BOOKING_DATE = "booking_date";

    // Новые колонки для таблицы бронирований
    private static final String COLUMN_BOOKING_PICKUP_LOCATION = "pickup_location";
    private static final String COLUMN_BOOKING_LOCATION_DETAILS = "location_details";
    private static final String COLUMN_BOOKING_PAYMENT_METHOD = "payment_method";
    private static final String COLUMN_BOOKING_DELIVERY_TYPE = "delivery_type";
    private static final String COLUMN_BOOKING_DELIVERY_ADDRESS = "delivery_address";
    private static final String COLUMN_BOOKING_DELIVERY_FEE = "delivery_fee";

    // Константы для таблицы уведомлений
    private static final String TABLE_NOTIFICATIONS = "notifications";
    private static final String COLUMN_NOTIFICATION_ID = "id";
    private static final String COLUMN_NOTIFICATION_USER_EMAIL = "user_email";
    private static final String COLUMN_NOTIFICATION_TITLE = "title";
    private static final String COLUMN_NOTIFICATION_MESSAGE = "message";
    private static final String COLUMN_NOTIFICATION_TYPE = "type";
    private static final String COLUMN_NOTIFICATION_IS_READ = "is_read";
    private static final String COLUMN_NOTIFICATION_TIMESTAMP = "timestamp";

    // Константы для таблицы доступности автомобилей
    private static final String TABLE_CARS_AVAILABILITY = "cars_availability";
    private static final String COLUMN_CAR_ID = "car_id";
    private static final String COLUMN_IS_AVAILABLE = "is_available";

    // Константы для таблицы методов оплаты
    private static final String TABLE_PAYMENT_METHODS = "payment_methods";
    private static final String COLUMN_PAYMENT_ID = "id";
    private static final String COLUMN_PAYMENT_USER_EMAIL = "user_email";
    private static final String COLUMN_PAYMENT_CARD_NUMBER = "card_number";
    private static final String COLUMN_PAYMENT_CARD_HOLDER = "card_holder";
    private static final String COLUMN_PAYMENT_EXPIRY_DATE = "expiry_date";
    private static final String COLUMN_PAYMENT_CVV = "cvv";
    private static final String COLUMN_PAYMENT_CARD_LAST_FOUR = "card_last_four";
    private static final String COLUMN_PAYMENT_CREATED_AT = "created_at";

    // Синглтон паттерн
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUsersTable(db);
        createBookingsTable(db);
        createNotificationsTable(db);
        createCarsAvailabilityTable(db);
        createPaymentMethodsTable(db);
        initDefaultCarStatuses(db);
        Log.d("DatabaseHelper", "Database created with all tables");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 11) {
            // Обновление с версии 10 на 11 - обновляем структуру таблиц
            updateBookingsTable(db);
            Log.d("DatabaseHelper", "Migrated from version " + oldVersion + " to " + newVersion);
        } else if (oldVersion < 10) {
            // Миграция с версии 9 на 10
            createPaymentMethodsTable(db);
            Log.d("DatabaseHelper", "Migrated from version " + oldVersion + " to " + newVersion);
        } else if (oldVersion < 9) {
            // Миграция с версии 8 на 9
            String tempTable = TABLE_BOOKINGS + "_temp";

            String createNewTable = "CREATE TABLE " + tempTable + "("
                    + COLUMN_BOOKING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_BOOKING_CAR_ID + " INTEGER NOT NULL,"
                    + COLUMN_BOOKING_USER_EMAIL + " TEXT NOT NULL,"
                    + COLUMN_BOOKING_CAR_NAME + " TEXT NOT NULL,"
                    + COLUMN_BOOKING_PRICE_PER_DAY + " INTEGER NOT NULL,"
                    + COLUMN_BOOKING_START_DATE + " TEXT NOT NULL,"
                    + COLUMN_BOOKING_END_DATE + " TEXT NOT NULL,"
                    + COLUMN_BOOKING_TOTAL_DAYS + " INTEGER NOT NULL,"
                    + COLUMN_BOOKING_TOTAL_PRICE + " INTEGER NOT NULL,"
                    + COLUMN_BOOKING_STATUS + " TEXT DEFAULT 'active',"
                    + COLUMN_BOOKING_DATE + " TEXT NOT NULL,"
                    + COLUMN_BOOKING_PICKUP_LOCATION + " TEXT,"
                    + COLUMN_BOOKING_LOCATION_DETAILS + " TEXT,"
                    + COLUMN_BOOKING_PAYMENT_METHOD + " TEXT DEFAULT 'credit_card',"
                    + COLUMN_BOOKING_DELIVERY_TYPE + " TEXT DEFAULT 'self_pickup',"
                    + COLUMN_BOOKING_DELIVERY_ADDRESS + " TEXT,"
                    + COLUMN_BOOKING_DELIVERY_FEE + " INTEGER DEFAULT 0"
                    + ")";
            db.execSQL(createNewTable);

            String copyData = "INSERT INTO " + tempTable + " ("
                    + COLUMN_BOOKING_ID + ", " + COLUMN_BOOKING_CAR_ID + ", "
                    + COLUMN_BOOKING_USER_EMAIL + ", " + COLUMN_BOOKING_CAR_NAME + ", "
                    + COLUMN_BOOKING_PRICE_PER_DAY + ", " + COLUMN_BOOKING_START_DATE + ", "
                    + COLUMN_BOOKING_END_DATE + ", " + COLUMN_BOOKING_TOTAL_DAYS + ", "
                    + COLUMN_BOOKING_TOTAL_PRICE + ", " + COLUMN_BOOKING_STATUS + ", "
                    + COLUMN_BOOKING_DATE + ") "
                    + "SELECT " + COLUMN_BOOKING_ID + ", " + COLUMN_BOOKING_CAR_ID + ", "
                    + COLUMN_BOOKING_USER_EMAIL + ", " + COLUMN_BOOKING_CAR_NAME + ", "
                    + COLUMN_BOOKING_PRICE_PER_DAY + ", " + COLUMN_BOOKING_START_DATE + ", "
                    + COLUMN_BOOKING_END_DATE + ", " + COLUMN_BOOKING_TOTAL_DAYS + ", "
                    + COLUMN_BOOKING_TOTAL_PRICE + ", " + COLUMN_BOOKING_STATUS + ", "
                    + COLUMN_BOOKING_DATE + " FROM " + TABLE_BOOKINGS;
            db.execSQL(copyData);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
            db.execSQL("ALTER TABLE " + tempTable + " RENAME TO " + TABLE_BOOKINGS);
            createPaymentMethodsTable(db);

            Log.d("DatabaseHelper", "Migrated from version " + oldVersion + " to " + newVersion);
        } else {
            // Полное пересоздание для других версий
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARS_AVAILABILITY);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYMENT_METHODS);
            onCreate(db);
        }
    }

    private void updateBookingsTable(SQLiteDatabase db) {
        // Обновляем структуру таблицы бронирований при необходимости
        try {
            // Проверяем существование нужных индексов
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_car_id ON " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_CAR_ID + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_user_email ON " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_USER_EMAIL + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_status ON " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_STATUS + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_dates ON " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_START_DATE + ", " + COLUMN_BOOKING_END_DATE + ")");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error updating bookings table: " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        try {
            super.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error closing database: " + e.getMessage());
        }
    }

    // ===================== СОЗДАНИЕ ТАБЛИЦ =====================

    private void createUsersTable(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMAIL + " TEXT UNIQUE NOT NULL,"
                + COLUMN_PASSWORD + " TEXT NOT NULL,"
                + COLUMN_FULL_NAME + " TEXT,"
                + COLUMN_PASSPORT_SERIES + " TEXT,"
                + COLUMN_PASSPORT_NUMBER + " TEXT,"
                + COLUMN_LICENSE_NUMBER + " TEXT,"
                + COLUMN_PROFILE_PHOTO + " TEXT"
                + ")";
        db.execSQL(createUsersTable);
    }

    private void createBookingsTable(SQLiteDatabase db) {
        String createBookingsTable = "CREATE TABLE " + TABLE_BOOKINGS + "("
                + COLUMN_BOOKING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BOOKING_CAR_ID + " INTEGER NOT NULL,"
                + COLUMN_BOOKING_USER_EMAIL + " TEXT NOT NULL,"
                + COLUMN_BOOKING_CAR_NAME + " TEXT NOT NULL,"
                + COLUMN_BOOKING_PRICE_PER_DAY + " INTEGER NOT NULL,"
                + COLUMN_BOOKING_START_DATE + " TEXT NOT NULL,"
                + COLUMN_BOOKING_END_DATE + " TEXT NOT NULL,"
                + COLUMN_BOOKING_TOTAL_DAYS + " INTEGER NOT NULL,"
                + COLUMN_BOOKING_TOTAL_PRICE + " INTEGER NOT NULL,"
                + COLUMN_BOOKING_STATUS + " TEXT DEFAULT 'active',"
                + COLUMN_BOOKING_DATE + " TEXT NOT NULL,"
                + COLUMN_BOOKING_PICKUP_LOCATION + " TEXT,"
                + COLUMN_BOOKING_LOCATION_DETAILS + " TEXT,"
                + COLUMN_BOOKING_PAYMENT_METHOD + " TEXT DEFAULT 'credit_card',"
                + COLUMN_BOOKING_DELIVERY_TYPE + " TEXT DEFAULT 'self_pickup',"
                + COLUMN_BOOKING_DELIVERY_ADDRESS + " TEXT,"
                + COLUMN_BOOKING_DELIVERY_FEE + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(createBookingsTable);

        // Создаем индексы для ускорения запросов
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_car_id ON " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_CAR_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_user_email ON " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_USER_EMAIL + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_status ON " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_STATUS + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_dates ON " + TABLE_BOOKINGS + "(" + COLUMN_BOOKING_START_DATE + ", " + COLUMN_BOOKING_END_DATE + ")");
    }

    private void createNotificationsTable(SQLiteDatabase db) {
        String createNotificationsTable = "CREATE TABLE " + TABLE_NOTIFICATIONS + "("
                + COLUMN_NOTIFICATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NOTIFICATION_USER_EMAIL + " TEXT NOT NULL,"
                + COLUMN_NOTIFICATION_TITLE + " TEXT NOT NULL,"
                + COLUMN_NOTIFICATION_MESSAGE + " TEXT NOT NULL,"
                + COLUMN_NOTIFICATION_TYPE + " TEXT NOT NULL,"
                + COLUMN_NOTIFICATION_IS_READ + " INTEGER DEFAULT 0,"
                + COLUMN_NOTIFICATION_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(createNotificationsTable);
    }

    private void createCarsAvailabilityTable(SQLiteDatabase db) {
        String createCarsAvailabilityTable = "CREATE TABLE " + TABLE_CARS_AVAILABILITY + "("
                + COLUMN_CAR_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_IS_AVAILABLE + " INTEGER DEFAULT 1"
                + ")";
        db.execSQL(createCarsAvailabilityTable);
    }

    private void createPaymentMethodsTable(SQLiteDatabase db) {
        String createPaymentMethodsTable = "CREATE TABLE " + TABLE_PAYMENT_METHODS + "("
                + COLUMN_PAYMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PAYMENT_USER_EMAIL + " TEXT NOT NULL,"
                + COLUMN_PAYMENT_CARD_NUMBER + " TEXT NOT NULL,"
                + COLUMN_PAYMENT_CARD_HOLDER + " TEXT NOT NULL,"
                + COLUMN_PAYMENT_EXPIRY_DATE + " TEXT NOT NULL,"
                + COLUMN_PAYMENT_CVV + " TEXT NOT NULL,"
                + COLUMN_PAYMENT_CARD_LAST_FOUR + " TEXT NOT NULL,"
                + COLUMN_PAYMENT_CREATED_AT + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + COLUMN_PAYMENT_USER_EMAIL + ") REFERENCES "
                + TABLE_USERS + "(" + COLUMN_EMAIL + ")"
                + ")";
        db.execSQL(createPaymentMethodsTable);
    }

    private void initDefaultCarStatuses(SQLiteDatabase db) {
        for (int i = 1; i <= 8; i++) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CAR_ID, i);
            values.put(COLUMN_IS_AVAILABLE, 1);
            db.insertWithOnConflict(TABLE_CARS_AVAILABILITY, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // ===================== ОПЕРАЦИИ С ПОЛЬЗОВАТЕЛЯМИ =====================

    public synchronized long addUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_PASSWORD, password);

            long result = db.insert(TABLE_USERS, null, values);

            if (result != -1) {
                addWelcomeNotification(email);
            }
            return result;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error adding user: " + e.getMessage());
            return -1;
        }
    }

    public synchronized boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_EMAIL + " = ? AND " + COLUMN_PASSWORD + " = ?";
            cursor = db.rawQuery(query, new String[]{email, password});
            return cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error checking user: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public synchronized boolean checkUserExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_EMAIL + " = ?";
            cursor = db.rawQuery(query, new String[]{email});
            return cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error checking user existence: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public synchronized boolean updateUserProfile(String email, String fullName, String passportSeries,
                                                  String passportNumber, String licenseNumber, String profilePhoto) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_FULL_NAME, fullName);
            values.put(COLUMN_PASSPORT_SERIES, passportSeries);
            values.put(COLUMN_PASSPORT_NUMBER, passportNumber);
            values.put(COLUMN_LICENSE_NUMBER, licenseNumber);

            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                values.put(COLUMN_PROFILE_PHOTO, profilePhoto);
            }

            int rowsAffected = db.update(TABLE_USERS, values,
                    COLUMN_EMAIL + " = ?", new String[]{email});

            if (rowsAffected > 0) {
                addSystemNotification(email, "Профиль обновлен",
                        "Ваш профиль был успешно обновлен.");
            }

            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error updating user profile: " + e.getMessage());
            return false;
        }
    }

    public synchronized Cursor getUserProfile(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            String query = "SELECT " + COLUMN_FULL_NAME + ", " + COLUMN_PASSPORT_SERIES + ", " +
                    COLUMN_PASSPORT_NUMBER + ", " + COLUMN_LICENSE_NUMBER + ", " +
                    COLUMN_PROFILE_PHOTO + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_EMAIL + " = ?";
            return db.rawQuery(query, new String[]{email});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting user profile: " + e.getMessage());
            return null;
        }
    }

    public synchronized String getUserProfilePhoto(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT " + COLUMN_PROFILE_PHOTO + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_EMAIL + " = ?";
            cursor = db.rawQuery(query, new String[]{email});

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_PROFILE_PHOTO);
                if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
                    return cursor.getString(columnIndex);
                }
            }
            return null;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting user profile photo: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public synchronized String getUserFullName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT " + COLUMN_FULL_NAME + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_EMAIL + " = ?";
            cursor = db.rawQuery(query, new String[]{email});

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_FULL_NAME);
                if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
                    return cursor.getString(columnIndex);
                }
            }
            return null;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting user full name: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ===================== ОПЕРАЦИИ С БРОНИРОВАНИЯМИ =====================

    public synchronized long addBooking(Booking booking) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_BOOKING_CAR_ID, booking.getCarId());
            values.put(COLUMN_BOOKING_USER_EMAIL, booking.getUserEmail());
            values.put(COLUMN_BOOKING_CAR_NAME, booking.getCarName());
            values.put(COLUMN_BOOKING_PRICE_PER_DAY, booking.getPricePerDay());
            values.put(COLUMN_BOOKING_START_DATE, booking.getStartDate());
            values.put(COLUMN_BOOKING_END_DATE, booking.getEndDate());
            values.put(COLUMN_BOOKING_TOTAL_DAYS, booking.getTotalDays());
            values.put(COLUMN_BOOKING_TOTAL_PRICE, booking.getTotalPrice());
            values.put(COLUMN_BOOKING_STATUS, booking.getStatus());
            values.put(COLUMN_BOOKING_DATE, booking.getBookingDate());

            values.put(COLUMN_BOOKING_PICKUP_LOCATION, booking.getPickupLocation());
            values.put(COLUMN_BOOKING_LOCATION_DETAILS, booking.getLocationDetails());
            values.put(COLUMN_BOOKING_PAYMENT_METHOD, booking.getPaymentMethod());
            values.put(COLUMN_BOOKING_DELIVERY_TYPE, booking.getDeliveryType());
            values.put(COLUMN_BOOKING_DELIVERY_ADDRESS, booking.getDeliveryAddress());
            values.put(COLUMN_BOOKING_DELIVERY_FEE, booking.getDeliveryFee());

            long result = db.insert(TABLE_BOOKINGS, null, values);

            if (result != -1) {
                addBookingNotification(booking.getUserEmail(), booking.getCarName(),
                        booking.getStartDate(), booking.getEndDate());
            }

            return result;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error adding booking: " + e.getMessage());
            return -1;
        }
    }

    public synchronized boolean updateBookingStatus(long bookingId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_BOOKING_STATUS, status);

            int result = db.update(TABLE_BOOKINGS, values,
                    COLUMN_BOOKING_ID + " = ?", new String[]{String.valueOf(bookingId)});

            return result > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error updating booking status: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean cancelBooking(int bookingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            Booking booking = getBookingById(bookingId);

            ContentValues values = new ContentValues();
            values.put(COLUMN_BOOKING_STATUS, "cancelled");

            int result = db.update(TABLE_BOOKINGS, values,
                    COLUMN_BOOKING_ID + " = ?", new String[]{String.valueOf(bookingId)});

            if (result > 0 && booking != null) {
                addBookingCancelledNotification(booking.getUserEmail(), booking.getCarName());
            }

            return result > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error canceling booking: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean completeBooking(int bookingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_BOOKING_STATUS, "completed");

            int result = db.update(TABLE_BOOKINGS, values,
                    COLUMN_BOOKING_ID + " = ?", new String[]{String.valueOf(bookingId)});

            return result > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error completing booking: " + e.getMessage());
            return false;
        }
    }

    @SuppressLint("Range")
    public synchronized Booking getBookingById(int bookingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + TABLE_BOOKINGS +
                    " WHERE " + COLUMN_BOOKING_ID + " = ?";
            cursor = db.rawQuery(query, new String[]{String.valueOf(bookingId)});

            if (cursor != null && cursor.moveToFirst()) {
                Booking booking = new Booking();
                booking.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_ID)));
                booking.setCarId(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_CAR_ID)));
                booking.setUserEmail(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_USER_EMAIL)));
                booking.setCarName(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_CAR_NAME)));
                booking.setPricePerDay(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_PRICE_PER_DAY)));
                booking.setStartDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_START_DATE)));
                booking.setEndDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_END_DATE)));
                booking.setTotalDays(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_TOTAL_DAYS)));
                booking.setTotalPrice(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_TOTAL_PRICE)));
                booking.setStatus(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_STATUS)));
                booking.setBookingDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DATE)));

                booking.setPickupLocation(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_PICKUP_LOCATION)));
                booking.setLocationDetails(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_LOCATION_DETAILS)));
                booking.setPaymentMethod(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_PAYMENT_METHOD)));
                booking.setDeliveryType(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_TYPE)));
                booking.setDeliveryAddress(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_ADDRESS)));
                booking.setDeliveryFee(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_FEE)));

                return booking;
            }
            return null;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting booking by id: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @SuppressLint("Range")
    public synchronized List<Booking> getUserBookings(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Booking> bookings = new ArrayList<>();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + TABLE_BOOKINGS +
                    " WHERE " + COLUMN_BOOKING_USER_EMAIL + " = ?" +
                    " ORDER BY " + COLUMN_BOOKING_DATE + " DESC";
            cursor = db.rawQuery(query, new String[]{userEmail});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Booking booking = new Booking();
                    booking.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_ID)));
                    booking.setCarId(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_CAR_ID)));
                    booking.setUserEmail(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_USER_EMAIL)));
                    booking.setCarName(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_CAR_NAME)));
                    booking.setPricePerDay(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_PRICE_PER_DAY)));
                    booking.setStartDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_START_DATE)));
                    booking.setEndDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_END_DATE)));
                    booking.setTotalDays(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_TOTAL_DAYS)));
                    booking.setTotalPrice(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_TOTAL_PRICE)));
                    booking.setStatus(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_STATUS)));
                    booking.setBookingDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DATE)));

                    booking.setPickupLocation(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_PICKUP_LOCATION)));
                    booking.setLocationDetails(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_LOCATION_DETAILS)));
                    booking.setPaymentMethod(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_PAYMENT_METHOD)));
                    booking.setDeliveryType(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_TYPE)));
                    booking.setDeliveryAddress(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_ADDRESS)));
                    booking.setDeliveryFee(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_FEE)));

                    bookings.add(booking);
                } while (cursor.moveToNext());
            }
            return bookings;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting user bookings: " + e.getMessage());
            return bookings;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @SuppressLint("Range")
    public synchronized List<Booking> getActiveCarBookings(int carId) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Booking> bookings = new ArrayList<>();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + TABLE_BOOKINGS +
                    " WHERE " + COLUMN_BOOKING_CAR_ID + " = ?" +
                    " AND " + COLUMN_BOOKING_STATUS + " IN ('active', 'paid', 'pending_payment')" +
                    " ORDER BY " + COLUMN_BOOKING_START_DATE + " ASC";
            cursor = db.rawQuery(query, new String[]{String.valueOf(carId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Booking booking = new Booking();
                    booking.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_ID)));
                    booking.setCarId(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_CAR_ID)));
                    booking.setUserEmail(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_USER_EMAIL)));
                    booking.setCarName(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_CAR_NAME)));
                    booking.setPricePerDay(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_PRICE_PER_DAY)));
                    booking.setStartDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_START_DATE)));
                    booking.setEndDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_END_DATE)));
                    booking.setTotalDays(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_TOTAL_DAYS)));
                    booking.setTotalPrice(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_TOTAL_PRICE)));
                    booking.setStatus(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_STATUS)));
                    booking.setBookingDate(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DATE)));

                    booking.setPickupLocation(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_PICKUP_LOCATION)));
                    booking.setLocationDetails(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_LOCATION_DETAILS)));
                    booking.setPaymentMethod(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_PAYMENT_METHOD)));
                    booking.setDeliveryType(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_TYPE)));
                    booking.setDeliveryAddress(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_ADDRESS)));
                    booking.setDeliveryFee(cursor.getInt(cursor.getColumnIndex(COLUMN_BOOKING_DELIVERY_FEE)));

                    bookings.add(booking);
                } while (cursor.moveToNext());
            }
            return bookings;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting active car bookings: " + e.getMessage());
            return bookings;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ===================== ОПЕРАЦИИ С УВЕДОМЛЕНИЯМИ =====================

    public synchronized long addNotification(String userEmail, String title, String message, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NOTIFICATION_USER_EMAIL, userEmail);
            values.put(COLUMN_NOTIFICATION_TITLE, title);
            values.put(COLUMN_NOTIFICATION_MESSAGE, message);
            values.put(COLUMN_NOTIFICATION_TYPE, type);
            values.put(COLUMN_NOTIFICATION_IS_READ, 0);

            return db.insert(TABLE_NOTIFICATIONS, null, values);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error adding notification: " + e.getMessage());
            return -1;
        }
    }

    public synchronized Cursor getUserNotifications(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            String query = "SELECT * FROM " + TABLE_NOTIFICATIONS +
                    " WHERE " + COLUMN_NOTIFICATION_USER_EMAIL + " = ?" +
                    " ORDER BY " + COLUMN_NOTIFICATION_TIMESTAMP + " DESC";
            return db.rawQuery(query, new String[]{userEmail});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting user notifications: " + e.getMessage());
            return null;
        }
    }

    public synchronized int getUnreadNotificationsCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS +
                    " WHERE " + COLUMN_NOTIFICATION_USER_EMAIL + " = ?" +
                    " AND " + COLUMN_NOTIFICATION_IS_READ + " = 0";
            cursor = db.rawQuery(query, new String[]{userEmail});

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting unread notifications count: " + e.getMessage());
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public synchronized boolean markNotificationAsRead(int notificationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NOTIFICATION_IS_READ, 1);

            int result = db.update(TABLE_NOTIFICATIONS, values,
                    COLUMN_NOTIFICATION_ID + " = ?", new String[]{String.valueOf(notificationId)});

            return result > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error marking notification as read: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean markAllNotificationsAsRead(String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NOTIFICATION_IS_READ, 1);

            int result = db.update(TABLE_NOTIFICATIONS, values,
                    COLUMN_NOTIFICATION_USER_EMAIL + " = ? AND " +
                            COLUMN_NOTIFICATION_IS_READ + " = 0", new String[]{userEmail});

            return result > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error marking all notifications as read: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean deleteNotification(int notificationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int result = db.delete(TABLE_NOTIFICATIONS,
                    COLUMN_NOTIFICATION_ID + " = ?", new String[]{String.valueOf(notificationId)});
            return result > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting notification: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean deleteAllUserNotifications(String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int rowsDeleted = db.delete(TABLE_NOTIFICATIONS,
                    COLUMN_NOTIFICATION_USER_EMAIL + " = ?", new String[]{userEmail});
            return rowsDeleted > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting all user notifications: " + e.getMessage());
            return false;
        }
    }

    // ===================== ОПЕРАЦИИ С ДОСТУПНОСТЬЮ АВТОМОБИЛЕЙ =====================

    @SuppressLint("Range")
    public synchronized boolean getCarBaseAvailability(int carId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT " + COLUMN_IS_AVAILABLE + " FROM " + TABLE_CARS_AVAILABILITY +
                    " WHERE " + COLUMN_CAR_ID + " = ?";
            cursor = db.rawQuery(query, new String[]{String.valueOf(carId)});

            if (cursor != null && cursor.moveToFirst()) {
                int availableInt = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_AVAILABLE));
                return availableInt == 1;
            }
            return true;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting car base availability: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public synchronized boolean updateCarBaseAvailability(int carId, boolean isAvailable) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_IS_AVAILABLE, isAvailable ? 1 : 0);

            int rowsAffected = db.update(TABLE_CARS_AVAILABILITY, values,
                    COLUMN_CAR_ID + " = ?", new String[]{String.valueOf(carId)});

            if (rowsAffected == 0) {
                values.put(COLUMN_CAR_ID, carId);
                long result = db.insert(TABLE_CARS_AVAILABILITY, null, values);
                return result != -1;
            }

            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error updating car base availability: " + e.getMessage());
            return false;
        }
    }

    // ОСНОВНОЙ МЕТОД ДЛЯ ПРОВЕРКИ ЗАБРОНИРОВАН ЛИ АВТОМОБИЛЬ НА ДАТЫ
    public synchronized boolean isCarBooked(int carId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Проверяем бронирования со статусами active, paid, pending_payment
            // Условие пересечения дат:
            // существующее бронирование (start, end) пересекается с запрашиваемыми датами (startDate, endDate)
            String query = "SELECT COUNT(*) FROM " + TABLE_BOOKINGS +
                    " WHERE " + COLUMN_BOOKING_CAR_ID + " = ?" +
                    " AND " + COLUMN_BOOKING_STATUS + " IN ('active', 'paid', 'pending_payment')" +
                    " AND (" + COLUMN_BOOKING_START_DATE + " <= ? AND " + COLUMN_BOOKING_END_DATE + " >= ?)";

            cursor = db.rawQuery(query, new String[]{
                    String.valueOf(carId),
                    endDate,
                    startDate
            });

            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d("DatabaseHelper", "isCarBooked: carId=" + carId + ", startDate=" + startDate +
                        ", endDate=" + endDate + ", count=" + count);
                return count > 0;
            }
            return false;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error checking if car is booked: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // Упрощенная проверка бронирования (с использованием текущих дат)
    public synchronized boolean isCarCurrentlyBooked(int carId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String today = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        String futureDate = sdf.format(calendar.getTime());

        return isCarBooked(carId, today, futureDate);
    }

    public synchronized boolean isCarAvailableForDates(int carId, String startDate, String endDate) {
        // Сначала проверяем, не на ремонте ли автомобиль
        if (!getCarBaseAvailability(carId)) {
            Log.d("DatabaseHelper", "Car " + carId + " is on repair");
            return false;
        }
        // Затем проверяем, нет ли пересекающихся бронирований
        boolean isBooked = isCarBooked(carId, startDate, endDate);
        Log.d("DatabaseHelper", "Car " + carId + " is available for dates: " + !isBooked);
        return !isBooked;
    }

    @SuppressLint("Range")
    public synchronized Map<Integer, Boolean> getAllCarsBaseAvailability() {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<Integer, Boolean> availabilityMap = new HashMap<>();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + TABLE_CARS_AVAILABILITY;
            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int carId = cursor.getInt(cursor.getColumnIndex(COLUMN_CAR_ID));
                    int availableInt = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_AVAILABLE));
                    boolean isAvailable = availableInt == 1;
                    availabilityMap.put(carId, isAvailable);
                } while (cursor.moveToNext());
            }
            return availabilityMap;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting all cars base availability: " + e.getMessage());
            return availabilityMap;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // Получить все автомобили с их полной доступностью (с учетом бронирований)
    @SuppressLint("Range")
    public synchronized Map<Integer, Boolean> getAllCarsFullAvailability(String checkStartDate, String checkEndDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<Integer, Boolean> availabilityMap = new HashMap<>();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + TABLE_CARS_AVAILABILITY;
            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int carId = cursor.getInt(cursor.getColumnIndex(COLUMN_CAR_ID));
                    int availableInt = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_AVAILABLE));
                    boolean isOnRepair = availableInt == 0;

                    if (isOnRepair) {
                        availabilityMap.put(carId, false);
                    } else {
                        boolean isBooked = isCarBooked(carId, checkStartDate, checkEndDate);
                        availabilityMap.put(carId, !isBooked);
                    }
                } while (cursor.moveToNext());
            }
            return availabilityMap;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting all cars full availability: " + e.getMessage());
            return availabilityMap;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ===================== ОПЕРАЦИИ С МЕТОДАМИ ОПЛАТЫ =====================

    public synchronized long addPaymentMethod(String userEmail, String cardNumber,
                                              String cardHolder, String expiry,
                                              String cvv, String lastFourDigits) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_PAYMENT_USER_EMAIL, userEmail);
            values.put(COLUMN_PAYMENT_CARD_NUMBER, cardNumber);
            values.put(COLUMN_PAYMENT_CARD_HOLDER, cardHolder);
            values.put(COLUMN_PAYMENT_EXPIRY_DATE, expiry);
            values.put(COLUMN_PAYMENT_CVV, cvv);
            values.put(COLUMN_PAYMENT_CARD_LAST_FOUR, lastFourDigits);
            values.put(COLUMN_PAYMENT_CREATED_AT, System.currentTimeMillis());

            long result = db.insert(TABLE_PAYMENT_METHODS, null, values);

            if (result != -1) {
                addSystemNotification(userEmail, "Новая карта добавлена",
                        "Карта **** " + lastFourDigits + " была успешно добавлена.");
            }

            return result;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error adding payment method: " + e.getMessage());
            return -1;
        }
    }

    public synchronized Cursor getUserPaymentMethods(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            String query = "SELECT " + COLUMN_PAYMENT_CARD_LAST_FOUR + ", "
                    + COLUMN_PAYMENT_EXPIRY_DATE + ", "
                    + COLUMN_PAYMENT_CARD_HOLDER + " FROM " + TABLE_PAYMENT_METHODS +
                    " WHERE " + COLUMN_PAYMENT_USER_EMAIL + " = ?" +
                    " ORDER BY " + COLUMN_PAYMENT_CREATED_AT + " DESC" +
                    " LIMIT 1";
            return db.rawQuery(query, new String[]{email});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting user payment methods: " + e.getMessage());
            return null;
        }
    }

    public synchronized Cursor getAllUserPaymentMethods(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            String query = "SELECT * FROM " + TABLE_PAYMENT_METHODS +
                    " WHERE " + COLUMN_PAYMENT_USER_EMAIL + " = ?" +
                    " ORDER BY " + COLUMN_PAYMENT_CREATED_AT + " DESC";
            return db.rawQuery(query, new String[]{email});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting all user payment methods: " + e.getMessage());
            return null;
        }
    }

    public synchronized boolean deletePaymentMethod(int paymentMethodId, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int result = db.delete(TABLE_PAYMENT_METHODS,
                    COLUMN_PAYMENT_ID + " = ? AND " + COLUMN_PAYMENT_USER_EMAIL + " = ?",
                    new String[]{String.valueOf(paymentMethodId), userEmail});

            if (result > 0) {
                addSystemNotification(userEmail, "Карта удалена",
                        "Карта была успешно удалена из вашего профиля.");
            }

            return result > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting payment method: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean deleteAllUserPaymentMethods(String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int rowsDeleted = db.delete(TABLE_PAYMENT_METHODS,
                    COLUMN_PAYMENT_USER_EMAIL + " = ?",
                    new String[]{userEmail});

            if (rowsDeleted > 0) {
                addSystemNotification(userEmail, "Все карты удалены",
                        "Все ваши карты были удалены из профиля.");
            }

            return rowsDeleted > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting all user payment methods: " + e.getMessage());
            return false;
        }
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

    public synchronized void clearAllBookings() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_BOOKINGS, null, null);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error clearing all bookings: " + e.getMessage());
        }
    }

    private void addWelcomeNotification(String userEmail) {
        addNotification(userEmail,
                "Добро пожаловать!",
                "Добро пожаловать в приложение аренды автомобилей. Здесь вы будете получать уведомления о ваших бронированиях.",
                "system"
        );
    }

    private void addBookingNotification(String userEmail, String carName, String startDate, String endDate) {
        addNotification(userEmail,
                "Бронирование подтверждено",
                "Ваше бронирование автомобиля " + carName + " с " + startDate + " по " + endDate + " подтверждено.",
                "booking"
        );
    }

    private void addBookingCancelledNotification(String userEmail, String carName) {
        addNotification(userEmail,
                "Бронирование отменено",
                "Ваше бронирование автомобиля " + carName + " было отменено.",
                "booking"
        );
    }

    private void addSystemNotification(String userEmail, String title, String message) {
        addNotification(userEmail, title, message, "system");
    }
}