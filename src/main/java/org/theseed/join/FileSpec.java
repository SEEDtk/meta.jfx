/**
 *
 */
package org.theseed.join;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.theseed.io.KeyedFileMap;
import org.theseed.io.TabbedLineReader;

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
        // The initial file spec's type cannot be changed.
        super.cmbType.setVisible(false);
    }

    @Override
    public void apply(KeyedFileMap keyedMap) throws IOException {
        // Note that this is much shorter than what the base class does.  We are starting from
        // scratch, not combining two maps.
        File inFile = this.getInFile();
        try (TabbedLineReader inStream = new TabbedLineReader(inFile)) {
            int inCount = 0;
            // Get the key column name and index.
            String keyName = this.getKeyColumn();
            int keyIdx = inStream.findField(keyName);
            // Get the names and indices of the data columns.
            List<String> headers = this.getHeaders();
            int[] cols = new int[headers.size()];
            for (int i = 0; i < cols.length; i++)
                cols[i] = inStream.findField(headers.get(i));
            // Add the qualifier to the headers, if needed.
            String qualifier = this.txtQualifier.getText();
            if (! qualifier.isBlank())
                headers = headers.stream().map(x -> qualifier + "." + x).collect(Collectors.toList());
            // Set the output headers.
            keyedMap.addHeaders(headers);
            // Now read in the file.
            for (TabbedLineReader.Line line : inStream) {
                String key = line.get(keyIdx);
                List<String> data = Arrays.stream(cols).mapToObj(i -> line.get(i)).collect(Collectors.toList());
                keyedMap.addRecord(key, data);
                inCount++;
            }
            // Display the result message.
            this.txtMessage.setText(String.format("%d input records, %d duplicate keys.",
                    inCount, keyedMap.getDupCount()));
        }
    }


}
