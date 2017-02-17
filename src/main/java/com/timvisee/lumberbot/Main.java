package com.timvisee.lumberbot;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) throws NativeHookException, InterruptedException {
        // Register the native hook, and disable it's logging
        Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.OFF);
        GlobalScreen.registerNativeHook();

        // Create a bot instance
        Bot bot = new Bot();

        // Initialize the bot
        try {
            bot.start();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
}
