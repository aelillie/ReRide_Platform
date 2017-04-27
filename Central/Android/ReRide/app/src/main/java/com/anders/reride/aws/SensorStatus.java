package com.anders.reride.aws;

/**
 * Representation of the flex sensor. Used to represent the IoT shadow of a particular
 * environmental sensor, when subscribing to shadow updates.
 */

class SensorStatus {
    State state;

    SensorStatus() {
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

            Integer value;
            String curState;
        }

        class Delta {
            Delta() {
            }

            public Integer value;
            public String curState;
        }
    }

    public Long version;
    public Long timestamp;
}
