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
import de.fuberlin.panda.api.data.ValueExchangeExt;
import de.fuberlin.panda.data.resources.DataCsvResource;

/**
 * This class defines the REST interface for requests to CSV documents.
 * 
 * @author Christoph Schröder
 */
public class JerseyCsvResource extends AbstractJerseyResource {

    /**
     * Method for request of CSV table data as XML.
     * 
     * @return Response Values in requested MediaType.
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/{Reference:(\\*|[A-Z][A-Z]*[1-9][0-9]*|\\*[1-9][0-9]*|[A-Z][A-Z]*\\*|[A-Z][A-Z]*[1-9][0-9]*[\\-\\:][A-Z][A-Z]*[1-9][0-9]*)}")
    @Produces(MediaType.APPLICATION_XML)
    public Response getCsvValuesXML() throws WebApplicationException {
        Response response = null;
        ValueExchangeExt valList = new ValueExchangeExt();

        try {
            if (pandaSettings.getUseClientCaching()) {
                response = validateClientCache();
                if (response != null) {
                    return response;
                }
            }
            valList = getCsvValues();
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
     * Method for request of CSV table data as JSON.
     * 
     * @return Response Values in requested MediaType.
     * @throws WebApplicationException HTTP exception
     */
    @GET
    @Path("/{Reference:(\\*|[A-Z][A-Z]*[1-9][0-9]*|\\*[1-9][0-9]*|[A-Z][A-Z]*\\*|[A-Z][A-Z]*[1-9][0-9]*[\\-\\:][A-Z][A-Z]*[1-9][0-9]*)}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCsvValuesJSON() throws WebApplicationException {
        Response response = null;
        ValueExchangeExt valList = null;

        try {
            if (pandaSettings.getUseClientCaching()) {
                response = validateClientCache();
                if (response != null) {
                    return response;
                }
            }
            valList = getCsvValues();
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
     * Request values from file or cache.
     * 
     * @return ValueExchangeExt list of values.
     */
    private ValueExchangeExt getCsvValues() {
        ValueExchangeExt valList = new ValueExchangeExt();
        DataCsvResource csvResource = new DataCsvResource();
        valList = csvResource.getCsvValues(uriInfo, pandaSettings);

        // 404 if no values found
        if (valList.getValue().isEmpty()) {
            throw new WebApplicationException(404);
        }

        return valList;
    }

}
