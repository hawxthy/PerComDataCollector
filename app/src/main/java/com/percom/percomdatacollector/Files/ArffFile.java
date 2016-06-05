package com.percom.percomdatacollector.Files;

/**
 * This class is a simple POJO class. It's only purpose is to save the accelerometer data
 * as an ARFF file.
 *
 * @author Tristan Rust
 */
public class ArffFile {

    // Constants
    private static final String FILE_TYPE_ARFF = ".arff";

    // Filename
    private String strFileName = "";

    // Context: Walking - Jogging - Sport MAYBE NOT NEEDED!!!
    private String strContext = "";

    // FileContent
    private String strFileContent = "";

    /**
     * Default constructor.
     */
    public ArffFile() {}

    public ArffFile(String strFileName, String strFileContent) {
        setStrFileName(strFileName, false);
        setStrFileContent(strFileContent);
    }

    public String getStrFileName() {
        return strFileName;
    }

    /**
     * Set the FileName.
     * @param strFileName : String : FileName
     * @param attachFileType : boolean : true filetype *.arff attached
     */
    public void setStrFileName(String strFileName, boolean attachFileType) {
        if (attachFileType) this.strFileName = strFileName + FILE_TYPE_ARFF;
        else this.strFileName = strFileName;
    }

    public String getStrFileContent() {
        return strFileContent;
    }

    public void setStrFileContent(String strFileContent) {
        this.strFileContent = strFileContent;
    }

    /**
     * Returns the FileName without the Filetype.
     */
    public String getClearFileName() {
        String[] strClearFileName = this.strFileName.split(FILE_TYPE_ARFF);

        return strClearFileName[0];
    }

    public String getStrContext() {
        return strContext;
    }

    public void setStrContext(String strContext) {
        this.strContext = strContext;
    }
}
