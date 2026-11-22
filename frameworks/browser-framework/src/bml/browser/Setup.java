package bml.browser;

/**
 * One side browser as its build starts ({@link View#setup}): what the view's setup gets, before the
 * side browser reads its session for the first time. Call on Bitwig's UI thread.
 */
public interface Setup {
    /** The view's ID, as declared. */
    String viewId();

    /** Bitwig's side browser being built (a {@code cFV}, as the mod kit names it). */
    Object browser();

    /** The document the side browser belongs to (a {@code pHd}, as the mod kit names it). */
    Object document();

    /** The side browser's search session (a {@code Jp}): its source, search and filters. */
    Object session();

    /** True until the side browser is disposed (its window is rebuilt or closed). */
    boolean live();

    /**
     * Runs {@code action} when the side browser is disposed; at once if it already is. An action that
     * throws is logged.
     */
    void onDispose(Runnable action);
}
