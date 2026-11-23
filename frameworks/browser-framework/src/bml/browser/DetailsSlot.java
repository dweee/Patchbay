package bml.browser;

/**
 * One side browser's details slot ({@link View#details}): what a view gets to customise or wrap the
 * details pane before Bitwig puts it under the results. The details slot comes before the results
 * slot, and the column the results slot hands over holds what the view returned. Call on Bitwig's UI
 * thread.
 */
public interface DetailsSlot {
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
     * Bitwig's details pane (a {@code dve}: what Bitwig shows of the selected item under the
     * results), not yet in any container.
     */
    Object details();

    /** The view can't run in this browser: its claims lapse and the browser is rebuilt (logged with {@code reason}). */
    void fail(String reason);
}
