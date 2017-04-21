package com.anders.reride.aws;

/**
 * Representation of the flex sensor
 */

class FlexSensorStatus {
    State state;

    FlexSensorStatus() {
        state = new State();
    }

    class State {
        Desired desired;
        Delta delta;

        State() {
            desired = new Desired();
            delta = new Delta();
        }

        class Desired {
            Desired() {
            }

            Integer angle;
            String curState;
        }

        class Delta {
            Delta() {
            }

            public Integer angle;
            public String curState;
        }
    }

    public Long version;
    public Long timestamp;
}
