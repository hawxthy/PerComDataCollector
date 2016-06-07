package com.percom.percomdatacollector.activites;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.percom.percomdatacollector.Files.ArffFile;
import com.percom.percomdatacollector.R;
import com.percom.percomdatacollector.controller.FileHandler;
import com.percom.percomdatacollector.services.FileService;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This activity is responsible for collecting sensor data with the acceleraometers. The sensor
 * data is saved in an *.arff file and can be exported to the SD-Card or deleted from the app
 * storage.
 *
 * @author Tristan Rust
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // The filename
    private static final String FILE_NAME = "acc-data,walking,jogging,sport.arff";

    // TextViews
    private static TextView txtvAccData = null;
    private static TextView txtvButtonTitle = null;

    // Button
    private static Button cmdExportToSdCard = null;
    private static Button cmdDeleteFile = null;

    // RadioButtons
    private static RadioButton rdbWalking = null;
    private static RadioButton rdbJogging = null;
    private static RadioButton rdbSport   = null;

    // Toggle Button
    private static ToggleButton tbRecord = null;

    // FileService is needed in order to create, read, save and export files
    private static FileService fileService = null;

    // SensorManager in order to get the acc.
    private static SensorManager sensorManager = null;

    // Accelerometer
    private static Sensor accelerometer = null;

    /**
     * ServiceConnection to the FileService which creates a lose connection to the FileService.
     */
    private ServiceConnection connection = new ServiceConnection() {
        /**
         * Happens when the services is connecting.
         * Generates the Binder-Object for the ServiceConnection.
         * @param name
         * @param service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Is called when the connection with the service has been established, gives the
            // service object for interaction with the service.
            FileService.FileServiceBinder binder = (FileService.FileServiceBinder) service;

            fileService = binder.getFileService(); // Load the file from device

            try {
                // Load the content from the .arff file
                FileHandler.getInstance().setCurrentArffFile(fileService.loadFileFromDevice(fileService.openFileInput(FILE_NAME)));
                FileHandler.getInstance().getCurrentArffFile().setStrFileName(FILE_NAME, false);
            } catch (FileNotFoundException e) {
                // Create a new file if no file was found
                addArffFile();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fileService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);

        // Widgets
        initializeWidgets();

        // SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Accelerometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // listener
        tbRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    // Generate Binding and FileService
                    // bindConnectionAndStartFileService();
                    registerAccelerometer(sensorManager);
                } else {
                    unregisterAccelerometer(sensorManager);
                }
            }
        });

        cmdExportToSdCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToSdCard();
            }
        });

        cmdDeleteFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Deletes the old file
                deleteFile(FILE_NAME);
                // Creates a new one with the attributes
                addArffFile();
            }
        });
    }

    private void initializeWidgets() {
        // TextView
        txtvAccData = (TextView) findViewById(R.id.txtvAccData);
        txtvButtonTitle = (TextView) findViewById(R.id.txtvButtonTitle);

        // Button
        cmdExportToSdCard = (Button) findViewById(R.id.cmdExportToSdCard);
        cmdDeleteFile = (Button) findViewById(R.id.cmdDeleteFile);

        // RadioButtons
        rdbWalking = (RadioButton) findViewById(R.id.rdbWalking);
        rdbJogging = (RadioButton) findViewById(R.id.rdbJogging);
        rdbSport   = (RadioButton) findViewById(R.id.rdbSport);

        // ToggleButton
        tbRecord = (ToggleButton) findViewById(R.id.tbRecord);
    }

    /**
     * This methode exports the current arff file to the SD-Card.
     */
    private void exportToSdCard() {
        if (this.getFileService() != null) {
            // Generate ArffFile



            ArffFile arffFile = FileHandler.getInstance().getCurrentArffFile();

            // Use FileService for export
            try {
                getFileService().exportAFileToSdCard(arffFile);
            } catch (IOException e) {
                Toast.makeText(this, "Export to SD-Card FAILED!", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Export to SD-Card FAILED (fileService is null)!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Register the listener for events and initialize the accelerometor
     */
    public void registerAccelerometer(SensorManager sensorManager) {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // check if accelerometer is available
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // Error, do something here
        }
    }

    /**
     * Disables the sensor if not needed.
     */
    public void unregisterAccelerometer(SensorManager sensorManager) {
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Generate Binding and FileService
        bindConnectionAndStartFileService();
        // registerAccelerometer(sensorManager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Generate Binding and FileSerive
        //bindConnectionAndStartFileService();
        // registerAccelerometer(sensorManager);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // unbindConnectioonAndKillfileService();
        // finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Only destroy the service when the activity was destroyed
        // unregisterAccelerometer(sensorManager);
        // unbindConnectioonAndKillfileService();
        // unbindConnectioonAndKillfileService();
        // finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Only destroy the service when the activity was destroyed
        unregisterAccelerometer(sensorManager);
        unbindConnectioonAndKillfileService();
    }

    /**
     * This method establishes the binding and generates the FileService object.
     */
    private void bindConnectionAndStartFileService() {
        Intent intent = new Intent(this, FileService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void unbindConnectioonAndKillfileService() {
        if (fileService != null) {
            // stop service
            fileService.stopSelf();

            // 2. remove binding
            unbindService(connection);

            // 3. release object memory
            fileService = null;
        }
    }

    /**
     * Adds a predefined arff file.
     */
    private void addArffFile() {
        String strInputFileName = FILE_NAME;
        String strArffDef = "@relation detectSportType\n"
                + "\n"
                + "@attribute mean numeric\n"
                + "@attribute stdDeviation numeric\n"
                + "@attribute min numeric\n"
                + "@attribute max numeric\n"
                + "@attribute movementType {walking,jogging,sport}\n"
                + "@attribute sensor {accelerometer}\n"
                + "\n"
                + "@data";

        try {
            // Generate the file
            ArffFile arffFile = new ArffFile();

            // Set attributes
            arffFile.setStrFileName(strInputFileName, false);
            arffFile.setStrFileContent(strArffDef);

            // FileService
            this.getFileService().saveAFile(arffFile, this.openFileOutput(arffFile.getStrFileName(), this.MODE_PRIVATE));

            FileHandler.getInstance().setCurrentArffFile(arffFile);

            Toast.makeText(this, "File created", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "File couldn't be created", Toast.LENGTH_SHORT).show();
        }
    }


    private void addNewRecord(float mean, float stdDeviation, float min, float max, String movementType, String sensor) {
        try {
            if (this.getFileService() != null) {
                ArffFile arffFile = FileHandler.getInstance().getCurrentArffFile();

                String newRecord = "\n"
                        + Float.toString(mean) + ","
                        + Float.toString(stdDeviation) + ","
                        + Float.toString(min) + ","
                        + Float.toString(max) + ","
                        + movementType + ","
                        + sensor;

                String newContent = "";
                newContent = arffFile.getStrFileContent() + newRecord;
                arffFile.setStrFileContent(newContent);

                // this.getFileService().deleteFile(FILE_NAME);
                //  this.getFileService().saveAFile(arffFile, this.openFileOutput(arffFile.getStrFileName(), this.MODE_APPEND));
                this.getFileService().appendToFile(newRecord, this.openFileOutput(arffFile.getStrFileName(), this.MODE_APPEND));

            } else {
                Toast.makeText(MainActivity.this, "FileService is null!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't add the new record.", Toast.LENGTH_SHORT).show();
        }
    }

    public FileService getFileService() {return fileService;}

    /**
     * Accelerometer methode.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    /**
     * Accelerometer methode in order to get current sensor values.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Read the x, y, z values from the sensor event
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Output
        txtvAccData.setText(Float.toString(x) + ", " + Float.toString(y) + ", " + Float.toString(z));

        // FileSize
        txtvButtonTitle.setText("Dateigröße: " + Long.toString(getFileService().calcFileSize(FILE_NAME)) + "KB");

        float mean = calcMean(x, y, z);
        float stdDeviation = (float) calcStdDeviation(x, y, z);
        float min = findMin(x, y, z);
        float max = findMax(x, y, z);

        addNewRecord(mean, stdDeviation, min, max, getMovementType(), "accelerometer");

    }

    private String getMovementType() {
        String strContext = "";

        if (rdbWalking.isChecked()) strContext = "walking";
        else if (rdbJogging.isChecked()) strContext = "jogging";
        else if (rdbSport.isChecked()) strContext = "sport";

        return strContext;
    }

    private float findMax(float x, float y, float z) {
        float max = 0;

        if (x>y) {
            max = x;
            if (x<z)
                max = z;
        } else {
            max = y;
            if (y<z)
                max = z;
        }

        return max;
    }

    private float findMin(float x, float y, float z) {
        float min = 0;

        if (x<y) {
            min = x;
            if (x>z)
                min = z;
        } else {
            min = y;
            if (y>z)
                min = z;
        }

        return min;
    }
    private double calcStdDeviation(float x, float y, float z) {
        double stdDeviation = 0;
        double mean = 0;
        double variance = 0;

        mean = calcMean(x, y, z);
        variance = (Math.pow((x-mean), 2) + Math.pow((y-mean), 2) + Math.pow((z-mean), 2)) / 3;
        stdDeviation = Math.sqrt(variance);

        return stdDeviation;
    }

    private float calcMean(float x, float y, float z) {
        float mean = 0;

        mean = ((x + y + z) / 3);

        return mean;
    }

}
