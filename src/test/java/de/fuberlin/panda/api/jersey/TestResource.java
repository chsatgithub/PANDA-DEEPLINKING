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
import javax.ws.rs.core.MediaType;

import de.fuberlin.panda.api.APIHelper;

/**
 * Test resource to check online status or for test examples.
 * 
 * @author Christoph Schröder
 */
@Path("/data/TestData")
public class TestResource {

    /**
     * Method to test online and deployment status of Jersey. TestURI:
     * http://localhost:8080/PANDA/rest/Data/TestData
     * 
     * @return test value
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        return "Hello!";
    }

    /**
     * Loads a local XML test file, will work only for inmemory tests.
     * 
     * @param subID subID of test resource
     * @return example XML String
     */
    @GET
    @Path("XmlTest/{subID}")
    @Produces(MediaType.APPLICATION_XML)
    public String getXmlExample(@PathParam("subID") String subID) {
        return TestHelper.getTestFile("WebContent/testData/valueExample" + subID + ".xml");
    }

    /**
     * Shows an example of returned XML response. TestURI:
     * http://localhost:8080/PANDA/rest/Data/TestData/{subID}
     * 
     * @param subID subID of test resource
     * @return requested value
     */
    @GET
    @Path("{subID}")
    @Produces(MediaType.APPLICATION_XML)
    public String getValue(@PathParam("subID") String subID) {
        String directory = "testData";
        String fileName = "valueExample" + subID + ".xml";
        return APIHelper.readFileContent(directory, fileName);
    }
}