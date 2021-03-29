package com.example.tp2inf8405;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
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
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Location mLastLocation; //https://developer.android.com/codelabs/advanced-android-training-device-location#3
    private LocationRequest locationRequest; //https://developer.android.com/training/location/request-updates
    private LocationCallback locationCallback; //https://developer.android.com/training/location/request-updates


    private FusedLocationProviderClient mFusedLocationClient;

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
        setContentView(R.layout.activity_maps);

//        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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


        statusCheck();



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(
                new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mLastLocation = location;
                            String latitude = String.valueOf(mLastLocation.getLatitude());
                            String longitude = String.valueOf(mLastLocation.getLongitude());
                            String time = String.valueOf(mLastLocation.getTime());
                            Log.d("location", latitude + " " + longitude + " " + time);
                        } else {
                            Log.d("location", "no location found");
                        }
                    }
                });



        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d("frère","je sais plus");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d("location test","");
                    mLastLocation = location;
                    String latitude = String.valueOf(mLastLocation.getLatitude());
                    String longitude = String.valueOf(mLastLocation.getLongitude());
                    String time = String.valueOf(mLastLocation.getTime());
                    Log.d("location", latitude + " " + longitude + " " + time);

                }
            }
        };

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        startLocationUpdates();




        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

//        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        bluetoothAdapter.startDiscovery();
        mFusedLocationClient.getLastLocation();



    }




    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("ACTION",action);

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                //state change of bluetooth
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
//                        Log.d("blutoh state", "turnin off");
                        break;

                    case BluetoothAdapter.STATE_OFF:
//                        Log.d("blutoh state", "off");
                        //surement besoin d'un message/toast "vous avez besoin de bluetooth pour..."
                        break;


                    case BluetoothAdapter.STATE_TURNING_ON:
//                        Log.d("blutoh state", "turnin on");
                        break;


                    case BluetoothAdapter.STATE_ON:
                        Log.d("blutoh state", "on");
                        bluetoothAdapter.startDiscovery();
                        break;
                }
            }

            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
                Log.d("STATE", "DISCOVERY BEGIN");
            }

            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
                Log.d("STATE", "DISCOVERY END");
                bluetoothAdapter.startDiscovery();
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                Log.d("STATE","DEVICE FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAlias = device.getAlias();
                String deviceType = " ";
                switch(device.getType()) {

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


                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("STATE",deviceName + " " + deviceAlias + " " + deviceType + " " + deviceHardwareAddress);
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
                Looper.getMainLooper());
    }



    //au cas où, peut etre à utiliser, lorsqu'on demande permission
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case 2:
//                // If the permission is granted, get the location,
//                // otherwise, show a Toast
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    getLocation();
//                } else {
//                    Toast.makeText(this,
//                            R.string.location_permission_denied,
//                            Toast.LENGTH_SHORT).show();
//                }
//                break;
//        }
//    }



    @Override
    protected void onDestroy() {
        super.onDestroy();


        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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
}