package com.maid.silentcity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;

public class InstructionsActivity extends MainActivity {

    private ScrollView scrollView;
    private ImageButton btnScrollToTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);

        scrollView = findViewById(R.id.scrollView);
        btnScrollToTop = findViewById(R.id.btnScrollToTop);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Повернутись");
        }

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
        return true;
    }

}