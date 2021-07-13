package com.example.organizeit_tal_aviv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette; // לשאול את רואי מה זה?????

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class ImageActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView mImageView; // התמונה בדגול
    private ProgressBar pb; // עיגול טעינה קטן
    ImageView btnDeleteImage, btnBackToGallery, btnShare; // כפתורים
    FirebaseAuth mAuth; // אוטנטיקציב פייר-בייס
    FirebaseDatabase database; //  כדי ליצור ref למיקום מסוים ב- real time
    DatabaseReference myRef; // הפניה ל-real time
    String imageName; // שם התמונה
    Boolean alreadyPressed = false;
    StorageReference storageReference; // הפניה לסטוראג'
    TextView imageDate, imageLocation; // טקסט ויו להצגת המיקום והתאריך
    LinearLayout screenDetails; // המסך ללחיצה עליו כדי להציג ולהתיר פרטים
    Boolean isDetailsVisible = false; // בוליאני כדי להציג ולהסתיר את הפרטים
    FrameLayout imageDetails, imageDetailsTop; // הפסים עליהם נמצא המידע
    Bitmap imageBitmap; // כדי שנוכל לעשות שיתוף לתמונה
    String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        // בדיקה אם יש תקשורת
        if (!checkConnection()) {
            Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
        }

        initViews(); // אתחול המשתנים
        initImageDetails(); // אתחול הפרטים על התמונה
        initButtons(); //אתחול הכפתורים
        showImage(); // הצגת התמונה ע"י גלייד
    }

    // בדיקת רשת
    private boolean checkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // אתחול המשתנים
    private void initViews() {
        imageDate = findViewById(R.id.imageDate);
        imageLocation = findViewById(R.id.imageLocation);
        mImageView = (ImageView) findViewById(R.id.image);
        pb = findViewById(R.id.progress_bar);
        btnShare = findViewById(R.id.btnShare);
        btnDeleteImage = findViewById(R.id.btnDeleteImage);
        btnBackToGallery = findViewById(R.id.backToGallery);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        // הפנייה לתיקייה מסויימת
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child(getIntent().getStringExtra("folderName"));
        imageDetails = findViewById(R.id.imageDetails);
        imageDetailsTop = findViewById(R.id.imageDetailsTop);
        screenDetails = findViewById(R.id.screenDetails);
    }

    // אתחול פרטי תמונה
    private void initImageDetails() {
        imageUrl = getIntent().getStringExtra("imageUrl"); // אתחול הקישור של התמונה כדי לממש בגלייד
        imageName = getIntent().getStringExtra("imageName"); // שליפת שם התמונה כדי להוציא את התאריך
        Long d = Long.parseLong(imageName.substring(0, imageName.length() - 4)); // קיצוץ .jpg כדדי שיהיה לנו את ה- TimeStamp
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); // יצירת פורמט תאריך
        imageLocation.setText(getIntent().getStringExtra("imageLocation")); // הצגת המיקום
        imageDate.setText(formatter.format(new Date(d))); // הצגת התאריך בפורמט שרצינו
    }

    // אתחול הכפתורים
    private void initButtons() {
        btnShare.setOnClickListener(this);
        btnDeleteImage.setOnClickListener(this);
        btnBackToGallery.setOnClickListener(this);
    }

    // הצגת התמונה
    private void showImage() {
        Glide.with(this)
                .load(imageUrl)// טען את התמונה מה- url
                .asBitmap()
                .error(R.drawable.ic_launcher_background)
                .listener(new RequestListener<String, Bitmap>() {

                    @Override
                    public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }
                    // כאשר התמונה מוכנה
                    @Override
                    public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        pb.setVisibility(View.GONE); // להעלים את עיגול הטעינה
                        setScreenClick(); // אתחל אירוע לחיצה על המסך(נמצא פה כדי שלא יוכלו לראות את הפרטים לפני שהתמונה נטענה
                        imageBitmap = resource; // שמירת ה-BitMap כדי שנוכל לשתף את התמונה אחר כך
                        return false;
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(mImageView); // טעינת התמונה לתוך ה-ImageView
    }


    // אתחול אירוע לחיצה על המסך
    private void setScreenClick() {
        screenDetails.setOnClickListener(this); // יצא לפונקציה כדי שיהיה גישה ל-this
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDeleteImage:
                if (checkConnection()) {
                    deleteDialog();
                } else {
                    Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.screenDetails:
                handleImageDetails();
                break;
            case R.id.backToGallery:
                backToFolder();
                break;
            case R.id.btnShare:
                if (imageBitmap != null) {
                    shareImage();
                }
                break;
        }
    }

    // חלונית האם למחוק
    public void deleteDialog() {
        final AlertDialog.Builder deleteFolderDialog = new AlertDialog.Builder(ImageActivity.this); // יוצר חלונית קופצת
        deleteFolderDialog.setTitle("Are you sure you want to delete the image?\n");

        // מה שקורה אם לוחצים על yes
        deleteFolderDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteImage(); // מחיקת תמונה
            }
        });

        // מה שקורה אם לוחצים על no
        deleteFolderDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        deleteFolderDialog.create().show(); // הצגת החלונית
    }

    // מחיקת התמונה
    private void deleteImage() {
        // מחיקה מהסטוראג'
        storageReference = FirebaseStorage.getInstance().getReference().child(mAuth.getUid()).child(getIntent().getStringExtra("folderName")).child(imageName);
        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        });

        alreadyPressed = false;
        // מחיקה מה - realtime
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (alreadyPressed) return;
                alreadyPressed = true;
                Folder folder = snapshot.getValue(Folder.class); // שליפת התיקיה הנוכחית
                folder.removeImage(imageName); // מחיקת התמונה מהתיקייה הנוכחית
                myRef.setValue(folder, new DatabaseReference.CompletionListener() { // דריסת התיקייה הקיימת בתיקייה המעודכנת
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        Intent intent = new Intent(getApplicationContext(), FolderActivity.class); // לשאול את רואי האם אפשר להשתמש במיין נקודה דיס במקום הפרמטר הראשון
                        intent.putExtra("FolderName", getIntent().getStringExtra("folderName"));
                        startActivity(intent); // מעבר חזרה לתיקייה
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(),error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    // הצגה והסתרה של פרטי תמונה וכפתורים
    private void handleImageDetails() {
        if (!isDetailsVisible) {
            imageDetails.setVisibility(View.VISIBLE);
            imageDetailsTop.setVisibility(View.VISIBLE);
            isDetailsVisible = true;
        } else {
            imageDetails.setVisibility(View.GONE);
            imageDetailsTop.setVisibility(View.GONE);
            isDetailsVisible = false;
        }
    }

    // לחזור לתיקייה
    public void backToFolder() {
        Intent intent = new Intent(getApplicationContext(), FolderActivity.class);
        intent.putExtra("FolderName", getIntent().getStringExtra("folderName"));
        startActivity(intent);
    }

    // שיתוף התמונה
    private void shareImage() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        // בדיקה של גרסה 10 +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                shareIntent.putExtra(Intent.EXTRA_STREAM, saveBitmap(this, imageBitmap,Bitmap.CompressFormat.JPEG,"image/jpeg",imageUrl));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else // עבור גרסאות נמוכות מ-10
        {
            shareIntent.putExtra(Intent.EXTRA_STREAM, getImageUri(this, imageBitmap));
        }        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, null));
    }

    // המרה מ-BitMap ל-Uri כדי שנוכל לשתף
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    // המרה מ-BitMap ל-Uri כדי שנוכל לשתף בגרסאות 10 ומעלה
    @Nullable
    private Uri saveBitmap(@NonNull final Context context, @NonNull final Bitmap bitmap,
                           @NonNull final Bitmap.CompressFormat format, @NonNull final String mimeType,
                           @NonNull final String displayName) throws IOException
    {
        final String relativeLocation = Environment.DIRECTORY_DCIM;

        final ContentValues  contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation);

        final ContentResolver resolver = context.getContentResolver();

        OutputStream stream = null;
        Uri uri = null;

        try
        {
            final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = resolver.insert(contentUri, contentValues);

            if (uri == null)
            {
                throw new IOException("Failed to create new MediaStore record.");
            }

            stream = resolver.openOutputStream(uri);

            if (stream == null)
            {
                throw new IOException("Failed to get output stream.");
            }

            if (bitmap.compress(format, 95, stream) == false)
            {
                throw new IOException("Failed to save bitmap.");
            }
        }
        catch (IOException e)
        {
            if (uri != null)
            {
                // Don't leave an orphan entry in the MediaStore
                resolver.delete(uri, null, null);
            }

            throw e;
        }
        finally
        {
            if (stream != null)
            {
                stream.close();
            }
        }

        return uri;
    }

    @Override
    public void onBackPressed() {
        backToFolder();
    }

}