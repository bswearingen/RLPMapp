package com.bswearingen.www.rlpmapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Ben on 4/1/2017.
 */

public class RLPBluetoothManager extends Activity{
    public static RLPBluetoothManager mInstance = null;

    final int REQUEST_ENABLE_BT = 1;
    // Well known SPP UUID
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket mBluetoothSocket = null;
    private OutputStream outStream = null;

    // Variables for displaying the list of android devices
    AlertDialog alertDialogObject;
    ArrayAdapter<String> devicesListAdapter;
    ArrayList<BluetoothDevice> devices;
    ArrayList<String> allDevices;
    BluetoothDevice deviceToConnect;
    Activity mActivity;

    private RLPBluetoothManager(Activity activity){
        mActivity = activity;
        connectToBluetooth();
    }

    public static RLPBluetoothManager getInstance(Activity activity){
        if(mInstance == null)
            mInstance = new RLPBluetoothManager(activity);
        return mInstance;
    }

    public static RLPBluetoothManager getInstance(){
        return mInstance;
    }

    public boolean sendMsg(String msg)
    {
        if(outStream == null)
            return false;

        try {
            outStream.write(msg.getBytes());
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }

    public void onPause(){
        try {
            outStream.close();
            mBluetoothSocket.close();
            mInstance = null;
        }
        catch(Exception e)
        {
            Log.e("DEMOACTIVITY", e.getMessage());
        }
    }

    private void connectToBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.cancelDiscovery();

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
            getPairedDevices();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                getPairedDevices();
            }
        }

    }

    private void getPairedDevices(){
        if(devices == null)
            devices = new ArrayList<BluetoothDevice>();
        else
            devices.clear();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                devices.add(device);
            }
        }
        showPairedList();
    }

    private void showPairedList(){
        List<String> tempDevices = new ArrayList<String>();

        if(devices.isEmpty())
            return;

        for (BluetoothDevice b : devices) {
            String paired = "Paired";
            if (b.getBondState() != 12) {
                paired = "Not Paired";
            }
            tempDevices.add(b.getName() + " - [ " + paired + " ] ");
        }

        if (allDevices == null)
            allDevices = new ArrayList<String>();
        else
            allDevices.clear();

        allDevices.addAll(tempDevices);

        if (devicesListAdapter == null) {

            ListView devicesList = new ListView(mActivity);
            devicesList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            devicesListAdapter = new ArrayAdapter<>((mActivity),
                    android.R.layout.simple_list_item_single_choice, android.R.id.text1, allDevices);
            devicesList.setAdapter(devicesListAdapter);
            //Create sequence of items
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity);
            dialogBuilder.setTitle("Paired/Unpaired BT Devices");
            dialogBuilder.setView(devicesList);
            //Create alert dialog object via builder
            final AlertDialog alertDialogObject = dialogBuilder.create();
            devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    deviceToConnect = devices.get(position);
                    devicesListAdapter = null;
                    alertDialogObject.dismiss();
                    connectToDevice();
                }
            });
            //Show the dialog
            alertDialogObject.show();
            alertDialogObject.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    devicesListAdapter = null;
                }
            });
        } else {
            devicesListAdapter.notifyDataSetChanged();
        }
    }

    private void connectToDevice() {
        try {
            mBluetoothSocket = deviceToConnect.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {

        }
        mBluetoothAdapter.cancelDiscovery();

        try {
            mBluetoothSocket.connect();
        } catch (IOException e) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e2) {
            }
        }

        // Create a data stream so we can talk to server.

        try {
            outStream = mBluetoothSocket.getOutputStream();
        } catch (IOException e) {
        }
    }
}
