package de.fuberlin.panda.data.configuration.resourcemap.creator;

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


import java.net.URL;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;
import de.fuberlin.panda.enums.DataResourceType;

/**
 * Custom table model since Swing default table model does not support HashMap.
 * 
 * @author Christoph Schröder
 */
public class ResourceTableModel extends AbstractTableModel {

    /**
     * 
     */
    private static final long         serialVersionUID = -1778454752115851434L;

    // maps with resources and resource informations
    private Map<String, ResourceInfo> resourceMap      = new TreeMap<String, ResourceInfo>();
    // Names of columns.
    private String[]                  columnNames      = { "Resource ID", "Type", "File Path",
            "URL"                                     };

    @Override
    public String getColumnName(int column) {
        return columnNames[column];

    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return resourceMap.values().toArray().length;
    }

    @Override
    public Object getValueAt(int row, int column) {
        String resourceID = "";
        try {
            resourceID = (String) resourceMap.keySet().toArray()[row];
        } catch (Exception e) {
            return "";
        }

        if (column == 0) {
            return resourceID;
        }

        ResourceInfo resInfo = resourceMap.get(resourceID);

        switch (column) {
        case 1:
            return resInfo.getType();
        case 2:
            try {
                String filePath = resInfo.getFilePath();
                return filePath;
            } catch (Exception e) {
                // return empty String if filePath not set
                return "";
            }
        case 3:
            try {
                URL url = resInfo.getURL();
                return url.toString();
            } catch (Exception e) {
                // return empty String if URL not set
                return "";
            }
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column == 0) {
            resourceMap.put((String) value, null);
        }

        String resourceID = (String) resourceMap.keySet().toArray()[row];
        ResourceInfo resInfo = (ResourceInfo) resourceMap.values().toArray()[row];

        switch (column) {
        case 1:
            resInfo.setType((DataResourceType) value);
            break;
        case 2:
            resInfo.setFilePath((String) value);
        case 3:
            resInfo.setURL((URL) value);
            break;
        }
        resourceMap.put(resourceID, resInfo);
        fireTableCellUpdated(row, column);
    }

    /**
     * Returns the class type of a column.
     */
    public Class<?> getColumnClass(int c) {
        Class<?> type = null;
        try {
            type = getValueAt(0, c).getClass();
        } catch (Exception e) {
            return String.class;
        }
        return type;
    }

    /**
     * Returns the current resource map.
     * 
     * @return map of resource information as {@code Map<String, ResourceInfo>}
     */
    public Map<String, ResourceInfo> getResourceMap() {
        return this.resourceMap;
    }

    /**
     * Loads a new resource map into table.
     * 
     * @param resourceMap - resource map to load
     */
    public void setResourceMap(Map<String, ResourceInfo> resourceMap) {
        this.resourceMap = resourceMap;
        fireTableDataChanged();
    }

    /**
     * Adds a resource to table.
     * 
     * @param resourceID unique ID of resource
     * @param newResInfo resource information of new resource
     */
    public void addResource(String resourceID, ResourceInfo newResInfo) {
        boolean isDuplicateFilePath = false;
        boolean isDuplicateURL = false;

        // check if resource with same filePath already exists
        if (newResInfo.getFilePath() != null)
            isDuplicateFilePath = isDuplicateFilePath(newResInfo.getFilePath());
        // check if resource with same URL already exists
        if (newResInfo.getURL() != null)
            isDuplicateURL = isDuplicateURL(newResInfo.getURL());

        if (!isDuplicateFilePath && !isDuplicateURL) {
            this.resourceMap.put(resourceID, newResInfo);
            fireTableDataChanged();
        }
    }

    /**
     * Remove one resource (row) from table.
     * 
     * @param recourceID unique ID of resource
     */
    public void removeResource(String recourceID) {
        this.resourceMap.remove(recourceID);
        fireTableDataChanged();
    }

    /**
     * Remove one or more rows from table.
     * 
     * @param delRows list of rows that shall be deleted
     */
    public void removeRows(int[] delRows) {
        Object[] resIDs = resourceMap.keySet().toArray();
        LinkedList<String> delete = new LinkedList<String>();
        for (int i : delRows) {
            delete.add((String) resIDs[i]);
        }

        for (String resID : delete) {
            this.resourceMap.remove(resID);
        }
        fireTableDataChanged();
    }

    /**
     * Checks whether resource with same URL is already in resourceMap.
     * 
     * @param resURL
     * @return
     */
    private boolean isDuplicateURL(URL resURL) {
        String urlString = resURL.toString();
        for (ResourceInfo resInfo : resourceMap.values()) {
            URL checkURL = resInfo.getURL();
            if (checkURL != null) {
                if (urlString.equals(checkURL.toString()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Checks whether resource with same filePath is already in resourceMap.
     * 
     * @param filePath
     * @return
     */
    private boolean isDuplicateFilePath(String filePath) {
        for (ResourceInfo resInfo : resourceMap.values()) {
            String checkFilePath = resInfo.getFilePath();
            if (checkFilePath != null) {
                if (filePath.equals(checkFilePath))
                    return true;
            }
        }
        return false;
    }
}
