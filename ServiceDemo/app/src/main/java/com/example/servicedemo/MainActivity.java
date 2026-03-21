package com.example.servicedemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // -------------------------------------------------------
    // UI References
    // -------------------------------------------------------
    private Button btnStartService, btnStopService;
    private Button btnBindService, btnUnbindService, btnGetData;
    private TextView tvServiceStatus;

    // -------------------------------------------------------
    // BoundService reference — we get this through ServiceConnection
    // This is null until the service is bound
    // -------------------------------------------------------
    private BoundService boundService;

    // Flag to track whether we are currently bound
    private boolean isBound = false;

    // -------------------------------------------------------
    // ServiceConnection is the callback interface that Android calls
    // when the binding is established or broken
    // We define it as a field so we can use it in unbindService() later
    // -------------------------------------------------------
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // This is called on the MAIN THREAD when binding is successful
            // 'service' is the IBinder returned by BoundService.onBind()

            // We cast it to our LocalBinder subclass
            BoundService.LocalBinder binder = (BoundService.LocalBinder) service;

            // Now we get the actual BoundService instance
            boundService = binder.getService();

            // Update our flag
            isBound = true;

            Log.d(TAG, "onServiceConnected: Successfully bound to BoundService");
            tvServiceStatus.setText("Status: Bound to service");
            Toast.makeText(MainActivity.this, "Service Connected!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Called when the service CRASHES or is killed unexpectedly
            // NOT called when you manually call unbindService()
            isBound = false;
            boundService = null;

            Log.d(TAG, "onServiceDisconnected: Service disconnected unexpectedly");
            tvServiceStatus.setText("Status: Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find all views by their IDs from activity_main.xml
        btnStartService  = findViewById(R.id.btnStartService);
        btnStopService   = findViewById(R.id.btnStopService);
        btnBindService   = findViewById(R.id.btnBindService);
        btnUnbindService = findViewById(R.id.btnUnbindService);
        btnGetData       = findViewById(R.id.btnGetData);
        tvServiceStatus  = findViewById(R.id.tvServiceStatus);

        // -------------------------------------------------------
        // Started Service: Start Button
        // -------------------------------------------------------
        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an explicit Intent targeting StartedService
                Intent intent = new Intent(MainActivity.this, StartedService.class);

                // startService() triggers onStartCommand() in the service
                startService(intent);

                tvServiceStatus.setText("Status: Started Service running...");
                Log.d(TAG, "startService() called");
            }
        });

        // -------------------------------------------------------
        // Started Service: Stop Button
        // -------------------------------------------------------
        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StartedService.class);

                // stopService() triggers onDestroy() in the service
                stopService(intent);

                tvServiceStatus.setText("Status: Started Service stopped");
                Log.d(TAG, "stopService() called");
            }
        });

        // -------------------------------------------------------
        // Bound Service: Bind Button
        // -------------------------------------------------------
        btnBindService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isBound) {
                    Intent intent = new Intent(MainActivity.this, BoundService.class);

                    // bindService(intent, connection, flags)
                    // BIND_AUTO_CREATE: Create service if it doesn't exist yet
                    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

                    tvServiceStatus.setText("Status: Binding to Bound Service...");
                    Log.d(TAG, "bindService() called");
                } else {
                    Toast.makeText(MainActivity.this,
                            "Already bound!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // -------------------------------------------------------
        // Bound Service: Unbind Button
        // -------------------------------------------------------
        btnUnbindService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    // unbindService triggers onUnbind() in the service
                    unbindService(serviceConnection);
                    isBound = false;
                    boundService = null;

                    tvServiceStatus.setText("Status: Unbound from service");
                    Log.d(TAG, "unbindService() called");
                } else {
                    Toast.makeText(MainActivity.this,
                            "Not currently bound!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // -------------------------------------------------------
        // Bound Service: Get Data Button
        // -------------------------------------------------------
        btnGetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound && boundService != null) {
                    // Directly call the service method — this is the power of Bound Service
                    // It's like calling a method on any regular Java object
                    String status = boundService.getStatusMessage();
                    int counter = boundService.getCounter();

                    tvServiceStatus.setText("Data from Service:\n" + status);
                    Log.d(TAG, "Data fetched from BoundService: Counter = " + counter);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Not bound to service yet!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // VERY IMPORTANT: Always unbind when Activity is destroyed
        // Failing to do this causes a ServiceConnection leak
        // Android will print a warning: "Activity has leaked ServiceConnection"
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}