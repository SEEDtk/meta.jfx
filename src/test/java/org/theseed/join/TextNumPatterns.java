/**
 *
 */
package org.theseed.join;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.theseed.test.Matchers.*;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TextNumPatterns {

    @Test
    public void test() {
        String[] floats = new String[] { "123.456", "123.", ".456", "123e-1", "-456e1", "+1.2", "-12.",
                "+1.2e+1" };
        String[] ints = new String[] { "1", "-2", "+34567" };
        String[] bads = new String[] { "1+2", "345.6.7", "8.4+1" };
        for (String fString : floats) {
            assertThat(fString, ExcelSaveSpec.DOUBLE_PATTERN.matcher(fString).matches(), isTrue());
            assertThat(fString, ExcelSaveSpec.INTEGER_PATTERN.matcher(fString).matches(), isFalse());
        }
        for (String fString : ints) {
            assertThat(fString, ExcelSaveSpec.DOUBLE_PATTERN.matcher(fString).matches(), isTrue());
            assertThat(fString, ExcelSaveSpec.INTEGER_PATTERN.matcher(fString).matches(), isTrue());
        }
        for (String fString : bads) {
            assertThat(fString, ExcelSaveSpec.DOUBLE_PATTERN.matcher(fString).matches(), isFalse());
            assertThat(fString, ExcelSaveSpec.INTEGER_PATTERN.matcher(fString).matches(), isFalse());
        }

    }

}
