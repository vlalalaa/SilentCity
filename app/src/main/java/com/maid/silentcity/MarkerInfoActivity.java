package com.maid.silentcity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MarkerInfoActivity extends AppCompatActivity {

    private String firebaseKey; // NEW: Для зберігання ключа видалення
    private String authorEmail; // NEW: Для перевірки прав
    private Button deleteButton; // NEW: Для самої кнопки

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_info);

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

        // NEW: Ініціалізуємо кнопку
        deleteButton = findViewById(R.id.btn_delete_marker);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String cause = extras.getString("CAUSE", "Невідомо");
            String avgNoise = extras.getString("AVG_NOISE", "0.0");
            String maxNoise = extras.getString("MAX_NOISE", "0.0");
            String minNoise = extras.getString("MIN_NOISE", "0.0");
            double lat = extras.getDouble("LATITUDE", 0.0);
            double lon = extras.getDouble("LONGITUDE", 0.0);
            long timestamp = extras.getLong("TIMESTAMP", System.currentTimeMillis());

            // NEW: Отримуємо ключ і email автора
            firebaseKey = extras.getString("FIREBASE_KEY");
            authorEmail = extras.getString("AUTHOR_EMAIL", "Невідомий користувач");

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());
            String dateString = sdf.format(new Date(timestamp));

            dateText.setText(String.format("Дата запису: %s", dateString));
            avgNoiseText.setText(String.format("Середній рівень шуму: %s дБ", avgNoise));
            maxNoiseText.setText(String.format("Максимальний рівень шуму: %s дБ", maxNoise));
            minNoiseText.setText(String.format("Мінімальний рівень шуму: %s дБ", minNoise));
            locationText.setText(String.format("Місцезнаходження: %.6f, %.6f", lat, lon));
            causeText.setText(String.format("Чинник(и) шуму: %s", cause));
            authorText.setText(String.format("Хто вніс: %s", authorEmail));

            // NEW: Перевірка, чи користувач може видалити
            checkIfUserCanDelete();

            // NEW: Обробник натискання кнопки
            deleteButton.setOnClickListener(v -> deleteEntry());
        }
    }

    // NEW: Метод перевірки прав
    private void checkIfUserCanDelete() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null && currentUser.getEmail() != null &&
                currentUser.getEmail().equals(authorEmail) && firebaseKey != null) {

            // Користувач є автором і ключ наявний
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }
    }

    // NEW: Метод видалення з Firebase
    private void deleteEntry() {
        if (firebaseKey == null) {
            Toast.makeText(this, "Помилка: Ключ видалення відсутній.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference entryRef = FirebaseDatabase.getInstance()
                .getReference("noise_entries")
                .child(firebaseKey);

        entryRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MarkerInfoActivity.this, "Запис видалено з бази даних.", Toast.LENGTH_LONG).show();

                    // Повертаємо результат до MainActivity, щоб оновити карту та статистику
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("IS_DELETED", true); // Сигнал про видалення
                    setResult(Activity.RESULT_OK, resultIntent);

                    finish(); // Закриваємо поточну Activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MarkerInfoActivity.this, "Помилка видалення: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Завжди повертаємо результат OK, щоб MainActivity знала, що ми повернулися
            setResult(Activity.RESULT_OK);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Перевизначаємо onBackPressed для повернення на головну сторінку
    @Override
    public void onBackPressed() {
        // Повертаємо результат OK, щоб MainActivity знала, що ми повернулися
        setResult(Activity.RESULT_OK);
        super.onBackPressed();
    }
}