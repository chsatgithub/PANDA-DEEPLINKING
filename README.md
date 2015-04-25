# PANDA-DEEPLINKING

*** Overview ***

This software was developed as the result of a project with the purpose to show whether and how data from typical data formats of "Open Data" can be linked to the web. It is focused on performance and thus different kinds of access methods were used starting with high-level frameworks down to direct access to underlying file structures. This project can be seen as example for further development of other applications. Some parts of it are still in an experimental state developed for testing purposes and are NOT meant to be used in a productive environment in its current state.

Different data formats were analyzed in terms of structure to gather information that can generally be used for access to the data. Those information were used to form URI pattern with the ability to reference a single piece or a bulk of data. The URI patterns are neither complete nor unchangeable. They are just examples and you may change or extend them. However, this would also require an alteration of the data access part.


*** Configuration ***

There are 2 ways to put information in an URI. One could code them directly into the URI. This is useful for dynamic information regarding the data structure. These information are chosen by the client as query to retrieve specific parts of the data. However this won't be appropriate for some kinds of static information e.g. a file path or access information for a database which you don't want hardcoded in your URI. Furthermore those information might change which would lead to broken URIs. Therefore those information may be represented as an ID in the URI and retrieved from a configuration stored on the server.

In PANDA this is done by a JAXB class (configurations can be saved as XML):
"\de\fuberlin\panda\data\configuration\resourcemap"


*** Interface ***

After deploying the project on a web server, the data can simply be accessed through http requests. The interface provided for the clients uses "Jersey", the reference implementation of REST for Java to provide an interface to the client. You will not need a REST complying client, however it will be necessary to use all caching functions.
The REST interface supports different formats for representation chosen by content negotiation between client and server. In the current state a XML and a JSON representation was implemented for all data formats except images. Furthermore a simple text representation can be retrieved for the text formats of MS Word and PDF.
The structure of the representation is also just an example and can be easily changed, by altering the JAXB class.


PANDA provides 2 panels for configuration and testing purposes:
http://localhost:8080/PANDA-DEEPLINKING/rest/admin/configuration
http://localhost:8080/PANDA-DEEPLINKING/rest/admin/test
Note: The panels are not password protected. Remove or protect the panels if you plan to use the software in a productive environment.

Test whether the service is online:
http://localhost:8080/PANDA-DEEPLINKING/


*** Data formats/URI pattern ***

The implemented data formats were chosen according to their importance to serveral open data platforms to get a broad representation of data formats used to publish open data. 
The following list will show all implemented data formats and the URI patterns as regular expressions used in Jersey.

** URI pattern **
Base URI: "http://localhost:8080/PANDA-DEEPLINKING/rest/data/"

The base URI identifies our REST service. This part can be altered without braking the identity of the values.
Values are identified by a relative URI with an absolute path (see RFC 3986). The following section will describe the pattern of these relative URIs that identify a single value or a bulk of values. A request e.g. in a web browser can be done by combining the base URI and the relative sub URI that identifies the values.


** Comma Separated Values **
Pattern: "{ResourceID}/{Reference:(\\*|[A-Z][A-Z]*[1-9][0-9]*|\\*[1-9][0-9]*|[A-Z][A-Z]*\\*|[A-Z][A-Z]*[1-9][0-9]*[\\-\\:][A-Z][A-Z]*[1-9][0-9]*)}"

Examples: 
"A1" for a single cell
"A1:D4" or "A1-D4" to reference a table (Note: the separator ":" or "-" can be configured)
"A*" for column "A"
"*1" for row "1" 


** MS Excel **
Pattern: "{ResourceID}/tables/{SheetName}/{Reference:(\\*|[A-Z][A-Z]*[1-9][0-9]*|\\*[1-9][0-9]*|[A-Z][A-Z]*\\*|[A-Z][A-Z]*[1-9][0-9]*[\\-\\:][A-Z][A-Z]*[1-9][0-9]*)}"

Examples:
Same as CSV plus "tables" to tell the service, that you want to reference table data and the name of the sheet you want to access. Use "*" as sheet name to reference a cell, column etc. from all sheets.

You can also reference used pictures by their internal order:
Pattern: "{ResourceID}/pictures/{PictureID: (0|[1-9][0-9]*)}"


** XML/HTML **
Pattern: "{ResourceID}/{XPathExp:.*}"
All valid XPath (Version 1) expressions. Note: The expression starts after the last "/", therefore the URI for a request of the entire document looks like "http://localhost:8080/PANDA-DEEPLINKING/rest/{ResourceID}//".


** PDF **
Pattern for text: "{ResourceID}/text/{PageRef:(\\*|[1-9][0-9]*|[1-9][0-9]*[\\-\\:][1-9][0-9]*)}/{LineRef:(\\*|[1-9][0-9]*|[1-9][0-9]*[\\-\\:][1-9][0-9]*)}"

Pattern for pictures: "{ResourceID}/pictures/{PageRef: ([1-9][0-9]*)}/{PictureID: ([1-9][0-9]*)}"


** Word **
Pattern for text: "{ResourceID}/text/{Paragraph: (\\*|[1-9][0-9]*|[1-9][0-9]*[\\-\\:][1-9][0-9]*)}"

Pattern for tables: "{ResourceID}/tables/{TablePos: (\\*|[1-9][0-9]*)}/{Table:(\\*|[A-Z][A-Z]*[1-9][0-9]*|\\*[1-9][0-9]*|[A-Z][A-Z]*\\*|[A-Z][A-Z]*[1-9][0-9]*[\\-\\:][A-Z][A-Z]*[1-9][0-9]*)}/{Paragraph: (\\*|[1-9][0-9]*|[1-9][0-9]*[\\-\\:][1-9][0-9]*)}"

Pattern for pictures: "{ResourceID}/pictures/{PictureID: (0|[1-9][0-9]*)}"


*** Caching ***

This project implements 2 types of caching:
- client side caching
- server side caching

The client caching uses cache control information according to the REST standard.

The server caching is an experimental attempt, to convert the data into a hierarchical structure according to the URI patterns. This expects the URI patterns to meet the requirement, that each sub part of an URIs path segment is unique itself. 
This method will still require the server to resolve an URI and filter the requested data. The advantage of this attempt is, that it has a low memory consumption. There won't be any redundant data or data that is not needed for our purpose e.g. style information. Be aware that there is still no cache invalidation strategy and it may not be used in a productive environment.

*** Test Data ***
Test data originated from:
http://data.gov.uk
http://www.data.gov

License of data from data.gov.uk: "Open Government License"
 


