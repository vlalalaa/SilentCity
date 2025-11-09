package com.maid.silentcity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// *** НЕОБХІДНІ ІМПОРТИ ДЛЯ ГРАФІКІВ ***
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
// ****************************************

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StatisticActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoiseEntryAdapter adapter;
    private final List<NoiseEntry> noiseEntries = new ArrayList<>();
    private DatabaseReference noiseEntriesRef;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private LineChart activityChart;
    private static final String TAG = "StatisticActivity";

    // Константа для кількості днів, які ми хочемо відобразити на графіку (наприклад, 30 днів)
    private static final int DAYS_TO_LOAD = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Silent city");
        }

        NavigationHelper.setupBottomNavigation(this);

        mAuth = FirebaseAuth.getInstance();
        recyclerView = findViewById(R.id.recyclerView_statistics);
        progressBar = findViewById(R.id.progressBar_statistics);
        activityChart = findViewById(R.id.activity_chart);
        noiseEntriesRef = FirebaseDatabase.getInstance().getReference("noise_entries");

        setupRecyclerView();
        setupChart(); // Оновлено для скролінгу
        loadMyEntries();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoiseEntryAdapter(this, noiseEntries);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Налаштовує вигляд LineChart: вмикає прокручування та масштабування.
     */
    private void setupChart() {
        activityChart.getDescription().setEnabled(false);
        activityChart.setTouchEnabled(true); // УВІМКНУТИ інтерактивність
        activityChart.setDragEnabled(true);  // УВІМКНУТИ перетягування (скролінг)
        activityChart.setScaleXEnabled(true); // УВІМКНУТИ масштабування по X
        activityChart.setScaleYEnabled(false); // Вимкнути масштабування по Y
        activityChart.setPinchZoom(false); // Не використовувати двопальцевий зум
        activityChart.setDrawGridBackground(false);
        activityChart.getLegend().setEnabled(false);

        // Налаштування осі Y (ліва)
        activityChart.getAxisLeft().setAxisMinimum(0f);
        activityChart.getAxisLeft().setGranularity(1f);
        activityChart.getAxisLeft().setTextColor(Color.DKGRAY);
        activityChart.getAxisLeft().setDrawGridLines(false);

        // Налаштування осі Y (права)
        activityChart.getAxisRight().setEnabled(false);

        // Налаштування осі X
        XAxis xAxis = activityChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.DKGRAY);
        // labelCount буде автоматично встановлено на основі даних, але встановлюємо мінімум
        xAxis.setLabelCount(7, true);
    }

    /**
     * Відображає графік активності користувача за останні DAYS_TO_LOAD днів.
     * @param userEntries - усі записи користувача.
     */
    private void displayUserActivityChart(List<NoiseEntry> userEntries) {

        Map<String, Integer> dailyCount = new HashMap<>();
        // Формат YYYY-MM-dd для точного ключа (для підрахунку)
        SimpleDateFormat dateKeySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        // Формат для міток на графіку (dd/MM)
        SimpleDateFormat labelSdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        final List<String> xAxisLabels = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();

        // 1. Визначення діапазону (останні DAYS_TO_LOAD днів)
        calendar.setTimeInMillis(System.currentTimeMillis());
        // Встановлюємо дату на DAYS_TO_LOAD - 1 днів тому
        calendar.add(Calendar.DAY_OF_YEAR, -(DAYS_TO_LOAD - 1));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startTime = calendar.getTimeInMillis(); // Час початку діапазону

        // Створення міток для X-осі та ініціалізація лічильників
        for (int i = 0; i < DAYS_TO_LOAD; i++) {
            String key = dateKeySdf.format(calendar.getTime());
            String label = labelSdf.format(calendar.getTime());
            xAxisLabels.add(label);
            dailyCount.put(key, 0);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // 2. Підрахунок записів за датою
        for (NoiseEntry entry : userEntries) {
            // Фільтруємо записи, що входять в діапазон
            if (entry.getTimestamp() >= startTime) {
                String dateKey = dateKeySdf.format(entry.getTimestamp());
                if (dailyCount.containsKey(dateKey)) {
                    dailyCount.put(dateKey, dailyCount.get(dateKey) + 1);
                }
            }
        }

        // 3. Формування даних для LineChart
        List<Entry> entries = new ArrayList<>();
        // Скидаємо календар на початок діапазону для послідовного формування точок
        calendar.setTimeInMillis(startTime);

        for (int i = 0; i < DAYS_TO_LOAD; i++) {
            String dateKey = dateKeySdf.format(calendar.getTime());
            Integer count = dailyCount.get(dateKey);
            // Використовуємо індекс 'i' як X-координату
            entries.add(new Entry(i, count != null ? count : 0));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // 4. Створення LineDataSet
        LineDataSet dataSet = new LineDataSet(entries, "Записи");
        dataSet.setColor(Color.parseColor("#344278"));
        dataSet.setLineWidth(3f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(12f);
        dataSet.setCircleColor(Color.parseColor("#344278"));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true);

        // ВИМОГА 2: Лінії мають бути прямими
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        // 5. Встановлення LineData та міток X-осі
        LineData lineData = new LineData(dataSet);
        activityChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        activityChart.setData(lineData);

        // 6. Налаштування прокрутки графіка (вимога 3)
        // Встановлюємо, що на екрані поміщається максимум 7 точок
        activityChart.setVisibleXRangeMaximum(7f);
        // Переміщуємо вид до останньої точки (кінець графіка), щоб бачити останні 7 днів одразу
        if (entries.size() > 7) {
            activityChart.moveViewToX(entries.size());
        }

        // 7. Оновлення графіка
        activityChart.animateY(1000);
        activityChart.invalidate();
    }

    private void loadMyEntries() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Помилка: Користувач не автентифікований.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        final String userEmail = user.getEmail();

        // Завантажуємо записи за останній місяць (або всі, якщо база невелика)
        // Для ефективності, можна тут додати фільтр по часу, якщо записів дуже багато
        Query myEntriesQuery = noiseEntriesRef.orderByChild("timestamp");

        myEntriesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<NoiseEntry> allUserEntries = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    NoiseEntry entry = dataSnapshot.getValue(NoiseEntry.class);

                    if (entry != null && userEmail.equals(entry.getAuthorEmail())) {
                        allUserEntries.add(entry);
                    }
                }

                // Відображаємо графік з історичними даними
                displayUserActivityChart(allUserEntries);

                // Підготовка даних для RecyclerView (від нового до старого)
                noiseEntries.clear();
                noiseEntries.addAll(allUserEntries);
                Collections.reverse(noiseEntries);

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (noiseEntries.isEmpty()) {
                    Toast.makeText(StatisticActivity.this, "Ви ще не додали жодного запису.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Помилка завантаження статистики: " + error.getMessage());
                Toast.makeText(StatisticActivity.this, "Помилка завантаження: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}