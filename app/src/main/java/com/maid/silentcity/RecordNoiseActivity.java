package com.maid.silentcity;

import android.Manifest;
import android.app.Activity;
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

    private MediaRecorder mRecorder = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private FusedLocationProviderClient fusedLocationClient;

    private TextView timerText, dbLevelText;

    private int secondsElapsed = 0;
    private double maxNoise = 0.0;
    private double minNoise = 150.0;
    private double totalNoise = 0.0;
    private int readingsCount = 0;
    private Location lastKnownLocation;
    private String tempFilePath;
    private long recordingStartTime; // Змінна для зберігання часу початку запису

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_noise);

        timerText = findViewById(R.id.timer_text);
        dbLevelText = findViewById(R.id.db_level_text);
        Button cancelButton = findViewById(R.id.cancel_record_button);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        cancelButton.setOnClickListener(v -> finishAndCancel());

        checkPermissions();
    }

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
            recordingStartTime = System.currentTimeMillis(); // Збереження часу початку
            mHandler.post(measurementRunnable);
        } catch (IOException e) {
            Log.e(LOG_TAG, "MediaRecorder failed", e);
            Toast.makeText(this, "Помилка мікрофона", Toast.LENGTH_SHORT).show();
            finishAndCancel();
        }
    }

    private double getAmplitudeDb() {
        if (mRecorder != null) {
            int amplitude = mRecorder.getMaxAmplitude();
            if (amplitude > 0) {
                return 20 * Math.log10(amplitude);
            }
        }
        return 0.0;
    }

    private Runnable measurementRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsElapsed < RECORD_TIME_SECONDS) {
                secondsElapsed++;
                timerText.setText(String.format("Час: %d с / %d с", secondsElapsed, RECORD_TIME_SECONDS));

                double currentDb = getAmplitudeDb();

                if (currentDb > 0) {
                    maxNoise = Math.max(maxNoise, currentDb);
                    minNoise = Math.min(minNoise, currentDb);
                    totalNoise += currentDb;
                    readingsCount++;

                    dbLevelText.setText(String.format("Поточний рівень: %.1f дБ", currentDb));
                }

                mHandler.postDelayed(this, 1000);
            } else {
                stopMeasurement();
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
        new File(tempFilePath).delete();

        double avgNoise = readingsCount > 0 ? totalNoise / readingsCount : 0.0;

        if (readingsCount == 0) {
            Toast.makeText(this, "Не вдалося виміряти шум. Спробуйте ще раз.", Toast.LENGTH_LONG).show();
            finishAndCancel();
            return;
        }

        // Перехід до активності вибору причини
        Intent intent = new Intent(RecordNoiseActivity.this, SelectCauseActivity.class);
        intent.putExtra("MAX_NOISE", String.format("%.1f", maxNoise));
        intent.putExtra("MIN_NOISE", String.format("%.1f", minNoise));
        intent.putExtra("AVG_NOISE", String.format("%.1f", avgNoise));
        intent.putExtra("TIMESTAMP", recordingStartTime); // ПЕРЕДАЄМО ЧАС

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTIVITY_SELECT_CAUSE_REQUEST_CODE) {
            // Передаємо результат далі до MainActivity
            setResult(resultCode, data);

            // Завершуємо RecordNoiseActivity
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
        mHandler.removeCallbacks(measurementRunnable);

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