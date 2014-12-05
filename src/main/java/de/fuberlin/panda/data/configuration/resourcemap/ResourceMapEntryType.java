package de.fuberlin.panda.data.configuration.resourcemap;

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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import de.fuberlin.panda.enums.DataResourceType;

/**
 * Type of entries of resource map.
 * 
 * @author Christoph Schröder
 */
public class ResourceMapEntryType {

    @XmlAttribute
    public String       resourceID;

    @XmlElement(name = "ResourceInfo", required = true)
    public ResourceInfo resourceInfo;

    public static class ResourceInfo {

        // resource attributes
        private DataResourceType type;
        private String           filePath;
        private URL              resURL;
        private String           separator;

        public ResourceInfo() {
        }

        public ResourceInfo(DataResourceType type, String filePath) {
            this.type = type;
            this.filePath = filePath;
        }

        public ResourceInfo(DataResourceType type, URL resURL) {
            this.type = type;
            this.resURL = resURL;
        }

        public DataResourceType getType() {
            return type;
        }

        public void setType(DataResourceType type) {
            this.type = type;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public URL getURL() {
            return resURL;
        }

        public void setURL(URL resURL) {
            this.resURL = resURL;
        }

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }

    }

}
