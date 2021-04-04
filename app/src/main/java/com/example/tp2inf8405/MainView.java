package com.example.tp2inf8405;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainView extends AppCompatActivity {

    boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

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


    }
}