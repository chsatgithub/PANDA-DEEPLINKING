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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;

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
public class DataWordVtdResource {
    private PandaSettings pandaSettings;
    private Integer       startTablePara, endTablePara, startPara, endPara;
    private TableArea     tableArea;

    /**
     * Main method of this class to extract text data from DOCX document.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return list of values as {@code ValueExchangeExt}
     * @throws WebApplicationException HTTP exception
     */
    public ValueExchangeExt getTextParagraphsXML(UriInfo uriInfo, PandaSettings pandaSettings)
            throws WebApplicationException {
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
                // Handling of Word 2007 or newer (docx) documents
                if (resourceType.equals(DataResourceType.DOCX)) {

                    // Open Word file
                    OPCPackage pkg = OPCPackage.open(file);

                    // get /word/document.xml
                    PackageRelationshipCollection rels = pkg
                            .getRelationshipsByType(PackageRelationshipTypes.CORE_DOCUMENT);
                    PackageRelationship docRel = rels.getRelationship(0);
                    PackagePart docPP = pkg.getPart(docRel);
                    byte[] document = IOUtils.toByteArray(docPP.getInputStream());

                    valList = processTextParagraphs(document, resID, requestType, paragraphRef);

                    // add resource to cache if not already done
                    if (pandaSettings.getServerCacheUsage()
                            && !cache.checkValidityResource(resID, eTag)) {
                        ValueExchangeExt textCacheValues = processTextParagraphs(document, resID,
                                "text", "*");
                        ValueExchangeExt tableCacheValues = processTableParagraphs(document, resID,
                                "tables", "*", "*", "*");
                        cache.addResourceValues(textCacheValues, resInfo.getType(), resID, eTag);
                        cache.addResourceValues(tableCacheValues, resInfo.getType(), resID, eTag);
                    }

                    // use revert instead of close (read only)
                    pkg.revert();
                } else {
                    throw new WebApplicationException(404);
                }
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(404);
        }

        // return collected values
        return valList;
    }

    /**
     * Main method of this class to extract table data from DOCX document.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return list of values as {@code ValueExchangeExt}
     * @throws WebApplicationException HTTP exception
     */
    public ValueExchangeExt getTableParagraphsXML(UriInfo uriInfo, PandaSettings pandaSettings)
            throws WebApplicationException {
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

        // List of gathered values for response
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
                // Handling of Word 2007 or newer (docx) documents
                if (resourceType.equals(DataResourceType.DOCX)) {

                    // Open OOXML Package
                    OPCPackage pkg = OPCPackage.open(file);

                    // get /word/document.xml
                    PackageRelationshipCollection rels = pkg
                            .getRelationshipsByType(PackageRelationshipTypes.CORE_DOCUMENT);
                    PackageRelationship docRel = rels.getRelationship(0);
                    PackagePart docPP = pkg.getPart(docRel);
                    byte[] document = IOUtils.toByteArray(docPP.getInputStream());
                    pkg.close();

                    // process word document and get a list of requested values
                    valList = processTableParagraphs(document, resID, requestType, tablePos,
                            cellRef, paragraphRef);

                    // add resource to cache if not already done
                    if (pandaSettings.getServerCacheUsage()
                            && !cache.checkValidityResource(resID, eTag)) {
                        ValueExchangeExt textCacheValues = processTextParagraphs(document, resID,
                                "text", "*");
                        ValueExchangeExt tableCacheValues = processTableParagraphs(document, resID,
                                "tables", "*", "*", "*");
                        cache.addResourceValues(textCacheValues, resInfo.getType(), resID, eTag);
                        cache.addResourceValues(tableCacheValues, resInfo.getType(), resID, eTag);
                    }
                } else {
                    throw new WebApplicationException(404);
                }
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            // 404 in case of exception (e.g. NullPointerException if requested
            // paragraph does not exist)
            e.printStackTrace();
            throw new WebApplicationException(404);
        }

        // return collected values
        return valList;
    }

    /**
     * Main method of this class to extract a picture DOCX document.
     * 
     * @param uriInfo URI of request.
     * @param resourceMap resource map with information about requested
     *            resource.
     * @return Response Jersey Response
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
            // Handling of Word 2007 or newer (docx) documents
            if (resourceType.equals(DataResourceType.DOCX)) {
                // Open OOXML Package
                OPCPackage pkg = OPCPackage.open(file);

                // regex pattern to get media files
                Pattern p = Pattern.compile("/word/media/.*");
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
     * Processing of requests to text paragraphs
     * 
     * @param document main XML document as byte array
     * @param resID resource ID to set BaseURI
     * @param paragraphRef position of requested paragraph (start at 0)
     * @return list of values as {@code ValueExchangeExt}
     * @throws EncodingException
     * @throws EOFException
     * @throws EntityException
     * @throws ParseException
     * @throws XPathParseException
     * @throws NavException
     * @throws XPathEvalException
     */
    private ValueExchangeExt processTextParagraphs(byte[] document, String resID,
            String requestType, String paraRef) throws EncodingException, EOFException,
            EntityException, ParseException, XPathParseException, XPathEvalException, NavException {
        ValueExchangeExt valList = new ValueExchangeExt();
        valList.setBaseURI("/" + resID + "/" + requestType + "/");
        VTDGen vg = new VTDGen();
        vg.setDoc(document);
        vg.parse(true);
        VTDNav vn = vg.getNav();
        AutoPilot ap = new AutoPilot(vn);
        declareNameSpaces(ap);

        setStartEndParagraph(paraRef, "*");

        // element list for evaluation of subtree
        ArrayList<String> elements = new ArrayList<String>();
        elements.add("w:p");

        // list of according indexes of subtree elements, "*" if all elements
        ArrayList<EvalRange> elementIndex = new ArrayList<EvalRange>();
        elementIndex.add(new EvalRange(this.startPara, this.endPara));

        // XPath expression for evaluation
        String xpathExp = "/w:document/w:body";
        ap.selectXPath(xpathExp);
        // prefix to build subURI
        String pathPrefix = new String();
        while (ap.evalXPath() != -1) {
            // evaluate subtree of selected node
            valList.addValues(evalSubTree(ap, vn, elements, elementIndex, 0, pathPrefix));
        }

        return valList;
    }

    /**
     * Processing of requests to tables.
     * 
     * @param document word document
     * @param resID unique ID of resource
     * @param tablePos position of requested table (1 based)
     * @param cellRef reference to cell or table (e.g. A1 or B1:C4)
     * @param paragraphPos position of requested paragraph
     * @return list of values as {@code ValueExchangeExt}
     * @throws EncodingException
     * @throws EOFException
     * @throws EntityException
     * @throws ParseException
     * @throws XPathEvalException
     * @throws NavException
     * @throws XPathParseException
     */
    private ValueExchangeExt processTableParagraphs(byte[] document, String resID,
            String requestType, String tablePos, String cellRef, String paragraphRef)
            throws EncodingException, EOFException, EntityException, ParseException,
            XPathEvalException, NavException, XPathParseException {
        ValueExchangeExt valList = new ValueExchangeExt();

        VTDGen vg = new VTDGen();
        vg.setDoc(document);
        vg.parse(true);
        VTDNav vn = vg.getNav();
        AutoPilot ap = new AutoPilot(vn);
        declareNameSpaces(ap);

        // get parameters to determine operation
        Boolean allTables = tablePos.equals("*");
        this.setStartEndParagraph("*", paragraphRef);
        this.tableArea = ResourceHelper.evalTableReference(cellRef);

        // element list for evaluation of subtree
        ArrayList<String> elements = new ArrayList<String>();
        elements.add("w:tr");
        elements.add("w:tc");
        elements.add("w:p");

        // list of according indexes of subtree elements, "*" if all elements
        ArrayList<EvalRange> elementIndex = new ArrayList<EvalRange>();
        elementIndex.add(new EvalRange(this.tableArea.getRowStart(), this.tableArea.getRowEnd()));
        elementIndex.add(new EvalRange(this.tableArea.getColStart(), this.tableArea.getColEnd()));
        elementIndex.add(new EvalRange(this.startTablePara, this.endTablePara));

        // prefix to build subURI
        String pathPrefix = "";
        // XPath expression for evaluation
        String xpathExp = new String();

        if (allTables) {
            valList.setBaseURI("/" + resID + "/" + requestType + "/");
            // request from all Tables:
            xpathExp = "/w:document/w:body/w:tbl";
        } else {
            valList.setBaseURI("/" + resID + "/" + requestType + "/" + tablePos + "/");
            // request to a single table
            String tblPos = String.valueOf((Integer.parseInt(tablePos)));
            xpathExp = "/w:document/w:body/w:tbl[" + tblPos + "]";
            // adjust XPath expression for row requests to use AutoPilot for row
            // selection
            if (this.tableArea.getColStart() == -1 && this.tableArea.getColEnd() == -1
                    && this.tableArea.getRowStart() == this.tableArea.getRowEnd()
                    && this.tableArea.getRowStart() != -1) {
                // request of certain row of one table
                String tblRow = String.valueOf((this.tableArea.getRowStart() + 1));
                xpathExp = "/w:document/w:body/w:tbl[" + tblPos + "]/w:tr[" + tblRow + "]";
                // remove "w:tr" since it will be evaluated by AutoPilot
                elementIndex.remove(0);
                elements.remove(0);
            }

        }

        ap.selectXPath(xpathExp);
        // table counter
        Integer tblPos = 1;
        // evaluate XPath expression
        while (ap.evalXPath() != -1) {
            // add table to local path if request to all tables
            if (allTables) {
                pathPrefix = tblPos.toString();
            }
            // evaluate subtree of selected nodes
            valList.addValues(evalSubTree(ap, vn, elements, elementIndex, 0, pathPrefix));
            tblPos++;
        }
        return valList;
    }

    /**
     * Manual navigation inside XML tree according to given elements and
     * indexes. Sets value to text node of last element in elements list. Counts
     * elements to set sub-URI.
     * 
     * @param ap VTD autopilot for XPath evaluation
     * @param vn VTD navigator
     * @param elements list of elements to traverse
     * @param elementIndex list of indexes of elements, if "*" all elements will
     *            be traversed.
     * @param depthLevel current level of traversing in subtree, start with 0.
     * @param pathPrefix prefix used for subURI
     * @param textValue used to build value from more than one node since the
     *            chosen granularity only know w:p paragraphs while w:r and
     *            their w:t elements are combined as value for the whole
     *            paragraph.
     * @return list of values as {@code LinkedList<Value>}
     * @throws XPathEvalException
     * @throws NavException
     */
    private LinkedList<Value> evalSubTree(AutoPilot ap, VTDNav vn, ArrayList<String> elements,
            ArrayList<EvalRange> elementIndex, int depthLevel, String pathPrefix)
            throws XPathEvalException, NavException {

        LinkedList<Value> valList = new LinkedList<Value>();
        if (vn.toElement(VTDNav.FIRST_CHILD)) {
            // counter for requested elements with same name
            Integer posCount;
            // number of elements to skip
            Integer skipElements;

            // set skipElements, if 0 cursor will always be moved to the next
            // element with elementName
            if (elementIndex.get(depthLevel).getStart() == -1) {
                skipElements = 0;
                posCount = 1;
            } else {
                skipElements = elementIndex.get(depthLevel).getStart();
                posCount = elementIndex.get(depthLevel).getStart() + 1;
            }

            // iterate the siblings or move to certain sibling at given position
            while (moveToSibling(vn, elements.get(depthLevel), skipElements)) {
                // set skip to 0 for further evaluation
                skipElements = 0;
                String path = new String();

                // concatenate pathPrefix and current index to current path
                if (pathPrefix.isEmpty()) {
                    path = posCount.toString();
                } else {
                    // convert "w:tc" and "w:tr" path segment to excel like cell
                    // reference (e.g. A1)
                    if (elements.get(depthLevel).equals("w:tc")) {
                        String colRef = ResourceHelper.convertColNumToColRef(posCount - 1);
                        String[] pathSegments = pathPrefix.split("(?<=/)|(?=/)");
                        pathSegments[pathSegments.length - 1] = colRef
                                + pathSegments[pathSegments.length - 1];
                        for (String s : pathSegments) {
                            path += s;
                        }
                    } else {
                        path = pathPrefix + "/" + posCount.toString();
                    }
                }

                // set new value if current element is a paragraph (all text
                // runs w:r will be combined as one paragraph value)
                if (elements.get(depthLevel).equals("w:p")) {
                    // used to combine values of text runs w:r
                    StringBuilder pValue = new StringBuilder();

                    // move to first child of paragraph
                    if (vn.toElement(VTDNav.FIRST_CHILD)) {
                        evalParagraph(vn, pValue);
                        // move back to "w:p" node
                        vn.toElement(VTDNav.PARENT);
                    }
                    String val = pValue.toString().trim();

                    // set attributes and add value to list
                    if (!val.isEmpty()) {
                        Value value = new Value();
                        value.setSubURI(path);
                        value.setType("xs:string");
                        value.setValue(val);
                        valList.add(value);
                    }
                } else {
                    // evaluate next level
                    valList.addAll(evalSubTree(ap, vn, elements, elementIndex, depthLevel + 1, path));
                }

                // stop if end reached
                if (!(elementIndex.get(depthLevel).getEnd() == -1)
                        && !((posCount - 1) < elementIndex.get(depthLevel).getEnd())) {
                    break;
                }

                // stop if no other siblings
                if (!vn.toElement(VTDNav.NEXT_SIBLING)) {
                    break;
                }
                posCount++;
            }
            // go back to parent for further traversing of subtree
            vn.toElement(VTDNav.PARENT);
        }
        return valList;
    }

    /**
     * Evaluation of paragraph nodes, traverses subtree of paragraph node and
     * appends all text segments to a paragraph value.
     * 
     * @param vn VTD navigator
     * @param text object for text concatenation
     * @throws NavException
     */
    private void evalParagraph(VTDNav vn, StringBuilder text) throws NavException {

        if (vn.matchTokenString(vn.getCurrentIndex(), "w:t")) {
            int tVal = vn.getText();
            if (tVal >= 0) {
                text.append(vn.toString(tVal));
            }
        }

        // note: this will only append a linebreak cha while softbreaks in
        // WordML can refer to
        // textwrap, page or column which can't be expressed in plain text
        if (vn.matchTokenString(vn.getCurrentIndex(), "w:br")) {
            text.append("\n");
        }

        // this will only append a tab char while tabulators in WordML can have
        // different properties
        if (vn.matchTokenString(vn.getCurrentIndex(), "w:tab")) {
            text.append("\t");
        }

        if (vn.toElement(VTDNav.FIRST_CHILD)) {
            evalParagraph(vn, text);
            vn.toElement(VTDNav.PARENT);
        }

        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            evalParagraph(vn, text);
        }
    }

    /**
     * Moves the cursor to an element (sibling) with given name and position
     * (the nth element with he same name starting to count at 0)
     * 
     * @param vtdNav vtdXML navigator
     * @param elementName name of element
     * @param skipElements index of element starting with 0
     * @return true if element found
     * @throws NavException
     */
    private boolean moveToSibling(VTDNav vtdNav, String elementName, Integer skipElements)
            throws NavException {
        boolean foundElement = false;
        // counter for elements with elementName
        Integer currentPosition = 0;
        // iterate siblings until element found or no other siblings
        do {
            // break if element with position found and return true
            if (vtdNav.matchTokenString(vtdNav.getCurrentIndex(), elementName)) {
                if (skipElements == currentPosition) {
                    foundElement = true;
                    break;
                }
                currentPosition++;
            }
        } while (vtdNav.toElement(VTDNav.NEXT_SIBLING));
        return foundElement;
    }

    /**
     * Set start and end index for paragraphs according to reference Strings.
     * 
     * @param paraRef reference to text paragraphs
     * @param tableParaRef reference to table paragraphs
     */
    private void setStartEndParagraph(String paraRef, String tableParaRef) {
        if (paraRef.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            // request of a group of paragraphs (URI values are one based!)
            String[] refTokens = paraRef.replaceAll("(?<=\\p{L})(?=\\d)",
                    pandaSettings.getRangeDelimiterChar()).split(pandaSettings.getRangeDelimiterChar());
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
                    pandaSettings.getRangeDelimiterChar()).split(pandaSettings.getRangeDelimiterChar());
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

    /**
     * Declares namespaces prefixes used in SpreadsheetML
     * 
     * @param ap - VTDXml Autopilot
     */
    private void declareNameSpaces(AutoPilot ap) {
        ap.declareXPathNameSpace("w",
                "http://schemas.openxmlformats.org/wordprocessingml/2006/main");
        ap.declareXPathNameSpace("wp",
                "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing");
        ap.declareXPathNameSpace("w10", "urn:schemas-microsoft-com:office:word");
        ap.declareXPathNameSpace("wne", "http://schemas.microsoft.com/office/word/2006/wordml");
        ap.declareXPathNameSpace("m", "http://schemas.openxmlformats.org/officeDocument/2006/math");
        ap.declareXPathNameSpace("v", "urn:schemas-microsoft-com:vml");
        ap.declareXPathNameSpace("r",
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships");
        ap.declareXPathNameSpace("o", "urn:schemas-microsoft-com:office:office");
        ap.declareXPathNameSpace("ve",
                "http://schemas.openxmlformats.org/markup-compatibility/2006");
    }

    /**
     * Java bean to wrap a range.
     * 
     * @author Christoph Schröder
     */
    private class EvalRange {
        private Integer start, end;

        EvalRange(Integer start, Integer end) {
            this.start = start;
            this.end = end;
        }

        Integer getStart() {
            return this.start;
        }

        Integer getEnd() {
            return this.end;
        }
    }
}
