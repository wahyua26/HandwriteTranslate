package com.example.imagerecog;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends AppCompatActivity {

    private Button inputImage;
    private Button recognizeText;
    private ImageView image;
    private EditText recognizedText;
    private static final String TAG = "MAIN_TAG";
    private Uri imageUri = null;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;
    private String[] cameraPermissions;
    private String[] storagePermissions;
    private TextRecognizer textRecognizer;
    private Translator translator;
    private ProgressBar progressBar;
    private Button btnTranslate;
    private EditText editText;
    private ImageButton btnAbout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputImage = findViewById(R.id.btnInputImage);
        recognizeText = findViewById(R.id.btnRecognize);
        image = findViewById(R.id.ivImage);
        recognizedText = findViewById(R.id.etRecognizedText);

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        inputImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }
        });

        recognizeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri == null){
                    Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_LONG).show();
                }
                else{
                    RelativeLayout layout = findViewById(R.id.parentRelative);

                    progressBar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleLarge);
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);
                    layout.addView(progressBar, params);
                    progressBar.setVisibility(View.VISIBLE);

                    recognizeTextFromImage();
                }
            }
        });

        btnTranslate = findViewById(R.id.btnTranslate);
        editText = findViewById(R.id.editText);
        btnTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(recognizedText.getText().toString())){
                    Toast.makeText(MainActivity.this, "No text allowed", Toast.LENGTH_SHORT).show();
                }
                else{
                    TranslatorOptions options = new TranslatorOptions.Builder()
                            .setTargetLanguage("id")
                            .setSourceLanguage("en")
                            .build();

                    translator = Translation.getClient(options);

                    String sourceText = recognizedText.getText().toString();

                    RelativeLayout layout = findViewById(R.id.deltaRelative);

                    progressBar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleLarge);
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);
                    layout.addView(progressBar, params);
                    progressBar.setVisibility(View.VISIBLE);
                    btnTranslate.setEnabled(false);

                    translator.downloadModelIfNeeded().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {

                            progressBar.setVisibility(View.GONE);
                            btnTranslate.setEnabled(true);
                            translateText(sourceText);

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });

        btnAbout = findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, About.class));
            }
        });
    }

    private void recognizeTextFromImage(){
        Log.d(TAG,"recognizeTextFromImage: ");

        try {
            InputImage inputImage = InputImage.fromFilePath(this,imageUri);
            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            progressBar.setVisibility(View.GONE);
                            String recogText = text.getText();
                            Log.d(TAG, "onSuccess: recognizedText: "+recogText);
                            recognizedText.setText(recogText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Log.d(TAG, "onFailure: ",e);
                            Toast.makeText(MainActivity.this,"Failed recognizing text due to "+e.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e){
            progressBar.setVisibility(View.GONE);
            Log.d(TAG, "recognizeTextFromImage: ",e);
            Toast.makeText(this, "Failed preparing image due to "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void translateText(String sourceText){
        Task<String> result = translator.translate(sourceText)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        editText.setText(s);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, "Wait for the Translator Model", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void showInputImageDialog(){
        PopupMenu popupMenu = new PopupMenu(this, inputImage);

        popupMenu.getMenu().add(Menu.NONE, 1,1,"CAMERA");
        popupMenu.getMenu().add(Menu.NONE,2,2,"GALLERY");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if(id == 1){
                    Log.d(TAG, "onMenuItemClick: Camera Cliked...");
                    if(checkCameraPermission()){
                        pickImageCamera();
                    }
                    else{
                        requestCameraPermission();
                    }
                }
                else if (id == 2){
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked...");
                    if(checkStoragePermission()){
                        pickImageGallery();
                    }
                    else{
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });
    }

    private void pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK){
                                Intent data = result.getData();
                                imageUri = data.getData();
                                Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                                image.setImageURI(imageUri);
                            }
                            else {
                                Log.d(TAG, "onActivityResult: cancelled");
                                Toast.makeText(MainActivity.this,"Cancelled...", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                        image.setImageURI(imageUri);
                    }
                    else {
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled...",Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return  result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return  cameraResult && storageResult;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);

        switch(requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted && storageAccepted){
                        pickImageCamera();
                    }
                    else{
                        Toast.makeText(this, "Camera & Storage permissions are required", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length>0){

                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(storageAccepted){
                        pickImageGallery();
                    }
                    else{
                        Toast.makeText(this, "Storage permissions are required", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                }
            }
            break;
        }
    }
}