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

public class JerseyExcelResourceXlsTest extends JerseyTest {
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
        String filePath = rootPath + "msexcel/Driving-Licence-Tables-Nov2012.xls";
        ResourceMap resourceMap = new ResourceMap();
        HashMap<String, ResourceInfo> resMap = new HashMap<String, ResourceInfo>();
        resMap.put("ExcelTest", new ResourceInfo(DataResourceType.XLS, filePath));
        resourceMap.setMap(resMap);
        return resourceMap;
    }

    @Test
    public void testSingleCellRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/DRL0110/A6").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleCellRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSingleRowRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/DRL0110/*27").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleRowRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleRowRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSingleColumnRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/DRL0110/C*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleColumnRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSingleColumnRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleCellRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/*/A6").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleCellRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleCellRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleRowRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/*/*27").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleRowRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleRowRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testMultipleColumnRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/*/C*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleColumnRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testMultipleColumnRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testSheetRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/DRL0110/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath + "msexcel/sheetRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testSheetRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testDocumentRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/*/*").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/documentRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testDocumentRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testTableAreaRequestXls() throws SAXException, IOException {
        final String response = target("/data/ExcelTest/tables/DRL0110/C26:F29").request(
                MediaType.APPLICATION_XML).get(String.class);
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/tableAreaRequest.xml");
        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testTableAreaRequestXls: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

}
