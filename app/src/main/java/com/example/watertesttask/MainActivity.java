package com.example.watertesttask;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String FILE_NAME = "glasses_count.json";
    private static final String CHANEL_ID = "my_channel_01";
    private Button applyBtn;
    private EditText amountOfGlasses;


    private static final String TAG = "MainActivity";
    private static final String NameForJSONObject = "count";

    private Uri filePath;
    JSONObject glassesCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Request permissions
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        Toast.makeText(MainActivity.this, getString(R.string.permission_denyed), Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();

        //Init views
        applyBtn = findViewById(R.id.apply_btn);
        amountOfGlasses = findViewById(R.id.amount_of_glasses);

        applyBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        glassesCount = createWaterJSON();  //Glasses amount added to a JSON Object

        uploadToServer(); // Upload JSON file to firebase storage
    }

    //Create JSON file
    public JSONObject createWaterJSON() {

        JSONObject glassesCount = new JSONObject();

        try {
            glassesCount.put(NameForJSONObject, amountOfGlasses.getText().toString());

            // Define the File Path and its Name
            File file = new File(this.getFilesDir(), FILE_NAME);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(glassesCount.toString());
            bufferedWriter.close();

            //Save file path
            filePath = Uri.fromFile(file);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return glassesCount;
    }

    //This method will upload the file
    private void uploadToServer() {
        //if there is a file to upload
        if (filePath != null) {

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReferenceFromUrl("gs://watertesttask.appspot.com/");

            // Create a reference to "file"
            StorageReference storageReference = storageRef.child(FILE_NAME);

            //Upload file to firebase storage
            storageReference.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            showNotification();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.i(TAG, "Here");
                        }
                    });
        }
        //if there is not any file
        else {
            Log.i(TAG, "Error");
            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
        }
    }

    private void showNotification() {

        int NOTIFICATION_ID = 234;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            CharSequence name = "my_channel";
            String Description = "This is my channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Glasses amount")
                .setContentText("Successfully uploaded " + amountOfGlasses.getText().toString() + " glasses")
                .setPriority(NotificationCompat.PRIORITY_HIGH);


        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
