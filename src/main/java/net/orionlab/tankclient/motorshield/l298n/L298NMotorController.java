package net.orionlab.tankclient.motorshield.l298n;

import com.pi4j.io.gpio.*;

public class L298NMotorController {
    private L298NMotor leftMotor;
    private L298NMotor rightMotor;
    private boolean isInitialized = false;

    public L298NMotorController() {
        leftMotor = new L298NMotor("LeftMotor");
        rightMotor = new L298NMotor("RightMotor");
    }

    /**
     * GPIO Wiring to Raspberry mapping: https://pinout.xyz/pinout/wiringpi#
     */
    public void initMotors(GpioController gpioController) {
        if (isInitialized) return;
        isInitialized = true;
        leftMotor.initPins(gpioController, RaspiPin.GPIO_26, RaspiPin.GPIO_04, RaspiPin.GPIO_05);
        rightMotor.initPins(gpioController, RaspiPin.GPIO_01, RaspiPin.GPIO_03, RaspiPin.GPIO_02);
    }

    public void leftForward(float speed) {
        leftMotor.forward(speed);
    }

    public void rightForward(float speed) {
        rightMotor.forward(speed);
    }

    public void leftBackward(float speed) {
        leftMotor.backward(speed);
    }

    public void rightBackward(float speed) {
        rightMotor.backward(speed);
    }

    public void brake() {
        leftMotor.brake();
        rightMotor.brake();
    }

    public void close() {
        try {
            brake();
            leftMotor.close();
            rightMotor.close();
            leftMotor = null;
            rightMotor = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}