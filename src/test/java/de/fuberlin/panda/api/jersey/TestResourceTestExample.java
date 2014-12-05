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


import static org.junit.Assert.*;

import java.io.IOException;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.custommonkey.xmlunit.Diff;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * This class is an example for in memory JUnit tests of Jersey resources using
 * the DataTestResource.
 * 
 * @author Christoph Schröder
 */
public class TestResourceTestExample extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig testConfig = new TestResourceConfig();
        testConfig.register(TestResource.class);
        return testConfig;
    }

    @Override
    public TestContainerFactory getTestContainerFactory() {
        return new InMemoryTestContainerFactory();
    }

    @Test
    public void testSayHello() {
        final String response = target("/data/TestData").request(MediaType.TEXT_PLAIN).get(
                String.class);
        assertEquals("Hello!", response);
    }

    @Test
    public void testGetValueExample() throws SAXException, IOException {
        final String response = target("/data/TestData/XmlTest/Bulk").request(
                MediaType.APPLICATION_XML).get(String.class);

        final String checkResponse = TestHelper
                .getTestFile("WebContent/testData/valueExampleBulk.xml");

        Diff myDiff = new Diff(checkResponse, response);
        assertTrue(myDiff.identical());
    }
}
