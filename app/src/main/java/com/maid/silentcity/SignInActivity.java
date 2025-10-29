package com.maid.silentcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

// --- ВИПРАВЛЕНО: ДОДАНО НЕОБХІДНИЙ ІМПОРТ TASK ---
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "SignInActivity";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        // ВАШ ПРАВИЛЬНИЙ WEB CLIENT ID ВСТАВЛЕНО
        String webClientId = "381137546739-hps238rkv71m3iclch0gbahvvn3bvr07.apps.googleusercontent.com";

        // ПЕРЕВІРКА, ЩО БЛОКУВАЛА ВХІД, ВИДАЛЕНА!

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // КРИТИЧНО для Firebase Auth
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button signInButton = findViewById(R.id.btn_sign_in);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // --- ТИМЧАСОВА ПЕРЕВІРКА ПЕРЕД ВХОДОМ ---
            if (account.getIdToken() == null) {
                Toast.makeText(this, "Помилка: Не вдалося отримати ID Token від Google.", Toast.LENGTH_LONG).show();
                // Ця помилка означає, що Web Client ID все ще неправильний
                return;
            }
            // --- КІНЕЦЬ ТИМЧАСОВОЇ ПЕРЕВІРКИ ---

            // 3. КРИТИЧНО: ВИКЛИК Firebase Auth для створення FirebaseUser
            firebaseAuthWithGoogle(account.getIdToken());

        } catch (ApiException e) {
            // ... Помилка 12500 виникає тут
            Log.w(TAG, "Google sign in failed: " + e.getStatusCode(), e);
            Toast.makeText(this, "Помилка входу Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth successful");
                        Toast.makeText(SignInActivity.this, "Вхід успішний! Дані синхронізовано.", Toast.LENGTH_SHORT).show();

                        // Перенаправляємо на головну Activity
                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.w(TAG, "Firebase Auth failed", task.getException());
                        Toast.makeText(SignInActivity.this, "Помилка автентифікації Firebase.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}