package de.fuberlin.panda.data.resources;

/*
 * #%L
 * PANDA-DEEPLINKING
 * %%
 * Copyright (C) 2014 Freie Universitaet Berlin
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import java.text.SimpleDateFormat;
import java.util.Date;

import de.fuberlin.panda.data.configuration.PandaSettings;

public class ResourceHelper {
    private static PandaSettings pandaSettings;

    /**
     * Set PANDA settings.
     * 
     * @param pandaSettings - PANDA system settings
     */
    public static void setPandaSettings(PandaSettings pandaSettings) {
        ResourceHelper.pandaSettings = pandaSettings;
    }

    /**
     * Returns TableArea Object with start and end values for rows and columns.
     * 
     * @param reference reference with pattern
     *            (\\*|[A-Z][A-Z]*[1-9][0-9]*|\\*[1-9][0-9] *|[A-Z][A-
     *            Z]*\\*|[A-Z][A-Z]*[1-9][0-9]*\\:[A-Z][A-Z]*[1-9][0-9]*)
     * @return table area object as {@code TableArea}
     */
    public static TableArea evalTableReference(String reference) {

        // start and end index values for reference, 0 based!, "*" => -1 => all
        // values
        Integer colStart, rowStart, colEnd, rowEnd;

        if (reference.equals("*")) {
            colStart = colEnd = rowStart = rowEnd = -1;
        } else if (reference.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            String[] refTokens = reference.replaceAll("(?<=\\p{L})(?=\\d)",
                    pandaSettings.getRangeDelimiterChar()).split(pandaSettings.getRangeDelimiterChar());
            if (refTokens.length != 4) {
                // input has not the correct pattern
                return null;
            }

            colStart = convertColRefToColNum(refTokens[0]);
            rowStart = Integer.valueOf(refTokens[1]) - 1;
            colEnd = convertColRefToColNum(refTokens[2]);
            rowEnd = Integer.valueOf(refTokens[3]) - 1;

            // start should be smaller or even to end
            if (rowEnd < rowStart) {
                Integer temp = rowStart;
                rowStart = rowEnd;
                rowEnd = temp;
            }

            // start should be smaller or even to end
            if (colEnd < colStart) {
                Integer temp = colStart;
                colStart = colEnd;
                colEnd = temp;
            }
        } else if (reference.replaceAll("(?<=\\p{L}|\\*)(?=\\d|\\*)", ";").split(";").length == 2) {

            String[] refTokens = reference.replaceAll("(?<=\\p{L}|\\*)(?=\\d|\\*)", ";").split(";");
            String colRef = refTokens[0];
            String rowRef = refTokens[1];

            if (!rowRef.equals("*")) {
                rowStart = rowEnd = Integer.valueOf(rowRef) - 1;
            }
            // all rows (request to a column)
            else {
                rowStart = rowEnd = -1;
            }

            // single column, start = end
            if (!colRef.equals("*")) {
                colEnd = colStart = convertColRefToColNum(colRef);
            }
            // all columns (request to a row)
            else {
                colStart = colEnd = -1;
            }

        } else {
            throw new IllegalArgumentException(
                    "Input table/cell reference has not the correct pattern!");
        }

        TableArea tableArea = new TableArea(rowStart, rowEnd, colStart, colEnd);
        return tableArea;
    }

    /**
     * Converts a column index into the according excel letter scheme
     * 
     * @param colNum Index of column
     * @return excel column reference
     */
    public static String convertColNumToColRef(Integer colNum) {
        String colRef = "";
        Integer colID = colNum + 1;
        boolean finished = false;
        char c;
        while (!finished) {
            if (colID <= 26)
                finished = true;
            int offset = colID % 26;
            if (offset == 0)
                offset = 26;
            c = (char) (64 + offset);
            colID = (int) Math.floor(colID / 26);
            colRef = c + colRef;
        }

        return colRef;
    }

    /**
     * Helper method that converts a column reference into an column index,
     * starting at 'A' = 0
     * 
     * @param colRef column reference like 'A' or 'ZV'
     * @return Index of column starting at 0
     */
    public static Integer convertColRefToColNum(String colRef) {
        Integer colNum = 0;
        int n = 0;
        for (int i = colRef.length(); i > 0; i--) {
            char letter = colRef.charAt(i - 1);
            colNum = (int) (colNum + ((letter - 64) * Math.pow(26, n)));
            n++;
        }
        colNum = colNum - 1;
        return colNum;
    }

    /**
     * Helper method to convert internal excel date format into XML type
     * xs:dateTime
     * 
     * @param date date to convert
     * @return date as XML xs:dateTime String
     */
    public static String getGregorianDate(Date date) {
        String gregorianDate = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        gregorianDate = sdf.format(date);
        return gregorianDate;
    }

    /**
     * Contains start and end indexes for table requests. If value == -1 all
     * values of a row/column are requested.
     * 
     * @author Christoph Schröder
     */
    public static class TableArea {
        private Integer rowStart, rowEnd, colStart, colEnd;

        TableArea(Integer rowStart, Integer rowEnd, Integer colStart, Integer colEnd) {
            this.rowStart = rowStart;
            this.rowEnd = rowEnd;
            this.colStart = colStart;
            this.colEnd = colEnd;
        }

        public Integer getRowStart() {
            return this.rowStart;
        }

        public Integer getRowEnd() {
            return this.rowEnd;
        }

        public Integer getColStart() {
            return this.colStart;
        }

        public Integer getColEnd() {
            return this.colEnd;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (!(obj instanceof TableArea))
                return false;
            TableArea taObj = (TableArea) obj;
            if (this.colStart == taObj.getColStart() && this.colEnd == taObj.getColEnd()
                    && this.rowStart == taObj.getRowStart() && this.rowEnd == taObj.getRowEnd()) {
                return true;
            } else {
                return false;
            }
        }
    }
}
