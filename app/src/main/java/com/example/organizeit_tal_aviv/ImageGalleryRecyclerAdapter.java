package com.example.organizeit_tal_aviv;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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

public class ImageGalleryRecyclerAdapter extends RecyclerView.Adapter<ImageGalleryRecyclerAdapter.MyViewHolder> {

    ArrayList<Image> images; // מערך התמונות
    Context mContext; // מאיפה הגענו
    String folderName; // שם התיקייה
    StorageReference storageReference; // הפניה לסטוראג'
    FirebaseAuth mAuth; // אוטנטיקציב של פייר-בייס
    DatabaseReference myRef;// הפניה ל- realtime
    boolean alreadyPressed = false;
    FolderActivity folderActivity; // משתנה שמאפשר גישה ל- FolderActivity

    public ImageGalleryRecyclerAdapter(Context context, ArrayList<Image> images, String folderName, FolderActivity folderActivity) {
        mContext = context;
        this.images = images;
        Collections.reverse(images); // היפוך התמונות כדי שיהיו לפי סדר העלאה, כלומר החדש למעלה
        this.folderName = folderName;
        this.folderActivity = folderActivity;
    }

    // בגדול ViewHolder מחזיק נתונים אודות Item view ספציפי ומיקומו ב- recyclerView
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // לקיחת XML והמרתו לאובייקט מסוג View
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View photoView = inflater.inflate(R.layout.image_item, parent, false);
        //----------------------------------------
        return new MyViewHolder(photoView, folderActivity);
    }

    // מיועד להכנסת מידע ל- View של Item ספציפי
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        String imageUrl = images.get(position).getUrl(); // הכתובת של התמונה
        ImageView imageView = holder.mPhotoImageView; // תמונה מסויימת
        ProgressBar pb = holder.pb; // עיגול טעינה קטן של כל תמונה
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child(folderName); // הפניה לתיקיה ספציפית
        pb.setVisibility(View.VISIBLE); // הצגת עיגול הטעינה הקטן
        // שימוש בספריית גלייד(קוד פתוח) אשר מאפשרת טעינת והצגת תמונות בצורה יעילה ומהירה מבחינת זכרון ומטמון
        Glide.with(mContext)
                .load(imageUrl)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }
                    // כאשר התמונה מוכנה להצגה
                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        pb.setVisibility(View.GONE); // להעלים את עיגול הטעינה
                        imageView.setVisibility(View.VISIBLE); // ולהציג את התמונה
                        return false;
                    }
                })
                //.placeholder(R.drawable.ic_launcher_background) -- השארנו לשימוש עתידי
                .into(imageView);
        // טיפול בצ'אק בוקסים
        if (!folderActivity.isContextualModeEnable) {
            holder.checkBox.setVisibility(View.GONE);
        } else {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(false);
        }
        //--------------------------------------------------
    }

    @Override
    public int getItemCount() {
        return (images.size());
    } // כמה פריטים יש לנו


    // הקלאס של Item ספציפי
    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView mPhotoImageView; // תמונה ספציפית
        public ProgressBar pb; // עיגול טעינה קטן
        CheckBox checkBox; // צ'אק בוקס ספציפי
        View view;

        public MyViewHolder(View itemView, FolderActivity folderActivity) {
            super(itemView);
            mPhotoImageView = (ImageView) itemView.findViewById(R.id.iv_photo);
            pb = (ProgressBar) itemView.findViewById(R.id.progress_bar);
            itemView.setOnClickListener(this);
            checkBox = itemView.findViewById(R.id.checkBox);
            checkBox.setOnClickListener(this);
            view = itemView;
            view.setOnLongClickListener(folderActivity);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.checkBox:
                    handleMultiSelectAndToolbar(view);
                    break;
                default: //בלחיצה על כל מקום בתמונה
                    if (!folderActivity.isContextualModeEnable) // אם אנחנו לא במצב בחירה מרובה
                        goToBigImage(); // כנס לתמונה
                    break;
            }
        }

        // בלחיצה על צ'אק-בוקס הפעל את הפונקציה שנמצאת ב- FolderActivity
        private void handleMultiSelectAndToolbar(View view) {
            folderActivity.makeSelection(view, getAdapterPosition());
        }

        // הצגה של כל תמונה בגדול
        private void goToBigImage() {
            int position = getAdapterPosition(); // מיקום התמונה
            if (position != RecyclerView.NO_POSITION) {
                Image image = images.get(position);
                // העברת התמונה ושם התיקייה
                Intent intent = new Intent(mContext, ImageActivity.class);
                intent.putExtra("imageUrl", image.getUrl());
                intent.putExtra("imageName", image.getName());
                intent.putExtra("imageLocation", image.getLocation());
                intent.putExtra("folderName", folderName);
                //------------------------------------------------------------
                mContext.startActivity(intent); // מעבר לעמוד ImageActivity
            }
        }
    }

    // פונקציה אשר מופעלת מ-FolderActivity למחיקת תמונות שנבחרו
    public void removeSelection(ArrayList<Image> selectionList) {
        alreadyPressed = false;
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) { // מחיקה מה-realtime

                if (alreadyPressed) return;
                alreadyPressed = true;
                Folder folder = snapshot.getValue(Folder.class);
                for (int i = 0; i < selectionList.size(); i++) // מעבר על התמונות שעברנו
                {
                    folder.removeImage(selectionList.get(i).getName()); // מחיקת התמונה מהתיקייה
                    notifyDataSetChanged();
                }
                myRef.setValue(folder); // דריסת התיקייה הקודמת בתיקייה שמחקנו ממנה את התמונות
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(mContext,error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });

        for (int i = 0; i < selectionList.size(); i++)
        {
            // מחיקה מהסטורג'
            storageReference = FirebaseStorage.getInstance().getReference().child(mAuth.getUid()).child(folderName).child(selectionList.get(i).getName());
            storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            });
        }
    }
}
