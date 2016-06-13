package com.percom.percomdatacollector.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import com.percom.percomdatacollector.Files.ArffFile;
import com.percom.percomdatacollector.R;
import com.percom.percomdatacollector.activites.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * This service class saves and deletes ArffFiles from the local storage or exports them to
 * SD-Card.
 *
 * @author Tristan Rust
 */
public class FileService extends Service {

    // OutputStream
    private OutputStreamWriter out = null;
    // InputStream
    private InputStreamReader inputStreamReader = null;
    // Read in
    private BufferedReader in = null;

    // Notification Manager
    private NotificationManager notificationManager;
    // Unique identification number for the notification
    private int NOTIFICATION = R.string.local_service_started;

    /**
     * Class for clients to access. Returns the instance of the service.
     */
    public class FileServiceBinder extends Binder {
        public FileService getFileService() {return FileService.this;}
    }

    // Binder Object from the above defined class (IBinder is an Interface)
    IBinder binderFileService = new FileServiceBinder();

    /**
     * Default constructor.
     */
    public FileService() {}

    /**
     * Returns the FileServiceBinder. It represents the communication interface between the
     * service and the calling class.
     * @param intent : Intent
     * @return : FileServiceBinder
     */
    @Override
    public IBinder onBind(Intent intent) {return binderFileService;}

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Toast.makeText(this, R.string.local_service_label, Toast.LENGTH_SHORT).show();

        // Display a notification about starting.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        notificationManager.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a notification in the status bar while this service is running.
     */
    private void showNotification() {
        // Message which should be shown
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch the activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel
        Notification notification = new Notification.Builder(this)
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                //Test
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification
        notificationManager.notify(NOTIFICATION, notification);

    }

    /**
     * Save the File in the device storage.
     *
     * @param arffFile : ArffFile : File which should be saved.
     * @param fileOutputStream: FileOutputStream: Stream to write into the file.
     */
    public void saveAFile(ArffFile arffFile, FileOutputStream fileOutputStream) throws IOException{
        // Generate OutputStreamWriter
        out = new OutputStreamWriter(fileOutputStream);

        // Write into the file
        // out.write(arffFile.getStrFileName() + "\n" + arffFile.getStrFileContent());
        out.write(arffFile.getStrFileContent());

        // Close OutputStreamWriter
        if (out != null) out.close();

        // Close OutputStream
        if (fileOutputStream != null) fileOutputStream.close();
    }

    /**
     * Appends the record at the end of a file.
     * @param record : String : Record which should be appended to the file.
     * @param fileOutputStream : FileOutputStream: Stream to write into the file.
     * @throws IOException
     */
    public void appendToFile(String record, FileOutputStream fileOutputStream) throws IOException {
        // Generate OutputStreamWriter
        out = new OutputStreamWriter(fileOutputStream);

        // Write into the file
        // out.write(arffFile.getStrFileName() + "\n" + arffFile.getStrFileContent());
        out.write(record);

        // Close OutputStreamWriter
        if (out != null) out.close();

        // Close OutputStream
        if (fileOutputStream != null) fileOutputStream.close();
    }

    /**
     * Load the File from the device.
     *
     * @param fileInputStream : FileInputStream: Reads the file from the storage.
     */
    public ArffFile loadFileFromDevice(FileInputStream fileInputStream) {
        ArffFile arffFile = null;

        try {
            // Create InputStream
            this.inputStreamReader = new InputStreamReader(fileInputStream);

            // Create ReadObject
            this.in = new BufferedReader(this.inputStreamReader);

            // Represents a read line
            String strReadLine = "";
            boolean firstRun = true;

            /*
             * Using a StringBuilder object, because the size and number of rows is unkown.
             * Makes it more efficient. Adds the string to the already existing string.
             */
            StringBuilder stringBuilder = new StringBuilder();

            // end of file variable
            boolean eof = false;

            // Repeat as long as the end of the file isn't reached
            while (!eof) {
                // Read line
                strReadLine = in.readLine();

                // Check if not end of the file is reached
                if (strReadLine != null) {
                    stringBuilder.append(strReadLine + "\n");
                } else {
                    eof = true;
                }
            }

            // Generate ArffFile
            arffFile = new ArffFile();

            // Set attributes
            arffFile.setStrFileContent(stringBuilder.toString());

        } catch (Exception e) {
            arffFile = null;
        } finally {
            try {
                // Close all objects and streams
                if (in != null) {
                    in = null;
                }

                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }

                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return arffFile;
    }

    /**
     * This method save the ArffFile to the SD-Card.
     *
     * @param arffFile : ArffFile : ArffFile which should be saved.
     */
    public void exportAFileToSdCard(ArffFile arffFile) throws IOException {
        // State of the SD-Card
        String strStatus        = Environment.getExternalStorageState();
        String strAbsolutePath  = "";

        // Device variable state
        boolean canRead     = false;
        boolean canWrite    = false;

        // Check medium state
        if (Environment.MEDIA_MOUNTED.equals(strStatus)) {
            // Read/Write access
            canRead = true;
            canWrite = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(strStatus)) {
            // Read access
            canRead = true;
            canWrite = false;
        } else {
            // No access
            canRead = false;
            canWrite = false;
        }

        // Read/Write access
        if ((canRead) && (canWrite)) {
            // Find rood directory
            File fileExternal = Environment.getExternalStorageDirectory();

            // Define storage path
            strAbsolutePath = fileExternal.getAbsolutePath() + File.separator + "android"  + File.separator + "data" + File.separator + "arffExport" + File.separator + "files";

            // FileObject for the directory
            File appDir = new File(strAbsolutePath);

            // Generate directories
            appDir.mkdirs();

            // Create and save file
            createAndSaveFile(appDir, arffFile);

            Toast.makeText(this, "Datei wurde exportiert", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a file.
     *
     * @param appDir : File : FileObject with Directory
     * @param arffFile : ArffFile: ArffFile
     *
     * @throws IOException
     */
    private void createAndSaveFile(File appDir, ArffFile arffFile) throws IOException {
        // Generate File
        File newFile = new File(appDir, arffFile.getStrFileName());

        // Outputstream
        FileOutputStream fileOutputStream = new FileOutputStream(newFile);

        // Save
        saveAFile(arffFile, fileOutputStream);

        fileOutputStream.close();
    }

    /**
     * Calculates the size of a given file.
     *
     * @param fileName : fileName : The name of the file.
     * @return long : File size in KB.
     */
    public long calcFileSize(String fileName) {
        // Get the directory
        File filesDir = getFilesDir();

        // File for the size calculation
        File currentFile = new File(filesDir, fileName);

        // long fileSize = currentFile.length();
        long fileSize = currentFile.length()/1024; // KB

        return fileSize;
    }

    /**
     * Deletes the arff file.
     *
     * @return boolean : true if deleted.
     */
    public boolean deleteFile(String fileName) {
        // Get directory
        File filesDir = getFilesDir();

        // File to delete
        File currentFile = new File(filesDir, fileName);

        boolean deleted = currentFile.delete();

        if (deleted) Toast.makeText(this, "Datei wurde gelöscht.", Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "Datei konnte nicht gelöscht werden.", Toast.LENGTH_SHORT).show();

        return deleted;
    }

}
















