.. topic:: Description of the NERD console.

(N)ERD Console
==============

The (N)ERD console is a **graphical web interface**, part of the (N)ERD project, providing means to test the service and explore the functionalities. With the console, it is possible to process chunks of text, PDF files and to verify which entities are recognised and how they are disambiguated.

The console is also a **reference implementation** in javascript (with JQuery) of a web application using the (N)ERD API service. As such, it illustrates how to call the services with mainstream Ajax queries and how to parse JSON results with *vulgus JQuery*.

The console is available at the root address of the server (e.g. for Tomcat at `http://<server instance name>/<root context name>`, or `http://localhost:8080` for jetty deployed instance).

The About page provides licence (Open Source Apache 2 licence for the entire tool including used dependencies) and contact information.

.. image:: images/nerdConsole1.png

The web page "Services" allows to test the different REST requests.

.. image:: images/nerdConsole2.png

A free text form allows the analysis of any text. On the right side of the input form, samples of text can be found, from news, historical documents, etc.

In the lower part, entities are recognised in the provided text and displayed using different colors, based on the entity type and domain. On the lower right side, an infobox is displaying various information provided by the service about the disambiguated Wikidata/Wikipedia entity.

In this example the text box is used to disambiguate a search query:

.. image:: images/nerdConsole3.png

The console allows to test all the different services provided by (N)ERD, e.g. it’s possible to visualise the various sentences identified by the the sentence segmentation service (more details on this specific service in the REST API documentation).

.. image:: images/nerdConsole4.png

In addition, it is possible to view the service raw response (in JSON format) for helping the integration phase:

.. image:: images/nerdConsole5.png

More details about the response in the next section.



