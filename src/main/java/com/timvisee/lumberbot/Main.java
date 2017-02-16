package com.timvisee.lumberbot;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

public class Main {

    public static void main(String[] args) throws NativeHookException, InterruptedException {
        // Register the native hook
//        GlobalScreen.registerNativeHook();

        // Create a bot instance
        Bot bot = new Bot();

        // Register the key listener
//        GlobalScreen.addNativeKeyListener(new KeyListener(bot));

        // Initialize the bot
        bot.init();
    }
}
