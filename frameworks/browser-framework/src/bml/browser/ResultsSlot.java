package bml.browser;

/**
 * One side browser's results slot ({@link View#results}): what a view gets to place the results
 * column. Call on Bitwig's UI thread.
 */
public interface ResultsSlot {
    /** The view's ID, as declared. */
    String viewId();

    /** Bitwig's side browser being built (a {@code cFV}, as the mod kit names it). */
    Object browser();

    /** The side browser's search session (a {@code Jp}): its source, search and filters. */
    Object session();

    /** The document the side browser belongs to (a {@code pHd}). */
    Object document();

    /** What the view's {@link View#setup setup} returned for this side browser, or null. */
    Object handle();

    /** True until the side browser is disposed (its window is rebuilt or closed). */
    boolean live();

    /** Runs {@code action} when the side browser is disposed; at once if it already is. */
    void onDispose(Runnable action);

    /**
     * Bitwig's results column (a {@code dve}), to place in the widget the factory returns. It holds
     * the results list and, under it, the details pane (or what the view's details slot returned).
     */
    Object results();

    /**
     * Calls {@code listener} after any of the session's filters gets or loses a value, or is
     * offered or no longer offered for the source, for as long as the side browser lives.
     */
    void onFiltersChanged(Runnable listener);

    /**
     * The view can't run in this browser: its claims lapse and the browser is rebuilt without its
     * slot (logged with {@code reason}).
     */
    void fail(String reason);
}
