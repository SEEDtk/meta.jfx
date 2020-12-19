/**
 *
 */
package org.theseed.dl4j.jfx;

import java.util.List;

import org.theseed.reports.IValidationReport;

/**
 * This interface is required by the controller class used to display prediction results.
 *
 * @author Bruce Parrello
 */
public interface IDisplayController extends IValidationReport {

    /**
     * Initialize for display.
     *
     * @param labels	list of labels for the governing model
     */
    public void init(List<String> labels);

}
