package us.deathmarine.luyten;

import java.io.File;
import java.util.ArrayList;

/* This file holds all the files uploaded so tim can access them with them */


public class UploadeFiles {
	
	private UploadedFilesContainer uploadedFiles;
	
	
	
	
	public ArrayList<File> setUploadedFiles(UploadedFilesContainer filesUpload){
		uploadedFiles = filesUpload;
		return uploadedFiles.getAllFiles();
	}
	
	
	

}
