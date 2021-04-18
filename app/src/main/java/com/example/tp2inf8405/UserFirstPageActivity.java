package com.example.tp2inf8405;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
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

public class UserFirstPageActivity extends AppCompatActivity {
    public static ArrayList<String> namesInDB = new ArrayList<String>();
    private FirebaseDatabase dbRootNode = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = dbRootNode.getReference();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getAllInitNamesInDB();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_first_page);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Button createAccountButton = (Button) findViewById(R.id.createAccountButton);
        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText userInput = (TextInputEditText) findViewById(R.id.username_input);
                String inputText = String.valueOf(userInput.getText());
                if (!inputText.equals("")) {
                    Intent toMapsIntent = new Intent(getApplicationContext(), MapsActivity.class);
                    toMapsIntent.putExtra("username", inputText);
                    startActivity(toMapsIntent);
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
                        namesInDB.add(d.getKey());
                    }
                }
            }
        });
    }


    }
