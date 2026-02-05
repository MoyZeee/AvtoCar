package com.example.avto;

import java.io.Serializable;

public class Car implements Serializable {
    private int id;
    private String name;
    private int pricePerDay;
    private String bodyType;
    private boolean available;
    private String transmission;
    private String fuelType;
    private int seats;
    private int imageResId;

    public Car(int id, String name, int pricePerDay, String bodyType,
               boolean available, String transmission, String fuelType, int seats, int imageResId) {
        this.id = id;
        this.name = name;
        this.pricePerDay = pricePerDay;
        this.bodyType = bodyType;
        this.available = available;
        this.transmission = transmission;
        this.fuelType = fuelType;
        this.seats = seats;
        this.imageResId = imageResId;
    }

    // Геттеры
    public int getId() { return id; }
    public String getName() { return name; }
    public int getPricePerDay() { return pricePerDay; }
    public String getBodyType() { return bodyType; }
    public boolean isAvailable() { return available; }
    public String getTransmission() { return transmission; }
    public String getFuelType() { return fuelType; }
    public int getSeats() { return seats; }
    public int getImageResId() { return imageResId; }

    // СЕТТЕР ДЛЯ СТАТУСА (ДОБАВИТЬ)
    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getDescription() {
        return transmission + ", " + fuelType + ", " + seats + " мест";
    }

    public String getFormattedPrice() {
        return String.format("%,d ₽", pricePerDay);
    }
}