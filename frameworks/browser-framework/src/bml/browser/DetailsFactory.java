package bml.browser;

/**
 * Customises or wraps the side browser's details pane for a view ({@link View#details}). Called on
 * Bitwig's UI thread while the side browser lays out its rows, once per side browser built with the
 * view active, before its results slot.
 */
@FunctionalInterface
public interface DetailsFactory {
    /**
     * Returns what Bitwig puts under the results in place of the details pane: one of Bitwig's widgets
     * not yet in any container, normally {@link DetailsSlot#details()} itself or a widget holding it.
     * The Show details pane feature shows and hides what is returned. Throwing, or returning anything
     * else, makes the view's claims lapse (logged): the details pane is taken out of any container the
     * view put it in and placed as Bitwig places it, and the browser is rebuilt.
     */
    Object create(DetailsSlot slot);
}
