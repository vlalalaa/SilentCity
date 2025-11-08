package com.maid.silentcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;

public class RecordNoiseActivity extends AppCompatActivity {

    private static final int RECORD_TIME_SECONDS = 10;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int ACTIVITY_SELECT_CAUSE_REQUEST_CODE = 1;
    private static final String LOG_TAG = "NoiseRecorder";

    // Інтервал для плавної візуалізації та збору статистики
    private static final int VISUALIZATION_INTERVAL_MS = 50;
    // НОВЕ: Інтервал для оновлення тексту дБ на екрані
    private static final int DISPLAY_UPDATE_INTERVAL_MS = 1000;

    private MediaRecorder mRecorder = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private FusedLocationProviderClient fusedLocationClient;

    private TextView timerText, dbLevelText;
    private VisualizerView visualizerView;

    private int secondsElapsed = 0;
    private double maxNoise = 0.0;
    private double minNoise = 150.0;
    private double totalDbSum = 0.0;
    private int totalDbCount = 0;
    private double currentDbLevel = 0.0; // Змінна для зберігання останнього виміру дБ

    private Location lastKnownLocation;
    private String tempFilePath;
    private long recordingStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_noise);

        timerText = findViewById(R.id.timer_text);
        dbLevelText = findViewById(R.id.db_level_text);
        Button cancelButton = findViewById(R.id.cancel_record_button);
        visualizerView = findViewById(R.id.visualizer_view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        cancelButton.setOnClickListener(v -> finishAndCancel());

        checkPermissions();
    }

    // ... (checkPermissions, onRequestPermissionsResult, getLocation залишаються незмінними) ...

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            startRecordingAndLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                startRecordingAndLocation();
            } else {
                Toast.makeText(this, "Потрібні дозволи для запису шуму та геолокації.", Toast.LENGTH_LONG).show();
                finishAndCancel();
            }
        }
    }

    private void startRecordingAndLocation() {
        getLocation();
        startNoiseMeasurement();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    lastKnownLocation = location;
                    Log.d(LOG_TAG, "Location found: " + location.getLatitude());
                } else {
                    Log.w(LOG_TAG, "Location is null. Using placeholder.");
                }
            });
        }
    }

    private void startNoiseMeasurement() {
        tempFilePath = getExternalCacheDir().getAbsolutePath() + File.separator + "temp_noise_measurement.3gp";

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(tempFilePath);

        try {
            mRecorder.prepare();
            mRecorder.start();
            recordingStartTime = System.currentTimeMillis();

            // Запускаємо ТРИ Runnable: таймер (1с), вимірювання (50мс), оновлення дБ тексту (1с)
            mHandler.post(timerRunnable);
            mHandler.post(measurementRunnable);
            mHandler.post(updateDisplayRunnable); // НОВЕ: Запускаємо окреме оновлення тексту
        } catch (IOException e) {
            Log.e(LOG_TAG, "MediaRecorder failed", e);
            Toast.makeText(this, "Помилка мікрофона", Toast.LENGTH_SHORT).show();
            finishAndCancel();
        }
    }

    private int getAmplitude() {
        if (mRecorder != null) {
            return mRecorder.getMaxAmplitude();
        }
        return 0;
    }

    private double getAmplitudeDb(int amplitude) {
        if (amplitude > 0) {
            return 20 * Math.log10(amplitude / 1.0);
        }
        return 0.0;
    }

    // Runnable 1: Оновлення таймера (кожну секунду)
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsElapsed < RECORD_TIME_SECONDS) {
                secondsElapsed++;
                timerText.setText(String.format("Час: %d с / %d с", secondsElapsed, RECORD_TIME_SECONDS));
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    // Runnable 2: Збір даних та Візуалізація (кожні 50 мс)
    private Runnable measurementRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsElapsed < RECORD_TIME_SECONDS) {

                int rawAmplitude = getAmplitude();
                double currentDb = getAmplitudeDb(rawAmplitude);

                // 1. Оновлення візуалізатора (завжди 50 мс для плавності)
                if (visualizerView != null) {
                    visualizerView.setAmplitude(rawAmplitude);
                }

                // 2. Збір статистики
                if (currentDb > 0) {
                    currentDbLevel = currentDb; // Зберігаємо останнє значення
                    maxNoise = Math.max(maxNoise, currentDb);
                    minNoise = Math.min(minNoise, currentDb);
                    totalDbSum += currentDb;
                    totalDbCount++;
                }

                mHandler.postDelayed(this, VISUALIZATION_INTERVAL_MS);
            } else {
                // Якщо час вийшов, зупиняємо
                stopMeasurement();
            }
        }
    };

    // Runnable 3: Оновлення тексту дБ на екрані (кожну секунду)
    private Runnable updateDisplayRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsElapsed < RECORD_TIME_SECONDS) {
                // ВИПРАВЛЕНО: Оновлюємо текст лише раз на секунду
                dbLevelText.setText(String.format("Поточний рівень: %.1f дБ", currentDbLevel));
                mHandler.postDelayed(this, DISPLAY_UPDATE_INTERVAL_MS);
            }
        }
    };


    private void stopMeasurement() {
        // Зупинка MediaRecorder
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (RuntimeException stopException) {
                Log.e(LOG_TAG, "Stop failed", stopException);
            }
            mRecorder = null;
        }
        // Видаляємо тимчасовий файл
        File tempFile = new File(tempFilePath);
        if (tempFile.exists()) {
            tempFile.delete();
        }

        // Зупиняємо УСІ Runnable
        mHandler.removeCallbacks(timerRunnable);
        mHandler.removeCallbacks(measurementRunnable);
        mHandler.removeCallbacks(updateDisplayRunnable);


        double avgNoise = totalDbCount > 0 ? totalDbSum / totalDbCount : 0.0;

        if (totalDbCount == 0) {
            Toast.makeText(this, "Не вдалося виміряти шум. Спробуйте ще раз.", Toast.LENGTH_LONG).show();
            finishAndCancel();
            return;
        }

        if (minNoise == 150.0) {
            minNoise = avgNoise;
        }


        // Перехід до активності вибору причини
        Intent intent = new Intent(RecordNoiseActivity.this, SelectCauseActivity.class);
        intent.putExtra("MAX_NOISE", String.format("%.1f", maxNoise));
        intent.putExtra("MIN_NOISE", String.format("%.1f", minNoise));
        intent.putExtra("AVG_NOISE", String.format("%.1f", avgNoise));
        intent.putExtra("TIMESTAMP", recordingStartTime);

        // Передача координат
        if (lastKnownLocation != null) {
            intent.putExtra("LATITUDE", lastKnownLocation.getLatitude());
            intent.putExtra("LONGITUDE", lastKnownLocation.getLongitude());
        } else {
            intent.putExtra("LATITUDE", 0.0);
            intent.putExtra("LONGITUDE", 0.0);
            Toast.makeText(this, "Попередження: Геолокація не отримана.", Toast.LENGTH_LONG).show();
        }

        startActivityForResult(intent, ACTIVITY_SELECT_CAUSE_REQUEST_CODE);
    }

    // ... (onActivityResult та finishAndCancel оновлено для видалення updateDisplayRunnable) ...

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTIVITY_SELECT_CAUSE_REQUEST_CODE) {
            setResult(resultCode, data);
            finish();
        }
    }

    private void finishAndCancel() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (RuntimeException ignored) {}
            mRecorder = null;
        }
        // Обов'язково видаляємо ВСІ Runnable
        mHandler.removeCallbacks(timerRunnable);
        mHandler.removeCallbacks(measurementRunnable);
        mHandler.removeCallbacks(updateDisplayRunnable); // ДОДАНО

        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing() && mRecorder != null) {
            finishAndCancel();
        }
    }
}