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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import com.ximpleware.AutoPilot;
import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

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
 * Class for retrieval of data from XSLX document according to request URI via
 * direct access with VTD-XML.
 * 
 * @author Christoph Schröder
 */
public class DataExcelVtdResource {
    private PandaSettings             pandaSettings;

    // uses style format indexes as key and boolean whether they are a date
    // format or not as value for reuse
    private HashMap<Integer, Boolean> dateFormats       = new HashMap<Integer, Boolean>();
    // shared string table to get string values from text cells
    private SharedStringsData         sstData           = null;
    // contains index boundaries, if -1 => all values requested
    private TableArea                 tableArea;
    // avoids building the entire SST if true
    private boolean                   singleCellRequest = false;

    /**
     * Main method of this class to extract table data from XLSX document.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return list of values as {@code ValueExchangeExt}
     * @throws UnsupportedEncodingException URL decoding exception
     * @throws EncodingException
     * @throws EOFException
     * @throws EntityException
     * @throws ParseException
     * @throws XPathParseException
     * @throws XPathEvalException
     * @throws NavException
     * @throws OpenXML4JException
     */
    public ValueExchangeExt getExcelValues(UriInfo uriInfo, PandaSettings pandaSettings)
            throws UnsupportedEncodingException, EncodingException, EOFException, EntityException,
            ParseException, XPathParseException, XPathEvalException, NavException,
            OpenXML4JException {
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

        ValueExchangeExt valList = new ValueExchangeExt();

        try {
            File file = new File(filePath);
            Date lastModified = new Date(file.lastModified());
            EntityTag eTag = new EntityTag(resID + "_" + lastModified.getTime(), false);

            // Handling of Excel 2007 or newer (xlsx) documents
            if (!resourceType.equals(DataResourceType.XLSX)) {
                throw new WebApplicationException(404);
            } else {
                // lookup values in the cache if caching is on
                if (pandaSettings.getServerCacheUsage() && cache.checkValidityResource(resID, eTag)) {
                    String path = uriInfo.getPath();
                    path = path.substring(path.indexOf(resID));
                    valList = cache.getValues(path, resourceType);
                } else {
                    OPCPackage pkg = OPCPackage.open(file);
                    XSSFReader xssfReader = new XSSFReader(pkg);
                    valList = processExcelRequest(resID, requestType, sheetName, reference,
                            xssfReader);

                    // add resource to cache if not already done
                    if (pandaSettings.getServerCacheUsage()
                            && !cache.checkValidityResource(resID, eTag)) {
                        ValueExchangeExt newCacheValues = processExcelRequest(resID, requestType,
                                "*", "*", xssfReader);
                        cache.addResourceValues(newCacheValues, resInfo.getType(), resID, eTag);
                    }

                    // use revert instead of close (read only)
                    pkg.revert();
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
     * Main method of this class to extract a picture from XLSX document.
     * 
     * @param uriInfo Jersey URI object
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
        try {
            File file = new File(filePath);

            if (resourceType.equals(DataResourceType.XLSX)) {
                // Open OOXML Package
                OPCPackage pkg = OPCPackage.open(file);

                // regex pattern to get media files
                Pattern p = Pattern.compile("/xl/media/.*");
                // get media files
                List<PackagePart> media = pkg.getPartsByName(p);
                // remove other media than images
                for (PackagePart pp : media) {
                    if (!pp.getContentType().startsWith("image/")) {
                        media.remove(pp);
                    }
                }
                // get image by ID
                PackagePart pkgPart = pkg.getPartsByName(p).get(Integer.valueOf(imageID) - 1);
                imageData = IOUtils.toByteArray(pkgPart.getInputStream());
                mimeType = pkgPart.getContentType();

                pkg.revert();
            } else {
                throw new WebApplicationException(404);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(404);
        }

        return new ImageData(imageData, mimeType);
    }

    /**
     * Method to build the XPath expression according to the request parameters.
     * 
     * @param reference - table, column, row or cell reference
     * @return String XPath expression
     */
    private String buildXPathExp(String reference) {
        String rowPart = new String();

        this.tableArea = ResourceHelper.evalTableReference(reference);
        // ResourceHelper.convertColRefToColNum(colRef);
        if (reference.equals("*")) {
            // document or sheet request;
            rowPart = "row";
        } else if (reference.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            // table (area) request like B2:D8
            // build part of XPath expression for rows
            rowPart = "row[number(@r) >=\"" + String.valueOf(this.tableArea.getRowStart() + 1)
                    + "\" and number(@r) <=\"" + String.valueOf(this.tableArea.getRowEnd() + 1)
                    + "\"]";
        } else {
            // request to certain rows, columns or cells
            String[] refTokens = reference.replaceAll("(?<=\\p{L}|\\*)(?=\\d|\\*)", ";").split(";");
            String colRef = refTokens[0];
            String rowRef = refTokens[1];

            // single row, use XPath index syntax
            if (!rowRef.equals("*")) {
                rowPart = "row[@r=\"" + rowRef + "\"]";
            }
            // all rows (request to a column)
            else {
                rowPart = "row";
            }

            // request to a single cell, avoid building the entire SST
            if (!colRef.equals("*") && !rowRef.equals("*"))
                singleCellRequest = true;
        }
        String xpathExp = "/worksheet/sheetData/" + rowPart;
        return xpathExp;
    }

    /**
     * Method used to process the whole request.
     * 
     * @param resID unique ID of resource
     * @param requestType designator of URI scheme
     * @param sheetName name of sheet
     * @param reference table, column, row or cell reference
     * @param xssfReader package reader of POI
     * @return list of values as {@code ValueExchangeExt}
     * @throws EncodingException
     * @throws EOFException
     * @throws EntityException
     * @throws InvalidFormatException not an OOXML file
     * @throws ParseException error during parsing
     * @throws IOException can't open file
     * @throws XPathParseException error during parsing
     * @throws XPathEvalException error during XPath evaluation
     * @throws NavException error during XML navigation
     */
    private ValueExchangeExt processExcelRequest(String resID, String requestType,
            String sheetName, String reference, XSSFReader xssfReader) throws EncodingException,
            EOFException, EntityException, InvalidFormatException, ParseException, IOException,
            XPathParseException, XPathEvalException, NavException {
        ValueExchangeExt valList = new ValueExchangeExt();

        this.sstData = new SharedStringsData(xssfReader);
        StylesTable styles = xssfReader.getStylesTable();
        InputStream workBook = xssfReader.getWorkbookData();

        LinkedHashMap<String, InputStream> sheets = getSheets(workBook, xssfReader, sheetName);

        String xpathExp = buildXPathExp(reference);
        Boolean allSheets = sheetName.equals("*");

        if (allSheets) {
            valList.setBaseURI("/" + resID + "/" + requestType + "/");
        } else {
            valList.setBaseURI("/" + resID + "/" + requestType + "/"
                    + URLEncoder.encode(sheetName, "US-ASCII") + "/");
        }

        // iterate sheets for evaluation
        for (String curSheetName : sheets.keySet()) {
            String pathPrefix = "";
            if (allSheets)
                pathPrefix = URLEncoder.encode(curSheetName, "US-ASCII");
            InputStream sheetIS = sheets.get(curSheetName);
            byte[] sheet = IOUtils.toByteArray(sheetIS);
            // evaluate XPath expression for current sheet
            valList.addValues(evalSheet(sheet, xpathExp, reference, styles, pathPrefix));
        }
        return valList;
    }

    /**
     * Method that evaluates the XPath expression for a single worksheet.
     * 
     * @param sheetAp VTD auto pilot for XPath evaluation
     * @param sheetVn VTD navigator
     * @param sharedStrings - shared string table of excel document
     * @param styles styles table of excel document
     * @param pathPrefix prefix of subURI
     * @return list of sheet values as {@code LinkedList<Value>}
     * @throws EncodingException
     * @throws EOFException
     * @throws EntityException
     * @throws ParseException
     * @throws XPathEvalException
     * @throws NavException
     * @throws XPathParseException
     */
    private LinkedList<Value> evalSheet(byte[] sheet, String xpathExp, String reference,
            StylesTable styles, String pathPrefix) throws EncodingException, EOFException,
            EntityException, ParseException, XPathEvalException, NavException, XPathParseException {

        LinkedList<Value> sheetValues = new LinkedList<Value>();

        // setup VTD
        VTDGen sheetVg = new VTDGen();
        sheetVg.setDoc(sheet);
        sheetVg.parse(true);
        VTDNav sheetVn = sheetVg.getNav();
        AutoPilot sheetAp = new AutoPilot(sheetVn);
        sheetAp.selectXPath(xpathExp);

        // evaluate XPath and select cells
        // a cell can hold information about location (reference), value, data
        // type, formatting, and formula
        while (sheetAp.evalXPath() != -1) {
            // move to first cell
            if (sheetVn.toElement(VTDNav.FIRST_CHILD)) {
                // iterate cells
                while (moveToSibling(sheetVn, "c")) {
                    Value cellValue = null;
                    String cellRef = "";
                    if (sheetVn.hasAttr("r")) {
                        cellRef = sheetVn.toRawString(sheetVn.getAttrVal("r"));
                        if (this.tableArea.getColStart() == -1) {
                            cellValue = evalCell(sheetVn, styles, cellRef, pathPrefix);
                        } else {
                            // replaces empty character between letters and
                            // digits with ";" and split both parts
                            String[] cellRefTokens = cellRef.replaceAll("(?<=\\p{L})(?=\\d)", ";")
                                    .split(";");
                            Integer colID = ResourceHelper.convertColRefToColNum(cellRefTokens[0]);
                            if (this.tableArea.getColStart() <= colID
                                    && colID <= this.tableArea.getColEnd()) {
                                cellValue = evalCell(sheetVn, styles, cellRef, pathPrefix);
                            }
                        }
                    }

                    // add value to list
                    if (cellValue != null)
                        sheetValues.add(cellValue);

                    // stop if no other siblings
                    if (!sheetVn.toElement(VTDNav.NEXT_SIBLING)) {
                        break;
                    }
                }
                // move back to cell
                sheetVn.toElement(VTDNav.PARENT);
            }
        }

        return sheetValues;
    }

    /**
     * Method for evaluation of a cell node.
     * 
     * @param sheetVn VTD navigator
     * @param styles styles table of excel document
     * @param cellRef reference to cell
     * @param pathPrefix prefix of subURI
     * @return value of cell as {@code Value}
     * @throws NavException indicates error in XML navigation
     * @throws XPathParseException indicates error during parsing
     * @throws XPathEvalException indicates error during XPath evaluation
     */
    private Value evalCell(VTDNav sheetVn, StylesTable styles, String cellRef, String pathPrefix)
            throws NavException, XPathParseException, XPathEvalException {
        Value cellValue = new Value();

        String cellType = "";
        String cellStyle = "";
        String val = "";

        if (sheetVn.hasAttr("t")) {
            cellType = sheetVn.toRawString(sheetVn.getAttrVal("t"));
        }
        if (sheetVn.hasAttr("s")) {
            cellStyle = sheetVn.toRawString(sheetVn.getAttrVal("s"));
        }

        // get index of current node to return cursor of VTDNav
        int node = sheetVn.getCurrentIndex();
        // move to first child element of cell
        sheetVn.toElement(VTDNav.FIRST_CHILD);
        // move to v if existent and get value
        if (moveToSibling(sheetVn, "v")) {
            val = sheetVn.toRawString(sheetVn.getText());
        }

        switch (cellType) {
        case "b":
            // (Boolean) Cell containing a boolean
            cellValue.setType("xs:boolean");
            if (val.equals("1"))
                val = "true";
            else
                val = "false";
            break;
        case "d":
            // (Date) Cell contains a date in the ISO 8601 format
            val = ResourceHelper.getGregorianDate(DateUtil.getJavaDate(Double.parseDouble(val)));
            cellValue.setType("xs:dateTime");
            break;
        case "e":
            // (Error) Cell containing an error
            cellValue.setType("xs:string");
            break;
        case "inlineStr":
            // (inline) rich string, i.e., one not in the shared string
            // table. If this cell type is used, then the cell value is
            // in the is element rather than the v element in the cell
            // first element should be "t" with inline string value,
            // schema does not guarantee occurrence of "t" element!
            if (moveToSibling(sheetVn, "is")) {
                sheetVn.toElement(VTDNav.FIRST_CHILD);
                if (moveToSibling(sheetVn, "t")) {
                    val = sheetVn.toRawString(sheetVn.getText());
                }
            }
            cellValue.setType("xs:string");
            break;
        case "n":
            // (Number) Cell containing a number
            cellValue.setType("xs:double");
            break;
        case "s":
            // s (Shared String) Cell containing a shared string
            if (singleCellRequest)
                val = sstData.getSingleSstValue(val);
            else
                val = sstData.getSstValue(val);

            cellValue.setType("xs:string");
            break;
        case "str":
            // str (String) Cell containing a formula string
            cellValue.setType("xs:string");
            break;
        default:
            // no explicit type
            // dates are internally saved as double values
            // check style information whether it is a number or a date
            Double cellVal = Double.parseDouble(val);
            Boolean isDate = false;
            if (DateUtil.isValidExcelDate(cellVal) && !cellStyle.equals("")) {
                long idx = Long.parseLong(cellStyle);
                XSSFCellStyle style = styles.getStyleAt((int) idx);
                int i = style.getDataFormat();

                // save and use
                if (dateFormats.containsKey(i)) {
                    isDate = dateFormats.get(i);
                } else {
                    String f = style.getDataFormatString();
                    isDate = DateUtil.isADateFormat(i, f);
                    dateFormats.put(i, isDate);
                }
            }

            // if cell seems to be a date value calculate XML conform
            // xs:DateTime
            if (isDate) {
                val = ResourceHelper.getGregorianDate(DateUtil.getJavaDate(cellVal));
                cellValue.setType("xs:dateTime");
            } else {
                cellValue.setType("xs:double");
            }
        }

        // return to former node for further XPath evaluation
        sheetVn.recoverNode(node);

        // set cellValue and local URI
        if (!val.equals("")) {
            String path = cellRef;
            if (!pathPrefix.isEmpty()) {
                path = pathPrefix + "/" + path;
            }
            cellValue.setSubURI(path);
            cellValue.setValue(val);
        } else {
            // return null if empty value
            return null;
        }

        return cellValue;
    }

    /**
     * Method to get all sheets from which data was requested.
     * 
     * @param workBook excel document
     * @param xssfReader package reader of POI
     * @param sheetName name of excel sheet
     * @return list of sheets as {@code LinkedHashMap<String, InputStream>}
     * @throws IOException can't open file
     * @throws EncodingException
     * @throws EOFException
     * @throws EntityException
     * @throws ParseException
     * @throws XPathParseException
     * @throws XPathEvalException
     * @throws NavException
     * @throws InvalidFormatException not an OOXML file
     */
    private LinkedHashMap<String, InputStream> getSheets(InputStream workBook,
            XSSFReader xssfReader, String sheetName) throws IOException, EncodingException,
            EOFException, EntityException, ParseException, XPathParseException, XPathEvalException,
            NavException, InvalidFormatException {

        LinkedHashMap<String, InputStream> sheets = new LinkedHashMap<String, InputStream>();
        String relationshipNS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
        VTDGen wbkVg = new VTDGen();
        byte[] wbByteArr = IOUtils.toByteArray(workBook);
        wbkVg.setDoc(wbByteArr);
        wbkVg.parse(true);
        VTDNav wbVn = wbkVg.getNav();
        AutoPilot wbAp = new AutoPilot(wbVn);

        wbAp.declareXPathNameSpace("r", relationshipNS);

        if (sheetName.equals("*")) {
            wbAp.selectXPath("/workbook/sheets/sheet");
        } else {
            wbAp.selectXPath("/workbook/sheets/sheet[@name=\"" + sheetName + "\"]");
        }

        while (wbAp.evalXPath() != -1) {

            String relId = wbVn.toRawString(wbVn.getAttrValNS(relationshipNS, "id"));
            String curSheetName = wbVn.toRawString(wbVn.getAttrVal("name"));
            sheets.put(curSheetName, xssfReader.getSheet(relId));
        }

        if (sheets.isEmpty()) {
            throw new WebApplicationException(404);
        }

        return sheets;
    }

    /**
     * Helper method to move to an specific sibling.
     * 
     * @param vtdNav VTD navigator object
     * @param elementName name of sibling
     * @return true if sibling found
     * @throws NavException
     */
    private boolean moveToSibling(VTDNav vtdNav, String elementName) throws NavException {
        boolean foundElement = false;
        do {
            if (vtdNav.matchTokenString(vtdNav.getCurrentIndex(), elementName)) {
                foundElement = true;
                break;
            }
        } while (vtdNav.toElement(VTDNav.NEXT_SIBLING));

        return foundElement;
    }

    /**
     * Class used for low level access to shared string table. Not: there is
     * also a class in XSSFReader for access to SST, however retrieving the SST
     * object seems to be slower than direct access via VTDXml.
     * 
     * @author Christoph Schröder
     */
    private class SharedStringsData {

        ArrayList<String> sstTable = null;
        private VTDNav    sstVn;
        private AutoPilot sstAp;

        public SharedStringsData(XSSFReader xssfReader) throws InvalidFormatException, IOException,
                EncodingException, EOFException, EntityException, ParseException {
            VTDGen sstVg = new VTDGen();
            byte[] sstData = IOUtils.toByteArray(xssfReader.getSharedStringsData());
            sstVg.setDoc(sstData);
            sstVg.parse(true);
            sstVn = sstVg.getNav();
            sstAp = new AutoPilot(sstVn);
        }

        /**
         * Standard method to get shared string. All shared strings will be
         * gathered in a list. Prefer this method for bulk requests.
         * 
         * @param idx index of node inside shared string table
         * @return SST value as {@code String}
         * @throws XPathParseException
         * @throws XPathEvalException
         * @throws NavException
         */
        public String getSstValue(String idx) throws XPathParseException, XPathEvalException,
                NavException {
            if (sstTable == null) {
                sstTable = new ArrayList<String>();
                // set XPath expression to get shared strings
                String xpathExp = "/sst/si/t/text()";
                sstAp.selectXPath(xpathExp);
                // gather all shared strings
                while (sstAp.evalXPath() != -1) {
                    sstTable.add(sstVn.toString(sstVn.getCurrentIndex()));
                }

            }
            // get shared string value
            return sstTable.get(Integer.parseInt(idx));
        }

        /**
         * Method to lookup a single shared string inside shared string table.
         * Use this method for single cell requests.
         * 
         * @param idx index of node inside shared string table
         * @return SST value as {@code String}
         * @throws XPathParseException
         */
        public String getSingleSstValue(String idx) throws XPathParseException {
            String val = "";
            // Start to count at 1 instead of 0
            Integer refNum = Integer.parseInt(idx) + 1;
            String xpathExp = "/sst/si[" + refNum.toString() + "]/t/text()";
            sstAp.selectXPath(xpathExp);
            val = sstAp.evalXPathToString();
            return val;
        }
    }
}
