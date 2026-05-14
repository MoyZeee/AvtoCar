package com.example.avto;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Booking implements Parcelable {
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
    private String pickupLocation;
    private String locationDetails;
    private String paymentMethod;
    private String deliveryType;
    private String deliveryAddress;
    private int deliveryFee;

    // Конструкторы
    public Booking() {}

    public Booking(int carId, String userEmail, String carName, int pricePerDay,
                   String startDate, String endDate, int totalDays, int totalPrice,
                   String status, String bookingDate, String pickupLocation,
                   String locationDetails, String paymentMethod, String deliveryType,
                   String deliveryAddress, int deliveryFee) {
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
        this.pickupLocation = pickupLocation;
        this.locationDetails = locationDetails;
        this.paymentMethod = paymentMethod;
        this.deliveryType = deliveryType;
        this.deliveryAddress = deliveryAddress;
        this.deliveryFee = deliveryFee;
    }

    // Parcelable implementation
    protected Booking(Parcel in) {
        id = in.readInt();
        carId = in.readInt();
        userEmail = in.readString();
        carName = in.readString();
        pricePerDay = in.readInt();
        startDate = in.readString();
        endDate = in.readString();
        totalDays = in.readInt();
        totalPrice = in.readInt();
        status = in.readString();
        bookingDate = in.readString();
        pickupLocation = in.readString();
        locationDetails = in.readString();
        paymentMethod = in.readString();
        deliveryType = in.readString();
        deliveryAddress = in.readString();
        deliveryFee = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(carId);
        dest.writeString(userEmail);
        dest.writeString(carName);
        dest.writeInt(pricePerDay);
        dest.writeString(startDate);
        dest.writeString(endDate);
        dest.writeInt(totalDays);
        dest.writeInt(totalPrice);
        dest.writeString(status);
        dest.writeString(bookingDate);
        dest.writeString(pickupLocation);
        dest.writeString(locationDetails);
        dest.writeString(paymentMethod);
        dest.writeString(deliveryType);
        dest.writeString(deliveryAddress);
        dest.writeInt(deliveryFee);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Booking> CREATOR = new Creator<Booking>() {
        @Override
        public Booking createFromParcel(Parcel in) {
            return new Booking(in);
        }

        @Override
        public Booking[] newArray(int size) {
            return new Booking[size];
        }
    };

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

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getLocationDetails() {
        return locationDetails;
    }

    public void setLocationDetails(String locationDetails) {
        this.locationDetails = locationDetails;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public int getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(int deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    // Вспомогательные методы
    public String getFormattedDates() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            Date start = inputFormat.parse(startDate);
            Date end = inputFormat.parse(endDate);

            return outputFormat.format(start) + " - " + outputFormat.format(end);
        } catch (Exception e) {
            return startDate + " - " + endDate;
        }
    }

    public String getFormattedTotalPrice() {
        return String.format(Locale.getDefault(), "%,d ₽", totalPrice);
    }

    public String getStatusInRussian() {
        switch (status) {
            case "pending_payment":
                return "Ожидает оплаты";
            case "paid":
                return "Оплачено";
            case "active":
                return "Активно";
            case "completed":
                return "Завершено";
            case "cancelled":
                return "Отменено";
            case "payment_failed":
                return "Ошибка оплаты";
            default:
                return status;
        }
    }

    public String getPaymentMethodInRussian() {
        switch (paymentMethod) {
            case "credit_card":
                return "Банковская карта";
            case "bitcoin":
                return "Bitcoin (BTC)";
            case "ethereum":
                return "Ethereum (ETH)";
            case "usdt":
                return "Tether (USDT)";
            default:
                return paymentMethod;
        }
    }

    public String getDeliveryTypeInRussian() {
        if ("delivery".equals(deliveryType)) {
            return "Доставка";
        } else {
            return "Самовывоз";
        }
    }

    public String getFormattedBookingDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

            Date date = inputFormat.parse(bookingDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return bookingDate;
        }
    }

    public boolean isActive() {
        return "active".equals(status) || "paid".equals(status) || "pending_payment".equals(status);
    }

    public boolean isCancelled() {
        return "cancelled".equals(status);
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }
}