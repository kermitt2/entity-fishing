(N)ERD REST API
===============

As RESTful web services, (N)ERD is defined by a certain number of stateless transformations of data made available to "consumers" via its interface.

All these RESTful services are available through Cross-origin resource sharing (CORS), allowing clients, such as web browser and server to interact in a flexible manner with cross-origin request.


NERD text processing
********************


The NERD text processing service takes as input a JSON structured query and returns the JSON query enriched with a list of identified and, when possible, disambiguated entities.
The NERD service can be applied on 4 types of input:
 * **text**, provided as JSON string value, for example one or several paragraphs of natural language,
 * **search query**, provided as JSON string value, corresponding to several search terms used together and which can possibly be disambiguated when associated,
 * **weighted vector of terms**, provided by a structured JSON array, where each term will be disambiguated, when possible, in the context of the complete vector - weighted vector of term is a very common structure used in information retrieval, clustering and classification.
 * **PDF document**, provided as multipart data with the JSON query string.

One and only one input type is mandatory in a query, otherwise an HTTP error 400 is returned (see response status codes below). Multiple inputs are not possible in a single request.


Supported languages
-------------------

In the current version, only English, French and German are supported. We plan to extend the support to with Spanish and Italian in the next iterations.

The service returns an HTTP error 406 if the language of the text to be processed is not supported, see below.

Response status codes
---------------------
In the following table are listed the status codes returned by this entry point.

.. table:: 
   :widths: auto

   ===================  ========================================================
     HTTP Status code    Reason
   ===================  ========================================================
         200               Successful operation.
         400               Wrong request, missing parameters, missing header
         404               Indicates property was not found
         406               The language is not supported
         500               Indicate an internal service error
   ===================  ========================================================


REST query
----------

POST /disambiguate
^^^^^^^^^^^^^^^^^^

.. table:: Parameters
   :widths: auto

(1) Parameters

==========  =======  =====================  =====================================
 required    name     content-type value      description
==========  =======  =====================  =====================================
 required    query    application/json       Query to be processed in JSON UTF-8
 required    file     multipart/form-data    PDF file (as multipart)
==========  =======  =====================  =====================================

(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------+------------------+--------------------------------------+
| required | name   | value            | description                          |
+==========+========+==================+======================================+
| optional | Accept | application/json | Set the response type of the output  |
+----------+--------+------------------+--------------------------------------+


Query format description
------------------------

The NERD text processing service always consumes a parameter which is a JSON string representing a query, and optionally a PDF file. The service thus follows a Query DSL approach (like for instance ElasticSearch) to express queries instead of multiples HTTP parameters. This approach allows queries which are much richer, flexible and simple to express, but also interactive scenarios (where output of the services can be used easily as input after some changes from the user, as for instance in an interactive text editing task).

The JSON query indicates what is the textual content to process, the various (optional) parameters to consider when processing it, optionally some already existing disambiguated entities (already disambiguated by a user or via a particular workflow), and an optional customisation to provide more context to the disambiguation process.

The JSON query is similar to the response of the NERD service, so that a NERD service response can be sent as query after light modifications in an interactive usage scenario, or to be able to process easily already partially annotated text.

When annotations are present in the query, the NERD system will consider them certain and:

* ensure that the user annotations will be present in the output response without inconsistencies with other annotations,

* exploit the user annotations to improve the context for identifying and disambiguating the other possible entities.

Similarly,

* if no language is indicated (usual scenario), the NERD service will use a language identifier to detect the correct language and the language resources to use. However, the query can also optionally specify a language for the text to be processed. This will force the NERD service to process the text with the corresponding particular language resources.

* It is possible also to pass an existing sentence segmentation to the NERD service via the JSON query, in order that the NERD service provides back identified entities following the given sentence segmentation.

The client must respect the JSON format of the NERD response as new query, as described below:


Generic format
^^^^^^^^^^^^^^
The JSON format for the query parameter to be sent to the service is identical to a response of the service:
::
   {
       "text": "The text to be processed.",
       "shortText": "term1 term2 ...",
       "termVector": [
           {
               "term": "term1",
               "score": 0.3
           },
           {
               "term": "term2",
               "score": 0.1
           }
       ],
       "language": {
           "lang": "en"
       },
       "entities": [],
       "resultLanguages": [
           "fr",
           "de"
       ],
       "onlyNER": false,
       "nbest": 0,
       "sentence": false,
       "customisation": "generic",
       "processSentence": []
   }


One and only one of the 4 possible input type - JSON field text, shortText, termVector or a PDF file - must be provided in a query to be valid.
Using multiple input type in the same query is not supported in the version of the API described here.

(1) text
""""""""
Provides a text to be processed (e.g. one or several paragraphs).

(2) shortText
"""""""""""""
Provides a search query to be processed.

(3) termVector
""""""""""""""
Provides a list of terms, each term being associated to a weight indicating the importance of the term as compared to the other terms.

(4) resultLanguages
"""""""""""""""""""
The additional parameter resultLanguages providing a list of language codes, permits to get the wikipedia pages, if they exist, of such additional languages.
Currently only English, German and French wikipedia are supported.

(5) language
""""""""""""
When the source language (parameters language) is pre-set the language is considered certain, and the language identifier is not used.

(6) entities
""""""""""""
In the input example above, the list entities can be used to provide predefined entities (typically pre-annotated by a user).
Having an already annotated entity helps the disambiguation service to resolve entity mentions by offering an important contribution to the global context.
When the entities attribute is not present or empty there are simply no predefined annotations.

For example having a text with the mention “Washington” and manually providing its referring entity (e.g. the city Washington DC) provides an important contribution to the correct disambiguation of the other entity mentions in the text.

Here an example of how the pre-annotated entity can be provided:
::
   {
       "text": "Austria invaded and fought the Serbian army at the Battle of Cer and Battle of Kolubara beginning on 12 August.",
       "language": {
           "lang": "en"
       },
       "entities": [
           {}
       ]
   }

In a typical interactive scenario, an application client first sends a text to be processed via the */disambiguate* service, and receives a JSON response with some entities.

The annotated text is displayed to a user which might correct some invalid annotations.

The client updates the modified annotations in the first JSON response and can send it back to the service now as new query via the */disambiguate*.

The corrected annotations will then be exploited by the (N)ERD system to possibly improve the other annotations and disambiguations.

(7) processSentence
"""""""""""""""""""
The processSentence parameter is introduced to support interactive text editing scenarios. For instance, a user starts writing a text and wants to use the NERD service to annotate dynamically the text with entities as it is typed.

To avoid having the server reprocessing several time the same chunk of text and slowing down a processing time which has to be almost real time, the client can simply indicate a sentence - the one that has just been changed - to be processed.

The goal is to be able to process around two requests per second, even if the typed text is very long, so that the annotations can be locally refreshed smoothly, even considering the fastest keystroke rates that a human can realize.

The processSentence parameter is followed by a list of notations (number and intervals, e.g. *[1, 2-5, 7]* - note that the index starts from 0) corresponding to the sentence index will limit the disambiguation to the selected sentences, while considering the entire text and the previous annotations.

In this example only the second sentence will be processed by NERD:
::
   {
       "text": "The army, led by general Paul von Hindenburg defeated Russia in a series of battles collectively known as the First Battle of Tannenberg. But the failed Russian invasion, causing the fresh German troops to move to the east, allowed the tactical Allied victory at the First Battle of the Marne.",
       "processSentence": [
           1
       ]
   }


When *processSentence* is set, the sentence segmentation is triggered anyway and the value of the attribute *sentence* is ignored:
::
   {
       "text": "The army, led by general Paul von Hindenburg defeated Russia in a series of battles collectively known as the First Battle of Tannenberg. But the failed Russian invasion, causing the fresh German troops to move to the east, allowed the tactical Allied victory at the First Battle of the Marne.",
       "processSentence": [
           1
       ],
       "sentences": [
           {
               "offsetStart": 0,
               "offsetEnd": 163
           },
           {
               "offsetStart": 163,
               "offsetEnd": 319
           }
       ],
       "entities": [
           {
               "rawName": "Russian",
               "type": "NATIONAL",
               "offsetStart": 179,
               "offsetEnd": 186
           }
       ]
   }


PDF input
^^^^^^^^^

This service is processing text contained in the PDF provided in input. In addition to the query it accepts a PDF file via multi-part/form-data.

The JSON format for the query parameter to be sent to the service is identical to a response of the service:
::
   {
      "language": {
         "lang": "en"
      },
      "entities": [],
      "resultLanguages" : ["fr", "de"],
      "onlyNER": false,
      "nbest": 0,
      "sentence": false,
      "customisation": "generic"
   }

Weighted term disambiguation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Process a weighted vector of terms. Each term will be disambiguated - when possible - in the context of the complete vector.

Example request
::
   {
      "termVector":
      [
         {
            "term" : "computer science",
            "score" : 0.3
         },
         {
            "term" : "engine",
            "score" : 0.1
         }
      ],
      "language": {
         "lang": "en"
      },
      "resultLanguages": ["de"],
      "nbest": 0,
      "customisation": "generic"
   }


The termVector field is required for having a well-formed query. resultLanguages can be set to get wikipedia pages for languages in addition to the language of the input terms.


Search query disambiguation
^^^^^^^^^^^^^^^^^^^^^^^^^^^

This functionality provides disambiguation for a search query expressed as a “short text”.

The input is the list of terms that are typically provided in the search bar of a search engine, and response time are optimized to remain very low (1-5ms).

For example the query: "concrete pump sensor". From this association of search terms, it is clear that the sense corresponding to concrete is the material, the entity is the device called “concrete pump”, and it has nothing to do with “concrete” as the antonym of “abstract”.

Processing this kind of input permits to implement semantic search (search based on concept matching) and semantic-based ranking (ranking of documents based on semantic proximity with a query) in a search engine.

Search query disambiguation uses a special model optimized for a small number of non-strictly ordered terms and trained with search queries.

The difference between standard text and short text is similar to the one of the `ERD 2014 challenge <http://web-ngram.research.microsoft.com/erd2014/Docs/Detail%20Rules.pdf>`_.


Example request:
::
   {
      "shortText": "concrete pump sensor",
      “language": {
         "lang": "en"
      },
      "nbest": 0,
      "customisation": "generic"
   }


Response
--------

The response returned by the (N)ERD text processing service is basically the same JSON as the JSON query, enriched by the list of identified and, when possible, disambiguated entities, together with a server runtime information.

If the textual content to be processed is provided in the query as a string, the identified entities will be associated to offset positions in the input string, so that the client can associate precisely the textual mention and the entity “annotation”.

If the textual content to be processed is provided as a PDF document, the identified entities will be associated to coordinates positions in the input PDF, so that the client can associate precisely the textual mention in the PDF via a bounding box and makes possible dynamic PDF annotations.


**Response when processing a text**
::
   {
      "runtime": 223,
      "onlyNER": false,
      "nbest": false,
      "text": "Austria was attaching Serbia.",
      "language": {
         "lang": "en",
         "conf": 0.9999948456042864
      },
      "entities":
      [
         {
            "rawName": "Austria",
            "type": "LOCATION",
            "offsetStart": 0,
            "offsetEnd": 7,
            "nerd_score": "0.5447067973132087",
            "nerd_selection_score": "0.8667510394325003",
            "sense": {
               "fineSense": "country/N1"
            },
            "wikipediaExternalRef": "26964606",
            "domains": [
               "Atomic_Physic",
               "Engineering",
               "Administration",
               "Geology",
               "Oceanography",
               "Earth"
            ]
         },
   [...] }


In the example above, the root layer of JSON values correspond to:

- runtime: the amount of time in milliseconds to process the request on server side,

- onlyNER: as provided in the query - when true the disambiguation against wikipedia is skipped,

- nbest: as provided in the query - when false or 0 returns only the best disambiguated result, otherwise indicates to return up to the specified number of concurrent entities for each disambiguated mention,

- text: input text as provided in the query, all the offset position information are based on the text in this field,

- language: language detected in the text and his confidence score (if the language is provided in the query, conf is equal to 1.0),

- entities: list of named entities recognised in the text (with possibly entities provided in the query, considered then as certain),

- global_categories: provides a weighted list of Wikipedia categories, in order of relevance that are representing the context of the whole text in input.

For each entity the following information are provided:

- rawName: string realizing the entity as it appears in the text

- offsetStart, offsetEnd: the position offset of where the entity starts and ends in the text element in characters (JSON UTF-8 characters)

- nerd_score: disambiguation confidence score, indicates the score of the entity against the other entity candidates for the text mention,

- nerd_selection_score: selection confidence score, indicates how certain the disambiguated entity is actually valid for the text mention,

- wikipediaExternalRef: id of the wikipedia page. This id can be used to retrieve the original page from wikipedia3 or to retrieve all the information associated to the concept in the knowledge base (definition, synonyms, categories, etc. - see the section “Knowledge base concept retrieval”),

- type: NER class of the entity (see table of the 26 NER classes below under “2. Named entity types”),

- sense: NER sense mapped on the Wordnet4 synset - senses are provided to improve the disambiguation process, but they are currently not very reliable.


The type of recognised entities are restricted to a set of 26 classes of named entities (see GROBID NER documentation5). Entities not covered by the knowledge bases (the identified entities unknown by Wikipedia) will be characterized only by an entity class, a word sense estimation and a confidence score, without any reference to a Wikipedia article or domain information.

**Response when processing a search query**
::
   {
      "runtime": 146,
      "onlyNER": false,
      "nbest": false,
      "shortText": "concrete pump sensor",
      "language": {
         "lang": "en",
         "conf": 0.0
      },
      "global_categories":
      [
         {
            "weight": 0.08448995135780164,
            "source": "wikipedia-en",
            "category": "Construction equipment",
            "page_id": 24719865
         },
         [...]
      ],
      "entities":
      [
         {
            "rawName": "concrete",
            "offsetStart": 0,
            "offsetEnd": 8,
            "nerd_score": "0.3416037625644609",
            "nerd_selection_score": "0.9793831523036264",
            "wikipediaExternalRef": "5371",
            "domains": [
               "Mechanics", "Engineering", "Architecture"
            ]
         },
         {
            "rawName": "concrete pump",
            "offsetStart": 0,
            "offsetEnd": 13,
            "nerd_score": "0.695783745626837",
            "nerd_selection_score": "0.9576960838921623",
            "wikipediaExternalRef": "7088907",
            "domains": [
               "Mechanics",
               "Engineering"
            ]
         },
         {
            "rawName": "pump",
            "offsetStart": 9,
            "offsetEnd": 13,
            "nerd_score": "0.33995668143945024",
            "nerd_selection_score": "0.9640450279784305",
            "wikipediaExternalRef": "23617",
            "domains": [
               "Engineering",
               "Mechanics"
            ]
         },
         [...]


**Response when processing a weighted vector of terms**
::
   {
      "runtime": 870,
      "onlyNER": false,
      "nbest": false,
      "termVector": [
         {
            "term": "computer science", "score": 0.3,
            "entities": [
               {
                  "rawName": "computer science",
                  "preferredTerm": "Computer science",
                  "nerd_score": "0.5238665311593967",
                  "nerd_selection_score": "0.0",
                  "wikipediaExternalRef": "5323",
                  "definitions": [
                     {
                        "definition": "'''Computer science''' blablabla.",
                        "source": "wikipedia-en",
                        "lang": "en"
                     }
                  ],
                  "categories": [
                     {
                        "source": "wikipedia-en",
                        "category": "Computer science",
                        "page_id": 691117
                     },
                     [...]
                  ],
            "multilingual": [
               {
               "lang": "de",
               "term": "Informatik",
               "page_id": 2335
            } ]
      } ]
   }

**Response description when processing PDF**
::
   {
      "runtime": 2823,
      "onlyNER": false,
      "nbest": false,
      "file”: "filename.pdf",
      “pages”: 10,
      "language": {
         "lang": "en",
         "conf": 0.9999948456042864
      },
      "pages":
         [
            {
               "page_height":792.0,
               "page_width":612.0
            },
            {
               "page_height":792.0,
               "page_width":612.0
            },
            {
               "page_height":792.0,
               "page_width":612.0
            },
            {
               "page_height":792.0,
               "page_width":612.0
            }
         ],
      "entities": [
         {
            "rawName": "Austria",
            "type": "LOCATION",
            "nerd_score": "0.5447067973132087",
            "nerd_selection_score": "0.8667510394325003",
            "pos": [
               { "p": 1, "x": 20, "y": 20, "h": 10, "w": 30 },
               { "p": 1, "x": 30, "y": 20, "h": 10, "w": 30 } ]
            "sense": {
               "fineSense": "country/N1"
            },
            "wikipediaExternalRef": "26964606",
            "domains": [
               "Atomic_Physic", "Engineering", "Administration", "Geology", "Oceanography", "Earth"
            ] },
      [...] }

As apparent in the above example, for PDF the offset position of the entities are replaced by coordinates information introduced by the JSON attribute pos. These coordinates refer to the PDF that has been processed and permit to identify the chunk of annotated text by the way of a list of bounding boxes.

In addition, an attribute pages is used to indicate the size of each page of the PDF document which is a necessary information to position correctly annotations.

The next section further specifies the coordinates information provided by the service (see `GROBID <http://github.com/kermitt2/grobid>`_).

**PDF Coordinates**

The PDF coordinates system has three main characteristics:

* contrary to usage, the origin of a document is at the upper left corner. The x-axis extends to the right and the y-axis extends downward,
* all locations and sizes are stored in an abstract value called a PDF unit,
* PDF documents do not have a resolution: to convert a PDF unit to a physical value such as pixels, an external value must be provided for the resolution.

In addition, contrary to usage in computer science, the index associated to the first page is 1 (not 0).

The response of the processing of a PDF document by the NERD service contains two specific structures for positioning entity annotations in the PDF:

* the list of page size, introduced by the JSON attribute pages. The dimension of each page is given successively by two attributes page_height and page_height.
* for each entity, a json attribute pos introduces a list of bounding boxes to identify the area of the annotation corresponding to the entity. Several bounding boxes might be necessary because a textual mention does not need to be a rectangle, but the union of rectangles (a union of bounding boxes), for instance when a mention to be annotated is on several lines.

A bounding box is defined by the following attributes:

* p: the number of the page (beware, in the PDF world the first page has index 1!),
* x: the x-axis coordinate of the upper-left point of the bounding box,
* y: the y-axis coordinate of the upper-left point of the bounding box (beware, in the PDF world the y-axis extends downward!),
* h: the height of the bounding box,
* w: the width of the bounding box.

As a PDF document expresses value in abstract PDF unit and do not have resolution, the coordinates have to be converted into the scale of the PDF layout used by the client (usually in pixels).
This is why the dimension of the pages are necessary for the correct scaling, taking into account that, in a PDF document, pages can be of different size.

The (N)ERD console offers a reference implementation with PDF.js for dynamically positioning entity annotations on a processed PDF.