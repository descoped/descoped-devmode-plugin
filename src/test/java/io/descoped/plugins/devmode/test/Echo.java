package io.descoped.plugins.devmode.test;

/**
 * Created by oranheim on 06/01/2017.
 */
public class Echo {

    public Echo() {
        System.out.println("Echo will wait for 2000ms..");
    }

    private void sleep() {
        try {
            System.out.println("Sleep some...");
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Echo echo = new Echo();
        echo.sleep();
    }

}
