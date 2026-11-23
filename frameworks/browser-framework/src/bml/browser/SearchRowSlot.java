package bml.browser;

/**
 * One side browser's search-row slot ({@link View#searchRow}): what a view gets to place the search
 * field. The side browser builds its search row after its results column, so a widget the view
 * returned from its {@link View#results results slot} is already in place. Call on Bitwig's UI
 * thread.
 */
public interface SearchRowSlot {
    /** The view's ID, as declared. */
    String viewId();

    /** Bitwig's side browser being built (a {@code cFV}, as the mod kit names it). */
    Object browser();

    /** The document the side browser belongs to (a {@code pHd}). */
    Object document();

    /** The side browser's search session (a {@code Jp}). */
    Object session();

    /** What the view's {@link View#setup setup} returned for this side browser, or null. */
    Object handle();

    /** True until the side browser is disposed. */
    boolean live();

    /** Runs {@code action} when the side browser is disposed; at once if it already is. */
    void onDispose(Runnable action);

    /**
     * Bitwig's search field (a {@code dve}: the field with its own padding, as the side browser
     * builds it), not yet in any container. The view may put it in a widget of its own inside this
     * side browser, such as the one its results slot returned; keys and focus requests reach it there
     * as in its own row. Don't call {@link #row()} as well.
     */
    Object field();

    /**
     * The search row as the framework would place it: the field, or with
     * {@link Feature#ALL_SOURCES_BUTTON} the All Sources button and the field side by side. Built on
     * the first call, with the field in it.
     */
    Object row();

    /** The view can't run in this browser: its claims lapse and the browser is rebuilt (logged with {@code reason}). */
    void fail(String reason);
}
