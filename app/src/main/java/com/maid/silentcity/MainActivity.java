package com.maid.silentcity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private ActivityResultLauncher<Intent> noiseRecordingLauncher;
    private static final String TAG = "MainActivity";

    // --- ПОЛЯ FIREBASE ---
    private DatabaseReference noiseEntriesRef;
    private FirebaseAuth mAuth;
    private String currentAuthorEmail = "anonymous@example.com";

    // Прослуховувач стану автентифікації
    private FirebaseAuth.AuthStateListener mAuthListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Silent city");
        }

        mAuth = FirebaseAuth.getInstance();
        initializeFirebase();
        initializeMap();
        initializeNoiseRecordingLauncher();

        Button addDataButton = findViewById(R.id.add_data_button);
        if (addDataButton != null) {
            // Використовуємо вашу оригінальну логіку запуску
            addDataButton.setOnClickListener(v -> launchNoiseRecording());
        }

        NavigationHelper.setupBottomNavigation(this);

        // Налаштування AuthStateListener
        setupAuthListener();
    }

    private void setupAuthListener() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // КОРИСТУВАЧ УВІЙШОВ У FIREBASE - Встановлюємо email та завантажуємо дані.
                    if (user.getEmail() != null) {
                        currentAuthorEmail = user.getEmail();
                    }
                    Log.d(TAG, "User is signed in: " + currentAuthorEmail);
                    // Тільки тут безпечно завантажувати мітки
                    if (mMap != null) {
                        loadNoiseMarkers();
                    } else {
                        // Якщо карта ще не готова, loadNoiseMarkers() буде викликано з onMapReady()
                    }
                } else {
                    // КОРИСТУВАЧ НЕ УВІЙШОВ У FIREBASE
                    currentAuthorEmail = "anonymous@example.com";
                    Log.d(TAG, "User is signed out.");
                    if (mMap != null) {
                        mMap.clear();
                    }
                }
            }
        };
    }


    private void initializeFirebase() {
        // Ініціалізуємо посилання на базу даних
        noiseEntriesRef = FirebaseDatabase.getInstance().getReference("noise_entries");
    }

    // --- ЛОГІКА ЗАПУСКУ ТА РЕЗУЛЬТАТІВ ШУМУ ---

    private void initializeNoiseRecordingLauncher() {
        noiseRecordingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        double lat = data.getDoubleExtra("LATITUDE", 0.0);
                        double lon = data.getDoubleExtra("LONGITUDE", 0.0);
                        String avgNoise = data.getStringExtra("AVG_NOISE");
                        String maxNoise = data.getStringExtra("MAX_NOISE");
                        String minNoise = data.getStringExtra("MIN_NOISE");
                        String cause = data.getStringExtra("CAUSE");
                        long timestamp = data.getLongExtra("TIMESTAMP", System.currentTimeMillis());
                        String authorEmail = data.getStringExtra("AUTHOR_EMAIL");

                        if (lat != 0.0 || lon != 0.0) {

                            // КРИТИЧНО: ДОДАНА ПЕРЕВІРКА ПЕРЕД ЗБЕРЕЖЕННЯМ
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user == null) {
                                // Якщо користувач не автентифікований у Firebase, показуємо помилку
                                Toast.makeText(this, "Помилка збереження: Користувач не автентифікований. Спробуйте увійти знову.", Toast.LENGTH_LONG).show();
                                return; // Вихід без спроби збереження
                            }

                            // ЗБЕРЕЖЕННЯ У FIREBASE
                            saveNoiseEntryToFirebase(cause, avgNoise, maxNoise, minNoise, lat, lon, timestamp, authorEmail);

                        } else {
                            Toast.makeText(this, "Помилка: не вдалося отримати координати. Мітку не додано.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Запис шуму скасовано.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchNoiseRecording() {
        // ВИДАЛЕНО: Перевірка mAuth.getCurrentUser() == null, яка викликала проблему

        Intent intent = new Intent(this, RecordNoiseActivity.class);
        intent.putExtra("AUTHOR_EMAIL", currentAuthorEmail);
        noiseRecordingLauncher.launch(intent);
    }

    private void saveNoiseEntryToFirebase(String cause, String avgNoise, String maxNoise, String minNoise, double lat, double lon, long timestamp, String authorEmail) {

        // КРИТИЧНО: ЯВНА ПЕРЕВІРКА АВТЕНТИФІКАЦІЇ ПЕРЕД ЗАПИСОМ
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // Якщо користувач не автентифікований у Firebase, ми не можемо записати дані
            Log.e(TAG, "Помилка збереження: Користувач не автентифікований Firebase.");
            Toast.makeText(this, "Помилка збереження: Користувач не автентифікований. Спробуйте увійти знову.", Toast.LENGTH_LONG).show();
            // Опціонально: Перенаправити на SignInActivity
            // startActivity(new Intent(this, SignInActivity.class));
            return;
        }

        // Якщо користувач є, використовуємо його email для більшої надійності,
        // хоча ви його вже передаєте з intent
        String finalAuthorEmail = user.getEmail() != null ? user.getEmail() : authorEmail;

        NoiseEntry newEntry = new NoiseEntry(cause, avgNoise, maxNoise, minNoise, lat, lon, timestamp, finalAuthorEmail);

        // Створення унікального ключа і збереження
        String key = noiseEntriesRef.push().getKey();
        if (key != null) {
            noiseEntriesRef.child(key).setValue(newEntry)
                    .addOnSuccessListener(aVoid -> {
                        // Мітка буде додана автоматично після оновлення карти з Firebase
                        Toast.makeText(this, "Дані збережено. Мітку додано!", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase Save Error: " + e.getMessage());
                        // Якщо тут все ще "Permission denied", це 100% Rules або Firebase
                        Toast.makeText(this, "Помилка збереження: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void addNoiseMarker(NoiseEntry entry) {
        if (mMap != null) {
            LatLng position = new LatLng(entry.getLatitude(), entry.getLongitude());

            // Складання рядка snippet для передачі всіх даних (включаючи email) у MarkerInfoActivity
            String markerSnippetData = String.format(Locale.US, "%s|%s|%s|%s|%.6f|%.6f|%d|%s",
                    entry.getCause(), entry.getAvgNoise(), entry.getMaxNoise(), entry.getMinNoise(),
                    position.latitude, position.longitude, entry.getTimestamp(), entry.getAuthorEmail());

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Шум: " + entry.getAvgNoise() + " дБ")
                    .snippet(markerSnippetData));

            // Встановлюємо тег для ідентифікації мітки
            if (marker != null) {
                marker.setTag("noise_data");
            }
        }
    }

    // Завантаження всіх міток із Firebase
    private void loadNoiseMarkers() {
        if (mMap == null) return;
        mMap.clear();

        // Тепер ми знаємо, що цей код викликається ТІЛЬКИ після успішної Firebase Auth
        noiseEntriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap != null) {
                    mMap.clear();
                }

                for (DataSnapshot postSnapshot: snapshot.getChildren()) {
                    NoiseEntry entry = postSnapshot.getValue(NoiseEntry.class);
                    if (entry != null) {
                        addNoiseMarker(entry);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Помилка завантаження даних: " + error.getMessage());
                // Якщо тут з'явиться "Permission denied", це вказує на проблему з Rules або SHA-1
                Toast.makeText(MainActivity.this, "Помилка завантаження даних: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- КАРТА ТА ГЕОЛОКАЦІЯ ---

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main_map_fragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);

        LatLng kyiv = new LatLng(50.4501, 30.5234);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kyiv, 10f));

        // Після готовності карти, якщо користувач вже увійшов, завантажуємо мітки
        if (mAuth.getCurrentUser() != null) {
            loadNoiseMarkers();
        }
        checkLocationPermission();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if (marker.getTag() != null && marker.getTag().equals("noise_data")) {
            String snippet = marker.getSnippet();
            if (snippet != null) {
                String[] data = snippet.split("\\|");
                // Очікуємо 8 елементів: Cause, Avg, Max, Min, Lat, Lon, Timestamp, AuthorEmail
                if (data.length == 8) {
                    Intent intent = new Intent(this, MarkerInfoActivity.class);
                    try {
                        intent.putExtra("CAUSE", data[0]);
                        intent.putExtra("AVG_NOISE", data[1]);
                        intent.putExtra("MAX_NOISE", data[2]);
                        intent.putExtra("MIN_NOISE", data[3]);
                        intent.putExtra("LATITUDE", Double.parseDouble(data[4]));
                        intent.putExtra("LONGITUDE", Double.parseDouble(data[5]));
                        intent.putExtra("TIMESTAMP", Long.parseLong(data[6]));
                        intent.putExtra("AUTHOR_EMAIL", data[7]);
                        startActivity(intent);
                        return true;
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Помилка парсингу даних мітки: " + e.getMessage());
                        Toast.makeText(this, "Помилка даних мітки.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        // Залишаємо логіку кліку для ручного додавання, якщо вона потрібна
        // Toast.makeText(this, "Мітка встановлена: " + latLng.latitude, Toast.LENGTH_SHORT).show();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (mMap != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Доступ до геолокації відхилено.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- ЛОГІКА ВХОДУ ТА МЕНЮ ---

    @Override
    protected void onStart() {
        super.onStart();

        // ДОДАНО: Додавання прослуховувача стану автентифікації
        if (mAuthListener != null) {
            mAuth.addAuthStateListener(mAuthListener);
        }

        // Перевірка входу Google (ваш оригінальний робочий код)
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            // Це запустить SignInActivity, якщо користувач не увійшов
            startActivity(new Intent(this, SignInActivity.class));
        } else {
            // Оновлюємо email, якщо користувач успішно увійшов
            if (account.getEmail() != null) {
                currentAuthorEmail = account.getEmail();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Видалення прослуховувача стану автентифікації
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_burger_menu) {
            showBurgerMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showBurgerMenu() {
        BurgerMenuFragment dialogFragment = new BurgerMenuFragment();
        dialogFragment.show(getSupportFragmentManager(), "BurgerMenuDialog");
    }
}