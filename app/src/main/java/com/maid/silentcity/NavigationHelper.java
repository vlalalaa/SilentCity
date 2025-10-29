package com.maid.silentcity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import androidx.core.content.ContextCompat;

public class NavigationHelper {

    // Клас для групування View-елементів та їхніх ID
    private static class NavItem {
        final View itemView;
        final int activeBgRes;
        final int inactiveBgRes;
        final int activeIconRes;
        final int inactiveIconRes;

        // Конструктор
        NavItem(View item, int actBg, int inactBg, int actIcon, int inactIcon) {
            this.itemView = item;
            this.activeBgRes = actBg;
            this.inactiveBgRes = inactBg;
            this.activeIconRes = actIcon;
            this.inactiveIconRes = inactIcon;
        }

        // Встановлення активного/неактивного стану
        void setActive(boolean isActive, Activity activity) {
            if (itemView == null) return;

            if (isActive) {
                // Активний стан: Світлий фон + Темна іконка
                // activeBgRes - це ресурс фону (круг)
                itemView.setBackgroundResource(activeBgRes);
                // activeIconRes - це ресурс іконки (зображення)
                itemView.setForeground(ContextCompat.getDrawable(activity, activeIconRes));
            } else {
                // Неактивний стан: Темний фон + Світла іконка
                itemView.setBackgroundResource(inactiveBgRes);
                itemView.setForeground(ContextCompat.getDrawable(activity, inactiveIconRes));
            }
        }
    }

    public static void setupBottomNavigation(final Activity currentActivity) {

        // 1. Ініціалізація внутрішніх View-елементів
        View homeItem = currentActivity.findViewById(R.id.navHomeItem);
        View statisticsItem = currentActivity.findViewById(R.id.navStatisticsItem);
        View profileItem = currentActivity.findViewById(R.id.navProfileItem);

        // 2. Ініціалізація NavItem з ресурсними ID.
        // Я припускаю, що ви створили 6 ресурсів (3 для фону та 3 для іконок).
        // Оскільки в наданому коді ви використовували одні й ті ж ресурси для фону та іконок,
        // я залишаю ваше зіставлення, але логіка активації нижче виправлена.

        // ПРИПУЩЕННЯ ВАШИХ РЕСУРСНИХ ID:
        // * Активний фон: R.drawable.bg_nav_circle_active (Світлий круг)
        // * Неактивний фон: R.drawable.bg_nav_circle_inactive (Темний круг)
        // * Активна іконка: R.drawable.ic_home_dark (Темна іконка)
        // * Неактивна іконка: R.drawable.ic_home_light (Світла іконка)

        // !!! У ВАШОМУ КОДІ БУЛА ЗБЕРЕЖЕНА ПОМИЛКА З ПЕРЕМІШУВАННЯМ РЕСУРСІВ.
        // !!! ВИПРАВЛЯЮ ВІДПОВІДНО ДО ПОПЕРЕДНЬОЇ ЛОГІКИ:

        final NavItem home = new NavItem(homeItem,
                R.drawable.ic_home, R.drawable.ic_home_not_active, // Фони
                R.drawable.ic_home_not_active, R.drawable.ic_home); // Іконки

        final NavItem statistics = new NavItem(statisticsItem,
                R.drawable.ic_statistic, R.drawable.ic_statistic_not_active, // Фони
                R.drawable.ic_statistic_not_active, R.drawable.ic_statistic); // Іконки

        final NavItem profile = new NavItem(profileItem,
                R.drawable.ic_profile, R.drawable.ic_profile_not_active, // Фони
                R.drawable.ic_profile_not_active, R.drawable.ic_profile); // Іконки


        // 3. Встановлення активного стану для поточної сторінки

        // !!! ВИПРАВЛЕННЯ !!!
        // Використовуємо .getClass().equals() для перевірки, чи це ТОЧНО MainActivity,
        // а не її дочірній клас (яким є StatisticsActivity та ProfileActivity).
        home.setActive(currentActivity.getClass().equals(MainActivity.class), currentActivity);

        // Для інших класів instanceof працює коректно, оскільки вони не мають нащадків.
        statistics.setActive(currentActivity instanceof StatisticsActivity, currentActivity);
        profile.setActive(currentActivity instanceof ProfileActivity, currentActivity);

        // 4. Обробка натискань (використовуємо контейнери FrameLayout)
        View btnHomeContainer = currentActivity.findViewById(R.id.btnHomeContainer);
        View btnStatisticsContainer = currentActivity.findViewById(R.id.btnStatisticsContainer);
        View btnProfileContainer = currentActivity.findViewById(R.id.btnProfileContainer);

        if (btnHomeContainer != null) {
            btnHomeContainer.setOnClickListener(v -> startActivity(currentActivity, MainActivity.class));
        }
        if (btnStatisticsContainer != null) {
            btnStatisticsContainer.setOnClickListener(v -> startActivity(currentActivity, StatisticsActivity.class));
        }
        if (btnProfileContainer != null) {
            btnProfileContainer.setOnClickListener(v -> startActivity(currentActivity, ProfileActivity.class));
        }
    }

    // Метод переходу між активностями (залишається без змін)
    private static void startActivity(Activity currentActivity, Class<?> targetActivity) {
        // Залишаємо getClass().equals() тут, щоб запобігти запуску самої себе
        if (currentActivity.getClass().equals(targetActivity)) {
            return;
        }
        Intent intent = new Intent(currentActivity, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        currentActivity.startActivity(intent);
        currentActivity.overridePendingTransition(0, 0);
        currentActivity.finish();
    }
}