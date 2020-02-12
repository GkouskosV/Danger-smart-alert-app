package com.example.unipismartalert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    final static int REQUESTCODE = 324;
    final float NOICE = (float) 0.01;
    final float CRITICAL_POINT_VER = (float) 0.8;
    final float CRITICAL_POINT_ROT = (float) 2.0;
    private static final float NS2S = 1.0f / 1000000000.0f;

    Button sosButton, abortButton;
    MenuItem item;
    SensorManager mSensorManager;
    Sensor mAccelerometer, mGyroscope, mRotation;

    IntentFilter intentFilter;
    Intent batteryStatus;

    TextView x_view, y_view, z_view, timeStart, timeLast, elapsedTime, countView;
    CountDownTimer countDownTimer;
    private MyTts myTts;

    LocationManager locationManager;
    Double latitude, longitude;

    boolean motionStarted, verticalMotion, dbUpdated, counterStarted, isCharging, rotateMove;
    double sec;
    float lastX, lastY, lastZ;
    long elapsed, startVer, finVer, startRot, finRot;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = getApplicationContext().registerReceiver(null, intentFilter);

        myTts = new MyTts(this);
        motionStarted = false;
        verticalMotion = false;
        dbUpdated = false;
        counterStarted = false;
        isCharging = false;

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sosButton = findViewById(R.id.sos_button);
        abortButton = findViewById(R.id.abort_button);
        item = findViewById(R.id.statistics);
        x_view = findViewById(R.id.x_axis);
        y_view = findViewById(R.id.y_axis);
        z_view = findViewById(R.id.z_axis);
        timeLast = findViewById(R.id.time_last);
        timeStart = findViewById(R.id.time_start);
        elapsedTime = findViewById(R.id.elapsed_time);
        countView = findViewById(R.id.countdown);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUESTCODE);
        }else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grandResults);
        if (grandResults[0] == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.gps_ok), Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.ask_gps), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // έναρξη κίνησης
            if(!motionStarted) {
                lastX = x;
                lastY = y;
                lastZ = z;
                x_view.setText("0.0");
                y_view.setText("0.0");
                z_view.setText("0.0");
                motionStarted = true;
            }
            else {
                float dX = Math.abs(lastX - x);
                float dY = Math.abs(lastY - y);
                float dZ = Math.abs(lastX - z);

                // Αν κίνηση μικρότερη από ΝΟΙCE  κάνε τη 0
                if (dX < NOICE) dX = (float) 0.0;
                if (dY < NOICE) dY = (float) 0.0;
                if (dZ < NOICE) dZ = (float) 0.0;

                lastX = x;
                lastY = y;
                lastZ = z;

                x_view.setText(String.valueOf(dX));
                y_view.setText(String.valueOf(dY));
                z_view.setText(String.valueOf(dZ));

                // Αν οι άξονες κοινούνται ταυτόχρονα ενεργοποίηση κυκλικής κίνησης
                if (dX > 0.1 && dY > 0.1 && dZ > 0.1)
                {
                    if (!rotateMove) {
                        startRot = event.timestamp;
                        rotateMove = true;
                    }
                }
                if (dX > dY) {
                    //Toast.makeText(this, "Horizontal movement", Toast.LENGTH_SHORT).show();
                }

                // Αν η κάθετη κίνηση είναι μεγαλύτερη από την οριζόντια ενεργοποίηση κάθετης κίνησης
                else if (dY > dX) {
                    if (!verticalMotion) {
                        startVer = event.timestamp;
                        verticalMotion = true;
                    }
                    //Toast.makeText(this, "Falling down!!", Toast.LENGTH_SHORT).show();
                }

                else if (dY == dX) {
                    // Αν η συσκευή σταμάτησε να κινήται μετά από κάθετη κίνηση
                    if (verticalMotion) {
                        finVer = event.timestamp;
                        verticalMotion = false;

                        elapsed = finVer - startVer;
                        sec = (double)elapsed * NS2S;
                        elapsedTime.setText(String.valueOf(sec));

                        if (sec > CRITICAL_POINT_VER && !dbUpdated) {
                            Toast.makeText(this, "Update database, vertical movement", Toast.LENGTH_SHORT).show();
                            if (!counterStarted) {
                                counterStarted = true;
                                countDownTimer = new CountDownTimer(30000, 1000) {
                                    @Override
                                    public void onTick(long millisUntilFinished) {
                                        countView.setVisibility(View.VISIBLE);
                                        countView.setText(getResources().getString(R.string.remaining_time) + millisUntilFinished / 1000);
                                    }

                                    @Override
                                    public void onFinish() {
                                        counterStarted = false;
                                        countView.setText(getResources().getString(R.string.done));
                                    }
                                }.start();
                             }
                            dbUpdated= true;
                        }
                        dbUpdated = false;
                        // Αν η συσκευή σταμάτησε να κινήται μετά από κυκλική κίνηση
                    } else if (rotateMove) {
                        finRot = event.timestamp;
                        rotateMove = false;

                        elapsed = finRot - startRot;
                        sec = (double) elapsed * NS2S;

                        if (sec > CRITICAL_POINT_ROT && !dbUpdated) {
                            Toast.makeText(this, "Update database, earthquake", Toast.LENGTH_SHORT).show();
                        }
                        dbUpdated = true;
                    }
                    dbUpdated = false;
                }
            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void sendSMS(View view) {
        Toast.makeText(this, getResources().getString(R.string.sms_toast), Toast.LENGTH_LONG).show();
        for (int i=0; i<5; i++) {
            myTts.speak("Please help I am in danger!");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUESTCODE);
        }else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0, this);
        }

        latitude = Objects.requireNonNull(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).getLatitude();
        longitude = Objects.requireNonNull(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).getLongitude();

        timeStart.setText(String.valueOf(latitude));
        timeLast.setText(String.valueOf(longitude));

        //@SuppressLint("StringFormatMatches") String sos_m = getResources().getString(R.string.sos_message, String.valueOf(latitude), String.valueOf(longitude));
        //Toast.makeText(this, sos_m, Toast.LENGTH_SHORT).show();
    }

    public void abortAlert(View view) {
        if (counterStarted)
        {
            counterStarted = false;
            countDownTimer.cancel();
            countView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.statistics:
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateConfig(String locale) {
        Locale mylocale = new Locale(locale);
        Locale.setDefault(mylocale);
        Configuration config = new Configuration();
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        recreate();
    }

    public void english(View view) {
        updateConfig("en");
    }
    public void greek(View view) {
        updateConfig("el");
    }
    public void spanish(View view) { updateConfig("es"); }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onLocationChanged(Location location) {}
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}
}
