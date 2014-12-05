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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;

import com.ximpleware.*;

import de.fuberlin.panda.data.caching.ResourceCache;
import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;

/**
 * Class for retrieval of data from XML documents according to request URI.
 * 
 * @author Christoph Schröder
 */
public class DataXmlResource {

    /**
     * Main method of this class to extract data from XML document via XPath.
     * 
     * @param uriInfo Jersey URI object
     * @param pandaSettings PANDA system settings
     * @return result as XML String
     * @throws WebApplicationException HTTP exception
     */
    public String getXml(UriInfo uriInfo, PandaSettings pandaSettings)
            throws WebApplicationException {

        String xmlRet = "";

        // Get List of all Path Segments
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        // Get Request parameters from URI
        String resID = pathSegments.get(1).getPath();

        // Get local path to file
        ResourceInfo resInfo = pandaSettings.getResourceMap().getMap().get(resID);
        String filePath = resInfo.getFilePath();

        // Get resource cache
        ResourceCache cache = pandaSettings.getResourceCache();

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
            byte[] doc = null;
            // lookup if document in the cache if caching is on
            if (pandaSettings.getServerCacheUsage() && cache.checkValidityResource(resID, eTag)) {
                doc = (byte[]) cache.getDocument(resID);
            } else {
                InputStream xmlDoc;
                if (filePath != null) {
                    xmlDoc = new FileInputStream(file);
                } else {
                    // Get online resource
                    URL resURL = pandaSettings.getResourceMap().getMap().get(resID).getURL();
                    xmlDoc = getOnlineResource(resURL);
                }
                doc = IOUtils.toByteArray(xmlDoc);

                // add document to cache if not already done
                if (pandaSettings.getServerCacheUsage()
                        && !cache.checkValidityResource(resID, eTag)) {
                    cache.addDocument(doc, resID, eTag);
                }
            }

            xmlRet = getXml(doc, xpathExp, pandaSettings, filePath);
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(404);
        }

        return xmlRet;
    }

    /**
     * Evaluate XPath expression for XML document, used by other DataResource
     * classes
     * 
     * @param document tidy (XML conform) HTML document as byte array
     * @param xpathExp XPath expression
     * @param pandaSettings PANDA system settings
     * @param filePath path to XML file
     * @return result as XML String
     * @throws WebApplicationException HTTP exception
     */
    public String getXml(byte[] document, String xpathExp, PandaSettings pandaSettings,
            String filePath) throws WebApplicationException {

        String xmlRet = "";

        try {
            String elements = "";
            VTDGen vg = new VTDGen();
            VTDNav vtdNav = null;

            // parse XML file
            vg.setDoc(document);
            vg.parse(pandaSettings.getNamespaceAwareness());
            vtdNav = vg.getNav();

            AutoPilot ap = new AutoPilot(vtdNav);
            String nameSpaces = "";
            // set namespace prefixes
            if (pandaSettings.getNamespaceAwareness()) {
                nameSpaces = setNameSpaces(vtdNav, ap);
            }

            // set XPath expression
            ap.selectXPath(xpathExp);
            elements = evalXpath(ap, vtdNav, xpathExp);

            // 404 if no elements found
            if (elements.length() == 0) {
                throw new WebApplicationException(404);
            }

            xmlRet = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<PANDA"
                    + nameSpaces + ">\n" + elements + "</PANDA>";
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(404);
        }

        return xmlRet;
    }

    /**
     * Evaluate XPath expression.
     * 
     * @param ap VTD AutoPilot for XPath evaluation
     * @param vtdNav VTD navigator
     * @param xpathExp XPath expression
     * @return result as XML String
     * @throws NavException
     * @throws XPathEvalException
     */
    private String evalXpath(AutoPilot ap, VTDNav vtdNav, String xpathExp) throws NavException,
            XPathEvalException {
        String xmlRet = "";

        // evaluate XPath expression
        StringBuilder elements = new StringBuilder();
        while (ap.evalXPath() != -1) {
            int tokenType = vtdNav.getTokenType(vtdNav.getCurrentIndex());
            // attribute node
            if (tokenType == VTDNav.TOKEN_ATTR_NAME) {
                String attrVal = vtdNav.toRawString(vtdNav.getCurrentIndex() + 1).trim();
                elements.append("\t<value type=\"xs:string\">" + attrVal + "</value>\n");
            }
            // text node
            else if (tokenType == VTDNav.TOKEN_CHARACTER_DATA) {
                String textVal = vtdNav.toRawString(vtdNav.getCurrentIndex()).trim();
                elements.append("\t<value type=\"xs:string\">" + textVal + "</value>\n");
            }
            // CDATA Section
            else if (tokenType == VTDNav.TOKEN_CDATA_VAL) {
                String textVal = formatElementString(vtdNav.toRawString(vtdNav.getCurrentIndex()),
                        false).trim();
                elements.append("\t<value type=\"xs:string\"><![CDATA[" + textVal + "]]></value>\n");
            }
            // element node
            else {
                // get index and length of current element and its content
                // (encoded in long value)
                long l = vtdNav.getContentFragment();
                // get element content as String
                String element = vtdNav.toRawString((int) l, (int) (l >> 32));
                // remove processing instruction if document node
                if (tokenType == VTDNav.TOKEN_DOCUMENT) {
                    element = element.trim();
                    // remove processing instruction and DTD
                    if (element.startsWith("<?") || element.startsWith("<!"))
                        element = element.substring(element.indexOf(">") + 1);
                }
                // format String and add to return value
                elements.append("\t<value type=\"xs:string\">\n"
                        + formatElementString(element, true) + "\n\t</value>\n");
            }
        }
        xmlRet = elements.toString();

        return xmlRet;
    }

    /**
     * Declaration of namespace, be aware that duplicates of prefixes might not
     * work with VTDxml since the declared namespace will be overwritten
     * 
     * @param vtdNav VTD XML navigator for navigation inside tree
     * @param ap AutoPilot for automatic XPath evaluation
     * @throws NavException
     */
    private String setNameSpaces(VTDNav vtdNav, AutoPilot ap) throws NavException {
        String nsDeclaration = "";
        Map<String, String> nameSpaces = new TreeMap<String, String>();
        // number of all tokens
        int tokenCount = vtdNav.getTokenCount();
        // check all tokens for namespace declarations
        for (int i = 0; i < tokenCount; i++) {
            if (vtdNav.getTokenType(i) == VTDNav.TOKEN_ATTR_NS) {
                String token = vtdNav.toNormalizedString(i);
                if (!nameSpaces.containsKey(token)) {
                    String prefix = token.substring(token.indexOf(":") + 1);
                    String uri = vtdNav.toNormalizedString(i + 1);
                    ap.declareXPathNameSpace(prefix, uri);
                    nameSpaces.put(token, uri);
                }
            }
        }

        for (String ns : nameSpaces.keySet()) {
            nsDeclaration += " " + ns + "=\"" + nameSpaces.get(ns) + "\"";
        }

        return nsDeclaration;
    }

    /**
     * Formats the String value of a selected node and it's content. Removes
     * unnecessary tabs or adds missing tabs for correct indentation (Note:
     * assumes that indentation in source document was correct)
     * 
     * @param element String value of node + content
     * @return formated element as XML String
     */
    private String formatElementString(String element, boolean elementNode) {
        // omit empty elements
        if (element.length() == 0)
            return element;

        String retString = element;
        // remove leading linefeed
        if (retString.startsWith("\n")) {
            retString = retString.replaceFirst("\n", "");
        }

        // separate into lines
        String[] lines = retString.split("\n");
        if (lines.length >= 1) {
            StringBuilder nElement = new StringBuilder();
            int fN = 0;
            // get index of first node, should be > zero only if mixed content
            // xml
            if (elementNode) {
                boolean fNFound = false;
                for (String line : lines) {
                    if (line.trim().startsWith("<")) {
                        fNFound = true;
                        break;
                    }
                    fN++;
                }
                if (!fNFound)
                    fN = 0;
            }

            // get number of leading tabs
            int tabs = 0;
            char[] firstNode = lines[fN].toCharArray();
            for (char c : firstNode) {
                if (c == 9)
                    tabs++;
                else
                    break;
            }

            // remove tabs
            if (tabs > 2) {
                // build remove sequence, omit 2 tabs for root node and value
                // node
                String remove = "";
                for (int i = 0; i < (tabs - 2); i++) {
                    remove += "\t";
                }

                // remove leading tabs
                for (String line : lines) {
                    if (line.startsWith(remove)) {
                        nElement.append(line.replaceFirst(remove, "") + "\n");
                    } else {
                        nElement.append(line + "\n");
                    }
                }
            }
            // add tabs
            else if (tabs < 2) {
                // build add sequence, there should be at least 2 leading tabs
                // for root and value node
                String add = "";
                for (int i = 0; i < (2 - tabs); i++) {
                    add += "\t";
                }

                // add leading tabs
                for (String line : lines) {
                    nElement.append(add + line + "\n");
                }
            } else {
                nElement.append(retString);
            }
            // build String and remove unnecessary whitespace at end
            retString = nElement.toString().replaceFirst("\\s+$", "");
            // retString = nElement.toString();
        }
        return retString;
    }

    /**
     * Retrieve online resource via URL.
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
