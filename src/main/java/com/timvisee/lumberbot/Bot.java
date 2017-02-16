package com.timvisee.lumberbot;

import org.jnativehook.GlobalScreen;

import java.awt.*;
import java.awt.event.KeyEvent;

public class Bot {

    /**
     * Robot instance.
     */
    private Robot robot;

    /**
     * Current bot state.
     */
    private BotState state = BotState.INIT;

    /**
     * True if requested to stop.
     */
    private boolean requestStop = false;

    /**
     * Check whether an action is invoked.
     */
    public boolean invokedAction = false;

    /**
     * Tree scanning point.
     */
    public Point treePoint;

    /**
     * Branch scanning point.
     */
    public Point branchPoint;

    private static final int COLOR_BRANCH_MAX_RED = 186;
    private static final int COLOR_BRANCH_MAX_GREEN = 140;
    private static final int COLOR_BRANCH_MAX_BLUE = 77;

    /**
     * Constructor.
     */
    public Bot() {}

    /**
     * Start the bot.
     *
     * @throws AWTException Throws if an error occurred.
     */
    public void start() throws AWTException {
        // Create a robot instance
        this.robot = new Robot();

        // Register the key listener
        GlobalScreen.addNativeKeyListener(new KeyListener(this));

        // Create a thread to run the bot in
        final Thread botThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Show a status message
                System.out.println("Starting bot...");

                // Loop until the bot should stop
                while(!isRequestStop()) {
                    // Call the bot update method
                    update();

                    // Sleep a little
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Show a status message
                System.out.println("The bot has stopped!");
            }
        });

        // Start the thread
        botThread.start();
    }

    /**
     * Update call.
     */
    public void update() {
        // Select the code for the current state
        switch(this.state) {
            case INIT:
                // Show an introduction message
                System.out.println("Move your mouse on the tree, then press ENTER. (not a branch)");
                this.state = BotState.SELECT_TREE;
                break;

            case SELECT_TREE:
                // Break if no action key was pressed
                if(!consumeActionInvoked())
                    return;

                // Store the tree position
                this.treePoint = getMousePosition();

                // Make sure the color under the mouse is the color of a branch
                if(!isBranchAt(this.treePoint)) {
                    System.out.println("Your mouse isn't hovering the tree. Try again.");
                    return;
                }

                // Show a status message
                System.out.println("Move your mouse on the wood of the bottom branch, then press ENTER.");
                this.state = BotState.SELECT_BRANCH;
                break;

            case SELECT_BRANCH:
                // Break if no action key was pressed
                if(!consumeActionInvoked())
                    return;

                // Store the branch position
                this.branchPoint = getMousePosition();

                // Make sure the color under the mouse is the color of a branch
                if(!isBranchAt(this.branchPoint)) {
                    System.out.println("Your mouse isn't hovering the branch. Try again.");
                    return;
                }

                System.out.println("Move your mouse on the wood of the bottom branch, then press ENTER.");
                break;

            case PLAYING:
                break;

            case DONE:
                break;
        }
    }

    /**
     * Get the current mouse position.
     *
     * @return Mouse position.
     */
    private Point getMousePosition() {
        return MouseInfo.getPointerInfo().getLocation();
    }

    /**
     * Get the pixel color at the given point.
     *
     * @param point Point to get the color for.
     *
     * @return Point color.
     */
    public Color getColorAt(Point point) {
        return robot.getPixelColor(point.x, point.y);
    }

    /**
     * Check whether there's a branch color at the given point.
     *
     * @param point Point.
     *
     * @return True if there's a branch color, false if not.
     */
    public boolean isBranchAt(Point point) {
        return isBranchColor(getColorAt(point));
    }

    /**
     * Check whether the given color is the color of a branch.
     * Note: This is an approximation.
     *
     * @param color Color to check.
     *
     * @return True if the color is the color if a branch, false if not.
     */
    private boolean isBranchColor(Color color) {
        return color.getRed() <= COLOR_BRANCH_MAX_RED &&
                color.getGreen() <= COLOR_BRANCH_MAX_GREEN &&
                color.getBlue() <= COLOR_BRANCH_MAX_BLUE;
    }

    /**
     * Invoke an action.
     */
    public void invokeAction() {
        this.invokedAction = true;
    }

    /**
     * Check whether an action is invoked.
     *
     * @return True if an action is invoked.
     */
    public boolean isActionInvoked() {
        return this.invokedAction;
    }

    /**
     * Consume an invoked action.
     *
     * @return True if an action was invoked, false if not.
     */
    public boolean consumeActionInvoked() {
        // Get the result
        final boolean result = this.invokedAction;

        // Reset the invoked action s tate
        this.invokedAction = false;

        // Return the result
        return result;
    }

    /**
     * Request to stop the bot.
     */
    public void requestStop() {
        this.requestStop = true;
    }

    /**
     * Check whether a stop is requested.
     *
     * @return True if a stop is requested.
     */
    public boolean isRequestStop() {
        return this.requestStop;
    }


    // TODO: OLD CODE, REMOVE IT
    public void init() throws InterruptedException {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                // Wait
                try {
                    Thread.sleep(2000);

                    // Get the mouse location
                    Point scanPoint = MouseInfo.getPointerInfo().getLocation();

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
