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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.w3c.tidy.Tidy;

import de.fuberlin.panda.data.caching.ResourceCache;
import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;

/**
 * Class for retrieval of data from HTML documents according to request URI.
 * 
 * @author Christoph Schröder
 */
public class DataHtmlResource {

    /**
     * Main method of this class to extract data from HTML document via XPath.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return result as XML String
     * @throws WebApplicationException HTTP exception
     */
    public String getHtml(UriInfo uriInfo, PandaSettings pandaSettings)
            throws WebApplicationException {

        String xmlRet = "";

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();

        // Get Request parameters from URI
        String resID = pathSegments.get(1).getPath();

        // Get resource cache
        ResourceCache cache = pandaSettings.getResourceCache();

        // Get local path to file
        ResourceInfo resInfo = pandaSettings.getResourceMap().getMap().get(resID);
        String filePath = resInfo.getFilePath();

        // get XPath String from URI, note: using PathSegments doesn't work with
        // relative path
        String absURI = uriInfo.getAbsolutePath().getPath();
        String xpathExp = absURI.substring(absURI.indexOf(resID) + resID.length() + 1);

        try {
            File file = null;
            EntityTag eTag = null;

            if (filePath != null) {
                file = new File(filePath);
                Date lastModified = new Date(file.lastModified());
                eTag = new EntityTag(resID + "_" + lastModified.getTime(), false);
            }

            xpathExp = URLDecoder.decode(xpathExp, "UTF-8");
            // get tidy HTML document (XML)
            byte[] doc = null;
            // lookup if document in the cache if caching is on
            if (pandaSettings.getServerCacheUsage() && cache.checkValidityDocument(resID, eTag)) {
                doc = (byte[]) cache.getDocument(resID);
            } else {
                if (pandaSettings.getUseJtidy())
                    doc = getTidyHtml(pandaSettings, resID);
                else
                    doc = getCleanHtml(pandaSettings, resID);

                // add document to cache if not already done
                if (pandaSettings.getServerCacheUsage()
                        && !cache.checkValidityDocument(resID, eTag)) {
                    cache.addDocument(doc, resID, eTag);
                }
            }

            // create XML resource evaluator
            DataXmlResource xmlResource = new DataXmlResource();
            // evaluate XPath expression
            xmlRet = xmlResource.getXml(doc, xpathExp, pandaSettings, filePath);

            // 404 if no elements found
            if (xmlRet.length() == 0) {
                throw new WebApplicationException(404);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(404);
        }

        return xmlRet;
    }

    /**
     * Clean HTML document and return XML as byte array
     * 
     * @param resourceMap map of resources
     * @param resID unique ID of resource
     * @return clean XHTML document as {@code byte[]}
     * @throws IOException
     */
    private byte[] getTidyHtml(PandaSettings pandaSettings, String resID) throws IOException {
        byte[] doc = null;
        // Get local path to file, if null the URL field will be used to
        // retrieve resource
        ResourceInfo resInfo = pandaSettings.getResourceMap().getMap().get(resID);
        String filePath = resInfo.getFilePath();

        // properties for HTML cleaning
        Tidy tidy = new Tidy();
        // no output of warnings/errors
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);

        tidy.setHideEndTags(true);
        tidy.setInputEncoding("UTF-8");
        tidy.setOutputEncoding("UTF-8");
        tidy.setWraplen(Integer.MAX_VALUE);
        // set output to XML
        tidy.setXmlOut(true);

        // get HTML document, parse HTML
        InputStream htmlDoc = null;
        if (filePath != null) {
            htmlDoc = new FileInputStream(filePath);
        } else {
            // Get online resource
            URL resURL = pandaSettings.getResourceMap().getMap().get(resID).getURL();
            htmlDoc = getOnlineResource(resURL);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tidy.parse(htmlDoc, out);
        doc = out.toByteArray();

        return doc;
    }

    /**
     * Clean HTML document and return XML as byte array
     * 
     * @param resourceMap map of resources
     * @param resID unique ID of resource
     * @return clean XHTML document as {@code byte[]}
     * @throws IOException
     */
    private byte[] getCleanHtml(PandaSettings pandaSettings, String resID) throws IOException {
        byte[] doc = null;
        // Get local path to file, if null the URL field will be used to
        // retrieve resource
        ResourceInfo resInfo = pandaSettings.getResourceMap().getMap().get(resID);
        String filePath = resInfo.getFilePath();

        // properties for HTML cleaning
        CleanerProperties props = new CleanerProperties();
        // preserve namespace prefixes
        props.setNamespacesAware(true);
        // remove <?TAGNAME....> or <!TAGNAME....>
        props.setIgnoreQuestAndExclam(true);
        // do not split attributes with multiple words
        props.setAllowMultiWordAttributes(true);
        // omits <html> tag
        // props.setOmitHtmlEnvelope(true);
        // omit DTD
        props.setOmitDoctypeDeclaration(true);
        // omit xml declaration
        props.setOmitXmlDeclaration(true);
        // omit comments
        props.setOmitComments(true);
        // omit deprecated tags like <font...>
        props.setOmitDeprecatedTags(true);
        // treat script and style tag contents as CDATA
        props.setUseCdataForScriptAndStyle(true);
        // replace html character in form &#XXXX with real unicode characters
        props.setRecognizeUnicodeChars(true);
        // replace special entities with unicode character
        props.setTranslateSpecialEntities(true);
        // if true do not escape valid xml character sequences
        props.setAdvancedXmlEscape(true);

        // get HTML document, parse HTML
        TagNode tagNode = null;
        if (filePath != null) {
            tagNode = new HtmlCleaner(props).clean(new File(filePath));
        } else {
            // Get online resource
            URL resURL = pandaSettings.getResourceMap().getMap().get(resID).getURL();
            InputStream htmlDoc = getOnlineResource(resURL);
            tagNode = new HtmlCleaner(props).clean(htmlDoc);
        }

        PrettyXmlSerializer pXmlS = new PrettyXmlSerializer(props);
        doc = pXmlS.getAsString(tagNode).getBytes();

        return doc;
    }

    /**
     * Retrieve online located resource via URL.
     * 
     * @param resURL URL of resource
     * @return resource as {@code InputStream}
     */
    private InputStream getOnlineResource(URL resURL) {
        InputStream res;
        try {
            URLConnection resCon = resURL.openConnection();
            resCon.setReadTimeout(3000);
            resCon.setConnectTimeout(1000);
            resCon.connect();
            res = resCon.getInputStream();
        } catch (IOException e) {
            throw new WebApplicationException(404);
        }
        return res;
    }

}
