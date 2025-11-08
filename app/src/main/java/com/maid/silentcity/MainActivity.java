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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location; // NEW
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView; // NEW
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.location.FusedLocationProviderClient; // NEW
import com.google.android.gms.location.LocationServices; // NEW
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private ActivityResultLauncher<Intent> noiseRecordingLauncher;
    private static final String TAG = "MainActivity";

    // --- NEW: ПОЛЯ ДЛЯ ГЕОЛОКАЦІЇ ТА ПОРАДИ ---
    private FusedLocationProviderClient fusedLocationClient;
    private TextView noiseAdviceTextView;
    private static final double ADVICE_RADIUS_METERS = 10.0;
    private Location lastKnownLocation;
    // --- КІНЕЦЬ NEW ПОЛІВ ---

    // --- ПОЛЯ FIREBASE ---
    private DatabaseReference noiseEntriesRef;
    private FirebaseAuth mAuth;
    private String currentAuthorEmail = "anonymous@example.com";

    // Прослуховувач стану автентифікації
    private FirebaseAuth.AuthStateListener mAuthListener;

    // NEW: Код запиту для відстеження видалення мітки
    private static final int DELETE_REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Silent city");
        }

        // NEW: Ініціалізація полів
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        noiseAdviceTextView = findViewById(R.id.noise_advice_textview);

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
                        // NEW: Після завантаження міток, отримуємо локацію та пораду
                        getLastLocationAndGetAdvice();
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
                    // NEW: Оновлення поради при виході
                    noiseAdviceTextView.setText("Будь ласка, увійдіть, щоб отримати пораду щодо шуму.");
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

                            // NEW: Оновлення поради після додавання нових даних
                            getLastLocationAndGetAdvice();

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

            // ... (Ваш код формування markerSnippetData залишається без змін) ...
            String markerSnippetData = String.format(Locale.US, "%s|%s|%s|%s|%.6f|%.6f|%d|%s",
                    entry.getCause(), entry.getAvgNoise(), entry.getMaxNoise(), entry.getMinNoise(),
                    position.latitude, position.longitude, entry.getTimestamp(), entry.getAuthorEmail());

            // --- НОВА ЛОГІКА: ВИЗНАЧЕННЯ КОЛЬОРУ ТА РОЗМІРУ МАРКЕРА ---
            BitmapDescriptor icon = null;
            try {
                // 1. Очищуємо рядок шуму, видаляючи все, крім цифр, коми та крапки
                String avgNoiseString = entry.getAvgNoise()
                        .replaceAll("[^0-9.,]", "")
                        .trim();

                // 2. Замінюємо кому на крапку (стандартний десятковий роздільник для Java/Locale.US)
                avgNoiseString = avgNoiseString.replace(',', '.');

                // 3. Намагаємося перетворити очищений рядок на число
                double avgNoise = Double.parseDouble(avgNoiseString);

                if (avgNoise >= 0 && avgNoise <= 45) {
                    icon = getMarkerIconFromDrawable(R.drawable.marker_green_small);
                } else if (avgNoise > 45 && avgNoise <= 75) {
                    icon = getMarkerIconFromDrawable(R.drawable.marker_orange_medium);
                } else if (avgNoise > 75) {
                    icon = getMarkerIconFromDrawable(R.drawable.marker_red_large);
                } else {
                    // Використовуємо стандартний маркер для від'ємних або нульових значень
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
                }
            } catch (NumberFormatException e) {
                // Цей блок спрацює, якщо навіть очищений рядок не є числом (наприклад, порожній рядок)
                Log.e(TAG, "Помилка парсингу рівня шуму після очищення: " + entry.getAvgNoise(), e);
                // Примусово використовуємо стандартний маркер
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
            }
            // --- КІНЕЦЬ НОВОЇ ЛОГІКИ ---

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title("Шум: " + entry.getAvgNoise() + " дБ")
                    .snippet(markerSnippetData);

            if (icon != null) {
                markerOptions.icon(icon); // Встановлюємо динамічну іконку
            }

            Marker marker = mMap.addMarker(markerOptions);

            if (marker != null) {
                // NEW: Використовуємо ключ Firebase як Marker Tag
                marker.setTag(entry.getFirebaseKey());
            }
        }
    }

    // --- НОВИЙ ДОПОМІЖНИЙ МЕТОД: Конвертація Drawable у BitmapDescriptor ---
    private BitmapDescriptor getMarkerIconFromDrawable(int drawableResId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableResId);
        if (drawable != null) {
            // Отримуємо розміри drawable (які ми встановили в XML)
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        }
        return null;
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
                        // NEW: Отримуємо ключ і встановлюємо його в об'єкт
                        entry.setFirebaseKey(postSnapshot.getKey());
                        addNoiseMarker(entry);
                    }
                }

                // NEW: Оновлення поради після завантаження даних
                getLastLocationAndGetAdvice();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Помилка завантаження даних: " + error.getMessage());
                // Якщо тут з'явиться "Permission denied", це вказує на проблему з Rules або SHA-1
                Toast.makeText(MainActivity.this, "Помилка завантаження даних: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- NEW: ЛОГІКА ПОРАД ЩОДО ШУМУ ---

    // Перетворення NoiseEntry.getAvgNoise() на число
    private double parseNoiseLevel(String noiseString) {
        try {
            String cleanedString = noiseString
                    .replaceAll("[^0-9.,]", "")
                    .trim()
                    .replace(',', '.'); // Заміна коми на крапку

            return Double.parseDouble(cleanedString);
        } catch (Exception e) {
            Log.e(TAG, "Помилка парсингу рівня шуму: " + noiseString, e);
            return -1.0; // Повертаємо від'ємне число, якщо парсинг не вдався
        }
    }

    private void getLastLocationAndGetAdvice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Якщо дозволу немає, просимо його. Порада буде отримана в onRequestPermissionsResult.
            checkLocationPermission();
            noiseAdviceTextView.setText("Дозвольте доступ до геолокації, щоб отримати пораду.");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        lastKnownLocation = location;
                        getNoiseAdvice(location);
                    } else {
                        noiseAdviceTextView.setText("Не вдалося визначити ваше розташування. Спробуйте пізніше.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Помилка отримання локації: " + e.getMessage());
                    noiseAdviceTextView.setText("Помилка доступу до геолокації.");
                });
    }

    private void getNoiseAdvice(Location currentLocation) {
        // Оновлюємо текст-заглушку, поки йде запит до Firebase
        noiseAdviceTextView.setText("Аналізую дані в радіусі " + (int)ADVICE_RADIUS_METERS + " метрів...");

        noiseEntriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int greenCount = 0; // <= 45 дБ
                int orangeCount = 0; // 45 < дБ <= 75 дБ
                int redCount = 0; // > 75 дБ
                int totalCount = 0;

                // 1. Збираємо мітки в радіусі 10 метрів
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    NoiseEntry entry = postSnapshot.getValue(NoiseEntry.class);
                    if (entry != null) {

                        Location entryLocation = new Location("");
                        entryLocation.setLatitude(entry.getLatitude());
                        entryLocation.setLongitude(entry.getLongitude());

                        float distance = currentLocation.distanceTo(entryLocation);

                        if (distance <= ADVICE_RADIUS_METERS) {
                            totalCount++;
                            double avgNoise = parseNoiseLevel(entry.getAvgNoise());

                            if (avgNoise > 0) {
                                if (avgNoise <= 45) {
                                    greenCount++;
                                } else if (avgNoise <= 75) {
                                    orangeCount++;
                                } else { // > 75
                                    redCount++;
                                }
                            }
                        }
                    }
                }

                // 2. Визначаємо пораду
                String advice;

                if (totalCount < 2) {
                    // Менше 2 міток
                    advice = "Нерозвинена зона. Карта шуму в цьому районі майже порожня. Ваші дані є важливими!";
                } else {
                    int maxCount = Math.max(greenCount, Math.max(orangeCount, redCount));

                    // Перевіряємо, чи є два або більше лічильників, що дорівнюють максимальному
                    // Наприклад: (зелений=2, помаранчевий=2, червоний=1) -> maxCount=2, isTie=true
                    // Наприклад: (зелений=1, помаранчевий=1, червоний=1) -> maxCount=1, isTie=true
                    boolean isTie = (greenCount == maxCount && greenCount > 0 ? 1 : 0) +
                            (orangeCount == maxCount && orangeCount > 0 ? 1 : 0) +
                            (redCount == maxCount && redCount > 0 ? 1 : 0) >= 2;

                    if (isTie) {
                        // Якщо кількість найбільш поширених міток однакова (наприклад, 2 зелених і 2 помаранчевих)
                        advice = "Змінна атмосфера. Цей район непередбачуваний: тут буває і тихо, і небезпечно гучно. Будьте уважні!";
                    } else if (greenCount == maxCount) {
                        // Найбільше зелених міток
                        advice = "Вітаємо! Ви у зоні акустичного комфорту. Ваш слух у безпеці, насолоджуйтесь тишею! 🟢";
                    } else if (orangeCount == maxCount) {
                        // Найбільше помаранчевих міток
                        advice = "Обережно, помірний шум. Ви в галасливому куточку міста. Варто потурбуватися про захист вух. 🟠";
                    } else if (redCount == maxCount) {
                        // Найбільше червоних міток
                        advice = "Критичне шумове забруднення! 🔴 Цей рівень є небезпечним. Захистіть свій слух або змініть місцезнаходження.";
                    } else {
                        // Запасний варіант (не має відбутися, якщо дані коректні)
                        advice = "Аналіз завершено. Знайдено міток: " + totalCount + ".";
                    }
                }

                // 3. Відображаємо пораду
                noiseAdviceTextView.setText(advice);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Помилка запиту даних для поради: " + error.getMessage());
                noiseAdviceTextView.setText("Помилка завантаження даних для поради.");
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
        // NEW: Отримуємо ключ Firebase з Tag
        String firebaseKey = (String) marker.getTag();

        // Перевіряємо, чи має маркер ключ
        if (firebaseKey != null && !firebaseKey.isEmpty()) {
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

                        // NEW: Передача ключа Firebase
                        intent.putExtra("FIREBASE_KEY", firebaseKey);

                        // NEW: Запуск Activity з очікуванням результату
                        startActivityForResult(intent, DELETE_REQUEST_CODE);
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
            // NEW: Отримуємо пораду одразу після отримання дозволу
            getLastLocationAndGetAdvice();
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
                // NEW: Отримуємо пораду після надання дозволу
                getLastLocationAndGetAdvice();
            } else {
                Toast.makeText(this, "Доступ до геолокації відхилено.", Toast.LENGTH_LONG).show();
                noiseAdviceTextView.setText("Для поради потрібен доступ до геолокації.");
            }
        }
    }

    // --- NEW: Обробка результату видалення з MarkerInfoActivity ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE) {
            // Перевіряємо, чи повернулася активність з результатом "успішно видалено"
            if (resultCode == Activity.RESULT_OK && data != null && data.getBooleanExtra("IS_DELETED", false)) {

                // Успішне видалення з Firebase.
                // Оновлюємо карту, щоб видалена мітка зникла.
                loadNoiseMarkers();

                // NEW: Оновлення поради після видалення даних
                getLastLocationAndGetAdvice();

                // Повідомлення відображається в MarkerInfoActivity, але можна додати додаткове:
                // Toast.makeText(this, "Мітку успішно видалено.", Toast.LENGTH_LONG).show();

                // Статистика оновить дані автоматично, коли користувач відкриє StatisticsActivity.
            } else if (resultCode == Activity.RESULT_OK) {
                // Це означає, що MarkerInfoActivity було просто закрито (без видалення).
                // Якщо loadNoiseMarkers() є у AuthStateListener, карта оновиться самостійно.
            }
        }

        // ВАЖЛИВО: Логіка для noiseRecordingLauncher обробляється через registerForActivityResult
        // і не повинна тут дублюватися, але якщо ви використовували цю функцію,
        // вона буде виглядати приблизно так:
        /*
        else if (requestCode == ACTIVITY_SELECT_CAUSE_REQUEST_CODE) {
             // ...
        }
        */
    }
    // --- END NEW: Обробка результату видалення ---


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