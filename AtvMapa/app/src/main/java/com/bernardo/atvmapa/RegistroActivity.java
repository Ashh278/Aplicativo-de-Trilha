package com.bernardo.atvmapa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RegistroActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button startTrackButton;
    private Button saveTrackButton;

    private long startTimeMillis;
    private TextView speedTextView;
    private TextView timerTextView;
    private TextView distanceTextView;
    private Polyline trailPolyline;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Handler timerHandler = new Handler();
    private long startTime = 0L;
    private double totalDistance = 0.0;
    private Location lastLocation = null;
    private List<LatLng> pathPoints = new ArrayList<>();
    private List<Long> timestamps = new ArrayList<>();

    private boolean tracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializar o cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar o mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicializar os botões e os TextViews
        startTrackButton = findViewById(R.id.start_track_button);
        saveTrackButton = findViewById(R.id.save_track_button);
        speedTextView = findViewById(R.id.text_view_speed);
        timerTextView = findViewById(R.id.text_view_timer);
        distanceTextView = findViewById(R.id.text_view_distance);

        // Definir o listener de clique para o botão "Iniciar Trilha"
        startTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!tracking) {
                    startTracking();
                } else {
                    stopTracking();
                }
            }
        });

        // Definir o listener de clique para o botão "Salvar Trilha"
        saveTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTrack();
            }
        });

        // Callback para atualizações de localização
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null && mMap != null && tracking) {
                        updateLocation(location);
                    }
                }
            }
        };
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Verificar permissão de localização
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Habilitar a localização do usuário no mapa
    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    // Iniciar a trilha
    private void startTracking() {
        tracking = true;
        startTrackButton.setText("Parar Trilha");
        startTimeMillis = System.currentTimeMillis();
        startTime = SystemClock.elapsedRealtime();
        totalDistance = 0.0;
        lastLocation = null;
        pathPoints.clear();
        timestamps.clear();
        if (trailPolyline != null) {
            trailPolyline.remove();
        }
        startLocationUpdates();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopTracking() {
        tracking = false;
        startTrackButton.setText("Iniciar Trilha");
        stopLocationUpdates();
        timerHandler.removeCallbacks(timerRunnable);

        // Calculando a velocidade média
        long elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis; // Tempo decorrido em milissegundos
        double elapsedTimeInHours = elapsedTimeMillis / (1000.0 * 60 * 60); // Convertendo para horas
        double averageSpeed = totalDistance / elapsedTimeInHours; // Calculando velocidade média em m/h

        // Exibindo todas as informações
        StringBuilder trackInfo = new StringBuilder();
        trackInfo.append("Distância percorrida: ").append(String.format("%.2f m", totalDistance)).append("\n");
        trackInfo.append("Duração da trilha: ").append(formatElapsedTime(elapsedTimeMillis)).append("\n");
        trackInfo.append("Velocidade média: ").append(String.format("%.2f m/h", averageSpeed)).append("\n");

        Toast.makeText(this, trackInfo.toString(), Toast.LENGTH_LONG).show();
    }

    // Método auxiliar para formatar o tempo decorrido em formato legível
    private String formatElapsedTime(long millis) {
        int seconds = (int) (millis / 1000);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Iniciar atualizações de localização
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // Intervalo de atualização de 5 segundos
        locationRequest.setFastestInterval(2000); // Intervalo mais rápido de 2 segundos
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    // Parar atualizações de localização
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    // Atualizar a localização do usuário e calcular velocidade e distância
    private void updateLocation(Location location) {
        if (lastLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    location.getLatitude(), location.getLongitude(),
                    results);
            totalDistance += results[0];
            distanceTextView.setText(String.format("Distância: %.2f m", totalDistance));

            float speed = location.getSpeed(); // m/s
            speedTextView.setText(String.format("Velocidade: %.2f m/s", speed));

            // Adicionar o ponto atual à lista de pontos da trilha
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            pathPoints.add(currentLatLng);
            timestamps.add(location.getTime());

            // Atualizar a Polyline com os novos pontos
            if (trailPolyline != null) {
                trailPolyline.remove();
            }
            trailPolyline = mMap.addPolyline(new PolylineOptions().addAll(pathPoints).color(0xFF0000FF).width(10)); // Azul

            // Mover a câmera para o novo local
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
        }
        lastLocation = location;
    }

    // Runnable para atualizar o cronômetro
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextView.setText(String.format("Tempo: %02d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };

    // Salvar a trilha em um arquivo de texto no armazenamento externo
    private void saveTrack() {
        if (pathPoints.isEmpty()) {
            Toast.makeText(this, "Nenhuma trilha para salvar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculando a duração da trilha
        long elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;
        String duration = formatElapsedTime(elapsedTimeMillis);

        // Calculando a velocidade média
        double elapsedTimeInHours = elapsedTimeMillis / (1000.0 * 60 * 60);
        double averageSpeed = totalDistance / elapsedTimeInHours;

        StringBuilder trackData = new StringBuilder();
        trackData.append("Data e Hora: ").append(getCurrentDateAndTime()).append("\n");
        trackData.append("Distância Percorrida: ").append(String.format("%.2f m", totalDistance)).append("\n");
        trackData.append("Duração da Trilha: ").append(duration).append("\n");
        trackData.append("Velocidade Média: ").append(String.format("%.2f m/h", averageSpeed)).append("\n\n");

        // Adicionando coordenadas e timestamps
        for (int i = 0; i < pathPoints.size(); i++) {
            LatLng point = pathPoints.get(i);
            long timestamp = timestamps.get(i);
            trackData.append(point.latitude).append(",").append(point.longitude).append(",").append(timestamp).append("\n");
        }

        // Nome do arquivo com data e hora
        String fileName = "trilha_" + getCurrentDateAndTime().replace("/", "_").replace(" ", "_").replace(":", "_") + ".txt";

        File trackFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
        try (FileOutputStream fos = new FileOutputStream(trackFile)) {
            fos.write(trackData.toString().getBytes());
            Toast.makeText(this, "Trilha salva em " + trackFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao salvar a trilha.", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para obter a data e hora atual
    private String getCurrentDateAndTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");
        return sdf.format(new Date());
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Permissão de localização não concedida", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
