package bml.browser;

/**
 * A view's setup for each side browser ({@link View#setup}). Called on Bitwig's UI thread at the
 * start of each side browser's build with the view active, before the side browser reads its
 * session for the first time (Bitwig builds the results list from it next).
 */
@FunctionalInterface
public interface SetupFactory {
    /**
     * Returns the view's handle for this side browser, any object (or null), which the side
     * browser's slots and panel then give back as {@code handle()}. Throwing makes the view's claims
     * lapse (logged) and the side browser is built as Bitwig builds it.
     */
    Object create(Setup setup);
}
