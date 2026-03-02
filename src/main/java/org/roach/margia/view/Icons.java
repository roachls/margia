package org.roach.margia.view;

import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("javadoc")
public class Icons {
    private static final Logger LOGGER = LogManager.getLogger(Icons.class);
    private static final int TOOLBAR_ICON_SIZE = 24;
    private static final int MENU_ICON_SIZE = 18;
    private static final Map<String, ImageIcon> LARGE_ICONS = new HashMap<>();
    private static final Map<String, ImageIcon> SMALL_ICONS = new HashMap<>();
    private static final Map<String, ImageIcon> HUGE_ICONS = new HashMap<>();
    public static final String ABOUT = "about";
    public static final String ADD = "add";
    public static final String ADD_CIRCLE = "add_circle";
    public static final String ADD_FAN = "fan";
    public static final String ADD_GRID = "add_grid";
    public static final String CONNECT = "connect";
    public static final String COPY = "copy";
    public static final String DELETE = "delete";
    public static final String DESELECT_ALL = "deselect_all";
    public static final String DISCONNECT = "disconnect";
    public static final String EXIT = "exit";
    public static final String HELP = "help";
    public static final String KEYBOARD = "keyboard";
    public static final String LISTENING = "listening";
    public static final String LOCK_ALL = "lock_all";
    public static final String LOCK = "lock";
    public static final String MOVE = "move";
    public static final String MUTE_ALL = "mute_all";
    public static final String MUTE = "mute";
    public static final String NOT_LISTENING = "not_listening";
    public static final String OPEN = "open";
    public static final String OPTIONS = "options";
    public static final String PASTE = "paste";
    public static final String PAUSE = "pause";
    public static final String REWIND = "rewind";
    public static final String SAVE = "save";
    public static final String SELECT = "select";
    public static final String SELECT_ALL = "select_all";
    public static final String SELECT_CONNECTED = "select_connected";
    public static final String START = "start";
    public static final String UNLOCK_ALL = "unlock_all";
    public static final String UNLOCK = "unlock";
    public static final String UNMUTE = "unmute";
    public static final String UNMUTE_ALL = "unmute_all";
    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
    // @formatter:off
            Map.entry(ABOUT, "an circle with the letter i for information"),
            Map.entry(ADD, "an outline of a person with a plus symbol"),
            Map.entry(ADD_CIRCLE, "eight small circles arranged in a circle and connected by lines"),
            Map.entry(ADD_GRID, "nine small circles arranged in a three by three grid and connected by lines"),
            Map.entry(CONNECT, "two dots with a line between them"),
            Map.entry(COPY, "a notepad"),
            Map.entry(DELETE, "a large capital X"),
            Map.entry(DESELECT_ALL, "a hand with an 'A' and a red 'X' through it"),
            Map.entry(DISCONNECT, "two dots with a broken line between them"),
            Map.entry(EXIT, "A door with an arrow pointing out of it"),
            Map.entry(ADD_FAN, "7 dots in a binary tree formation"),
            Map.entry(HELP, "a question mark"),
            Map.entry(LISTENING, "an ear"),
            Map.entry(LOCK_ALL, "a closed lock with an 'A'"),
            Map.entry(LOCK, "a closed lock"),
            Map.entry(KEYBOARD, "a keyboard"),
            Map.entry(MOVE, "a four-way arrow icon"),
            Map.entry(MUTE_ALL, "a speaker that is muted with a small 'A'"),
            Map.entry(MUTE, "a speaker that is muted"),
            Map.entry(NOT_LISTENING, "an ear with a line through it"),
            Map.entry(OPEN, "an open folder"),
            Map.entry(OPTIONS, ""),
            Map.entry(PASTE, "a clipboard"),
            Map.entry(PAUSE, ""),
            Map.entry(REWIND, ""),
            Map.entry(SAVE, "a floppy disk"),
            Map.entry(SELECT, "a pointing hand"),
            Map.entry(SELECT_ALL, "a pointing hand with an 'A'"),
            Map.entry(SELECT_CONNECTED, "a bunch of circles, some connected, some not, with connected circles in red"),
            Map.entry(START, ""),
            Map.entry(UNLOCK, "an open lock"),
            Map.entry(UNLOCK_ALL, "an open lock with an 'A'"),
            Map.entry(UNMUTE, "an unmuted speaker"),
            Map.entry(UNMUTE_ALL, "an unmuted speaker with an 'A'")
            // @formatter:on
    );

    static {
        for (var key : new String[] { ABOUT, ADD, ADD_CIRCLE, ADD_GRID, CONNECT, COPY, DELETE, DISCONNECT, DESELECT_ALL,
                ADD_FAN, HELP, KEYBOARD, LISTENING, LOCK_ALL, LOCK, MOVE, MUTE_ALL, MUTE, NOT_LISTENING, OPEN, OPTIONS,
                PASTE, SAVE, SELECT_ALL, SELECT, SELECT_CONNECTED, UNLOCK, UNLOCK_ALL, UNMUTE, UNMUTE_ALL, EXIT }) {
            LARGE_ICONS.put(key, createImageIcon(key, DESCRIPTIONS.get(key), TOOLBAR_ICON_SIZE));
            SMALL_ICONS.put(key, createImageIcon(key, DESCRIPTIONS.get(key), MENU_ICON_SIZE));
        }
        for (var key : new String[] { PAUSE, START, REWIND }) {
            HUGE_ICONS.put(key, createImageIcon(key, DESCRIPTIONS.get(key), 25));
        }
    }

    private Icons() {
        // no instantiation
    }

    public static ImageIcon getMenuIcon(String name) {
        return LARGE_ICONS.get(name);
    }

    public static ImageIcon getToolbarIcon(String name) {
        return SMALL_ICONS.get(name);
    }

    public static ImageIcon getButtonIcon(String name) {
        return HUGE_ICONS.get(name);
    }

    private static ImageIcon createImageIcon(String path, String description, int scale) {
        try (var resource = Icons.class.getResourceAsStream(String.format("/icons/%s.png", path))) {
            var image = ImageIO.read(resource);
            var resizedImage = image.getScaledInstance(scale, scale, Image.SCALE_SMOOTH);
            return new ImageIcon(resizedImage, description);
        } catch (IOException e) {
            LOGGER.atError().withThrowable(e).log("Unable to load image file {}", path);
            return null;
        }
    }

}
