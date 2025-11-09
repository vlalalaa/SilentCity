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
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View; // NEW
import android.widget.AdapterView; // NEW
import android.widget.ArrayAdapter; // NEW
import android.widget.Button;
import android.widget.Spinner; // NEW
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

import java.util.Locale;

// Реалізуємо OnItemSelectedListener для Spinner
public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener, AdapterView.OnItemSelectedListener {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private ActivityResultLauncher<Intent> noiseRecordingLauncher;
    private static final String TAG = "MainActivity";

    private FusedLocationProviderClient fusedLocationClient;
    private TextView noiseAdviceTextView;
    private static final double ADVICE_RADIUS_METERS = 10.0;
    private Location lastKnownLocation;

    private DatabaseReference noiseEntriesRef;
    private FirebaseAuth mAuth;
    private String currentAuthorEmail = "anonymous@example.com";

    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final int DELETE_REQUEST_CODE = 101;

    // --- ПОЛЯ ДЛЯ ЧАСОВОЇ ФІЛЬТРАЦІЇ ---
    private Handler handler = new Handler();
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 300000; // 5 хвилин (300000 мс)
    private Spinner timeRangeSpinner; // НОВЕ ПОЛЕ
    private int selectedTimeRangeIndex = 0; // Індекс обраного проміжку (0 - Поточний час)
    private boolean isSpinnerInitialized = false; // Флаг для запобігання першому автоматичному виклику
    // --- КІНЕЦЬ ПОЛІВ ДЛЯ ЧАСОВОЇ ФІЛЬТРАЦІЇ ---


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Silent city");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        noiseAdviceTextView = findViewById(R.id.noise_advice_textview);
        timeRangeSpinner = findViewById(R.id.time_range_spinner); // НОВА ІНІЦІАЛІЗАЦІЯ

        mAuth = FirebaseAuth.getInstance();
        initializeFirebase();
        initializeMap();
        initializeNoiseRecordingLauncher();
        initializeTimeRangeSpinner(); // НОВИЙ МЕТОД ДЛЯ SPINNER

        Button addDataButton = findViewById(R.id.add_data_button);
        if (addDataButton != null) {
            addDataButton.setOnClickListener(v -> launchNoiseRecording());
        }

        NavigationHelper.setupBottomNavigation(this);
        setupAuthListener();

        // Ініціалізація Runnable для періодичного оновлення карти
        refreshRunnable = new Runnable() {
            public void run() {
                // Викликаємо оновлення, тільки якщо вибрано "Поточний час" (індекс 0)
                if (selectedTimeRangeIndex == 0) {
                    loadNoiseMarkers();
                }
                // Плануємо наступний запуск незалежно від вибору
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
    }

    // --- НОВИЙ МЕТОД: Ініціалізація Spinner ---
    private void initializeTimeRangeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.time_ranges_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeRangeSpinner.setAdapter(adapter);
        timeRangeSpinner.setOnItemSelectedListener(this);
        // За замовчуванням залишаємо індекс 0 ("Поточний час")
        timeRangeSpinner.setSelection(0);
    }

    // --- Імплементація OnItemSelectedListener ---
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Запобігаємо виклику при первинній ініціалізації
        if (!isSpinnerInitialized) {
            isSpinnerInitialized = true;
            return;
        }

        selectedTimeRangeIndex = position;

        // Якщо вибрано "Поточний час" (індекс 0), ми дозволимо Handler оновлювати карту.
        // Якщо вибрано інший проміжок, ми зупиняємо Handler і викликаємо оновлення один раз.
        if (position == 0) {
            handler.post(refreshRunnable); // Відновлюємо періодичне оновлення
        } else {
            handler.removeCallbacks(refreshRunnable); // Зупиняємо періодичне оновлення
        }

        // Завантажуємо дані з новим фільтром
        loadNoiseMarkers();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Нічого не робимо
    }

    private void setupAuthListener() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    if (user.getEmail() != null) {
                        currentAuthorEmail = user.getEmail();
                    }
                    Log.d(TAG, "User is signed in: " + currentAuthorEmail);

                    if (mMap != null) {
                        loadNoiseMarkers();
                        getLastLocationAndGetAdvice();
                    }
                } else {
                    currentAuthorEmail = "anonymous@example.com";
                    Log.d(TAG, "User is signed out.");
                    if (mMap != null) {
                        mMap.clear();
                    }
                    noiseAdviceTextView.setText("Будь ласка, увійдіть, щоб отримати пораду щодо шуму.");
                }
            }
        };
    }


    private void initializeFirebase() {
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

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user == null) {
                                Toast.makeText(this, "Помилка збереження: Користувач не автентифікований. Спробуйте увійти знову.", Toast.LENGTH_LONG).show();
                                return;
                            }

                            saveNoiseEntryToFirebase(cause, avgNoise, maxNoise, minNoise, lat, lon, timestamp, authorEmail);
                            getLastLocationAndGetAdvice();
                            loadNoiseMarkers(); // Оновлення карти після додавання нових даних

                        } else {
                            Toast.makeText(this, "Помилка: не вдалося отримати координати. Мітку не додано.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Запис шуму скасовано.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchNoiseRecording() {
        Intent intent = new Intent(this, RecordNoiseActivity.class);
        intent.putExtra("AUTHOR_EMAIL", currentAuthorEmail);
        noiseRecordingLauncher.launch(intent);
    }

    private void saveNoiseEntryToFirebase(String cause, String avgNoise, String maxNoise, String minNoise, double lat, double lon, long timestamp, String authorEmail) {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Помилка збереження: Користувач не автентифікований Firebase.");
            Toast.makeText(this, "Помилка збереження: Користувач не автентифікований. Спробуйте увійти знову.", Toast.LENGTH_LONG).show();
            return;
        }

        String finalAuthorEmail = user.getEmail() != null ? user.getEmail() : authorEmail;

        NoiseEntry newEntry = new NoiseEntry(cause, avgNoise, maxNoise, minNoise, lat, lon, timestamp, finalAuthorEmail);

        String key = noiseEntriesRef.push().getKey();
        if (key != null) {
            noiseEntriesRef.child(key).setValue(newEntry)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Дані збережено. Мітку додано!", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase Save Error: " + e.getMessage());
                        Toast.makeText(this, "Помилка збереження: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void addNoiseMarker(NoiseEntry entry) {
        if (mMap != null) {
            LatLng position = new LatLng(entry.getLatitude(), entry.getLongitude());

            String markerSnippetData = String.format(Locale.US, "%s|%s|%s|%s|%.6f|%.6f|%d|%s",
                    entry.getCause(), entry.getAvgNoise(), entry.getMaxNoise(), entry.getMinNoise(),
                    position.latitude, position.longitude, entry.getTimestamp(), entry.getAuthorEmail());

            BitmapDescriptor icon = null;
            try {
                String avgNoiseString = entry.getAvgNoise()
                        .replaceAll("[^0-9.,]", "")
                        .trim()
                        .replace(',', '.');

                double avgNoise = Double.parseDouble(avgNoiseString);

                if (avgNoise >= 0 && avgNoise <= 45) {
                    icon = getMarkerIconFromDrawable(R.drawable.marker_green_small);
                } else if (avgNoise > 45 && avgNoise <= 75) {
                    icon = getMarkerIconFromDrawable(R.drawable.marker_orange_medium);
                } else if (avgNoise > 75) {
                    icon = getMarkerIconFromDrawable(R.drawable.marker_red_large);
                } else {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Помилка парсингу рівня шуму після очищення: " + entry.getAvgNoise(), e);
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
            }

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title("Шум: " + entry.getAvgNoise() + " дБ")
                    .snippet(markerSnippetData);

            if (icon != null) {
                markerOptions.icon(icon);
            }

            Marker marker = mMap.addMarker(markerOptions);

            if (marker != null) {
                marker.setTag(entry.getFirebaseKey());
            }
        }
    }

    private BitmapDescriptor getMarkerIconFromDrawable(int drawableResId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableResId);
        if (drawable != null) {
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

    // *** МОДИФІКОВАНО: Впроваджено фільтрацію за обраним часовим проміжком ***
    private void loadNoiseMarkers() {
        if (mMap == null) return;

        // 1. Визначаємо часовий проміжок для фільтрації на основі вибору Spinner
        final TimeUtils.TimeRange selectedRange = TimeUtils.getTimeRangeByIndex(selectedTimeRangeIndex);

        // 2. Використовуємо addListenerForSingleValueEvent
        noiseEntriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap != null) {
                    mMap.clear();
                }

                for (DataSnapshot postSnapshot: snapshot.getChildren()) {
                    NoiseEntry entry = postSnapshot.getValue(NoiseEntry.class);
                    if (entry != null) {

                        // *** ЗАСТОСУВАННЯ ФІЛЬТРАЦІЇ ***
                        if (!TimeUtils.isTimestampInSelectedRange(entry.getTimestamp(), selectedRange)) {
                            // Якщо дані не належать до обраного проміжку, пропускаємо їх.
                            continue;
                        }

                        entry.setFirebaseKey(postSnapshot.getKey());
                        addNoiseMarker(entry);
                    }
                }

                // Оновлення поради (можливо, варто переглянути, чи потрібна порада на основі фільтрованих даних)
                getLastLocationAndGetAdvice();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Помилка завантаження даних: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Помилка завантаження даних: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- ЛОГІКА ПОРАД ЩОДО ШУМУ (без змін) ---
    private double parseNoiseLevel(String noiseString) {
        try {
            String cleanedString = noiseString
                    .replaceAll("[^0-9.,]", "")
                    .trim()
                    .replace(',', '.');

            return Double.parseDouble(cleanedString);
        } catch (Exception e) {
            Log.e(TAG, "Помилка парсингу рівня шуму: " + noiseString, e);
            return -1.0;
        }
    }

    private void getLastLocationAndGetAdvice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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
        noiseAdviceTextView.setText("Аналізую дані в радіусі " + (int)ADVICE_RADIUS_METERS + " метрів...");

        noiseEntriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int greenCount = 0;
                int orangeCount = 0;
                int redCount = 0;
                int totalCount = 0;

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    NoiseEntry entry = postSnapshot.getValue(NoiseEntry.class);
                    if (entry != null) {

                        // Додаємо фільтрацію, щоб порада відповідала тому, що бачить користувач на карті
                        final TimeUtils.TimeRange currentAdviceRange = TimeUtils.getTimeRangeByIndex(selectedTimeRangeIndex);
                        if (!TimeUtils.isTimestampInSelectedRange(entry.getTimestamp(), currentAdviceRange)) {
                            continue;
                        }
                        // Кінець фільтрації

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

                String advice;

                if (totalCount < 2) {
                    advice = "Нерозвинена зона. Карта шуму в цьому районі майже порожня. Ваші дані є важливими!";
                } else {
                    int maxCount = Math.max(greenCount, Math.max(orangeCount, redCount));

                    boolean isTie = (greenCount == maxCount && greenCount > 0 ? 1 : 0) +
                            (orangeCount == maxCount && orangeCount > 0 ? 1 : 0) +
                            (redCount == maxCount && redCount > 0 ? 1 : 0) >= 2;

                    if (isTie) {
                        advice = "Змінна атмосфера. Цей район непередбачуваний: тут буває і тихо, і небезпечно гучно. Будьте уважні!";
                    } else if (greenCount == maxCount) {
                        advice = "Вітаємо! Ви у зоні акустичного комфорту. Ваш слух у безпеці, насолоджуйтесь тишею! 🟢";
                    } else if (orangeCount == maxCount) {
                        advice = "Обережно, помірний шум. Ви в галасливому куточку міста. Варто потурбуватися про захист вух. 🟠";
                    } else if (redCount == maxCount) {
                        advice = "Критичне шумове забруднення! 🔴 Цей рівень є небезпечним. Захистіть свій слух або змініть місцезнаходження.";
                    } else {
                        advice = "Аналіз завершено. Знайдено міток: " + totalCount + ".";
                    }
                }

                noiseAdviceTextView.setText(advice);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Помилка запиту даних для поради: " + error.getMessage());
                noiseAdviceTextView.setText("Помилка завантаження даних для поради.");
            }
        });
    }

    // --- КАРТА ТА ГЕОЛОКАЦІЯ (без змін, окрім onResume/onPause) ---

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

        if (mAuth.getCurrentUser() != null) {
            loadNoiseMarkers();
        }
        checkLocationPermission();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        String firebaseKey = (String) marker.getTag();

        if (firebaseKey != null && !firebaseKey.isEmpty()) {
            String snippet = marker.getSnippet();
            if (snippet != null) {
                String[] data = snippet.split("\\|");
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
                        intent.putExtra("FIREBASE_KEY", firebaseKey);

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
        // ...
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
                getLastLocationAndGetAdvice();
            } else {
                Toast.makeText(this, "Доступ до геолокації відхилено.", Toast.LENGTH_LONG).show();
                noiseAdviceTextView.setText("Для поради потрібен доступ до геолокації.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getBooleanExtra("IS_DELETED", false)) {
                loadNoiseMarkers();
                getLastLocationAndGetAdvice();
            }
        }
    }

    // --- ЛОГІКА ВХОДУ ТА МЕНЮ (модифіковано onResume/onPause) ---

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuthListener != null) {
            mAuth.addAuthStateListener(mAuthListener);
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            startActivity(new Intent(this, SignInActivity.class));
        } else {
            if (account.getEmail() != null) {
                currentAuthorEmail = account.getEmail();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Запуск періодичного оновлення, тільки якщо вибрано "Поточний час"
        if (mAuth.getCurrentUser() != null && selectedTimeRangeIndex == 0) {
            handler.post(refreshRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Зупинка періодичного оновлення
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
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