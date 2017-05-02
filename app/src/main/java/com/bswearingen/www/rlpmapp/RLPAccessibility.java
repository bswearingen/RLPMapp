package com.bswearingen.www.rlpmapp;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ArrayAdapter;
import android.widget.RemoteViews;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Ben on 4/30/2017.
 */

public class RLPAccessibility extends AccessibilityService {
    static final String TAG = "RLPAccessibility";
    RLPBluetoothManager mRLPBluetoothManager;

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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.v(TAG, String.format(
                "onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
                "Notification", event.getClassName(), event.getPackageName(),
                event.getEventTime(), event.getText()));
        Notification parcelable = (Notification) event.getParcelableData();

        if(parcelable == null) return;
        List<String> text = getText(parcelable);

        if(text != null) {
            Log.v(TAG, text.toString());
            String nextAction = text.get(0).toLowerCase();
            String[] words = nextAction.split(" ");

            if(nextAction.contains("turn") && TextUtils.isDigitsOnly(words[0]) && Integer.parseInt(words[0]) <= 60)
            {
                if(nextAction.contains("left")) sendMsg("L");
                else if(nextAction.contains("right")) sendMsg("R");
            }

        }

    }

    public static List<String> getText(Notification notification)
    {
        // We have to extract the information from the view
        RemoteViews views = notification.bigContentView;
        if (views == null) views = notification.contentView;
        if (views == null) return null;

        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        List<String> text = new ArrayList<String>();
        try
        {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions)
            {

                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;

                // View ID
                parcel.readInt();

                String methodName = parcel.readString();
                if (methodName == null) continue;

                    // Save strings
                else if (methodName.equals("setText"))
                {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();

                    // Store the actual string
                    CharSequence c = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                    if(c != null && c.length()>0) {
                        String t = c.toString().trim();
                        text.add(t);
                    }
                }

                parcel.recycle();
            }
        }

        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e)
        {
            Log.e("NotificationClassifier", e.toString());
        }

        return text;
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onServiceConnected(){
        super.onServiceConnected();

        //TODO - Connect to bluetooth here.
        //mRLPBluetoothManager = RLPBluetoothManager.getInstance(this);
        connectToBluetooth();
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

    private void connectToBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.cancelDiscovery();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Notification n  = new Notification.Builder(this)
                    .setContentTitle("Unable to connect to your co-piglet.")
                    .setContentText("ERROR")
                    .setSmallIcon(R.drawable.ic_stat_pignotificationicon_no_pupil_light)
                    .setAutoCancel(true).build();


            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(0, n);
        }
        else
            getPairedDevices();

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
            String name = b.getName();
            if(name.equalsIgnoreCase("itead")) {
                deviceToConnect = b;
                connectToDevice();
            }
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
