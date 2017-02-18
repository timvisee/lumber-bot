package com.timvisee.lumberbot;

public enum BotState {

    /**
     * State used for initialization.
     */
    INIT,

    /**
     * State the user must select the tree in.
     */
    SELECT_TREE,

    /**
     * State the user must select the bottom branch in.
     */
    SELECT_BRANCH,

    /**
     * State letting the user focus the game window. Before the bot starts playing.
     */
    BEFORE_PLAYING,

    /**
     * State the bot is playing in.
     */
    PLAYING;
}
