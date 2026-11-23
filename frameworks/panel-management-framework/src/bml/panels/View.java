package bml.panels;

import java.util.function.Function;

/** A declared view. */
public interface View {
    /**
     * Its icon (required): a resource ID from the mod's manifest, either one of Bitwig's icons
     * ({@code "kind": "bitwig"}, a path under {@code icons/}, checked to exist) or the mod's own SVG
     * ({@code "kind": "file"}). Bitwig tints it with the theme: only its shape counts. A view whose
     * icon can't be used is logged and left out.
     */
    View icon(String resourceId);

    /**
     * Its content (required): called each time Bitwig builds a window's panel for the view, once
     * for every place the view is in, returning one of Bitwig's widgets ({@code dve}). The framework
     * puts it in Bitwig's scaled container (Panel Scale) inside Bitwig's panel widget.
     */
    View content(Function<ViewContext, Object> content);

    /**
     * A saved text value of the view's own, declared as Bitwig's panels declare theirs: kept with
     * the project's view settings (per display profile), never in the project. One value per
     * profile, shared by every place the view is in; its full key is
     * {@link ViewContext#savedKey}{@code (key)}.
     */
    View savedText(String key, String defaultValue);

    /**
     * One of Bitwig's panels the view can take the place of: {@code "mixer"}. In a mode whose main
     * content is that panel (MIX's mixer), the main content becomes a choice between the panel and
     * the view (both buttons in the bottom bar's row for that mode, one shown at a time, the panel
     * by default); there the view is not also a tab of the lower panel. Other modes keep it as a
     * lower-panel tab. Without a view stack only (in a stack every view has its own place).
     */
    View replaces(String bitwigPanel);

    /**
     * A place for the view outside the main area: {@code "browser"}, the browser side (the tab
     * group of the Browser, Project, Studio I/O and Mappings panels). The view becomes one more tab
     * of each window's browser side, after Bitwig's own tabs, its button after theirs in the bottom
     * bar and working as theirs do; it is then neither a tab of the modes' lower panels nor a view of
     * a stack. Where a layout mod has made the browser side's panels side columns, the view is one
     * more column, after theirs (towards the main area), hidden by default. A window without a
     * browser side doesn't show the view (logged once per profile). Without a side (the default) the
     * view goes in the main area.
     */
    View side(String place);

    /**
     * With {@link #side}: true makes the view a full-height column of its own between the main area
     * and the browser side, instead of one of its tabs: a side column (Layout#sideColumns) with its
     * own width (saved with the project), shown by default, its button in Bitwig's order with the
     * other side panels' buttons and the view buttons' clicks (click shows it alone, again hides it;
     * Cmd/Ctrl-click adds or removes it). Where a layout mod has made side columns of the browser
     * side's panels, it is one more of them, after theirs. It stays beside the browser side when the
     * side is moved (Panel: Choose Sides). Default false.
     */
    View sideColumn(boolean column);

    /** Whether it has a scale: Panel Scale setting and right-click scale choices (default true). */
    View scale(boolean scalable);

    /**
     * Whether the framework lists its scale in Settings > User Interface > Panel Scale (default
     * true). Pass false to place the setting where the mod wants it, with {@link PanelScale}.
     */
    View scaleSetting(boolean listed);
}
