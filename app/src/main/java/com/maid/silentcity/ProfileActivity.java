package com.maid.silentcity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import androidx.annotation.Nullable;


public class ProfileActivity extends MainActivity {

    private ImageView profilePic;
    private TextView userName;
    private Button signOutButton;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Припускаємо, що ваш макет профілю називається activity_profile
        setContentView(R.layout.activity_profile);

        // Ініціалізація UI елементів
        profilePic = findViewById(R.id.iv_profile_pic);
        userName = findViewById(R.id.tv_user_name);
        signOutButton = findViewById(R.id.btn_sign_out);

        // Конфігурація Google Sign-In (потрібна для виходу)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signOutButton.setOnClickListener(v -> signOut());

        // Встановлення нижньої навігації
        NavigationHelper.setupBottomNavigation(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Перевірка наявного акаунта (ЗАПАМ'ЯТОВУВАННЯ СЕСІЇ)
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        checkSignInStatus(account);
    }

    private void checkSignInStatus(GoogleSignInAccount account) {
        if (account != null) {
            // Користувач зареєстрований: відображаємо його дані
            updateUI(account);
        } else {
            // Користувач не зареєстрований: повертаємо його на сторінку входу
            Toast.makeText(this, "Будь ласка, увійдіть.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
            // Очищаємо стек, щоб не можна було повернутися назад кнопкою "Назад"
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    // Функція для виходу з акаунта
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    Toast.makeText(ProfileActivity.this, "Ви вийшли з акаунта.", Toast.LENGTH_SHORT).show();
                    // Перенаправлення на сторінку входу після виходу
                    Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    // Відображення даних профілю
    private void updateUI(GoogleSignInAccount account) {
        // Відображення імені
        String name = account.getDisplayName();
        if (name != null) {
            userName.setText(name);
        } else {
            userName.setText("Ім'я не знайдено");
        }

        // Відображення зображення профілю
        Uri photoUrl = account.getPhotoUrl();
        if (photoUrl != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .circleCrop() // Зробити зображення круглим
                    .into(profilePic);
        } else {
            profilePic.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }
}