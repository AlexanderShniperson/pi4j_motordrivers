package net.orionlab.tankclient.motorshield.l293d;

import com.pi4j.io.gpio.*;

import java.util.HashMap;
import java.util.Map;

public class L293DMotorController {
    // Shift register
    private GpioPinDigitalOutput dirLatch;
    private GpioPinDigitalOutput dirClk;
    private GpioPinDigitalOutput dirSer;
    private boolean isInitialized = false;

    public static final int DC_MOTOR_1 = 1;
    public static final int DC_MOTOR_2 = 2;
    public static final int DC_MOTOR_3 = 3;
    public static final int DC_MOTOR_4 = 4;

    private Map<Integer, L293DMotor> motors = new HashMap<>();

    /**
     * Configure Shift registrar 74HCT595N
     * Configure Motor pins
     */
    public void initMotors(GpioController gpioController) {
        if(isInitialized) return;

        isInitialized = true;
        
        // latch 29, clock 28, ser 27
        this.dirLatch = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_29, PinState.LOW);
        this.dirClk = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_28, PinState.LOW);
        this.dirSer = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_27, PinState.LOW);

        // pins 5, 6, 13, 19
        // direction [forward, backward, stop]
        L293DMotor motor1 = new L293DMotor("Motor1");
        motor1.initAsDigitalOutput(gpioController, RaspiPin.GPIO_21, new Integer[]{4, 8, 4 | 8});
        this.motors.put(DC_MOTOR_1, motor1);

        L293DMotor motor2 = new L293DMotor("Motor2");
        motor2.initAsDigitalOutput(gpioController, RaspiPin.GPIO_22, new Integer[]{2, 16, 2 | 16});
        this.motors.put(DC_MOTOR_2, motor2);

        L293DMotor motor3 = new L293DMotor("Motor3");
        motor3.initAsDigitalOutput(gpioController, RaspiPin.GPIO_23, new Integer[]{32, 128, 32 | 128});
        this.motors.put(DC_MOTOR_3, motor3);

        L293DMotor motor4 = new L293DMotor("Motor4");
        motor4.initAsDigitalOutput(gpioController, RaspiPin.GPIO_24, new Integer[]{1, 64, 1 | 64});
        this.motors.put(DC_MOTOR_4, motor4);
    }

    public void motorForward(Integer motorIndex, Integer speed) {
        if (motorIndex < DC_MOTOR_1 || motorIndex > DC_MOTOR_4) {
            System.out.println(String.format("Can't move forward %d, motor index out of range, must be between %d and %d.", motorIndex, DC_MOTOR_1, DC_MOTOR_4));
            return;
        }
        run_dc_motor(motorIndex, true, speed);
    }

    public void motorBackward(Integer motorIndex, Integer speed) {
        if (motorIndex < DC_MOTOR_1 || motorIndex > DC_MOTOR_4) {
            System.out.println(String.format("Can't move backward %d, motor index out of range, must be between %d and %d.", motorIndex, DC_MOTOR_1, DC_MOTOR_4));
            return;
        }
        run_dc_motor(motorIndex, false, speed);
    }

    public void motorStop(Integer motorIndex) {
        if (motorIndex < DC_MOTOR_1 || motorIndex > DC_MOTOR_4) {
            System.out.println(String.format("Can't stop motor %d, motor index out of range, must be between %d and %d.", motorIndex, DC_MOTOR_1, DC_MOTOR_4));
            return;
        }
        stop_dc_motor(motorIndex);
    }

    private boolean test_shift_pins() {
        return dirLatch != null && dirClk != null && dirSer != null;
    }

    public void stop_all_motors() {
        try {
            if (test_shift_pins()) {
                shift_write(0);
                stop_dc_motor(DC_MOTOR_1);
                stop_dc_motor(DC_MOTOR_2);
                stop_dc_motor(DC_MOTOR_3);
                stop_dc_motor(DC_MOTOR_4);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Write given value to the shift register
     *
     * @param value value which you want to write to shift register
     */
    private void shift_write(int value) {
        if (!test_shift_pins()) {
            System.out.println("ERROR: PINs for shift register were not set properly.");
            stop_all_motors();
        }

        this.dirLatch.low();
        for (int x = 0; x <= 8; x++) {
            int temp = value & 0x80;
            this.dirClk.low();
            if (temp == 0x80) {
                // data bit HIGH
                this.dirSer.high();
            } else {
                // data bit LOW
                this.dirSer.low();
            }
            this.dirClk.high();
            value <<= 0x01;  // shift left
        }
        this.dirLatch.high();
    }

    /**
     * Compute number that should be written to shift register to run / stop motor
     *
     * @param motorIndex     number of dc motor
     * @param directionIndex index to motor direction list
     * @return number for shift register, motors direction value
     */
    private Integer[] get_motors_direction(int motorIndex, int directionIndex) {
        int direction_value = motors.get(motorIndex).getDirection()[directionIndex];
        int all_motors_direction = direction_value;
        for (int tmp_dc_motor : motors.keySet()) {
            if (tmp_dc_motor == motorIndex)
                continue;
            if (motors.get(motorIndex).getRunningDirection() != null)
                all_motors_direction += motors.get(motorIndex).getRunningDirection();
        }

        return new Integer[]{all_motors_direction, direction_value};
    }

    /**
     * Run motor with given direction
     *
     * @param motorIndex number of dc motor
     * @param isForward  True for clockwise False for counterclockwise
     * @param speed      pwm duty cycle (range 0-100)
     * @return boolean False in case of an ERROR, True if everything is OK
     */
    private boolean run_dc_motor(Integer motorIndex, Boolean isForward, Integer speed) {
        Integer[] motorsDirection = get_motors_direction(motorIndex, (!isForward) ? 1 : 0);
        Integer all_motors_direction = motorsDirection[0];
        Integer direction_value = motorsDirection[1];
        L293DMotor currentMotor = motors.get(motorIndex);

        if (currentMotor.getRunning() && currentMotor.getRunningDirection().equals(direction_value)) return true;

        //set motors direction
        shift_write(all_motors_direction);

        //turn the motor on ( if speed argument is not given then full speed, otherwise set pwm according to speed)
        if (speed == null && !currentMotor.getIsPwmOutput()) {
            currentMotor.getPin().high();
        } else {
            currentMotor.setSpeed(speed);
        }

        currentMotor.setRunning(true);
        currentMotor.setRunningDirection(direction_value);

        return true;
    }

    /**
     * Stop running motor
     *
     * @param motorIndex number of dc motor
     * @return False in case of an ERROR, True if everything is OK
     */
    private boolean stop_dc_motor(int motorIndex) {
        L293DMotor currentMotor = motors.get(motorIndex);

        if (currentMotor.getPin() == null) {
            System.out.println(String.format("WARNING: Pin for DC_Motor_%d is not set. Stopping motor could not be done", motorIndex));
            return false;
        }

        Integer[] motorsDirection = get_motors_direction(motorIndex, 2);
        Integer all_motors_direction = motorsDirection[0];
        Integer direction_value = motorsDirection[1];

        shift_write(all_motors_direction);

        if (currentMotor.getPwm() == null) {
            currentMotor.getPin().low();
        } else {
            currentMotor.setSpeed(0);
        }

        currentMotor.setRunning(false);
        currentMotor.setRunningDirection(null);
        return true;
    }

    public void close() {
        try {
            stop_all_motors();
            motors.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
