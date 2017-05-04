package us.deathmarine.luyten;

import com.strobel.Procyon;
import com.strobel.decompiler.DecompilerSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ListIterator;

/**
 * Main menu (only MainWindow should be called from here)
 */
public class MainMenuBar extends JMenuBar {
    private static final long serialVersionUID = -7949855817172562075L;
    private final MainWindow mainWindow;
    //private final Map<String, Language> languageLookup = new HashMap<String, Language>();

    private JMenu recentFiles;
    private JMenuItem clearRecentFiles;


    private DecompilerSettings settings;
    private LuytenPreferences luytenPrefs;

    public MainMenuBar(MainWindow mainWnd) {
        this.mainWindow = mainWnd;
        final ConfigSaver configSaver = ConfigSaver.getLoadedInstance();
        settings = configSaver.getDecompilerSettings();
        luytenPrefs = configSaver.getLuytenPreferences();

        final JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem("..."));
        this.add(fileMenu);

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem("..."));
        this.add(helpMenu);

        // start quicker
        new Thread() {
            public void run() {
                try {
                    // build menu later
                    buildFileMenu(fileMenu);
                    updateRecentFiles();
                    refreshMenuPopup(fileMenu);

                    buildHelpMenu(helpMenu);
                    refreshMenuPopup(helpMenu);
                } catch (Exception e) {
                    Luyten.showExceptionDialog("Exception!", e);
                }
            }

            // refresh currently opened menu
            // (if user selected a menu before it was ready)
            private void refreshMenuPopup(JMenu menu) {
                try {
                    if (menu.isPopupMenuVisible()) {
                        menu.getPopupMenu().setVisible(false);
                        menu.getPopupMenu().setVisible(true);
                    }
                } catch (Exception e) {
                    Luyten.showExceptionDialog("Exception!", e);
                }
            }
        }.start();
    }

    public void updateRecentFiles() {
        if (RecentFiles.paths.isEmpty()) {
            recentFiles.setEnabled(false);
            clearRecentFiles.setEnabled(false);
            return;
        } else {
            recentFiles.setEnabled(true);
            clearRecentFiles.setEnabled(true);
        }

        recentFiles.removeAll();
        ListIterator<String> li = RecentFiles.paths.listIterator(RecentFiles.paths.size());
        boolean rfSaveNeeded = false;

        while (li.hasPrevious()) {
            String path = li.previous();
            final File file = new File(path);

            if (!file.exists()) {
                rfSaveNeeded = true;
                continue;
            }

            JMenuItem menuItem = new JMenuItem(path);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!mainWindow.checkIfFileUploadSizeReached() && !mainWindow.checkIfFileAlreadyAdded(file)) {
                        mainWindow.getModel().checkFileSelected(file);
                    }
                }
            });
            recentFiles.add(menuItem);
        }

        if (rfSaveNeeded)
            RecentFiles.save();
    }

    // Building File Menu
    private void buildFileMenu(final JMenu fileMenu) {

        // Adding Upload File Button
        fileMenu.removeAll();
        JMenuItem menuItem = new JMenuItem("Upload File...");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onOpenFileMenu();
            }
        });
        fileMenu.add(menuItem);
        fileMenu.addSeparator();


        // Adds Recent Files

        recentFiles = new JMenu("Recent Files");
        fileMenu.add(recentFiles);

        clearRecentFiles = new JMenuItem("Clear Recent Files");
        clearRecentFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RecentFiles.paths.clear();
                RecentFiles.save();
                updateRecentFiles();
            }
        });
        fileMenu.add(clearRecentFiles);
        fileMenu.addSeparator();

        // maybe move this somewhere else???
        JMenuItem riverGen = new JMenuItem("New points.txt");
        riverGen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runtime runTime = Runtime.getRuntime();
                try {
                    Process process = runTime.exec(".\\RiverTracer\\RiverTracer.exe");
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });
        fileMenu.add(riverGen);
        fileMenu.addSeparator();


        // Only add the exit command for non-OS X. OS X handles its close
        // automatically
        if (!("true".equals(System.getProperty("us.deathmarine.luyten.Luyten.running_in_osx")))) {
            menuItem = new JMenuItem("Exit");
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mainWindow.onExitMenu();
                }
            });
            fileMenu.add(menuItem);
        }


    }

    private void buildHelpMenu(JMenu helpMenu) {
        helpMenu.removeAll();
        JMenuItem menuItem = new JMenuItem("Legal");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onLegalMenu();
            }
        });
        helpMenu.add(menuItem);


        menuItem = new JMenuItem("About");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JPanel pane = new JPanel();
                pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
                JLabel title = new JLabel("Luyten " + Luyten.getVersion());
                title.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
                pane.add(title);
                pane.add(new JLabel("by Deathmarine"));
                String project = "https://github.com/deathmarine/Luyten/";
                JLabel link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + project + "</U></FONT></HTML>");
                link.setCursor(new Cursor(Cursor.HAND_CURSOR));
                link.addMouseListener(new LinkListener(project, link));
                pane.add(link);
                pane.add(new JLabel("Contributions By:"));
                pane.add(new JLabel("zerdei, toonetown, dstmath"));
                pane.add(new JLabel("virustotalop, xtrafrancyz"));
                pane.add(new JLabel("mbax, quitten, mstrobel, and FisheyLP"));
                pane.add(new JLabel(" "));
                pane.add(new JLabel("Powered By:"));
                String procyon = "https://bitbucket.org/mstrobel/procyon";
                link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + procyon + "</U></FONT></HTML>");
                link.setCursor(new Cursor(Cursor.HAND_CURSOR));
                link.addMouseListener(new LinkListener(procyon, link));
                pane.add(link);
                pane.add(new JLabel("Version: " + Procyon.version()));
                pane.add(new JLabel("(c) 2016 Mike Strobel"));
                String rsyntax = "https://github.com/bobbylight/RSyntaxTextArea";
                link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + rsyntax + "</U></FONT></HTML>");
                link.setCursor(new Cursor(Cursor.HAND_CURSOR));
                link.addMouseListener(new LinkListener(rsyntax, link));
                pane.add(link);
                pane.add(new JLabel("Version: 2.6.1"));
                pane.add(new JLabel("(c) 2017 Robert Futrell"));
                pane.add(new JLabel(" "));
                JOptionPane.showMessageDialog(null, pane);
            }
        });
        helpMenu.add(menuItem);
    }


    private class LinkListener extends MouseAdapter {
        String link;
        JLabel label;

        public LinkListener(String link, JLabel label) {
            this.link = link;
            this.label = label;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            try {
                Desktop.getDesktop().browse(new URI(link));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            label.setText("<HTML><FONT color=\"#00aa99\"><U>" + link + "</U></FONT></HTML>");
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setText("<HTML><FONT color=\"#000099\"><U>" + link + "</U></FONT></HTML>");
        }

    }
}
