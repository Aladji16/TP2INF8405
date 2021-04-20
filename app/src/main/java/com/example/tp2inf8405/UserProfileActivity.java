package com.example.tp2inf8405;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;

import static com.example.tp2inf8405.UserFirstPageActivity.con;

public class UserProfileActivity extends AppCompatActivity {
    private FirebaseDatabase dbRootNode = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = dbRootNode.getReference();

    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private String currentUsername;
    private String currentUserKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        // Set to app locale from config
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        res.updateConfiguration(con, dm);

        setContentView(R.layout.activity_user_profile);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        currentUsername = getIntent().getStringExtra("currentUsername");
        currentUserKey = getIntent().getStringExtra("currentUserKey");

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


        Button configChangePFP = (Button) findViewById(R.id.configChangePFP);
        configChangePFP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int permissionCheck1 = ContextCompat.checkSelfPermission(UserProfileActivity.this, Manifest.permission.CAMERA);
                if (permissionCheck1 != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(UserProfileActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
                }

                else {
                    takePicture();

                }
            }
        });


        ActionBar actionBar = getActionBar();

    }



    public void takePicture()
    {
        //https://developer.android.com/training/camera/photobasics
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //dans cette version du code on envoie directement la nouvelle image à la base de donnée
        super.onActivityResult(requestCode, resultCode, data);
        ImageView profilePicture = (ImageView) findViewById(R.id.configProfilePicture);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            profilePicture.setImageBitmap(imageBitmap);

            dbRef = dbRootNode.getReference("accounts/" + currentUserKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            String b64profilePicture = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            dbRef.child("profilePicture").setValue(b64profilePicture);
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        //https://stackoverflow.com/questions/42275906/how-to-ask-runtime-permissions-for-camera-in-android-runtime-storage-permissio
        //https://developer.android.com/training/camera/photobasics
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Log.d("ok","t'es un bon");
                    takePicture();
                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    String toastText = "Vous garderez la photo de profil par défaut.";
                    Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
                    toast.show();
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }


}