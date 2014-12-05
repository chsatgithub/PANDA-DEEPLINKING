<%--
  #%L
  PANDA-DEEPLINKING
  %%
  Copyright (C) 2014 Freie Universität Berlin
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
<%@page import="de.fuberlin.panda.enums.RangeDelimiter"%>
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
	    PandaSettings pandaSettings = (PandaSettings) request.getAttribute("it");
	%>
	<h1>PANDA Configuration</h1>
	<form method="post" name="pandaConfig" action="configuration">
		<table>
			<tr>
				<td>Server Caching:</td>
				<td><input type="checkbox" name="useServerCaching" value="true"
					<%=pandaSettings.getServerCacheUsage() ? "checked='checked'" : ""%>></td>
			</tr>
			<tr>
				<td>Namespace Awareness:</td>
				<td><input type="checkbox" name="namespaceAwareness"
					value="true"
					<%=pandaSettings.getNamespaceAwareness() ? "checked='checked'" : ""%>></td>
			</tr>
			<tr>
				<td>Client Caching:</td>
				<td><input type="checkbox" name="useClientCaching" value="true"
					<%=pandaSettings.getUseClientCaching() ? "checked='checked'" : ""%>></td>
			</tr>
			<tr>
				<td>JTidy:</td>
				<td><input type="checkbox" name="useJtidy" value="true"
					<%=pandaSettings.getUseJtidy() ? "checked='checked'" : ""%>></td>
			</tr>
			<tr>
				<td>VTD-XML:</td>
				<td><input type="checkbox" name="useVtdXml" value="true"
					<%=pandaSettings.getVtdUsage() ? "checked='checked'" : ""%>></td>
			</tr>
			<tr>
				<td>Range Delimiter:</td>
				<td><select name="rangeDelimiter" size="1">
						<%
						    for (RangeDelimiter rangeDelimiter : RangeDelimiter.values()) {
						        StringBuilder option = new StringBuilder();
						        option.append("<option");
						        if (rangeDelimiter == pandaSettings.getRangeDelimiter()) {
						            option.append(" selected='selected'");
						        }
						        option.append(">" + rangeDelimiter.name() + "</option>");
						        out.print(option.toString());
						    }
						%>
				</select></td>
			</tr>
		</table>
		<br /> <input type="submit" value="Set Configuration" />
	</form>
</body>
</html>
