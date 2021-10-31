/**
 *
 */
package org.theseed.join;

import java.util.List;

import org.theseed.io.KeyedFileMap;

/**
 * This is the controller for the initial input file.
 *
 * @author Bruce Parrello
 *
 */
public class FileSpec extends JoinSpec {

    @Override
    protected void initialConfigure() {
        // The initial file spec cannot be deleted.
        super.btnDelete.setVisible(false);
    }

    @Override
    protected void processLine(KeyedFileMap keyedMap, String key, List<String> data) {
        keyedMap.addRecord(key, data);
    }

    @Override
    protected void finishFile(KeyedFileMap keyedMap, int width) {
    }


}
