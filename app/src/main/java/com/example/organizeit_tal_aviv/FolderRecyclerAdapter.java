package com.example.organizeit_tal_aviv;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class FolderRecyclerAdapter extends RecyclerView.Adapter<FolderRecyclerAdapter.MyViewHolder> {

    ArrayList<Folder> folders; // מחזיק את התיקיות של משתמש
    Context context; // מאיפה באנו
    FirebaseAuth mAuth; // משמש לאוטנטיקציה עם פיירבייס
    FirebaseDatabase database; // כדי ליצור ref למיקום מסוים ב- real time
    DatabaseReference myRef; //  הפניה למיקום מסוים ב- real time
    HashMap<Long, String> foldersNameDate; // מילון בו המפתח הוא זמן העלאת התיקייה והערך הוא שם התיקייה
    StorageReference storageReference; // משמש לגישה ל- storage
    private RecyclerViewClickListener listener; // הגישה למימוש האינטרפייס
    ProgressDialog progressDialog; // משמש ליצירת חלונית טעינה כאשר מוחקים תיקייה
    Boolean alreadyPressed;
    ArrayList<Image> images; // מערך התמונות בתיקייה. נמצא פה כדי שנוכל למחוק אותן במידת הצורך.
    WelcomeActivity welcomeActivity; // כדי לגשת למשתנים בוולקאם

    public FolderRecyclerAdapter(Context context, ArrayList<Folder> folders, HashMap<Long, String> foldersNameDate, RecyclerViewClickListener listener, WelcomeActivity welcomeActivity) {
        this.folders = folders;
        this.context = context;
        this.foldersNameDate = foldersNameDate;
        this.listener = listener;
        organizeFolders(); // סידור התיקיות לפי זמן העלאה
        images = new ArrayList<Image>();
        this.welcomeActivity = welcomeActivity;
        alreadyPressed = false;
        initViews();
    }

    // בגדול ViewHolder מחזיק נתונים אודות Item view ספציפי ומיקומו ב- recyclerView
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // לקיחת XML והמרתו לאובייקט מסוג View
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.folder_row, parent, false);
        //-------------------------------------------------------
        return new MyViewHolder(view);
    }

    private void organizeFolders() {
        // מיון התיקיות לפי זמן העלאה. שימוש ב- compare שיצרנו בקלאס FolderDateSort
        Collections.sort(folders, new FolderDateSort());
    }

    // אתחול המשתנים הקשורים בפייר-בייס
    private void initViews() {
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid());
    }

    // מיועד להכנסת מידע ל- View של Item ספציפי
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Folder f = folders.get(position); // מביא תיקייה במיקום מסויים
        holder.title.setText(foldersNameDate.get(f.getUploadDate()));
        holder.amount.setText(String.valueOf(f.getImages_counter()));
        // כפתור מחיקת תיקייה
        holder.btnRemoveFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog.Builder deleteFolderDialog = new AlertDialog.Builder(v.getContext()); // יוצר חלונית קופצת
                deleteFolderDialog.setTitle("Are you sure you want to delete the folder?\n");
                deleteFolderDialog.setMessage("All folder contents will be permanently deleted.");

                // מה שקורה אם לוחצים על yes
                deleteFolderDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (checkConnection()) {
                            // הצגת חלון טעינה
                            progressDialog = new ProgressDialog(context);
                            progressDialog.setTitle("deleting folder...");
                            progressDialog.show();
                            // שם התיקייה למחיקה
                            String folderKey = foldersNameDate.get(f.getUploadDate());
                            // מחיקת תיקייה מהסטוראג'
                            deleteFolder(folderKey, images);
                            // מחיקה מה- RealTime
                            myRef.child(folderKey).removeValue();
                            // אתחול כדי שפעם הבאה בלחיצה ארוכה יתייחס לזה כאילו לא נלחץ כלום לפני
                            welcomeActivity.btnDelPosition = -1;
                            // מעלים את חלונית הטעינה של המחיקה
                            progressDialog.dismiss();
                        }
                        else
                        {
                            Toast.makeText(context, "No internet connection!", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                // מה שקורה אם לוחצים על no
                deleteFolderDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // לא קורה כלום ופשוט נסגר החלון
                    }
                });

                deleteFolderDialog.create().show(); // הצגת החלונית
            }
        });
    }

    // בודק חיבור לרשת
    private boolean checkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    // מחיקת תיקייה מהסטוראג'
    private void deleteFolder(String folderKey, ArrayList<Image> images) {
        alreadyPressed = false;
        // משיכת התמונות הנמצאות בתיקייה מסויימת על מנת שנוכל למחוק אותן
        myRef.child(folderKey).child("images").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (alreadyPressed) return;
                alreadyPressed = true;
                images.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    images.add(ds.getValue(Image.class)); // יצירת מערך תמונות הנמצאות בתיקייה כדי שנוכל לעבור עליהם כדי למחוק אותן
                }
                deleteData(folderKey); // מחיקת התמונות. שלחנו את שם התיקייה בשביל ההפניה לסטורג'
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context,error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    // מחיקת התמונות של תיקייה מסוימת מהסטורג'
    private void deleteData(String folderKey) {
        for (int i = 0; i < images.size(); i++) {
            storageReference = FirebaseStorage.getInstance().getReference().child(mAuth.getUid()).child(folderKey).child(images.get(i).getName());
            storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
        }
    }

    // כמה פריטים יש לנו
    @Override
    public int getItemCount() {
        return folders.size();
    }

    // ממשק שנוכל לממש את האירוע לחיצה דרך welcome
    public interface RecyclerViewClickListener {
        void onClick(View v, int position);
    }


    // הקלאס של Item ספציפי
    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView title, amount;
        ImageView myImage;
        ImageView btnRemoveFolder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            amount = itemView.findViewById(R.id.amount);
            myImage = itemView.findViewById(R.id.myImageView);
            btnRemoveFolder = itemView.findViewById(R.id.btnDeleteFolder);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        // לחיצה על Item ספציפי
        @Override
        public void onClick(View v) {
            // אם אנחנו לא במצב בו מוצג כפתור המחיקה
            if (welcomeActivity.btnDelPosition == -1) {
                // אירוע לחיצה על תיקייה
                listener.onClick(itemView, getAdapterPosition());
            } else {
                // אם מוצג כפתור מחיקה באחד הפריטים, להעלים אותו
                welcomeActivity.prevView.findViewById(R.id.btnDeleteFolder).setVisibility(View.GONE);
                welcomeActivity.btnDelPosition = -1;
            }
        }

        // לחיצה ארוכה על Item ספציפי
        @Override
        public boolean onLongClick(View v) {
            // אם אין כפתור מחיקה שכבר מוצג, תציג כפתור מחיקה ב- Item עליו לחצנו
            if (welcomeActivity.btnDelPosition == -1) {
                welcomeActivity.prevView = v; // שמירת ה-item עליו לחצנו
                v.findViewById(R.id.btnDeleteFolder).setVisibility(View.VISIBLE);
                welcomeActivity.btnDelPosition = getAdapterPosition(); // שמירת המיקום של ה-item עליו לחצנו
            }
            // אם לחצנו שוב על אותו הפריט, להסיר את הכפתור
            else if (getAdapterPosition() == welcomeActivity.btnDelPosition) {
                v.findViewById(R.id.btnDeleteFolder).setVisibility(View.GONE);
                welcomeActivity.btnDelPosition = -1;
            }
            // אם יש פריט קודם שנלחץ ולחצנו על פריט אחר, להעלים את הכפתור מהפריט הקודם ולהציג את הכפתור בפריט הנוכחי
            else {
                welcomeActivity.prevView.findViewById(R.id.btnDeleteFolder).setVisibility(View.GONE); // העלמת הכפתור מהקודם
                welcomeActivity.btnDelPosition = getAdapterPosition(); // שמירת המיקום החדש
                welcomeActivity.prevView = v; // שמירת ה-view הנוכחי
                v.findViewById(R.id.btnDeleteFolder).setVisibility(View.VISIBLE); // הצגת כפתור המחיקה ב-view הנוכחי
            }
            // מחזירים true כי ככה זה מבטל את הלחיצה הרגילה.
            return true;
        }
    }
}
