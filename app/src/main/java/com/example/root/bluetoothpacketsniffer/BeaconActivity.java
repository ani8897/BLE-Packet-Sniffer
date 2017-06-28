package com.example.root.bluetoothpacketsniffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.os.RemoteException;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import org.altbeacon.beacon.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*******************************************

 Refer to this website to get started with the Android beacon library:- https://altbeacon.github.io/android-beacon-library/

********************************************/

public class BeaconActivity extends Activity implements BeaconConsumer {

    public static final String TAG = "BeaconsEverywhere";
    private BeaconManager beaconManager;
    public int clickCount = 0;
    public long baseTime=0,timeStamp=0;
    public List<String[]> data = new ArrayList<>();
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        //Checking the compatibility of the device.
        verifyBluetooth();

        //Instantiating the buttons
        Button startButton = (Button) findViewById(R.id.startButton);
        Button stopButton = (Button) findViewById(R.id.stopButton);
        Button markButton = (Button) findViewById(R.id.markButton);
        Button exportButton = (Button) findViewById(R.id.exportButton);
        Button clearButton = (Button) findViewById(R.id.clearButton);

        //Creating a beacon manager which manages all the beacons
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setBackgroundScanPeriod(1000);
        beaconManager.setBackgroundBetweenScanPeriod(1);

        //Assigning functions to be executed on pressing the buttons
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbinding();
            }
        });

        markButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                marking();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearing();
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    EditText filename = (EditText) findViewById(R.id.filename); //Fetching the file name entered by the user
                    exportData(filename.getText().toString());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    //Start the scan for beacons (On pressing the 'Start' Button)
    public void binding(){
        if(!beaconManager.isBound(this)) {

            /*
            Notification notification = new Notification(R.mipmap.ic_launcher,
                    context.getString(R.string.app_name),
                    System.currentTimeMillis());
            notification.flags |= Notification.FLAG_NO_CLEAR
                    | Notification.FLAG_ONGOING_EVENT;
            NotificationManager notifier = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifier.notify(1, notification);*/

            baseTime = System.currentTimeMillis(); //Variable which is used for maintaining the timestamps of the packets
            raiseAtoast(R.string.startToast);
            data.add(new String[]{"Timestamp", "MAC Address", "RSSI (in dBm)", "Mark"}); //Setting the header for the data file which will stored later on as a csv
            beaconManager.bind(this); //Binding the beacon manager in order to activate the scanning procedure
        }
        else { //When the start button is pressed while scanning.
           raiseAtoast(R.string.startBeforeStop);
        }
    }

    //Stop the scan for beacons (On pressing the 'Stop' Button)
    public void unbinding(){
        if(beaconManager.isBound(this)) {
           raiseAtoast(R.string.stopToast);
            clickCount=0;   //Resetting the 'Mark' attribute
            beaconManager.unbind(this); //Unbinding the beacon manager to stop the scanning procedure
        }
        else{ //When the stop button is pressed unnecessarily
           raiseAtoast(R.string.stopBeforeStart);
        }
    }

    //Mark the Ground Truth (On pressing the 'Mark' Button)
    public void marking(){
        if(beaconManager.isBound(this)) {
            raiseAtoast(R.string.markToast);
            clickCount += 1;    //Increments the 'Mark' attribute
        }
        else{   //When the mark button is pressed while the data is not collected
            raiseAtoast(R.string.markingError);
        }
    }

    //Clearing the screen and the data collected so far (On pressing the 'Clear' Button)
    public void clearing(){
        clearView();    //Clearing the contents on the screen
        data = new ArrayList<>();   //Resetting the data variable
        raiseAtoast(R.string.clearToast);
        if(beaconManager.isBound(this)) {   //When the clear button is pressed during data collection
            data.add(new String[]{"Timestamp", "MAC Address", "RSSI (in dBm)", "Mark"});
        }
    }

    //Exporting the data collected in a csv file (On pressing the 'Export Data' Button)
    public void exportData(String filename) throws IOException{
        TextView editText = (TextView) BeaconActivity.this.findViewById(R.id.timeStamp);
        if(editText.getText().toString().equals("")){   //When no data is collected
            raiseAtoast(R.string.noData);
        }
        else if(beaconManager.isBound(this)) {      //When the button is pressed during data collection
            raiseAtoast(R.string.DataCollectionInterrupt);
        }
        else if(filename.equals("")){   //When no file name is given
            raiseAtoast(R.string.noFilename);
        }
        else{
            raiseAtoast(R.string.exportToast);
            //Hardcoded folder for storing the data
            String dirString = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/BData";
            File directory = new File(dirString);
            if(!directory.exists()) {
                Log.i(TAG,"No directory");
                ContextWrapper cw = new ContextWrapper(context);
                directory = cw.getDir(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/BData", Context.MODE_PRIVATE);
            }

            //Writing the data to the file
            File file = new File(dirString + "/" + filename + ".csv");
            if (!file.exists()) {
                FileWriter fwriter = new FileWriter(file, false);
                CSVWriter writer = new CSVWriter(fwriter);
                writer.writeAll(data);
                writer.close();
                data = new ArrayList<>(); //Resetting data
                clearView();    //Clearing the screen content
                EditText filen = (EditText) findViewById(R.id.filename);
                filen.setText("");  //Clearing the filename on the screen
                raiseAtoast(R.string.success);
            }
            else{   //When the filename entered already exists
                raiseAtoast(R.string.existingFile);
            }
        }
    }

    //Default function, nothing new here except the part where the scanning is stopped
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
        //((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
    }

    // This is the function which is called whenever the bind() function is called.
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setBackgroundScanPeriod(1000);
        beaconManager.setBackgroundBetweenScanPeriod(1);
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {   //
                    timeStamp = System.currentTimeMillis() - baseTime;
                    int s = beacons.toArray().length;
                    Beacon[] beaconArr = beacons.toArray(new Beacon[s]);    //Obtaining the list of beacons heard
                    //List<BeaconParser> beaconParserList = beaconManager.getBeaconParsers();
                    //Log.d(TAG,Long.toString(beaconParserList.get(0).getDataFieldCount()));
                    //Log.d(TAG,Long.toString(beaconParserList.get(1).bytesToHex(
                     //       beaconParserList.get(1).getBeaconAdvertisementData()
                    //)));
                    for(int i=0;i<s;i++) {
                        //Log.i(TAG, "Time:"+timeStamp+" Address: " + beaconArr[i].getBluetoothAddress()
                          //      + " RSSI: " + beaconArr[i].getRssi() + " Marker: " + clickCount);
                        logToDisplay(Long.toString(timeStamp),              //Adding the information to the screen
                                beaconArr[i].getBluetoothAddress(),
                                Integer.toString(beaconArr[i].getRssi()),
                                Integer.toString(clickCount));
                        data.add(new String[]{Long.toString(timeStamp),     //Appending the information to the data variable
                                beaconArr[i].getBluetoothAddress(),
                                Integer.toString(beaconArr[i].getRssi()),
                                Integer.toString(clickCount)});
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //Function for checking the compatibility (Reference:- Android beacon library reference master)
    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(context.getString(R.string.noBluetooth));
                builder.setMessage(context.getString(R.string.noBluetoothMessage));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(context.getString(R.string.noBLE));
            builder.setMessage(context.getString(R.string.noBLEMessage));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();
        }
    }

    //Displaying the contents on the screen dynamically
    private void logToDisplay(final String Timestamp,final String Address, final String RSSI, final String Mark) {
        runOnUiThread(new Runnable() {
            public void run() {
                final TextView editTimestamp = (TextView) BeaconActivity.this.findViewById(R.id.timeStamp);
                TextView editAddress = (TextView) BeaconActivity.this.findViewById(R.id.address);
                TextView editRSSI = (TextView) BeaconActivity.this.findViewById(R.id.rssi);
                TextView editMark = (TextView) BeaconActivity.this.findViewById(R.id.mark);
                editTimestamp.append(Timestamp + "\n");
                editAddress.append(Address + "\n");
                editRSSI.append(RSSI + "\n");
                editMark.append(Mark + "\n");
            }
        });
    }

    //Clearing the contents on the screen
    private void clearView(){
        TextView editTimestamp = (TextView) BeaconActivity.this.findViewById(R.id.timeStamp);
        TextView editAddress = (TextView) BeaconActivity.this.findViewById(R.id.address);
        TextView editRSSI = (TextView) BeaconActivity.this.findViewById(R.id.rssi);
        TextView editMark = (TextView) BeaconActivity.this.findViewById(R.id.mark);
        editTimestamp.setText("");
        editAddress.setText("");
        editRSSI.setText("");
        editMark.setText("");
    }

    //Raising a Toast message
    private void raiseAtoast(int resID){
        Toast.makeText(context, context.getString(resID),
                Toast.LENGTH_SHORT).show();
    }

}
