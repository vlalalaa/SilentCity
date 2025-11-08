package com.maid.silentcity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatisticActivity extends MainActivity {

    private RecyclerView recyclerView;
    private NoiseEntryAdapter adapter;
    private final List<NoiseEntry> noiseEntries = new ArrayList<>();
    private DatabaseReference noiseEntriesRef;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private static final String TAG = "StatisticActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Встановлення макету для сторінки статистики
        setContentView(R.layout.activity_statistic);

        // Встановлення заголовка ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Silent city");
        }
        NavigationHelper.setupBottomNavigation(this);

        mAuth = FirebaseAuth.getInstance();
        recyclerView = findViewById(R.id.recyclerView_statistics);
        progressBar = findViewById(R.id.progressBar_statistics);
        noiseEntriesRef = FirebaseDatabase.getInstance().getReference("noise_entries");

        setupRecyclerView();
        loadMyEntries();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoiseEntryAdapter(this, noiseEntries);
        recyclerView.setAdapter(adapter);
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

        // 1. Сортування від найновіших до найстаріших
        // Запитуємо всі записи, сортуючи за полем "timestamp"
        Query myEntriesQuery = noiseEntriesRef.orderByChild("timestamp");

        myEntriesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noiseEntries.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    NoiseEntry entry = dataSnapshot.getValue(NoiseEntry.class);

                    // 2. Фільтруємо на стороні клієнта за email, оскільки Firebase не підтримує Query AND Query
                    if (entry != null && userEmail.equals(entry.getAuthorEmail())) {
                        noiseEntries.add(entry);
                    }
                }

                // 3. Зворотне сортування
                // Оскільки orderByChild("timestamp") сортує від старого до нового,
                // ми інвертуємо список, щоб отримати від нового до старого.
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