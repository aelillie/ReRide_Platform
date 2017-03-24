package com.example.anders.flexsensor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements FlexSensorManager.CallBack{

    public static final String LOG_TAG = MainActivity.class.getCanonicalName();

    private TextView stateText;
    private EditText angleText;
    private Button updateButton;
    private Button refresh;

    private FlexSensorManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = new FlexSensorManager(this);

        stateText = (TextView) findViewById(R.id.stateText);
        angleText = (EditText) findViewById(R.id.angle_text);
        updateButton = (Button) findViewById(R.id.updateAngle);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String angle = angleText.getText().toString();
                manager.update(angle);
            }
        });
        refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.getShadow();
            }
        });
    }

    @Override
    public void updatedState(int newState) {
        //angleText.setText(newState);
    }
}
