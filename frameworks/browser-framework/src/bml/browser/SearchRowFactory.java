package bml.browser;

/**
 * Places the side browser's search field for a view ({@link View#searchRow}). Called on Bitwig's UI
 * thread while the side browser lays out its rows, once per side browser built with the view active,
 * after its results slot.
 */
@FunctionalInterface
public interface SearchRowFactory {
    /**
     * Returns what goes in the side browser's search row, padded as Bitwig pads its search field: one
     * of Bitwig's widgets not yet in any container, normally {@link SearchRowSlot#row()}, the field,
     * or a widget holding it; or null to leave the row empty (it is hidden, and the layout closes the
     * gap), for a view that placed the field in a widget of its own. Throwing, or returning anything
     * else, makes the view's claims lapse (logged): the field is taken out of any container the view
     * put it in and shown in its row, and the browser is rebuilt.
     */
    Object create(SearchRowSlot slot);
}
