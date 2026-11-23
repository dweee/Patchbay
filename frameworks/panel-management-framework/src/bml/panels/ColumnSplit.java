package bml.panels;

import bitwig.obf.aBW; // a window's project view
import bitwig.obf.akZ; // a splitter between two parts of a window's layout
import bitwig.obf.dVx; // which side of a splitter keeps its size
import bitwig.obf.dWb; // a row of widgets (here: a group of the bottom bar)
import bitwig.obf.akq; // a main area (here only a stand-in)
import bitwig.obf.akw; // a part of a window's layout

/**
 * A left/right split of side columns: one column (a panel, keeping its own width) beside the rest
 * of the window towards the main area. The outermost column is Bitwig's own splitter; these are
 * nested inside it, one per further column. A layout part's saved values are keyed by its path of
 * names: every part of a column split is named flat under the outermost splitter, so the main
 * area keeps the path it has without the columns (nothing of it resets), and each column split is
 * named by its column ({@code <outer>.column_<panel>}), so its saved width stays with its panel.
 * A hidden column leaves no strip (view-splitter), in these and in Bitwig's outer splitter.
 */
final class ColumnSplit extends akZ {
    private final akZ outer;  // Bitwig's splitter holding the outermost column
    private final akw column; // its panel

    private ColumnSplit(akZ outer, akw column) {
        this.outer = outer;
        this.column = column;
    }

    /** The split for one column, with stand-ins as its parts until they are set. */
    static ColumnSplit create(akZ outer, akw column) {
        ColumnSplit split = new ColumnSplit(outer, column);
        split.MRY("column_" + column.tSS(), true, akq.cfM("panel-stand-in"), akq.cfM("panel-stand-in")); // name, left/right, two stand-ins
        split.H_(); // Bitwig's initialisation
        return split;
    }

    /** Its column's panel. */
    akw column() {
        return column;
    }

    /** Its other part: the rest of the window towards the main area. */
    akw core() {
        return E3() == column ? wGc() : E3();
    }

    /**
     * Puts its column on the given side (towards the window's edge) and the rest on the other,
     * with the column's side keeping its width: after a layout mod moved the edge to the other side
     * of the window (Panel: Choose Sides swaps the splitters it knows), every column follows. The
     * paths don't change (flat under the outer splitter), so widths and visibility stay. Nothing if
     * it is already so.
     */
    void settle(boolean columnFirst) {
        akw first = E3(), second = wGc();
        if ((first == column) == columnFirst) return;
        IwV(akq.cfM("panel-stand-in")); // the second part: a stand-in (a part has one parent)
        MRY(second);                     // the first part
        IwV(first);                      // the second part
        cfM(columnFirst ? dVx.cfM : dVx.MRY); // the column's side keeps its width
    }

    /**
     * The bottom bar's left group: Bitwig adds the buttons of the panels outside the main area by
     * walking the layout, first part then second, so the columns are put beside their edge first
     * (settle) and the buttons follow the columns' order on screen.
     */
    @Override
    public void cfM(dWb row, aBW window) {
        Layouts.settleAt(this);
        super.cfM(row, window);
    }

    /** The bottom bar's right group: as the left (cfM(dWb, aBW)). */
    @Override
    public void MRY(dWb row, aBW window) {
        Layouts.settleAt(this);
        super.MRY(row, window);
    }

    /** Its path (also the key of its saved divider position: the column's width). */
    @Override
    public String jG() {
        return outer.jG() + ".column_" + column.tSS();
    }

    /** Bitwig's path rule for its two parts: flat under the outermost splitter. */
    @Override
    public String cfM(akw child) {
        return outer.jG() + "." + child.tSS(); // tSS(): the part's name
    }
}
