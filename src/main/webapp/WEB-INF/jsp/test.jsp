<%--
  #%L
  PANDA-DEEPLINKING
  %%
  Copyright (C) 2014 Freie Universitaet Berlin
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public
  License along with this program.  If not, see
  <http://www.gnu.org/licenses/gpl-3.0.html>.
  #L%
  --%>
<%@page import="de.fuberlin.panda.data.configuration.PandaSettings"%>
<%@page import="de.fuberlin.panda.enums.DataResourceType"%>
<%@page import="de.fuberlin.panda.api.data.PandaAdministrationBean"%>
<%@page import="java.util.Map"%>
<%@page import="javax.ws.rs.core.MediaType"%>
<%@page import="java.util.List"%>
<%@page import="java.util.LinkedList"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>PANDA Configuration</title>
</head>
<body>
	<%
	    PandaAdministrationBean formBean = (PandaAdministrationBean) request.getAttribute("it");
	    PandaSettings pandaSettings = formBean.getPandaSettings();
	    String result = formBean.getResult();
	    List<MediaType> supportedMediaTypes = new LinkedList<MediaType>();
	    supportedMediaTypes.add(MediaType.APPLICATION_XML_TYPE);
	    supportedMediaTypes.add(MediaType.APPLICATION_JSON_TYPE);
	    supportedMediaTypes.add(MediaType.TEXT_PLAIN_TYPE);
	%>
	<h1>PANDA Test</h1>

	<form method="post" name="testrequest" action="test">
		<table>
			<tr>
				<td>Base URI:</td>
				<td><input name="baseURI" type="text" size="50" maxlength="256"
					<%=(formBean.getBaseURI() != null) ? " value='" + formBean.getBaseURI() + "'"
                    : "value='http://localhost:8080/PANDA-DEEPLINKING/rest/data/'"%>></td>
			</tr>
			<tr>
				<td>Resource:</td>
				<td><select name="resourceID" size="1">
						<%
						    for (String resID : pandaSettings.getResourceMap().getMap().keySet()) {
						        StringBuilder resourceOption = new StringBuilder();
						        DataResourceType type = pandaSettings.getResourceMap().getMap().get(resID)
						                .getType();
						        String resourceID = resID + " (" + type.name() + ")";

						        resourceOption.append("<option");
						        if (resourceID.equals(formBean.getResourceID())) {
						            resourceOption.append(" selected='selected'");
						        }
						        resourceOption.append(">" + resourceID + "</option>");
						        out.print(resourceOption.toString());
						    }
						%>
				</select></td>
			</tr>
			<tr>
				<td>Resource Path:</td>
				<td><input name="resourcePath" type="text" size="50"
					maxlength="256"
					<%=(formBean.getResourcePath() != null) ? " value='"
                    + formBean.getResourcePath() + "'" : ""%>></td>
			</tr>
			<tr>
				<td>MediaType:</td>
				<td><select name="mediaType" size="1">
						<%
						    for (MediaType mediaType : supportedMediaTypes) {
						        StringBuilder outputOption = new StringBuilder();
						        outputOption.append("<option");
						        if (mediaType.equals(formBean.getMediaType())) {
						            outputOption.append(" selected='selected'");
						        }
						        outputOption.append(">" + mediaType + "</option>");
						        out.print(outputOption.toString());
						    }
						%>
				</select></td>
			</tr>
		</table>
		<br /> <input type="submit" value="Send Request" />
	</form>
	<%-- 	<table>
		<tr>
			<td><h2>Result:</h2></td>
		</tr>
		<tr>
			<td><textarea name="result" cols="100" rows="20"><%=(result != null) ? result : ""%></textarea></td>
		</tr>
	</table> --%>

	<%
	    MediaType mediaType = formBean.getMediaType();
	    String aceMode;
	    if (mediaType == null) {
	        aceMode = "'ace/mode/text'";
	    } else if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
	        aceMode = "'ace/mode/xml'";
	    } else if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
	        aceMode = "'ace/mode/json'";
	    } else if (mediaType.equals(MediaType.TEXT_PLAIN_TYPE)) {
	        aceMode = "'ace/mode/text'";
	    } else {
	        aceMode = "'ace/mode/text'";
	    }
	%>
	<br />
	<table>
		<tr>
			<td>Requested URI:</td>
			<td><input name="requestedURI" type="text" size="100"
				maxlength="256"
				<%=(formBean.getRequestedURI() != null) ? " value='"
                    + formBean.getRequestedURI() + "'" : ""%>></td>
		</tr>
	</table>
	<!-- load ace -->
	<script src="./../../webjars/ace/01.08.2014/src/ace.js"></script>
	<div class="scrollmargin"></div>
	<pre id="editor"><%=(result != null) ? result : ""%></pre>

	<script>
		var editor = ace.edit("editor");
		editor.setTheme("ace/theme/tommorow");
		editor.session.setMode(
	<%=aceMode%>
		);
		editor.setAutoScrollEditorIntoView(true);
		editor.setOption("minLines", 1);
		editor.setOption("maxLines", 25);
		editor.getSession().setUseWrapMode(true);
		editor.setReadOnly(true);
		editor.setShowPrintMargin(false);
	</script>

</body>
</html>