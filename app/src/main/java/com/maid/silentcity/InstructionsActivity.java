package com.maid.silentcity;

import androidx.appcompat.app.AppCompatActivity; // Змінено на AppCompatActivity для кращої сумісності
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView; // Додано для роботи з TextView

public class InstructionsActivity extends AppCompatActivity { // Змінено на AppCompatActivity

    private ScrollView scrollView;
    private ImageButton btnScrollToTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);

        scrollView = findViewById(R.id.scrollView);
        btnScrollToTop = findViewById(R.id.btnScrollToTop);
        TextView instructionContent = findViewById(R.id.instructionContent); // Отримання TextView

        // 1. Застосування HTML-форматування
        String htmlText = getString(R.string.instruction_text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            instructionContent.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT));
        } else {
            instructionContent.setText(Html.fromHtml(htmlText));
        }

        // 2. Налаштування ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Використовуємо заголовок з ресурсів
            getSupportActionBar().setTitle(getString(R.string.title_instruction));
        }

        // 3. Обробка прокручування
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = scrollView.getScrollY();

            if (scrollY > 300) {
                if (btnScrollToTop.getVisibility() == View.GONE) {
                    btnScrollToTop.setVisibility(View.VISIBLE);
                }
            } else {
                if (btnScrollToTop.getVisibility() == View.VISIBLE) {
                    btnScrollToTop.setVisibility(View.GONE);
                }
            }
        });

        btnScrollToTop.setOnClickListener(v -> {
            scrollView.smoothScrollTo(0, 0);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Залишаємо true або false в залежності від того, чи хочете ви відображати меню.
        // Оскільки в WhyImportantActivity ви залишили return true, залишимо тут true.
        return true;
    }
}