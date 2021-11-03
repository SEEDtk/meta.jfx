/**
 *
 */
package org.theseed.join;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.KeyedFileMap;

import javafx.stage.FileChooser.ExtensionFilter;

/**
 * This is the basic method of saving the join result as a flat file.  An option is provided to
 * open the file using the desktop.
 *
 * @author Bruce Parrello
 *
 */
public class FileSaveSpec extends SaveSpec {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(FileSaveSpec.class);

    @Override
    public boolean isValid() {
        return (this.getOutFile() != null);
    }

    @Override
    public void apply(KeyedFileMap keyedMap) throws IOException {
        keyedMap.write(this.getOutFile());
        this.checkForOpen();
    }

    @Override
    protected String getFileLabel() {
        return "Flat File";
    }

    @Override
    protected ExtensionFilter getFilter() {
        return JoinDialog.FLAT_FILTER;
    }

    @Override
    protected void initControls() {
    }


}
