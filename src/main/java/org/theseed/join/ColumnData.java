/**
 *
 */
package org.theseed.join;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.theseed.io.KeyedFileMap;

/**
 * This class contains utilities for analyzing data columns.  Each column contains
 * a bunch of strings.  The type of the column can be INTEGER, FLOAT, TEXT, or FLAG.
 * We use pattern-matching to determine the column type.  A column has a specific
 * type only if every non-blank value in the column matches the type pattern.  There
 * is a nice progression of patterns where INTEGER implies FLOAT and everything matches
 * TEXT.  The only anomaly is FLAG, which is TEXT with a maximum length of 1.
 *
 * @author Bruce Parrello
 *
 */
public class ColumnData {

    /**
     * This enumeration is used to pass back the actual type
     */
    public static enum Type {
        INTEGER, DOUBLE, TEXT, FLAG;
    }

    // FIELDS
    /** numeric type of column */
    private Type numType;
    /** FALSE if any column items could not be a flag */
    private boolean possibleFlag;
    /** double data type pattern */
    protected static final Pattern DOUBLE_PATTERN = KeyedFileMap.DOUBLE_PATTERN;
    /** integer data type pattern */
    protected static final Pattern INTEGER_PATTERN = Pattern.compile("\\s*[\\-+]?\\d+\\s*");
    /** flag data type pattern */
    protected static final Pattern FLAG_PATTERN = Pattern.compile("\\s*.\\s*");

    /**
     * Construct a column-data object for an empty column.
     */
    public ColumnData() {
        this.numType = Type.INTEGER;
        this.possibleFlag = true;
    }

    /**
     * Update the type based on a new column string.
     *
     * @param string		string to check
     */
    public void check(String string) {
        if (! StringUtils.isBlank(string)) {
            if (this.possibleFlag)
                this.possibleFlag = FLAG_PATTERN.matcher(string).matches();
            if (this.numType == Type.INTEGER && ! INTEGER_PATTERN.matcher(string).matches())
                this.numType = Type.DOUBLE;
            if (this.numType == Type.DOUBLE && ! DOUBLE_PATTERN.matcher(string).matches())
                this.numType = Type.TEXT;
        }
    }

    /**
     * @return the type of this column
     */
    public Type getType() {
        Type retVal = this.numType;
        if (this.numType == Type.TEXT && this.possibleFlag)
            retVal = Type.FLAG;
        return retVal;
    }

}
