package us.deathmarine.luyten.ui;

import com.strobel.Procyon;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import org.benf.cfr.reader.util.CfrVersionInfo;
import us.deathmarine.luyten.Luyten;
import us.deathmarine.luyten.RecentFiles;
import us.deathmarine.luyten.config.ConfigSaver;
import us.deathmarine.luyten.config.LuytenPreferences;
import us.deathmarine.luyten.decompiler.Decompiler;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.util.*;

/**
 * Main menu (only MainWindow should be called from here)
 */
public class MainMenuBar extends JMenuBar {
    private static final long serialVersionUID = -7949855817172562075L;
    private final MainWindow mainWindow;
    private final Map<String, Language> languageLookup = new HashMap<>();

    private JMenu recentFiles;
    private JMenuItem clearRecentFiles;
    private JCheckBoxMenuItem flattenSwitchBlocks;
    private JCheckBoxMenuItem forceExplicitImports;
    private JCheckBoxMenuItem forceExplicitTypes;
    private JCheckBoxMenuItem showSyntheticMembers;
    private JCheckBoxMenuItem excludeNestedTypes;
    private JCheckBoxMenuItem retainRedundantCasts;
    private JCheckBoxMenuItem unicodeReplacement;
    private JCheckBoxMenuItem debugLineNumbers;
    private JCheckBoxMenuItem showDebugInfo;
    private JCheckBoxMenuItem bytecodeLineNumbers;
    private JRadioButtonMenuItem java;
    private JRadioButtonMenuItem bytecode;
    private JRadioButtonMenuItem bytecodeAST;
    private ButtonGroup languagesGroup;
    private ButtonGroup themesGroup;
    private ButtonGroup decompilersGroup;
    private JCheckBoxMenuItem packageExplorerStyle;
    private JCheckBoxMenuItem filterOutInnerClassEntries;
    private JCheckBoxMenuItem singleClickOpenEnabled;
    private JCheckBoxMenuItem exitByEscEnabled;
    private final DecompilerSettings settings;
    private LuytenPreferences luytenPrefs;

    public MainMenuBar(MainWindow mainWnd) {
        this.mainWindow = mainWnd;
        final ConfigSaver configSaver = ConfigSaver.getLoadedInstance();
        settings = configSaver.getDecompilerSettings();
        luytenPrefs = configSaver.getLuytenPreferences();

        final JMenu fileMenu = new JMenu("文件");
        fileMenu.add(new JMenuItem("..."));
        this.add(fileMenu);
        final JMenu editMenu = new JMenu("编辑");
        editMenu.add(new JMenuItem("..."));
        this.add(editMenu);
        final JMenu themesMenu = new JMenu("主题");
        themesMenu.add(new JMenuItem("..."));
        this.add(themesMenu);
        final JMenu decompilersMenu = new JMenu("反编译器");
        decompilersMenu.add(new JMenuItem("..."));
        this.add(decompilersMenu);
        final JMenu operationMenu = new JMenu("操作");
        operationMenu.add(new JMenuItem("..."));
        this.add(operationMenu);
        final JMenu settingsMenu = new JMenu("设置");
        settingsMenu.add(new JMenuItem("..."));
        this.add(settingsMenu);
        final JMenu helpMenu = new JMenu("帮助");
        helpMenu.add(new JMenuItem("..."));
        this.add(helpMenu);

        // start quicker
        new Thread() {
            public void run() {
                try {
                    // build menu later
                    buildFileMenu(fileMenu);
                    refreshMenuPopup(fileMenu);

                    buildEditMenu(editMenu);
                    refreshMenuPopup(editMenu);

                    buildThemesMenu(themesMenu);
                    refreshMenuPopup(themesMenu);

                    buildDecompilersMenu(decompilersMenu);
                    refreshMenuPopup(decompilersMenu);

                    buildOperationMenu(operationMenu);
                    refreshMenuPopup(operationMenu);

                    buildSettingsMenu(settingsMenu, configSaver);
                    refreshMenuPopup(settingsMenu);

                    buildHelpMenu(helpMenu);
                    refreshMenuPopup(helpMenu);

                    updateRecentFiles();
                } catch (Exception e) {
                    Luyten.showExceptionDialog("发生异常！", e);
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
                    Luyten.showExceptionDialog("发生异常！", e);
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
            menuItem.addActionListener(e -> mainWindow.loadNewFile(file));
            recentFiles.add(menuItem);
        }

        if (rfSaveNeeded) RecentFiles.save();
    }

    @SuppressWarnings("deprecation")
    private void buildFileMenu(final JMenu fileMenu) {
        fileMenu.removeAll();
        JMenuItem menuItem = new JMenuItem("打开文件...");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(e -> mainWindow.onOpenFileMenu());
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("关闭文件");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(e -> {
            JTabbedPane house = mainWindow.getSelectedModel().house;

            if (e.getModifiers() != InputEvent.CTRL_MASK || house.getTabCount() == 0)
                mainWindow.onCloseFileMenu();
            else {
                mainWindow.getSelectedModel().closeOpenTab(house.getSelectedIndex());
            }
        });
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("另存为...");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(e -> mainWindow.onSaveAsMenu());
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("全部另存为...");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(e -> mainWindow.onSaveAllMenu());
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        recentFiles = new JMenu("最近打开的文件");
        fileMenu.add(recentFiles);

        clearRecentFiles = new JMenuItem("清空最近打开的文件记录");
        clearRecentFiles.addActionListener(e -> {
            RecentFiles.paths.clear();
            RecentFiles.save();
            updateRecentFiles();
        });
        fileMenu.add(clearRecentFiles);

        fileMenu.addSeparator();

        // Only add the exit command for non-OS X. OS X handles its close
        // automatically
        if (!Boolean.getBoolean("apple.laf.useScreenMenuBar")) {
            menuItem = new JMenuItem("Exit");
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
            menuItem.addActionListener(e -> mainWindow.onExitMenu());
            fileMenu.add(menuItem);
        }
    }

    @SuppressWarnings("deprecation")
    private void buildEditMenu(JMenu editMenu) {
        editMenu.removeAll();
        JMenuItem menuItem = new JMenuItem("剪切");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setEnabled(false);
        editMenu.add(menuItem);

        menuItem = new JMenuItem("复制");
        menuItem.addActionListener(new DefaultEditorKit.CopyAction());
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        editMenu.add(menuItem);

        menuItem = new JMenuItem("粘贴");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setEnabled(false);
        editMenu.add(menuItem);

        editMenu.addSeparator();

        menuItem = new JMenuItem("全选");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(e -> mainWindow.onSelectAllMenu());
        editMenu.add(menuItem);
        editMenu.addSeparator();

        menuItem = new JMenuItem("查找...");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(e -> mainWindow.onFindMenu());
        editMenu.add(menuItem);

        menuItem = new JMenuItem("查找下一个");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        menuItem.addActionListener(e -> {
            if (mainWindow.findBox != null) mainWindow.findBox.fireExploreAction(true);
        });
        editMenu.add(menuItem);

        menuItem = new JMenuItem("查找上一个");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK));
        menuItem.addActionListener(e -> {
            if (mainWindow.findBox != null) mainWindow.findBox.fireExploreAction(false);
        });
        editMenu.add(menuItem);

        menuItem = new JMenuItem("全局查找");
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(e -> mainWindow.onFindAllMenu());
        editMenu.add(menuItem);
    }

    private void buildThemesMenu(JMenu themesMenu) {
        themesMenu.removeAll();
        themesGroup = new ButtonGroup();
        JRadioButtonMenuItem a = new JRadioButtonMenuItem(new ThemeAction("默认", "default.xml"));
        a.setSelected("default.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("默认-Alt", "default-alt.xml"));
        a.setSelected("default-alt.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("暗黑", "dark.xml"));
        a.setSelected("dark.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("雅黑", "onedark.xml"));
        a.setSelected("onedark.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("Eclipse", "eclipse.xml"));
        a.setSelected("eclipse.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("Visual Studio", "vs.xml"));
        a.setSelected("vs.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("IntelliJ", "idea.xml"));
        a.setSelected("idea.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);
    }

    private void buildDecompilersMenu(JMenu decompilersMenu) {
        decompilersMenu.removeAll();
        decompilersGroup = new ButtonGroup();
        Arrays.stream(Decompiler.values()).forEach(dec -> {
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(new DecompilerAction(dec.getProperName(), dec));
            mi.setSelected(dec.equals(luytenPrefs.getDecompiler()));
            decompilersGroup.add(mi);
            decompilersMenu.add(mi);
        });
    }

    private void buildOperationMenu(JMenu operationMenu) {
        operationMenu.removeAll();
        packageExplorerStyle = new JCheckBoxMenuItem("包浏览器风格目录");
        packageExplorerStyle.setSelected(luytenPrefs.isPackageExplorerStyle());
        packageExplorerStyle.addActionListener(e -> {
            luytenPrefs.setPackageExplorerStyle(packageExplorerStyle.isSelected());
            mainWindow.onTreeSettingsChanged();
        });
        operationMenu.add(packageExplorerStyle);

        filterOutInnerClassEntries = new JCheckBoxMenuItem("过滤掉内部类条目");
        filterOutInnerClassEntries.setSelected(luytenPrefs.isFilterOutInnerClassEntries());
        filterOutInnerClassEntries.addActionListener(e -> {
            luytenPrefs.setFilterOutInnerClassEntries(filterOutInnerClassEntries.isSelected());
            mainWindow.onTreeSettingsChanged();
        });
        operationMenu.add(filterOutInnerClassEntries);

        singleClickOpenEnabled = new JCheckBoxMenuItem("单击打开");
        singleClickOpenEnabled.setSelected(luytenPrefs.isSingleClickOpenEnabled());
        singleClickOpenEnabled.addActionListener(e -> luytenPrefs.setSingleClickOpenEnabled(singleClickOpenEnabled.isSelected()));
        operationMenu.add(singleClickOpenEnabled);

        exitByEscEnabled = new JCheckBoxMenuItem("按 Esc 退出");
        exitByEscEnabled.setSelected(luytenPrefs.isExitByEscEnabled());
        exitByEscEnabled.addActionListener(e -> luytenPrefs.setExitByEscEnabled(exitByEscEnabled.isSelected()));
        operationMenu.add(exitByEscEnabled);
    }

    private void buildSettingsMenu(JMenu settingsMenu, ConfigSaver configSaver) {
        settingsMenu.removeAll();
        ActionListener settingsChanged = e -> new Thread(() -> {
            populateSettingsFromSettingsMenu();
            mainWindow.onSettingsChanged();
        }).start();
        flattenSwitchBlocks = new JCheckBoxMenuItem("展开Switch块");
        flattenSwitchBlocks.setSelected(settings.getFlattenSwitchBlocks());
        flattenSwitchBlocks.addActionListener(settingsChanged);
        settingsMenu.add(flattenSwitchBlocks);

        forceExplicitImports = new JCheckBoxMenuItem("强制显式导入");
        forceExplicitImports.setSelected(settings.getForceExplicitImports());
        forceExplicitImports.addActionListener(settingsChanged);
        settingsMenu.add(forceExplicitImports);

        forceExplicitTypes = new JCheckBoxMenuItem("强制显式类型");
        forceExplicitTypes.setSelected(settings.getForceExplicitTypeArguments());
        forceExplicitTypes.addActionListener(settingsChanged);
        settingsMenu.add(forceExplicitTypes);

        showSyntheticMembers = new JCheckBoxMenuItem("显示合成成员");
        showSyntheticMembers.setSelected(settings.getShowSyntheticMembers());
        showSyntheticMembers.addActionListener(settingsChanged);
        settingsMenu.add(showSyntheticMembers);

        excludeNestedTypes = new JCheckBoxMenuItem("排除嵌套类型");
        excludeNestedTypes.setSelected(settings.getExcludeNestedTypes());
        excludeNestedTypes.addActionListener(settingsChanged);
        settingsMenu.add(excludeNestedTypes);

        retainRedundantCasts = new JCheckBoxMenuItem("保留冗余强制转换");
        retainRedundantCasts.setSelected(settings.getRetainRedundantCasts());
        retainRedundantCasts.addActionListener(settingsChanged);
        settingsMenu.add(retainRedundantCasts);

        unicodeReplacement = new JCheckBoxMenuItem("启用 Unicode 替换");
        unicodeReplacement.setSelected(settings.isUnicodeOutputEnabled());
        unicodeReplacement.addActionListener(settingsChanged);
        settingsMenu.add(unicodeReplacement);

        debugLineNumbers = new JCheckBoxMenuItem("显示调试行号");
        debugLineNumbers.setSelected(settings.getShowDebugLineNumbers());
        debugLineNumbers.addActionListener(settingsChanged);
        settingsMenu.add(debugLineNumbers);

        JMenu debugSettingsMenu = new JMenu("调试设置");
        showDebugInfo = new JCheckBoxMenuItem("包含错误诊断");
        showDebugInfo.setSelected(settings.getIncludeErrorDiagnostics());
        showDebugInfo.addActionListener(settingsChanged);

        debugSettingsMenu.add(showDebugInfo);
        settingsMenu.add(debugSettingsMenu);
        settingsMenu.addSeparator();

        languageLookup.put(Languages.java().getName(), Languages.java());
        languageLookup.put(Languages.bytecode().getName(), Languages.bytecode());
        languageLookup.put(Languages.bytecodeAst().getName(), Languages.bytecodeAst());

        languagesGroup = new ButtonGroup();
        java = new JRadioButtonMenuItem(Languages.java().getName());
        java.getModel().setActionCommand(Languages.java().getName());
        java.setSelected(Languages.java().getName().equals(settings.getLanguage().getName()));
        languagesGroup.add(java);
        settingsMenu.add(java);
        bytecode = new JRadioButtonMenuItem(Languages.bytecode().getName());
        bytecode.getModel().setActionCommand(Languages.bytecode().getName());
        bytecode.setSelected(Languages.bytecode().getName().equals(settings.getLanguage().getName()));
        languagesGroup.add(bytecode);
        settingsMenu.add(bytecode);
        bytecodeAST = new JRadioButtonMenuItem(Languages.bytecodeAst().getName());
        bytecodeAST.getModel().setActionCommand(Languages.bytecodeAst().getName());
        bytecodeAST.setSelected(Languages.bytecodeAst().getName().equals(settings.getLanguage().getName()));
        languagesGroup.add(bytecodeAST);
        settingsMenu.add(bytecodeAST);

        JMenu debugLanguagesMenu = new JMenu("调试语言");
        for (final Language language : Languages.debug()) {
            final JRadioButtonMenuItem m = new JRadioButtonMenuItem(language.getName());
            m.getModel().setActionCommand(language.getName());
            m.setSelected(language.getName().equals(settings.getLanguage().getName()));
            languagesGroup.add(m);
            debugLanguagesMenu.add(m);
            languageLookup.put(language.getName(), language);
        }
        for (AbstractButton button : Collections.list(languagesGroup.getElements())) {
            button.addActionListener(settingsChanged);
        }
        settingsMenu.add(debugLanguagesMenu);

        bytecodeLineNumbers = new JCheckBoxMenuItem("显示字节码中的行号");
        bytecodeLineNumbers.setSelected(settings.getIncludeLineNumbersInBytecode());
        bytecodeLineNumbers.addActionListener(settingsChanged);
        settingsMenu.add(bytecodeLineNumbers);
    }

    private void buildHelpMenu(JMenu helpMenu) {
        helpMenu.removeAll();
        JMenuItem menuItem = new JMenuItem("许可");
        menuItem.addActionListener(e -> mainWindow.onLegalMenu());
        helpMenu.add(menuItem);
        JMenu menuDebug = new JMenu("调试");
        menuItem = new JMenuItem("列出 JVM 类");
        menuItem.addActionListener(e -> mainWindow.onListLoadedClasses());
        menuDebug.add(menuItem);
        helpMenu.add(menuDebug);
        menuItem = new JMenuItem("关于");
        menuItem.addActionListener(event -> {
            JPanel pane = new JPanel();
            pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
            JLabel title = new JLabel("LuytenX " + Luyten.getVersion());
            title.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            pane.add(title);
            pane.add(new JLabel("by Deathmarine & ByteZ"));
            String project = "https://github.com/ByteZ1337/LuytenX";
            JLabel link = new JLabel("<html><font color=\"#03bafc\"><u>" + project + "</u></font></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new LinkListener(project, link));
            pane.add(link);
            pane.add(new JLabel("贡献者:"));
            pane.add(new JLabel("zerdei, toonetown, dstmath"));
            pane.add(new JLabel("virustotalop, xtrafrancyz,"));
            pane.add(new JLabel("mbax, quitten, mstrobel,"));
            pane.add(new JLabel("FisheyLP, and Syquel"));
            pane.add(new JLabel(" "));
            pane.add(new JLabel("汉化:"));
            pane.add(new JLabel("IShiraiKurokoI"));
            String chinese = "https://github.com/IShiraiKurokoI/LuytenX";
            link = new JLabel("<html><font color=\"#03bafc\"><u>" + chinese + "</u></font></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new LinkListener(chinese, link));
            pane.add(link);
            pane.add(new JLabel(" "));
            pane.add(new JLabel("由以下项目组成:"));

            String procyon = "https://github.com/mstrobel/procyon";
            link = new JLabel("<html><font color=\"#03bafc\"><u>" + procyon + "</u></font></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new LinkListener(procyon, link));
            pane.add(link);
            pane.add(new JLabel("版本: " + Procyon.version()));
            pane.add(new JLabel("(c) 2018 Mike Strobel"));

            String cfr = "https://github.com/leibnitz27/cfr";
            link = new JLabel("<html><font color=\"#03bafc\"><u>" + cfr + "</u></font></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new LinkListener(cfr, link));
            pane.add(link);
            pane.add(new JLabel("版本: " + CfrVersionInfo.VERSION_INFO));
            pane.add(new JLabel("(c) 2011-2019 Lee Benfield"));

            String vineflower = "https://github.com/Vineflower/vineflower";
            link = new JLabel("<html><font color=\"#03bafc\"><u>" + vineflower + "</u></font></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new LinkListener(vineflower, link));
            pane.add(link);
            pane.add(new JLabel("版本: 1.9.1"));
            pane.add(new JLabel("(c) 2023 Vineflower Contributors"));

            String kotlinp = "https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlinp";
            link = new JLabel("<html><font color=\"#03bafc\"><u>" + kotlinp + "</u></font></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new LinkListener(kotlinp, link));
            pane.add(link);
            pane.add(new JLabel("版本: 1.9.1"));
            pane.add(new JLabel("(c) 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors"));

            String rsyntax = "https://github.com/bobbylight/RSyntaxTextArea";
            link = new JLabel("<html><font color=\"#03bafc\"><u>" + rsyntax + "</u></font></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new LinkListener(rsyntax, link));
            pane.add(link);
            pane.add(new JLabel("版本: 3.3.0"));
            pane.add(new JLabel("(c) 2021 Robert Futrell"));

            String darkLaf = "https://github.com/weisJ/darklaf/";
            link = new JLabel("<html><font color=\"#03bafc\"><u>" + darkLaf + "</u></font></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new LinkListener(darkLaf, link));
            pane.add(link);
            pane.add(new JLabel("版本: 3.0.2"));
            pane.add(new JLabel("(c) 2019-2022 Jannis Weis"));
            pane.add(new JLabel(" "));
            JOptionPane.showMessageDialog(null, pane);
        });
        helpMenu.add(menuItem);
    }

    private void populateSettingsFromSettingsMenu() {
        // synchronized: do not disturb decompiler at work (synchronize every
        // time before run decompiler)
        synchronized (settings) {
            settings.setFlattenSwitchBlocks(flattenSwitchBlocks.isSelected());
            settings.setForceExplicitImports(forceExplicitImports.isSelected());
            settings.setShowSyntheticMembers(showSyntheticMembers.isSelected());
            settings.setExcludeNestedTypes(excludeNestedTypes.isSelected());
            settings.setForceExplicitTypeArguments(forceExplicitTypes.isSelected());
            settings.setRetainRedundantCasts(retainRedundantCasts.isSelected());
            settings.setIncludeErrorDiagnostics(showDebugInfo.isSelected());
            settings.setUnicodeOutputEnabled(unicodeReplacement.isSelected());
            settings.setShowDebugLineNumbers(debugLineNumbers.isSelected());
            //
            // Note: You shouldn't ever need to set this. It's only for
            // languages that support catch
            // blocks without an exception variable. Java doesn't allow this. I
            // think Scala does.
            //
            // settings.setAlwaysGenerateExceptionVariableForCatchBlocks(true);
            //

            final ButtonModel selectedLanguage = languagesGroup.getSelection();
            if (selectedLanguage != null) {
                final Language language = languageLookup.get(selectedLanguage.getActionCommand());

                if (language != null)
                    settings.setLanguage(language);
            }

            if (java.isSelected()) {
                settings.setLanguage(Languages.java());
            } else if (bytecode.isSelected()) {
                settings.setLanguage(Languages.bytecode());
            } else if (bytecodeAST.isSelected()) {
                settings.setLanguage(Languages.bytecodeAst());
            }
            settings.setIncludeLineNumbersInBytecode(bytecodeLineNumbers.isSelected());
        }
    }

    private class ThemeAction extends AbstractAction {
        private static final long serialVersionUID = -6618680171943723199L;
        private String xml;

        public ThemeAction(String name, String xml) {
            putValue(NAME, name);
            this.xml = xml;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            luytenPrefs.setThemeXml(xml);
            mainWindow.onThemesChanged();
        }
    }

    private class DecompilerAction extends AbstractAction {
        private static final long serialVersionUID = 5241351687025589732L;

        private Decompiler decompiler;

        public DecompilerAction(String name, Decompiler decompiler) {
            putValue(NAME, name);
            this.decompiler = decompiler;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            luytenPrefs.setDecompiler(decompiler);
        }
    }

    private static class LinkListener extends MouseAdapter {
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
            label.setText("<html><font color=\"#0078a3\"><u>" + link + "</u></font></html>");
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setText("<html><font color=\"#03bafc\"><u>" + link + "</u></font></html>");
        }

    }
}
