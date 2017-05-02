package com.bswearingen.www.rlpmapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    RLPBluetoothManager mRLPBluetoothManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        mRLPBluetoothManager = RLPBluetoothManager.getInstance(this);
        connectToDevice();
    }

    protected void onPause(){
        super.onPause();
        mRLPBluetoothManager.onPause();
    }

    private void connectToDevice(){
        Button left = (Button) findViewById(R.id.demo_left);
        Button both = (Button) findViewById(R.id.demo_both);
        Button right = (Button) findViewById(R.id.demo_right);

        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = "L";

                mRLPBluetoothManager.sendMsg(msg);
            }
        });
        both.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = "U";

                mRLPBluetoothManager.sendMsg(msg);
            }
        });
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = "R";

                mRLPBluetoothManager.sendMsg(msg);
            }
        });
    }
}