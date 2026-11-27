package dev.litematicastats.gui;

/** A short message shown inside the stats list (chat is hidden behind the material list screen). */
public final class StatsFeedback {
    private static String text;
    private static long until;

    private StatsFeedback() {
    }

    public static void show(String message) {
        text = message;
        until = System.currentTimeMillis() + 4500L;
    }

    public static String current() {
        return text != null && System.currentTimeMillis() < until ? text : null;
    }
}
