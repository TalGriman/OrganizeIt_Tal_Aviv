package com.example.organizeit_tal_aviv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.CaseMap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener {
    FirebaseAuth mAuth;  // אובייקט לתקשורת עם אוטנטיקציה של פיירבייס
    FirebaseDatabase database; // כדי ליצור ref למיקום מסוים ב- real time
    DatabaseReference myRef; // הפניה למיקום מסוים ב- real time
    RecyclerView recyclerView; // מיועד להצגה בצורה יעילה בה כאשר גוללים נטען עוד מידע
    FolderRecyclerAdapter folderRecyclerAdapter; // בעזרת אובייקט זה מכניסים את המידע ל- recycler view
    private FolderRecyclerAdapter.RecyclerViewClickListener listener;
    ExtendedFloatingActionButton btnAddFolder; // כפתור הוספה צף
    ProgressDialog progressDialog; // להצגת חלונית טעינת התיקיות
    ArrayList<Folder> folders; // מערך תיקיות המשתמש
    HashMap<Long, String> foldersNameDate; // מילון של שמות התיקיות כערך ותאריך העלאת התיקייה כמפתח
    int btnDelPosition; // שומר את אינדקס הכפתור עליו לחצנו לחיצה ארוכה
    View prevView; // ה- view הקודם
    TextView no_folders; // טקסט למקרה שעדיין אין תמונות
    ImageView btnLogout; // כפתור התנתקות


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // במידה ואין אינטרנט אז יציג הודעה
        if (!checkConnection()) {
            Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
        }

        // אתחול המשתנים
        initViews();

        // אתחול הכפתורים
        initButtons();

        // הצגת חלונית טעינת תיקיות
        loadDataDialog();

        // קבלת התיקיות של משתמש מפיירבייס
        getDataFromFirebase();

        // שינוי נראות הכפתור הצף בגלילה
        setUpFab();
    }

    // בודק האם יש רשת
    private boolean checkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo(); // מידע על הרשת
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting(); // בדיקה האם אנו מחוברים
    }

    // אתחול משתנים
    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        no_folders = findViewById(R.id.no_folders);
        // הפניה לכל התיקיות של משתמש מסוים
        myRef = database.getReference("Users").child(mAuth.getUid());
        btnAddFolder = findViewById(R.id.btnAddFolder);
        // אתחול ה- recyclerView
        recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        // ----------------------
        folders = new ArrayList<>(); // מערך תיקיות
        foldersNameDate = new HashMap<Long, String>(); // מפתח טיים-סטאמפ של זמן העלאה ערך הוא שם התיקיה
        btnDelPosition = -1; // משתנה עזר כדי להחליט מה לעשות בלחיצה ארוכה על תיקיה(מחיקה לדוגמה)
    }

    // אתחול אירוע לחיצה על כפתורים
    private void initButtons() {
        btnAddFolder.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
    }

    // יצירה והצגת חלונית טעינת תיקיות
    private void loadDataDialog() {
        progressDialog = new ProgressDialog(WelcomeActivity.this,R.style.AppCompatAlertDialogStyle);
        progressDialog.setTitle("loading folders...");
        progressDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAddFolder: // הצגת חלונית הוספת תיקייה במידה ויש חיבור רשת
                showAddFolderWindow();
                break;
            case R.id.btnLogout:
                logOutDialog(); // חלונית האם אתה בטוח שאתה רוצה להתנתק
                break;
        }
    }

    // בלחיצה על כפתור הוספת תיקייה - פותח את חלונית ההוספה במידה ויש רשת
    private void showAddFolderWindow() {
        if (checkConnection()) {
            BottomSheetAddFolder bottomSheetAddFolder = new BottomSheetAddFolder(WelcomeActivity.this); // יצירת חלונית הוספת תיקייה
            bottomSheetAddFolder.show(getSupportFragmentManager(), "TAG"); // הצגת החלונית של הוספת תיקייה
        } else {
            Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
        }
    }

    // כפתור צף
    private void setUpFab() {
        // שינוי נראות הכפתור בגלילה
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // בגלילה למטה לסגור לפלוס, בגלילה למעלה לפתוח ולכתוב add folder
                if (dy > 0) {
                    btnAddFolder.shrink();
                } else {
                    btnAddFolder.extend();
                }
            }
        });
    }

    // שליפת התיקיות מפיירבייס
    private void getDataFromFirebase() {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                folders.clear(); // מנקה את התיקיות מלפני
                foldersNameDate.clear(); // מנקה את המילון של התיקיות מלפני
                // מעבר על תיקיות המשתמש והכנסתן למערך
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Folder f = ds.getValue(Folder.class);
                    folders.add(f); // כאן שלפנו את התיקיות בשלמותן (תמונות וקאונטר הסופר כמה תמונות יש בתיקייה)
                    foldersNameDate.put(f.getUploadDate(), ds.getKey()); // מילון עם מפתח - תאריך העלאת התיקייה וערך - שם התיקייה
                }
                setOnClickListener(); // בלחיצה על תיקייה, מעבר לתוך התיקייה.
                // עדכון ה- recycler view
                folderRecyclerAdapter = new FolderRecyclerAdapter(WelcomeActivity.this, folders, foldersNameDate, listener, WelcomeActivity.this);
                recyclerView.setAdapter(folderRecyclerAdapter); // ממלא בתיקיות את הריסייקלר-ויו
                folderRecyclerAdapter.notifyDataSetChanged(); // מודיע לריסייקלר-ויו שמשהו השתנה
                progressDialog.dismiss(); // העלמת חלונית טעינת תיקיות
                if (folders.size() > 0) { // אם יש לנו תיקיות
                    no_folders.setVisibility(View.GONE); // להעלים את הטקסט שאומר "No folders yet"
                } else {
                    no_folders.setVisibility(View.VISIBLE); // להציג את הכיתוב
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // בלחיצה על תיקייה, לעבור לתוך התיקייה ולשלוח לה את שם התיקייה
    // בישטה זו משתמשים באינטרפייס הנותן גישה למיקום של התיקיה
    private void setOnClickListener() {
        listener = new FolderRecyclerAdapter.RecyclerViewClickListener() {
            @Override
            public void onClick(View v, int position) {
                Intent intent = new Intent(getApplicationContext(), FolderActivity.class); // לשאול את רואי האם אפשר להשתמש במיין נקודה דיס במקום הפרמטר הראשון
                Folder f = folders.get(position);
                intent.putExtra("FolderName", foldersNameDate.get(f.getUploadDate()));
                startActivity(intent);
            }
        };
    }

    // בלחיצה על back התנתקות
    @Override
    public void onBackPressed() {
        logOutDialog();
    }

    // חלונית אתה בטוח שאתה רוצה להתנתק
    public void logOutDialog() {
        final AlertDialog.Builder deleteFolderDialog = new AlertDialog.Builder(WelcomeActivity.this); // יוצר חלונית קופצת
        deleteFolderDialog.setTitle("Are you sure you want to logout?\n");

        // מה שקורה אם לוחצים על yes
        deleteFolderDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logOut();
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

    // התנתקות
    private void logOut() {
        if (LoginManager.getInstance() != null) { // מנתק את הפייסבוק
            LoginManager.getInstance().logOut(); // התנתקות שבלחיצה על back לא יבקש שוב אוטנטיקציה של פייסבוק
        }
        mAuth.signOut();
        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
    }
}