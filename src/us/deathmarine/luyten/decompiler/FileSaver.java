package us.deathmarine.luyten.decompiler;

import com.strobel.assembler.metadata.*;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import us.deathmarine.luyten.Luyten;
import us.deathmarine.luyten.config.ConfigSaver;
import us.deathmarine.luyten.config.LuytenPreferences;
import us.deathmarine.luyten.util.JarEntryFilter;
import us.deathmarine.luyten.util.LuytenTypeLoader;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * Performs Save and Save All
 */
public class FileSaver {

    private JProgressBar bar;
    private JLabel label;
    private boolean cancel;
    private boolean extracting;

    public FileSaver(JProgressBar bar, JLabel label) {
        this.bar = bar;
        this.label = label;
        final JPopupMenu menu = new JPopupMenu("取消");
        final JMenuItem item = new JMenuItem("取消");
        item.addActionListener(arg0 -> setCancel(true));
        menu.add(item);
        this.label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent ev) {
                if (SwingUtilities.isRightMouseButton(ev) && isExtracting())
                    menu.show(ev.getComponent(), ev.getX(), ev.getY());
            }
        });
    }

    public void saveText(final String text, final File file) {
        new Thread(() -> {
            DecompilerSettings settings = cloneSettings();
            boolean isUnicodeEnabled = settings.isUnicodeOutputEnabled();
            long time = System.currentTimeMillis();
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter writer = isUnicodeEnabled ? new OutputStreamWriter(fos, StandardCharsets.UTF_8)
                     : new OutputStreamWriter(fos);
                 BufferedWriter bw = new BufferedWriter(writer)) {
                label.setText("正在解压：" + file.getName());
                bar.setVisible(true);
                bw.write(text);
                bw.flush();
                label.setText("完成：" + getTime(time));
            } catch (Exception e1) {
                label.setText("无法保存文件：" + file.getName());
                Luyten.showExceptionDialog("无法保存文件！\n", e1);
            } finally {
                setExtracting(false);
                bar.setVisible(false);
            }
        }).start();
    }

    public void saveAllDecompiled(final File inFile, final File outFile) {
        new Thread(() -> {
            long time = System.currentTimeMillis();
            try {
                bar.setVisible(true);
                setExtracting(true);
                label.setText("正在解压：" + outFile.getName());
                System.out.println("[SaveAll]: " + inFile.getName() + " -> " + outFile.getName());
                String inFileName = inFile.getName().toLowerCase();

                if (inFileName.endsWith(".jar") || inFileName.endsWith(".zip")) {
                    doSaveJarDecompiled(inFile, outFile);
                } else if (inFileName.endsWith(".class")) {
                    doSaveClassDecompiled(inFile, outFile);
                } else {
                    doSaveUnknownFile(inFile, outFile);
                }
                if (cancel) {
                    label.setText("已取消");
                    outFile.delete();
                    setCancel(false);
                } else {
                    label.setText("完成：" + getTime(time));
                }
            } catch (Exception e1) {
                label.setText("无法保存文件：" + outFile.getName());
                Luyten.showExceptionDialog("无法保存文件！\n", e1);
            } finally {
                setExtracting(false);
                bar.setVisible(false);
            }
        }).start();
    }

    private void doSaveJarDecompiled(File inFile, File outFile) throws Exception {
        try (JarFile jfile = new JarFile(inFile);
             FileOutputStream dest = new FileOutputStream(outFile);
             BufferedOutputStream buffDest = new BufferedOutputStream(dest);
             ZipOutputStream out = new ZipOutputStream(buffDest)) {
            bar.setMinimum(0);
            bar.setMaximum(jfile.size());
            byte[] data = new byte[1024];
            DecompilerSettings settings = cloneSettings();
            LuytenTypeLoader typeLoader = new LuytenTypeLoader();
            MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
            ITypeLoader jarLoader = new JarTypeLoader(jfile);
            typeLoader.getTypeLoaders().add(jarLoader);

            DecompilationOptions decompilationOptions = new DecompilationOptions();
            decompilationOptions.setSettings(settings);
            decompilationOptions.setFullDecompilation(true);

            List<String> mass;
            JarEntryFilter jarEntryFilter = new JarEntryFilter(jfile);
            LuytenPreferences luytenPrefs = ConfigSaver.getLoadedInstance().getLuytenPreferences();
            if (luytenPrefs.isFilterOutInnerClassEntries()) {
                mass = jarEntryFilter.getEntriesWithoutInnerClasses();
            } else {
                mass = jarEntryFilter.getAllEntriesFromJar();
            }

            Enumeration<JarEntry> ent = jfile.entries();
            Set<String> history = new HashSet<>();
            int tick = 0;
            while (ent.hasMoreElements() && !cancel) {
                bar.setValue(++tick);
                JarEntry entry = ent.nextElement();
                if (!mass.contains(entry.getName()))
                    continue;
                label.setText("正在解压：" + entry.getName());
                bar.setVisible(true);
                if (entry.getName().endsWith(".class")) {
                    JarEntry etn = new JarEntry(entry.getName().replace(".class", ".java"));
                    label.setText("正在解压：" + etn.getName());
                    System.out.println("[SaveAll]: " + etn.getName() + " -> " + outFile.getName());

                    if (history.add(etn.getName())) {
                        out.putNextEntry(etn);
                        try {
                            boolean isUnicodeEnabled = decompilationOptions.getSettings().isUnicodeOutputEnabled();
                            String internalName = StringUtilities.removeRight(entry.getName(), ".class");
                            TypeReference type = metadataSystem.lookupType(internalName);
                            TypeDefinition resolvedType;
                            if ((type == null) || ((resolvedType = type.resolve()) == null)) {
                                throw new Exception("Unable to resolve type.");
                            }
                            Writer writer = isUnicodeEnabled ? new OutputStreamWriter(out, StandardCharsets.UTF_8)
                                : new OutputStreamWriter(out);
                            PlainTextOutput plainTextOutput = new PlainTextOutput(writer);
                            plainTextOutput.setUnicodeOutputEnabled(isUnicodeEnabled);
                            settings.getLanguage().decompileType(resolvedType, plainTextOutput, decompilationOptions);
                            writer.flush();
                        } catch (Exception e) {
                            label.setText("无法反编译文件：" + entry.getName());
                            Luyten.showExceptionDialog("无法反编译文件！\n跳过...", e);
                        } finally {
                            out.closeEntry();
                        }
                    }
                } else {
                    try {
                        JarEntry etn = new JarEntry(entry.getName());
                        if (entry.getName().endsWith(".java"))
                            etn = new JarEntry(entry.getName().replace(".java", ".src.java"));
                        if (history.add(etn.getName())) {
                            out.putNextEntry(etn);
                            try {
                                InputStream in = jfile.getInputStream(etn);
                                if (in != null) {
                                    try {
                                        int count;
                                        while ((count = in.read(data, 0, 1024)) != -1) {
                                            out.write(data, 0, count);
                                        }
                                    } finally {
                                        in.close();
                                    }
                                }
                            } finally {
                                out.closeEntry();
                            }
                        }
                    } catch (ZipException ze) {
                        if (!ze.getMessage().contains("duplicate")) {
                            throw ze;
                        }
                    }
                }
            }
        }
    }

    private void doSaveClassDecompiled(File inFile, File outFile) throws Exception {
        DecompilerSettings settings = cloneSettings();
        LuytenTypeLoader typeLoader = new LuytenTypeLoader();
        MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
        TypeReference type = metadataSystem.lookupType(inFile.getCanonicalPath());

        DecompilationOptions decompilationOptions = new DecompilationOptions();
        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);

        boolean isUnicodeEnabled = decompilationOptions.getSettings().isUnicodeOutputEnabled();
        TypeDefinition resolvedType;
        if (type == null || ((resolvedType = type.resolve()) == null)) {
            throw new Exception("Unable to resolve type.");
        }
        StringWriter stringwriter = new StringWriter();
        PlainTextOutput plainTextOutput = new PlainTextOutput(stringwriter);
        plainTextOutput.setUnicodeOutputEnabled(isUnicodeEnabled);
        settings.getLanguage().decompileType(resolvedType, plainTextOutput, decompilationOptions);
        String decompiledSource = stringwriter.toString();

        System.out.println("[SaveAll]: " + inFile.getName() + " -> " + outFile.getName());
        try (FileOutputStream fos = new FileOutputStream(outFile);
             OutputStreamWriter writer = isUnicodeEnabled ? new OutputStreamWriter(fos, StandardCharsets.UTF_8)
                 : new OutputStreamWriter(fos);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(decompiledSource);
            bw.flush();
        }
    }

    private void doSaveUnknownFile(File inFile, File outFile) throws Exception {
        try (FileInputStream in = new FileInputStream(inFile); FileOutputStream out = new FileOutputStream(outFile)) {
            System.out.println("[SaveAll]: " + inFile.getName() + " -> " + outFile.getName());

            byte[] data = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                out.write(data, 0, count);
            }
        }
    }

    private DecompilerSettings cloneSettings() {
        DecompilerSettings settings = ConfigSaver.getLoadedInstance().getDecompilerSettings();
        DecompilerSettings newSettings = new DecompilerSettings();
        if (newSettings.getJavaFormattingOptions() == null) {
            newSettings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());
        }
        // synchronized: against main menu changes
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (settings) {
            newSettings.setExcludeNestedTypes(settings.getExcludeNestedTypes());
            newSettings.setFlattenSwitchBlocks(settings.getFlattenSwitchBlocks());
            newSettings.setForceExplicitImports(settings.getForceExplicitImports());
            newSettings.setForceExplicitTypeArguments(settings.getForceExplicitTypeArguments());
            newSettings.setOutputFileHeaderText(settings.getOutputFileHeaderText());
            newSettings.setLanguage(settings.getLanguage());
            newSettings.setShowSyntheticMembers(settings.getShowSyntheticMembers());
            newSettings.setAlwaysGenerateExceptionVariableForCatchBlocks(
                settings.getAlwaysGenerateExceptionVariableForCatchBlocks());
            newSettings.setOutputDirectory(settings.getOutputDirectory());
            newSettings.setRetainRedundantCasts(settings.getRetainRedundantCasts());
            newSettings.setIncludeErrorDiagnostics(settings.getIncludeErrorDiagnostics());
            newSettings.setIncludeLineNumbersInBytecode(settings.getIncludeLineNumbersInBytecode());
            newSettings.setRetainPointlessSwitches(settings.getRetainPointlessSwitches());
            newSettings.setUnicodeOutputEnabled(settings.isUnicodeOutputEnabled());
            newSettings.setMergeVariables(settings.getMergeVariables());
            newSettings.setShowDebugLineNumbers(settings.getShowDebugLineNumbers());
        }
        return newSettings;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isExtracting() {
        return extracting;
    }

    public void setExtracting(boolean extracting) {
        this.extracting = extracting;
    }

    public static String getTime(long time) {
        long lap = System.currentTimeMillis() - time;
        lap = lap / 1000;
        StringBuilder sb = new StringBuilder();
        long hour = ((lap / 60) / 60);
        long min = ((lap - (hour * 60 * 60)) / 60);
        long sec = ((lap - (hour * 60 * 60) - (min * 60)));
        if (hour > 0)
            sb.append(hour).append("时");
        sb.append(min).append("分").append(sec).append("秒");
        return sb.toString();
    }
}
