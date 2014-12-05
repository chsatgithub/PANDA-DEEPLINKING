package de.fuberlin.panda.data.resources;

/*
 * #%L
 * PANDA-DEEPLINKING
 * %%
 * Copyright (C) 2014 Freie Universität Berlin
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.fuberlin.panda.api.data.ValueExchange.Value;
import de.fuberlin.panda.api.data.ImageData;
import de.fuberlin.panda.api.data.ValueExchangeExt;
import de.fuberlin.panda.data.caching.ResourceCache;
import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMap;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;
import de.fuberlin.panda.data.resources.ResourceHelper.TableArea;
import de.fuberlin.panda.enums.DataResourceType;

/**
 * Class for retrieval of data from XLS/XSLX documents according to request URI
 * via Apache POI.
 * 
 * @author Christoph Schröder
 */
public class DataExcelResource {
    private PandaSettings    pandaSettings;
    private FormulaEvaluator eval;

    /**
     * Main method of this class to extract table data from XLS/XLSX document.
     * 
     * @param uriInfo - Jersey URI object
     * @param pandaSettings - PANDA system settings
     * @return list of values as {@code ValueExchangeExt}
     * @throws InvalidFormatException not a OOXML file
     * @throws UnsupportedEncodingException
     */
    public ValueExchangeExt getExcelValues(UriInfo uriInfo, PandaSettings pandaSettings)
            throws InvalidFormatException, UnsupportedEncodingException {
        this.pandaSettings = pandaSettings;

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        // Get Request parameters from URI
        String resID = pathSegments.get(1).getPath();
        String requestType = pathSegments.get(2).getPath();
        String sheetName = URLDecoder.decode(pathSegments.get(3).getPath(), "UTF-8");
        String reference = pathSegments.get(4).getPath();

        // Get local path to file
        ResourceInfo resInfo = pandaSettings.getResourceMap().getMap().get(resID);
        String filePath = resInfo.getFilePath();

        // Get resource type
        DataResourceType resourceType = resInfo.getType();

        // Get resource cache
        ResourceCache cache = pandaSettings.getResourceCache();

        Workbook wb = null;
        ValueExchangeExt valList = new ValueExchangeExt();

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
                if (resourceType.equals(DataResourceType.XLS)) {
                    NPOIFSFileSystem fs = new NPOIFSFileSystem(file);

                    // Create Workbook
                    wb = new HSSFWorkbook(fs.getRoot(), false);

                    fs.close();
                } else if (resourceType.equals(DataResourceType.XLSX)) {
                    OPCPackage pkg = OPCPackage.open(file);

                    // Create Workbook
                    wb = new XSSFWorkbook(pkg);

                    // revert = close without saving
                    pkg.revert();

                } else {
                    throw new WebApplicationException(404);
                }

                // process excel document and get a list of requested values
                valList = processExcelRequest(wb, resID, requestType, sheetName, reference);

                // add resource to cache if not already done
                if (pandaSettings.getServerCacheUsage()
                        && !cache.checkValidityResource(resID, eTag)) {
                    ValueExchangeExt newCacheValues = processExcelRequest(wb, resID, requestType,
                            "*", "*");
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
     * Main method of this class to extract a picture from XLS/XLSX document.
     * 
     * @param uriInfo Jersey URI object.
     * @param resourceMap map of resources
     * @return picture as {@code ImageData}
     * @throws WebApplicationException HTTP exception
     * @throws InvalidFormatException not a OOXML file
     */
    public ImageData getPicture(UriInfo uriInfo, ResourceMap resourceMap)
            throws WebApplicationException, InvalidFormatException {
        byte[] imageData = null;
        String mimeType = new String();

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        String resID = pathSegments.get(1).getPath();
        String imageID = pathSegments.get(3).getPath();
        // Get local path to file
        String filePath = resourceMap.getMap().get(resID).getFilePath();
        // Get resource type
        DataResourceType resourceType = resourceMap.getMap().get(resID).getType();

        Workbook wb;
        PictureData picture = null;
        try {
            File file = new File(filePath);
            // Handling of Excel 97-2003 (xls) documents
            if (resourceType.equals(DataResourceType.XLS)) {
                NPOIFSFileSystem fs = new NPOIFSFileSystem(file);

                // Create Workbook
                wb = new HSSFWorkbook(fs.getRoot(), false);

                // get picture by ID
                picture = wb.getAllPictures().get(Integer.valueOf(imageID));
                imageData = picture.getData();
                mimeType = picture.getMimeType();

                fs.close();
            }
            // Handling of Excel 2007 or newer (xlsx) documents
            else if (resourceType.equals(DataResourceType.XLSX)) {
                OPCPackage pkg = OPCPackage.open(file);

                // Create Workbook
                wb = new XSSFWorkbook(pkg);

                // get picture by ID
                picture = wb.getAllPictures().get(Integer.valueOf(imageID) - 1);
                imageData = picture.getData();
                mimeType = picture.getMimeType();

                pkg.close();
            } else {
                throw new InvalidFormatException("Neither XLSX not XLS");
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(404);
        }

        return new ImageData(imageData, mimeType);
    }

    /**
     * Method to process Excel documents
     * 
     * @param wb POI excel document object
     * @param resID unique ID of resource
     * @param sheetName name of Sheet
     * @param rowRef reference of row starting at '1'
     * @param colRef reference of column e.g. 'A'
     * @return list of values as {@code ValueExchangeExt}
     * @throws UnsupportedEncodingException
     */
    private ValueExchangeExt processExcelRequest(Workbook wb, String resID, String requestType,
            String sheetName, String reference) throws UnsupportedEncodingException {
        ValueExchangeExt valList = new ValueExchangeExt();

        // Create formula evaluator to calculate cell values if it is a formula
        eval = wb.getCreationHelper().createFormulaEvaluator();

        // reference to row or column
        String rowRef, colRef;
        // indexes for table (area of sheets) requests
        TableArea tableArea = ResourceHelper.evalTableReference(reference);

        // booleans to determine next operation
        Boolean allSheets, getTable, getRow, getColumn;
        allSheets = getTable = getRow = getColumn = false;

        // get booleans and indexes used for request processing and determine
        // operation
        allSheets = sheetName.equals("*");
        if (reference.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            // table (area) request like B2:D8
            getTable = true;
        } else if (reference.equals("*")) {
            // document or sheet request
            getRow = true;
            getColumn = true;
        } else {
            // request to certain rows, columns or cells
            String[] refTokens = reference.replaceAll("(?<=\\p{L}|\\*)(?=\\d|\\*)", ";").split(";");
            colRef = refTokens[0];
            rowRef = refTokens[1];

            if (colRef.equals("*"))
                getRow = true;

            if (rowRef.equals("*"))
                getColumn = true;
        }

        List<Sheet> sheets = new LinkedList<Sheet>();
        // request to all Sheets of Excel document
        if (allSheets) {
            valList.setBaseURI("/" + resID + "/" + requestType + "/");
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                sheets.add(wb.getSheetAt(i));
            }
        }

        // request to a single sheet
        else {
            valList.setBaseURI("/" + resID + "/" + requestType + "/"
                    + URLEncoder.encode(sheetName, "US-ASCII") + "/");
            sheets.add(wb.getSheet(sheetName));
        }

        // iterate requested sheets
        for (Sheet s : sheets) {
            String pathPrefix = "";
            if (allSheets) {
                pathPrefix = URLEncoder.encode(s.getSheetName(), "US-ASCII");
            }

            if (getTable) {
                // get a table from sheet
                valList.addValues(getTable(s, tableArea.getRowStart(), tableArea.getRowEnd(),
                        tableArea.getColStart(), tableArea.getColEnd(), pathPrefix));
            } else if (getRow && getColumn) {
                // get whole sheet
                valList.addValues(getSheet(s, pathPrefix));
            } else if (getRow) {
                // get a row
                Row row = s.getRow(tableArea.getRowStart());
                valList.addValues(getRow(row, pathPrefix));
            } else if (getColumn) {
                // get a column
                valList.addValues(getColumn(s, tableArea.getColStart(), pathPrefix));
            } else {
                // get a single cell value
                Cell cell = s.getRow(tableArea.getRowStart()).getCell(tableArea.getColStart());
                valList.addValue(getCell(cell, pathPrefix));
            }
        }

        return valList;
    }

    /***
     * Method to get a whole sheet of an excel file.
     * 
     * @param sheet reference to Excel sheet
     * @param pathPrefix prefix of SubURI
     * @return list of sheet values as {@code LinkedList<Value>}
     * @throws WebApplicationException
     */
    private LinkedList<Value> getSheet(Sheet sheet, String pathPrefix)
            throws WebApplicationException {
        LinkedList<Value> valList = new LinkedList<Value>();

        for (Row r : sheet) {
            for (Cell c : r) {
                if (c != null) {
                    valList.add(getCell(c, pathPrefix));
                }
            }
        }
        return valList;
    }

    /***
     * Method to get an entire column of a table.
     * 
     * @param sheet reference to Excel sheet
     * @param colNum index of column
     * @param pathPrefix prefix of SubURI
     * @return list of column values as {@code LinkedList<Value>}
     * @throws WebApplicationException
     */
    private LinkedList<Value> getColumn(Sheet sheet, Integer colNum, String pathPrefix)
            throws WebApplicationException {
        LinkedList<Value> valList = new LinkedList<Value>();

        for (Row r : sheet) {
            Cell c = r.getCell(colNum);
            if (c != null) {
                valList.add(getCell(c, pathPrefix));
            }
        }
        return valList;
    }

    /***
     * Method to get an entire row of a table.
     * 
     * @param row reference to Row
     * @param pathPrefix prefix of SubURI
     * @return list of row values as {@code LinkedList<Value>}
     * @throws WebApplicationException
     */
    private LinkedList<Value> getRow(Row row, String pathPrefix) throws WebApplicationException {
        LinkedList<Value> valList = new LinkedList<Value>();

        for (Cell c : row) {
            if (c != null) {
                valList.add(getCell(c, pathPrefix));
            }
        }
        return valList;
    }

    /**
     * Gets a table (area) from a sheet e.g. 'B1:D1'
     * 
     * @param sheet excel sheet
     * @param rowStart first row index
     * @param rowEnd last row index
     * @param colStart first column index
     * @param colEnd last column index
     * @param pathPrefix prefix of SubURI
     * @return list of table values as {@code LinkedList<Value>}
     * @throws WebApplicationException
     */
    private LinkedList<Value> getTable(Sheet sheet, Integer rowStart, Integer rowEnd,
            Integer colStart, Integer colEnd, String pathPrefix) throws WebApplicationException {
        LinkedList<Value> valList = new LinkedList<Value>();

        for (int i = rowStart; (i <= rowEnd) && (i <= sheet.getLastRowNum()); i++) {
            Row r = sheet.getRow(i);
            if (r != null) {
                for (int j = colStart; (j <= colEnd) && (j <= r.getLastCellNum()); j++) {
                    Cell c = r.getCell(j);
                    if (c != null) {
                        valList.add(getCell(c, pathPrefix));
                    }
                }
            }
        }

        return valList;
    }

    /***
     * Method to get value and informations about a single cell.
     * 
     * @param cell reference to a cell
     * @param pathPrefix prefix of SubURI
     * @return value of cell as {@code Value}
     * @throws WebApplicationException
     */
    private Value getCell(Cell cell, String pathPrefix) throws WebApplicationException {
        // Evaluation of formulas, cell type "CELL_TYPE_FORMULA" should never
        // occur after evaluation
        CellValue cellVal = this.eval.evaluate(cell);

        Value singleVal = new Value();
        switch (cellVal.getCellType()) {
        case Cell.CELL_TYPE_STRING:
            singleVal.setType("xs:string");
            singleVal.setValue(cell.getRichStringCellValue().getString());
            break;
        case Cell.CELL_TYPE_NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
                singleVal.setType("xs:dateTime");
                singleVal.setValue(ResourceHelper.getGregorianDate(cell.getDateCellValue()));
            } else {
                singleVal.setType("xs:double");
                // singleVal.setValue(String.valueOf(cell.getNumericCellValue()));
                double d = cell.getNumericCellValue();
                // removes unnecessary ".0"
                String s = (long) d == d ? "" + (long) d : "" + d;
                singleVal.setValue(s);
            }
            break;
        case Cell.CELL_TYPE_BOOLEAN:
            singleVal.setType("xs:boolean");
            singleVal.setValue(String.valueOf(cell.getBooleanCellValue()));
            break;
        case Cell.CELL_TYPE_FORMULA:
            // Should never occur if FormularEvaluator is used
            singleVal.setType("xs:string");
            singleVal.setValue(cell.getCellFormula());
            break;
        default:
            throw new WebApplicationException(422);
        }

        // set the path for subURI of this cell
        String path = ResourceHelper.convertColNumToColRef(cell.getColumnIndex())
                + String.valueOf(cell.getRowIndex() + 1);
        if (!pathPrefix.isEmpty()) {
            path = pathPrefix + "/" + path;
        }
        singleVal.setSubURI(path);

        return singleVal;
    }

}
