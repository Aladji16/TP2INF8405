package com.example.tp2inf8405;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public  BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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


            //nécessité de demander l'autorisation pour ACCESS_COARSE_LOCATION
            int permissionCheck = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            Log.d("granted", String.valueOf(permissionCheck));
            if (permissionCheck != PackageManager.PERMISSION_GRANTED )
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            }


        }

        else {
            //nécessité de demander l'autorisation pour ACCESS_COARSE_LOCATION
            int permissionCheck = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            Log.d("granted", String.valueOf(permissionCheck));
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            }


        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

//        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        bluetoothAdapter.startDiscovery();


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
                        Log.d("blutoh state", "turnin off");
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        Log.d("blutoh state", "off");
                        //surement besoin d'un message/toast "vous avez besoin de bluetooth pour..."
                        break;


                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d("blutoh state", "turnin on");
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
                int deviceType = device.getType();
                switch(deviceType) {

                    case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                        Log.d("TYPE", "Classic");
                        break;

                    case BluetoothDevice.DEVICE_TYPE_LE:
                        Log.d("TYPE", "Low Energy");
                        break;

                    case BluetoothDevice.DEVICE_TYPE_DUAL:
                        Log.d("TYPE", "Dual");
                        break;


                    case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                        Log.d("TYPE", "Unknown");
                        break;
                }


                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("STATE",deviceName + " " + deviceHardwareAddress);
            }
        }
    };


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
}