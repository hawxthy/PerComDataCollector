package com.percom.percomdatacollector.controller;

import com.percom.percomdatacollector.Files.ArffFile;

/**
 * This class is a singleton. Its purpose is to save during run time.
 *
 * @author Tristan Rust
 */
public class FileHandler {

    // The unique instance during runtime
    private static FileHandler instance = null;

    // Current ArffFile
    private ArffFile currentArffFile = new ArffFile();

    private FileHandler() {}

    /**
     * Returns the singleton instance.
     *
     * @return instance : FileHandler
     */
    public static synchronized FileHandler getInstance() {
        if (instance == null)
            instance = new FileHandler();

        return instance;
    }

    public ArffFile getCurrentArffFile() {
        return currentArffFile;
    }

    public void setCurrentArffFile(ArffFile currentArffFile) {
        this.currentArffFile = currentArffFile;
    }

}
