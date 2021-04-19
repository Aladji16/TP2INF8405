package com.example.tp2inf8405;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class MainView extends AppCompatActivity {

    boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);



        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Button btn = findViewById(R.id.btn);
        Button language = findViewById(R.id.Language);
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
        language.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Resources res = getResources();
               Configuration con = res.getConfiguration();
               DisplayMetrics dm = res.getDisplayMetrics();
               Locale en = new Locale("en");
               if (con.locale == en) {
                   con.locale = new Locale("fr-rCA");
               } else {
                   con.locale = en;
               }
               res.updateConfiguration(con, dm);
               recreate();
           }
        });


    }
}