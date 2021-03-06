package us.deathmarine.luyten;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.Animator;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.jar.JarFile;


/**
 * Jar-level model
 */
public class Model extends JSplitPane {
    private static final long serialVersionUID = 6896857630400910200L;
    //Max file size allowed
    private static final long MAX_JAR_FILE_SIZE_BYTES = 1_000_000_000;
    private static LuytenTypeLoader typeLoader = new LuytenTypeLoader();
    public static MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
    final DrawMap dm = new DrawMap();
    public JTabbedPane house;
    private JTree tree;
    private JList<String> list; // Used to display List of files user decided to upload
    private DefaultListModel<String> listModel; //Model holds the Items
    private JButton submitFileButton; // Buttons to Submit the files Line 140
    private JButton startSimulation; // Button to Start the Simulation Line 128
    private JButton resetSimulationBtn; //Used to reset everything so user can rerun Simulation
    private File file;
    private DecompilerSettings settings;
    private DecompilationOptions decompilationOptions;
    private Theme theme;
    private MainWindow mainWindow;
    private JLabel label;
    private HashSet<OpenFile> hmap = new HashSet<OpenFile>();
    private boolean open = false;

    // filesSubmitted allows functions to check if the files have been submitted
    private boolean filesSubmitted = false;
    private ConfigSaver configSaver;

    // Building Panes for the MainWindow
    public Model(final MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        mainWindow.getBar();
        this.setLabel(mainWindow.getLabel());

        configSaver = ConfigSaver.getLoadedInstance();
        settings = configSaver.getDecompilerSettings();

        tree = new JTree();
        tree.setModel(new DefaultTreeModel(null));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new CellRenderer());

        /***This list is used to display the files chosen by user to upload*****/
        listModel = new DefaultListModel<String>();

        list = new JList<String>(listModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1); //lets list know it can create a scroll of the list items anytime the view is not
        // big enough
        list.setFont(new Font("Times New Roman",Font.BOLD,22)); //Adding font to the files uploaded

        // renderer allows JList to center String Names Added
        DefaultListCellRenderer renderer = (DefaultListCellRenderer) list.getCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);

        // The JSrollPane provides a scrollable view for list
        JScrollPane listScrollPane = new JScrollPane(list);
        // Adding Key Click listeners to list, and listModel
        addUploadedFilesListKeyListener(list, listModel);

        // leftMainPanel will be a container for all other left panels
        JPanel leftMainPanel = new JPanel();
        leftMainPanel.setLayout(new BoxLayout(leftMainPanel, BoxLayout.Y_AXIS));


        /*********************** Upload Panel for Files Names ****************/
        JPanel uploadFileLeftPanel = new JPanel();
        uploadFileLeftPanel.setLayout(new BoxLayout(uploadFileLeftPanel, 1));
        uploadFileLeftPanel.setBorder(BorderFactory.createTitledBorder(null, "Files Uploaded", TitledBorder.CENTER, TitledBorder.CENTER, new Font("times new roman",Font.PLAIN,16)));
        uploadFileLeftPanel.setToolTipText("Use Delete or BackSpace to Remove Uploaded File");
        uploadFileLeftPanel.add(listScrollPane);


        /**********************startSimulation File Button**************/

        startSimulation = new JButton("Upload Files..");
        startSimulation.setIcon(new ImageIcon(getClass().getResource("/resources/famfamfam_mini_icons/folder_new.gif")));
        startSimulation.setHorizontalAlignment(SwingConstants.LEFT);
        startSimulation.setToolTipText("Maximum of 3 Files Allowed");

        startSimulation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                mainWindow.onOpenFileMenu();
            }
        });

        /******************* Submit File Button ******************************/
        submitFileButton = new JButton("Start Simulation");
        submitFileButton.setIcon(new ImageIcon(getClass().getResource("/resources/famfamfam_mini_icons/action_go.gif")));
        submitFileButton.setHorizontalAlignment(SwingConstants.LEFT);
        submitFileButton.setEnabled(false);

        // This Listener Detects Submit Button Click
        submitFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                onSubmitButtonClicked();
            }
        });

        /********************Button Panel For the Buttons ****************/
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, 0));
        buttonPanel.add(submitFileButton);
        buttonPanel.add(startSimulation, 0);

        /**********************Reset Simulation Button *********************/
        resetSimulationBtn = new JButton("Reset");
        resetSimulationBtn.setIcon(new ImageIcon(getClass().getResource("/resources/famfamfam_mini_icons/page_refresh.gif")));
        resetSimulationBtn.setHorizontalAlignment(SwingConstants.LEFT);
        resetSimulationBtn.setEnabled(false);

        /********************Second Simulation Button Panel ************************/
        JPanel simulationButtonPanel = new JPanel();
        simulationButtonPanel.setLayout(new BoxLayout(simulationButtonPanel,0));
        //simulationButtonPanel.add(resetSimulationBtn,0);


        /*********************Adding items to Left Main Panel *************/
        leftMainPanel.add(uploadFileLeftPanel);
        leftMainPanel.add(buttonPanel);
        leftMainPanel.add(simulationButtonPanel);

        /******************** Panel 3 For Test ************************/

        JPanel panel3 = new JPanel();
        panel3.setLayout(new BoxLayout(panel3, 1));
        panel3.setBorder(BorderFactory.createTitledBorder("Simulation Controls"));
        panel3.add(new JScrollPane(tree));

        leftMainPanel.add(panel3);


         /***** Main Panel This is where the map is added *****/
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 1));

        panel.setBorder(BorderFactory.createTitledBorder("Map"));

        GLJPanel gljpanel = new GLJPanel(new GLCapabilities(GLProfile.getDefault()));
        gljpanel.setPreferredSize(new Dimension(400, 400));


        final Animator a = new Animator();
        a.add(gljpanel);
        a.start();
        gljpanel.addGLEventListener(new GLEventListener() {
            @Override
            public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
                dm.setup(glautodrawable.getGL().getGL2(), width, height);
            }

            @Override
            public void init(GLAutoDrawable glautodrawable) {
            }

            @Override
            public void dispose(GLAutoDrawable glautodrawable) {
            }

            @Override
            public void display(GLAutoDrawable glautodrawable) {
                dm.render(glautodrawable.getGL().getGL2(), glautodrawable.getSurfaceWidth(), glautodrawable.getSurfaceHeight());
            }
        });
        panel.add(gljpanel);


        /***************** Setting The Panels ***************************/
        this.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        this.setDividerLocation(280 % mainWindow.getWidth());
        this.setLeftComponent(leftMainPanel);
        this.setRightComponent(panel);

        decompilationOptions = new DecompilationOptions();
        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);
    }
    
    //--------------- Model Methods -------------------------

    // BackSpace and Delete Listener for List that holds Uploaded Files
    private void addUploadedFilesListKeyListener(final JList<String> list, final DefaultListModel<String> listModel) {
        list.addKeyListener(new KeyListener() {
            //KeyListener allows me too assign tasks too keyboard keys

            //When a key is clicked it check if it's the delete of back space key , if so I delete the selected files
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    //Deleting selected files
                    deleteUploadedFiles();
                }
            }

            //I don't need this functions but I have to implement them for the Interface KeyListener()
            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });
    }

    // Deleting uploaded Files if the user has not submitted
    private void deleteUploadedFiles() {
        // If user has submitted then files will not be uploaded
        if (filesSubmitted) {
            return;
        }
        // If there's nothing in the JList or the User didn't Select
        // a file to delete nothing happens
        if (list.getSelectedIndices().length > 0) {
            // Getting the amount of files the user selected 
            int[] selectedIndices = list.getSelectedIndices();
            for (int i = 0; i <= selectedIndices.length - 1; i++) {
                //list.getSelectedIndex deletes the first selected file
                mainWindow.removeFile(listModel.getElementAt(list.getSelectedIndex()));
                listModel.removeElementAt(list.getSelectedIndex());// Deleting Selected Items
                //Removing files means user doesn't have 3 files uploaded so the submit button is decativated 
                submitButtonAccess(false);
            }
        }
    }


    //Asks the MainWindows to Check if files are ready to be Submits The files
    public void onSubmitButtonClicked() {
        filesSubmitted = mainWindow.onSubmitFilesButtonClicked();
        if (filesSubmitted) {
            submitButtonAccess(false);
        }
    }

    // Uploads files Chosen from Recent Files Menu
    public void checkFileSelected(File file) {
        this.file = file;

        RecentFiles.add(file.getAbsolutePath());
        mainWindow.mainMenuBar.updateRecentFiles();
        verifyFile();
    }

    // Verifies files to see if its valid
    private void verifyFile() {
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
                        throw new TooLargeFileException(file.length()); // Throwing
                        // Error
                    }

                    // Throwing error if file does not pass isFile test or
                    // canRead Test
                    /*
                     * isFile() - Tests whether the file denoted by this
					 * abstract pathname is a normal file. A file is normal if
					 * it is not a directory and, in addition, satisfies other
					 * system-dependent criteria. Any non-directory file created
					 * by a Java application is guaranteed to be a normal file.
					 */

					/*
                     * canRead() - checks file privileges and returns true if
					 * the privileges allow the client to read the file
					 */

                    if (!file.isFile() || !file.canRead()) {
                        throw new Exception();
                    } else {
                        open = true; // boolean to know the file can be opened
                    }
                    // Catching and Displaying Error to User
                } catch (TooLargeFileException e) {
                    System.out.println("TooLargeFileException Called ");
                    Luyten.showInformationDialog("File: " + file.getName() + "  (Size:  " + file.length()
                            + " ) too large. " + " Size Limit : " + MAX_JAR_FILE_SIZE_BYTES);
                    open = false;
                } catch (Exception e1) { // File cannot Open error
                    Luyten.showExceptionDialog("Cannot open " + file.getName() + "!", e1);
                    getLabel().setText("Cannot open: " + file.getName());
                    open = false;
                } finally {
                    mainWindow.onFileLoadEnded(file, open);
                    addFileUploadedToPane(file);
                }
            }

        }).start();
    }

    // Adds file to Files Uploaded Files Pane
    private void addFileUploadedToPane(File file) {
        String name = file.getName();

        int index = list.getSelectedIndex();
        if (index == -1) { // no selection, so insert at the beginning
            index = 0;
        } else {
            index++;
        }
        listModel.insertElementAt(name, index);
        list.ensureIndexIsVisible(index);
        list.setVisibleRowCount(index);
    }

    // Enables or Disables submitFileButton
    public void submitButtonAccess(Boolean access) {
        submitFileButton.setEnabled(access);
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

    public JLabel getLabel() {
        return label;
    }

    public void setLabel(JLabel label) {
        this.label = label;
    }

    //This Function is called when the start simulation button is clicked
    // Submits data to DrawMap for DrawMap to simulate
    public void submitData(float[][] data) {
        double data2[][] = new double[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data2[i][j] = (double) data[i][j];
            }
        }
//		for (int index = 0; index < 3){
//			System.out.print();
//		}
        dm.setDataPoints(data2);
        dm.play();

    }

    final class State implements AutoCloseable {
        final JarFile jarFile;
        final ITypeLoader typeLoader;
        private final String key;
        private final File file;

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
    }
}
