package us.deathmarine.luyten;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * FileChoosers find files to open for the user
 */
public class FileDialog {
    private ConfigSaver configSaver;
    private LuytenPreferences luytenPrefs;
    private Component parent;
    private JFileChooser fcOpen;
    // private JFileChooser fcSave;
    // private JFileChooser fcSaveAll;

    public FileDialog(Component parent) {
        this.parent = parent;
        configSaver = ConfigSaver.getLoadedInstance();
        luytenPrefs = configSaver.getLuytenPreferences();

        new Thread() {
            public void run() {
                try {
                    initOpenDialog();
                    // Thread.sleep(500);
                    // initSaveAllDialog();
                    // Thread.sleep(500);
                    // initSaveDialog();
                } catch (Exception e) {
                    Luyten.showExceptionDialog("Exception!", e);
                }
            }

            ;
        }.start();
    }

    // Opens Dialog Box for the user to select file to upload
    public File doOpenDialog() {
        File selectedFile = null;
        initOpenDialog();

        retrieveOpenDialogDir(fcOpen);
        fcOpen.setDialogTitle("Upload Files"); // Setting Title of the file chooser
        int returnVal = fcOpen.showOpenDialog(parent);
        saveOpenDialogDir(fcOpen);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fcOpen.getSelectedFile();
        }
        return selectedFile;
    }

    public synchronized void initOpenDialog() {
        if (fcOpen == null) {
            fcOpen = createFileChooser();
            retrieveOpenDialogDir(fcOpen);
        }
    }

    // Configures file Choices for the user when the user is uploading files
    private JFileChooser createFileChooser() {
        JFileChooser fc = new JFileChooser();
        //Filtering files by .txt
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Text documents (.txt)", "txt"));
        fc.setAcceptAllFileFilterUsed(false); // Disables ALL Files as a File Filter
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY); // User can only upload Files
        fc.setMultiSelectionEnabled(false); // User can only select one file at a time
        return fc;
    }

    // Gets the Location of the file directory Usre previously used
    private void retrieveOpenDialogDir(JFileChooser fc) {
        try {
            String currentDirStr = luytenPrefs.getFileOpenCurrentDirectory();
            if (currentDirStr != null && currentDirStr.trim().length() > 0) {
                File currentDir = new File(currentDirStr);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    fc.setCurrentDirectory(currentDir);
                }
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    // Saving Directory Location to minimize traversal to previously opened directory
    private void saveOpenDialogDir(JFileChooser fc) {
        try {
            File currentDir = fc.getCurrentDirectory();
            if (currentDir != null && currentDir.exists() && currentDir.isDirectory()) {
                luytenPrefs.setFileOpenCurrentDirectory(currentDir.getAbsolutePath());
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    public class FileChooserFileFilter extends FileFilter {
        String objType;

        public FileChooserFileFilter(String string) {
            objType = string;
        }

        @Override
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            return f.getName().toLowerCase().endsWith(objType.substring(1));
        }

        @Override
        public String getDescription() {
            return objType;
        }
    }
}
