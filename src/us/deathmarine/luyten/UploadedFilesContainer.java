package us.deathmarine.luyten;

import java.io.File;
import java.util.ArrayList;

// This file will keep track of the files uploaded by the user 

public class UploadedFilesContainer {
	public static ArrayList<File> files = new ArrayList<>();
	private static int maxFilesAllowed = 3;
	
	public void add(File file){
		if(files.contains(file)){
			return;
		}
		if(files.size() > getMaxFilesAllowed()){
			return;
		}
		
		files.add(file);
	}
	
	// Removes file uploaded 
	public static void removeFile(File file){
		files.remove(file);
	}
	
	// Returns the total file upload size allowed
	public int getMaxFilesAllowed(){
		return UploadedFilesContainer.maxFilesAllowed;
	}
	
	// returns amount of files uploaded 
	public int getTotalFilesUploaded(){
		return files.size();
	}
	
	// returns files upload size left
	public int getFileUploadSizeLeft(){
		return (getMaxFilesAllowed() - getTotalFilesUploaded());
	}
	
	// Returns all the files 
	public ArrayList<File> getAllFiles(){
		return files;
	}

}
