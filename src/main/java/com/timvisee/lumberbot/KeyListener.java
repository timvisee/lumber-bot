package com.timvisee.lumberbot;

import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class KeyListener implements NativeKeyListener {

    private Bot bot;

    public KeyListener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if(e.getKeyCode() == NativeKeyEvent.VC_S) {
            System.out.println("Action key pressed!");

            bot.notifyAll();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {}
}

