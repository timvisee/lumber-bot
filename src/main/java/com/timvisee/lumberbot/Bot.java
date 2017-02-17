package com.timvisee.lumberbot;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jnativehook.GlobalScreen;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
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
     * Branch scanning point on the other side of the tree.
     */
    public Point otherBranchPoint;

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
     * Minimum value of red for a branch color.
     */
    private static final int COLOR_BRANCH_MIN_RED = 136;

    /**
     * Minimum value of green for a branch color.
     */
    private static final int COLOR_BRANCH_MIN_GREEN = 99;

    /**
     * Minimum value of blue for a branch color.
     */
    private static final int COLOR_BRANCH_MIN_BLUE = 50;

    /**
     * Key press duration for simulated keys, in milliseconds.
     */
    private static final int KEY_PRESS_DURATION = 10;

    /**
     * Amount of milliseconds to wait for each move.
     */
    private static final int MOVE_DELAY = 150;

    /**
     * Minimum amount of milliseconds to wait for each move.
     */
    private static final int MOVE_DELAY_MIN = 15;

    /**
     * Current delay between moves, in milliseconds.
     */
    private int currentMoveDelay = 0;

    /**
     * Offset of the last branch.
     */
    private int lastBranchOffset = 0;

    /**
     * Last deviation in branch positions.
     */
    private int lastDeviation = 0;

    /**
     * Queue of the last branch offsets.
     * Used to determine the average offset and deviation.
     */
    private CircularFifoQueue<Integer> branchOffsetQueue = new CircularFifoQueue<>(10);

    /**
     * Thickness of the branch.
     */
    private int branchThickness = 0;

    /**
     * Buffer containing the upcoming player moves.
     */
    private LinkedList<Boolean> movesBuffer = new LinkedList<>();

    /**
     * True to automatically restart after playing. False otherwise.
     */
    private boolean autoRestart = false;

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
                System.out.println("Press ESCAPE to stop the bot.");
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

                // Show a status message
                System.out.println("Scanning branch thickness...");

                // Determine the size of the branch
                int branchMax = 0;
                int branchMin = 0;
                for (int i = 0; i < 25; i++) {
                    if(!isBranchColor(robot.getPixelColor(this.branchPoint.x, this.branchPoint.y + i))) {
                        branchMax = this.branchPoint.y + i - 1;
                        break;
                    }
                }
                for (int i = 0; i > -25; i--) {
                    if(!isBranchColor(robot.getPixelColor(this.branchPoint.x, this.branchPoint.y + i))) {
                        branchMin = this.branchPoint.y + i + 1;
                        break;
                    }
                }
                branchThickness = branchMax - branchMin;

                // Resetting scanning point to the middle of the branch
                System.out.println("Setting scan point to middle of branch...");
                this.branchPoint.setLocation(this.branchPoint.x, branchMin + (branchMax - branchMin) / 2);

                // Determine the position of branches on the other side of the tree, show a status message
                System.out.println("Scanning for branch on other side of tree...");
                int sideMultiplier = isBranchPointLeft() ? 1 : -1;
                for (int i = 0; i < 300; i++) {
                    // Determine the x coordinate
                    final int x = this.branchPoint.x + (i * sideMultiplier);

                    // Determine whether there's a branch there
                    if (!isBranchColor(robot.getPixelColor(x, this.branchPoint.y))) {
                        // Set the other branch point
                        this.otherBranchPoint = new Point(x + (25 * sideMultiplier), this.branchPoint.y);

                        // Show a status message, and break
                        System.out.println("Other side found.");
                        break;
                    }
                }

                // Show a status message with the branch thickness
                System.out.println("Branch thickness: " + branchThickness + " pixels");

                // Show status message, go to the next state
                System.out.println("Make sure the game window is focused, then press ENTER.");
                this.state = BotState.BEFORE_PLAYING;
                break;

            case BEFORE_PLAYING:
                // Break if no action key was pressed
                if(!consumeActionInvoked())
                    return;

                // Press the space key to start a new game
                simulateKeyPress(KeyEvent.VK_SPACE, 1, 200);

                // Show status message, go to the next state
                System.out.println("Playing!");
                this.state = BotState.PLAYING;

                // Reset the move delay and offset
                currentMoveDelay = MOVE_DELAY;
                lastBranchOffset = 0;

                // Clear the branch offset queue
                branchOffsetQueue.clear();

                // Add an initial move for the player
                this.movesBuffer.clear();
                this.movesBuffer.add(true);
                break;

            case PLAYING:
                // Make sure the tree is still here
                if(!isBranchAtTreePoint()) {
                    // Show a status message
                    System.out.println("You've died!\n");
                    System.out.println("Press ESCAPE stop the bot.");

                    if(!isAutoRestart())
                        System.out.println("Press ENTER to restart the game, press A to toggle auto restart.");
                    else {
                        // Show a status message and wait 2 seconds
                        System.out.println("Automatically restarting in 2 seconds, press A to toggle auto restart.");
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Invoke a new action to automatically restart
                        invokeAction();
                    }

                    // Set the new state
                    this.state = BotState.BEFORE_PLAYING;
                    return;
                }

                // Buffer the next move
                bufferNextMove();

                // Simulate the next move
                simulateNextMove();

                try {
                    // Sleep for a little
                    Thread.sleep(currentMoveDelay);

                    // Decrease or increase the move delay based on the last deviation
                    if(lastDeviation <= 45)
                        currentMoveDelay = Math.max(currentMoveDelay - (currentMoveDelay > 60 ? 3 : 1), MOVE_DELAY_MIN);
                    else
                        currentMoveDelay++;

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
        // Constant holding the scanning area size
        final int scanningSize = 80;

        // Get the offset average, minimum and maximum from the queue
        int offsetAverage = 0;
        int min = -1;
        int max = -1;
        for (Integer entry : branchOffsetQueue) {
            // Append the value to the average counter
            offsetAverage += entry;

            // Update the minimum and maximum
            min = Math.min(entry, min);
            max = Math.max(entry, max);

            // Set the initial value
            if(min < 0)
                min = entry;
        }
        if(branchOffsetQueue.size() > 0)
            offsetAverage = (int) ((float) offsetAverage / (float) branchOffsetQueue.size());

        // Store the deviation
        lastDeviation = max - min;

        // Determine what screen offset to use for the image data
        int screenOffset = Math.max(offsetAverage - 40, 0);

        // Fetch a screen image containing the pixel data we need
        BufferedImage img = robot.createScreenCapture(new Rectangle(this.branchPoint.x, this.branchPoint.y - scanningSize - screenOffset, 1, 80));

        // Loop through the positions the branch might be at
        for (int i = scanningSize - 1; i >= 0; i -= Math.min(branchThickness / 2, 10)) {
            // Get the color of the current pixel
            final Color pixelColor = new Color(img.getRGB(0, i));

            // Check whether there's a branch at the current position, return true if that's the case
            if(isBranchColor(pixelColor)) {
                // Update the last branch offset
                lastBranchOffset = (scanningSize - i) + screenOffset;

                // Queue the branch offset
                branchOffsetQueue.add(lastBranchOffset);

                // Print a status message
                System.out.println("INFO: delay: " + currentMoveDelay + ", deviation: " + lastDeviation + ", offset: " + screenOffset + ", avg: " + offsetAverage + ", last: " + lastBranchOffset);

                // Return the result
                return true;
            }
        }

        // Fetch a screen image containing the pixel data we need from the other side
        img = robot.createScreenCapture(new Rectangle(this.otherBranchPoint.x, this.otherBranchPoint.y - scanningSize - screenOffset, 1, 80));

        // Loop through the positions the branch might be at
        for (int i = scanningSize - 1; i >= 0; i -= Math.min(branchThickness / 2, 10)) {
            // Get the color of the current pixel
            final Color pixelColor = new Color(img.getRGB(0, i));

            // Check whether there's a branch at the current position, return true if that's the case
            if(isBranchColor(pixelColor)) {
                // Update the last branch offset
                lastBranchOffset = (scanningSize - i) + screenOffset;

                // Queue the branch offset
                branchOffsetQueue.add(lastBranchOffset);

                // Print a status message
                System.out.println("INFO: delay: " + currentMoveDelay + ", deviation: " + lastDeviation + ", offset: " + screenOffset + ", avg: " + offsetAverage + ", last: " + lastBranchOffset);
                break;
            }
        }

        // No branch found, return false
        return false;
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
        return color.getRed() <= COLOR_BRANCH_MAX_RED && color.getRed() >= COLOR_BRANCH_MIN_RED &&
                color.getGreen() <= COLOR_BRANCH_MAX_GREEN && color.getGreen() >= COLOR_BRANCH_MIN_GREEN &&
                color.getBlue() <= COLOR_BRANCH_MAX_BLUE && color.getBlue() >= COLOR_BRANCH_MIN_BLUE;
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
        simulateKeyPress(keyCode, count, KEY_PRESS_DURATION);
    }

    /**
     * Simulate a key press for the given key.
     *
     * @param keyCode Key code of the key to press.
     * @param count Amount of times to press the key.
     * @param delay Delay in milliseconds.
     */
    public void simulateKeyPress(int keyCode, int count, int delay) {
        try {
            // Loop through the key count
            for (int i = 0; i < count; i++) {
                this.robot.keyPress(keyCode);
                Thread.sleep(delay);
                this.robot.keyRelease(keyCode);
                Thread.sleep(delay);
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
        System.out.println("MOVE: " + (left ? "Left" : "Right"));

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

    /**
     * Check whether auto restart is enabled.
     *
     * @return True if auto restart is enabled, false if not.
     */
    public boolean isAutoRestart() {
        return this.autoRestart;
    }

    /**
     * Toggle the auto restart mode.
     */
    public void toggleAutoRestart() {
        // Set the new mode
        this.autoRestart = !this.autoRestart;

        // Show a status message
        System.out.println((this.autoRestart ? "Enabled" : "Disabled") + " auto restart mode, press A to toggle.");
    }
}
