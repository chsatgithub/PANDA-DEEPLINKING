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


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.json.XML;

import de.fuberlin.panda.data.resources.DataXmlResource;

/**
 * This class defines the REST interface for requests to XML documents.
 * 
 * @author Christoph Schröder
 */
public class JerseyXmlResource extends AbstractJerseyResource {

    /**
     * Method for request of XML data via XPath.
     * 
     * @return Response XML according to valueExcangeSchema.xsd
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/{XPathExp:.*}")
    @Produces(MediaType.APPLICATION_XML)
    public Response getXmlXML() throws WebApplicationException {
        Response response = null;
        String xmlDoc = new String();

        try {
            if (pandaSettings.getUseClientCaching()) {
                response = validateClientCache();
                if (response != null) {
                    return response;
                }
            }

            DataXmlResource xmlResource = new DataXmlResource();
            xmlDoc = xmlResource.getXml(uriInfo, pandaSettings);

            // Throw 404 WebApplicationException if no values found
            if (xmlDoc.isEmpty()) {
                throw new WebApplicationException(404);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.warning(e.getMessage() + "\n" + uriInfo.getAbsolutePath().getPath());
            throw new WebApplicationException(404);
        }

        response = Response.ok(xmlDoc, MediaType.APPLICATION_XML).tag(this.eTag).build();
        return response;
    }

    /**
     * Method for request of XML data via XPath.
     * 
     * @return Response XML according to valueExcangeSchema.xsd
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/{XPathExp:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXmlJSON() throws WebApplicationException {
        Response response = null;
        String xmlDoc = new String();
        String jsonDoc = new String();

        try {
            if (pandaSettings.getUseClientCaching()) {
                response = validateClientCache();
                if (response != null) {
                    return response;
                }
            }

            DataXmlResource xmlResource = new DataXmlResource();
            xmlDoc = xmlResource.getXml(uriInfo, pandaSettings);
            JSONObject json = XML.toJSONObject(xmlDoc);

            //JSON String with pretty printing
            jsonDoc = json.toString(4);
            
            // Throw 404 WebApplicationException if no values found
            if (jsonDoc.isEmpty()) {
                throw new WebApplicationException(404);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.warning(e.getMessage() + "\n" + uriInfo.getAbsolutePath().getPath());
            throw new WebApplicationException(404);
        }

        response = Response.ok(jsonDoc, MediaType.APPLICATION_JSON).tag(this.eTag).build();
        return response;
    }

}
