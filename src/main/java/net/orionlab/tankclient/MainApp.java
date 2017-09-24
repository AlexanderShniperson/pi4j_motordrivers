package net.orionlab.tankclient;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.util.Console;
import com.pi4j.wiringpi.Gpio;
import net.orionlab.tankclient.motorshield.l293d.L293DMotorController;
import net.orionlab.tankclient.motorshield.l298n.L298NMotorController;

public class MainApp {
    private static Console console = null;
    private static GpioController gpioController;
    public static L298NMotorController chassisController;
    public static L293DMotorController towerController;

    public static void main(String[] args) {
        if (Gpio.wiringPiSetup() < 0) console.println(">>> WiringPi initialization problem.");
        gpioController = GpioFactory.getInstance();
        chassisController = new L298NMotorController();
        towerController = new L293DMotorController();

        try {
            console = new Console();
            console.title("Welcome to tank client.");

            initMotorControllers();
            chassisController.leftForward(((float) 0));
            chassisController.rightForward(((float) 50));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public synchronized void start() {
                    MainApp.chassisController.close();
                    MainApp.towerController.close();
                    gpioController.shutdown();
                    console.goodbye();
                    super.start();
                }
            });

            console.waitForExit();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
        }
    }

    public static void initMotorControllers() {
        MainApp.chassisController.initMotors(gpioController);
        MainApp.towerController.initMotors(gpioController);
    }
}
