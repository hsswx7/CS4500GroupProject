package us.deathmarine.luyten;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


//TODO In-Progress


/**
 * Dispatcher
 */
public class MainWindow extends JFrame {
	private static final long serialVersionUID = 5265556630724988013L;

	private static final String TITLE = "Legrange Reach Research"; 

	public static Model model;
	private JProgressBar bar;
	private JLabel label;
	private FindBox findBox;
	private FindAllBox findAllBox;
	private ConfigSaver configSaver;
	private WindowPosition windowPosition;
	private LuytenPreferences luytenPrefs;
	private FileDialog fileDialog;
	//private FileSaver fileSaver;
	private UploadedFilesContainer uploadedFilesContainer; //this container holds an array foe File Objects the User wants to upload 
	private UploadeFiles uploadeFiles;  // this will allow me to send the uploadedFilesContainer to edit the files 
	public MainMenuBar mainMenuBar;

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
				Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/Luyten.png"))).getImage());

		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		label = new JLabel();
		label.setHorizontalAlignment(JLabel.LEFT);
		panel1.setBorder(new BevelBorder(BevelBorder.LOWERED));
		panel1.setPreferredSize(new Dimension(this.getWidth() / 2, 20));
		panel1.setBorder(BorderFactory.createTitledBorder("Panel 1 from MainWindows.java line 82"));
		panel1.add(label);

		JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bar = new JProgressBar();

		bar.setStringPainted(true);
		bar.setOpaque(false);
		bar.setVisible(false);
		panel2.setPreferredSize(new Dimension(this.getWidth() / 3, 20));
		panel2.setBorder(BorderFactory.createTitledBorder("Panel 2 from MainWindows.java line 91"));
		panel2.add(bar);

		model = new Model(this);
		this.getContentPane().add(model);
		// TODO the following line to change pane structure
		
		JSplitPane spt = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1, panel2) {
			private static final long serialVersionUID = 2189946972124687305L;
			private final int location = 400;

			{
				setDividerLocation(location);
			}

			@Override
			public int getDividerLocation() {
				return location;
			}

			@Override
			public int getLastDividerLocation() {
				return location;
			}
		};
		spt.setBorder(new BevelBorder(BevelBorder.LOWERED));
		spt.setPreferredSize(new Dimension(this.getWidth(), 24));
		this.add(spt, BorderLayout.SOUTH);
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
		//fileSaver = new FileSaver(bar, label);

		this.setExitOnEscWhenEnabled(model);

		if (fileFromCommandLine == null || fileFromCommandLine.getName().toLowerCase().endsWith(".jar")
				|| fileFromCommandLine.getName().toLowerCase().endsWith(".zip")) {
			model.startWarmUpThread();
		}
		
		if(RecentFiles.load() > 0) mainMenuBar.updateRecentFiles();
	}

	// This is Where the file will go to once the user selects a file
	public void onOpenFileMenu() {
		if(checkIfFileUploadSizeReached()){
			return;
		}
		File selectedFile = fileDialog.doOpenDialog();
		if (selectedFile != null) {
			System.out.println("[Open]: Opening " + selectedFile.getAbsolutePath());
			
			this.getModel().checkFileSelected(selectedFile);
		}
	}
	
	public boolean checkIfFileUploadSizeReached(){
		if(uploadedFilesContainer == null){
			uploadedFilesContainer = new UploadedFilesContainer();
		}
		
		if(uploadedFilesContainer.getFileUploadSizeLeft() == 0){
			Luyten.showErrorDialog("File Upload Size Limit ( " + uploadedFilesContainer.getMaxFilesAllowed() + " ) Reached!");
			return true;
		}
		
		return false;
	}

	/*public void onCloseFileMenu() {
		this.getModel().closeFile();
	}*/

	/*public void onSaveAsMenu() {
		RSyntaxTextArea pane = this.getModel().getCurrentTextArea();
		if (pane == null)
			return;
		String tabTitle = this.getModel().getCurrentTabTitle();
		if (tabTitle == null)
			return;

		String recommendedFileName = tabTitle.replace(".class", ".java");
		File selectedFile = fileDialog.doSaveDialog(recommendedFileName);
		if (selectedFile != null) {
			fileSaver.saveText(pane.getText(), selectedFile);
		}
	}

	public void onSaveAllMenu() {
		File openedFile = this.getModel().getOpenedFile();
		if (openedFile == null)
			return;

		String fileName = openedFile.getName();
		if (fileName.endsWith(".class")) {
			fileName = fileName.replace(".class", ".java");
		} else if (fileName.toLowerCase().endsWith(".jar")) {
			fileName = "decompiled-" + fileName.replaceAll("\\.[jJ][aA][rR]", ".zip");
		} else {
			fileName = "saved-" + fileName;
		}

		File selectedFileToSave = fileDialog.doSaveAllDialog(fileName);
		if (selectedFileToSave != null) {
			fileSaver.saveAllDecompiled(openedFile, selectedFileToSave);
		}
	}*/

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
				for (Iterator<?> iter = list(myCL); iter.hasNext();) {
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

	public void onSettingsChanged() {
		this.getModel().updateOpenClasses();
	}

	/*public void onTreeSettingsChanged() {
		this.getModel().updateTree();
	}*/

	public void onFileDropped(File file) {
		if (file != null) {
			this.getModel().checkFileSelected(file);
		}
	}
	
	//This functions sets the files in the uploadedfilesContainer after the Models checks the files 
	public void onFileLoadEnded(File file, boolean isSuccess) {
		//System.out.println("At main window with file : " +file.getName()+" isSuccess : " + isSuccess);
		try {
			if (file != null && isSuccess) {
				uploadedFilesContainer.add(file);
				this.setTitle(TITLE + " - " + file.getName());
			} else {
				this.setTitle(TITLE);
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}
	
	//User clicks the button and this makes sure the user has correctly uplaoded the files 
	public void onSubmitFilesButtonClicked(){
		System.out.println("Submit Files button Clicked");
		//Checking if user has not files uploaded  
		if(uploadedFilesContainer == null || uploadedFilesContainer.getFileUploadSizeLeft() == uploadedFilesContainer.getMaxFilesAllowed()){
			Luyten.showErrorDialog("No files Uploaded");
			return;
		}else if (uploadedFilesContainer.getFileUploadSizeLeft() > 0){
			Luyten.showErrorDialog("Please Upload " + uploadedFilesContainer.getFileUploadSizeLeft() + " more Files");
			return; 
		}
		
		//TODO TIM you can have your function start from here
		//If files are uploaded 
		if(uploadedFilesContainer.getFileUploadSizeLeft() == 0){
			uploadeFiles = new UploadeFiles();
			uploadeFiles.setUploadedFiles(uploadedFilesContainer);
		}		
		
	}
	
	

	public void onNavigationRequest(String uniqueStr) {
		this.getModel().navigateTo(uniqueStr);
	}

	//When opening the client this function Sets windows size to user's preference
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
