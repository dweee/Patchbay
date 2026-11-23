package bml.api;

/** Where a {@link Hook} runs in its target method. */
public enum At {
    /** Before the first instruction. */
    ENTRY,
    /** Just before the method's final return; with {@link Hook#allReturns()}, before every return. */
    BEFORE_RETURN,
    /**
     * Receives the value the method is about to return at its final return and returns the value to
     * use instead; with {@link Hook#allReturns()}, at every return.
     */
    WRAP_RETURN,
    /** Instead of one call inside the method (see {@link Hook#call()}). */
    REPLACE_CALL,
    /** A new override in the target class: Bitwig's inherited method runs, then the hook. */
    OVERRIDE_AFTER_SUPER,
    /** A new override in the target class: the hook runs, then Bitwig's inherited method. */
    OVERRIDE_BEFORE_SUPER,
    /** A new override in the target class: only the hook runs. */
    OVERRIDE_INSTEAD
}
