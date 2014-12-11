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


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import de.fuberlin.panda.api.data.ValueExchange.Value;
import de.fuberlin.panda.api.data.ValueExchangeExt;
import de.fuberlin.panda.data.caching.ResourceCache;
import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;
import de.fuberlin.panda.data.resources.ResourceHelper.TableArea;

/**
 * Class for retrieval of data from CSV documents according to request URI.
 * 
 * @author Christoph Schröder
 */
public class DataCsvResource {

    // start and end index for requested columns/rows, -1 = all columns/rows
    private TableArea tableArea;

    /**
     * Main method of this class to extract table data from CSV document.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return list of values as {@code ValueExchangeExt}
     */
    public ValueExchangeExt getCsvValues(UriInfo uriInfo, PandaSettings pandaSettings)
            throws WebApplicationException {

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        // Get Request parameters from URI
        String resID = pathSegments.get(1).getPath();
        String reference = pathSegments.get(2).getPath();

        // Get local path to file
        ResourceInfo resInfo = pandaSettings.getResourceMap().getMap().get(resID);
        String filePath = resInfo.getFilePath();
        String csvDelimiter = resInfo.getSeparator();

        // Get resource cache
        ResourceCache cache = pandaSettings.getResourceCache();

        ValueExchangeExt valList = new ValueExchangeExt();
        valList.setBaseURI("/" + resID + "/");

        try {
            File file = new File(filePath);
            Date lastModified = new Date(file.lastModified());
            EntityTag eTag = new EntityTag(resID + "_" + lastModified.getTime(), false);

            // lookup values in the cache if caching is on
            if (pandaSettings.getServerCacheUsage() && cache.checkValidityResource(resID, eTag)) {
                String path = uriInfo.getPath();
                path = path.substring(path.indexOf(resID));
                valList = cache.getValues(path, resInfo.getType());
            } else {
                if (!file.exists()) {
                    throw new WebApplicationException(404);
                }
                valList.addValues(processCsv(file, csvDelimiter, reference));

                // add resource to cache if not already done
                if (pandaSettings.getServerCacheUsage()
                        && !cache.checkValidityResource(resID, eTag)) {
                    ValueExchangeExt newCacheValues = new ValueExchangeExt();
                    newCacheValues.addValues(processCsv(file, csvDelimiter, "*"));
                    newCacheValues.setBaseURI("/" + resID + "/");
                    cache.addResourceValues(newCacheValues, resInfo.getType(), resID, eTag);
                }
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(404);
        }

        // return collected values of all selected sheets
        return valList;
    }

    /**
     * Processing of request to CSV file, returns list of values.
     * 
     * @param file CSV file
     * @param separator delimiter used for columns
     * @param reference reference to cell or table (see:
     *            JerseyCsvResource.class)
     * @return list of values as {@code LinkedList<Value>}
     * @throws IOException
     */
    private LinkedList<Value> processCsv(File file, String separator, String reference)
            throws IOException {
        // analyze request and set start/end indexes for columns/rows
        this.tableArea = ResourceHelper.evalTableReference(reference);

        LinkedList<Value> valList = new LinkedList<Value>();

        CSVFormat format;
        switch (separator) {
        case "\u002C":
            // RFC 4180 + empty lines allowed
            format = CSVFormat.DEFAULT;
            break;
        case "\u0009":
            format = CSVFormat.TDF;
            break;
        default:
            // custom delimiter and format
            format = CSVFormat.EXCEL.withDelimiter(separator.charAt(0));
            break;
        }

        CSVParser parser = CSVParser.parse(file, Charset.forName("UTF-8"), format);

        boolean allRows, allColumns;
        allRows = allColumns = false;
        if (tableArea.getRowEnd() == -1)
            allRows = true;
        if (tableArea.getColEnd() == -1)
            allColumns = true;

        // start counting lines (rows) at 1 to be excel conform
        for (CSVRecord csvRecord : parser) {
            long row = parser.getRecordNumber() - 1;
            // stop if current row > last requested row
            if (row > tableArea.getRowEnd() && !allRows) {
                break;
            }

            if (allRows || (row >= tableArea.getRowStart())) {

                if (tableArea.getColStart() < csvRecord.size()) {
                    // determine indexes for current row
                    int first = tableArea.getColStart();
                    int last;
                    // all cells of this row
                    if (allColumns) {
                        first = 0;
                        last = csvRecord.size() - 1;
                    } else if (tableArea.getColEnd() >= csvRecord.size()) {
                        last = csvRecord.size() - 1;
                    } else {
                        last = tableArea.getColEnd();
                    }

                    // get cell values
                    for (int col = first; col <= last; col++) {
                        Value cellValue = new Value();
                        String cellRef = ResourceHelper.convertColNumToColRef(col)
                                + new Integer((int) (row + 1)).toString();
                        String value = csvRecord.get(col);
                        cellValue.setValue(value);
                        cellValue.setSubURI(cellRef);
                        try {
                            Double.valueOf(value);
                            cellValue.setType("xs:double");
                        } catch (NumberFormatException e) {
                            cellValue.setType("xs:string");
                        }
                        valList.add(cellValue);
                    }
                }
            }
        }

        return valList;
    }

}
