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

public class JerseyHtmlResourceTest extends JerseyTest {
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
        String filePath = rootPath + "html/02050399.html";
        ResourceMap resourceMap = new ResourceMap();
        HashMap<String, ResourceInfo> resMap = new HashMap<String, ResourceInfo>();
        resMap.put("HtmlTest", new ResourceInfo(DataResourceType.HTML, filePath));
        resourceMap.setMap(resMap);
        return resourceMap;
    }

    @Test
    public void documentRequestHtml() throws SAXException, IOException {
        final String response = target("/data/HtmlTest//").request(MediaType.APPLICATION_XML).get(
                String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "html/documentRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("documentRequestHtml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void singleAttributeRequestHtml() throws SAXException, IOException {
        final String response = target("/data/HtmlTest//html/body/div/div/a/img/@src").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "html/singleAttributeRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("singleAttributeRequestHtml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void multipleAttributeRequestHtml() throws SAXException, IOException {
        final String response = target("/data/HtmlTest//html/head/link/@href").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "html/multipleAttributeRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("multipleAttributeRequestHtml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void singleElementRequestHtml() throws SAXException, IOException {
        final String response = target("/data/HtmlTest//html//h1").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "html/singleElementRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("singleElementRequestHtml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void multipleElementByIndeHxtml() throws SAXException, IOException {
        final String response = target("/data/HtmlTest///table/tr[2]").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "html/multipleElementByIndexRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("multipleElementByIndeHxtml: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

}
