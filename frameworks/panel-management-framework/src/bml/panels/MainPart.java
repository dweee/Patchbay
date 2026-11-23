package bml.panels;

import bitwig.obf.dXs; // the show/hide state of a collapsible part
import bitwig.obf.dXx; // a collapsible part: lays its content out at no less than the content's minimum, clipped
import bitwig.obf.dve; // a widget
import bitwig.obf.dyC; // a widget, as a container's child
import com.bitwig.base.geom.Jkg; // the side of a splitter a part is on
import com.bitwig.base.geom.tfK; // a widget size (width, height)

/**
 * The main area beside side columns (what the innermost column's splitter holds towards the main
 * area): Bitwig's main area in Bitwig's collapsible part, always shown and held, asking for no
 * minimum width. The columns' splitters may then make the main area as narrow as the room allows,
 * so a column always opens, never makes the window wider, and nothing is closed for want of room.
 * The main area's content still gets at least its own minimum width (Bitwig's part lays it out so,
 * from its left edge, as while a part slides) and is clipped by the narrower part. Its minimum
 * height is the content's.
 */
final class MainPart extends dXx {
    private dve content;

    private MainPart() {
    }

    static MainPart create(dve content) {
        MainPart part = new MainPart();
        dXs state = dXs.cfM(Jkg.MRY, null, false); // Bitwig's show/hide state; this side lays the content out from the left
        part.cfM(state);                            // as dXx.cfM(dve, Jkg) does
        part.H_();
        part.cfM((dyC) content);                    // its widget
        part.content = content;
        state.cfM(part);
        state.MRY = true;                           // held: shows at once
        part.MRY(true);                             // shown, always
        return part;
    }

    /** No minimum width; the content's minimum height (PYl()). */
    @Override
    public tfK mL_() {
        return new tfK(0, content != null ? content.PYl() : 0);
    }
}
