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


import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.custommonkey.xmlunit.Diff;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.fuberlin.panda.data.configuration.resourcemap.ResourceMap;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;
import de.fuberlin.panda.enums.DataResourceType;

public class JerseyXmlResourceTest extends JerseyTest {
    private TestResourceConfig testConfig;
    private final String       rootPath = "src/test/resources/testdata/";

    @Override
    protected Application configure() {
        this.testConfig = new TestResourceConfig();
        testConfig.getResourceSettings().setResourceMap(this.getResourceMap());
        testConfig.register(JerseyExcelResource.class);
        testConfig.getResourceSettings().setNamespaceAwareness(true);
        return testConfig;
    }

    @Override
    public TestContainerFactory getTestContainerFactory() {
        return new InMemoryTestContainerFactory();
    }

    private ResourceMap getResourceMap() {
        String filePath = rootPath + "xml/02050399.xml";
        ResourceMap resourceMap = new ResourceMap();
        HashMap<String, ResourceInfo> resMap = new HashMap<String, ResourceInfo>();
        resMap.put("XmlTest", new ResourceInfo(DataResourceType.XML, filePath));
        resourceMap.setMap(resMap);
        return resourceMap;
    }

    @Test
    public void documentRequestXml() throws SAXException, IOException {
        final String response = target("/data/XmlTest//").request(MediaType.APPLICATION_XML).get(
                String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "xml/documentRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("documentRequestXml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void singleAttributeRequestXml() throws SAXException, IOException {
        final String response = target("/data/XmlTest//Result/primaryTopic/PreviousNames/@href")
                .request(MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "xml/singleAttributeRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("singleAttributeRequestXml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void multipleAttributeRequestXml() throws SAXException, IOException {
        final String response = target("/data/XmlTest//Result/primaryTopic/*/@href").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "xml/multipleAttributeRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("multipleAttributeRequestXml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void singleElementRequestXml() throws SAXException, IOException {
        final String response = target("/data/XmlTest//Result/primaryTopic/*/SicText").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "xml/singleElementRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("singleElementRequestXml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void multipleElementRequestXml() throws SAXException, IOException {
        final String response = target("/data/XmlTest//Result/primaryTopic/*[@href]").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "xml/multipleElementRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("multipleElementRequestXml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void elementByIndexXml() throws SAXException, IOException {
        final String response = target("/data/XmlTest//Result/primaryTopic/*[3]").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "xml/elementByIndexRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("elementByIndexXml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

}
