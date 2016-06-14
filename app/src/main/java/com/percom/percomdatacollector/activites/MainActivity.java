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
import android.os.PowerManager;
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

import java.io.File;
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
    private static TextView txtvGyrData = null;
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

    // Gyroscope
    private static Sensor gyroscope = null;

    // Power Manager
    PowerManager powerManager = null;
    PowerManager.WakeLock wakeLock = null;

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

                txtvButtonTitle.setText(fileSizeToMBString(getFileService().calcFileSize(FILE_NAME)));
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

    public FileService getFileService() {return fileService;}

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

        // Gyroscope
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Power Manager
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PerComWakelockTag");


        // listener
        tbRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    // start sensor recording
                    registerAccelerometer(sensorManager);
                    registerGyroscope(sensorManager);

                    // WakeLock let's the service running with screen off
                    wakeLock.acquire();
                } else {
                    // stop sensor recording
                    unregisterAccelerometer(sensorManager);

                    // Releases the claim to the CPU and battery waste
                    wakeLock.release();
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
                // Show the new file size in the UI
                txtvButtonTitle.setText("Dateigröße: " + getFileService().calcFileSize(FILE_NAME));
                // Creates a new one with the attributes
                addArffFile();
            }
        });
    }

    private void initializeWidgets() {
        // TextView
        txtvAccData = (TextView) findViewById(R.id.txtvAccData);
        txtvGyrData = (TextView) findViewById(R.id.txtvGyrData);
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
            // Use FileService for export
            try {
                // Load ArffFile
                FileHandler.getInstance().setCurrentArffFile(fileService.loadFileFromDevice(fileService.openFileInput(FILE_NAME)));
                FileHandler.getInstance().getCurrentArffFile().setStrFileName(FILE_NAME, false);

                getFileService().exportAFileToSdCard(FileHandler.getInstance().getCurrentArffFile());
            } catch (IOException e) {
                Toast.makeText(this, "Export to SD-Card FAILED!", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Export to SD-Card FAILED (fileService is null)!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Register the listener for events and initialize the accelerometor.
     */
    public void registerAccelerometer(SensorManager sensorManager) {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // check if accelerometer is available
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
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

    /**
     * Register the listener for events and initialize the gyrosceope.
     */
    public void registerGyroscope(SensorManager sensorManager) {
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //check if gyroscope is available
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            // Error do something here
        }
    }

    /**
     * Disables the sensor if not needed.
     */
    public void unregisterGyrosceope(SensorManager sensorManager) {
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        bindConnectionAndStartFileService();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Only destroy the service when the activity was destroyed
        unregisterAccelerometer(sensorManager);
        unbindConnectioonAndKillfileService();

        // Releases the claim to the CPU and battery waste
        if (wakeLock != null)
            wakeLock.release();
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
                + "@attribute sensor {accelerometer,gyroscope}\n"
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

                // String newContent = "";
                // newContent = arffFile.getStrFileContent() + newRecord;
                // arffFile.setStrFileContent(newContent);

                // this.getFileService().deleteFile(FILE_NAME);
                // this.getFileService().saveAFile(arffFile, this.openFileOutput(arffFile.getStrFileName(), this.MODE_APPEND));
                this.getFileService().appendToFile(newRecord, this.openFileOutput(arffFile.getStrFileName(), this.MODE_APPEND));

            } else {
                Toast.makeText(MainActivity.this, "FileService is null!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't add the new record.", Toast.LENGTH_SHORT).show();
        }
    }

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
        Sensor sensorType = event.sensor;

        // ACCELEROMETER
        if (sensorType.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Accelerometer: Read the x, y, z values from the sensor event
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Accelerometer: Output
            txtvAccData.setText(Float.toString(x) + ", " + Float.toString(y) + ", " + Float.toString(z));

            // Accelerometer: Calc attributes
            float mean = calcMean(x, y, z);
            float stdDeviation = (float) calcStdDeviation(x, y, z);
            float min = findMin(x, y, z);
            float max = findMax(x, y, z);

            // Accelerometer: Write record into the .arff file
            addNewRecord(mean, stdDeviation, min, max, getMovementType(), "accelerometer");

        }

        // GYROSCOPE
        if (sensorType.getType() == Sensor.TYPE_GYROSCOPE) {

            // maximal allowable margin of error
            float EPSILON = 0.000000001f;
            // Read the X, Y, Z value from sensor
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            // Gyroscope: Calculate the angular speed of the data
            float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);;

            // Normalize values of X, Y, Z axis
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            // Gyroscope: Delete the last few numbers, because the size is to big for the UI
            String strAxisX = Float.toString(axisX);
            String strAxisY = Float.toString(axisY);
            String strAxisZ = Float.toString(axisZ);

            // Deletes the last a specified number
            int numberOfLetters = 3;

            strAxisX = strAxisX.substring(0, strAxisX.length()-numberOfLetters);
            strAxisY = strAxisY.substring(0, strAxisY.length()-numberOfLetters);
            strAxisZ = strAxisZ.substring(0, strAxisZ.length()-numberOfLetters);

            // Gyroscope: Output
            txtvGyrData.setText(strAxisX + ", " + strAxisY + ", " + strAxisZ);

            // Gyroscope: Calc attributes
            float mean = calcMean(axisX, axisY, axisZ);
            float stdDeviation = (float) calcStdDeviation(axisX, axisY, axisZ);
            float min = findMin(axisX, axisY, axisZ);
            float max = findMax(axisX, axisY, axisZ);

            // Gyroscope: Write record into the .arff file
            addNewRecord(mean, stdDeviation, min, max, getMovementType(), "gyroscope");

        }

        // FileSize
        txtvButtonTitle.setText(fileSizeToMBString(getFileService().calcFileSize(FILE_NAME)));

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

    /**
     * Return the file size as a string.
     * @param fileSize: long: FileSize in KB.
     * @return The method returns the file size in MB when the file size of 10MB is reached in
     * KB.
     */
    private String fileSizeToMBString(long fileSize) {
        String strFileSize = "";

        if (fileSize > 10240) {
            fileSize = fileSize / 1024;
            strFileSize = "Dateigröße: " + Long.toString(fileSize) + "MB";
        } else {
            strFileSize = "Dateigröße: " + Long.toString(fileSize) + "KB";
        }

        return strFileSize;
    }

}
