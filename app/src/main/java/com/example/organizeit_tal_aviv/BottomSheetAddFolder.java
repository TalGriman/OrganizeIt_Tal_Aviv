package com.example.organizeit_tal_aviv;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BottomSheetAddFolder extends BottomSheetDialogFragment implements View.OnClickListener {
    Context context;  // מאיפה הגענו (welcome)
    FirebaseAuth mAuth; // לתקשורת עם אוטנטיקציה של פיירבייס
    FirebaseDatabase database; // משמש ליצירת הפניה ל- real time
    DatabaseReference myRef; // הפניה ל- real time
    View view; // מיועד לתפיסת החלונית הוספת תיקייה
    Button btnAdd; // כפתור הוספת תיקייה הנמצא התוך חלונית ההוספה
    EditText folderName; // אינפוט שם התיקייה
    Boolean alreadyPressed = false;


    public BottomSheetAddFolder(Context context) {
        this.context = context;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true); // על מנת שלא יקרוס בסיבוב ליתר בטחון

        initViews(inflater, container); // אתחול המשתנים

        initButtons(); // אתחול הכפתורים

        return view;
    }


    // אתחול המשתנים
    private void initViews(LayoutInflater inflater, ViewGroup container) {
        view = inflater.inflate(R.layout.row_add_folder, container, false); // לוקח את ה- XML של החלונית הקופצת להוספת תיקייה
        btnAdd = view.findViewById(R.id.btnAdd);
        folderName = view.findViewById(R.id.txtFolderName); // אינפוט של שם התיקייה
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()); // הפניה לכל התיקיות מתחת ליוזר מסוים
    }

    // אתחול הכפתורים
    private void initButtons() {
        btnAdd.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAdd:
                addFolderToDB();
                break;
        }
    }

    // בודק חיבור לאינטרנט
    private boolean checkConnection() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    // הוספת תיקייה לפיירבייס
    private void addFolderToDB() {
        if (checkConnection()) {
            // בדיקת תקינות שם התיקייה
            String pattern = "^[a-zA-Z0-9_!\\-]+([ ][a-zA-Z0-9_!\\-]+)*$"; // תבנית המאפשרת אותיות באנגלית, ספרות והסימנים: -_!
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(folderName.getText().toString());
            if (!m.matches()) {
                Toast.makeText(context, "Invalid folder name. Only English letters, digits or the following characters: !-_  and space between them allowed.", Toast.LENGTH_LONG).show();
                return;
            }
            // -----------------------
            alreadyPressed = false;
            Folder newFolder = new Folder(0, new ArrayList<Image>(), System.currentTimeMillis()); // יצירת אובייקט התיקייה החדשה
            // בדיקת כפילות שם
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (alreadyPressed) return;
                    alreadyPressed = true;
                    for (DataSnapshot dp : snapshot.getChildren()) {
                        // מניעת כפילות שם תיקייה
                        if (dp.getKey().toUpperCase().equals(folderName.getText().toString().toUpperCase())) {
                            Toast.makeText(context, "This folder is already exists!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    myRef.child(folderName.getText().toString()).setValue(newFolder); // הוספת תיקייה חדשה לפיירבייס
                    startActivity(new Intent(context, WelcomeActivity.class)); // חזרה ל- welcome
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(context, "No internet connection!", Toast.LENGTH_LONG).show();
        }
    }
}
