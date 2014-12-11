package de.fuberlin.panda.api.jersey;

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


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.fuberlin.panda.data.resources.DataHtmlResource;

/**
 * This class defines the REST interface for requests to HTML documents.
 * 
 * @author Christoph Schröder
 */
public class JerseyHtmlResource extends AbstractJerseyResource {

    /**
     * Method for request of HTML data via XPath.
     * 
     * @return Response XML according to valueExcangeSchema.xsd
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/{XPathExp:.*}")
    @Produces(MediaType.APPLICATION_XML)
    public Response getXml() throws WebApplicationException {
        Response response = null;
        String htmlDoc = new String();
        DataHtmlResource htmlResource = new DataHtmlResource();

        try {
            if (pandaSettings.getUseClientCaching()) {
                response = validateClientCache();
                if (response != null) {
                    return response;
                }
            }

            htmlDoc = htmlResource.getHtml(uriInfo, pandaSettings);

            // Throw 404 WebApplicationException if no values found
            if (htmlDoc.isEmpty()) {
                throw new WebApplicationException(404);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.warning(e.getMessage() + "\n" + uriInfo.getAbsolutePath().getPath());
            throw new WebApplicationException(404);
        }

        response = Response.ok(htmlDoc, MediaType.APPLICATION_XML).lastModified(this.lastModified)
                .tag(this.eTag).build();
        return response;
    }

}
