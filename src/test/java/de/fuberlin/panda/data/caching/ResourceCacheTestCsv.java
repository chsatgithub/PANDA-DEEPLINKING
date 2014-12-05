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

public class ResourceCacheTestCsv {
    private final String  rootPath  = "src/test/resources/testdata/";
    private ResourceCache testCache = new ResourceCache(new PandaSettings());

    /**
     * Setup a cache with a prepared result from JerseyCsvResourceTest. Note: If
     * the value request test fail there might also be a problem with adding
     * values. For positive test results for requests via getValues(...) method
     * both (add + get) have to work fine!
     */
    @Before
    public void setupCache() {
        try {
            File docReqResultFile = new File(rootPath + "csv/documentRequest.xml");
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
                testCache.addResourceValue(uri, value, type, DataResourceType.CSV);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCacheDocumentRequestCsv() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/CsvTest/*", DataResourceType.CSV);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/documentRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheDocumentRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheSingleCellRequestCsv() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/CsvTest/I4000", DataResourceType.CSV);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/singleCellRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheSingleCellRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheRowRequestCsv() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/CsvTest/*25", DataResourceType.CSV);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/rowRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheRowRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheColumnRequestCsv() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/CsvTest/H*", DataResourceType.CSV);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/columnRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheColumnRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheTableRequestCsv() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/CsvTest/B2:E5", DataResourceType.CSV);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath + "csv/tableRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheTableRequestCsv: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

}
