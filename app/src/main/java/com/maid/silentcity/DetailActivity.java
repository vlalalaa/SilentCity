package com.maid.silentcity;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Тут буде activity_detail.xml, який ми створимо пізніше
        setContentView(R.layout.activity_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Деталі запису");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Отримання даних, переданих з адаптера
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            displayDetails(extras);
        }
    }

    private void displayDetails(Bundle extras) {
        // Отримання посилань на TextView
        TextView tvTimestamp = findViewById(R.id.tv_detail_timestamp);
        TextView tvCause = findViewById(R.id.tv_detail_cause);
        TextView tvAvgNoise = findViewById(R.id.tv_detail_avg_noise);
        TextView tvMaxNoise = findViewById(R.id.tv_detail_max_noise);
        TextView tvMinNoise = findViewById(R.id.tv_detail_min_noise);
        TextView tvCoordinates = findViewById(R.id.tv_detail_coordinates);

        // Форматування часу
        long timestamp = extras.getLong("TIMESTAMP", 0);
        String formattedDate = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(new Date(timestamp));

        // Встановлення тексту
        tvTimestamp.setText(formattedDate);
        tvCause.setText(String.format("Чинник(и): %s", extras.getString("CAUSE", "Не вказано")));
        tvAvgNoise.setText(String.format("Середній рівень шуму: %s дБ", extras.getString("AVG_NOISE", "-")));
        tvMaxNoise.setText(String.format("Максимальний рівень шуму: %s дБ", extras.getString("MAX_NOISE", "-")));
        tvMinNoise.setText(String.format("Мінімальний рівень шуму: %s дБ", extras.getString("MIN_NOISE", "-")));
        tvCoordinates.setText(String.format("Координати: %.4f, %.4f",
                extras.getDouble("LATITUDE", 0.0),
                extras.getDouble("LONGITUDE", 0.0)));
    }
}