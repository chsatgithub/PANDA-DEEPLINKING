package de.fuberlin.panda.api.jersey;

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
import java.util.Date;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;

/**
 * Abstract implementation of PANDAs Jersey classes. Each Jersey class defines
 * methods for the REST interface of one or similar data formats.
 * 
 * @author Christoph Schröder
 */
public abstract class AbstractJerseyResource {

    @Context
    UriInfo             uriInfo;
    @Context
    Request             request;
    @Inject
    PandaSettings       pandaSettings;

    protected Logger    logger       = Logger.getLogger(this.getClass().getName());
    protected Date      lastModified = null;
    protected EntityTag eTag         = null;

    /**
     * Validation of client cache.
     * 
     * @return response with HTTP 304 if cache valid or null if cache not valid
     */
    public Response validateClientCache() {
        // Get Request parameters from URI
        String resID = uriInfo.getPathSegments().get(1).getPath();
        ResourceInfo resInfo = pandaSettings.getResourceMap().getMap().get(resID);
        String filePath = resInfo.getFilePath();

        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                lastModified = new Date(file.lastModified());
                eTag = new EntityTag(resID + "_" + lastModified.getTime(), false);
                ResponseBuilder builder = request.evaluatePreconditions(lastModified, eTag);

                // resource not modified
                if (builder != null) {
                    return builder.cacheControl(pandaSettings.getCacheControl())
                            .lastModified(lastModified).tag(eTag).build();
                }
            } else {
                throw new WebApplicationException(404);
            }
        }

        return null;
    }
}
