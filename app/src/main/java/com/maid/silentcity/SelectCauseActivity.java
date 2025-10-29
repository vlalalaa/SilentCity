package com.maid.silentcity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SelectCauseActivity extends AppCompatActivity {

    private double latitude, longitude;
    private String avgNoise, maxNoise, minNoise;
    private long timestamp;
    private String authorEmail; // НОВЕ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_cause);

        TextView statsText = findViewById(R.id.noise_stats_text);
        RadioGroup causeRadioGroup = findViewById(R.id.cause_radio_group);
        Button saveButton = findViewById(R.id.save_data_button);
        Button cancelButton = findViewById(R.id.cancel_save_button);

        // Отримуємо дані про шум та геолокацію
        Intent incomingIntent = getIntent();
        maxNoise = incomingIntent.getStringExtra("MAX_NOISE");
        minNoise = incomingIntent.getStringExtra("MIN_NOISE");
        avgNoise = incomingIntent.getStringExtra("AVG_NOISE");
        latitude = incomingIntent.getDoubleExtra("LATITUDE", 0.0);
        longitude = incomingIntent.getDoubleExtra("LONGITUDE", 0.0);
        timestamp = incomingIntent.getLongExtra("TIMESTAMP", System.currentTimeMillis());
        authorEmail = incomingIntent.getStringExtra("AUTHOR_EMAIL"); // НОВЕ: Отримуємо email

        // Відображаємо статистику
        statsText.setText(String.format("Середній: %s дБ\nМакс: %s дБ\nМін: %s дБ", avgNoise, maxNoise, minNoise));

        // Обробка кнопки "Зберегти дані"
        saveButton.setOnClickListener(v -> {
            int selectedId = causeRadioGroup.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(this, "Будь ласка, оберіть причину шуму.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (latitude == 0.0 && longitude == 0.0) {
                Toast.makeText(this, "Не вдалося отримати точні координати. Дані не можуть бути збережені.", Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            }

            RadioButton selectedRadioButton = findViewById(selectedId);
            String selectedCause = selectedRadioButton.getText().toString();

            // ПОВЕРНЕННЯ РЕЗУЛЬТАТУ RESULT_OK (Успіх)
            Intent resultIntent = new Intent();
            resultIntent.putExtra("LATITUDE", latitude);
            resultIntent.putExtra("LONGITUDE", longitude);
            resultIntent.putExtra("AVG_NOISE", avgNoise);
            resultIntent.putExtra("MAX_NOISE", maxNoise);
            resultIntent.putExtra("MIN_NOISE", minNoise);
            resultIntent.putExtra("CAUSE", selectedCause);
            resultIntent.putExtra("TIMESTAMP", timestamp);
            resultIntent.putExtra("AUTHOR_EMAIL", authorEmail); // НОВЕ: Повертаємо email

            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });

        // Обробка кнопки "Скасувати запис"
        cancelButton.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }
}