/**
 *
 */
package org.theseed.join;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestColumnData {

    @Test
    void testColumnData() {
        String[] intCol = new String[] { "  ", "100", "", " 200", " 300", " 124 ", "", "\t", "66 " };
        String[] floatCol = new String[] { "123  ", "123  ", "  ", null, " 456.7 ", "78 ", "1e-6" };
        String[] textCol = new String[] { "  ", " ab ", " cd", "x", " ", "", "y", "123", "" };
        String[] flagCol = new String[] { "  a ", " b", "",  "\t \t", "d", "", "1", "2" };
        ColumnData intCheck = runCol(intCol);
        assertThat(intCheck.getType(), equalTo(ColumnData.Type.INTEGER));
        ColumnData floatCheck = runCol(floatCol);
        assertThat(floatCheck.getType(), equalTo(ColumnData.Type.DOUBLE));
        ColumnData textCheck = runCol(textCol);
        assertThat(textCheck.getType(), equalTo(ColumnData.Type.TEXT));
        ColumnData flagCheck = runCol(flagCol);
        assertThat(flagCheck.getType(), equalTo(ColumnData.Type.FLAG));
    }

    public ColumnData runCol(String[] col) {
        ColumnData retVal = new ColumnData();
        Arrays.stream(col).forEach(x -> retVal.check(x));
        return retVal;
    }

}
