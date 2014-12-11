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


import java.io.File;

import javax.ws.rs.ApplicationPath;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;

import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMap;

/**
 * Obligatory Jersey class for servlet configuration. Used features and binding
 * of classes for dependency injection is done here.
 * 
 * @author Christoph Schröder
 */
@ApplicationPath("/rest/*")
public class PandaJerseyConfig extends ResourceConfig {

    // contains system wide used settings and objects
    private PandaSettings pandaSettings = new PandaSettings();

    public PandaJerseyConfig() throws JAXBException {
        packages("de.fuberlin.panda.api.jersey");
        setApplicationName("PANDA");

        // Load Resource Map
        JAXBContext jaxbContext = JAXBContext.newInstance(ResourceMap.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        File configFile = new File(pandaSettings.getResourceConfFilePath());
        pandaSettings.setResourceMap((ResourceMap) unmarshaller.unmarshal(configFile));

        // create default settings
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(pandaSettings);
            }
        });

        // register class for JSON support
        this.register(JacksonFeature.class);

        // register JSP support
        this.register(JspMvcFeature.class);
        this.property(MvcFeature.TEMPLATE_BASE_PATH, "/WEB-INF/jsp");

        // Custom JAXB marshaller provider
        this.register(JaxbMarshallerProvider.class);

        // Custom Jackson ObjectMapper provider
        this.register(JacksonObjectMapperProvider.class);

    }

    public PandaSettings getResourceSettings() {
        return this.pandaSettings;
    }

}
