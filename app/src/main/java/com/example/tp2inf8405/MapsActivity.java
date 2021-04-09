package com.example.tp2inf8405;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Location mLastLocation; //https://developer.android.com/codelabs/advanced-android-training-device-location#3
    private LocationRequest locationRequest; //https://developer.android.com/training/location/request-updates
    private LocationCallback locationCallback; //https://developer.android.com/training/location/request-updates
    private BluetoothDevice currentDevice;
    private FirebaseDatabase dbRootNode = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = dbRootNode.getReference();
    private List<String> locationKeys = new ArrayList<String>();

    class Device {
        public String mac_addr;
        public String name;
        public String alias;
        public String type;
    }

    private List<Device> nearbyDevices = new ArrayList<Device>();

    private boolean firstLocationisInBD = false;

    private Marker currentPosMarker;
    private FusedLocationProviderClient mFusedLocationClient;

    private String currentKey;

    boolean isDarkMode = false;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        https://developer.android.com/guide/topics/connectivity/bluetooth
//        https://www.tutorialspoint.com/android/android_bluetooth.htm
//        https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#startDiscovery()
//        http://www.londatiga.net/it/programming/android/how-to-programmatically-scan-or-discover-android-bluetooth-device/
//        https://stackoverflow.com/questions/38512993/bluetooth-action-found-broadcastreceiver-is-not-working
//        https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
//        action_state_changed https://www.programcreek.com/java-api-examples/?class=android.bluetooth.BluetoothAdapter&method=ACTION_STATE_CHANGED

        //        Get all locationIDs already in Firebase DB
        getAllInitLocationKeys();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Button btn = findViewById(R.id.btn);
        ConstraintLayout container = (ConstraintLayout) findViewById(R.id.mainView);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isDarkMode) {
                    container.setBackgroundResource(R.color.black);
                    btn.setBackgroundColor(Color.rgb(77, 0, 153));
                    btn.setTextColor(Color.WHITE);
                    isDarkMode = true;
                }
                else if(isDarkMode) {
                    container.setBackgroundResource(R.color.white);
                    btn.setBackgroundColor(Color.rgb(204, 153, 255));
                    btn.setTextColor(Color.BLACK);
                    isDarkMode = false;
                }
            }
        });

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);


            //nécessité de demander l'autorisation pour l'accès aux localisations
            int permissionCheck1 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            int permissionCheck2 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION);

            if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            }
        }

        else {
            //nécessité de demander l'autorisation pour l'accès aux localisations
            int permissionCheck1 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            int permissionCheck2 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION);

            if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            }
        }


        // TEST DB 1 time
        // Handle Firebase DB Listeners

        // Read from the database
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d("TAG", "Failed to read value.", error.toException());
            }
        });

        statusCheck();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this::onMapReady);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(
                new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            if (mLastLocation == null || isDistantEnough(latitude, mLastLocation.getLatitude())
                                    || isDistantEnough(longitude, mLastLocation.getLongitude())) {
                                mLastLocation = location;

                                // RESET EVERTYHING
                                nearbyDevices = new ArrayList<Device>();
                                TextView liste = findViewById(R.id.textView);
                                liste.setText("");


                                isLocationInBD(location);
                                if (firstLocationisInBD)
                                {
                                    dbRef = dbRootNode.getReference("locations").push();
                                    String locationKey = dbRef.getKey();
                                    Log.d("locationkey", locationKey);
                                    currentKey = locationKey;
                                    locationKeys.add(locationKey);
                                    dbRef.child("latitude").setValue(latitude);
                                    dbRef.child("longitude").setValue(longitude);
                                }

                                LatLng test = new LatLng(latitude, longitude);
                                currentPosMarker = mMap.addMarker(new MarkerOptions().position(test).icon(BitmapDescriptorFactory.fromResource(R.drawable.stickman)).title("CurrentPosition"));

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(test,20));

                            }

                            String time = String.valueOf(mLastLocation.getTime());

                        } else {
                            Log.d("location", "no location found");
                        }
                    }
                });



        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng test = new LatLng(latitude, longitude);

                if (mLastLocation == null || isDistantEnough(latitude, mLastLocation.getLatitude())
                        || isDistantEnough(longitude, mLastLocation.getLongitude())) {

                    // RESET EVERYTHING
                    nearbyDevices = new ArrayList<Device>();
                    TextView liste = findViewById(R.id.textView);
                    liste.setText("");


                    Log.d("LOCATION","new location");
                    mLastLocation = location;
                    dbRef = dbRootNode.getReference("locations").push();
                    String locationKey = dbRef.getKey();
                    currentKey = locationKey;
                    locationKeys.add(locationKey);
                    dbRef.child("latitude").setValue(latitude);
                    dbRef.child("longitude").setValue(longitude);
                    if (currentPosMarker != null) {
                        //ajouter l'épingle avec les différents device trouvés
                        LatLng prev_pos = currentPosMarker.getPosition();
                        double prev_latitude = prev_pos.latitude;
                        double prev_longitude = prev_pos.longitude;
                        mMap.addMarker(new MarkerOptions().position(prev_pos).icon(BitmapDescriptorFactory.fromResource(R.drawable.epingler)).title("Devices found in lat " + String.valueOf(prev_latitude) + " and longitude " + String.valueOf(prev_longitude)));

                        currentPosMarker.remove();

                    }

                    currentPosMarker = mMap.addMarker(new MarkerOptions().position(test).icon(BitmapDescriptorFactory.fromResource(R.drawable.stickman)).title("CurrentPosition"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(test,20));

                }

            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(receiver, filter);
        bluetoothAdapter.startDiscovery();
        mFusedLocationClient.getLastLocation();

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        startLocationUpdates();

    }


    private GoogleMap.OnMarkerClickListener eventMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            LatLng clicked_position = marker.getPosition();
            double clicked_latitude = clicked_position.latitude;
            double clicked_longitude = clicked_position.longitude;



            for (int i = 0; i < locationKeys.size(); i++) {
                String loopKey = locationKeys.get(i);
                dbRef = dbRootNode.getReference("locations/" + loopKey);
                dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Log.e("firebase", "Error getting data", task.getException());
                        }
                        else {
                            DataSnapshot snap_long = task.getResult().child("longitude");
                            DataSnapshot snap_lat = task.getResult().child("latitude");
                            double long_loop = (double) snap_long.getValue();
                            double lat_loop = (double) snap_lat.getValue();

                            String mac_addr = "", name = "", alias = "", type = "";

                            for (DataSnapshot d: task.getResult().getChildren()) {
                                if (! d.getKey().equals("latitude") && ! d.getKey().equals("longitude"))
                                {
                                    mac_addr = String.valueOf(d.getKey());
                                    name = String.valueOf(d.child("name").getValue());
                                    alias = String.valueOf(d.child("alias").getValue());
                                    type = String.valueOf(d.child("type").getValue());


                                }


                            }
                        }
                    }

                });
            }
            return false;
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                //state change of bluetooth
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
//                        Log.d("blutoh state", "off");
                        //surement besoin d'un message/toast "vous avez besoin de bluetooth pour..."
                        break;
                    case BluetoothAdapter.STATE_ON:
                        bluetoothAdapter.startDiscovery();
                        break;
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                bluetoothAdapter.startDiscovery();

            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                currentDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = currentDevice.getName();
                if (deviceName == null) {
                    deviceName = "Unknown";
                }
                String deviceAlias = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    deviceAlias = currentDevice.getAlias();
                }
                if (deviceAlias == null) {
                    deviceAlias = "Unknown";
                }
                String deviceType = " ";
                switch(currentDevice.getType()) {
                    case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                        deviceType = "Classic";
                        break;
                    case BluetoothDevice.DEVICE_TYPE_LE:
                        deviceType = "Low Energy";
                        break;
                    case BluetoothDevice.DEVICE_TYPE_DUAL:
                        deviceType = "Dual";
                        break;
                    case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                        deviceType = "Unknown";
                        break;
                }


                String deviceHardwareAddress = currentDevice.getAddress(); // MAC address
                Log.d("Device detected",deviceHardwareAddress);

                updateDeviceLocation(deviceHardwareAddress);

                if (currentKey != null) {
                    dbRef = dbRootNode.getReference("locations/" + currentKey);

                    String finalDeviceName = deviceName;
                    String finalDeviceAlias = deviceAlias;
                    String finalDeviceType = deviceType;
                    dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (!task.isSuccessful()) {
                                Log.e("firebase", "Error getting data", task.getException());
                            } else {
                                //si le device n'existe pas encore dans la table
                                if (!task.getResult().hasChild(deviceHardwareAddress))
                                {
                                    Log.d("NEW", "new device...");
                                    // Write a message to the database
                                    dbRef.child(deviceHardwareAddress).child("name").setValue(finalDeviceName);
                                    dbRef.child(deviceHardwareAddress).child("alias").setValue(finalDeviceAlias);
                                    dbRef.child(deviceHardwareAddress).child("type").setValue(finalDeviceType);
                                }


                            }
                        }
                    });
                    addNearbyDevice(deviceHardwareAddress, finalDeviceName, finalDeviceAlias, finalDeviceType);
                }
            }
        }
    };

    private void startLocationUpdates() {
        //nécessité de demander l'autorisation pour l'accès aux localisations
        int permissionCheck1 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);

        }

        mFusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null);
    }

    // Algos pour determiner si la localization retrouvee est assez loin
    private boolean isDistantEnough(double l1, double l2) {
        double distance = Math.abs(l1 - l2);
        return distance >= 0.00005; //5e-5 vaut a peu pres 5 mètres
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(eventMarkerClickListener);
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }


    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
/*
    private void getList() {

        TextView liste = findViewById(R.id.textView);

        liste.setText("");

        for (int i = 0; i < locationKeys.size(); i++) {
            String loopKey = locationKeys.get(i);
            dbRef = dbRootNode.getReference("locations/" + loopKey);
            dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {

                    String list = "";

                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        for (DataSnapshot d : task.getResult().getChildren()) {

                            String mac_addr = "", name = "", alias = "", type = "";

                            if (!d.getKey().equals("latitude") && !d.getKey().equals("longitude")) {
                                mac_addr = String.valueOf(d.getKey());
                                name = String.valueOf(d.child("name").getValue());
                                alias = String.valueOf(d.child("alias").getValue());
                                type = String.valueOf(d.child("type").getValue());
                            }

                            liste.setText(liste.getText() + name + "\n" + mac_addr + "\n" + "-------------------------" + "\n");
                        }

                    }
                }
            });
        }
    }

    */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addNearbyDevice(String macAddr, String name, String alias, String type) {

        TextView liste = findViewById(R.id.textView);

        Device discoveredDevice = new Device();
        discoveredDevice.mac_addr = macAddr;
        discoveredDevice.name = name;
        discoveredDevice.alias = alias;
        discoveredDevice.type = type;


        boolean contains  = nearbyDevices.stream().filter(device -> device.mac_addr.equals(macAddr)).findFirst().orElse(null) != null;
        Log.d("Nearby Device", String.valueOf(contains));

        if (nearbyDevices.isEmpty() || !contains) {
            nearbyDevices.add(discoveredDevice);
            liste.setText(liste.getText() + discoveredDevice.mac_addr + "\n" + discoveredDevice.name + "\n" + "-------------------------" + "\n");

        }
    }

    private void updateDeviceLocation(String deviceHardwareAddress) {

        for (int i = 0; i < locationKeys.size(); i++) {
            String loopKey = locationKeys.get(i);
            dbRef = dbRootNode.getReference("locations/" + loopKey);

            dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        for (DataSnapshot d: task.getResult().getChildren()) {

                            if (d.getKey().equals(deviceHardwareAddress) && loopKey != currentKey) {
                                d.getRef().removeValue();
                                Log.d("firebase MAC addr", "Removing device " + deviceHardwareAddress + " from location " + loopKey);

                            }
                        }
                    }
                }
            });
        }
    }

    private void isLocationInBD(Location location)
    {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();


        for (int i = 0; i < locationKeys.size(); i++)
        {
            String loopKey = locationKeys.get(i);
            dbRef = dbRootNode.getReference("locations/" + loopKey);

            dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        double loop_long = (double) task.getResult().child("longitude").getValue();
                        double loop_lat = (double) task.getResult().child("latitude").getValue();
                        if (!isDistantEnough(loop_long, longitude) && !isDistantEnough(loop_lat, latitude))
                        {
                            currentKey = loopKey;
                            firstLocationisInBD = true;
                        }
                    }
                }
            });
        }
    }

    private void getAllInitLocationKeys() {
        dbRef = dbRootNode.getReference("locations");
        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    for (DataSnapshot d: task.getResult().getChildren()) {
                        Log.d("firebase location key", d.getKey());
                        locationKeys.add(d.getKey());

                    }
                }

                for (int i = 0; i < locationKeys.size(); i++) {
                    String loopKey = locationKeys.get(i);
                    dbRef = dbRootNode.getReference("locations/" + loopKey);
                    dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (!task.isSuccessful()) {
                                Log.e("firebase", "Error getting data", task.getException());
                            }
                            else {
                                double latitude = (double) task.getResult().child("latitude").getValue();
                                double longitude = (double) task.getResult().child("longitude").getValue();
                                LatLng test1 = new LatLng(latitude, longitude);
                                mMap.addMarker(new MarkerOptions().position(test1).icon(BitmapDescriptorFactory.fromResource(R.drawable.epingler)).title("Devices found in lat " + String.valueOf(latitude) + " and longitude " + String.valueOf(longitude)));

                            }
                        }

                    });
                }
            }
        });
    }
}