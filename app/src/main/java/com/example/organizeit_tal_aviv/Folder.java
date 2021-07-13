package com.example.organizeit_tal_aviv;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;


// קלאס המייצג תיקייה
public class Folder {
    // כדי שנוכל להציג כמה תמונות יש בתיקייה
    private int images_counter;
    // מערך התמונות שהתיקייה מכילה
    private ArrayList<Image> images;
    // כדי שנוכל למיין את התיקיות לפי זמן העלאה
    private Long uploadDate;

    public Folder() {
    }

    public Folder(int images_counter, ArrayList<Image> images, Long uploadDate) {
        this.images_counter = images_counter;
        this.images = images;
        this.uploadDate = uploadDate;
    }

    public ArrayList<Image> getImages() {
        return images;
    }

    public void setImages(ArrayList<Image> images) {
        this.images = images;
    }

    public int getImages_counter() {
        return images_counter;
    }

    public void setImages_counter(int images_counter) {
        this.images_counter = images_counter;
    }

    public Long getUploadDate() { return uploadDate; }

    public void setUploadDate(Long uploadDate) { this.uploadDate = uploadDate; }

    // הוספת תמונה למערך התמונות בתיקיה וקידום המונה
    public void addImage(Image image) {
        if (images == null) {
            images = new ArrayList<Image>();
        }
        images.add(image);
        images_counter++;
    }

    // הסרת תמונה ממערך התמונות בתיקייה
    public void removeImage(String imageName) {
        images.removeIf(image -> image.getName().equals(imageName));
        images_counter--;
    }
}
