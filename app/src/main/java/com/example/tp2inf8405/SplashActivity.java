package com.example.tp2inf8405;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Splash);
        setContentView(R.layout.activity_splash);

        // Rajout du son
        MediaPlayer mp = MediaPlayer.create(getBaseContext(), R.raw.splash);
        mp.start();

        handler = new Handler();
        handler.postDelayed(() -> {
            mp.stop();
//            Intent viewIntent = new Intent(getApplicationContext(), MainView.class);
//            startActivity(viewIntent);
            Intent mapIntent = new Intent(getApplicationContext(), MapsActivity.class);
            startActivity(mapIntent);
            finish();
        },3000);
    }
}