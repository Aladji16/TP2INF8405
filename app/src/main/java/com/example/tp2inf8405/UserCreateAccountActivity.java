package com.example.tp2inf8405;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;

import static com.example.tp2inf8405.UserFirstPageActivity.con;
import static com.example.tp2inf8405.UserFirstPageActivity.namesInDB;

public class UserCreateAccountActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private FirebaseDatabase dbRootNode = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = dbRootNode.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Set to app locale from config
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        res.updateConfiguration(con, dm);

        setContentView(R.layout.activity_create_account);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Button validateUsername = (Button) findViewById(R.id.validate_username);
        Button changePicture = (Button) findViewById(R.id.change_pfp);



        //https://developer.android.com/training/camera/photobasics
        changePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int permissionCheck1 = ContextCompat.checkSelfPermission(UserCreateAccountActivity.this, Manifest.permission.CAMERA);
                if (permissionCheck1 != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(UserCreateAccountActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
                }

                else {
                    takePicture();
                }
            }
        });


        validateUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextInputEditText userInput = (TextInputEditText) findViewById(R.id.username_input);
                String inputText = String.valueOf(userInput.getText());
                if (!inputText.equals(""))
                {
                    if (namesInDB.contains(inputText))
                    {
                        String toastText = getString(R.string.userAlreadyInDB);
                        Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
                        toast.show();
                    }

                    else
                    {
                        // Creer le user
                        namesInDB.add(inputText);
                        dbRef = dbRootNode.getReference("accounts").push();
                        dbRef.child("username").setValue(inputText);
                        // Envoyer photo de profil à la base de données
                        ImageView profilePicture = (ImageView) findViewById(R.id.profilePicture);
                        Bitmap bitmap = ((BitmapDrawable) profilePicture.getDrawable()).getBitmap();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        String b64profilePicture = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                        dbRef.child("profilePicture").setValue(b64profilePicture);

                        // Ramener vers le login
                        Intent backToLoginIntent = new Intent(getApplicationContext(), UserFirstPageActivity.class);
                        backToLoginIntent.putExtra("newAccountUsername", inputText);
                        startActivity(backToLoginIntent);
                    }
                }
            }
        });

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
        super.onActivityResult(requestCode, resultCode, data);
        ImageView profilePicture = (ImageView) findViewById(R.id.profilePicture);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            profilePicture.setImageBitmap(imageBitmap);
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
                    takePicture();
                }  else {
                    String toastText = "Vous garderez la photo de profil par défaut.";
                    Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
                    toast.show();
                }
                return;
        }
    }

}



