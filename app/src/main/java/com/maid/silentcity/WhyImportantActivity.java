package com.maid.silentcity;

import androidx.appcompat.app.AppCompatActivity; // Рекомендовано, але ви можете використовувати MainActivity, якщо це єдиний робочий варіант

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;

// ПРИМІТКА: Розширте клас, який ви використовуєте в InstructionsActivity (ймовірно, MainActivity або AppCompatActivity)
public class WhyImportantActivity extends MainActivity { // Замініть на AppCompatActivity, якщо вона у вас працює

    private ScrollView scrollView;
    private ImageButton btnScrollToTop;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Завантажуємо НОВИЙ макет
        setContentView(R.layout.activity_why_important);

        // 1. Ініціалізація елементів
        scrollView = findViewById(R.id.scrollView_why_important);
        btnScrollToTop = findViewById(R.id.btnScrollToTop_why_important);

        // 2. Налаштування ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Повернутись");
            getSupportActionBar().setElevation(0f);
        }

        // 3. Обробка прокручування для відображення/приховування кнопки "Нагору"
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = scrollView.getScrollY();

            // Показуємо кнопку, якщо прокрутили більше ніж на 500 пікселів
            if (scrollY > 500) {
                if (btnScrollToTop.getVisibility() == View.GONE) {
                    btnScrollToTop.setVisibility(View.VISIBLE);
                }
            } else {
                if (btnScrollToTop.getVisibility() == View.VISIBLE) {
                    btnScrollToTop.setVisibility(View.GONE);
                }
            }
        });

        // 4. Обробка натискання кнопки "Нагору"
        btnScrollToTop.setOnClickListener(v -> {
            scrollView.smoothScrollTo(0, 0);
        });
    }

    // 5. Обробка натискання стрілки "Назад"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 6. ПРИБИРАЄМО БУРГЕР-МЕНЮ (як і в InstructionsActivity)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}