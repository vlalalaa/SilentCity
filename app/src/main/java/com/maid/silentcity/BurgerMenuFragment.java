package com.maid.silentcity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

// BurgerMenuFragment повинен успадковувати DialogFragment
public class BurgerMenuFragment extends DialogFragment {

    // 1. Створення DialogFragment без заголовка і з прозорим фоном
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // 2. Інфлейт (завантаження) XML-макету
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Завантажуємо наш XML-макет: res/layout/fragment_burger_menu.xml
        return inflater.inflate(R.layout.fragment_burger_menu, container, false);
    }

    // 3. Обробка подій та ініціалізація елементів
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Обробка кнопки ЗАКРИТИ
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dismiss());

//        LinearLayout llProfile = view.findViewById(R.id.llProfile);
//        llProfile.setOnClickListener(v -> {
//            Toast.makeText(getContext(), "Перехід до профілю", Toast.LENGTH_SHORT).show();
//            dismiss();
//        });

//        TextView tvStatistics = view.findViewById(R.id.tvStatistics);
//        tvStatistics.setOnClickListener(v -> {
//            Toast.makeText(getContext(), "Статистика", Toast.LENGTH_SHORT).show();
//            dismiss();
//        });

        TextView tvInstructions = view.findViewById(R.id.tvInstructions);
        tvInstructions.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), InstructionsActivity.class);
            startActivity(intent);
            dismiss();
        });

        TextView tvWhyImportant = view.findViewById(R.id.tvWhyImportant); // ⚠️ Переконайтеся, що ID правильний!
        tvWhyImportant.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), WhyImportantActivity.class);
            startActivity(intent);
            dismiss();
        });

    }

    // 4. Налаштування розмірів і розташування вікна (КЛЮЧОВИЙ МОМЕНТ)
    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            // Розташовує діалог у верхньому правому куті
            window.setGravity(Gravity.TOP | Gravity.END);

            // Налаштовуємо ширину/висоту відповідно до вмісту
            window.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            // Додаємо відступи (якщо ви створили dimen-ресурси, як було описано раніше)
            int marginRight = getResources().getDimensionPixelSize(R.dimen.menu_margin_right);
            int marginTop = getResources().getDimensionPixelSize(R.dimen.menu_margin_top);

            window.getAttributes().x = marginRight;
            window.getAttributes().y = marginTop;
        }
    }
}