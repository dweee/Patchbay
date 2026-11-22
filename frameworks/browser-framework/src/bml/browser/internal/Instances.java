package bml.browser.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bitwig.flt.document.core.iface.pHd; // a document (a project)

import bitwig.obf.Jp; // a browser session
import bitwig.obf.cFV; // the side browser
import bml.browser.Setup;

/**
 * One side browser built with a view active, from the start of its build to its disposal: its
 * document, its session, the view's handle for it ({@code View.setup}) and the view's disposal
 * actions. The view's slots and panel for that side browser reach it, so they share the handle and
 * the lifetime. Bitwig's objects are held weakly; a side browser that goes without being disposed is
 * dropped (and its disposal actions run) the next time the list is read.
 */
final class Instances {
    /** One side browser's instance. */
    static final class Instance implements Setup {
        final Views.Declared view;
        private final WeakReference<cFV> browser;
        private final WeakReference<pHd> document;
        private final WeakReference<Jp> session;
        private final List<Runnable> onDispose = new ArrayList<>();
        private volatile Object handle;
        private volatile boolean live = true;

        Instance(Views.Declared view, cFV browser, pHd document, Jp session) {
            this.view = view;
            this.browser = new WeakReference<>(browser);
            this.document = new WeakReference<>(document);
            this.session = new WeakReference<>(session);
        }

        @Override
        public String viewId() {
            return view.id;
        }

        @Override
        public Object browser() {
            return browser.get();
        }

        cFV browserObject() {
            return browser.get();
        }

        @Override
        public Object document() {
            return document.get();
        }

        @Override
        public Object session() {
            return session.get();
        }

        Object handle() {
            return handle;
        }

        @Override
        public boolean live() {
            return live;
        }

        @Override
        public void onDispose(Runnable action) {
            if (action == null) return;
            synchronized (onDispose) {
                if (live) {
                    onDispose.add(action);
                    return;
                }
            }
            run(action);
        }

        /** The side browser is disposed: the view's actions run once. */
        void dispose() {
            List<Runnable> actions;
            synchronized (onDispose) {
                if (!live) return;
                live = false;
                actions = List.copyOf(onDispose);
                onDispose.clear();
            }
            for (Runnable action : actions) run(action);
            handle = null;
        }

        private void run(Runnable action) {
            try {
                action.run();
            } catch (Throwable t) {
                State.LOG.log("view " + view.key() + ": a disposal action failed", t);
            }
        }
    }

    /** The instances of the side browsers still alive, in the order they were built. */
    private static final List<Instance> LIVE = new ArrayList<>();
    /** The instance of the side browser being built on this thread (it builds on the UI thread), or null. */
    private static final ThreadLocal<Instance> BUILDING = new ThreadLocal<>();

    private Instances() {
    }

    /**
     * A side browser starts its build: with a working view active that uses the side browser's build
     * (setup, slots or content), its instance, and the view's setup. A setup that throws makes the
     * view fail, so the build goes on as Bitwig's.
     */
    static void starts(cFV browser, pHd document, Jp session) {
        BUILDING.remove();
        disposed(browser); // the same side browser built again: the earlier build's instance goes
        State.get(); // the providers have declared
        Views.Declared view = Views.activeBuilt();
        if (view == null) return;
        if (view.results != null && Slots.launchSkipped()) return; // the view failed: the last launch's slot never showed
        Instance instance = new Instance(view, browser, document, session);
        synchronized (LIVE) {
            LIVE.add(instance);
        }
        BUILDING.set(instance);
        if (view.setup == null) return;
        try {
            instance.handle = view.setup.create(instance);
            State.LOG.log("view " + view.key() + ": set up a side browser");
        } catch (Throwable t) {
            State.LOG.log("view " + view.key() + ": its setup failed; the side browser is built as Bitwig's", t);
            Views.fail("its setup failed");
        }
    }

    /** The instance of the side browser being built on this thread, or null. */
    static Instance building() {
        Instance instance = BUILDING.get();
        return instance != null && instance.browserObject() == SideBrowsers.building() ? instance : null;
    }

    /** The side browser is built. */
    static void built() {
        BUILDING.remove();
    }

    /** The live instance of {@code browser}, or null. */
    static Instance of(Object browser) {
        if (browser == null) return null;
        Instance found = null;
        for (Instance instance : live()) {
            if (instance.browserObject() == browser) found = instance;
        }
        return found;
    }

    /** A side browser is disposed: its instance too. */
    static void disposed(cFV browser) {
        List<Instance> gone = new ArrayList<>();
        synchronized (LIVE) {
            for (Iterator<Instance> it = LIVE.iterator(); it.hasNext(); ) {
                Instance instance = it.next();
                if (instance.browserObject() == browser) {
                    it.remove();
                    gone.add(instance);
                }
            }
        }
        for (Instance instance : gone) instance.dispose();
    }

    /** The live instances; drops (and disposes) those whose side browser has gone. */
    private static List<Instance> live() {
        List<Instance> gone = new ArrayList<>();
        List<Instance> live;
        synchronized (LIVE) {
            for (Iterator<Instance> it = LIVE.iterator(); it.hasNext(); ) {
                Instance instance = it.next();
                if (instance.browserObject() == null) {
                    it.remove();
                    gone.add(instance);
                }
            }
            live = List.copyOf(LIVE);
        }
        for (Instance instance : gone) instance.dispose();
        return live;
    }
}
