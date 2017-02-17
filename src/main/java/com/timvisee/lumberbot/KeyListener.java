package com.timvisee.lumberbot;

import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class KeyListener implements NativeKeyListener {

    /**
     * Related bot instance.
     */
    private Bot bot;

    /**
     * Constructor.
     *
     * @param bot Bot instance.
     */
    public KeyListener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // Handle the escape key
        if(e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            // Request to stop the bot
            bot.requestStop();
            return;
        }

        // Handle the action key
        if(e.getKeyCode() == NativeKeyEvent.VC_ENTER)
            // Invoke the action
            bot.invokeAction();

        // Handle the auto restart key
        if(e.getKeyCode() == NativeKeyEvent.VC_A)
            // Toggle auto restart
            bot.toggleAutoRestart();
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {}
}

