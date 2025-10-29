package com.maid.silentcity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MarkerInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_info);

        // Приховуємо стандартний ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Повернутись");
            getSupportActionBar().setElevation(0f);
        }

        TextView dateText = findViewById(R.id.info_date);
        TextView avgNoiseText = findViewById(R.id.info_avg_noise);
        TextView maxNoiseText = findViewById(R.id.info_max_noise);
        TextView minNoiseText = findViewById(R.id.info_min_noise);
        TextView locationText = findViewById(R.id.info_location);
        TextView causeText = findViewById(R.id.info_cause);
        TextView authorText = findViewById(R.id.info_author_email);



        // Отримання даних, переданих з MainActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String cause = extras.getString("CAUSE", "Невідомо");
            String avgNoise = extras.getString("AVG_NOISE", "0.0");
            String maxNoise = extras.getString("MAX_NOISE", "0.0");
            String minNoise = extras.getString("MIN_NOISE", "0.0");
            double lat = extras.getDouble("LATITUDE", 0.0);
            double lon = extras.getDouble("LONGITUDE", 0.0);
            long timestamp = extras.getLong("TIMESTAMP", System.currentTimeMillis());
            String authorEmail = extras.getString("AUTHOR_EMAIL", "Невідомий користувач");

            // Форматування дати
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            String dateString = sdf.format(new Date(timestamp));

            // Відображення даних
            dateText.setText(String.format("Дата запису: %s", dateString));
            avgNoiseText.setText(String.format("Середній рівень шуму: %s дБ", avgNoise));
            maxNoiseText.setText(String.format("Максимальний рівень шуму: %s дБ", maxNoise));
            minNoiseText.setText(String.format("Мінімальний рівень шуму: %s дБ", minNoise));
            locationText.setText(String.format("Місцезнаходження: %.6f, %.6f", lat, lon));
            causeText.setText(String.format("Чинник(и) шуму: %s", cause));
            authorText.setText(String.format("Хто вніс: %s", authorEmail));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}