package de.fuberlin.panda.data.caching;

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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;

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

public class ResourceCacheTestDocx {
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
            LinkedList<String> files = new LinkedList<String>();
            files.add("msword/documentRequestDocX.xml");
            files.add("msword/documentTableRequestDocX.xml");
            for (String file : files) {
                File docReqResultFile = new File(rootPath + file);
                // unmarshal document request results from excel resource
                // testCache and
                // add them to resource cache
                JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                ValueExchange docReqResult = (ValueExchange) unmarshaller
                        .unmarshal(docReqResultFile);
                String baseURI = docReqResult.getBaseURI();
                for (Value v : docReqResult.getValue()) {
                    String uri = baseURI + v.getSubURI();
                    String type = v.getType();
                    String value = (String) v.getValue();
                    testCache.addResourceValue(uri, value, type, DataResourceType.DOCX);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCacheDocumentRequestDocx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/WordTest/text/*", DataResourceType.DOCX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/documentRequestDocx.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheDocumentRequestDocx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheSingleCellRequestDocx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/WordTest/tables/26/C1/2:4",
                DataResourceType.DOCX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/singleCellRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheSingleCellRequestDocx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheSingleParagraphRequestDocx() throws JAXBException, SAXException,
            IOException {
        // request values from cache
        ValueExchange valList = this.testCache
                .getValues("/WordTest/text/37", DataResourceType.DOCX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();
        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/singleParagraphRequestDocx.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheSingleParagraphRequestDocx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheSingleTableRequestDocx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/WordTest/tables/6/*/*",
                DataResourceType.DOCX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/singleTableRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheSingleTableRequestDocx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheMultipleCellRequestDocx() throws JAXBException, SAXException, IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/WordTest/tables/*/C1/*",
                DataResourceType.DOCX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/multipleCellRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheMultipleCellRequestDocx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheMultipleParagraphRequestDocx() throws JAXBException, SAXException,
            IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/WordTest/text/29:37",
                DataResourceType.DOCX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/multipleParagraphRequestDocx.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheMultipleParagraphRequestDocx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

    @Test
    public void testCacheMultipleTableAreaRequestDocx() throws JAXBException, SAXException,
            IOException {
        // request values from cache
        ValueExchange valList = this.testCache.getValues("/WordTest/tables/*/B3:E4/*",
                DataResourceType.DOCX);

        // marshal result to String
        JAXBContext jaxbContext = JAXBContext.newInstance(ValueExchange.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(valList, writer);
        final String response = writer.toString();

        // get result from JerseyExcelResource testCaches
        final String checkResponse = TestHelper.getTestFile(rootPath
                + "msword/multipleTableAreaRequest.xml");

        // Check differences
        Diff myDiff = new Diff(checkResponse, response);
        System.out.println("testCacheMultipleTableAreaRequestDocx: " + myDiff.toString());
        assertTrue(myDiff.identical());
    }

}
