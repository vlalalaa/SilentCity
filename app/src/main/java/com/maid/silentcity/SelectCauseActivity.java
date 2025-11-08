package com.maid.silentcity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox; // Змінено на CheckBox
import android.widget.LinearLayout; // Використовуємо LinearLayout для контейнера
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SelectCauseActivity extends AppCompatActivity {

    private double latitude, longitude;
    private String avgNoise, maxNoise, minNoise;
    private long timestamp;
    private String authorEmail;
    private LinearLayout causeCheckboxGroup; // Посилання на контейнер CheckBox-ів

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_cause);

        TextView statsText = findViewById(R.id.noise_stats_text);
        causeCheckboxGroup = findViewById(R.id.cause_checkbox_group); // Оновлено ID
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
        authorEmail = incomingIntent.getStringExtra("AUTHOR_EMAIL");

        // Відображаємо статистику
        statsText.setText(String.format("Середній: %s дБ\nМакс: %s дБ\nМін: %s дБ", avgNoise, maxNoise, minNoise));

        // Обробка кнопки "Зберегти дані"
        saveButton.setOnClickListener(v -> {

            // --- НОВА ЛОГІКА ЗБОРУ МНОЖИННИХ ПРИЧИН ---
            String selectedCause = getSelectedCauses();

            if (selectedCause.isEmpty()) {
                Toast.makeText(this, "Будь ласка, оберіть хоча б одну причину шуму.", Toast.LENGTH_SHORT).show();
                return;
            }
            // --- КІНЕЦЬ НОВОЇ ЛОГІКИ ---

            if (latitude == 0.0 && longitude == 0.0) {
                Toast.makeText(this, "Не вдалося отримати точні координати. Дані не можуть бути збережені.", Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            }

            // ПОВЕРНЕННЯ РЕЗУЛЬТАТУ RESULT_OK (Успіх)
            Intent resultIntent = new Intent();
            resultIntent.putExtra("LATITUDE", latitude);
            resultIntent.putExtra("LONGITUDE", longitude);
            resultIntent.putExtra("AVG_NOISE", avgNoise);
            resultIntent.putExtra("MAX_NOISE", maxNoise);
            resultIntent.putExtra("MIN_NOISE", minNoise);
            resultIntent.putExtra("CAUSE", selectedCause); // selectedCause тепер рядок з роздільниками
            resultIntent.putExtra("TIMESTAMP", timestamp);
            resultIntent.putExtra("AUTHOR_EMAIL", authorEmail);

            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });

        // Обробка кнопки "Скасувати запис"
        cancelButton.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    /**
     * Проходить по всіх CheckBox у контейнері та збирає вибрані причини в один рядок.
     * @return Рядок вибраних причин, розділених комою (наприклад, "Транспорт, Будівництво").
     */
    private String getSelectedCauses() {
        List<String> selectedCauses = new ArrayList<>();

        // Перебираємо всі дочірні елементи в контейнері
        for (int i = 0; i < causeCheckboxGroup.getChildCount(); i++) {
            View child = causeCheckboxGroup.getChildAt(i);

            // Перевіряємо, чи є дочірній елемент CheckBox
            if (child instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) child;

                // Якщо CheckBox вибраний, додаємо його текст до списку
                if (checkBox.isChecked()) {
                    selectedCauses.add(checkBox.getText().toString());
                }
            }
        }

        // Об'єднуємо список у рядок, розділений комою та пробілом
        return String.join(", ", selectedCauses);
    }
}