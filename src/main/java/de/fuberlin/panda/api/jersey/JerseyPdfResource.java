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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.fuberlin.panda.api.data.ImageData;
import de.fuberlin.panda.api.data.ValueExchangeExt;
import de.fuberlin.panda.api.data.ValueExchange.Value;
import de.fuberlin.panda.data.resources.DataPdfResource;

/**
 * This class defines the REST interface for requests to PDF documents.
 * 
 * @author Christoph Schröder
 */
public class JerseyPdfResource extends AbstractJerseyResource {

    /**
     * Method for request of PDF text data as XML.
     * 
     * @return Response Values in requested MediaType.
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/text/{PageRef:(\\*|[1-9][0-9]*|[1-9][0-9]*:[1-9][0-9]*)}/{LineRef:(\\*|[1-9][0-9]*|[1-9][0-9]*:[1-9][0-9]*)}")
    @Produces(MediaType.APPLICATION_XML)
    public Response getTextXML() throws WebApplicationException {
        Response response = null;
        ValueExchangeExt valList = null;

        try {
            if (pandaSettings.getUseClientCaching()) {
                response = validateClientCache();
                if (response != null) {
                    return response;
                }
            }
            valList = getText();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.warning(e.getMessage() + "\n" + uriInfo.getAbsolutePath().getPath());
            throw new WebApplicationException(404);
        }

        response = Response.ok(valList, MediaType.APPLICATION_XML).lastModified(this.lastModified)
                .tag(this.eTag).build();
        return response;
    }

    /**
     * Method for request of PDF text data as XML.
     * 
     * @return Response Values in requested MediaType.
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/text/{PageRef:(\\*|[1-9][0-9]*|[1-9][0-9]*:[1-9][0-9]*)}/{LineRef:(\\*|[1-9][0-9]*|[1-9][0-9]*:[1-9][0-9]*)}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTextJSON() throws WebApplicationException {
        Response response = null;
        ValueExchangeExt valList = null;

        try {
            if (pandaSettings.getUseClientCaching()) {
                response = validateClientCache();
                if (response != null) {
                    return response;
                }
            }
            valList = getText();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.warning(e.getMessage() + "\n" + uriInfo.getAbsolutePath().getPath());
            throw new WebApplicationException(404);
        }

        response = Response.ok(valList, MediaType.APPLICATION_JSON).lastModified(this.lastModified)
                .tag(this.eTag).build();
        return response;
    }

    /**
     * Method for request of PDF text data as XML.
     * 
     * @return Response Values in requested MediaType.
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/text/{PageRef:(\\*|[1-9][0-9]*|[1-9][0-9]*:[1-9][0-9]*)}/{LineRef:(\\*|[1-9][0-9]*|[1-9][0-9]*:[1-9][0-9]*)}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTextPlainText() throws WebApplicationException {
        Response response = null;
        ValueExchangeExt valList = null;

        try {
            if (pandaSettings.getUseClientCaching()) {
                response = validateClientCache();
                if (response != null) {
                    return response;
                }
            }
            valList = getText();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.warning(e.getMessage() + "\n" + uriInfo.getAbsolutePath().getPath());
            throw new WebApplicationException(404);
        }

        StringBuilder result = new StringBuilder();
        for (Value value : valList.getValue()) {
            result.append(value.getValue());
            result.append("\n");
        }

        response = Response.ok(result.toString(), MediaType.TEXT_PLAIN)
                .lastModified(this.lastModified).tag(this.eTag).build();

        return response;
    }

    /**
     * Method for request for pictures in PDF document.
     * 
     * @param pictureID index of picture
     * @return Response picture
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/pictures/{PageRef: ([1-9][0-9]*)}/{PictureID: ([1-9][0-9]*)}")
    @Produces("image/*")
    public Response getPicture(@PathParam("PictureID") String pictureID)
            throws WebApplicationException {
        Response response = null;
        ImageData imageData = null;
        try {
            response = validateClientCache();
            if (response != null) {
                return response;
            }

            DataPdfResource pdfResource = new DataPdfResource();
            imageData = pdfResource.getPicture(uriInfo, pandaSettings.getResourceMap());

            // Throw 404 WebApplicationException if image not found
            if (imageData == null) {
                throw new WebApplicationException(404);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.warning(e.getMessage() + "\n" + uriInfo.getAbsolutePath().getPath());
            throw new WebApplicationException(404);
        }

        response = Response.ok(imageData.getImageData(), imageData.getMimeType())
                .lastModified(lastModified).tag(this.eTag).build();
        return response;
    }

    /**
     * Request text values from file or cache.
     * 
     * @return ValueExchangeExt list of values.
     */
    private ValueExchangeExt getText() {
        ValueExchangeExt valList = new ValueExchangeExt();
        DataPdfResource pdfResource = new DataPdfResource();
        valList = pdfResource.getPdfTextContent(uriInfo, pandaSettings);

        // Throw 404 WebApplicationException if no values found
        if (valList.getValue().isEmpty()) {
            throw new WebApplicationException(404);
        }

        return valList;
    }

}
