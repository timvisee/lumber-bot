package com.timvisee.lumberbot;

import org.jnativehook.GlobalScreen;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.LinkedList;

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

    /**
     * Maximum value of red for a branch color.
     */
    private static final int COLOR_BRANCH_MAX_RED = 186;

    /**
     * Maximum value of green for a branch color.
     */
    private static final int COLOR_BRANCH_MAX_GREEN = 140;

    /**
     * Maximum value of blue for a branch color.
     */
    private static final int COLOR_BRANCH_MAX_BLUE = 77;

    /**
     * Key press duration for simulated keys, in milliseconds.
     */
    private static final int KEY_PRESS_DURATION = 20;

    /**
     * Amount of milliseconds to wait for each move.
     */
    private static final int MOVE_DELAY = 125;

    /**
     * Buffer containing the upcoming player moves.
     */
    private LinkedList<Boolean> movesBuffer = new LinkedList<>();

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

        // Add an initial move for the player
        this.movesBuffer.add(true);

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
                this.state = BotState.PLAYING;
                break;

            case PLAYING:
                // Make sure the tree is still here
                if(!isBranchAtTreePoint()) {
                    // Show a status message
                    System.out.println("You've died!");
                    requestStop();
                    return;
                }

                // Buffer the next move
                bufferNextMove();

                // Simulate the next move
                simulateNextMove();

                // Sleep for a little
                try {
                    Thread.sleep(MOVE_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
     * Check whether there's a branch at the branch point.
     *
     * @return True if there's a branch at the branch point.
     */
    public boolean isBranchAtBranchPoint() {
        return isBranchAt(this.branchPoint);
    }

    /**
     * Check whether there's a branch/tree at the tree position.
     *
     * @return True if there's a branch/tree, false if not.
     */
    public boolean isBranchAtTreePoint() {
        return isBranchAt(this.treePoint);
    }

    /**
     * Determine what buffered move to do.
     * This will check if there's any branch at the branch position, and it will determine a next move on this accordingly.
     *
     * @return Determine what buffered move to do.
     */
    public boolean determineBufferedMove() {
        return ((isBranchAtBranchPoint() ? 1 : 0) + (isBranchPointLeft() ? 1 : 0)) % 2 == 1;
    }

    /**
     * Determine what move to do next, and add it to the moves buffer.
     */
    public void bufferNextMove() {
        this.movesBuffer.add(determineBufferedMove());
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

    /**
     * Check whether the selected branch point is on the left side of the tree.
     *
     * @return True if the point is left, false if the point is right.
     */
    public boolean isBranchPointLeft() {
        return this.branchPoint.x <= this.treePoint.x;
    }

    /**
     * Simulate a key press for the given key.
     *
     * @param keyCode Key code of the key to press.
     * @param count Amount of times to press the key.
     */
    public void simulateKeyPress(int keyCode, int count) {
        try {
            // Loop through the key count
            for (int i = 0; i < count; i++) {
                this.robot.keyPress(keyCode);
                Thread.sleep(KEY_PRESS_DURATION);
                this.robot.keyRelease(keyCode);
                Thread.sleep(KEY_PRESS_DURATION);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Simulate a key press for an arrow key.
     *
     * @param left True if it's the left arrow, false if it's the right.
     * @param count Amount of times to press the key.
     */
    public void simulateKeyPressArrow(boolean left, int count) {
        // Show a status message
        System.out.println("Moving: " + (left ? "Left" : "Right"));

        // Simulate the key press
        simulateKeyPress(left ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT, count);
    }

    /**
     * Simulate the next move.
     */
    private void simulateNextMove() {
        // Get and consume the next move, then simulate it
        simulateKeyPressArrow(this.movesBuffer.pop(), 2);
    }
}
