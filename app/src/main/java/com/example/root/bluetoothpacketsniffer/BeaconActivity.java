package com.example.root.bluetoothpacketsniffer;

import android.app.Activity;
import android.app.AlertDialog;
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


public class BeaconActivity extends Activity implements BeaconConsumer {

    public static final String TAG = "BeaconsEverywhere";
    private BeaconManager beaconManager;
    public int clickCount = 0;
    public long baseTime=0,timeStamp=0;
    public List<String[]> data = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        verifyBluetooth();

        Button startButton = (Button) findViewById(R.id.startButton);
        Button stopButton = (Button) findViewById(R.id.stopButton);
        Button markButton = (Button) findViewById(R.id.markButton);
        Button exportButton = (Button) findViewById(R.id.exportButton);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setBackgroundScanPeriod(1000);
        beaconManager.setBackgroundBetweenScanPeriod(1);

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

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    EditText filename = (EditText) findViewById(R.id.filename);
                    exportData(filename.getText().toString());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    public void binding(){
        if(!beaconManager.isBound(this)) {
            baseTime = System.currentTimeMillis();
            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.startToast),
                    Toast.LENGTH_SHORT).show();
            data.add(new String[]{"Timestamp", "MAC Address", "RSSI (in dBm)", "Mark"});
            beaconManager.bind(this);
        }
        else {
            Toast.makeText(getApplicationContext(), "Press Stop before starting again",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void unbinding(){
        if(beaconManager.isBound(this)) {
            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.stopToast),
                    Toast.LENGTH_SHORT).show();
            clickCount=0;
            beaconManager.unbind(this);
        }
        else{
            Toast.makeText(getApplicationContext(), "Press Start to Sniff packets",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void marking(){
        if(beaconManager.isBound(this)) {
            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.markToast),
                    Toast.LENGTH_SHORT).show();
            clickCount += 1;
        }
        else{
            Toast.makeText(getApplicationContext(), "Press Start before setting Markers",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void exportData(String filename) throws IOException{
        TextView editText = (TextView) BeaconActivity.this.findViewById(R.id.rangingText);
        if(editText.equals("")){
            Toast.makeText(getApplicationContext(), "Error: No Data has been collected!",
                    Toast.LENGTH_LONG).show();
        }
        else if(beaconManager.isBound(this)) {
            Toast.makeText(getApplicationContext(), "Error: Data collection is taking place!",
                    Toast.LENGTH_LONG).show();
        }
        else if(filename.equals("")){
            Toast.makeText(getApplicationContext(), "Please give a suitable file name!",
                    Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.exportToast),
                    Toast.LENGTH_LONG).show();
            String csv = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/BData/";
            File file = new File(csv + filename + ".csv");
            if (!file.exists()) {
                FileWriter fwriter = new FileWriter(file, false);
                CSVWriter writer = new CSVWriter(fwriter);
                writer.writeAll(data);
                writer.close();
                data = new ArrayList<>();
                editText.setText("");
                EditText filen = (EditText) findViewById(R.id.filename);
                filen.setText("");
            }
            else{
                Toast.makeText(getApplicationContext(), "Error: File already exists!",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setBackgroundScanPeriod(1000);
        beaconManager.setBackgroundBetweenScanPeriod(1);
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    timeStamp = System.currentTimeMillis() - baseTime;
                    int s = beacons.toArray().length;
                    Beacon[] beaconArr = beacons.toArray(new Beacon[s]);
                    for(int i=0;i<s;i++) {
                        Log.i(TAG, "Time:"+timeStamp+" Address: " + beaconArr[i].getBluetoothAddress()
                                + " RSSI: " + beaconArr[i].getRssi() + " Marker: " + clickCount);
                        logToDisplay(timeStamp
                                + "         -           " +  beaconArr[i].getBluetoothAddress()
                                + "         -           " + beaconArr[i].getRssi()
                                + "         -           " + clickCount);
                        data.add(new String[]{Long.toString(timeStamp),beaconArr[i].getBluetoothAddress(),
                                Integer.toString(beaconArr[i].getRssi()), Integer.toString(clickCount)});
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

    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
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
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
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

    private void logToDisplay(final String line) {
        runOnUiThread(new Runnable() {
            public void run() {
                TextView editText = (TextView) BeaconActivity.this.findViewById(R.id.rangingText);
                editText.setMovementMethod(new ScrollingMovementMethod());
                editText.append(line + "\n");
            }
        });
    }

}
