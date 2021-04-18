package com.example.tp2inf8405;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
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
    private String currentUsername;

    private List<LocationKey> locationKeys = new ArrayList<LocationKey>();
    private ArrayList<String> favoritesList = new ArrayList<String>();

    class Device {
        public String mac_addr;
        public String name;
        public String alias;
        public String type;
    }

    private List<Device> nearbyDevices = new ArrayList<Device>();

    private Marker currentPosMarker;
    private FusedLocationProviderClient mFusedLocationClient;
    private Handler handler = new Handler();
    private Runnable usageUpdate;


    private String currentUserLocationKey;
    private String currentUserKey;

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


        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Receive username from textInput on UserFirstPageActivity
        currentUsername = getIntent().getStringExtra("username");
        handleCurrentUsername();

        setSwapThemeListener();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
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

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng test = new LatLng(latitude, longitude);


                if (mLastLocation == null || isDistantEnough(latitude, mLastLocation.getLatitude())
                        || isDistantEnough(longitude, mLastLocation.getLongitude())) {

                    Log.d("nearby device", "onSuccess location callback");


                    // RESET EVERYTHING
                    nearbyDevices = new ArrayList<Device>();

                    Log.d("LOCATION", "new location");
                    mLastLocation = location;
                    dbRef = dbRootNode.getReference("accounts/" + currentUserKey + "/locations").push();
                    String locationKey = dbRef.getKey();
                    currentUserLocationKey = locationKey;
                    locationKeys.add(new LocationKey(locationKey, latitude, longitude));
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
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(test, 15));

                }

            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(receiver, filter);

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


            LatLng currentPos = currentPosMarker.getPosition();
            if (isDistantEnough(clicked_latitude, currentPos.latitude) || isDistantEnough(clicked_longitude, currentPos.longitude)) {
                for (int i = 0; i < locationKeys.size(); i++) {
                    String loopKey = locationKeys.get(i).key;
                    double long_loop = locationKeys.get(i).longitude;
                    double lat_loop = locationKeys.get(i).latitude;

                    String loopPath = "accounts/" + currentUserKey + "/locations/" + loopKey;
                    dbRef = dbRootNode.getReference(loopPath);
                    dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (!task.isSuccessful()) {
                                Log.e("firebase", "Error getting data", task.getException());
                            } else {
                                if (!isDistantEnough(clicked_latitude, lat_loop) && !isDistantEnough(clicked_longitude, long_loop)) {
                                    String mac_addr = "", name = "", alias = "", type = "";

                                    LayoutInflater inflater = (LayoutInflater) MapsActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                    View popupLayout = inflater.inflate(R.layout.popup_layout, null, false);
                                    ListView popupListView = popupLayout.findViewById(R.id.popup_listView);
                                    TextView foundDevicesTextView = popupLayout.findViewById(R.id.textView);
                                    foundDevicesTextView.setText(foundDevicesTextView.getText()+"\nlongitude: "+ clicked_longitude+"\nlatitude: "+clicked_latitude);
//                                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) popupListView.getLayoutParams();
//                                    params.height = FrameLayout.LayoutParams.MATCH_PARENT;

                                    for (DataSnapshot d : task.getResult().getChildren()) {
                                        if (!d.getKey().equals("latitude") && !d.getKey().equals("longitude")) {
                                            mac_addr = String.valueOf(d.getKey());
                                            name = String.valueOf(d.child("name").getValue());
                                            alias = String.valueOf(d.child("alias").getValue());
                                            type = String.valueOf(d.child("type").getValue());

                                            Device currentDevice = new Device();
                                            currentDevice.mac_addr = mac_addr;
                                            currentDevice.name = name;
                                            currentDevice.alias = alias;
                                            currentDevice.type = type;

                                            Button button = new Button(getBaseContext());
                                            if (favoritesList.contains(mac_addr)) {
                                                button.setText("*" + name + "\n" + mac_addr);
                                            } else {
                                                button.setText(name + "\n" + mac_addr);
                                            }


                                            String finalName = name;
                                            String finalAlias = alias;
                                            String finalMac_addr = mac_addr;
                                            String finalType = type;
                                            button.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    String toastText = "Full device information : \n" +
                                                            "Name : " + finalName + "\n" +
                                                            "Alias : " + finalAlias + "\n" +
                                                            "Mac address : " + finalMac_addr + "\n" +
                                                            "Device type : " + finalType;
                                                    //affichage infos sur l'écran
                                                    Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
                                                    toast.show();
                                                    showOtherPosAlertDialogBuilder(currentDevice, button, clicked_latitude, clicked_longitude);
                                                }
                                            });

                                            popupListView.addHeaderView(button);
//                                            popupListView.setAdapter(new ArrayAdapter(getBaseContext(), R.layout.test));

                                        }
                                    }
                                    popupListView.setAdapter(new ArrayAdapter(getBaseContext(), R.layout.test));

                                    final PopupWindow popupWindow = new PopupWindow(popupLayout, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

                                    //Set up touch closing outside of pop-up
                                    popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                                    popupWindow.setTouchInterceptor(new View.OnTouchListener() {
                                        public boolean onTouch(View v, MotionEvent event) {
                                            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                                                popupWindow.dismiss();
                                                return true;
                                            }
                                            return false;
                                        }
                                    });
                                    popupWindow.setOutsideTouchable(true);
                                    popupWindow.showAtLocation(findViewById(R.id.mainView), Gravity.NO_GRAVITY, 500, 100);

                                }
                            }
                        }
                    });
                }
            }
            return true; //n'affiche pas le titre de l'épingle
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("NewApi")
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                //state change of bluetooth
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //surement besoin d'un message/toast "vous avez besoin de bluetooth pour..."
                        break;
                    case BluetoothAdapter.STATE_ON:
                        bluetoothAdapter.startDiscovery();
                        break;
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                bluetoothAdapter.startDiscovery();

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

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
                switch (currentDevice.getType()) {
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
                updateDeviceLocation(deviceHardwareAddress);
                String userLocationKeyPath = "accounts/" + currentUserKey + "/locations/" + currentUserLocationKey;
                dbRef = dbRootNode.getReference(userLocationKeyPath);

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
                            if (!task.getResult().hasChild(deviceHardwareAddress)) {
                                // Write a message to the database
                                dbRootNode.getReference(userLocationKeyPath).child(deviceHardwareAddress).child("name").setValue(finalDeviceName);
                                dbRootNode.getReference(userLocationKeyPath).child(deviceHardwareAddress).child("alias").setValue(finalDeviceAlias);
                                dbRootNode.getReference(userLocationKeyPath).child(deviceHardwareAddress).child("type").setValue(finalDeviceType);
                            }


                        }
                    }
                });
                addNearbyDevice(deviceHardwareAddress, finalDeviceName, finalDeviceAlias, finalDeviceType);
            }
        }
    };

    private void startLocationUpdates() {
        //nécessité de demander l'autorisation pour l'accès aux localisations
        int permissionCheck1 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);

        }

        mFusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null);
    }

    // Algos pour determiner si la localization retrouvee est assez loin
    private boolean isDistantEnough(double l1, double l2) {
        double distance = Math.abs(l1 - l2);
        return distance >= 0.0005; //5e-5 vaut a peu pres 5 mètres
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        handler.postDelayed(usageUpdate = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void run() {
                updateUsage(getApplicationContext());
                handler.postDelayed(usageUpdate, 30*1000);
            }
        }, 30*1000);
        super.onResume();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(usageUpdate);
        super.onPause();
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

    //    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addNearbyDevice(String macAddr, String name, String alias, String type) {

//        TextView liste = findViewById(R.id.textView);

        Device discoveredDevice = new Device();
        discoveredDevice.mac_addr = macAddr;
        discoveredDevice.name = name;
        discoveredDevice.alias = alias;
        discoveredDevice.type = type;

        ListView listView = findViewById(R.id.listView);

        boolean contains = false;
        int i = 0;
        if (!nearbyDevices.isEmpty()) {
            while (i < nearbyDevices.size() && !contains) {
                Device listCurrentDevice = nearbyDevices.get(i);
                String listCurrentAddress = listCurrentDevice.mac_addr;
                if (macAddr.equals(listCurrentAddress)) {
                    contains = true;
                }
                i++;
            }
        }

        if (nearbyDevices.isEmpty() || !contains) {
            nearbyDevices.add(discoveredDevice);
            //https://stackoverflow.com/questions/13005549/how-to-use-addheaderview-to-add-a-simple-imageview-to-a-listview

            Button button = new Button(getBaseContext());

            if (favoritesList.contains(discoveredDevice.mac_addr)) {
                button.setText("*" + name + "\n" + macAddr);
            } else {
                button.setText(name + "\n" + macAddr);
            }

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String toastText = "Full device information : \n" +
                            "Name : " + name + "\n" +
                            "Alias : " + alias + "\n" +
                            "Mac address : " + macAddr + "\n" +
                            "Device type : " + type;
                    //affichage infos sur l'écran
                    Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
                    toast.show();
                    showCurrentPosAlertDialogBuilder(discoveredDevice, button);
                }
            });
            //https://stackoverflow.com/questions/8933515/android-button-created-in-run-time-to-match-parent-in-java
            listView.addHeaderView(button);
            listView.setAdapter(new ArrayAdapter(getBaseContext(), R.layout.test));
        }
    }

    private void updateDeviceLocation(String deviceHardwareAddress) {

        for (int i = 0; i < locationKeys.size(); i++) {
            String loopKey = locationKeys.get(i).key;
            String loopPath = "accounts/" + currentUserKey + "/locations/" + loopKey;
            dbRef = dbRootNode.getReference(loopPath);

            dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        for (DataSnapshot d : task.getResult().getChildren()) {

                            if (d.getKey().equals(deviceHardwareAddress) && !loopKey.equals(currentUserLocationKey)) {
                                d.getRef().removeValue();
                                Log.d("firebase MAC addr", "Removing device " + deviceHardwareAddress + " from location " + loopKey +
                                        "\nTrue key = " + currentUserLocationKey);
                            }
                        }
                    }
                }
            });
        }
    }

    private int handleFirstLocation(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        dbRef = dbRootNode.getReference("accounts/" + currentUserKey + "/locations");
        Log.d("handleFirstLocation", currentUserKey);
        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    boolean isInBd = false;
                    for (DataSnapshot d : task.getResult().getChildren()) {
                        double loop_longitude = (double) d.child("longitude").getValue();
                        double loop_latitude = (double) d.child("latitude").getValue();
                        if (!isDistantEnough(loop_longitude, longitude) && !isDistantEnough(loop_latitude, latitude)) {
                            currentUserLocationKey = d.getKey();
                            isInBd = true;
                        }

                    }

                    if (!isInBd) {
                        Log.d("handleFirstLocation", currentUserKey);
                        dbRef = dbRootNode.getReference("accounts/" + currentUserKey + "/locations").push();
                        String locationKey = dbRef.getKey();
                        Log.d("locationkey", locationKey);
                        currentUserLocationKey = locationKey;
                        locationKeys.add(new LocationKey(locationKey, latitude, longitude));
                        dbRef.child("latitude").setValue(latitude);
                        dbRef.child("longitude").setValue(longitude);
                    }

                    bluetoothAdapter.startDiscovery();

                }
            }
        });


        return 0;
    }

    private void shareDevice(Device device) {
        Intent send = new Intent();
        String deviceName = device.name;
        String deviceAlias = device.alias;
        String deviceType = device.type;
        String deviceHardwareAddress = device.mac_addr;
        send.setAction(Intent.ACTION_SEND);
        send.putExtra(Intent.EXTRA_TEXT, deviceName + " " + deviceAlias +
                " " + deviceType + " " + deviceHardwareAddress);
        send.setType("text/plain");

        Intent share = Intent.createChooser(send, null);
        startActivity(share);
    }

    private void addToFavorites(Device device) {
        favoritesList.add(device.mac_addr);
        String userFavoritesPath = "accounts/" + currentUserKey + "/favorites";
        dbRef = dbRootNode.getReference(userFavoritesPath);

        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    dbRef.child(device.mac_addr).child("name").setValue(device.name);
                    dbRef.child(device.mac_addr).child("alias").setValue(device.alias);
                    dbRef.child(device.mac_addr).child("type").setValue(device.type);
                }
            }
        });

    }

    private void removeFromFavorites(Device device) {
        favoritesList.remove(device.mac_addr);
        String userFavoritesPath = "accounts/" + currentUserKey + "/favorites";
        dbRef = dbRootNode.getReference(userFavoritesPath);
        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    for (DataSnapshot d : task.getResult().getChildren()) {

                        if (d.getKey().equals(device.mac_addr)) {
                            d.getRef().removeValue();
                        }

                    }
                }
            }
        });
    }

    private void showCurrentPosAlertDialogBuilder(Device discoveredDevice, Button button) {

        //propositions de fonctionnalités
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
        alertDialogBuilder.setTitle("Fonctionnalités : \n");
        if (favoritesList.contains(discoveredDevice.mac_addr)) {
            //on ne considère pas ici la fonctionnalité "comment y aller" comme nous sommes déjà à l'emplacement
            alertDialogBuilder.setItems(new CharSequence[]
                            {"Retirer des favoris", "Partager"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: //retirer des favoris
                                    String newText = String.valueOf(button.getText()).replaceFirst("\\*", "");
                                    button.setText(newText);
                                    removeFromFavorites(discoveredDevice);
                                    break;
                                case 1: //partage
                                    shareDevice(discoveredDevice);
                                    break;
                            }
                        }
                    });
        } else {
            //on ne considère pas ici la fonctionnalité "comment y aller" comme nous sommes déjà à l'emplacement
            alertDialogBuilder.setItems(new CharSequence[]
                            {"Ajouter aux favoris", "Partager"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    //ajout aux favoris
                                    button.setText("*" + button.getText()); //étoile pour distinguer favoris
                                    addToFavorites(discoveredDevice);
                                    break;
                                case 1: //partage
                                    shareDevice(discoveredDevice);
                                    break;
                            }
                        }
                    });
        }

        AlertDialog alertDialog = alertDialogBuilder.create();
        new Handler().postDelayed(alertDialog::show, 3600);


    }

    private void showOtherPosAlertDialogBuilder(Device discoveredDevice, Button button, double latitude, double longitude) {

        //propositions de fonctionnalités
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
        alertDialogBuilder.setTitle("Fonctionnalités : \n");
        if (favoritesList.contains(discoveredDevice.mac_addr)) {
            alertDialogBuilder.setItems(new CharSequence[]
                            {"Retirer des favoris", "Partager", "Comment y aller"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: //retirer des favoris
                                    String newText = String.valueOf(button.getText()).replaceFirst("\\*", "");
                                    button.setText(newText);
                                    removeFromFavorites(discoveredDevice);
                                    break;
                                case 1: //partage
                                    shareDevice(discoveredDevice);
                                    break;
                                case 2: //y aller
                                    getDirection(latitude, longitude);
                                    break;
                            }
                        }
                    });
        } else {
            //on ne considère pas ici la fonctionnalité "comment y aller" comme nous sommes déjà à l'emplacement
            alertDialogBuilder.setItems(new CharSequence[]
                            {"Ajouter aux favoris", "Partager", "Comment y aller"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    //ajout aux favoris
                                    button.setText("*" + button.getText()); //étoile pour distinguer favoris
                                    addToFavorites(discoveredDevice);
                                    break;
                                case 1: //partage
                                    shareDevice(discoveredDevice);
                                    break;
                                case 2: //y aller
                                    getDirection(latitude, longitude);
                                    break;
                            }
                        }
                    });
        }

        AlertDialog alertDialog = alertDialogBuilder.create();
        new Handler().postDelayed(alertDialog::show, 3600);


    }


    private void getAllInitLocationKeys() {
        dbRef = dbRootNode.getReference("accounts/" + currentUserKey + "/locations");
        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    // Load all location keys for current user
                    for (DataSnapshot d : task.getResult().getChildren()) {
                        Log.d("firebase location key", d.getKey());
                        double loop_longitude = (double) d.child("longitude").getValue();
                        double loop_latitude = (double) d.child("latitude").getValue();
                        locationKeys.add(new LocationKey(d.getKey(), loop_latitude, loop_longitude));

                    }
                }

                // Add marker position on maps
                for (int i = 0; i < locationKeys.size(); i++) {
                    String loopKey = locationKeys.get(i).key;
                    double latitude = locationKeys.get(i).latitude;
                    double longitude = locationKeys.get(i).longitude;
                    String loopPath = "accounts/" + currentUserKey + "/locations/" + loopKey;
                    dbRef = dbRootNode.getReference(loopPath);
                    dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (!task.isSuccessful()) {
                                Log.e("firebase", "Error getting data", task.getException());
                            } else {
                                LatLng test1 = new LatLng(latitude, longitude);
                                mMap.addMarker(new MarkerOptions().position(test1).icon(BitmapDescriptorFactory.fromResource(R.drawable.epingler)).title("Devices found in lat " + String.valueOf(latitude) + " and longitude " + String.valueOf(longitude)));
                            }
                        }

                    });
                }
            }
        });
    }


    private void getAllInitFavorites() {

        String userFavoritesPath = "accounts/" + currentUserKey + "/favorites";
        dbRef = dbRootNode.getReference(userFavoritesPath);
        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    for (DataSnapshot d: task.getResult().getChildren()) {
                        favoritesList.add(d.getKey());
                    }
                }
            }
        });

    }

    public void getDirection(double latitude, double longitude) {
        //https://developer.android.com/reference/android/net/Uri
        Uri route = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, route);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    // Fetches all usernames from DB
    // On suppose avec UserCreateAccount que l'user est déjà dans la BD
    private void handleCurrentUsername() {
        dbRef = dbRootNode.getReference("accounts");
        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    for (DataSnapshot d: task.getResult().getChildren()) {
                        String snapShotUsername = d.child("username").getValue().toString();
                        Log.d("currentUsername", snapShotUsername);
                        if (snapShotUsername.equals(currentUsername)) {
                            // REFERER A CE USER
                            currentUserKey = d.getKey();
                        }
                    }

                    Log.d("currentuserkey", currentUserKey);
                    // Handle current user's locations and favorite devices
                    getAllInitLocationKeys();
                    getAllInitFavorites();

                    // Handle profile navigation and location callbacks
                    setProfileNavigationListener();
                    handleMFusedLocationClient();
                }

            }
        });
    }

    private void handleMFusedLocationClient() {
        // nécessité de demander l'autorisation pour l'accès aux localisations
        int permissionCheck1 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }

        mFusedLocationClient.getLastLocation().addOnSuccessListener(
                new OnSuccessListener<Location>() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            if (mLastLocation == null || isDistantEnough(latitude, mLastLocation.getLatitude())
                                    || isDistantEnough(longitude, mLastLocation.getLongitude())) {

                                Log.d("nearby device", "onSuccess 1st location");

                                mLastLocation = location;

                                // RESET EVERTYHING
                                nearbyDevices = new ArrayList<Device>();
                                handleFirstLocation(location);

                                LatLng test = new LatLng(latitude, longitude);
                                currentPosMarker = mMap.addMarker(new MarkerOptions().position(test).icon(BitmapDescriptorFactory.fromResource(R.drawable.stickman)).title("CurrentPosition"));

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(test, 15));

                            }
                            String time = String.valueOf(mLastLocation.getTime());
                        } else {
                            Log.d("location", "no location found");
                        }
                    }
                });
        mFusedLocationClient.getLastLocation();
    }

    private void setSwapThemeListener() {
        Button btn = findViewById(R.id.btn);
        ConstraintLayout container = (ConstraintLayout) findViewById(R.id.mainView);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isDarkMode) {
                    container.setBackgroundResource(R.color.black);
                    btn.setBackgroundColor(Color.rgb(77, 0, 153));
                    btn.setTextColor(Color.WHITE);
                    isDarkMode = true;
                } else if (isDarkMode) {
                    container.setBackgroundResource(R.color.white);
                    btn.setBackgroundColor(Color.rgb(204, 153, 255));
                    btn.setTextColor(Color.BLACK);
                    isDarkMode = false;
                }
            }
        });
    }

    private void setProfileNavigationListener() {
        Button profileBtn = findViewById(R.id.profileBtn);

        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toProfileIntent = new Intent(getApplicationContext(), UserProfileActivity.class);
                toProfileIntent.putExtra("currentUsername", currentUsername);
                toProfileIntent.putExtra("currentUserKey", currentUserKey);
                startActivity(toProfileIntent);
            }
        });
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void updateUsage(Context context) {
        Log.d("update","update");
        int check = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_NETWORK_STATE);
        if (check != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 2);
        }
        ConnectivityManager man = (ConnectivityManager)this.getSystemService(CONNECTIVITY_SERVICE);
        NetworkCapabilities cap = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            cap = man.getNetworkCapabilities(man.getActiveNetwork());
        }
        double downlink = cap.getLinkDownstreamBandwidthKbps();
        double uplink = cap.getLinkUpstreamBandwidthKbps();
        //https://developer.android.com/training/monitoring-device-state/battery-monitoring#CurrentLevel
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        float battery = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 /
                batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        TextView bat = findViewById(R.id.BatteryLevel);
        TextView up = findViewById(R.id.UplinkVal);
        TextView down = findViewById(R.id.DownlinkVal);
        bat.setText(getString(R.string.batteryLevel)+" "+String.valueOf(battery)+"%");
        up.setText(getString(R.string.UplinkVal)+" "+String.valueOf(uplink)+" kbps");
        down.setText(getString(R.string.DownlinkVal)+" "+String.valueOf(downlink)+" kbps");


    }


}