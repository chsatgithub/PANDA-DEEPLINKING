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

public class JerseyWordResourceDocTest extends JerseyTest {
    private TestResourceConfig testConfig;
    private final String       rootPath = "src/test/resources/testdata/";

    @Override
    protected Application configure() {
        this.testConfig = new TestResourceConfig();
        testConfig.getResourceSettings().setResourceMap(this.getResourceMap());
        testConfig.register(JerseyExcelResource.class);
        return testConfig;
    }

    @Override
    public TestContainerFactory getTestContainerFactory() {
        return new InMemoryTestContainerFactory();
    }

    private ResourceMap getResourceMap() {
        String filePath = rootPath + "msword/July_Spend_Publication_v1.0.doc";
        ResourceMap resourceMap = new ResourceMap();
        HashMap<String, ResourceInfo> resMap = new HashMap<String, ResourceInfo>();
        resMap.put("WordTest", new ResourceInfo(DataResourceType.DOC, filePath));
        resourceMap.setMap(resMap);
        return resourceMap;
    }

    @Test
    public void testDocumentRequestDoc() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/WordTest/text/*").request(MediaType.APPLICATION_XML)
                .get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/documentRequestDoc.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testDocumentRequestDoc: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void multipleCellRequestDoc() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/WordTest/tables/*/C1/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/multipleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("multipleCellRequestDoc: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void multipleParagraphRequestDoc() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/WordTest/text/48:56").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/multipleParagraphRequestDoc.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("multipleParagraphRequestDoc: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void multipleTableAreaRequestDoc() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/WordTest/tables/*/B3:E4/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/multipleTableAreaRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("multipleTableAreaRequestDoc: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void singleCellRequestDoc() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/WordTest/tables/26/C1/2:4").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/singleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("singleCellRequestDoc: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void singleParagraphRequestDoc() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/WordTest/text/56").request(MediaType.APPLICATION_XML)
                .get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/singleParagraphRequestDoc.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("singleParagraphRequestDoc: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void singleTableRequestDoc() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/WordTest/tables/6/*/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/singleTableRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("singleTableRequestDoc: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

}
