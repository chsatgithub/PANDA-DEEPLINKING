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

public class JerseyCsvResourceTest extends JerseyTest {

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
        String filePath = rootPath + "csv/MOTsitelist.csv";
        
        ResourceMap resourceMap = new ResourceMap();
        HashMap<String, ResourceInfo> resMap = new HashMap<String, ResourceInfo>();
        ResourceInfo resInfo = new ResourceInfo(DataResourceType.CSV, filePath);
        resInfo.setSeparator(",");
        resMap.put("CsvTest", resInfo);
        resourceMap.setMap(resMap);
        return resourceMap;
    }

    @Test
    public void testDocumentRequestCsv() throws SAXException, IOException {
        final String response = target("/data/CsvTest/*").request(MediaType.APPLICATION_XML).get(
                String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/documentRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testDocumentRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSingleCellRequestCsv() throws SAXException, IOException {
        final String response = target("/data/CsvTest/I4000").request(MediaType.APPLICATION_XML)
                .get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/singleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleCellRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testRowRequestCsv() throws SAXException, IOException {
        final String response = target("/data/CsvTest/*25").request(MediaType.APPLICATION_XML).get(
                String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/rowRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testRowRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testColumnRequestCsv() throws SAXException, IOException {
        final String response = target("/data/CsvTest/H*").request(MediaType.APPLICATION_XML).get(
                String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/columnRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testColumnRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testTableRequestCsv() throws SAXException, IOException {
        final String response = target("/data/CsvTest/B2:E5").request(MediaType.APPLICATION_XML)
                .get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/tableRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testTableRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }
}
