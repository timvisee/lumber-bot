package com.timvisee.lumberbot;

import java.awt.*;
import java.awt.event.KeyEvent;

public class Bot {

    public Bot() {}

    public void init() throws InterruptedException {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                // Wait
                try {
                    Thread.sleep(2000);

                    // Get the mouse location
                    Point scanPoint = MouseInfo.getPointerInfo().getLocation();
                    System.out.println("Scan point selected.");

                    Thread.sleep(1000);

                    Robot robot = new Robot();

                    // Get the branch color
                    Point branchPoint = MouseInfo.getPointerInfo().getLocation();
                    int branchColor = robot.getPixelColor(branchPoint.x, branchPoint.y).getRGB();
                    System.out.println("Branch color selected");

                    System.out.println("Started");

                    while(true) {
                        Color currentColor = robot.getPixelColor(scanPoint.x, scanPoint.y);

                        boolean left = currentColor.getRGB() == branchColor;
                        int key = left ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT;

                        System.out.println(left ? "LEFT" : "RIGHT");

                        for (int i = 0; i < 2; i++) {
                            robot.keyPress(key);
                            Thread.sleep(20);
                            robot.keyRelease(key);
                            Thread.sleep(20);
                        }

                        Thread.sleep(150);
                    }

                } catch (InterruptedException | AWTException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
}
