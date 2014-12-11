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


import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import de.fuberlin.panda.data.configuration.PandaSettings;

/**
 * Resource configuration that will be used for the in memory tests.
 * 
 * @author Christoph Schröder
 */
public class TestResourceConfig extends ResourceConfig {

    private PandaSettings resourceSettings = new PandaSettings();

    public TestResourceConfig() {

        setApplicationName("PANDA TEST");
        register(JerseyRootResource.class);

        // set standard settings for tests
        // cache and VTD usage has to be set explicitly
        // test if adding values to cache works
        // note: each test will initialize a new InMemoryTestContainer which
        // will avoid values coming from cache
        resourceSettings.setServerCacheUsage(false);
        resourceSettings.setVtdUsage(false);
        resourceSettings.setNamespaceAwareness(false);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(resourceSettings);
            }
        });

    }

    public PandaSettings getResourceSettings() {
        return this.resourceSettings;
    }
}
