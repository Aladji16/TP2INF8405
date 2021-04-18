package com.example.tp2inf8405;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileActivity extends AppCompatActivity {
    private FirebaseDatabase dbRootNode = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = dbRootNode.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        String currentUsername = getIntent().getStringExtra("currentUsername");
        String currentUserKey = getIntent().getStringExtra("currentUserKey");

        TextView profileUsername = findViewById(R.id.profileUsername);
        profileUsername.setText(profileUsername.getText() + currentUsername);

        ImageView profilePicture = findViewById(R.id.configProfilePicture);
        dbRef = dbRootNode.getReference("accounts/" + currentUserKey);
        dbRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
              @Override
              public void onComplete(@NonNull Task<DataSnapshot> task) {
                  if (!task.isSuccessful()) {
                      Log.e("firebase", "Error getting data", task.getException());
                  } else {
                      String b64ProfilePicture = task.getResult().child("profilePicture").getValue().toString();
                      byte[] decodedString = Base64.decode(b64ProfilePicture, Base64.DEFAULT);
                      Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0,decodedString.length);
                      profilePicture.setImageBitmap(decodedByte);
                  }
              }
        });

    }
}