package com.hfad.odometer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 698;

    private OdometerService odometer;
    private boolean bound = false;
    private boolean tracking = false;
    private final Handler handler = new Handler();
    private RecorridoDbHelper db;

    private EditText etPrecision, etIntervalo;
    private TextView tvDistance, tvLocation, tvReporte;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder =
                    (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new RecorridoDbHelper(this);
        etPrecision = findViewById(R.id.precision);
        etIntervalo = findViewById(R.id.intervalo);
        tvDistance = findViewById(R.id.distance);
        tvLocation = findViewById(R.id.location);
        tvReporte = findViewById(R.id.reporte);

        findViewById(R.id.btnIniciar).setOnClickListener(v -> iniciar());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> finalizarYGuardar());
        findViewById(R.id.btnReporte).setOnClickListener(v -> tvReporte.setText(db.reporte()));

        displayDistance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            OdometerService.PERMISSION_STRING,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            bindOdometer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE
                && ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING)
                == PackageManager.PERMISSION_GRANTED) {
            bindOdometer();
        } else {
            Toast.makeText(this, "Se requiere el permiso de ubicación precisa",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void bindOdometer() {
        if (!bound) {
            Intent intent = new Intent(this, OdometerService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    private void iniciar() {
        if (!bound) {
            Toast.makeText(this, "Servicio no conectado (revise el permiso)",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            float precision = Float.parseFloat(etPrecision.getText().toString());
            int intervalo = Integer.parseInt(etIntervalo.getText().toString());
            if (precision < 0 || intervalo < 1) throw new NumberFormatException();
            odometer.resetDistance();
            tracking = odometer.startTracking(intervalo, precision);
            if (!tracking) {
                Toast.makeText(this, "Active el GPS del dispositivo",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valores de configuración inválidos",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void finalizarYGuardar() {
        if (!bound || !tracking) {
            Toast.makeText(this, "Primero inicie la tutoría", Toast.LENGTH_SHORT).show();
            return;
        }
        odometer.stopTracking();
        tracking = false;
        Location loc = odometer.getLastLocation();
        float precision = Float.parseFloat(etPrecision.getText().toString());
        int intervalo = Integer.parseInt(etIntervalo.getText().toString());
        db.guardar(odometer.getDistance(), precision, intervalo,
                loc != null ? loc.getLatitude() : 0,
                loc != null ? loc.getLongitude() : 0);
        Toast.makeText(this, "Recorrido guardado", Toast.LENGTH_SHORT).show();
    }

    private void displayDistance() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (bound && odometer != null) {
                    tvDistance.setText(String.format(Locale.getDefault(),
                            "%,.2f m", odometer.getDistance()));
                    Location l = odometer.getLastLocation();
                    if (l != null) {
                        tvLocation.setText(String.format(Locale.getDefault(),
                                "Ubicación: %.6f, %.6f", l.getLatitude(), l.getLongitude()));
                    }
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        db.close();
    }
}