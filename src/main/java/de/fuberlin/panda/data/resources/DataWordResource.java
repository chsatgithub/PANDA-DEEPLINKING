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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

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
 * Class for retrieval of data from DOC/DOCX documents according to request URI
 * via Apache POI.
 * 
 * @author Christoph Schröder
 */
public class DataWordResource {
    private PandaSettings pandaSettings;
    private Integer       startTablePara, endTablePara, startPara, endPara;
    private TableArea     tableArea;

    /**
     * Main method of this class to extract text data from DOC/DOCX document.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return list of values as {@code ValueExchangeExt}
     * @throws WebApplicationException HTTP exception
     * @throws InvalidFormatException not a OOXML file
     * @return list of values as {@code ValueExchangeExt}
     */
    public ValueExchangeExt getTextParagraphsXML(UriInfo uriInfo, PandaSettings pandaSettings)
            throws WebApplicationException, InvalidFormatException {
        this.pandaSettings = pandaSettings;

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        // Get Request parameters from URI
        String resID = pathSegments.get(1).getPath();
        String requestType = pathSegments.get(2).getPath();
        String paragraphRef = pathSegments.get(3).getPath();
        // Get local path to file
        ResourceInfo resInfo = pandaSettings.getResourceMap().getMap().get(resID);
        String filePath = resInfo.getFilePath();

        // Get resource type
        DataResourceType resourceType = resInfo.getType();

        // Get resource cache
        ResourceCache cache = pandaSettings.getResourceCache();

        // List of gathered values for response
        ValueExchangeExt valList = new ValueExchangeExt();
        valList.setBaseURI("/" + resID + "/" + requestType + "/");

        try {
            File file = new File(filePath);
            Date lastModified = new Date(file.lastModified());
            EntityTag eTag = new EntityTag(resID + "_" + lastModified.getTime(), false);

            // lookup values in the cache if caching is on
            if (pandaSettings.getServerCacheUsage() && cache.checkValidityResource(resID, eTag)) {
                String path = uriInfo.getPath();
                path = path.substring(path.indexOf(resID));
                valList = cache.getValues(path, resourceType);
            } else {
                // Handling of Word 97-2003 (doc) documents
                if (resourceType.equals(DataResourceType.DOC)) {

                    // Open Word file
                    NPOIFSFileSystem fs = new NPOIFSFileSystem(file);
                    HWPFDocument doc = new HWPFDocument(fs.getRoot());
                    fs.close();

                    valList.addValues(processParagraphRequest(doc, paragraphRef));

                    // add resource to cache if not already done
                    if (pandaSettings.getServerCacheUsage()
                            && !cache.checkValidityResource(resID, eTag)) {
                        ValueExchangeExt textCacheValues = new ValueExchangeExt();
                        textCacheValues.setBaseURI("/" + resID + "/" + "text" + "/");
                        textCacheValues.addValues(processParagraphRequest(doc, "*"));
                        ValueExchangeExt tableCacheValues = processTableRequest(doc, resID,
                                "tables", "*", "*", "*");
                        cache.addResourceValues(textCacheValues, resInfo.getType(), resID, eTag);
                        cache.addResourceValues(tableCacheValues, resInfo.getType(), resID, eTag);
                    }
                }
                // Handling of Word 2007 or newer (docx) documents
                else if (resourceType.equals(DataResourceType.DOCX)) {

                    // Open OOXML Package
                    OPCPackage pkg = OPCPackage.open(file);

                    XWPFDocument docx = new XWPFDocument(pkg);

                    // revert = close without saving
                    pkg.revert();

                    valList.addValues(processParagraphRequest(docx, paragraphRef));

                    // add resource to cache if not already done
                    if (pandaSettings.getServerCacheUsage()
                            && !cache.checkValidityResource(resID, eTag)) {
                        ValueExchangeExt textCacheValues = new ValueExchangeExt();
                        textCacheValues.addValues(processParagraphRequest(docx, "*"));
                        cache.addResourceValues(textCacheValues, resInfo.getType(), resID, eTag);

                        ValueExchangeExt tableCacheValues = new ValueExchangeExt();
                        tableCacheValues.addValues(processParagraphRequest(docx, "*"));
                        cache.addResourceValues(tableCacheValues, resInfo.getType(), resID, eTag);
                    }
                } else {
                    throw new WebApplicationException(404);
                }
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(404);
        }

        return valList;
    }

    /**
     * Main method of this class to extract table data from DOC/DOCX document.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return list of values as {@code ValueExchangeExt}
     * @throws WebApplicationException HTTP exception
     * @throws InvalidFormatException not a OOXML file
     * @return list of values as {@code ValueExchangeExt}
     */
    public ValueExchangeExt getTableParagraphsXML(UriInfo uriInfo, PandaSettings pandaSettings)
            throws WebApplicationException, InvalidFormatException {
        this.pandaSettings = pandaSettings;

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        // Get Request parameters from URI
        String resID = pathSegments.get(1).getPath();
        String requestType = pathSegments.get(2).getPath();
        String tablePos = pathSegments.get(3).getPath();
        String cellRef = pathSegments.get(4).getPath();
        String paragraphRef = pathSegments.get(5).getPath();
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

            // lookup values in the cache if caching is on
            if (pandaSettings.getServerCacheUsage() && cache.checkValidityResource(resID, eTag)) {
                String path = uriInfo.getPath();
                path = path.substring(path.indexOf(resID));
                valList = cache.getValues(path, resourceType);
            } else {
                // Handling of Word 97-2003 (doc) documents
                if (resourceType.equals(DataResourceType.DOC)) {
                    NPOIFSFileSystem fs = new NPOIFSFileSystem(file);
                    HWPFDocument doc = new HWPFDocument(fs.getRoot());
                    fs.close();

                    // process word document and get a list of requested values
                    valList = processTableRequest(doc, resID, requestType, tablePos, cellRef,
                            paragraphRef);

                    // add resource to cache if not already done
                    if (pandaSettings.getServerCacheUsage()
                            && !cache.checkValidityResource(resID, eTag)) {
                        ValueExchangeExt textCacheValues = new ValueExchangeExt();
                        textCacheValues.setBaseURI("/" + resID + "/" + "text" + "/");
                        textCacheValues.addValues(processParagraphRequest(doc, "*"));
                        ValueExchangeExt tableCacheValues = processTableRequest(doc, resID,
                                "tables", "*", "*", "*");
                        cache.addResourceValues(textCacheValues, resInfo.getType(), resID, eTag);
                        cache.addResourceValues(tableCacheValues, resInfo.getType(), resID, eTag);
                    }
                }
                // Handling of Word 2007 or newer (docx) documents
                else if (resourceType.equals(DataResourceType.DOCX)) {
                    // Open OOXML Package
                    OPCPackage pkg = OPCPackage.open(file);
                    XWPFDocument docx = new XWPFDocument(pkg);
                    pkg.close();

                    // process word document and get a list of requested values
                    valList = processTableRequest(docx, resID, requestType, tablePos, cellRef,
                            paragraphRef);

                    // add resource to cache if not already done
                    if (pandaSettings.getServerCacheUsage()
                            && !cache.checkValidityResource(resID, eTag)) {
                        ValueExchangeExt textCacheValues = new ValueExchangeExt();
                        textCacheValues.addValues(processParagraphRequest(docx, "*"));
                        cache.addResourceValues(textCacheValues, resInfo.getType(), resID, eTag);

                        ValueExchangeExt tableCacheValues = new ValueExchangeExt();
                        tableCacheValues.addValues(processParagraphRequest(docx, "*"));
                        cache.addResourceValues(tableCacheValues, resInfo.getType(), resID, eTag);
                    }
                } else {
                    throw new WebApplicationException(404);
                }
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(404);
        }

        return valList;
    }

    /**
     * Main method of this class to extract a picture from DOC/DOCX document.
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
            // Handling of Word 97-2003 (doc) documents
            if (resourceType.equals(DataResourceType.DOC)) {

                NPOIFSFileSystem fs = new NPOIFSFileSystem(file);
                HWPFDocument doc = new HWPFDocument(fs.getRoot());
                // get image by id
                Picture hwpfPicture = doc.getPicturesTable().getAllPictures()
                        .get(Integer.valueOf(imageID) - 1);
                imageData = hwpfPicture.getContent();
                mimeType = hwpfPicture.getMimeType();

                fs.close();
            }
            // Handling of Word 2007 or newer (docx) documents
            else if (resourceType.equals(DataResourceType.DOCX)) {

                // Open Word file and get XWPFDocument
                OPCPackage pkg = OPCPackage.open(file);
                XWPFDocument docx = new XWPFDocument(pkg);

                // get image by id
                XWPFPictureData xwpfPicture = docx.getAllPictures().get(
                        Integer.valueOf(imageID) - 1);
                imageData = xwpfPicture.getData();
                mimeType = xwpfPicture.getPackagePart().getContentType();

                // revert = close without saving
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
     * ############## Methods for access to elements of Word 2007 or newer
     * (docx) documents ##############
     */

    /**
     * Processing of table request to Word 2007 document.
     * 
     * @param docx word document
     * @param resID unique ID of recource
     * @param tablePos index of table
     * @param cellRef reference of table cell
     * @param paragraphRef reference of table paragraphs
     * @return list of values as {@code ValueExchangeExt}
     */
    private ValueExchangeExt processTableRequest(XWPFDocument docx, String resID,
            String requestType, String tablePos, String cellRef, String paragraphRef) {
        ValueExchangeExt valList = new ValueExchangeExt();

        // get parameters to determine operation
        Boolean allTables = tablePos.equals("*");
        this.setStartEndParagraph("*", paragraphRef);
        this.tableArea = ResourceHelper.evalTableReference(cellRef);

        List<XWPFTable> tables = new LinkedList<XWPFTable>();
        if (allTables) {
            // get all tables
            tables = docx.getTables();
            valList.setBaseURI("/" + resID + "/" + requestType + "/");
        } else {
            // get table and position of table
            Integer tPos = Integer.parseInt(tablePos) - 1;
            tables.add(docx.getTables().get(tPos));
            valList.setBaseURI("/" + resID + "/" + requestType + "/" + tablePos + "/");
        }

        Integer tPos = 1;
        for (XWPFTable t : tables) {
            String pathPrefix;
            if (allTables) {
                pathPrefix = tPos + "/";
            } else {
                pathPrefix = "";
            }
            valList.addValues(processTable(t, pathPrefix));
            tPos++;
        }

        return valList;
    }

    /**
     * Processing of text requests.
     * 
     * @param docx word document
     * @param paraRef reference to paragraphs
     * @return list of values as {@code LinkedList<Value>}
     */
    private LinkedList<Value> processParagraphRequest(XWPFDocument docx, String paraRef) {
        LinkedList<Value> valList = new LinkedList<Value>();

        List<XWPFParagraph> paraList = docx.getParagraphs();
        setStartEndParagraph(paraRef, "*");
        // -1 => all paragraphs requested
        if (this.startPara == -1 || this.endPara == -1) {
            this.startPara = 0;
            this.endPara = paraList.size() - 1;
        }

        if (this.startPara < paraList.size()) {
            if (!(this.endPara < paraList.size())) {
                this.endPara = paraList.size() - 1;
            }

            for (Integer pPos = this.startPara; pPos <= this.endPara; pPos++) {
                Integer subURI = pPos + 1;
                valList.add(getParagraph(paraList.get(pPos), subURI.toString()));
            }
        }

        return valList;
    }

    /**
     * Get a single paragraph of Word 2007 document
     * 
     * @param paragraph XWPF paragraph to process
     * @param paragraphRef reference to paragraph
     * @return value of paragraph as {@code Value}
     */
    private Value getParagraph(XWPFParagraph paragraph, String paragraphRef) {
        Value singleVal = new Value();
        String pText = paragraph.getParagraphText().trim();
        
        // remove reference sequences
        pText = pText.replaceAll(Pattern.quote("[") + "footnoteRef:.*" + Pattern.quote("]"), "");
        pText = pText.replaceAll(Pattern.quote("[") + "endnoteRef:.*" + Pattern.quote("]"), "");

        singleVal.setValue(pText);
        singleVal.setSubURI(paragraphRef);
        singleVal.setType("xs:string");
        return singleVal;
    }

    /**
     * Process table and get values.
     * 
     * @param table table
     * @param pathPrefix prefix of subURI
     * @return list of table values as {@code LinkedList<Value>}
     */
    private LinkedList<Value> processTable(XWPFTable table, String pathPrefix) {
        LinkedList<Value> valList = new LinkedList<Value>();

        // temporary values for each table
        Integer rStart = this.tableArea.getRowStart();
        Integer rEnd = this.tableArea.getRowEnd();
        Integer cStart = this.tableArea.getColStart();
        Integer cEnd = this.tableArea.getColEnd();

        List<XWPFTableRow> rows = table.getRows();
        // -1 => all rows requested (column)
        if (this.tableArea.getRowStart() == -1 || this.tableArea.getRowEnd() == -1) {
            rStart = 0;
            rEnd = rows.size() - 1;
        }

        for (int i = rStart; (i <= rEnd) && (i < rows.size()); i++) {
            XWPFTableRow r = rows.get(i);

            if (r != null) {
                List<XWPFTableCell> cells = r.getTableCells();
                // -1 => all columns requested (row)
                if (this.tableArea.getColStart() == -1 || this.tableArea.getColEnd() == -1) {
                    cStart = 0;
                    cEnd = cells.size() - 1;
                }

                for (int j = cStart; (j <= cEnd) && (j < cells.size()); j++) {
                    XWPFTableCell c = cells.get(j);
                    if (c != null) {
                        String newPathPrefix = pathPrefix + ResourceHelper.convertColNumToColRef(j)
                                + (i + 1) + "/";
                        valList.addAll(getTableCell(c, newPathPrefix));
                    }
                }
            }
        }

        return valList;
    }

    /**
     * Process cell and get value.
     * 
     * @param cell cell
     * @param pathPrefix prefix of subURI
     * @return list of table paragraph cell values as {@code LinkedList<Value>}
     */
    private LinkedList<Value> getTableCell(XWPFTableCell cell, String pathPrefix) {
        LinkedList<Value> valList = new LinkedList<Value>();
        List<XWPFParagraph> paraList = cell.getParagraphs();

        // temporary values for each cell
        Integer pStart = startTablePara;
        Integer pEnd = endTablePara;

        // -1 => all paragraphs requested
        if (this.startTablePara == -1 || this.endTablePara == -1) {
            pStart = 0;
            pEnd = paraList.size() - 1;
        }

        if (pStart < paraList.size()) {
            if (!(pEnd < paraList.size())) {
                pEnd = paraList.size() - 1;
            }

            for (Integer pPos = pStart; pPos <= pEnd; pPos++) {
                XWPFParagraph p = paraList.get(pPos);
                Value value = new Value();
                String cellVal = p.getParagraphText().trim();
                if (!cellVal.isEmpty()) {
                    value.setValue(cellVal);
                    value.setSubURI(pathPrefix + (pPos + 1));
                    value.setType("xs:string");
                    valList.add(value);
                }
            }
        }

        return valList;
    }

    /**
     * ############## Methods for access to elements of Word 97-2003 (doc)
     * documents ##############
     */

    /**
     * Processing of table request to Word 97 - 2003 document.
     * 
     * @param doc word document
     * @param resID unique ID of resource
     * @param tablePos table index.
     * @param reference reference of table cell.
     * @param tableParaRef reference of table paragraph.
     * @return list of table values as {@code ValueExchangeExt}
     */
    private ValueExchangeExt processTableRequest(HWPFDocument doc, String resID,
            String requestType, String tablePos, String reference, String tableParaRef) {
        ValueExchangeExt valList = new ValueExchangeExt();

        // get booleans and indexes used for request processing and determine
        // operation
        Boolean allTables = tablePos.equals("*");

        this.setStartEndParagraph("*", tableParaRef);
        this.tableArea = ResourceHelper.evalTableReference(reference);

        List<Table> tables = new LinkedList<Table>();
        Range docRange = doc.getRange();
        TableIterator tableIterator = new TableIterator(docRange);
        // get all tables
        while (tableIterator.hasNext()) {
            tables.add(tableIterator.next());
        }

        if (allTables) {
            valList.setBaseURI("/" + resID + "/" + requestType + "/");
        } else {
            // get table and position of table
            Integer tPos = Integer.parseInt(tablePos) - 1;
            Table t = tables.get(tPos);
            tables.clear();
            tables.add(t);
            valList.setBaseURI("/" + resID + "/" + requestType + "/" + tablePos + "/");
        }

        Integer tPos = 1;
        for (Table t : tables) {
            String pathPrefix;
            if (allTables) {
                pathPrefix = tPos + "/";
            } else {
                pathPrefix = "";
            }
            valList.addValues(processTable(t, pathPrefix));
            tPos++;
        }

        return valList;
    }

    /**
     * Processing of text requests.
     * 
     * @param doc word document
     * @param paragraphRef reference to paragraphs
     * @return list of text values as {@code LinkedList<Value>}
     */
    private LinkedList<Value> processParagraphRequest(HWPFDocument doc, String paraRef) {
        LinkedList<Value> valList = new LinkedList<Value>();
        Range docRange = doc.getRange();

        setStartEndParagraph(paraRef, "*");
        if (this.startPara == -1 || this.endPara == -1) {
            this.startPara = 0;
            this.endPara = docRange.numParagraphs() - 1;
        }

        // -1 => all paragraphs requested
        if (this.startPara < docRange.numParagraphs()) {
            if (!(this.endPara < docRange.numParagraphs())) {
                this.endPara = docRange.numParagraphs() - 1;
            }

            for (Integer pPos = this.startPara; pPos <= this.endPara; pPos++) {
                Paragraph p = docRange.getParagraph(pPos);
                Integer subURI = pPos + 1;
                valList.add(getParagraph(p, subURI.toString()));
            }
        }

        return valList;
    }

    /**
     * Get a single paragraph of Word 97 - 2003 document
     * 
     * @param paragraph HWPF paragraph to process
     * @param paragraphRef position of paragraph
     * @return value of paragraph as {@code Value}
     */
    private Value getParagraph(Paragraph paragraph, String paragraphRef) {
        Value singleVal = new Value();

        // Word 97-2003 does not separate between text paragraphs and tables,
        // ignore paragraphs which are part of a table
        if (!paragraph.isInTable()) {
            String value = paragraph.text();
            // remove XML incompatible control sequences used by old word 97
            // format
            value = value.replaceAll("[\u0000-\u0009]", "");
            value = value.replaceAll("\u000B", "\n");
            value = value.replaceAll("[\u000C-\u001f]", "");

            singleVal.setValue(value.trim());
            singleVal.setSubURI(paragraphRef);
            singleVal.setType("xs:string");
        }

        return singleVal;
    }

    /**
     * Process table and get values.
     * 
     * @param table table
     * @param pathPrefix prefix of subURI
     * @return list of values as {@code LinkedList<Value>}
     */
    private LinkedList<Value> processTable(Table table, String pathPrefix) {
        LinkedList<Value> valList = new LinkedList<Value>();

        // temporary values for each table
        Integer rStart = this.tableArea.getRowStart();
        Integer rEnd = this.tableArea.getRowEnd();
        Integer cStart = this.tableArea.getColStart();
        Integer cEnd = this.tableArea.getColEnd();

        // -1 => all rows requested (column)
        if (this.tableArea.getRowStart() == -1 || this.tableArea.getRowEnd() == -1) {
            rStart = 0;
            rEnd = table.numRows() - 1;
        }

        for (int i = rStart; (i <= rEnd) && (i < table.numRows()); i++) {
            TableRow r = table.getRow(i);

            if (r != null) {
                // -1 => all columns requested (row)
                if (this.tableArea.getColStart() == -1 || this.tableArea.getColEnd() == -1) {
                    cStart = 0;
                    cEnd = r.numCells() - 1;
                }

                for (int j = cStart; (j <= cEnd) && (j < r.numCells()); j++) {
                    TableCell c = r.getCell(j);
                    if (c != null) {
                        String newPathPrefix = pathPrefix + ResourceHelper.convertColNumToColRef(j)
                                + (i + 1) + "/";
                        valList.addAll(getTableCell(c, newPathPrefix));
                    }
                }
            }
        }
        return valList;
    }

    /**
     * Process cell and get value.
     * 
     * @param cell cell
     * @param pathPrefix prefix for SubURI
     * @return list of table cell paragraph values as {@code LinkedList<Value>}
     */
    private LinkedList<Value> getTableCell(TableCell cell, String pathPrefix) {
        LinkedList<Value> valList = new LinkedList<Value>();

        // temporary values for each cell
        Integer pStart = startTablePara;
        Integer pEnd = endTablePara;

        // -1 => all paragraphs requested
        if (this.startTablePara == -1 || this.endTablePara == -1) {
            pStart = 0;
            pEnd = cell.numParagraphs() - 1;
        }

        if (pStart < cell.numParagraphs()) {
            if (!(pEnd < cell.numParagraphs())) {
                pEnd = cell.numParagraphs() - 1;
            }

            for (Integer pPos = pStart; pPos <= pEnd; pPos++) {
                String cellVal = cell.getParagraph(pPos).text().trim();
                if (!cellVal.isEmpty()) {
                    // remove XML incompatible control sequences used by old
                    // word 97 format
                    cellVal = cellVal.replaceAll("[\u0000-\u0009]", "");
                    cellVal = cellVal.replaceAll("\u000B", "\n");
                    cellVal = cellVal.replaceAll("[\u000C-\u001f]", "");

                    Value value = new Value();
                    value.setValue(cellVal.trim());
                    value.setSubURI(pathPrefix + (pPos + 1));
                    value.setType("xs:string");
                    valList.add(value);
                }
            }
        }
        return valList;

    }

    /**
     * Set start and end index for paragraphs according to reference Strings.
     * 
     * @param paraRef - reference to text paragraphs
     * @param tableParaRef - reference to paragraphs inside tables
     */
    private void setStartEndParagraph(String paraRef, String tableParaRef) {
        if (paraRef.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            // request of a group of paragraphs (URI values are one based!)
            String[] refTokens = paraRef.replaceAll("(?<=\\p{L})(?=\\d)",
                    pandaSettings.getRangeDelimiterChar()).split(
                    pandaSettings.getRangeDelimiterChar());
            this.startPara = Integer.valueOf(refTokens[0]) - 1;
            this.endPara = Integer.valueOf(refTokens[1]) - 1;

        } else if (paraRef.equals("*")) {
            // request of whole document including all paragraphs and tables
            this.startPara = this.endPara = -1;
        } else {
            // request of a certain paragraph (URI value is 1 based!)
            this.startPara = this.endPara = Integer.valueOf(paraRef) - 1;
        }

        if (tableParaRef.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            // request of a group of paragraphs (URI values are one based!)
            String[] refTokens = tableParaRef.replaceAll("(?<=\\p{L})(?=\\d)",
                    pandaSettings.getRangeDelimiterChar()).split(
                    pandaSettings.getRangeDelimiterChar());
            this.startTablePara = Integer.valueOf(refTokens[0]) - 1;
            this.endTablePara = Integer.valueOf(refTokens[1]) - 1;

        } else if (tableParaRef.equals("*")) {
            // request of whole document including all paragraphs and tables
            this.startTablePara = this.endTablePara = -1;
        } else {
            // request of a certain paragraph (URI value is 1 based!)
            this.startTablePara = this.endTablePara = Integer.valueOf(tableParaRef) - 1;
        }
    }

}
