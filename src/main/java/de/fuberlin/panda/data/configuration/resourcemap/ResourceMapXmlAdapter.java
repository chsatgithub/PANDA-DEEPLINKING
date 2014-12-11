package de.fuberlin.panda.data.configuration.resourcemap;

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


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;

/**
 * Adapter class since JAXB does not directly support HashMap.
 * 
 * @author Christoph Schröder
 */
public class ResourceMapXmlAdapter extends XmlAdapter<ResourceMapType, Map<String, ResourceInfo>> {

    @Override
    public ResourceMapType marshal(Map<String, ResourceInfo> v) throws Exception {
        ResourceMapType resourceList = new ResourceMapType();
        for (Entry<String, ResourceInfo> entry : v.entrySet()) {
            ResourceMapEntryType resourceEntry = new ResourceMapEntryType();
            resourceEntry.resourceID = entry.getKey();
            resourceEntry.resourceInfo = entry.getValue();
            resourceList.resourceList.add(resourceEntry);
        }
        return resourceList;
    }

    @Override
    public Map<String, ResourceInfo> unmarshal(ResourceMapType v) throws Exception {
        HashMap<String, ResourceInfo> resourceMap = new HashMap<String, ResourceInfo>();
        for (ResourceMapEntryType entry : v.resourceList) {
            resourceMap.put(entry.resourceID, entry.resourceInfo);
        }
        return resourceMap;
    }

}
