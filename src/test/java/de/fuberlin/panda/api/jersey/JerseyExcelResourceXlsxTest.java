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

public class JerseyExcelResourceXlsxTest extends JerseyTest {
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
        String filePath = rootPath + "msexcel/Driving-Licence-Tables-Nov2012.xlsx";
        ResourceMap resourceMap = new ResourceMap();
        HashMap<String, ResourceInfo> resMap = new HashMap<String, ResourceInfo>();
        resMap.put("ExcelTest", new ResourceInfo(DataResourceType.XLSX, filePath));
        resourceMap.setMap(resMap);
        return resourceMap;
    }

    @Test
    public void testSingleCellRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/ExcelTest/tables/DRL0110/A6").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleCellRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSingleCellRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("data/ExcelTest/tables/DRL0110/A6").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleCellRequestVtdXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSingleRowRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("data/ExcelTest/tables/DRL0110/*27").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleRowRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleRowRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSingleRowRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("data/ExcelTest/tables/DRL0110/*27").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleRowRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleRowRequestVtdXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSingleColumnRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("data/ExcelTest/tables/DRL0110/C*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleColumnRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleColumnRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSingleColumnRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("data/ExcelTest/tables/DRL0110/C*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleColumnRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleColumnRequestVtdXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleCellRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("data/ExcelTest/tables/*/A6").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleCellRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleCellRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("data/ExcelTest/tables/*/A6").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleCellRequestVtdXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleRowRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("data/ExcelTest/tables/*/*27").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleRowRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleRowRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleRowRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("data/ExcelTest/tables/*/*27").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleRowRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleRowRequestVtdXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleColumnRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("data/ExcelTest/tables/*/C*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleColumnRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleColumnRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleColumnRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("data/ExcelTest/tables/*/C*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleColumnRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleColumnRequestVtdXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSheetRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("data/ExcelTest/tables/DRL0110/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "msexcel/sheetRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSheetRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSheetRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("data/ExcelTest/tables/DRL0110/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "msexcel/sheetRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSheetRequestVtdXlsx:");
        System.out.println(myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testDocumentRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("data/ExcelTest/tables/*/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/documentRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testDocumentRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testDocumentRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("data/ExcelTest/tables/*/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/documentRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testDocumentRequestVtdXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testTableAreaRequestVtdXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(true);
        final String response = target("/data/ExcelTest/tables/DRL0110/C26:F29").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/tableAreaRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testTableAreaRequestVtdXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testTableAreaRequestXlsx() throws SAXException, IOException {
        testConfig.getResourceSettings().setVtdUsage(false);
        final String response = target("/data/ExcelTest/tables/DRL0110/C26:F29").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/tableAreaRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testTableAreaRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }
}
