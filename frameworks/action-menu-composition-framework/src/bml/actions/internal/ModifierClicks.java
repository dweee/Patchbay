package bml.actions.internal;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import bitwig.obf.Cal; // the description of what a click runs
import bitwig.obf.dmQ; // an action's binding: what Bitwig's own modifier clicks run
import bitwig.obf.dvW; // the base of every widget, which holds its status bar text
import bitwig.obf.dzc; // a modifier click that chooses its action each time it's asked (as the solo button's)
import bitwig.obf.dyU; // a click for one set of modifiers, with its status bar hint
import bitwig.obf.dzg; // a widget that takes clicks: buttons
import bitwig.obf.dzm; // Bitwig's default click on a button
import bitwig.obf.eoV; // a set of held modifier keys
import bml.actions.Clicks;
import com.bitwig.ramona.core.cuN; // a runnable document action: what a click runs

/**
 * Modifier clicks with status bar hints. Each click is a binding of a {@link ModAction} (never
 * registered) to the mod's Runnable; a button gets a {@code dzc} for the modifier, which Bitwig asks
 * for the binding to run and for the hint's label on every hover move.
 */
public final class ModifierClicks {
    private static final String ID = "[A-Za-z][A-Za-z0-9_.-]*";
    /** One Bitwig action per click ID: the binding's action, never registered. */
    private static final Map<String, ModAction> ACTIONS = new ConcurrentHashMap<>();
    /** Buttons with a plain-click hint of a mod's (weakly: widgets come and go). */
    private static final Map<dzg, Supplier<String>> PLAIN = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile boolean anyPlain;
    private static volatile boolean failedHint;
    /** The button whose default click hint is being built on this thread. */
    private static final ThreadLocal<dzg> HINTING = new ThreadLocal<>();

    private ModifierClicks() {
    }

    /** A click made by {@link #click}. */
    private record Made(String label, dmQ binding) implements Clicks.Click {
    }

    public static Clicks.Click click(String clickId, String label, Runnable run) {
        if (clickId == null || !clickId.matches(ID)) throw new IllegalArgumentException("invalid click ID: " + clickId);
        if (label == null || label.isBlank()) throw new IllegalArgumentException(clickId + " has no label");
        if (run == null) throw new IllegalArgumentException(clickId + " runs nothing");
        // The label is also the menu label, so Bitwig never looks a translation up for it (a click has no menu).
        ModAction action = ACTIONS.computeIfAbsent(clickId, id -> new ModAction(id, label, label, null));
        Runnable contained = () -> {
            try {
                run.run();
            } catch (Throwable t) {
                Registry.LOG.log("click " + clickId + " failed", t);
            }
        };
        return new Made(label, dmQ.cfM(action, contained)); // a binding that runs the Runnable
    }

    public static void register(Object widget, Clicks.Modifier modifier, Supplier<? extends Clicks.Click> current) {
        if (!(widget instanceof dzg button)) throw new IllegalArgumentException("not a Bitwig button: " + widget);
        if (modifier == null || current == null) throw new IllegalArgumentException("no modifier or no click supplier");
        button.cfM(keys(modifier), new Hinted(current)); // the left button with these modifiers; replaces an earlier one
    }

    public static void hint(Object widget, String text) {
        if (!(widget instanceof dvW w)) throw new IllegalArgumentException("not a Bitwig widget: " + widget);
        w.z3I(text == null ? "" : text); // the widget's status bar text
    }

    public static void hint(Object widget, Supplier<String> text) {
        if (!(widget instanceof dzg button)) throw new IllegalArgumentException("not a Bitwig button: " + widget);
        if (text == null) throw new IllegalArgumentException("no hint supplier");
        PLAIN.put(button, text); // replaces an earlier one
        anyPlain = true;
    }

    /**
     * A button's hint entry for one set of modifiers is being read: note the button when the entry's
     * click is Bitwig's default one (dzm) and the button has a plain-click hint of a mod's. Bitwig
     * uses the default for the plain click and for every modifier set with no click of its own, and
     * leaves out of the bar an entry whose text equals one with fewer modifiers; answering all of
     * them with the same text keeps the bar as Bitwig would.
     */
    public static void hintStarts(dzg button, dyU click) {
        if (!anyPlain) return;
        if (click instanceof dzm && button != null) HINTING.set(button);
        else HINTING.remove();
    }

    /** The entry's text: the mod's instead of Bitwig's default, with the button's shortcut, when noted. */
    public static String defaultHint(String own) {
        if (!anyPlain) return own;
        dzg button = HINTING.get();
        HINTING.remove();
        if (button == null) return own;
        Supplier<String> supplier = PLAIN.get(button);
        if (supplier == null) return own;
        try {
            String text = supplier.get();
            return text == null ? own : text + button.bB(); // bB: the button's own shortcut, or ""
        } catch (Throwable t) {
            if (!failedHint) Registry.LOG.log("a click hint's supplier failed; Bitwig's hint shown", t);
            failedHint = true;
            return own;
        }
    }

    private static eoV keys(Clicks.Modifier modifier) {
        return switch (modifier) {
            case SHIFT -> eoV.FzB;
            case SHORTCUT -> eoV.lYc; // Ctrl, Cmd on macOS
            case ALT -> eoV.hUR;
            case SHIFT_SHORTCUT -> eoV.fJR;
            case SHIFT_ALT -> eoV.eLY;
            case SHORTCUT_ALT -> eoV.iHb;
        };
    }

    /**
     * A modifier click that asks the mod each time: for the binding to run (and to describe) and for
     * the hint's label. No click: no hint entry, and the click runs nothing.
     */
    private static final class Hinted extends dzc {
        private final Supplier<? extends Clicks.Click> current;
        private boolean failed;

        Hinted(Supplier<? extends Clicks.Click> current) {
            this.current = current;
        }

        private Made made() {
            if (current == null) return null; // asked from Bitwig's constructor (with assertions on)
            try {
                return current.get() instanceof Made m ? m : null;
            } catch (Throwable t) {
                if (!failed) Registry.LOG.log("a modifier click's supplier failed; no click", t);
                failed = true;
                return null;
            }
        }

        /** What a click runs now, or none. */
        @Override
        public cuN cfM() {
            Made m = made();
            return m == null ? null : m.binding().BNq(); // the binding as Bitwig runs it
        }

        /** The hint's label: the click's, or none, which leaves the entry out. */
        @Override
        public String MRY() {
            Made m = made();
            return m == null ? null : m.label();
        }

        /** The description of what the click runs, as Bitwig's binding-backed modifier clicks give it. */
        @Override
        public Cal ar_() {
            Made m = made();
            return m == null ? super.ar_() : m.binding().ar_();
        }
    }
}
