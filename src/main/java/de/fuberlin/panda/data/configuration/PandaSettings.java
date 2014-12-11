package de.fuberlin.panda.data.configuration;

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


import javax.ws.rs.core.CacheControl;

import de.fuberlin.panda.api.APIHelper;
import de.fuberlin.panda.data.caching.ResourceCache;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMap;
import de.fuberlin.panda.data.resources.ResourceHelper;
import de.fuberlin.panda.enums.RangeDelimiter;

/**
 * JavaBean with PANDA system settings.
 * 
 * @author Christoph Schröder
 */
public class PandaSettings {

    private boolean        useVtdXml             = true;
    private boolean        useServerCaching      = false;
    private boolean        useClientCaching      = false;

    // use JTidy to clean HTML, HtmlCleaner otherwise
    private boolean        useJtidy              = true;
    private boolean        namespaceAwareness    = false;

    private ResourceCache  resourceCache         = new ResourceCache(this);
    private ResourceMap    resourceMap;
    private CacheControl   cacheControl          = new CacheControl();

    private final String   resConfigFilePath     = APIHelper.getWebContentDirPath()
                                                         + "configuration/resource_config.xml";
    private RangeDelimiter rangeDelimiter        = RangeDelimiter.COLON;

    public PandaSettings() {
        setUseClientCaching(this.useClientCaching);
        ResourceHelper.setPandaSettings(this);
    }

    public void setVtdUsage(boolean useVtdXml) {
        this.useVtdXml = useVtdXml;
    }

    public boolean getVtdUsage() {
        return this.useVtdXml;
    }

    public void setServerCacheUsage(boolean useServerCaching) {
        this.useServerCaching = useServerCaching;
    }

    public boolean getServerCacheUsage() {
        return this.useServerCaching;
    }

    public ResourceCache getResourceCache() {
        return this.resourceCache;
    }

    public void setResourceMap(ResourceMap resourceMap) {
        this.resourceMap = resourceMap;
    }

    public ResourceMap getResourceMap() {
        return this.resourceMap;
    }

    public void setNamespaceAwareness(boolean namespaceAwareness) {
        this.namespaceAwareness = namespaceAwareness;
    }

    public boolean getNamespaceAwareness() {
        return this.namespaceAwareness;
    }

    public String getResourceConfFilePath() {
        return this.resConfigFilePath;
    }

    public void setCacheControl(CacheControl cacheControl) {
        this.cacheControl = cacheControl;
    }

    public CacheControl getCacheControl() {
        return this.cacheControl;
    }

    public void setUseClientCaching(boolean useClientCaching) {
        this.useClientCaching = useClientCaching;
        // we will always use validation
        cacheControl.setMustRevalidate(true);
        if (useClientCaching) {
            cacheControl.setMaxAge(-1);
            cacheControl.setNoCache(false);
            cacheControl.setNoStore(false);
        } else {
            cacheControl.setMaxAge(0);
            cacheControl.setNoCache(true);
            cacheControl.setNoStore(true);
        }
    }

    public boolean getUseClientCaching() {
        return this.useClientCaching;
    }

    public void setRangeDelimiter(RangeDelimiter rangeDelimiter) {
        this.rangeDelimiter = rangeDelimiter;
    }

    public RangeDelimiter getRangeDelimiter() {
        return this.rangeDelimiter;
    }

    public String getRangeDelimiterChar() {

        switch (this.rangeDelimiter) {
        case COLON:
            return "\u003A";
        case HYPHEN:
            return "\u002D";
        default:
            return "\u003A";
        }
    }

    public void setUseJtidy(boolean useJtidy) {
        this.useJtidy = useJtidy;
    }

    public boolean getUseJtidy() {
        return this.useJtidy;
    }
}
