package de.fuberlin.panda.api.jersey.benchmark;

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


import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * Class for manual benchmark tests of different data formats. Note: test
 * configuration (media type, iterations, path) has to be done individually
 * before test.
 * 
 * @author Christoph Schröder
 */
public class JerseyResourceBenchmark {

    private Logger      logger     = Logger.getLogger("de.fuberlin.panda.api.jersey.benchmark");

    public String       baseURI    = "http://192.168.0.10:8080/PANDA/rest/Data";
    public Client       client;
    public CacheControl cacheControl;
    public int          iterations = 100;
    public MediaType    mediaType  = MediaType.APPLICATION_XML_TYPE;

    @Before
    public void prepareTest() {
        ClientConfig clientConfig = new ClientConfig();
        client = ClientBuilder.newClient(clientConfig);

        cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
    }

    @Test
    public void benchmarkHtml() {
        String path = "testhtml//";

        try {
            /**
             * request to fill cache (if tested) and remove slowdown effects of
             * first call
             **/
            client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                    .get(String.class);

            /** benchmark using wall time **/
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                        .get(String.class);
            }
            long endTime = System.currentTimeMillis();

            /** Calculate results **/
            double averageTime = (double) (endTime - startTime) / iterations;

            logger.info("HTML Benchmark Test: " + path + "\n" + "Path: " + path + "\n"
                    + "Average Time: " + averageTime);
        } catch (Exception e) {
            // server may return 404 or refuse request
            e.printStackTrace();
        }
    }

    @Test
    public void benchmarkXml() {
        String path = "testxml//";

        try {
            /**
             * request to fill cache (if tested) and remove slowdown effects of
             * first call
             **/
            client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                    .get(String.class);

            /** benchmark using wall time **/
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                        .get(String.class);
            }
            long endTime = System.currentTimeMillis();

            /** Calculate results **/
            double averageTime = (double) (endTime - startTime) / iterations;

            logger.info("XML Benchmark Test: " + path + "\n" + "Path: " + path + "\n"
                    + "Average Time: " + averageTime);
        } catch (Exception e) {
            // server may return 404 or refuse request
            e.printStackTrace();
        }
    }

    @Test
    public void benchmarkDoc() {
        String path;
        // specific page of test document
        path = "testdoc/text/34:56";
        // all tables of test document
        path = "testdoc/tables/*/*/*";

        try {
            /**
             * request to fill cache (if tested) and remove slowdown effects of
             * first call
             **/
            client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                    .get(String.class);

            /** benchmark using wall time **/
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                        .get(String.class);
            }
            long endTime = System.currentTimeMillis();

            /** Calculate results **/
            double averageTime = (double) (endTime - startTime) / iterations;

            logger.info("DOC Benchmark Test: " + path + "\n" + "Path: " + path + "\n"
                    + "Average Time: " + averageTime);
        } catch (Exception e) {
            // server may return 404 or refuse request
            e.printStackTrace();
        }
    }

    @Test
    public void benchmarkDocx() {
        String path;
        // specific page of test document
        path = "testdocx/text/15:37";
        // all tables of test document
        path = "testdocx/tables/*/*/*";

        try {
            /**
             * request to fill cache (if tested) and remove slowdown effects of
             * first call
             **/
            client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                    .get(String.class);

            /** benchmark using wall time **/
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                        .get(String.class);
            }
            long endTime = System.currentTimeMillis();

            /** Calculate results **/
            double averageTime = (double) (endTime - startTime) / iterations;

            logger.info("DOCX Benchmark Test: " + path + "\n" + "Path: " + path + "\n"
                    + "Average Time: " + averageTime);
        } catch (Exception e) {
            // server may return 404 or refuse request
            e.printStackTrace();
        }
    }

    @Test
    public void benchmarkPdf() {
        // single page
        String path = "testpdf/text/3/*";

        try {
            /**
             * request to fill cache (if tested) and remove slowdown effects of
             * first call
             **/
            client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                    .get(String.class);

            /** benchmark using wall time **/
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                        .get(String.class);
            }
            long endTime = System.currentTimeMillis();

            /** Calculate results **/
            double averageTime = (double) (endTime - startTime) / iterations;

            logger.info("PDF Benchmark Test: " + path + "\n" + "Path: " + path + "\n"
                    + "Average Time: " + averageTime);
        } catch (Exception e) {
            // server may return 404 or refuse request
            e.printStackTrace();
        }
    }

    @Test
    public void benchmarkXls() {
        String path;
        // all tables of test document
        path = "testxls/tables/*/*";
        // single row
        path = "testxls/tables/DRL0110/*27";
        // single column
        path = "testxls/tables/DRL0110/C*";
        // single cell
        path = "testxls/tables/DRL0110/A6";

        try {
            /**
             * request to fill cache (if tested) and remove slowdown effects of
             * first call
             **/
            client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                    .get(String.class);

            /** benchmark using wall time **/
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                        .get(String.class);
            }
            long endTime = System.currentTimeMillis();

            /** Calculate results **/
            double averageTime = (double) (endTime - startTime) / iterations;

            logger.info("XLS Benchmark Test: " + path + "\n" + "Path: " + path + "\n"
                    + "Average Time: " + averageTime);
        } catch (Exception e) {
            // server may return 404 or refuse request
            e.printStackTrace();
        }
    }

    @Test
    public void benchmarkXlsx() {
        String path;
        // all tables of test document
        path = "testxlsx/tables/*/*";
        // single row
        path = "testxlsx/tables/DRL0110/*27";
        // single column
        path = "testxlsx/tables/DRL0110/C*";
        // single cell
        path = "testxlsx/tables/DRL0110/A6";

        try {
            /**
             * request to fill cache (if tested) and remove slowdown effects of
             * first call
             **/
            client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                    .get(String.class);

            /** benchmark using wall time **/
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                client.target(baseURI).path(path).request(mediaType).cacheControl(cacheControl)
                        .get(String.class);
            }
            long endTime = System.currentTimeMillis();

            /** Calculate results **/
            double averageTime = (double) (endTime - startTime) / iterations;

            logger.info("XLS Benchmark Test: " + path + "\n" + "Path: " + path + "\n"
                    + "Average Time: " + averageTime);
        } catch (Exception e) {
            // server may return 404 or refuse request
            e.printStackTrace();
        }
    }

}
