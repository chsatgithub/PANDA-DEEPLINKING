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


import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.enums.DataResourceType;

/**
 * Main class of REST interface- resource class.
 * 
 * @author Christoph Schröder
 */
@Path("/data")
public class JerseyRootResource {

    @Inject
    PandaSettings pandaSettings;

    /**
     * This method directs the request to the adequate Jersey resource class.
     * 
     * @param resID unique ID of resource
     * @return class appropriate to handle request
     */
    @Path("{ResourceID}")
    public Class<?> getItemContentResource(@PathParam("ResourceID") String resID) {
        
        if (pandaSettings.getResourceMap().getMap().containsKey(resID)) {
            DataResourceType resourceType = pandaSettings.getResourceMap().getMap().get(resID)
                    .getType();
            switch (resourceType) {
            case DOC:
                return JerseyWordResource.class;
            case DOCX:
                return JerseyWordResource.class;
            case XLS:
                return JerseyExcelResource.class;
            case XLSX:
                return JerseyExcelResource.class;
            case XML:
                return JerseyXmlResource.class;
            case HTML:
                return JerseyHtmlResource.class;
            case PDF:
                return JerseyPdfResource.class;
            case CSV:
                return JerseyCsvResource.class;
            default:
                throw new WebApplicationException(422);
            }
        } else {
            // no resource with requested ID in system
            throw new WebApplicationException(404);
        }
    }

}
