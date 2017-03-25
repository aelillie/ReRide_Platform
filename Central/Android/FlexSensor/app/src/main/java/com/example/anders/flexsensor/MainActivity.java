package com.example.anders.flexsensor;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements FlexSensorManager.CallBack{

    public static final String LOG_TAG = MainActivity.class.getCanonicalName();

    private TextView stateText;
    private EditText angleText;
    private Button updateButton;
    private Button refresh;

    private FlexSensorManager flexSensorManager;
    private BLEManager bleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flexSensorManager = new FlexSensorManager(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (BLEManager.isBLESupported(getApplicationContext())) {
                bleManager = new BLEManager(getApplicationContext());
            } else {
                Toast.makeText(this, "BLE not supported", Toast.LENGTH_LONG).show();
                return; //TODO: Handle this correctly
            }
        } else {
            Toast.makeText(this, "Higher API required", Toast.LENGTH_LONG).show();
            return; //TODO: Handle this correctly
        }

        checkBLE();

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
    }

    @Override
    public void updatedState(int newState) {
        //angleText.setText(newState);
    }

    private void checkBLE() {
        Intent enableBLEIntent = bleManager.enableBLE();
        if (enableBLEIntent != null) {
            startActivityForResult(enableBLEIntent, BLEManager.REQUEST_ENABLE_BT);
        }
        //TODO: Handle result
    }
}
