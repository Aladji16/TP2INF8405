package com.example.tp2inf8405;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

public class UserFirstPageActivity extends AppCompatActivity {
    public static ArrayList<String> namesInDB = new ArrayList<String>();
    public static Configuration con;

    private FirebaseDatabase dbRootNode = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = dbRootNode.getReference();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAppLang();
        setContentView(R.layout.activity_user_first_page);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setLanguageListener();

        // If user comes back from creating an account
        String newAccountUsername = getIntent().getStringExtra("newAccountUsername");
        if (newAccountUsername != null) {
            TextInputEditText userInput = (TextInputEditText) findViewById(R.id.username_input);
            userInput.setText(newAccountUsername);
        } else {
            getAllInitNamesInDB();
        }

        Button createAccountButton = (Button) findViewById(R.id.createAccountButton);
        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText userInput = (TextInputEditText) findViewById(R.id.username_input);
                String inputText = String.valueOf(userInput.getText());
                if (!inputText.equals("")) {
                    if (!namesInDB.contains(inputText)) {
                        String userNotInDBMsg = getString(R.string.userNotInDB);
                        Toast toast = Toast.makeText(getApplicationContext(), "Username: " + userNotInDBMsg, Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        Intent toMapsIntent = new Intent(getApplicationContext(), MapsActivity.class);
                        toMapsIntent.putExtra("username", inputText);
                        startActivity(toMapsIntent);
                    }
                }
            }
        });

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapIntent = new Intent(getApplicationContext(), UserCreateAccountActivity.class);
                startActivity(mapIntent);
            }
        });


    }

    public void getAllInitNamesInDB()
    {
        dbRef = dbRootNode.getReference("accounts");
        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    for (DataSnapshot d: task.getResult().getChildren())
                    {
                        namesInDB.add(d.child("username").getValue().toString());
                    }
                }
            }
        });
    }

    private void initAppLang() {
        Resources res = getResources();
        con = getResources().getConfiguration();
        DisplayMetrics dm = res.getDisplayMetrics();
        Log.d("initAppLang non", String.valueOf(con.locale).substring(0,2));
        if (String.valueOf(con.locale).substring(0,2).equals("en")) {
            con.locale = new Locale("en");
        } else {
            con.locale = new Locale("fr_FR");
        }
        res.updateConfiguration(con, dm);
    }


    private void setLanguageListener() {
        Button language = findViewById(R.id.Language);
        language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Resources res = getResources();
                con = res.getConfiguration();
                DisplayMetrics dm = res.getDisplayMetrics();
                Locale en = new Locale("en");
                Log.d("locale",con.locale.toString());
                if (con.locale.equals(en)) {
                    Log.d("oui","oui");
                    con.locale = new Locale("fr_FR");
                } else {
                    Log.d("non","non");
                    con.locale = en;
                }
                res.updateConfiguration(con, dm);
                recreate();
            }
        });
    }





}
