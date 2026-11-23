package bml.browser.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import bitwig.obf.Fq; // Bitwig's UI-thread executor
import bitwig.obf.Jp; // a browser session
import bitwig.obf.cFV; // the side browser
import bitwig.obf.dve; // a widget
import bitwig.obf.dyC; // a splitter's child
import bml.browser.DetailsSlot;
import bml.browser.ResultsSlot;
import bml.browser.SearchRowSlot;

/**
 * A view's slots in the side browser's build. Results ({@code View.results}): where the side browser
 * adds its results column under the filter area, the active view's factory gets the column and
 * returns the widget shown in its place. Details ({@code View.details}): before the details pane goes
 * under the results list, the factory gets it and returns what goes there. Search row
 * ({@code View.searchRow}): after the results slot, the factory gets the search field and returns
 * what goes in the search row, or nothing. Mechanism only: the framework draws nothing of its own.
 * The slots of one side browser share its instance ({@link Instances}): document, handle, lifetime.
 *
 * <p>Safety: a launch marker is stored before the factory runs and cleared once its widget is on
 * screen 3 s later. A launch finding the marker (the last one crashed, hung or drew nothing) doesn't
 * call the factory and sets Browser view back to Standard. Any failure gives the column alone.
 */
final class Slots {
    /** The marker's key: the same as the side pane's before 0.3, so a marker left by it still counts. */
    private static final String PENDING = "pane.pending";
    private static final long CONFIRM_MS = 3000;
    private static final Timer TIMER = new Timer("bml-browser-slot", true);
    /** Whether this launch skips the slot, decided on first use. */
    private static Boolean skip;
    /** Every slot built, for its filter listeners. */
    private static final List<SlotImpl> LIVE = new ArrayList<>();

    private Slots() {
    }

    /** What every slot of one side browser gives: the view, the browser and its instance. */
    private abstract static class SlotBase {
        final Views.Declared view;
        final WeakReference<cFV> browser;
        final WeakReference<Jp> session;
        /** The side browser's instance, or null when it couldn't be made (logged). */
        final Instances.Instance instance;

        SlotBase(Views.Declared view, cFV browser, Jp session) {
            this.view = view;
            this.browser = new WeakReference<>(browser);
            this.session = new WeakReference<>(session);
            Instances.Instance found = Instances.building();
            this.instance = found != null && found.view == view ? found : null;
        }

        public String viewId() {
            return view.id;
        }

        public Object browser() {
            return browser.get();
        }

        public Object session() {
            return session.get();
        }

        public Object document() {
            if (instance != null) return instance.document();
            cFV b = browser.get();
            return b == null ? null : b.gfw(); // the side browser's project
        }

        public Object handle() {
            return instance == null ? null : instance.handle();
        }

        public boolean live() {
            return instance != null ? instance.live() : browser.get() != null;
        }

        public void onDispose(Runnable action) {
            if (instance != null) instance.onDispose(action);
            else State.LOG.log("view " + view.key() + ": onDispose: the side browser has no instance; ignored");
        }

        public void fail(String reason) {
            if (view.active()) Views.fail(reason);
        }
    }

    /** One side browser's results slot. */
    private static final class SlotImpl extends SlotBase implements ResultsSlot {
        final dve results;
        final List<Runnable> filterListeners = new ArrayList<>();

        SlotImpl(Views.Declared view, cFV browser, Jp session, dve results) {
            super(view, browser, session);
            this.results = results;
        }

        @Override
        public Object results() {
            return results;
        }

        @Override
        public void onFiltersChanged(Runnable listener) {
            if (listener == null) return;
            synchronized (filterListeners) {
                filterListeners.add(listener);
            }
        }

        @Override
        public void fail(String reason) {
            super.fail(reason == null ? "its results slot failed" : reason);
        }

        void filtersChanged() {
            List<Runnable> listeners;
            synchronized (filterListeners) {
                listeners = List.copyOf(filterListeners);
            }
            for (Runnable listener : listeners) {
                try {
                    listener.run();
                } catch (Throwable t) {
                    State.LOG.log("view " + view.key() + ": a filter listener failed", t);
                }
            }
        }
    }

    /** The widget the side browser adds as its results column: the column itself, or the active view's widget. */
    static dyC results(dyC column) {
        Views.Declared view;
        try {
            State.get(); // the providers have declared
            view = Views.activeResults();
        } catch (Throwable t) {
            State.LOG.log("reading the active view failed; the results are shown alone", t);
            return column;
        }
        if (view == null || skipped()) return column;
        cFV browser = SideBrowsers.building();
        Jp session = browser == null ? null : browser.EUG();
        if (!(column instanceof dve results) || session == null) {
            Views.fail("the side browser can't take its results slot");
            return column;
        }
        State.writeStringNow(PENDING, String.valueOf(System.currentTimeMillis()));
        SlotImpl slot = new SlotImpl(view, browser, session, results);
        Object widget;
        try {
            widget = view.results.create(slot);
        } catch (Throwable t) {
            State.writeString(PENDING, "");
            State.LOG.log("view " + view.key() + ": its results slot failed; the results are shown alone", t);
            Views.fail("its results slot could not be built");
            return column;
        }
        if (!(widget instanceof dve placed)) {
            State.writeString(PENDING, "");
            Views.fail("its results factory returned " + (widget == null ? "null" : widget.getClass().getName()) + ", not a widget");
            return column;
        }
        synchronized (LIVE) {
            LIVE.add(slot);
        }
        confirmLater(view, placed);
        State.LOG.log("view " + view.key() + ": results slot built");
        return placed;
    }

    /** One side browser's details slot. */
    private static final class DetailsImpl extends SlotBase implements DetailsSlot {
        final dve details;

        DetailsImpl(Views.Declared view, cFV browser, Jp session, dve details) {
            super(view, browser, session);
            this.details = details;
        }

        @Override
        public Object details() {
            return details;
        }

        @Override
        public void fail(String reason) {
            super.fail(reason == null ? "its details slot failed" : reason);
        }
    }

    /**
     * What the side browser puts under its results list as the details pane: the pane, or what the
     * active view's details slot returns. Any failure gives the pane, out of any container the view
     * put it in.
     */
    static dve details(cFV browser, dve details) {
        Views.Declared view;
        try {
            State.get(); // the providers have declared
            view = Views.activeDetails();
        } catch (Throwable t) {
            State.LOG.log("reading the active view failed; the details pane is placed as Bitwig's", t);
            return details;
        }
        if (view == null || details == null || browser == null) return details;
        DetailsImpl slot = new DetailsImpl(view, browser, browser.EUG(), details);
        Object widget;
        try {
            widget = view.details.create(slot);
        } catch (Throwable t) {
            State.LOG.log("view " + view.key() + ": its details slot failed; the details pane is placed as Bitwig's", t);
            Views.fail("its details slot could not be built");
            return released(details);
        }
        String refused = refusal(widget);
        if (refused != null) {
            Views.fail("its details factory returned " + refused);
            return released(details);
        }
        State.LOG.log("view " + view.key() + ": details slot built");
        return (dve) widget;
    }

    /** One side browser's search-row slot. */
    private static final class SearchRowImpl extends SlotBase implements SearchRowSlot {
        final dve field;
        /** The row the framework built with the field ({@link #row()}), or null. */
        dve row;

        SearchRowImpl(Views.Declared view, cFV browser, Jp session, dve field) {
            super(view, browser, session);
            this.field = field;
        }

        @Override
        public Object field() {
            return field;
        }

        @Override
        public Object row() {
            if (row == null) row = SideBrowsers.searchRow(field);
            return row;
        }

        @Override
        public void fail(String reason) {
            super.fail(reason == null ? "its search-row slot failed" : reason);
        }
    }

    /**
     * What the side browser pads as its search row: the field (or the framework's row with the All
     * Sources button), what the active view's search-row slot returns, or null for an empty row. Any
     * failure gives the framework's row, with the field out of any container the view put it in.
     */
    static dve searchRow(dve field) {
        Views.Declared view;
        try {
            State.get(); // the providers have declared
            view = Views.activeSearchRow();
        } catch (Throwable t) {
            State.LOG.log("reading the active view failed; the search field stays in its row", t);
            return SideBrowsers.searchRow(field);
        }
        cFV browser = SideBrowsers.building();
        if (view == null || field == null || browser == null) return SideBrowsers.searchRow(field);
        SearchRowImpl slot = new SearchRowImpl(view, browser, browser.EUG(), field);
        Object widget;
        try {
            widget = view.searchRow.create(slot);
        } catch (Throwable t) {
            State.LOG.log("view " + view.key() + ": its search-row slot failed; the search field stays in its row", t);
            Views.fail("its search-row slot could not be built");
            return fallbackRow(slot);
        }
        if (widget == null) {
            State.LOG.log("view " + view.key() + ": search-row slot built (the row is left empty)");
            return null;
        }
        String refused = refusal(widget);
        if (refused != null) {
            Views.fail("its search-row factory returned " + refused);
            return fallbackRow(slot);
        }
        State.LOG.log("view " + view.key() + ": search-row slot built");
        return (dve) widget;
    }

    /**
     * After a failed search-row slot: the framework's row with the field in it, out of any container
     * the view put either in. The row the view asked for is kept when the field is still in it.
     */
    private static dve fallbackRow(SearchRowImpl slot) {
        dve field = slot.field;
        dve row = slot.row;
        try {
            if (row != null && row != field && field.RbC() == row) return released(row);
        } catch (Throwable t) {
            State.LOG.log("checking the framework's search row failed", t);
        }
        return SideBrowsers.searchRow(released(field));
    }

    /** Why {@code widget} can't be placed (null when it can): not a widget, or already in a container. */
    private static String refusal(Object widget) {
        if (!(widget instanceof dve placed)) return widget == null ? "null" : widget.getClass().getName() + ", not a widget";
        if (placed.RbC() != null) return "a widget that is already in a container";
        return null;
    }

    /** {@code widget} out of any container a view put it in, so Bitwig can place it. */
    private static dve released(dve widget) {
        try {
            dve parent = widget.RbC(); // the container it is in
            if (parent != null) parent.MJ(widget); // taken out
        } catch (Throwable t) {
            State.LOG.log("taking a widget out of a view's container failed", t);
        }
        return widget;
    }

    /** A filter of {@code session} got or lost a value, or was offered or not: the slots of that session hear. */
    static void filtersChanged(Jp session) {
        List<SlotImpl> slots = new ArrayList<>();
        synchronized (LIVE) {
            for (Iterator<SlotImpl> it = LIVE.iterator(); it.hasNext(); ) {
                SlotImpl slot = it.next();
                if (slot.browser.get() == null) it.remove();
                else if (slot.session.get() == session) slots.add(slot);
            }
        }
        for (SlotImpl slot : slots) slot.filtersChanged();
    }

    /** A side browser is disposed: its slot is let go. */
    static void disposed(cFV browser) {
        synchronized (LIVE) {
            LIVE.removeIf(slot -> {
                cFV b = slot.browser.get();
                return b == null || b == browser;
            });
        }
    }

    /** True when the last launch's results slot never confirmed (the active view has then failed). */
    static boolean launchSkipped() {
        return skipped();
    }

    /** True when the last launch's slot never confirmed: Browser view goes back to Standard. */
    private static boolean skipped() {
        if (skip == null) {
            skip = !State.readString(PENDING).isEmpty();
            if (skip) {
                // The last launch built a slot and never confirmed it (it crashed, hung, or nothing drew):
                // back to Standard, so the view can't take Bitwig down again until it is chosen again.
                State.writeString(PENDING, "");
                State.writeString("view", "");
                State.LOG.log("the last launch's results slot never confirmed; Browser view is set back to Standard");
                Views.fail("its results slot didn't confirm in the last launch");
            }
        }
        return skip;
    }

    /** After {@link #CONFIRM_MS}, on the UI thread: the widget on screen clears the marker; otherwise the view fails. */
    private static void confirmLater(Views.Declared view, dve placed) {
        WeakReference<dve> ref = new WeakReference<>(placed);
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                Fq.cfM(() -> {
                    dve widget = ref.get();
                    if (widget == null) return;
                    try {
                        if (widget.uev()) {
                            State.writeString(PENDING, "");
                            State.LOG.log("view " + view.key() + ": results slot drew");
                            return;
                        }
                        // the marker stays: the next launch sets Browser view back to Standard
                        State.LOG.log("view " + view.key() + ": results slot not on screen");
                        if (view.active()) Views.fail("its results slot drew nothing");
                    } catch (Throwable t) {
                        State.LOG.log("checking the results slot failed", t);
                    }
                });
            }
        }, CONFIRM_MS);
    }
}
