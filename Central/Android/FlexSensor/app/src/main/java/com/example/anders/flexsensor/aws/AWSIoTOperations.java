package com.example.anders.flexsensor.aws;

import android.os.Bundle;

/**
 * Provides basic data transmission operations
 */

interface AWSIoTOperations {
    void publish(Bundle data);
    void subscribe();
    Bundle getShadow();
    void updateShadow(Bundle state);
    boolean connect();
    boolean disconnect();
}
