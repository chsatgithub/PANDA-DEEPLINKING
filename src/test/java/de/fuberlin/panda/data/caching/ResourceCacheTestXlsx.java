package de.fuberlin.panda.data.caching;

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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.custommonkey.xmlunit.Diff;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.fuberlin.panda.api.data.ValueExchange;
import de.fuberlin.panda.api.data.ValueExchange.Value;
import de.fuberlin.panda.api.jersey.TestHelper;
import de.fuberlin.panda.data.configuration.PandaSettings;
import de.fuberlin.panda.enums.DataResourceType;

public class ResourceCacheTestXlsx {
    private final String  rootPath  = "src/test/resources/testdata/";
    private ResourceCache testCache = new ResourceCache(new PandaSettings());

    /**
     * Setup a cache with a prepared result from JerseyExcelResourceTest. Note:
     * If the value request test fail there might also be a problem with adding
     * values. For positive test results for requests via getValues(...) method
     * both (add + get) have to work fine!
     */
    @Before
    public void setupCache() {
        try {
            File docReqResultFile = new File(rootPath + "msexcel/documentRequest.xml");
            // unmarshal document request results from excel resource testCache
            // and
            // add them to resource cache
            JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ValueExchange docReqResult = (ValueExchange) unmarshaller.unmarshal(docReqResultFile);
            String baseURI = docReqResult.getBaseURI();
            for (Value v : docReqResult.getValue()) {
                String uri = baseURI + v.getSubURI();
                String type = v.getType();
                String value = (String) v.getValue();
                testCache.addResourceValue(uri, value, type, DataResourceType.XLSX);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCacheDocumentRequestXlsx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/*/*",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/documentRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheDocumentRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheSingleCellRequestXlsx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/DRL0110/A6",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleCellRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheSingleCellRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheSingleRowRequestXlsx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/DRL0110/*27",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleRowRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheSingleRowRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheSingleColumnRequestXlsx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/DRL0110/C*",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/singleColumnRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheSingleColumnRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheMultipleCellRequestXlsx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/*/A6",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleCellRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheMultipleCellRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheMultipleRowRequestXlsx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/*/*27",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleRowRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheMultipleRowRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheMultipleColumnRequestXlsx() throws JAXBException, SAXException,
            IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/*/C*",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/multipleColumnRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheMultipleColumnRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheSheetRequestXlsx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/DRL0110/*",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath + "msexcel/sheetRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheSheetRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheTableAreaRequestXlsx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/ExcelTest/tables/DRL0110/C26:F29",
                DataResourceType.XLSX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msexcel/tableAreaRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheTableAreaRequestXlsx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

}
