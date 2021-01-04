/**
 *
 */
package org.theseed.jfx;

import java.util.Collection;
import java.util.Iterator;

import javafx.scene.Node;
import javafx.scene.control.TitledPane;

/**
 * This is the base class for column analysis.  It runs through a specific column from a list of string arrays
 * and produces a visual display of some sort, which is then passed back in a titled pane.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ColumnAnalysis {

    // FIELDS
    /** data buffer containin the lines of data */
    private Collection<String[]> dataBuffer;
    /** default height and width for output element */
    protected static final double DEFAULT_SIZE = 300.0;

    /**
     * Construct a column analysis.
     *
     * @param data		collection of data lines
     */
    public ColumnAnalysis(Collection<String[]> data) {
        this.dataBuffer = data;
    }

    /**
     * @return the pane displaying the analysis of a column
     *
     * @param name		name of the column being analyzed
     * @param colIdx	index of the column to analyze
     */
    public TitledPane getDisplay(String name, int colIdx) {
        Node analysis = this.getAnalysis(this.new Iter(colIdx));
        TitledPane retVal = new TitledPane(name, analysis);
        retVal.setCollapsible(false);
        return retVal;
    }

    /**
     * @return a display of the desired analysis
     *
     * @param column	iterator for the data to analyze
     */
    protected abstract Node getAnalysis(Iterator<String> column);

    /**
     * @return the number of data lines
     */
    protected int size() {
        return this.dataBuffer.size();
    }

    /**
     * This class creates an iterator for a single column.
     */
    public class Iter implements Iterator<String> {

        /** iterator through the lines of data */
        private Iterator<String[]> iter;
        /** index of the column of interest */
        private int colIdx;

        /**
         * Construct an iterator through the target column.
         *
         * @param colIdx	index of target column
         */
        public Iter(int colIdx) {
            this.iter = ColumnAnalysis.this.dataBuffer.iterator();
            this.colIdx = colIdx;
        }

        @Override
        public boolean hasNext() {
            return this.iter.hasNext();
        }

        @Override
        public String next() {
            return this.iter.next()[this.colIdx];
        }

    }

}
