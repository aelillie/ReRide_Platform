package com.example.anders.flexsensor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.anders.flexsensor.BLE.BLEActivity;

public class MainActivity extends AppCompatActivity implements FlexSensorManager.CallBack{

    public static final String LOG_TAG = MainActivity.class.getCanonicalName();

    private TextView stateText;
    private EditText angleText;
    private Button updateButton;
    private Button refresh;
    private Button scanner;

    private FlexSensorManager flexSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        flexSensorManager = new FlexSensorManager(this);
        stateText = (TextView) findViewById(R.id.stateText);
        angleText = (EditText) findViewById(R.id.angle_text);
        updateButton = (Button) findViewById(R.id.updateAngle);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String angle = angleText.getText().toString();
                flexSensorManager.update(angle);
            }
        });
        refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flexSensorManager.getShadow();
            }
        });
        scanner = (Button) findViewById(R.id.bleScanButton);
        scanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanForDevice();
            }
        });
    }

    @Override
    public void updatedState(int newState) {
        angleText.setText(newState);
    }

    private void scanForDevice() {
        if (BLEActivity.isBLESupported(this)) {
            Intent intent = new Intent(this, BLEActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_LONG).show();
        }
    }
}
