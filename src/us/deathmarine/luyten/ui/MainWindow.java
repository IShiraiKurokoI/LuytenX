package us.deathmarine.luyten.ui;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.github.weisj.darklaf.theme.OneDarkTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import us.deathmarine.luyten.Luyten;
import us.deathmarine.luyten.Model;
import us.deathmarine.luyten.RecentFiles;
import us.deathmarine.luyten.config.ConfigSaver;
import us.deathmarine.luyten.config.LuytenPreferences;
import us.deathmarine.luyten.decompiler.FileSaver;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * Dispatcher
 */
public class MainWindow extends JFrame {
    private static final long serialVersionUID = 5265556630724988013L;

    private static final String TITLE = "LuytenX 汉化修复版";
    private static final String DEFAULT_TAB = "#DEFAULT";

    private JProgressBar bar;
    private JLabel label;
    public FindBox findBox;
    private FindAllBox findAllBox;
    private ConfigSaver configSaver;
    private WindowPosition windowPosition;
    private LuytenPreferences luytenPrefs;
    private us.deathmarine.luyten.ui.FileDialog fileDialog;
    private FileSaver fileSaver;
    private JTabbedPane jarsTabbedPane;
    private Map<String, Model> jarModels;
    public MainMenuBar mainMenuBar;

    public MainWindow(File fileFromCommandLine) {
        configSaver = ConfigSaver.getLoadedInstance();
        windowPosition = configSaver.getMainWindowPosition();
        luytenPrefs = configSaver.getLuytenPreferences();

        jarModels = new HashMap<>();
        mainMenuBar = new MainMenuBar(this);
        this.setJMenuBar(mainMenuBar);

        this.adjustWindowPositionBySavedState();
        this.setHideFindBoxOnMainWindowFocus();
        this.setShowFindAllBoxOnMainWindowFocus();
        this.setQuitOnWindowClosing();
        this.setTitle(TITLE);
        this.setIconImage(new ImageIcon(
            Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/LuytenX.png"))).getImage());

        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        label = new JLabel();
        label.setHorizontalAlignment(JLabel.LEFT);
        panel1.setBorder(new BevelBorder(BevelBorder.LOWERED));
        panel1.setPreferredSize(new Dimension(this.getWidth() / 2, 20));
        panel1.add(label);

        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bar = new JProgressBar();

        bar.setStringPainted(true);
        bar.setOpaque(false);
        bar.setVisible(false);
        panel2.setPreferredSize(new Dimension(this.getWidth() / 3, 20));
        panel2.add(bar);
        jarsTabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        jarsTabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tab_placement, int run_count, int max_tab_height) {
                if (jarsTabbedPane.indexOfTab(DEFAULT_TAB) == -1)
                    return super.calculateTabAreaHeight(tab_placement, run_count, max_tab_height);
                else
                    return 0;
            }
        });

        //鼠标中键关闭文件
        MainWindow mainWindow = this;
        jarsTabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    int tabIndex = jarsTabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex !=-1){
                        Model selectedModel = (Model) jarsTabbedPane.getComponentAt(tabIndex);
                        String path = selectedModel.getOpenedFile().getAbsolutePath();
                        selectedModel.closeFile();
                        jarModels.remove(path);
                        if (tabIndex == 0 && jarsTabbedPane.getTabCount() ==1){
                            jarsTabbedPane.addTab(DEFAULT_TAB, new Model(mainWindow));
                        }
                        jarsTabbedPane.remove(tabIndex);
                    }
                }
            }
        });
        jarsTabbedPane.addTab(DEFAULT_TAB, new Model(this));
        this.getContentPane().add(jarsTabbedPane);

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
        Model jarModel = null;
        if (fileFromCommandLine != null) {
            jarModel = loadNewFile(fileFromCommandLine);
        }

        try {
            DropTarget dt = new DropTarget();
            dt.addDropTargetListener(new DropListener(this));
            this.setDropTarget(dt);
        } catch (Exception e) {
            Luyten.showExceptionDialog("发生异常！", e);
        }

        fileDialog = new FileDialog(this);
        fileSaver = new FileSaver(bar, label);

        if (jarModel != null) {
            this.setExitOnEscWhenEnabled(jarModel);
        }

        if (jarModel != null && (fileFromCommandLine.getName().toLowerCase().endsWith(".jar")
            || fileFromCommandLine.getName().toLowerCase().endsWith(".zip"))) {
            jarModel.startWarmUpThread();
        }

        if (RecentFiles.load() > 0) mainMenuBar.updateRecentFiles();
    }

    private void createDefaultTab() {
        jarsTabbedPane.addTab(DEFAULT_TAB, new Model(this));
    }

    private void removeDefaultTab() {
        jarsTabbedPane.remove(jarsTabbedPane.indexOfTab(DEFAULT_TAB));
    }

    public void onOpenFileMenu() {
        File selectedFile = fileDialog.doOpenDialog();
        if (selectedFile != null) {
            System.out.println("[Open]: Opening " + selectedFile.getAbsolutePath());
            this.loadNewFile(selectedFile);
        }
    }

    public Model loadNewFile(final File file) {
        // In case we open the same file again
        // we remove the old entry to force a refresh
        if (jarModels.containsKey(file.getAbsolutePath())) {
            jarModels.remove(file.getAbsolutePath());
            int index = jarsTabbedPane.indexOfTab(file.getName());
            jarsTabbedPane.remove(index);
        }

        Model jarModel = new Model(this);
        jarModel.loadFile(file);
        jarModels.put(file.getAbsolutePath(), jarModel);
        jarsTabbedPane.addTab(file.getName(), jarModel);
        jarsTabbedPane.setSelectedComponent(jarModel);

        final String tabName = file.getName();
        int index = jarsTabbedPane.indexOfTab(tabName);
        Model.Tab tabUI = new Model.Tab(tabName, () -> {
            int index1 = jarsTabbedPane.indexOfTab(tabName);
            jarModels.remove(file.getAbsolutePath());
            jarsTabbedPane.remove(index1);
            if (jarsTabbedPane.getTabCount() == 0) {
                createDefaultTab();
            }
            return null;
        });
        jarsTabbedPane.setTabComponentAt(index, tabUI);
        if (jarsTabbedPane.indexOfTab(DEFAULT_TAB) != -1 && jarsTabbedPane.getTabCount() > 1) {
            removeDefaultTab();
        }
        return jarModel;
    }

    public void onCloseFileMenu() {
        //修复关闭页面时的NullPointerException
        if (getSelectedModel().getOpenedFile() != null){
            String path = getSelectedModel().getOpenedFile().getAbsolutePath();
            int index = jarsTabbedPane.getSelectedIndex();
            this.getSelectedModel().closeFile();
            jarModels.remove(path);

            //同时移除文件标签里的对应tab
            if (index == 0 && jarsTabbedPane.getTabCount() ==1){
                jarsTabbedPane.addTab(DEFAULT_TAB, new Model(this));
            }
            jarsTabbedPane.remove(index);
        }
    }

    public void onSaveAsMenu() {
        RSyntaxTextArea pane = this.getSelectedModel().getCurrentTextArea();
        if (pane == null)
            return;
        String tabTitle = this.getSelectedModel().getCurrentTabTitle();
        if (tabTitle == null)
            return;

        String recommendedFileName = tabTitle.replace(".class", ".java");
        File selectedFile = fileDialog.doSaveDialog(recommendedFileName);
        if (selectedFile != null) {
            fileSaver.saveText(pane.getText(), selectedFile);
        }
    }

    public void onSaveAllMenu() {
        File openedFile = this.getSelectedModel().getOpenedFile();
        if (openedFile == null)
            return;

        String fileName = openedFile.getName();
        if (fileName.endsWith(".class")) {
            fileName = fileName.replace(".class", ".java");
        } else if (fileName.toLowerCase().endsWith(".jar")) {
            fileName = "反编译-" + fileName.replaceAll("\\.[jJ][aA][rR]", ".zip");
        } else {
            fileName = "另存-" + fileName;
        }

        File selectedFileToSave = fileDialog.doSaveAllDialog(fileName);
        if (selectedFileToSave != null) {
            fileSaver.saveAllDecompiled(openedFile, selectedFileToSave);
        }
    }

    public void onExitMenu() {
        quit();
    }

    public void onSelectAllMenu() {
        try {
            RSyntaxTextArea pane = this.getSelectedModel().getCurrentTextArea();
            if (pane != null) {
                pane.requestFocusInWindow();
                pane.setSelectionStart(0);
                pane.setSelectionEnd(pane.getText().length());
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("发生异常！", e);
        }
    }

    public void onFindMenu() {
        try {
            RSyntaxTextArea pane = this.getSelectedModel().getCurrentTextArea();
            if (pane != null) {
                if (findBox == null)
                    findBox = new FindBox(this);
                findBox.showFindBox();
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("发生异常！", e);
        }
    }

    public void onFindAllMenu() {
        try {
            if (findAllBox == null)
                findAllBox = new FindAllBox(this);
            findAllBox.showFindBox();

        } catch (Exception e) {
            Luyten.showExceptionDialog("发生异常！", e);
        }
    }

    public void onLegalMenu() {
        new Thread(() -> {
            try {
                bar.setVisible(true);
                bar.setIndeterminate(true);
                String legalStr = getLegalStr();
                getSelectedModel().showLegal(legalStr);
            } finally {
                bar.setIndeterminate(false);
                bar.setVisible(false);
            }
        }).start();
    }

    public void onListLoadedClasses() {
        try {
            StringBuilder sb = new StringBuilder();
            ClassLoader myCL = Thread.currentThread().getContextClassLoader();
            bar.setVisible(true);
            bar.setIndeterminate(true);
            while (myCL != null) {
                sb.append("ClassLoader: ").append(myCL).append("\n");
                for (Iterator<?> iter = list(myCL); iter != null && iter.hasNext(); ) {
                    sb.append("\t").append(iter.next()).append("\n");
                }
                myCL = myCL.getParent();
            }
            this.getSelectedModel().show("Debug", sb.toString());
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
            Luyten.showExceptionDialog("发生异常！", e);
        }
        return null;
    }

    @SuppressWarnings("ConstantConditions")
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
                new InputStreamReader(getClass().getResourceAsStream("/distfiles/CFR.License.txt")));
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
            sb.append("\n\n\n\n\n");
            reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/distfiles/Vineflower.License.txt")));
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
            sb.append("\n\n\n\n\n");
            reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/distfiles/Kotlinp.License.txt")));
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
            sb.append("\n\n\n\n\n");
            reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/distfiles/RSyntaxTextArea.License.txt")));
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
            sb.append("\n\n\n\n\n");
            reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/distfiles/DarkLaf.License.txt")));
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
        } catch (IOException e) {
            Luyten.showExceptionDialog("发生异常！", e);
        }
        return sb.toString();
    }

    public void onThemesChanged() {
        String xml = luytenPrefs.getThemeXml();
        if (xml.contains("dark")){
            if (!(LafManager.getInstalledTheme() instanceof OneDarkTheme)){
                LafManager.setTheme(new OneDarkTheme());
                LafManager.install();
            }
        }else {
            if (!(LafManager.getInstalledTheme() instanceof IntelliJTheme)){
                LafManager.setTheme(new IntelliJTheme());
                LafManager.install();
            }
        }
        for (Model jarModel : jarModels.values()) {
            jarModel.changeTheme(xml);
            luytenPrefs.setFont_size(jarModel.getTheme().baseFont.getSize());
        }
    }

    public void onSettingsChanged() {
        for (Model jarModel : jarModels.values()) {
            jarModel.updateOpenClasses();
        }
    }

    public void onTreeSettingsChanged() {
        for (Model jarModel : jarModels.values()) {
            jarModel.updateTree();
        }
    }

    public void onFileDropped(File file) {
        if (file != null) {
            this.loadNewFile(file);
        }
    }

    public void onFileLoadEnded(File file, boolean isSuccess) {
        try {
            if (file != null && isSuccess) {
                this.setTitle(TITLE + " - " + file.getName());
            } else {
                this.setTitle(TITLE);
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("发生异常！", e);
        }
    }

    public void onNavigationRequest(String uniqueStr) {
        this.getSelectedModel().navigateTo(uniqueStr);
    }

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

    @SuppressWarnings("finally")
    private void quit() {
        try {
            windowPosition.readPositionFromWindow(this);
            configSaver.saveConfig();
        } catch (Exception e) {
            Luyten.showExceptionDialog("发生异常！", e);
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

    public Model getSelectedModel() {
        return (Model) jarsTabbedPane.getSelectedComponent();
    }

    public JProgressBar getBar() {
        return bar;
    }

    public JLabel getLabel() {
        return label;
    }
}
