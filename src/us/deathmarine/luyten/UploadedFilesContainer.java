package us.deathmarine.luyten;

import java.io.File;
import java.util.ArrayList;

// This file will keep track of the files uploaded by the user 

public class UploadedFilesContainer {
    public static ArrayList<File> files = new ArrayList<>();
    private static int maxFilesAllowed = 3;

    // Removes file uploaded
    public static void removeFile(File file) {
        files.remove(file);
    }

    public void add(File file) {
        if (files.contains(file)) {
            return;
        }
        if (files.size() > getMaxFilesAllowed()) {
            return;
        }

        files.add(file);
    }

    // Returns the total file upload size allowed
    public int getMaxFilesAllowed() {
        return UploadedFilesContainer.maxFilesAllowed;
    }

    // returns amount of files uploaded
    public int getTotalFilesUploaded() {
        return files.size();
    }

    // returns files upload size left
    public int getFileUploadSizeLeft() {
        return (getMaxFilesAllowed() - getTotalFilesUploaded());
    }

    // Removes a file from the files array list
    public void removeFile(String fileName) {
        System.out.println("File Deleted: " + fileName);

        // Going through the Files Array List to Remove File by name
        int arrayListSize = files.size();
        for (int i = 0; i <= arrayListSize; i++) {
            if (files.get(i).getName().equals(fileName)) {
                files.remove(i);
                break;
            }
        }

        System.out.println("New File ArrayList");
        for (File counter : files) {
            System.out.println(counter.getName());
        }
    }

    // Returns all the files
    public ArrayList<File> getAllFiles() {
        return files;
    }

    // checks if files has already been added
    public boolean checkIfFileAlreadyAdded(File file) {
        if (files.isEmpty()) {
            System.out.println("File is Empty");
            return false;
        }

        int size = files.size();
        for (int i = 0; i < size; i++) {
            if (files.get(i).getName().equals(file.getName())) {
                return true;
            }
        }

		/*
         * if(files.contains(file)){ return true; }
		 */
        return false;
    }

}
