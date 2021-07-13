package com.example.organizeit_tal_aviv;

import java.util.Comparator;

// קלאס המיועד למיון התיקיות לפי זמן העלאה
public class FolderDateSort implements Comparator<Folder> {

    @Override
    public int compare(Folder f1, Folder f2) {
        return f2.getUploadDate().compareTo(f1.getUploadDate());
    }
}


