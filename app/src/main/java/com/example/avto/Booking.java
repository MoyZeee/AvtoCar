package com.example.avto;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Booking implements Serializable {
    private int id;
    private int carId;
    private String userEmail;
    private String carName;
    private int pricePerDay;
    private String startDate;
    private String endDate;
    private int totalDays;
    private int totalPrice;
    private String status;
    private String bookingDate;

    // Конструктор по умолчанию
    public Booking() {
    }

    // Полный конструктор
    public Booking(int carId, String userEmail, String carName, int pricePerDay,
                   String startDate, String endDate, int totalDays, int totalPrice,
                   String status, String bookingDate) {
        this.carId = carId;
        this.userEmail = userEmail;
        this.carName = carName;
        this.pricePerDay = pricePerDay;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = totalDays;
        this.totalPrice = totalPrice;
        this.status = status;
        this.bookingDate = bookingDate;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public int getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(int pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    // Методы форматирования
    public String getFormattedDates() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            Date start = inputFormat.parse(this.startDate);
            Date end = inputFormat.parse(this.endDate);

            return outputFormat.format(start) + " - " + outputFormat.format(end);
        } catch (ParseException e) {
            e.printStackTrace();
            return this.startDate + " - " + this.endDate;
        }
    }

    public String getFormattedTotalPrice() {
        return String.format("%,d ₽", this.totalPrice);
    }

    public String getStatusInRussian() {
        switch (this.status) {
            case "active":
                return "Активно";
            case "completed":
                return "Завершено";
            case "cancelled":
                return "Отменено";
            default:
                return this.status;
        }
    }

    // Метод для проверки, можно ли отменить бронирование
    public boolean canBeCancelled() {
        if (!"active".equals(this.status)) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDateObj = sdf.parse(this.startDate);
            Date today = new Date();

            // Можно отменить только если до начала бронирования больше 24 часов
            long diff = startDateObj.getTime() - today.getTime();
            long hoursDiff = diff / (1000 * 60 * 60);

            return hoursDiff > 24;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Метод для проверки, можно ли завершить бронирование
    public boolean canBeCompleted() {
        if (!"active".equals(this.status)) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date endDateObj = sdf.parse(this.endDate);
            Date today = new Date();

            // Можно завершить только если дата окончания уже прошла
            return today.after(endDateObj);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}