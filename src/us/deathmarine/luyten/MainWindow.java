package us.deathmarine.luyten;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

//TODO In-Progress

/**
 * Dispatcher
 */
public class MainWindow extends JFrame {
    private static final long serialVersionUID = 5265556630724988013L;

    private static final String TITLE = "La Grange Reach";

    public static Model model;
    // uploadedFiles will allow me to send the uploadedFilesContainer to file
    // parsing
    public MainMenuBar mainMenuBar;
    private JProgressBar bar;
    private JLabel label;
    private FindBox findBox;
    private FindAllBox findAllBox;
    private ConfigSaver configSaver;
    private WindowPosition windowPosition;
    private LuytenPreferences luytenPrefs;
    private FileDialog fileDialog;
    // UploaodedFilesContainer will store all files Uploaded by the user
    private UploadedFilesContainer uploadedFilesContainer;

    // Building The MainWindow
    public MainWindow(File fileFromCommandLine) {
        configSaver = ConfigSaver.getLoadedInstance();
        windowPosition = configSaver.getMainWindowPosition();
        luytenPrefs = configSaver.getLuytenPreferences();

        mainMenuBar = new MainMenuBar(this);
        this.setJMenuBar(mainMenuBar);

        this.adjustWindowPositionBySavedState();
        this.setHideFindBoxOnMainWindowFocus();
        this.setShowFindAllBoxOnMainWindowFocus();
        this.setQuitOnWindowClosing();
        this.setTitle(TITLE);
        this.setIconImage(new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/kidney.png"))).getImage());

        model = new Model(this);
        this.getContentPane().add(model);

        if (fileFromCommandLine != null) {
            model.checkFileSelected(fileFromCommandLine);
        }

        try {
            DropTarget dt = new DropTarget();
            dt.addDropTargetListener(new DropListener(this));
            this.setDropTarget(dt);
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }

        fileDialog = new FileDialog(this);
        // fileSaver = new FileSaver(bar, label);

        this.setExitOnEscWhenEnabled(model);

        if (fileFromCommandLine == null || fileFromCommandLine.getName().toLowerCase().endsWith(".jar")
                || fileFromCommandLine.getName().toLowerCase().endsWith(".zip")) {
            model.startWarmUpThread();
        }

        if (RecentFiles.load() > 0)
            mainMenuBar.updateRecentFiles();
    }

    private static Iterator<?> list(ClassLoader CL) {
        Class<?> CL_class = CL.getClass();
        while (CL_class != java.lang.ClassLoader.class) {
            CL_class = CL_class.getSuperclass();
        }
        java.lang.reflect.Field ClassLoader_classes_field;
        try {
            ClassLoader_classes_field = CL_class.getDeclaredField("classes");
            ClassLoader_classes_field.setAccessible(true);
            Vector<?> classes = (Vector<?>) ClassLoader_classes_field.get(CL);
            return classes.iterator();
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
        return null;
    }

    // This is Where the file will go to once the user selects a file
    public void onOpenFileMenu() {
        if (checkIfFileUploadSizeReached()) {
            return;
        }
        File selectedFile = fileDialog.doOpenDialog();
        if (selectedFile != null) {

            if (checkIfFileAlreadyAdded(selectedFile)) {
                return;
            }

            System.out.println("[Open]: Opening " + selectedFile.getAbsolutePath());

            this.getModel().checkFileSelected(selectedFile);
        }
    }

    // Checks if file upload size is reached
    public boolean checkIfFileUploadSizeReached() {
        if (uploadedFilesContainer == null) {
            uploadedFilesContainer = new UploadedFilesContainer();
        }

        if (uploadedFilesContainer.getFileUploadSizeLeft() == 0) {
            Luyten.showErrorDialog(
                    "File Upload Size Limit ( " + uploadedFilesContainer.getMaxFilesAllowed() + " ) Reached!");
            return true;
        }

        return false;
    }

    // Checks if user has already selected the file
    public boolean checkIfFileAlreadyAdded(File file) {
        if (uploadedFilesContainer.checkIfFileAlreadyAdded(file)) {
            Luyten.showErrorDialog("File : " + file.getName() + " already chosen.");
            return true;
        }
        return false;
    }

    public void removeFile(String fileName) {
        System.out.println("Removing File " + fileName);
        uploadedFilesContainer.removeFile(fileName);
    }

    public void onExitMenu() {
        quit();
    }

    public void onSelectAllMenu() {
        try {
            RSyntaxTextArea pane = this.getModel().getCurrentTextArea();
            if (pane != null) {
                pane.requestFocusInWindow();
                pane.setSelectionStart(0);
                pane.setSelectionEnd(pane.getText().length());
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    public void onFindMenu() {
        try {
            RSyntaxTextArea pane = this.getModel().getCurrentTextArea();
            if (pane != null) {
                if (findBox == null)
                    findBox = new FindBox(this);
                findBox.showFindBox();
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    public void onFindAllMenu() {
        try {
            if (findAllBox == null)
                findAllBox = new FindAllBox(this);
            findAllBox.showFindBox();

        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    public void onLegalMenu() {
        new Thread() {
            public void run() {
                try {
                    bar.setVisible(true);
                    bar.setIndeterminate(true);
                    String legalStr = getLegalStr();
                    MainWindow.this.getModel().showLegal(legalStr);
                } finally {
                    bar.setIndeterminate(false);
                    bar.setVisible(false);
                }
            }
        }.start();
    }

    public void onListLoadedClasses() {
        try {
            StringBuilder sb = new StringBuilder();
            ClassLoader myCL = Thread.currentThread().getContextClassLoader();
            bar.setVisible(true);
            bar.setIndeterminate(true);
            while (myCL != null) {
                sb.append("ClassLoader: " + myCL + "\n");
                for (Iterator<?> iter = list(myCL); iter.hasNext(); ) {
                    sb.append("\t" + iter.next() + "\n");
                }
                myCL = myCL.getParent();
            }
            MainWindow.this.getModel().show("Debug", sb.toString());
        } finally {
            bar.setIndeterminate(false);
            bar.setVisible(false);
        }
    }

    private String getLegalStr() {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream("/distfiles/Procyon.License.txt")));
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
            sb.append("\n\n\n\n\n");
            reader = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream("/distfiles/RSyntaxTextArea.License.txt")));
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
        } catch (IOException e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
        return sb.toString();
    }

    public void onThemesChanged() {
        this.getModel().changeTheme(luytenPrefs.getThemeXml());
    }

//	public void onSettingsChanged() {
//		this.getModel().updateOpenClasses();
//	}

    public void onFileDropped(File file) {
        if (file != null) {
            this.getModel().checkFileSelected(file);
        }
    }

    // This functions sets the files in the uploadedFilesContainer after the
    // Models checks the files
    public void onFileLoadEnded(File file, boolean isSuccess) {
        // System.out.println("At main window with file : " +file.getName()+"
        // isSuccess : " + isSuccess);
        try {
            if (file != null && isSuccess) {
                uploadedFilesContainer.add(file);
                //this.setTitle(TITLE + " - " + file.getName());
                if (uploadedFilesContainer.getFileUploadSizeLeft() == 0) {
                    model.submitButtonAccess(true);
                }
            } else {
                this.setTitle(TITLE);
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    // User clicks the button and this makes sure the user has correctly
    // Uploaded the files
    public boolean onSubmitFilesButtonClicked() {
        // Checking if user has not files uploaded
        if (uploadedFilesContainer == null
                || uploadedFilesContainer.getFileUploadSizeLeft() == uploadedFilesContainer.getMaxFilesAllowed()) {
            Luyten.showErrorDialog("No files Uploaded");
            return false;
        } else if (uploadedFilesContainer.getFileUploadSizeLeft() > 0) {
            Luyten.showErrorDialog("Please Upload " + uploadedFilesContainer.getFileUploadSizeLeft() + " more Files");
            return false;
        }

        // checking Station's to make sure the files are from three different stations
        if (!checkStationName(uploadedFilesContainer)) {
            Luyten.showErrorDialog("Please Upload Files from the three different Stations");
            return false;
        }

        // checking the year of the uploaded Files
        if (!checkYearOfUploadedFiles(uploadedFilesContainer)) {
            Luyten.showErrorDialog("Uploaded Files are not from the same year");
            return false;
        }


        // If files are uploaded
        if (uploadedFilesContainer.getFileUploadSizeLeft() == 0) {
            DataExtractorLoop uploadeFiles = new DataExtractorLoop();
            float[][] data = uploadeFiles.getData(uploadedFilesContainer);
            model.submitButtonAccess(false);
            return true;
        }
        return false;
    }

    //Checking if uploaded files are three different stations
    private boolean checkStationName(UploadedFilesContainer filesContainer) {
        int bitSize = filesContainer.getMaxFilesAllowed();
        BitSet bitSet = new BitSet(bitSize);
        bitSet.set(0, bitSize);
        // the bits are all true and if three unique stations are found then bits are set to false maing is length 0
        for (File file : filesContainer.getAllFiles()) {
            try {
                BufferedReader buf = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String lineFetched = null;
                lineFetched = buf.readLine();//make sure a valid file is uploaded.

                if (lineFetched.contains("Peoria")) {
                    bitSet.set(0, false);
                } else if (lineFetched.contains("Havana")) {
                    bitSet.set(1, false);
                } else if (lineFetched.contains("Beardstown")) {
                    bitSet.set(2, false);
                }

            } catch (Exception e) {
                Luyten.showExceptionDialog("checkStationName", e);
            }
        }

        // if length is not zero this means one of the stations is not included 
        return (bitSet.length() == 0); //if it's == 0 then true is returned else false
    }

    //Checking if the uploaded files are from the same year - true if same year | false if not
    private boolean checkYearOfUploadedFiles(UploadedFilesContainer filesContainer) {
        ArrayList<String> yearCheck = new ArrayList<String>();
        for (File file : filesContainer.getAllFiles()) {
            try {
                BufferedReader buf = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String patternString = "^\\d{1,2}\\/\\d{1,2}\\/\\d{4}.*";//look for the first data in data
                Pattern pattern = Pattern.compile(patternString);
                String lineFetched = null;
                boolean foundYear = false;//look for the first year

                while (!foundYear) {
                    lineFetched = buf.readLine();
                    if (lineFetched == null) {
                        break;
                    } else {
                        Matcher matcher = pattern.matcher(lineFetched);
                        boolean matches = matcher.matches();
                        if (matches) {
                            Pattern getYear = Pattern.compile(".*\\/(.?\\d{4})");
                            Matcher findYear = getYear.matcher(lineFetched);
                            if (findYear.find()) {//when the year is found stop and add to list
                                yearCheck.add(findYear.group(1));
                                foundYear = true;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                Luyten.showExceptionDialog("checkYears", e);
            }
        }//end of file loop
        int index = yearCheck.size();
        for (int i = 0; i < index; i++) {
            if (!yearCheck.get(0).equals(yearCheck.get(i))) {
                return false;
            }

        }
        return true;
    }

    // When opening the client this function Sets windows size to user's
    // preference
    private void adjustWindowPositionBySavedState() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (!windowPosition.isSavedWindowPositionValid()) {
            final Dimension center = new Dimension((int) (screenSize.width * 0.75), (int) (screenSize.height * 0.75));
            final int x = (int) (center.width * 0.2);
            final int y = (int) (center.height * 0.2);
            this.setBounds(x, y, center.width, center.height);

        } else if (windowPosition.isFullScreen()) {
            int heightMinusTray = screenSize.height;
            if (screenSize.height > 30)
                heightMinusTray -= 30;
            this.setBounds(0, 0, screenSize.width, heightMinusTray);
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);

            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (MainWindow.this.getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                        windowPosition.setFullScreen(false);
                        if (windowPosition.isSavedWindowPositionValid()) {
                            MainWindow.this.setBounds(windowPosition.getWindowX(), windowPosition.getWindowY(),
                                    windowPosition.getWindowWidth(), windowPosition.getWindowHeight());
                        }
                        MainWindow.this.removeComponentListener(this);
                    }
                }
            });

        } else {
            this.setBounds(windowPosition.getWindowX(), windowPosition.getWindowY(), windowPosition.getWindowWidth(),
                    windowPosition.getWindowHeight());
        }
    }

    private void setHideFindBoxOnMainWindowFocus() {
        this.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (findBox != null && findBox.isVisible()) {
                    findBox.setVisible(false);
                }
            }
        });
    }

    private void setShowFindAllBoxOnMainWindowFocus() {
        this.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (findAllBox != null && findAllBox.isVisible()) {
                    findAllBox.setVisible(false);
                }
            }
        });
    }

    private void setQuitOnWindowClosing() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
    }

    private void quit() {
        try {
            windowPosition.readPositionFromWindow(this);
            configSaver.saveConfig();
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        } finally {
            try {
                this.dispose();
            } finally {
                System.exit(0);
            }
        }
    }

    private void setExitOnEscWhenEnabled(JComponent mainComponent) {
        Action escapeAction = new AbstractAction() {
            private static final long serialVersionUID = -3460391555954575248L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (luytenPrefs.isExitByEscEnabled()) {
                    quit();
                }
            }
        };
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        mainComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escapeKeyStroke, "ESCAPE");
        mainComponent.getActionMap().put("ESCAPE", escapeAction);
    }

    public Model getModel() {
        return model;
    }

    public JProgressBar getBar() {
        return bar;
    }

    public JLabel getLabel() {
        return label;
    }
}
