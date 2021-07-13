package com.example.organizeit_tal_aviv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FolderActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final int PERMISSION_CAMERA = 1; // הרשאות
    private static final int REQUEST_IMAGE_CAPTURE = 2; // צילום תמונה
    ImageView addPhoto; // כפתור אייקון המצלמה
    StorageReference storageReference; // הפניה לסטורג'
    FirebaseAuth mAuth; // אוטנטיקציה עם פיירבייס
    FirebaseDatabase database; // משמש ליצירת הפניה ל- real time
    DatabaseReference myRef; // הפניה ל- real time
    DatabaseReference myRefFolderImages; // הפניה ישירות למערך התמונות בתיקייה
    Boolean alreadyPressed = false;
    RecyclerView.LayoutManager layoutManager; // כדי שנוכל לסדר את ה- recyclerview בשתי עמודות
    RecyclerView recyclerView; // תופס את כל הרסייקלר
    ImageGalleryRecyclerAdapter adapter; // נותן גישה לקלאס בו שולטים על כל תמונה בנפרד
    ArrayList<Image> images; // מערך התמונות בתיקייה
    String currentPhotoPath; // כתובת התמונה בפלאפון
    // multi delete toolbar
    private Toolbar toolbar; // הסרגל למעלה
    boolean isContextualModeEnable = false; // בוליאני לבדיקה האם הסרגל במצב בו מופיעה אופציה למחיקה
    ArrayList<Image> selectionList; // מערך התופס את כל התמונות שסומנו בצ'ק בוקס
    int selectCounter = 0; // סופר כמה תמונות סומנו
    TextView itemCounter; // מציג כמה פריטים (תמונות) נבחרו
    TextView no_images; // טקסט שיוצג במידה ואין תמונות
    FusedLocationProviderClient fusedLocationProviderClient; // למיקום
    String imageLocation = ""; // טקסט של שם המיקום


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

        // לבדוק האם יש אינטרנט ולהציג הודעה במידה ואין
        if (!checkConnection())
        {
            Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
        }

        initViews();

        // multi delete toolbar
        initMultiDeleteToolBar();

        initButton();

        getDataFromFirebase();
    }

    // אתחול המשתנים
    private void initViews() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this); // קשור למיקום
        selectionList = new ArrayList<Image>(); // מערך של תמונות שסומנו
        itemCounter = findViewById(R.id.itemCounter);
        no_images = findViewById(R.id.no_images);
        addPhoto = findViewById(R.id.AddPhoto);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child(getIntent().getStringExtra("FolderName"));
        myRefFolderImages = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child(getIntent().getStringExtra("FolderName")).child("images");
        images = new ArrayList<Image>(); // מערך של תמונות התיקייה
        // אתחול הריסייקלר וויוו
        layoutManager = new GridLayoutManager(this, 2); // על מנת שיהיו שתי עמודות בריסייקלר
        recyclerView = (RecyclerView) findViewById(R.id.rv_images);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ImageGalleryRecyclerAdapter(this, images, getIntent().getStringExtra("FolderName"), FolderActivity.this);
        recyclerView.setAdapter(adapter);
        //---------------------------------------------
    }

    // יצירת הסרגל למעלה
    private void initMultiDeleteToolBar() {
        toolbar = findViewById(R.id.toolBar);
        toolbar.setBackgroundColor(getColor(R.color.blue1));
        setSupportActionBar(toolbar);
        itemCounter.setText("Folder: " + getIntent().getStringExtra("FolderName"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // מציג את החץ בתפריט העליון

    }

    // אתחול הכפתורים
    private void initButton() {
        addPhoto.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
            switch (v.getId()) {
                case R.id.AddPhoto:
                    takePhoto();
                    break;
            }
    }

    // בדיקת חיבור לרשת
    private boolean checkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }


    // העלאת תמונה במידה ויש רשת
    private void takePhoto() {
        if (checkConnection()) {
            // בקשת הרשאת מצלמה,מיקום ואחסון במידה ועדיין אין לנו
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CAMERA);
            } else {
                getLocation(); // לקבל מיקום
                dispatchTakePictureIntent(); // לקחת תמונה
            }
        } else {
            Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
        }
    }

    // מה יקרה אחרי קבלת ההראות או שלילתם
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "permission granted!", Toast.LENGTH_SHORT).show();
                getLocation();
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getApplicationContext(), "permission declined!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // קבלת מיקום נוכחי
    private void getLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // קבלת מיקום
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    if (location != null) {
                        try {
                            //כדי שיביא לנו את המיקום רק בשפה האנגלית
                            Locale aLocale = new Locale.Builder().setLanguage("en").setScript("Latn").setRegion("RS").build();
                            Geocoder geocoder = new Geocoder(FolderActivity.this, aLocale);
                            List<Address> addresses = geocoder.getFromLocation(
                                    location.getLatitude(), location.getLongitude(), 1
                            );
                            // יצירת סטרינג של המיקום לפי פרמטרים שהחלטנו
                            String countryName = addresses.get(0).getCountryName() != null ? addresses.get(0).getCountryName() : "";
                            String city = addresses.get(0).getLocality() != null ? ", " + addresses.get(0).getLocality() : "";
                            String adminArea = addresses.get(0).getAdminArea() != null ? ", " + addresses.get(0).getAdminArea() : "";
                            imageLocation = countryName + city + adminArea;
                            //------------------------------------------------------
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    // מטפל בצילום תמונה
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // יצירת קובץ תמונה
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_"; // יצירת שם התמונה
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    // ברגע שחוזר מהמצלמה
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(isContextualModeEnable) // אם לחצנו על המצלמה במצב של בחירה מרובה שיאפס למצב הרגיל
        {
            selectionList.clear();
            RemoveContextualActionMode();
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File f = new File(currentPhotoPath);
            uploadImage(Uri.fromFile(f)); // העלאת תמונה לפייר-בייס
        }
    }

    private void uploadImage(Uri imageUri) {
        ProgressDialog progressDialog = new ProgressDialog(FolderActivity.this,R.style.AppCompatAlertDialogStyle); // חלונית העלאת תמונה
        progressDialog.setTitle("Uploading image...");
        progressDialog.show();
        if (imageUri != null) {
            String imageName = System.currentTimeMillis() + ".jpg";
            // המיקום אליו התמונה תיכנס בסטוראג'
            storageReference = FirebaseStorage.getInstance().getReference().child(mAuth.getUid()).child(getIntent().getStringExtra("FolderName")).child(imageName);
            // הכנסת התמונה לסטוראג'
            storageReference.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String url = uri.toString();
                            addDataToDB(imageName, url); // הוספת התמונה ל - realtime
                            progressDialog.dismiss(); // הורדת חלונית הטעינה
                        }
                    });
                }
            });
        }
    }

    // הוספת תמונה ל-realtime
    private void addDataToDB(String imageName, String url) {
        alreadyPressed = false;
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (alreadyPressed) return;
                alreadyPressed = true;
                Image image = new Image(url, imageName, imageLocation); // יצירת אובייקט של התמונה החדשה
                Folder folder = snapshot.getValue(Folder.class); // שליפת התיקייה בשלמותה
                folder.addImage(image); // הוספת התמונה לתיקייה(שימוש בהוספת תמונה מקלאס -folder)
                myRef.setValue(folder); // דריסת התיקייה הקיימת בתיקייה החדשה
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(),error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    // קבלת התמונות של התיקייה
    private void getDataFromFirebase() {
        myRefFolderImages.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                images.clear(); // ניקוי המערך תמונות
                selectionList.clear(); // ניקוי סימון התמונות
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // יצירת מערך התמונות
                    images.add(ds.getValue(Image.class));
                }
                // טיפול בריסייקלר-ויו
                adapter = new ImageGalleryRecyclerAdapter(FolderActivity.this, images, getIntent().getStringExtra("FolderName"), FolderActivity.this);
                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                //----------------------------------------------------------------
                // אם יש תמונות להסתיר את המשפט אין תמונות אחרת להציג את המשפט
                if (images.size() > 0) {
                    no_images.setVisibility(View.GONE);
                } else {
                    no_images.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(),error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    // בלחיצה אחורה לחזור ל-welcome
    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, WelcomeActivity.class));
    }

    // ליצירת התפריט למעלה
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.normal_menu, menu);
        return true;
    }

    // בלחיצה ארוכה על תמונה מציג את מצב הבחירה
    @Override
    public boolean onLongClick(View v) {
        if (checkConnection()) {
            //משתנה שאומר לנו האם אנחנו בלחיצה ארוכה או לא כדי להציג או להסתיר
            //  את הצ'אק בוקסים ולאפשר או לא לאפשר אירוע לחיצה על תמונה כדי להציגה בגדול
            isContextualModeEnable = true;
            toolbar.getMenu().clear();// מנקה את הבר למעלה
            toolbar.inflateMenu(R.menu.folder_menu); // מציג את התפריט עם הפח
            getSupportActionBar().setTitle("0 Items selected");
            toolbar.setBackgroundColor(getColor(R.color.middle_blue_grey));
            adapter.notifyDataSetChanged(); // כדי שיציג את הצ'אק בוקסים
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    // מופעל מהאדפטר של התמונות
    // מטפל בבחירה
    public void makeSelection(View view, int adapterPosition) {
        // אם הצ'אק-בוקס מסומן
        if (((CheckBox) view).isChecked()) {
            selectionList.add(images.get(adapterPosition));
            selectCounter++;
        } else { // אם הצאק בוקס לא מסומן
            selectionList.remove(images.get(adapterPosition));
            selectCounter--;
        }
        itemCounter.setText(selectCounter + " Items selected");
    }

    // כמו אירוע לחיצה על item בתפריט למעלה
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.multiDelete) { // בלחיצה על הפח
            if (selectionList.size() > 0) { // אם נבחרו תמונות
                deleteDialog(); // חלונית אתה בטוח שאתה רוצה למחוק?
            }
        } else if (item.getItemId() == android.R.id.home) { // בלחיצה על חץ אחורה בתפריט העליון
            if (isContextualModeEnable) // אם אנחנו בבחירה ברובה
            {
                selectionList.clear();
                RemoveContextualActionMode(); // איפוס כל הדברים הקשורים לבחירה מרובה
            }
            else{ // אם אנחנו במצב תפריט רגיל
                startActivity(new Intent(FolderActivity.this,WelcomeActivity.class));
            }
        }
        return true;
    }

    public void deleteDialog() {
        final AlertDialog.Builder deleteFolderDialog = new AlertDialog.Builder(FolderActivity.this); // יוצר חלונית קופצת
        deleteFolderDialog.setTitle("Are you sure you want to delete the selected images?\n");

        // מה שקורה אם לוחצים על yes
        deleteFolderDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(checkConnection()) {
                    adapter.removeSelection(selectionList); // למחוק את התמונות הנבחרות
                }
                else
                {
                    selectionList.clear();
                    Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
                }
                RemoveContextualActionMode(); // איפוס כל הדברים הקשורים לבחירה מרובה
            }
        });

        // מה שקורה אם לוחצים על no
        deleteFolderDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectionList.clear(); // מאפס את הבחירות
                RemoveContextualActionMode(); // איפוס כל הדברים הקשורים לבחירה מרובה
            }
        });

        deleteFolderDialog.create().show(); // הצגת החלונית
    }

    // איפוס כל הדברים הקשורים לבחירה מרובה
    private void RemoveContextualActionMode() {
        isContextualModeEnable = false; // הפחכת המשתנה ל-false כלומר חזרנו למצב הרגיל
        itemCounter.setText("Folder: " + getIntent().getStringExtra("FolderName")); // משנים את הקאונטר לשם התיקייה
        toolbar.getMenu().clear(); // מנקה את התפריט
        toolbar.inflateMenu(R.menu.normal_menu); // שם את התפריט הדיפולטיבי(בלי הפח)
        selectCounter = 0;
        toolbar.setBackgroundColor(getColor(R.color.blue1));
        adapter.notifyDataSetChanged();
    }
}