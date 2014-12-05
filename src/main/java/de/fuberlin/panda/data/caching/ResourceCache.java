package de.fuberlin.panda.data.caching;

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


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;

import de.fuberlin.panda.api.data.ValueExchange.Value;
import de.fuberlin.panda.api.data.ValueExchangeExt;
import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.data.resources.ResourceHelper;
import de.fuberlin.panda.data.resources.ResourceHelper.TableArea;
import de.fuberlin.panda.enums.DataResourceType;

/**
 * This is the main class of the PANDA server cache prototype. For resources
 * with static data structure informations the behavior of PANDA on real
 * documents is imitated while using a tree structure, which is leaned on the
 * hierarchical part of the URI. Documents with dynamically structure like XML
 * can be cached as whole binary array. Note: there is no cache invalidation
 * strategy implemented besides last modified date.
 * 
 * @author Christoph Schröder
 */
public class ResourceCache {

    private PandaSettings                    pandaSettings;

    private final CacheTreeNode              valueCache          = new CacheTreeNode();
    private final HashMap<String, Object>    docCache            = new HashMap<String, Object>();
    private final HashMap<String, EntityTag> eTags               = new HashMap<String, EntityTag>();
    private final ReentrantReadWriteLock     valueCacheLock      = new ReentrantReadWriteLock();
    private final Lock                       valueCacheWriteLock = valueCacheLock.writeLock();
    private final Lock                       valueCacheReadLock  = valueCacheLock.readLock();

    private final ReentrantReadWriteLock     docCacheLock        = new ReentrantReadWriteLock();
    private final Lock                       docCacheWriteLock   = docCacheLock.writeLock();
    private final Lock                       docCacheReadLock    = docCacheLock.readLock();

    public ResourceCache(PandaSettings pandaSettings) {
        this.pandaSettings = pandaSettings;
    }

    /**
     * Add multiple values from list to cache. Values need to have SubURIs to
     * reconstruct entire URI!
     * 
     * @param valList list of values
     * @param resourceType type of resource
     * @param resID unique ID of resource
     * @param eTag entity tag of resource for cache validation
     */
    public void addResourceValues(ValueExchangeExt valList, DataResourceType resourceType,
            String resID, EntityTag eTag) {
        this.eTags.put(resID, eTag);
        valueCacheWriteLock.lock();
        try {
            String baseURI = valList.getBaseURI();
            for (Value v : valList.getValue()) {
                String subURI = v.getSubURI();
                if (subURI == null) {
                    throw new WebApplicationException(422);
                } else {
                    addResourceValue(baseURI + v.getSubURI(), v.getValue(), v.getType(),
                            resourceType);
                }
            }
        } finally {
            valueCacheWriteLock.unlock();
        }

    }

    /**
     * Add a single value to cache.
     * 
     * @param uri URI of a single value
     * @param value the value
     * @param type type entry of value
     * @param resourceType type of resource
     */
    public void addResourceValue(String uri, Object value, String type,
            DataResourceType resourceType) {
        // convert URI in list of path segments fitting for cache tree structure
        LinkedList<String> pathSegments = handleInputURI(uri, resourceType);

        // start node is head of cache tree
        CacheTreeNode currentNode = this.valueCache;

        valueCacheWriteLock.lock();
        try {
            for (int i = 0; i < pathSegments.size(); i++) {
                String fragment = pathSegments.get(i);
                // child exists
                if (currentNode.containsKey(fragment)) {
                    Object temp = currentNode.getChild(fragment);
                    if (!(temp instanceof Value)) {
                        // child exists but is not a leaf
                        currentNode = (CacheTreeNode) temp;
                    } else {
                        // child exists and is leaf
                        break;
                    }
                }
                // child does not exist, new child is not a leaf
                else if (i < (pathSegments.size() - 1)) {
                    CacheTreeNode newChild = new CacheTreeNode();
                    currentNode.addChild(fragment, newChild);
                    currentNode = newChild;
                }
                // child does not exist, new child is a leaf
                else {
                    Value val = new Value();
                    val.setValue(value);
                    val.setType(type);
                    currentNode.addChild(fragment, val);
                }
            }
        } finally {
            valueCacheWriteLock.unlock();
        }

    }

    /**
     * Should be used to add other documents where the value cache can't be used
     * to avoid hard disk read operatSions.
     * 
     * @param doc document to add
     * @param resID unique ID of resource
     * @param eTag entity tag of document for cache validation
     */
    public void addDocument(Object doc, String resID, EntityTag eTag) {
        docCacheWriteLock.lock();
        try {
            this.docCache.put(resID, doc);
            this.eTags.put(resID, eTag);
        } finally {
            docCacheWriteLock.unlock();
        }
    }

    /**
     * Get values from value cache via URI. URI may contain bulk components
     * according to URI schemes defined in de.fuberlin.panda.api.jersey.*.
     * 
     * @param uri request URI
     * @param resourceType - type of resource
     * @return list of values as {@code ValueExchangeExt}
     */
    public ValueExchangeExt getValues(String uri, DataResourceType resourceType) {
        ValueExchangeExt valList = new ValueExchangeExt();
        LinkedList<String> pathSegments = handleInputURI(uri, resourceType);

        valueCacheReadLock.lock();
        try {
            // calculate start node for traversing, set baseURI
            StringBuilder baseURI = new StringBuilder();
            CacheTreeNode startNode = getStartNode(pathSegments, valueCache, baseURI, resourceType);

            // set baseURI
            valList.setBaseURI(baseURI.toString());

            // traverse tree and add values from cache to list
            valList.addValues(traverseCacheTree(startNode, pathSegments, 0, "", resourceType));

        } finally {
            valueCacheReadLock.unlock();
        }

        return valList;
    }

    /**
     * Get cached document.
     * 
     * @param resID unique ID of resource
     * @return binary document as {@code Object}
     */
    public Object getDocument(String resID) {
        try {
            docCacheReadLock.lock();
            return docCache.get(resID);
        } finally {
            docCacheReadLock.unlock();
        }
    }

    /**
     * Removes a resource from document cache.
     * 
     * @param resID unique ID of resource
     */
    public void removeDocument(String resID) {
        try {
            docCacheWriteLock.lock();
            if (this.docCache.containsKey(resID)) {
                this.docCache.remove(resID);
            }
            if (this.eTags.containsKey(resID)) {
                this.eTags.remove(resID);
            }
        } finally {
            docCacheWriteLock.unlock();
        }
    }

    /**
     * Removes a resource from value cache.
     * 
     * @param resID unique ID of resource
     */
    public void removeResourceValues(String resID) {
        try {
            valueCacheWriteLock.lock();
            if (this.valueCache.containsKey(resID)) {
                this.valueCache.removeChild(resID);
            }
            if (this.eTags.containsKey(resID)) {
                this.eTags.remove(resID);
            }
        } finally {
            valueCacheWriteLock.unlock();
        }
    }

    /**
     * Check validity for a resources in value cache. For resources that does
     * not provide EntityTags, null can be send to restrict validation to check
     * of existence. If resource is not valid, it will be removed. Note:
     * resources without entity information will never be updated in the current
     * implementation.
     * 
     * @param resID unique ID of resource
     * @param eTag EntityTag of representation as also used for client caching
     * @return validity of value resource as {@code boolean}
     */
    public boolean checkValidityResource(String resID, EntityTag eTag) {
        boolean valid = false;
        boolean inCache = false;

        valueCacheReadLock.lock();
        boolean locked = true;
        try {
            if (valueCache.containsKey(resID)) {
                inCache = true;
            } else {
                inCache = false;
            }

            if (inCache) {
                if (eTags.containsKey(resID)) {
                    EntityTag cacheTag = eTags.get(resID);
                    if (cacheTag == null)
                        valid = true;
                    else
                        valid = eTag.equals(eTags.get(resID));

                    if (!valid) {
                        valueCacheReadLock.unlock();
                        locked = false;
                        removeResourceValues(resID);
                    }
                } else {
                    // this should not happen
                    valid = false;
                }
            }
        } finally {
            if (locked)
                valueCacheReadLock.unlock();
        }

        return valid;
    }

    /**
     * Check validity for a document. For resources that does not provide
     * EntityTags, null can be send to restrict validation to check of
     * existence. If resource is not valid, it will be removed. Note: resources
     * without entity information will never be updated in the current
     * implementation.
     * 
     * @param resID unique ID of resource
     * @param eTag EntityTag of representation as also used for client caching
     * @return validity of document resource as {@code boolean}
     */
    public boolean checkValidityDocument(String resID, EntityTag eTag) {
        boolean valid = false;
        boolean inCache = false;

        docCacheReadLock.lock();
        boolean locked = true;
        try {
            if (docCache.containsKey(resID)) {
                inCache = true;
            } else {
                inCache = false;
            }

            if (inCache) {
                if (eTags.containsKey(resID)) {
                    EntityTag cacheTag = eTags.get(resID);
                    if (cacheTag == null)
                        valid = true;
                    else
                        valid = eTag.equals(eTags.get(resID));

                    if (!valid) {
                        docCacheReadLock.unlock();
                        locked = false;
                        removeDocument(resID);
                    }
                } else {
                    // this should not happen
                    valid = false;
                }
            }
        } finally {
            if (locked)
                docCacheReadLock.unlock();
        }
        return valid;
    }

    /**
     * Traversing of subtree for requested values.
     * 
     * @param node current node
     * @param pathSegments all path segments of request URI
     * @param currentSegment index of current path segment
     * @param path path to current node
     * @param resourceType type of resource
     * @return list of values gathered from cache tree as {@code List<Value>}
     */
    private List<Value> traverseCacheTree(CacheTreeNode node, LinkedList<String> pathSegments,
            int currentSegment, String path, DataResourceType resourceType) {
        List<Value> valList = new LinkedList<Value>();
        String pathSegment;
        if (currentSegment < pathSegments.size()) {
            pathSegment = pathSegments.get(currentSegment);
        } else {
            return valList;
        }

        Set<String> keys = new HashSet<String>();

        if (pathSegment.equals("*")) {
            keys = node.getKeySet();
        } else if (pathSegment.split(pandaSettings.getRangeDelimiterChar()).length == 2) {
            String[] token = pathSegment.split(pandaSettings.getRangeDelimiterChar());
            keys = node.getKeySubset(token[0], token[1]);
        } else {
            keys = node.getKeySubset(pathSegment, pathSegment);
        }

        for (String key : keys) {
            String newPath;
            Object obj = node.getChild(key);
            if (obj instanceof Value) {
                newPath = handleOutputURI(path + "/" + key, resourceType);
                Value val = (Value) obj;
                val.setSubURI(newPath);
                valList.add(val);
            } else if (obj instanceof CacheTreeNode) {
                newPath = path + "/" + key;
                valList.addAll(traverseCacheTree((CacheTreeNode) obj, pathSegments,
                        currentSegment + 1, newPath, resourceType));
            } else {
                throw new NotSupportedException("Unknown node type in CacheTree!");
            }
        }

        return valList;
    }

    /**
     * Converts URI in a form adjusted for CacheTree. CacheTree can handle only
     * one information per path segment: references like "A1" must be split into
     * two segments for row and column.
     * 
     * @param uri URI as String
     * @param resourceType type of resource used to locate spreadsheet
     *            references to split
     * @return list of path segments as {@code LinkedList<String>}
     */
    private LinkedList<String> handleInputURI(String uri, DataResourceType resourceType) {
        LinkedList<String> pathSegments = new LinkedList<String>(Arrays.asList(uri.split("/")));

        // remove empty path segments caused by leading or trailing "/"
        Iterator<String> iter = pathSegments.iterator();
        while (iter.hasNext()) {
            String s = iter.next();
            if (s.isEmpty())
                iter.remove();
        }

        // get cell/table/row/column reference from path segments
        if (resourceType == DataResourceType.XLS || resourceType == DataResourceType.XLSX) {
            int referenceSegment = 3;
            String reference = pathSegments.get(referenceSegment);
            List<String> refSegments = handleReference(reference);
            pathSegments.remove(referenceSegment);
            pathSegments.addAll(referenceSegment, refSegments);
        } else if (resourceType == DataResourceType.CSV) {
            int referenceSegment = 1;
            String reference = pathSegments.get(referenceSegment);
            List<String> refSegments = handleReference(reference);
            pathSegments.remove(referenceSegment);
            pathSegments.addAll(referenceSegment, refSegments);
        } else if (resourceType == DataResourceType.DOCX || resourceType == DataResourceType.DOC) {
            if (pathSegments.get(1).equals("tables")) {
                int referenceSegment = 3;
                String reference = pathSegments.get(referenceSegment);
                List<String> refSegments = handleReference(reference);
                pathSegments.remove(referenceSegment);
                pathSegments.addAll(referenceSegment, refSegments);
            }
        }

        return pathSegments;
    }

    /**
     * Adjust path of internal tree structure to URI pattern according to
     * resource type.
     * 
     * @param path path that should be handled e.g. a subURI
     * @param resourceType type of resource
     * @return URI formatted for output as {@code String}
     */
    private String handleOutputURI(String path, DataResourceType resourceType) {
        List<String> pathSegments = new LinkedList<String>(Arrays.asList(path.split("/")));

        // remove empty path segments
        Iterator<String> iter = pathSegments.iterator();
        while (iter.hasNext()) {
            String s = iter.next();
            if (s.isEmpty())
                iter.remove();
        }

        // index of row/column reference, -1 if doesn't use any cell/table
        // reference
        int rowRef = -1;
        int colRef = -1;
        if (resourceType == DataResourceType.XLS || resourceType == DataResourceType.XLSX) {
            rowRef = pathSegments.size() - 2;
        } else if (resourceType == DataResourceType.CSV) {
            rowRef = pathSegments.size() - 2;
        } else if (resourceType == DataResourceType.DOCX || resourceType == DataResourceType.DOC) {
            // get index of row reference if table request
            if (pathSegments.size() > 2) {
                rowRef = pathSegments.size() - 3;
            }
        }

        // combine reference of column and row to cell reference (e.g. "A1")
        if (rowRef >= 0) {
            colRef = rowRef + 1;
            pathSegments.set(rowRef, pathSegments.get(colRef) + pathSegments.get(rowRef));
            pathSegments.remove(colRef);
        }

        // concatenate path segments to String
        String retPath = new String();
        for (String segment : pathSegments) {
            retPath = retPath + segment + "/";
        }
        retPath = retPath.substring(0, retPath.length() - 1);
        return retPath;
    }

    /**
     * Handle table/cell reference and return list containing a separate row and
     * column path segment.
     * 
     * @param tableRef reference with pattern
     *            (\\*|[A-Z][A-Z]*[1-9][0-9]*|\\*[1-9][0-9] *|[A-Z][A-
     *            Z]*\\*|[A-Z][A-Z]*[1-9][0-9]*\\:[A-Z][A-Z]*[1-9][0-9]*)
     * @return list of split path segments as {@code List<String>}
     */
    private List<String> handleReference(String tableRef) {
        List<String> segments = new LinkedList<String>();
        TableArea tableArea = ResourceHelper.evalTableReference(tableRef);
        String colStart, colEnd, rowStart, rowEnd;
        colStart = ResourceHelper.convertColNumToColRef(tableArea.getColStart());
        colEnd = ResourceHelper.convertColNumToColRef(tableArea.getColEnd());
        rowStart = String.valueOf(tableArea.getRowStart() + 1);
        rowEnd = String.valueOf(tableArea.getRowEnd() + 1);

        boolean allRows, allColumns, cellRequest;
        allRows = allColumns = cellRequest = false;
        // is column request
        if ((tableArea.getRowEnd() == -1) || (tableArea.getRowStart() == -1)) {
            allRows = true;
        }
        // is row request
        if ((tableArea.getColEnd() == -1) || (tableArea.getColStart() == -1)) {
            allColumns = true;
        }
        // is single cell request
        if ((tableArea.getRowEnd() == tableArea.getRowStart())
                && (tableArea.getColEnd() == tableArea.getColStart()) && !(allRows || allColumns)) {
            cellRequest = true;
        }

        // set row segment
        if (allRows) {
            segments.add("*");
        } else if (!cellRequest) {
            segments.add(rowStart + pandaSettings.getRangeDelimiterChar() + rowEnd);
        } else {
            segments.add(rowStart);
        }

        // set column segment
        if (allColumns) {
            segments.add("*");
        } else if (!cellRequest) {
            segments.add(colStart + pandaSettings.getRangeDelimiterChar() + colEnd);
        } else {
            segments.add(colStart);
        }

        return segments;
    }

    /**
     * Get start node for cache tree by traversing baseURI.
     * 
     * @param pathSegments path segments of request URI
     * @param node current node
     * @param baseURI current determined baseURI
     * @param resourceType type of resource
     * @return start node for cache tree traversing as {@code CacheTreeNode}
     */
    private CacheTreeNode getStartNode(LinkedList<String> pathSegments, CacheTreeNode node,
            StringBuilder baseURI, DataResourceType resourceType) {
        CacheTreeNode currentNode = node;
        baseURI.append("/");

        int minBaseUriSegments = getMinBaseUriPathSegments(resourceType);
        int startSubUri = pathSegments.size() - getMinSubUriPathSegments(resourceType);

        Iterator<String> iter = pathSegments.iterator();
        for (int count = 0; iter.hasNext(); count++) {
            String pathSegment = iter.next();

            // get next child node if:
            // - it is the first node (head of resource subtree)
            // - path segment is not a bulk request
            // - current path segment is not part spared for subURI
            // - segment shall be ignored (e.g. inserted extra segments)
            if (((!pathSegment.equals("*")
                    && pathSegment.split(pandaSettings.getRangeDelimiterChar()).length < 2 && count < startSubUri))
                    || count < minBaseUriSegments) {

                if (!currentNode.containsKey(pathSegment)) {
                    throw new WebApplicationException(404);
                }

                Object obj = currentNode.getChild(pathSegment);
                if (obj instanceof CacheTreeNode) {
                    currentNode = (CacheTreeNode) obj;
                    baseURI.append(pathSegment + "/");
                    iter.remove();
                } else {
                    throw new NotSupportedException(
                            "Starting node for traversion of CacheTree shall not be a leaf.");
                }
            } else {
                // bulk segment found, traversing will branch here
                break;
            }
        }

        return currentNode;
    }

    /**
     * Returns minimum number of path segments of cache tree that will be part
     * of a SubURI, which identifies a value in a list of values.
     * 
     * @param resourceType type of resource
     * @return minimum number of path segments spared for subURI entry as
     *         {@code int}
     */
    private int getMinSubUriPathSegments(DataResourceType resourceType) {
        int sparePathSegments;
        if (resourceType == DataResourceType.XLS || resourceType == DataResourceType.XLSX
                || resourceType == DataResourceType.CSV) {
            // row + column will be combined to 1 path segment
            sparePathSegments = 2;
        } else if (resourceType == DataResourceType.DOCX || resourceType == DataResourceType.DOC) {
            // does only affect table requests
            // SubURI will contain column + row + paragraph index
            sparePathSegments = 3;
        } else {
            sparePathSegments = 1;
        }
        return sparePathSegments;
    }

    /**
     * Returns minimum number of path segments of cache tree that will be part
     * of a BaseURI.
     * 
     * @param resourceType type of resource
     * @return minimum number of segments in baseURI entry as {@code int}
     */
    private int getMinBaseUriPathSegments(DataResourceType resourceType) {
        if (resourceType == DataResourceType.XLS || resourceType == DataResourceType.XLSX
                || resourceType == DataResourceType.DOCX || resourceType == DataResourceType.DOC
                || resourceType == DataResourceType.PDF) {
            // 2 for all classes that implements multiple URI types with
            // additional designator
            return 2;
        } else {
            return 1;
        }
    }
}
