package bml.actions;

import java.util.function.Supplier;

import bml.actions.internal.ModifierClicks;

/**
 * Modifier clicks on Bitwig's buttons, named in Bitwig's status bar hint ("SHIFT+CLICK Arm
 * exclusively"), as Bitwig's own buttons do. A mod calls these from its own code, usually a hook on
 * the code that builds the button; no registration is needed.
 */
public final class Clicks {
    private Clicks() {
    }

    /** The modifier keys held for a click. {@link #SHORTCUT} is Ctrl on Windows and Linux, Cmd on macOS. */
    public enum Modifier {
        SHIFT, SHORTCUT, ALT, SHIFT_SHORTCUT, SHIFT_ALT, SHORTCUT_ALT
    }

    /** What a click does: its label in the status bar hint and what it runs. Made with {@link #click}. */
    public interface Click {
        /** The label the status bar hint shows after the modifier, e.g. "Arm exclusively". */
        String label();
    }

    /**
     * A click: {@code label} is shown in the status bar hint, {@code run} runs on Bitwig's UI thread
     * when the user clicks. {@code clickId} names it for Bitwig (it is never shown, and never
     * registered as an action, so it has no shortcut and isn't in the Commander); prefix it with the
     * mod ID. Make each click once and reuse it: a supplier given to {@link #onModifierClick} should
     * only choose between clicks it already has.
     */
    public static Click click(String clickId, String label, Runnable run) {
        return ModifierClicks.click(clickId, label, run);
    }

    /**
     * Gives a Bitwig button a click with {@code modifier} held. {@code current} is asked each time
     * Bitwig rebuilds the status bar hint (on every mouse move over the button) and when the user
     * clicks, so the hint and the action follow any setting it reads. It returns a click made with
     * {@link #click}, or {@code null} for none: then the hint shows no entry for that modifier and
     * the click does nothing. Keep it to a field read.
     *
     * <p>{@code widget} is one of Bitwig's buttons (a {@code bitwig.obf} widget that takes clicks,
     * such as a toggle button). A second call for the same button and modifier replaces the first.
     *
     * @throws IllegalArgumentException if {@code widget} isn't a Bitwig button
     */
    public static void onModifierClick(Object widget, Modifier modifier, Supplier<? extends Click> current) {
        ModifierClicks.register(widget, modifier, current);
    }

    /**
     * Sets the status bar hint for a plain click on a Bitwig widget (shown after "CLICK"), for a
     * widget whose plain click has no hint of its own.
     *
     * @throws IllegalArgumentException if {@code widget} isn't a Bitwig widget
     */
    public static void hint(Object widget, String text) {
        ModifierClicks.hint(widget, text);
    }

    /**
     * Sets the status bar hint for a plain click on a Bitwig button from {@code text}, which is
     * asked each time Bitwig rebuilds the hint (on every mouse move over the button), so it can
     * follow a setting. The click itself stays Bitwig's own: only its hint changes. {@code text}
     * returning {@code null} shows Bitwig's own hint. Keep it to a field read.
     *
     * @throws IllegalArgumentException if {@code widget} isn't a Bitwig button
     */
    public static void hint(Object widget, Supplier<String> text) {
        ModifierClicks.hint(widget, text);
    }
}
