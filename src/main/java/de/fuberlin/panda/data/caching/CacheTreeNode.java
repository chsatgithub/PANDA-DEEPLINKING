package de.fuberlin.panda.data.caching;

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


import java.util.Set;
import java.util.TreeMap;

import de.fuberlin.panda.api.data.ValueExchange.Value;

/**
 * This class defines nodes in value cache tree.
 * 
 * @author Christoph Schröder
 */
public class CacheTreeNode {
    private TreeMap<String, Object> children = new TreeMap<String, Object>(
                                                     new PathSegmentComparator());

    /**
     * Add a child to this node.
     * 
     * @param key key of child.
     * @param child child node.
     */
    public void addChild(String key, Object child) {
        if (child instanceof Value || child instanceof CacheTreeNode) {
            this.children.put(key, child);
        } else {
            throw new IllegalArgumentException(
                    "CacheTreeNode shall not hold objects other than CacheTreeNode or Value objects ");
        }
    }

    /**
     * Returns a child with a given key.
     * 
     * @param key key of child.
     * @return child of this node with given key
     */
    public Object getChild(String key) {
        return children.get(key);
    }

    /**
     * Remove child with given key.
     * 
     * @param key key of child.
     */
    public void removeChild(String key) {
        children.remove(key);
    }

    /**
     * Return true if node has a child with given key.
     * 
     * @param key key of child node.
     * @return true if node contains child with given key
     */
    public boolean containsKey(String key) {
        return this.children.containsKey(key);
    }

    /**
     * Return keySet of node.
     * 
     * @return the KeySet of this node
     */
    public Set<String> getKeySet() {
        return children.keySet();
    }

    /**
     * Returns a keys of a certain range given by a start key and end key.
     * 
     * @param startKey key of first child
     * @param endKey key of last child
     * @return subset of keys
     */
    public Set<String> getKeySubset(String startKey, String endKey) {
        Set<String> keySubset = children.tailMap(startKey, true).headMap(endKey, true).keySet();

        return keySubset;
    }

    /**
     * Get first key of children.
     * 
     * @return first key
     */
    public String getFirstKey() {
        return children.firstKey();
    }

    /**
     * Get last key of children.
     * 
     * @return last key
     */
    public String getLastKey() {
        return children.lastKey();
    }

}
