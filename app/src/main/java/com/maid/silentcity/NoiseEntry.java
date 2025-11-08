package com.maid.silentcity;

public class NoiseEntry {
    private String cause;
    private String avgNoise;
    private String maxNoise;
    private String minNoise;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String authorEmail;
    private String firebaseKey; // NEW: Поле для унікального ключа Firebase

    // 1. Обов'язковий порожній конструктор для Firebase
    public NoiseEntry() {
    }

    // 2. Повний конструктор (залишається без ключа, оскільки ключ генерується при push)
    public NoiseEntry(String cause, String avgNoise, String maxNoise, String minNoise, double latitude, double longitude, long timestamp, String authorEmail) {
        this.cause = cause;
        this.avgNoise = avgNoise;
        this.maxNoise = maxNoise;
        this.minNoise = minNoise;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.authorEmail = authorEmail;
    }

    // --- ГЕТТЕРИ ---

    public String getCause() { return cause; }
    public String getAvgNoise() { return avgNoise; }
    public String getMaxNoise() { return maxNoise; }
    public String getMinNoise() { return minNoise; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
    public String getAuthorEmail() { return authorEmail; }

    // NEW: Геттер для ключа Firebase
    public String getFirebaseKey() { return firebaseKey; }


    // --- СЕТЕРИ ---

    public void setCause(String cause) { this.cause = cause; }
    public void setAvgNoise(String avgNoise) { this.avgNoise = avgNoise; }
    public void setMaxNoise(String maxNoise) { this.maxNoise = maxNoise; }
    public void setMinNoise(String minNoise) { this.minNoise = minNoise; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    // NEW: Сеттер для ключа Firebase (використовується під час завантаження)
    public void setFirebaseKey(String firebaseKey) {
        this.firebaseKey = firebaseKey;
    }
}