package us.deathmarine.luyten;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.JarTypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

/**
 * Jar-level model
 */
public class Model extends JSplitPane {
	private static final long serialVersionUID = 6896857630400910200L;

	private static final long MAX_JAR_FILE_SIZE_BYTES = 1_000_000_000;
	private static final long MAX_UNPACKED_FILE_SIZE_BYTES = 1_000_000;

	private static LuytenTypeLoader typeLoader = new LuytenTypeLoader();
	public static MetadataSystem metadataSystem = new MetadataSystem(typeLoader);

	private JTree tree;
	private JList<String> list; //Used to display List of files user decided to upload
	private DefaultListModel<String> listModel; 
	public JTabbedPane house;
	private JButton submitFileButton;
	private File file;
	private DecompilerSettings settings;
	private DecompilationOptions decompilationOptions;
	private Theme theme;
	private MainWindow mainWindow;
	private JProgressBar bar;
	private JLabel label;
	private HashSet<OpenFile> hmap = new HashSet<OpenFile>();
	private boolean open = false;
	private State state;
	private ConfigSaver configSaver;
	private LuytenPreferences luytenPrefs;

	public Model(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.bar = mainWindow.getBar();
		this.setLabel(mainWindow.getLabel());

		configSaver = ConfigSaver.getLoadedInstance();
		settings = configSaver.getDecompilerSettings();
		luytenPrefs = configSaver.getLuytenPreferences();

		try {
			String themeXml = luytenPrefs.getThemeXml();
			theme = Theme.load(getClass().getResourceAsStream(LuytenPreferences.THEME_XML_PATH + themeXml));
		} catch (Exception e1) {
			try {
				Luyten.showExceptionDialog("Exception!", e1);
				String themeXml = LuytenPreferences.DEFAULT_THEME_XML;
				luytenPrefs.setThemeXml(themeXml);
				theme = Theme.load(getClass().getResourceAsStream(LuytenPreferences.THEME_XML_PATH + themeXml));
			} catch (Exception e2) {
				Luyten.showExceptionDialog("Exception!", e2);
			}
		}

		tree = new JTree();
		tree.setModel(new DefaultTreeModel(null));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new CellRenderer());
		TreeListener tl = new TreeListener();
		tree.addMouseListener(tl);
		tree.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					openEntryByTreePath(tree.getSelectionPath());
				}
			}
		});
		
		/* This list is used to display the files chosen by user to upload */
		listModel = new DefaultListModel<String>();
	
		list = new JList<String>(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount(-1);
		JScrollPane listScrollPane = new JScrollPane(list);
		


		// leftMainPanel will be a container for all other left panels
		JPanel leftMainPanel = new JPanel();
		leftMainPanel.setLayout(new BoxLayout(leftMainPanel, BoxLayout.Y_AXIS));

		JPanel uploadFileLeftPanel = new JPanel();
        uploadFileLeftPanel.setLayout(new BoxLayout(uploadFileLeftPanel, 0));
        uploadFileLeftPanel.setBorder(BorderFactory.createTitledBorder("Files Uploaded"));
        uploadFileLeftPanel.add(listScrollPane);

        submitFileButton = new JButton("Submit Uploaded Files");
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, 0));
        buttonPanel.add(submitFileButton);

        leftMainPanel.add(uploadFileLeftPanel);
        leftMainPanel.add(buttonPanel);
        
        submitFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				onSubmitButtonClicked();
			}
        });

        JPanel panel3 = new JPanel();
        panel3.setLayout(new BoxLayout(panel3, 1));
        panel3.setBorder(BorderFactory.createTitledBorder("Test Panel 3"));
        panel3.add(new JScrollPane(tree));


        leftMainPanel.add(panel3);

		house = new JTabbedPane();
		house.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		house.addChangeListener(new TabChangeListener());
		house.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isMiddleMouseButton(e)) {
					closeOpenTab(house.getSelectedIndex());
				}
			}
		});
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, 1));
		panel.setBorder(BorderFactory.createTitledBorder("Code"));
		panel.add(house);

		this.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		this.setDividerLocation(250 % mainWindow.getWidth());
		this.setLeftComponent(leftMainPanel);
		this.setRightComponent(panel);

		decompilationOptions = new DecompilationOptions();
		decompilationOptions.setSettings(settings);
		decompilationOptions.setFullDecompilation(true);
	}

	public void onSubmitButtonClicked(){
		mainWindow.onSubmitFilesButtonClicked();
	}
	public void showLegal(String legalStr) {
		show("Legal", legalStr);
	}

	public void show(String name, String contents) {
		OpenFile open = new OpenFile(name, "*/" + name, theme, mainWindow);
		open.setContent(contents);
		hmap.add(open);
		addOrSwitchToTab(open);
	}

	private void addOrSwitchToTab(final OpenFile open) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final String title = open.name;
					RTextScrollPane rTextScrollPane = open.scrollPane;
					if (house.indexOfTab(title) < 0) {
						house.addTab(title, rTextScrollPane);
						house.setSelectedIndex(house.indexOfTab(title));
						int index = house.indexOfTab(title);
						Tab ct = new Tab(title);
						ct.getButton().addMouseListener(new CloseTab(title));
						house.setTabComponentAt(index, ct);
					} else {
						house.setSelectedIndex(house.indexOfTab(title));
					}
					open.onAddedToScreen();
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			}
		});
	}

	public void closeOpenTab(int index) {
		RTextScrollPane co = (RTextScrollPane) house.getComponentAt(index);
		RSyntaxTextArea pane = (RSyntaxTextArea) co.getViewport().getView();
		OpenFile open = null;
		for (OpenFile file : hmap)
			if (pane.equals(file.textArea))
				open = file;
		if (open != null && hmap.contains(open))
			hmap.remove(open);
		house.remove(co);
		if (open != null)
			open.close();
	}

	/*private String getName(String path) {
		if (path == null)
			return "";
		int i = path.lastIndexOf("/");
		if (i == -1)
			i = path.lastIndexOf("\\");
		if (i != -1)
			return path.substring(i + 1);
		return path;
	}*/

	private class TreeListener extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent event) {
			boolean isClickCountMatches = (event.getClickCount() == 1 && luytenPrefs.isSingleClickOpenEnabled())
					|| (event.getClickCount() == 2 && !luytenPrefs.isSingleClickOpenEnabled());
			if (!isClickCountMatches)
				return;

			if (!SwingUtilities.isLeftMouseButton(event))
				return;

			final TreePath trp = tree.getPathForLocation(event.getX(), event.getY());
			if (trp == null)
				return;

			Object lastPathComponent = trp.getLastPathComponent();
			boolean isLeaf = (lastPathComponent instanceof TreeNode && ((TreeNode) lastPathComponent).isLeaf());
			if (!isLeaf)
				return;

			new Thread() {
				public void run() {
					openEntryByTreePath(trp);
				}
			}.start();
		}
	}

	public void openEntryByTreePath(TreePath trp) {
		String name = "";
		String path = "";
		try {
			bar.setVisible(true);
			if (trp.getPathCount() > 1) {
				for (int i = 1; i < trp.getPathCount(); i++) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) trp.getPathComponent(i);
					TreeNodeUserObject userObject = (TreeNodeUserObject) node.getUserObject();
					if (i == trp.getPathCount() - 1) {
						name = userObject.getOriginalName();
					} else {
						path = path + userObject.getOriginalName() + "/";
					}
				}
				path = path + name;

				if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
					if (state == null) {
						JarFile jfile = new JarFile(file);
						ITypeLoader jarLoader = new JarTypeLoader(jfile);

						typeLoader.getTypeLoaders().add(jarLoader);
						state = new State(file.getCanonicalPath(), file, jfile, jarLoader);
					}

					JarEntry entry = state.jarFile.getJarEntry(path);
					if (entry == null) {
						throw new FileEntryNotFoundException();
					}
					if (entry.getSize() > MAX_UNPACKED_FILE_SIZE_BYTES) {
						throw new TooLargeFileException(entry.getSize());
					}
					String entryName = entry.getName();
					if (entryName.endsWith(".class")) {
						getLabel().setText("Extracting: " + name);
						String internalName = StringUtilities.removeRight(entryName, ".class");
						TypeReference type = metadataSystem.lookupType(internalName);
						extractClassToTextPane(type, name, path, null);
					} else {
						getLabel().setText("Opening: " + name);
						try (InputStream in = state.jarFile.getInputStream(entry);) {
							extractSimpleFileEntryToTextPane(in, name, path);
						}
					}
				}
			} else {
				name = file.getName();
				path = file.getPath().replaceAll("\\\\", "/");
				if (file.length() > MAX_UNPACKED_FILE_SIZE_BYTES) {
					throw new TooLargeFileException(file.length());
				}
				if (name.endsWith(".class")) {
					getLabel().setText("Extracting: " + name);
					TypeReference type = metadataSystem.lookupType(path);
					extractClassToTextPane(type, name, path, null);
				} else {
					getLabel().setText("Opening: " + name);
					try (InputStream in = new FileInputStream(file);) {
						extractSimpleFileEntryToTextPane(in, name, path);
					}
				}
			}

			getLabel().setText("Complete");
		} catch (FileEntryNotFoundException e) {
			getLabel().setText("File not found: " + name);
		} catch (FileIsBinaryException e) {
			getLabel().setText("Binary resource: " + name);
		} catch (TooLargeFileException e) {
			getLabel().setText("File is too large: " + name + " - size: " + e.getReadableFileSize());
		} catch (Exception e) {
			getLabel().setText("Cannot open: " + name);
			Luyten.showExceptionDialog("Unable to open file!", e);
		} finally {
			bar.setVisible(false);
		}
	}

	void extractClassToTextPane(TypeReference type, String tabTitle, String path, String navigatonLink)
			throws Exception {
		if (tabTitle == null || tabTitle.trim().length() < 1 || path == null) {
			throw new FileEntryNotFoundException();
		}
		OpenFile sameTitledOpen = null;
		for (OpenFile nextOpen : hmap) {
			if (tabTitle.equals(nextOpen.name)) {
				sameTitledOpen = nextOpen;
				break;
			}
		}
		if (sameTitledOpen != null && path.equals(sameTitledOpen.path) && type.equals(sameTitledOpen.getType())
				&& sameTitledOpen.isContentValid()) {
			sameTitledOpen.setInitialNavigationLink(navigatonLink);
			addOrSwitchToTab(sameTitledOpen);
			return;
		}

		// resolve TypeDefinition
		TypeDefinition resolvedType = null;
		if (type == null || ((resolvedType = type.resolve()) == null)) {
			throw new Exception("Unable to resolve type.");
		}

		// open tab, store type information, start decompilation
		if (sameTitledOpen != null) {
			sameTitledOpen.path = path;
			sameTitledOpen.invalidateContent();
			sameTitledOpen.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
			sameTitledOpen.setType(resolvedType);
			sameTitledOpen.setInitialNavigationLink(navigatonLink);
			sameTitledOpen.resetScrollPosition();
			sameTitledOpen.decompile();
			addOrSwitchToTab(sameTitledOpen);
		} else {
			OpenFile open = new OpenFile(tabTitle, path, theme, mainWindow);
			open.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
			open.setType(resolvedType);
			open.setInitialNavigationLink(navigatonLink);
			open.decompile();
			hmap.add(open);
			addOrSwitchToTab(open);
		}
	}

	public void extractSimpleFileEntryToTextPane(InputStream inputStream, String tabTitle, String path)
			throws Exception {
		if (inputStream == null || tabTitle == null || tabTitle.trim().length() < 1 || path == null) {
			throw new FileEntryNotFoundException();
		}
		OpenFile sameTitledOpen = null;
		for (OpenFile nextOpen : hmap) {
			if (tabTitle.equals(nextOpen.name)) {
				sameTitledOpen = nextOpen;
				break;
			}
		}
		if (sameTitledOpen != null && path.equals(sameTitledOpen.path)) {
			addOrSwitchToTab(sameTitledOpen);
			return;
		}

		// build tab content
		StringBuilder sb = new StringBuilder();
		long nonprintableCharactersCount = 0;
		try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader reader = new BufferedReader(inputStreamReader);) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");

				for (byte nextByte : line.getBytes()) {
					if (nextByte <= 0) {
						nonprintableCharactersCount++;
					}
				}

			}
		}

		// guess binary or text
		String extension = "." + tabTitle.replaceAll("^[^\\.]*$", "").replaceAll("[^\\.]*\\.", "");
		boolean isTextFile = (OpenFile.WELL_KNOWN_TEXT_FILE_EXTENSIONS.contains(extension)
				|| nonprintableCharactersCount < sb.length() / 5);
		if (!isTextFile) {
			throw new FileIsBinaryException();
		}

		// open tab
		if (sameTitledOpen != null) {
			sameTitledOpen.path = path;
			sameTitledOpen.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
			sameTitledOpen.resetScrollPosition();
			sameTitledOpen.setContent(sb.toString());
			addOrSwitchToTab(sameTitledOpen);
		} else {
			OpenFile open = new OpenFile(tabTitle, path, theme, mainWindow);
			open.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
			open.setContent(sb.toString());
			hmap.add(open);
			addOrSwitchToTab(open);
		}
	}

	private class TabChangeListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			int selectedIndex = house.getSelectedIndex();
			if (selectedIndex < 0) {
				return;
			}
			for (OpenFile open : hmap) {
				if (house.indexOfTab(open.name) == selectedIndex) {

					if (open.getType() != null && !open.isContentValid()) {
						updateOpenClass(open);
						break;
					}

				}
			}
		}
	}

	public void updateOpenClasses() {
		// invalidate all open classes (update will happen at tab change)
		for (OpenFile open : hmap) {
			if (open.getType() != null) {
				open.invalidateContent();
			}
		}
		// update the current open tab - if it is a class
		for (OpenFile open : hmap) {
			if (open.getType() != null && isTabInForeground(open)) {
				updateOpenClass(open);
				break;
			}
		}
	}

	private void updateOpenClass(final OpenFile open) {
		if (open.getType() == null) {
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					bar.setVisible(true);
					getLabel().setText("Extracting: " + open.name);
					open.invalidateContent();
					open.decompile();
					getLabel().setText("Complete");
				} catch (Exception e) {
					getLabel().setText("Error, cannot update: " + open.name);
				} finally {
					bar.setVisible(false);
				}
			}
		}).start();
	}

	private boolean isTabInForeground(OpenFile open) {
		String title = open.name;
		int selectedIndex = house.getSelectedIndex();
		return (selectedIndex >= 0 && selectedIndex == house.indexOfTab(title));
	}

	final class State implements AutoCloseable {
		private final String key;
		private final File file;
		final JarFile jarFile;
		final ITypeLoader typeLoader;

		private State(String key, File file, JarFile jarFile, ITypeLoader typeLoader) {
			this.key = VerifyArgument.notNull(key, "key");
			this.file = VerifyArgument.notNull(file, "file");
			this.jarFile = jarFile;
			this.typeLoader = typeLoader;
		}

		@Override
		public void close() {
			if (typeLoader != null) {
				Model.typeLoader.getTypeLoaders().remove(typeLoader);
			}
			Closer.tryClose(jarFile);
		}

		public File getFile() {
			return file;
		}

		public String getKey() {
			return key;
		}
	}

	private class Tab extends JPanel {
		private static final long serialVersionUID = -514663009333644974L;
		private JLabel closeButton = new JLabel(new ImageIcon(
				Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/icon_close.png"))));
		private JLabel tabTitle = new JLabel();
		private String title = "";

		public Tab(String t) {
			super(new GridBagLayout());
			this.setOpaque(false);

			this.title = t;
			this.tabTitle = new JLabel(title);

			this.createTab();
		}

		public JLabel getButton() {
			return this.closeButton;
		}

		public void createTab() {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			this.add(tabTitle, gbc);
			gbc.gridx++;
			gbc.insets = new Insets(0, 5, 0, 0);
			gbc.anchor = GridBagConstraints.EAST;
			this.add(closeButton, gbc);
		}
	}

	private class CloseTab extends MouseAdapter {
		String title;

		public CloseTab(String title) {
			this.title = title;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			int index = house.indexOfTab(title);
			closeOpenTab(index);
		}
	}

	public DefaultMutableTreeNode loadNodesByNames(DefaultMutableTreeNode node, List<String> originalNames) {
		List<TreeNodeUserObject> args = new ArrayList<>();
		for (String originalName : originalNames) {
			args.add(new TreeNodeUserObject(originalName));
		}
		return loadNodesByUserObj(node, args);
	}

	public DefaultMutableTreeNode loadNodesByUserObj(DefaultMutableTreeNode node, List<TreeNodeUserObject> args) {
		if (args.size() > 0) {
			TreeNodeUserObject name = args.remove(0);
			DefaultMutableTreeNode nod = getChild(node, name);
			if (nod == null)
				nod = new DefaultMutableTreeNode(name);
			node.add(loadNodesByUserObj(nod, args));
		}
		return node;
	}

	@SuppressWarnings("unchecked")
	public DefaultMutableTreeNode getChild(DefaultMutableTreeNode node, TreeNodeUserObject name) {
		Enumeration<DefaultMutableTreeNode> entry = node.children();
		while (entry.hasMoreElements()) {
			DefaultMutableTreeNode nods = entry.nextElement();
			if (((TreeNodeUserObject) nods.getUserObject()).getOriginalName().equals(name.getOriginalName())) {
				return nods;
			}
		}
		return null;
	}

	public void checkFileSelected(File file) {
		this.file = file;
		
		RecentFiles.add(file.getAbsolutePath());
		mainWindow.mainMenuBar.updateRecentFiles();
		verifyFile();
	}

	public void verifyFile() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (file == null) {
						return;
					}
					tree.setModel(new DefaultTreeModel(null));

					// Checking If File is too large 
					if (file.length() > MAX_JAR_FILE_SIZE_BYTES) {
						System.out.println("File Length  " + file.length());
						throw new TooLargeFileException(file.length()); // Throwing Error 
					}
					
					// Throwing error if file does not pass isFile test or canRead Test
					/* isFile() - Tests whether the file denoted by this abstract pathname 
					 * is a normal file. A file is normal if it is not a directory and, 
					 * in addition, satisfies other system-dependent criteria. Any non-directory 
					 * file created by a Java application is guaranteed to be a normal file. */
					
					/*canRead() - checks file privileges and returns true if the privileges allow
					 * the client to read the file */
					
					if(!file.isFile() || !file.canRead()){
						throw new Exception ();
					}else{
						open = true; // boolean to know the file can be opened 
					}
					// Catching and Displaying Error to User 
				} catch (TooLargeFileException e) {
					System.out.println("TooLargeFileException Called ");
					Luyten.showExceptionDialog("File: " + file.getName() + "  (Size:  " + file.length() + " ) too large. " + " Size Limit : " +  MAX_JAR_FILE_SIZE_BYTES, e);
					Luyten.showErrorDialog("File: " + file.getName() + "  (Size:  " + file.length() + " ) too large. " + " Size Limit : " +  MAX_JAR_FILE_SIZE_BYTES);
					Luyten.showInformationDialog("File: " + file.getName() + "  (Size:  " + file.length() + " ) too large. " + " Size Limit : " +  MAX_JAR_FILE_SIZE_BYTES);
					open = false;
				} catch (Exception e1) { //File cannot Open error 
					Luyten.showExceptionDialog("Cannot open " + file.getName() + "!", e1);
					getLabel().setText("Cannot open: " + file.getName());
					open = false;
				} finally {
					try (InputStream in = new FileInputStream(file);) {
						String path = file.getPath().replaceAll("\\\\", "/");
						extractSimpleFileEntryToTextPane(in, file.getName(), path);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					mainWindow.onFileLoadEnded(file, open);
					addFileUploadedToPane(file);
					bar.setVisible(false);
				}
			}

		}).start();
	}
	
	public void addFileUploadedToPane(File file){
		String name = file.getName();
		
		int index = list.getSelectedIndex();
		if(index == -1){ //no selection, so insert at the beginning 
			index = 0;
		}else{
			index++;
		}
		listModel.insertElementAt(name, index);
		list.ensureIndexIsVisible(index);
		list.setVisibleRowCount(index);
	}

	public void changeTheme(String xml) {
		InputStream in = getClass().getResourceAsStream(LuytenPreferences.THEME_XML_PATH + xml);
		try {
			if (in != null) {
				theme = Theme.load(in);
				for (OpenFile f : hmap) {
					theme.apply(f.textArea);
				}
			}
		} catch (Exception e1) {
			Luyten.showExceptionDialog("Exception!", e1);
		}
	}

	public File getOpenedFile() {
		File openedFile = null;
		if (file != null && open) {
			openedFile = file;
		}
		if (openedFile == null) {
			getLabel().setText("No open file");
		}
		return openedFile;
	}

	public String getCurrentTabTitle() {
		String tabTitle = null;
		try {
			int pos = house.getSelectedIndex();
			if (pos >= 0) {
				tabTitle = house.getTitleAt(pos);
			}
		} catch (Exception e1) {
			Luyten.showExceptionDialog("Exception!", e1);
		}
		if (tabTitle == null) {
			getLabel().setText("No open tab");
		}
		return tabTitle;
	}

	public RSyntaxTextArea getCurrentTextArea() {
		RSyntaxTextArea currentTextArea = null;
		try {
			int pos = house.getSelectedIndex();
			if (pos >= 0) {
				RTextScrollPane co = (RTextScrollPane) house.getComponentAt(pos);
				currentTextArea = (RSyntaxTextArea) co.getViewport().getView();
			}
		} catch (Exception e1) {
			Luyten.showExceptionDialog("Exception!", e1);
		}
		if (currentTextArea == null) {
			getLabel().setText("No open tab");
		}
		return currentTextArea;
	}

	public void startWarmUpThread() {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(500);
					String internalName = FindBox.class.getName();
					TypeReference type = metadataSystem.lookupType(internalName);
					TypeDefinition resolvedType = null;
					if ((type == null) || ((resolvedType = type.resolve()) == null)) {
						return;
					}
					StringWriter stringwriter = new StringWriter();
					PlainTextOutput plainTextOutput = new PlainTextOutput(stringwriter);
					plainTextOutput
							.setUnicodeOutputEnabled(decompilationOptions.getSettings().isUnicodeOutputEnabled());
					settings.getLanguage().decompileType(resolvedType, plainTextOutput, decompilationOptions);
					String decompiledSource = stringwriter.toString();
					OpenFile open = new OpenFile(internalName, "*/" + internalName, theme, mainWindow);
					open.setContent(decompiledSource);
					JTabbedPane pane = new JTabbedPane();
					pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
					pane.addTab("title", open.scrollPane);
					pane.setSelectedIndex(pane.indexOfTab("title"));
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			}
		}.start();
	}

	public void navigateTo(final String uniqueStr) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (uniqueStr == null)
					return;
				String[] linkParts = uniqueStr.split("\\|");
				if (linkParts.length <= 1)
					return;
				String destinationTypeStr = linkParts[1];
				try {
					bar.setVisible(true);
					getLabel().setText("Navigating: " + destinationTypeStr.replaceAll("/", "."));

					TypeReference type = metadataSystem.lookupType(destinationTypeStr);
					if (type == null)
						throw new RuntimeException("Cannot lookup type: " + destinationTypeStr);
					TypeDefinition typeDef = type.resolve();
					if (typeDef == null)
						throw new RuntimeException("Cannot resolve type: " + destinationTypeStr);

					String tabTitle = typeDef.getName() + ".class";
					extractClassToTextPane(typeDef, tabTitle, destinationTypeStr, uniqueStr);

					getLabel().setText("Complete");
				} catch (Exception e) {
					getLabel().setText("Cannot navigate: " + destinationTypeStr.replaceAll("/", "."));
					Luyten.showExceptionDialog("Cannot Navigate!", e);
				} finally {
					bar.setVisible(false);
				}
			}
		}).start();
	}

	public JLabel getLabel() {
		return label;
	}

	public void setLabel(JLabel label) {
		this.label = label;
	}

	public State getState() {
		return state;
	}

}
