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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.util.PDFTextStripper;

import de.fuberlin.panda.api.data.ValueExchange.Value;
import de.fuberlin.panda.api.data.ImageData;
import de.fuberlin.panda.api.data.ValueExchangeExt;
import de.fuberlin.panda.data.caching.ResourceCache;
import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMap;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;
import de.fuberlin.panda.enums.DataResourceType;

/**
 * Class for retrieval of data from PDF documents according to request URI.
 * 
 * @author Christoph Schröder
 */
public class DataPdfResource {
    private PandaSettings pandaSettings;

    private String        encoding      = "UTF-16LE";
    private String        lineSeparator = "\n";

    // start and end index for requested pages/lines, -1 = all pages/lines
    private Integer       startPage     = -1;
    private Integer       endPage       = -1;
    private Integer       startLine     = -1;
    private Integer       endLine       = -1;

    /**
     * Main method of this class to extract text data from PDF document.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return list of values as {@code ValueExchangeExt}
     * @throws WebApplicationException HTTP exception
     */
    public ValueExchangeExt getPdfTextContent(UriInfo uriInfo, PandaSettings pandaSettings)
            throws WebApplicationException {
        this.pandaSettings = pandaSettings;

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        // Get Request parameters from URI
        String resID = pathSegments.get(1).getPath();
        String requestType = pathSegments.get(2).getPath();
        String pageRef = pathSegments.get(3).getPath();
        String lineRef = pathSegments.get(4).getPath();

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
                PDDocument pdDoc = PDDocument.load(file);
                if (pdDoc.isEncrypted()) {
                    throw new WebApplicationException(404);
                }
                valList = getPdfText(pdDoc, pageRef, lineRef);
                valList.setBaseURI("/" + resID + "/" + requestType + "/" + valList.getBaseURI());
                pdDoc.close();

                // add resource to cache if not already done
                if (pandaSettings.getServerCacheUsage()
                        && !cache.checkValidityResource(resID, eTag)) {
                    ValueExchangeExt newCacheValues = getPdfText(pdDoc, "*", "*");
                    newCacheValues.setBaseURI("/" + resID + "/" + requestType + "/");
                    cache.addResourceValues(newCacheValues, resInfo.getType(), resID, eTag);
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
     * Main method of this class to extract a picture from PDF document.
     * 
     * @param uriInfo Jersey URI object
     * @param resourceMap map of resources
     * @return picture as {@code ImageData}
     * @throws WebApplicationException HTTP exception
     */
    public ImageData getPicture(UriInfo uriInfo, ResourceMap resourceMap)
            throws WebApplicationException {
        byte[] imageData = null;
        String mimeType = new String();

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        String resID = pathSegments.get(1).getPath();
        String pageRef = pathSegments.get(3).getPath();
        String imageID = pathSegments.get(4).getPath();
        // Get local path to file
        String filePath = resourceMap.getMap().get(resID).getFilePath();
        try {
            File file = new File(filePath);

            PDDocument pdDoc = PDDocument.load(file);
            // get a list of all pages
            List<?> pages = pdDoc.getDocumentCatalog().getAllPages();
            // get the page object of requested page
            PDPage page = (PDPage) pages.get(Integer.valueOf(pageRef) - 1);
            // get resources of this page
            PDResources resources = page.getResources();
            // create a map of those objects, use TreeMap for sorted access by
            // index
            TreeMap<String, PDXObject> xObjectMap = new TreeMap<String, PDXObject>();
            xObjectMap.putAll(resources.getXObjects());
            // counter for image type xObjects, start at 1 for one based access
            int count = 1;
            Integer iID = Integer.valueOf(imageID);
            // iterate xObjects
            for (String key : xObjectMap.keySet()) {
                PDXObject xObject = xObjectMap.get(key);
                if (xObject instanceof PDXObjectImage) {
                    if (count == iID) {
                        PDXObjectImage pdImage = (PDXObjectImage) xObject;
                        mimeType = "image/" + pdImage.getSuffix();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        pdImage.write2OutputStream(out);
                        imageData = out.toByteArray();
                        break;
                    }
                    count++;
                }
            }
            pdDoc.close();
            if (imageData == null) {
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
     * Method that extracts lines from text from referenced pages.
     * 
     * @param pdDoc PDF document
     * @param pageRef reference to page/pages
     * @param lineRef reference to line/lines
     * @return list of values as {@code ValueExchangeExt}
     * @throws IOException
     */
    private ValueExchangeExt getPdfText(PDDocument pdDoc, String pageRef, String lineRef)
            throws IOException {
        setStartEndIndexes(pageRef, lineRef);
        ValueExchangeExt valList = new ValueExchangeExt();
        Integer numPages = pdDoc.getNumberOfPages();

        if (startPage <= numPages) {
            boolean singlePage = false;
            if (startPage == endPage && startPage != -1) {
                singlePage = true;
                valList.setBaseURI(startPage.toString() + "/");
            } else {
                valList.setBaseURI("");
            }

            // get index of first/last requested page
            int fP = startPage;
            int lP;
            if (startPage == -1) {
                fP = 1;
                lP = numPages;
            } else if (endPage > numPages) {
                lP = numPages;
            } else {
                lP = endPage;
            }

            PDFTextStripper stripper = new PDFTextStripper(encoding);
            stripper.setLineSeparator(lineSeparator);

            for (Integer page = fP; page <= lP; page++) {
                // set start and end page, 1 based!
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageContent = stripper.getText(pdDoc);
                String[] lines = pageContent.split("\n");

                if (startLine <= lines.length) {
                    // get index of first/last requested page, start/end inputs
                    // are 1 based!
                    int fL = startLine;
                    int lL;
                    if (startLine == -1) {
                        fL = 1;
                        lL = lines.length;
                    } else if (endLine > lines.length) {
                        lL = lines.length;
                    } else {
                        lL = endLine;
                    }

                    // get content of page
                    for (Integer line = fL; line <= lL; line++) {
                        Value lineContent = new Value();
                        String content = lines[line - 1];
                        lineContent.setValue(content);
                        if (singlePage) {
                            lineContent.setSubURI(line.toString());
                        } else {
                            lineContent.setSubURI(page.toString() + "/" + line.toString());
                        }
                        lineContent.setType("xs:string");
                        valList.addValue(lineContent);
                    }
                }
            }
        }

        return valList;
    }

    /**
     * Calculation of start/end index of pages and lines.
     * 
     * @param pageRef reference to page/pages
     * @param lineRef reference to line/lines
     */
    private void setStartEndIndexes(String pageRef, String lineRef) {
        // request to all pages
        if (pageRef.equals("*")) {
            startPage = -1;
            endPage = -1;
        }
        // request to multiple pages
        else if (pageRef.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            String[] refTokens = pageRef.split(pandaSettings.getRangeDelimiterChar());
            startPage = Integer.valueOf(refTokens[0]);
            endPage = Integer.valueOf(refTokens[1]);
            // start should be smaller or even to end
            if (endPage < startPage) {
                Integer temp = startPage;
                startPage = endPage;
                endPage = temp;
            }
        }
        // request to single Page
        else {
            startPage = endPage = Integer.valueOf(pageRef);
        }

        // request to all lines
        if (lineRef.equals("*")) {
            startLine = -1;
            endLine = -1;
        }
        // request to multiple lines
        else if (lineRef.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            String[] refTokens = lineRef.split(pandaSettings.getRangeDelimiterChar());
            startLine = Integer.valueOf(refTokens[0]);
            endLine = Integer.valueOf(refTokens[1]);
        }
        // request to single line
        else {
            startLine = endLine = Integer.valueOf(lineRef);
        }
    }
}
