package bml.panels;

import java.util.ArrayList;
import java.util.List;

import bitwig.obf.aBW; // a window's project view
import bitwig.obf.ajF; // a tab group (several panels, one shown)
import bitwig.obf.akC; // a display profile: its windows and their layouts
import bitwig.obf.akn; // a mode (ARRANGE, MIX, EDIT)
import bitwig.obf.akq; // a main area: one layout per mode
import bitwig.obf.akw; // a part of a window's layout
import bitwig.obf.akz; // one mode's layout in a main area
import bitwig.obf.ala; // a splitter between two parts of a layout

/**
 * Lookups for a mod that arranges its own views itself: a new panel node of one of its views, to
 * put into a layout it builds; the nodes its view has in a window now; and the icon name of one of
 * its SVG resources, for buttons it builds. Bitwig objects come as {@code Object}.
 */
public final class Views {
    private Views() {
    }

    /**
     * A new panel node ({@code akQ}) of the view {@code viewId} that the mod {@code modId} declared
     * ({@link Panels#view}), to put where Bitwig would put a panel in a layout the mod builds: for
     * example a mode's layout of its own, or a window of a display profile it builds. Every call
     * gives a new node (a part of a layout has one parent, so each place needs its own); each is a
     * plain panel, as the view is as a tab: it has no "is shown" of its own and shows wherever it is
     * placed. Each node gets the view's content (built for each window that shows it), icon, label,
     * button and scale, and the view's saved values (one value per display profile, shared by all
     * its nodes there). Its button's click shows it in the tab group holding it, as the framework's
     * tabs do; elsewhere the mod decides how it is shown. The Show action doesn't know about it.
     * Call it while the profile is built (from a layout policy or the mod's own hook on profile
     * building). Null (logged) when the mod declared no such view, or it was left out.
     */
    public static Object newNode(String modId, String viewId) {
        Registry.Declared view = Registry.find(modId, viewId);
        if (view == null || view.bitwig) {
            Registry.LOG.log("newNode: mod " + modId + " has no view " + viewId);
            return null;
        }
        ViewNode node = new ViewNode();
        node.init(view, false);
        return node;
    }

    /**
     * The panel nodes ({@code akQ}) the view {@code viewId} of the mod {@code modId} has in a window
     * ({@code aBW}, a window's project view) now: those outside its main areas (the browser side, side
     * columns, a view stack) and those in the window's current mode, whether the framework placed
     * them or the mod did ({@link #newNode}). A node is a window's own: each window, and each mode,
     * has its own. In layout order (each splitter's first part, left or top, before its second), so
     * a mod that put the view in several places tells them apart by where they are (their parents:
     * {@code uva()}). Empty when the view isn't in the window's layout now.
     */
    public static List<Object> nodes(Object window, String modId, String viewId) {
        List<Object> found = new ArrayList<>();
        Registry.Declared view = Registry.find(modId, viewId);
        if (view == null || view.bitwig || !(window instanceof aBW w)) return found;
        try {
            akC profile = w.puu();              // the window's display profile
            akn mode = w.RDJ().vpw();           // the current mode (from the window's view settings)
            collect(profile.cfM(w.sc()).TMF(), view, mode, found); // this window's layout
        } catch (Throwable failure) {
            Registry.LOG.log("could not find the nodes of " + view.who() + " in a window", failure);
        }
        return found;
    }

    private static void collect(Object part, Registry.Declared view, akn mode, List<Object> found) {
        if (part instanceof ViewNode node) {
            if (node.view() == view) found.add(node);
        } else if (part instanceof ajF group) {
            for (Object tab : group.lYc()) collect(tab, view, mode, found); // its panels
        } else if (part instanceof akq main) {
            for (int c = 0; c < main.wGc(); c++) { // its modes' layouts: the current mode's
                akz layout = main.cfM(c);
                if (layout.cfM(mode)) collect(layout.TMF(), view, mode, found);
            }
        } else if (part instanceof ala split) {
            collect(split.E3(), view, mode, found);
            collect(split.wGc(), view, mode, found);
        }
    }

    /**
     * The icon name a window loads for the mod's icon resource {@code resourceId} (declared in its
     * mod.json, with the same rules as a view's icon, {@link View#icon}), for a button the mod builds
     * itself (as Bitwig's buttons take an icon by name). A {@code "kind": "bitwig"} resource gives
     * the name of that Bitwig icon; the mod's own SVG gives a name the framework loads from the mod's
     * file, rendered and tinted as Bitwig's own icons. Null (logged) when the resource can't be used.
     */
    public static String icon(String modId, String resourceId) {
        if (resourceId == null) return null;
        return Icons.name(modId, resourceId);
    }
}
