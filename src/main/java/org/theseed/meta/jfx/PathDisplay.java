/**
 *
 */
package org.theseed.meta.jfx;

import org.theseed.jfx.ResizableController;

/**
 * @author Bruce Parrello
 *
 */
public class PathDisplay extends ResizableController {

    public PathDisplay() {
        super(200, 200, 1000, 600);
    }

    @Override
    public String getIconName() {
        return "fig-gear-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "Metabolic Path Display";
    }

    // TODO PathDisplay window

}
