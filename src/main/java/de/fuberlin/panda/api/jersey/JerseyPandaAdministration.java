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


import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringEscapeUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.mvc.Viewable;

import de.fuberlin.panda.api.data.PandaAdministrationBean;
import de.fuberlin.panda.enums.RangeDelimiter;

@Singleton
@Path("/admin")
public class JerseyPandaAdministration extends AbstractJerseyResource {

    private Client       client;
    private CacheControl cacheControl;

    public JerseyPandaAdministration() {
        this.prepareClient();
    }

    @GET
    @Path("configuration")
    @Produces("text/html")
    public Response getAdminForm() {
        Viewable view = new Viewable("/admin.jsp", this.pandaSettings);
        Response response = Response.ok(view).build();
        return response;
    }

    @POST
    @Path("configuration")
    @Produces("text/html")
    public Response setAdminForm(
            @DefaultValue("false") @FormParam("useServerCaching") Boolean useServerCaching,
            @DefaultValue("false") @FormParam("namespaceAwareness") Boolean namespaceAwareness,
            @DefaultValue("false") @FormParam("useClientCaching") Boolean useClientCaching,
            @DefaultValue("false") @FormParam("useJtidy") Boolean useJtidy,
            @DefaultValue("false") @FormParam("useVtdXml") Boolean useVtdXml,
            @FormParam("rangeDelimiter") RangeDelimiter rangeDelimiter) {
        this.pandaSettings.setServerCacheUsage(useServerCaching);
        this.pandaSettings.setNamespaceAwareness(namespaceAwareness);
        this.pandaSettings.setUseClientCaching(useClientCaching);
        this.pandaSettings.setUseJtidy(useJtidy);
        this.pandaSettings.setVtdUsage(useVtdXml);
        this.pandaSettings.setRangeDelimiter(rangeDelimiter);

        return getAdminForm();
    }

    @GET
    @Path("test")
    @Produces("text/html")
    public Response getTestForm() {
        PandaAdministrationBean formBean = new PandaAdministrationBean();
        formBean.setPandaSettings(this.pandaSettings);
        Viewable view = new Viewable("/test.jsp", formBean);
        Response response = Response.ok(view).build();
        return response;
    }

    @POST
    @Path("test")
    @Produces("text/html")
    public Response setTestForm(@FormParam("mediaType") MediaType mediaType,
            @FormParam("baseURI") String baseURI, @FormParam("resourcePath") String resourcePath,
            @FormParam("resourceID") String resourceID) {

        PandaAdministrationBean formBean = new PandaAdministrationBean();
        formBean.setPandaSettings(this.pandaSettings);
        formBean.setBaseURI(baseURI);
        formBean.setResourcePath(resourcePath);
        formBean.setMediaType(mediaType);
        formBean.setResourceID(resourceID);

        String resourceURI = resourceID.split(" ")[0] + "/" + resourcePath;
        String result = new String();
        try {
            result = client.target(baseURI).path(resourceURI)
                    .request(MediaType.TEXT_HTML_TYPE, mediaType).cacheControl(cacheControl)
                    .get(String.class);
            formBean.setRequestedURI(baseURI+resourceURI);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result = sw.toString();
        }

        result = StringEscapeUtils.escapeHtml4(result);
        formBean.setResult(result);
        Viewable view = new Viewable("/test.jsp", formBean);
        Response response = Response.ok(view).build();
        return response;
    }

    /**
     * Prepares a new client and cache control.
     */
    private void prepareClient() {
        Client client;
        ClientConfig clientConfig = new ClientConfig();
        client = ClientBuilder.newClient(clientConfig);
        CacheControl cacheControl;
        cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);

        this.client = client;
        this.cacheControl = cacheControl;
    }
}
