package com.maid.silentcity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity; // Або ваш базовий клас

public class StatisticsActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Silent city");
        }

        NavigationHelper.setupBottomNavigation(this);
    }
}