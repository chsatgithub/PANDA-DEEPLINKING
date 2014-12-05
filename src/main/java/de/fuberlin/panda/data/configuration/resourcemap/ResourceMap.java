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


import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;

/**
 * JAXB class for resource management map.
 * 
 * @author Christoph Schröder
 */
@XmlRootElement(name = "PANDA")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceMap {

    @XmlJavaTypeAdapter(ResourceMapXmlAdapter.class)
    @XmlElement(name = "ResourceList")
    Map<String, ResourceInfo> resourceMap = new HashMap<String, ResourceInfo>();

    public ResourceMap() {
    }

    public ResourceMap(Map<String, ResourceInfo> resourceMap) {
        this.resourceMap = resourceMap;
    }

    public Map<String, ResourceInfo> getMap() {
        return resourceMap;
    }

    public void setMap(Map<String, ResourceInfo> resourceMap) {
        this.resourceMap = resourceMap;
    }

}
