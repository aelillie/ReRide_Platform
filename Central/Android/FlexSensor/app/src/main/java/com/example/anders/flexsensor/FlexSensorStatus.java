package com.example.anders.flexsensor;

/**
 * Representation of the flex sensor
 */

public class FlexSensorStatus {
    public State state;

    FlexSensorStatus() {
        state = new State();
    }

    public class State {
        Desired desired;
        Delta delta;

        State() {
            desired = new Desired();
            delta = new Delta();
        }

        public class Desired {
            Desired() {
            }

            public Integer angle;
            public String curState;
        }

        public class Delta {
            Delta() {
            }

            public Integer angle;
            public String curState;
        }
    }

    public Long version;
    public Long timestamp;
}
