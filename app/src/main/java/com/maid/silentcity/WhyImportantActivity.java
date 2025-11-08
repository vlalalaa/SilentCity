package com.maid.silentcity;

import androidx.appcompat.app.AppCompatActivity; // Змінено на AppCompatActivity для кращої сумісності

import android.annotation.SuppressLint;
import android.os.Build; // Додано для перевірки версії API
import android.os.Bundle;
import android.text.Html; // Додано для роботи з HTML
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView; // Додано для роботи з TextView

public class WhyImportantActivity extends AppCompatActivity { // Змінено на AppCompatActivity

    private ScrollView scrollView;
    private ImageButton btnScrollToTop;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_why_important);

        scrollView = findViewById(R.id.scrollView_why_important);
        btnScrollToTop = findViewById(R.id.btnScrollToTop_why_important);
        TextView whyImportantContent = findViewById(R.id.whyImportantContent); // Отримання TextView (новий ID)

        // 1. Застосування HTML-форматування
        String htmlText = getString(R.string.why_important_text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            whyImportantContent.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT));
        } else {
            whyImportantContent.setText(Html.fromHtml(htmlText));
        }

        // 2. Налаштування ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Використовуємо заголовок з ресурсів
            getSupportActionBar().setTitle(getString(R.string.title_why_important));
            getSupportActionBar().setElevation(0f);
        }

        // 3. Обробка прокручування для відображення/приховування кнопки "Нагору"
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = scrollView.getScrollY();

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

    // 6. Залишаємо для узгодження з InstructionsActivity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}