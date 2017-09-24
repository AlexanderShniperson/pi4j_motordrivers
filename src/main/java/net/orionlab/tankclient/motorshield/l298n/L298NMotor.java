package net.orionlab.tankclient.motorshield.l298n;

import com.pi4j.io.gpio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class L298NMotor {
    private final Logger log = LoggerFactory.getLogger("L298NMotor");
    private GpioController gpio;
    private GpioPinPwmOutput speedPin;
    private GpioPinDigitalOutput forwardPin;
    private GpioPinDigitalOutput backwardPin;
    private String motorName;
    private boolean isInitialized = false;
    private final int pwmRange = 1000;
    private final int pwmClock = 500;

    public L298NMotor(String motorName) {
        this.motorName = motorName;
    }

    public void initPins(GpioController gpioController, Pin speedGpioPin, Pin forwardGpioPin, Pin backwardGpioPin) {
        if (isInitialized) return;

        gpio = gpioController;
        speedPin = gpio.provisionPwmOutputPin(speedGpioPin, String.format("%s_SPEED", motorName), 0);
        forwardPin = gpio.provisionDigitalOutputPin(forwardGpioPin, String.format("%s_FORWARD", motorName), PinState.LOW);
        backwardPin = gpio.provisionDigitalOutputPin(backwardGpioPin, String.format("%s_BACKWARD", motorName), PinState.LOW);
        // set shutdown state for this input pin
        speedPin.setShutdownOptions(true);
        forwardPin.setShutdownOptions(true);
        backwardPin.setShutdownOptions(true);

        this.speedPin.setPwmRange(pwmRange);

        // you can optionally use these wiringPi methods to further customize the PWM generator
        // see: http://wiringpi.com/reference/raspberry-pi-specifics/
        //com.pi4j.wiringpi.Gpio.pwmSetMode(com.pi4j.wiringpi.Gpio.PWM_MODE_MS);
        //com.pi4j.wiringpi.Gpio.pwmSetRange(pwmRange);
        //com.pi4j.wiringpi.Gpio.pwmSetClock(pwmClock);

        log.info(String.format(Locale.ROOT, "Pin speed address for %s=%d", motorName, this.speedPin.getPin().getAddress()));

        isInitialized = true;
    }

    public void forward(float speed) {
        if (!isInitialized) return;
        backwardPin.low();
        forwardPin.high();
        try {
            setSpeed(speed);
        } catch (Exception ex) {
            log.info(String.format(Locale.ROOT, "Error when writing to Forward speed pin(%d): %s", this.speedPin.getPin().getAddress(), ex.getMessage()));
        }
    }

    public void backward(float speed) {
        if (!isInitialized) return;
        forwardPin.low();
        backwardPin.high();
        try {
            setSpeed(speed);
        } catch (Exception ex) {
            log.info(String.format(Locale.ROOT, "Error when writing to Backward speed pin(%d): %s", this.speedPin.getPin().getAddress(), ex.getMessage()));
        }
    }

    private void setSpeed(float speed) throws Exception {
        if (!isInitialized) return;
        int speedValue = pwmRange / 100 * Math.round(speed);
        log.info(String.format(Locale.ROOT, "Set '%s' speed '%d'", this.motorName, speedValue));
        speedPin.setPwm(speedValue);
    }

    /**
     * Brake motors
     */
    public void brake() {
        if (!isInitialized) return;
        forwardPin.low();
        backwardPin.low();
        try {
            setSpeed(0f);
        } catch (Exception ex) {
            log.info(String.format(Locale.ROOT, "Error when writing to Brake speed pin(%d): %s", this.speedPin.getPin().getAddress(), ex.getMessage()));
        }
    }

    public void close() {
        if (!isInitialized) return;
        try {
            gpio.unexport(speedPin);
            gpio.unexport(forwardPin);
            gpio.unexport(backwardPin);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
