.. topic:: Description of the REST API for disambiguation

*entity-fishing* REST API
=========================

As RESTful web services, *entity-fishing* is defined by a certain number of stateless transformations of data made available to "consumers" via its interface.

All these RESTful services are available through Cross-origin resource sharing (CORS), allowing clients, such as web browser and server to interact in a flexible manner with cross-origin request.


*entity-fishing* query processing
*********************************


The *entity-fishing* query processing service takes as input a JSON structured query and returns the JSON query enriched with a list of identified and, when possible, disambiguated entities.

The *entity-fishing* service can be applied on 4 types of input content:
 * **text**, provided as JSON string value, for example one or several paragraphs of natural language,
 * **search query**, provided as JSON string value, corresponding to several search terms used together and which can possibly be disambiguated when associated,
 * **weighted vector of terms**, provided by a structured JSON array, where each term will be disambiguated, when possible, in the context of the complete vector - weighted vector of term is a very common structure used in information retrieval, clustering and classification.
 * **PDF document**, provided as multipart data with the JSON query string.

One and only one input type is mandatory in a query, otherwise an HTTP error 400 is returned (see response status codes below). Combining multiple inputs in a single request is currently not supported.


Supported languages
-------------------

In the current version English, French, German, Spanish and Italian are supported. We plan to extend the support in future releases, as long the volume of the Wikipedia corpus for a new language is sufficient.

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
 required    query    multipart/form-data    Query to be processed in JSON UTF-8
 optional    file     multipart/form-data    PDF file (as multipart)
==========  =======  =====================  =====================================

NOTE: To process the text query only (no PDF), is also possible to send it as normal `application/json` raw data.

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

The *entity-fishing* query processing service always consumes a parameter which is a JSON string representing a query, and optionally a PDF file. The service thus follows a Query DSL approach (like, for instance, ElasticSearch) to express queries instead of multiples HTTP parameters. This approach allows queries which are much richer, flexible and simple to express, but also interactive scenarios (where output of the services can be used easily as input after some changes from the user, as for instance in an interactive text editing task).

The JSON query indicates what is the textual content to process, the various (optional) parameters to consider when processing it, optionally some already existing disambiguated entities (already disambiguated by a user or via a particular workflow), and an optional customisation to provide more context to the disambiguation process.

The JSON query is similar to the response of the *entity-fishing* service, so that a *entity-fishing* service response can be sent as query after light modifications in an interactive usage scenario, or to be able to process easily already partially annotated text.

When annotations are present in the query, the *entity-fishing* system will consider them certain and:

* ensure that the user annotations will be present in the output response without inconsistencies with other annotations,

* exploit the user annotations to improve the context for identifying and disambiguating the other possible entities.

Similarly,

* if no language is indicated (usual scenario), the *entity-fishing* service will use a language identifier to detect the correct language and the language resources to use. However, the query can also optionally specify a language for the text to be processed. This will force the service to process the text with the corresponding particular language resources.

* It is possible also to pass an existing sentence segmentation to the *entity-fishing* service via the JSON query, in order that the service provides back identified entities following the given sentence segmentation.

The client must respect the JSON format of the *entity-fishing* response as new query, as described below:


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
       "mentions": ["ner","wikipedia"],
       "nbest": 0,
       "sentence": false,
       "customisation": "generic",
       "processSentence": [],
       "structure": "grobid"
   }


One and only one of the 4 possible input type - JSON field text, shortText, termVector or a PDF file - must be provided in a query to be valid.
Using multiple input type in the same query is not supported in the version of the API described here.

(1) text
""""""""
Provides a text to be processed (e.g. one or several paragraphs). The text have be greater than 5 character or 406 is returned. 

(2) shortText
"""""""""""""
Provides a search query to be processed.

(3) termVector
""""""""""""""
Provides a list of terms, each term being associated to a weight indicating the importance of the term as compared to the other terms.

(4) language
""""""""""""
When the source language (parameters language) is pre-set the language is considered certain, and the language identifier is not used.

(5) mentions
""""""""""""
Provides the methods to be used to identify mentions to be disambiguated, by default mentions are identified with an NER (the mentions are all Named Entity found in the input text to be processed) and with all the labels of Wikipedia for the appropraite language (all the anchors and titles used to refer to a Wikipedia page).

(6) entities
""""""""""""
In the input example above, the list entities can be used to provide predefined entities (typically pre-annotated by a user).
Having an already annotated entity helps the disambiguation service to resolve entity mentions by offering an important contribution to the global context.
When the entities attribute is not present or empty there are simply no predefined annotations.

For example having a text with the mention “Washington” and manually providing its referring entity (e.g. the city Washington DC) provides an important contribution to the correct disambiguation of the other entity mentions in the text.

Below an example of how the pre-annotated entity can be provided. The algorithm would naturally disambiguate *German Army* with
*German Army (Wehrmacht)* (wikipediaId: 12354993) because the text is contextualised on the First World War.
The users can alter this result, by forcing the term to be the *German Army* of the Second World War (wikipediaId: 11702744).
In the response the entity should be returned with confidence 1.0 (as it has been manually provided).

In order to get the wikipedia information for a term, check the `term lookup documentation <Term Lookup_>`_.

NOTE: At the moment the entity is taken in account only when the *wikipediaExternalRef* is provided:
::
   {
       "text": "Austria invaded and fought the Serbian army at the Battle of Cer and Battle of Kolubara beginning on 12 August.",
       "language": {
           "lang": "en"
       },
       "entities": [
            {
               "rawName": "German Army",
               "offsetStart": 1107,
               "offsetEnd": 1118,
               "wikipediaExternalRef": 11702744,
               "wikidataId": "Q701923"
            }
       ]
   }

In a typical interactive scenario, an application client first sends a text to be processed via the */disambiguate* service, and receives a JSON response with some entities.

The annotated text is displayed to a user which might correct some invalid annotations.

The client updates the modified annotations in the first JSON response and can send it back to the service now as new query via the */disambiguate*.

The corrected annotations will then be exploited by the *entity-fishing* system to possibly improve the other annotations and disambiguations.

(7) processSentence
"""""""""""""""""""
The processSentence parameter is introduced to support interactive text editing scenarios. For instance, a user starts writing a text and wants to use the *entity-fishing* service to annotate dynamically the text with entities as it is typed.

To avoid having the server reprocessing several time the same chunk of text and slowing down a processing time which has to be almost real time, the client can simply indicate a sentence - the one that has just been changed - to be processed.

The goal is to be able to process around two requests per second, even if the typed text is very long, so that the annotations can be locally refreshed smoothly, even considering the fastest keystroke rates that a human can realize.

The processSentence parameter is followed by a list of notations (only numbers in integer, e.g. *[1, 7]* - note that the index starts from 0) corresponding to the sentence index will limit the disambiguation to the selected sentences, while considering the entire text and the previous annotations.

In this example only the second sentence will be processed by *entity-fishing*:
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
               "offsetEnd": 138
           },
           {
               "offsetStart": 138,
               "offsetEnd": 293
           }
       ],
       "entities": [
           {
               "rawName": "Russian",
               "type": "NATIONAL",
               "offsetStart": 153,
               "offsetEnd": 160
           }
       ]
   }


**Example using CURL** (using the query above):
::
   curl 'http://cloud.science-miner.com/nerd/service/disambiguate' -X POST -F "query={ 'text': 'The army, led by general Paul von Hindenburg defeated Russia in a series of battles collectively known as the First Battle of Tannenberg. But the failed Russian invasion, causing the fresh German troops to move to the east, allowed the tactical Allied victory at the First Battle of the Marne.', 'processSentence': [ 1 ], 'sentences': [ { 'offsetStart': 0, 'offsetEnd': 138 }, { 'offsetStart': 138, 'offsetEnd': 293 } ], 'entities': [ { 'rawName': 'Russian', 'type': 'NATIONAL', 'offsetStart': 153, 'offsetEnd': 160 } ] }"


(8) structure
"""""""""""""

The **structure** parameter is only considered when the input is a PDF. For processing scientific and technical documents, in particular scholar papers, the value should be **grobid** which is a state of the art tool for structure the body of a scientific paper - it will avoid labelling bibliographical callout (like *Romary and al.*), running foot and head notes, figure content, it will identify the useful areas (header, paragraphs, captions, etc.), handling multiple columns, hyphen, etc. It will apply custom processing based on the nature of the identified structure. This enables "structure-aware" annotations. If no **structure** value is provided, the value **grobid** will be used. 

If you wish to process the whole document without specific structure analysis - this is advised for non-scientific papers -, use the value **full**.

**Example using CURL** for processing the full content of a PDF, *without* preliminar structure recognition:
::
   curl 'http://cloud.science-miner.com/nerd/service/disambiguate' -X POST -F "query={'language': {'lang':'en'}}, 'entities': [], 'nbest': false, 'sentence': false, 'structure': 'full'}" -F "file=@PATH_FILENAME.pdf"


PDF input
^^^^^^^^^

This service is processing a PDF provided as input after extracting and structuring its raw content. Structuration is currently specialized to scientific and technical articles. Processing a PDF not corresponding to scientific articles is currently not recommended. 

In addition to the query, it accepts a PDF file via ```multi-part/form-data```.

The JSON format for the query parameter to be sent to the service is identical to a response of the service:
::
   {
      "language": {
         "lang": "en"
      },
      "entities": [],
      "nbest": 0,
      "sentence": false,
      "structure": "grobid"
   }

An additional parameter related to the processing of the structure of the PDF is available, called `structure`. For processing scientific and technical documents, in particular scholar papers, the value should be `grobid` which is a state of the art tool for structure the body of a scientific paper - it will avoid labelling bibliographical information, foot and head notes, figure content, will identify the useful areas (header, paragraphs, captions, etc.) handling multiple columns, hyphen, etc. and it will apply custom processnig based on the identified structure. 

If you wish to process the whole document without specific structure analysis (this is advised for non-scientific papers), use the value **full** for the paramter **structure**.

**Example using CURL** (using the query above):
::
   curl 'http://cloud.science-miner.com/nerd/service/disambiguate' -X POST -F "query={'language': {'lang':'en'}}, 'entities': [], 'nbest': false, 'sentence': false, 'structure': 'grobid'}" -F "file=@PATH_FILENAME.pdf"


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
      "nbest": 0
   }


The termVector field is required for having a well-formed query.

**Example using CURL** (using the query above):
::
   curl 'http://cloud.science-miner.com/nerd/service/disambiguate' -X POST -F "query={ 'termVector': [ { 'term' : 'computer science', 'score' : 0.3 }, { 'term' : 'engine', 'score' : 0.1 } ], 'language': { 'lang': 'en' }, 'resultLanguages': ['de'], 'nbest': 0}"


Search query disambiguation
^^^^^^^^^^^^^^^^^^^^^^^^^^^

This functionality provides disambiguation for a search query expressed as a “short text”.

The input is the list of terms that are typically provided in the search bar of a search engine, and response time are optimized to remain very low (1-10ms).

For example, let's consider the search query: "concrete pump sensor". From this association of search terms, it is clear that the sense corresponding to *concrete* is the material, the entity is the device called *concrete pump*, and it has nothing to do with *concrete* as the antonym of *abstract*.

Processing this kind of input permits to implement semantic search (search based on concept matching) and semantic-based ranking (ranking of documents based on semantic proximity with a query, for instance exploiting clasifications, domain information, etc.) in a search engine.

Search query disambiguation uses a special model optimized for a small number of non-strictly ordered terms and trained with search queries.

The difference between standard *text* and *short text* is similar to the one of the `ERD 2014 challenge <http://web-ngram.research.microsoft.com/erd2014/Docs/Detail%20Rules.pdf>`_.


Example request:
::
   {
      "shortText": "concrete pump sensor",
      “language": {
         "lang": "en"
      },
      "nbest": 0
   }

**Example using CURL** (using the query above):
::
   curl 'http://cloud.science-miner.com/nerd/service/disambiguate' -X POST -F "query={'shortText': 'concrete pump sensor','language': { 'lang': 'en'},'nbest': 0}"


Response
--------

The response returned by the *entity-fishing* query processing service is basically the same JSON as the JSON query, enriched by the list of identified and, when possible, disambiguated entities, together with a server runtime information.

If the textual content to be processed is provided in the query as a string, the identified entities will be associated to offset positions in the input string, so that the client can associate precisely the textual mention and the entity “annotation”.

If the textual content to be processed is provided as a PDF document, the identified entities will be associated to  coordinates positions in the input PDF, so that the client can associate precisely the textual mention in the PDF via a bounding box and makes possible dynamic PDF annotations.


**Response when processing a text**
::
   {
      "runtime": 223,
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

- **runtime**: the amount of time in milliseconds to process the request on server side,

- **nbest**: as provided in the query - when false or 0 returns only the best disambiguated result, otherwise indicates to return up to the specified number of concurrent entities for each disambiguated mention,

- **text**: input text as provided in the query, all the offset position information are based on the text in this field,

- **language**: language detected in the text and his confidence score (if the language is provided in the query, conf is equal to 1.0),

- **entities**: list of entities recognised in the text (with possibly entities provided in the query, considered then as certain),

- **global_categories**: provides a weighted list of Wikipedia categories, in order of relevance that are representing the context of the whole text in input.


For each entity the following information are provided:

- **rawName**: string realizing the entity as it appears in the text

- **offsetStart, offsetEnd**: the position offset of where the entity starts and ends in the text element in characters (JSON UTF-8 characters)

- **nerd_score**: disambiguation confidence score, indicates the score of the entity against the other entity candidates for the text mention,

- **nerd_selection_score**: selection confidence score, indicates how certain the disambiguated entity is actually valid for the text mention,

- **wikipediaExternalRef**: id of the wikipedia page. This id can be used to retrieve the original page from wikipedia3 or to retrieve all the information associated to the concept in the knowledge base (definition, synonyms, categories, etc. - see the section “Knowledge base concept retrieval”),

- **type**: NER class of the entity (see table of the 27 NER classes below under “2. Named entity types”),

- **sense**: NER sense mapped on Wordnet synset - senses are provided to improve the disambiguation process, but they are currently not very reliable.

The type of recognised entities are restricted to a set of 27 classes of named entities (see `GROBID NER documentation <http://grobid-ner.readthedocs.io/en/latest/class-and-senses/>`_). Entities not covered by the knowledge bases (the identified entities unknown by Wikipedia) will be characterized only by an entity class, a word sense estimation and a confidence score, without any reference to a Wikipedia article or domain information.

**Response when processing a search query**
::
   {
      "runtime": 146,
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

The response of the processing of a PDF document by the *entity-fishing* service contains two specific structures for positioning entity annotations in the PDF:

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

The *entity-fishing* console offers a reference implementation with PDF.js for dynamically positioning entity annotations on a processed PDF.

Knowledge base concept retrieval
********************************

This service returns the knowledge base concept information. In our case case, language-independent information from Wikidata will be provided (Wikidata identifier, statements), together with language-dependent information (all the Wikipedia information: Wikipedia categories, definitions, translingual information, etc.). This service is typically used in pair with the main *entity-fishing* query processing service in order to retrieve a full description of an identified entity.

The service supports the following identifiers:
 - wikidata identifier (starting with `Q`, e.g. `Q61`)
 - wikipedia identifier

The *entity-fishing* content processing service returns the identifiers of the resulting entities with some position offset information. Then, if the client wants, for instance, to display an infobox for this entity, it will send a second call to this service and retrieve the full information for this particular entity.
Adding all the associated information for each entity in the response of the *entity-fishing* query processing service would result in a very large response which would slow a lot the client, such as a web browser for instance. Using such separate queries allows efficient asynchronous calls which will never block a browser and permits to make only one call per entity, even if the same entity has been found in several places in the same text.

The *entity-fishing* console offers an efficient reference implementation with Javascript and Ajax queries through the combination of the main *entity-fishing* query processing service and the Knowledge base concept retrieval.


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
         500               Indicate an internal service error
   ===================  ========================================================



GET /kb/concept/{id}
^^^^^^^^^^^^^^^^^^^^

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =======  =====================  ===============================================================================================================
 required    name     content-type value      description
==========  =======  =====================  ===============================================================================================================
 required    id       String                 ID of the concept to be retrieved (wikipedia, wikidata id (starting with `Q`) or property (starting with `P`).
 optional    lang     String                 (valid only for wikipedia IDs) The language knowledge base where to fetch the concept from. Default: `en`.
==========  =======  =====================  ===============================================================================================================

(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------+------------------+--------------------------------------+
| required | name   | value            | description                          |
+==========+========+==================+======================================+
| optional | Accept | application/json | Set the response type of the output  |
+----------+--------+------------------+--------------------------------------+


(3) Example response
::
   {
     "rawName": "Austria",
     "preferredTerm": "Austria",
     "nerd_score": "0.0",
     "nerd_selection_score": "0.0",
     "wikipediaExternalRef": "26964606",
     "wikidataId": "Q1234"
     "definitions": [
       {
         "definition": "'''Austria''', officially the '''Republic of Austria'''",
         "source": "wikipedia-en",
         "lang": "en"
       }
     ],
     "categories": [
       {
         "source": "wikipedia-en",
         "category": "Austria",
         "page_id": 707451
       },
       {
         "lang": "de",
         "source": "wikipedia-en",
         "category": "Erasmus Prize winners",
         "page_id": 1665997
       }
     ],
     "multilingual": [
       {
         "lang": "de",
         "term": "Österreich",
         "page_id": 1188788
       },
       {
         "lang": "fr",
         "term": "Autriche",
         "page_id": 15
       }
     ]
   }

The elements present in this response are:

- **rawName**: The term name

- **preferredTerm**: The normalised term name

- **nerd_score**: always 0.0 because no disambiguation took place in a KB access

- **nerd_selection_score**: always 0.0 because no disambiguation took place in a KB access

- **wikipediaExternalRef**: unique identifier of the concept in wikipedia

- **wikidataId**: unique identifier of the concept in wikidata

- **definitions**: list of wikipedia definitions (usually in wikipedia a concept contains one and only one definition). Each definition is characterized by three properties:

 - **definition**: The text of the definition

 - **source**: The knowledge base from which the definition comes from (in this case can be wikipedia-en, wikipedia-de and wikipedia-fr)

 - **lang**: the language of the definition

- **categories**: This provides a list of Wikipedia categories7 directly coming from the wikipedia page of the disambiguated entity. Each category is characterised by the following properties:

 - **category**: The category name

 - **source**: The knowledge base from which the definition comes from.

 - **pageId**: the Id of the page describing the category

- **domains**: For each entry, Wikipedia provides a huge set of categories, that are not always well curated (1 milion categories in the whole wikipedia). Domains are generic classification of concepts, they are mapped from the wikipedia categories.

- **multilingual**: provides references to multi-languages resources referring to the same entity. E.g. the entity country called Austria is Österreich in German wikipedia and Autriche in French wikipedia. The page_id provided here relates to the language-specific Wikipedia (e.g. in the above example the page_id for the country Autriche in the French Wikipedia is 15).


Term Lookup
***********

This service is used to search terms in the knowledge base. This service is useful to verify how many ambiguity a certain term can generate.

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
         500               Indicate an internal service error
   ===================  ========================================================

GET /kb/term/{term}
^^^^^^^^^^^^^^^^^^^

(1) Parameters

.. table:: Parameters
  :widths: auto

==========  =======  =====================  =============================================================================
 required    name     content-type value      description
==========  =======  =====================  =============================================================================
 required    term      String                 The term to be retrieved
 optional    lang      String                 The language knowledge base where to fetch the term from. Default: `en`.
==========  =======  =====================  =============================================================================

(2) Request header

.. table:: Request headers
  :widths: auto

+----------+--------+------------------+--------------------------------------+
| required | name   | value            | description                          |
+==========+========+==================+======================================+
| optional | Accept | application/json | Set the response type of the output  |
+----------+--------+------------------+--------------------------------------+



Language identification
***********************

Identify the language of a provided text, associated to a confidence score.

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
         500               Indicate an internal service error
   ===================  ========================================================


POST /language
^^^^^^^^^^^^^^

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =======  =====================  ================================================
 required    name     content-type value      description
==========  =======  =====================  ================================================
 required    text     String                 The text whose language needs to be identified
==========  =======  =====================  ================================================

(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
| optional | Content-Type | multipart/form-data | Define the format of the posted property  |
+----------+--------------+---------------------+-------------------------------------------+


(3) Example response (ISO 639-1)

Here a sample of the response
::
  {
     "lang":"en",
     "conf": 0.9
  }


GET /language?text={text}
^^^^^^^^^^^^^^^^^^^^^^^^^

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =======  =====================  ================================================
 required    name     content-type value      description
==========  =======  =====================  ================================================
 required    text     String                 The text whose language needs to be identified
==========  =======  =====================  ================================================

(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
+----------+--------------+---------------------+-------------------------------------------+


(3) Example response (ISO 639-1)

Here a sample of the response
::
  {
     "lang":"en",
     "conf": 0.9
  }

Sentence segmentation
*********************

This service segments a text into sentences. It is useful in particular for the interactive mode for indicating that only certain sentences need to be processed for a given query.

Beginning and end of each sentence are indicated with offset positions with respect to the input text.

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
         500               Indicate an internal service error
   ===================  ========================================================

POST /segmentation
^^^^^^^^^^^^^^^^^^

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =======  =====================  ================================================
 required    name     content-type value      description
==========  =======  =====================  ================================================
 required    text     String                 The text to be segmented into sentences
==========  =======  =====================  ================================================

(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
| optional | Content-Type | multipart/form-data | Define the format of the posted property  |
+----------+--------------+---------------------+-------------------------------------------+


(3) Example response

Here a sample of the response
::
  {
    "sentences": [
      {
        "offsetStart": 0,
        "offsetEnd": 7
      },
      {
        "offsetStart": 6,
        "offsetEnd": 21
      }
    ]
  }


GET /segmentation?text={text}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =======  =====================  ================================================
 required    name     content-type value      description
==========  =======  =====================  ================================================
 required    text     String                 The text whose language needs to be identified
==========  =======  =====================  ================================================

(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
+----------+--------------+---------------------+-------------------------------------------+


(3) Example response

Here a sample of the response:
::
   {
     "sentences": [
       {
         "offsetStart": 0,
         "offsetEnd": 7
       },
       {
         "offsetStart": 6,
         "offsetEnd": 21
       }
     ]
   }


Customisation API
*****************

The customisation is a way to specialize the entity recognition, disambiguation and resolution for a particular domain.
This API allows to manage customisations for the *entity-fishing* instance which can then be used as a parameter by the *entity-fishing* services.

Customisation are identified by their name (or, also called profile in the API).


Customisation body
------------------
The JSON profile of a customisation to be sent to the server for creation and extension has the following structure:
::
   {
     "wikipedia": [
       4764461,
       51499,
       1014346
     ],
     "language": {"lang":"en"},
     "texts": [
       "World War I (WWI or WW1 or World War One), also known as Germany and Austria-Hungary."
     ],
     "description": "Customisation for World War 1 domain"
   }


The context will be build based on Wikipedia articles and raw texts, which are all optional. Wikipedia articles are expressed as an array of Wikipedia page IDs.

Texts are represented as an array of raw text segments.

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
         500               Indicate an internal service error
   ===================  ========================================================


GET /customisations
^^^^^^^^^^^^^^^^^^^

Returns the list of existing customisations as a JSON array of customisation names.


(1) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
+----------+--------------+---------------------+-------------------------------------------+


(2) Example response

Here a sample of the response: 
::
   [
      "ww1",
      “ww2”,
      “biology”
   ]


GET /customisation/{name}
^^^^^^^^^^^^^^^^^^^^^^^^^

Retrieve the content of a specific customisation

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =========  =====================  ================================================
 required    name       content-type value      description
==========  =========  =====================  ================================================
 required    name       String                 name of the customisation to be retrieved
==========  =========  =====================  ================================================


(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
+----------+--------------+---------------------+-------------------------------------------+



(3) Example response

Here a sample of the response
::
   {
     "wikipedia": [
       4764461,
       51499,
       1014346
     ],
     "language": {
         "lang": "en"
     },
     "texts": [
       "World War I (WWI or WW1 or World War One), also known as the First World War or the Great War, was a global war centred in Europe that began on 28 July 1914 and lasted until 11 November 1918."
     ],
     "description": "Customisation for World War 1 domain"
   }

Or in case of issues:
::
   {
     "ok": "false",
     "message": "The customisation already exists."
   }


POST /customisations
^^^^^^^^^^^^^^^^^^^^

Creates a customisation as defined in the input JSON, named following the path parameter.
The JSON profile specifies a context via the combination of a list of Wikipedia article IDs and text fragments.
A text describing informally the customisation can be added optionally.

If the customisation already exists an error is returned.


(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =========  =====================  ==========================================================
 required    name       content-type value      description
==========  =========  =====================  ==========================================================
 required    name       String                 name of the customisation to be created
 required    value      String                 JSON representation of the customisation (see example)
==========  =========  =====================  ==========================================================


(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
+----------+--------------+---------------------+-------------------------------------------+



(3) Example response

Here a sample of the response
::
   {
     "ok": "true"
   }

Or in case of issues:
::
   {
     "ok": "false",
     "message": "The customisation already exists."
   }


PUT /customisation/{profile}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update an existing customisation as defined in the input JSON, named following the path parameter.
The JSON profile specifies a context via the combination of a list of Wikipedia article IDs, FreeBase entity mid and text fragments.

A text describing informally the customisation can be added optionally.

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =========  =====================  ================================================
 required    name       content-type value      description
==========  =========  =====================  ================================================
 required    profile     String                 name of the customisation to be updated
==========  =========  =====================  ================================================


(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
+----------+--------------+---------------------+-------------------------------------------+



(3) Example response

Here a sample of the response
::
   {
     "ok": "true"
   }

Or in case of issues:
::
   {
     "ok": "false",
     "message": "The customisation already exists."
   }

DELETE /customisation/{profile}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =========  =====================  ================================================
 required    name       content-type value      description
==========  =========  =====================  ================================================
 required    profile     String                 name of the customisation to be deleted
==========  =========  =====================  ================================================


(2) Request header

.. table:: Request headers
   :widths: auto

+----------+--------------+---------------------+-------------------------------------------+
| required | name         | value               | description                               |
+==========+==============+=====================+===========================================+
| optional | Accept       | application/json    | Set the response type of the output       |
+----------+--------------+---------------------+-------------------------------------------+

(3) Example response

Here a sample of the response
::
   {
     "ok": "true"
   }

Or in case of issues:
::
   {
     "ok": "false",
     "message": "The customisation already exists."
   }