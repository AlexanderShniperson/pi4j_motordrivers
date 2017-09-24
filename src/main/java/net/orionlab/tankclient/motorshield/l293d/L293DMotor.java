package net.orionlab.tankclient.motorshield.l293d;

import com.pi4j.io.gpio.*;

public class L293DMotor {
    private final String motorName;
    private GpioPinDigitalOutput pin;
    private Integer[] direction;
    private Boolean isRunning = false;
    private Integer runningDirection;
    private GpioPinPwmOutput pwm;
    private Boolean isConfiguredAsPWM = false;

    public L293DMotor(String motorName) {
        this.motorName = motorName;
    }

    public void initAsDigitalOutput(GpioController gpioController, Pin pin, Integer[] direction) {
        this.pin = gpioController.provisionDigitalOutputPin(pin, motorName, PinState.LOW);
        this.pin.setShutdownOptions(true);
        this.direction = direction;
    }

    public void initAsPWMOutput(GpioController gpioController,Pin pin, Integer[] direction, Integer pwmDutyCycle) {
        this.pwm = gpioController.provisionPwmOutputPin(pin, motorName, 0);
        this.pwm.setPwmRange(pwmDutyCycle);
        this.pwm.setShutdownOptions(true);
        this.direction = direction;
        this.isConfiguredAsPWM = true;
    }

    public boolean getIsPwmOutput() {
        return isConfiguredAsPWM;
    }

    public GpioPinDigitalOutput getPin() {
        return pin;
    }

    public Integer[] getDirection() {
        return direction;
    }

    public Boolean getRunning() {
        return isRunning;
    }

    public void setRunning(Boolean running) {
        isRunning = running;
    }

    public Integer getRunningDirection() {
        return runningDirection;
    }

    public void setRunningDirection(Integer runningDirection) {
        this.runningDirection = runningDirection;
    }

    public GpioPinPwmOutput getPwm() {
        return this.pwm;
    }

    public int getSpeed() {
        if (this.pwm == null) {
            System.out.println(String.format("Pin PWM isn't configured for '%s'.", motorName));
            return 0;
        }
        return this.pwm.getPwm();
    }

    public void setSpeed(int value) {
        if (this.pwm == null) {
            System.out.println(String.format("Pin PWM isn't configured for '%s'.", motorName));
            return;
        }
        if (value >= 0 && value <= 100) {
            System.out.println(String.format("WARNING: Speed argument must be in range 0-100! But %s given. " +
                    "Keeping previous setting (%s).", value, getSpeed()));
            return;
        }
        this.pwm.setPwm(value);
    }
}
