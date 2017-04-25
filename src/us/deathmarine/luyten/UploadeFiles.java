package us.deathmarine.luyten;

import java.io.File;
import java.util.ArrayList;

/* This file holds all the files uploaded so tim can access them with them */

public class UploadeFiles {

	private UploadedFilesContainer uploadedFiles;

	public ArrayList<File> setUploadedFiles(UploadedFilesContainer filesUpload) {
		uploadedFiles = filesUpload;

		System.out.println("Files Uploaded: ");
		for (File files : filesUpload.getAllFiles()) {
			System.out.println(files.getName());
		}
		return uploadedFiles.getAllFiles();
	}

}
