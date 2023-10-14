package us.deathmarine.luyten.config;

import us.deathmarine.luyten.decompiler.Decompiler;

/**
 * Do not instantiate this class, get the instance from ConfigSaver. All
 * not-static fields will be saved automatically named by the field's java
 * variable name. (Watch for collisions with existing IDs defined in
 * ConfigSaver.) Only String, boolean, int and enum fields are supported.
 * Write default values into the field declarations.
 */
public class LuytenPreferences {
    public static final String THEME_XML_PATH = "/org/fife/ui/rsyntaxtextarea/themes/";
    public static final String DEFAULT_THEME_XML = "onedark.xml";
    public static final Decompiler DEFAULT_DECOMPILER = Decompiler.PROCYON;

    private String themeXml = DEFAULT_THEME_XML;
    private Decompiler decompiler = DEFAULT_DECOMPILER;
    private String fileOpenCurrentDirectory = "";
    private String fileSaveCurrentDirectory = "";
    private int font_size = 10;

    private boolean isPackageExplorerStyle = true;
    private boolean isFilterOutInnerClassEntries = true;
    private boolean isSingleClickOpenEnabled = true;
    private boolean isExitByEscEnabled = false;

    public String getThemeXml() {
        return themeXml;
    }

    public void setThemeXml(String themeXml) {
        this.themeXml = themeXml;
    }

    public Decompiler getDecompiler() {
        return decompiler;
    }

    public void setDecompiler(Decompiler decompiler) {
        this.decompiler = decompiler;
    }

    public String getFileOpenCurrentDirectory() {
        return fileOpenCurrentDirectory;
    }

    public void setFileOpenCurrentDirectory(String fileOpenCurrentDirectory) {
        this.fileOpenCurrentDirectory = fileOpenCurrentDirectory;
    }

    public String getFileSaveCurrentDirectory() {
        return fileSaveCurrentDirectory;
    }

    public void setFileSaveCurrentDirectory(String fileSaveCurrentDirectory) {
        this.fileSaveCurrentDirectory = fileSaveCurrentDirectory;
    }

    public boolean isPackageExplorerStyle() {
        return isPackageExplorerStyle;
    }

    public void setPackageExplorerStyle(boolean isPackageExplorerStyle) {
        this.isPackageExplorerStyle = isPackageExplorerStyle;
    }

    public boolean isFilterOutInnerClassEntries() {
        return isFilterOutInnerClassEntries;
    }

    public void setFilterOutInnerClassEntries(boolean isFilterOutInnerClassEntries) {
        this.isFilterOutInnerClassEntries = isFilterOutInnerClassEntries;
    }

    public boolean isSingleClickOpenEnabled() {
        return isSingleClickOpenEnabled;
    }

    public void setSingleClickOpenEnabled(boolean isSingleClickOpenEnabled) {
        this.isSingleClickOpenEnabled = isSingleClickOpenEnabled;
    }

    public boolean isExitByEscEnabled() {
        return isExitByEscEnabled;
    }

    public void setExitByEscEnabled(boolean isExitByEscEnabled) {
        this.isExitByEscEnabled = isExitByEscEnabled;
    }

    public int getFont_size() {
        return font_size;
    }

    public void setFont_size(int font_size) {
        this.font_size = font_size;
    }
}
