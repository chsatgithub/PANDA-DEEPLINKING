package de.fuberlin.panda.api.data;

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


import javax.ws.rs.core.MediaType;

import de.fuberlin.panda.data.configuration.PandaSettings;

public class PandaAdministrationBean {
    private PandaSettings pandaSettings = null;
    private String        result        = null;
    private String        baseURI       = null;
    private String        resourcePath  = null;
    private String        resourceID    = null;
    private String        requestedURI  = null;
    private MediaType     mediaType     = null;

    public void setPandaSettings(PandaSettings pandaSettings) {
        this.pandaSettings = pandaSettings;
    }

    public PandaSettings getPandaSettings() {
        return this.pandaSettings;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return this.result;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public String getBaseURI() {
        return this.baseURI;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public void setMediaType(MediaType outputFormat) {
        this.mediaType = outputFormat;
    }

    public MediaType getMediaType() {
        return this.mediaType;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public String getResourceID() {
        return this.resourceID;
    }

    public void setRequestedURI(String requestedURI) {
        this.requestedURI = requestedURI;
    }

    public String getRequestedURI() {
        return this.requestedURI;
    }
}
