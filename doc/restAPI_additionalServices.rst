.. topic:: Description of the REST API additional services

NERD REST API additional service
================================

Knowledge base concept retrieval
********************************

This service returns the knowledge base concept information. In our case case, all the Wikipedia information is provided: Wikipedia categories, definitions, translingual information, etc.
This service is typically used in pair with the main NERD text processing service.

The NERD text processing service returns the identifiers of the resulting entities with some position offset information. Then, if the client wants, for instance, to display an infobox for this entity, it will send a second call to this service and retrieve the full information for this particular entity.
Adding all the associated information for each entity in the response of the NERD text processing service would result in a very large response which would slow a lot the client, such as a web browser for instance. Using such separate queries allows efficient asynchronous calls which will never block a browser and permits to make only one call per entity, even if the same entity has been found in several places in the same text.

The (N)ERD console offers an efficient reference implementation with Javascript and Ajax queries through the combination of the main NERD text processing service and the Knowledge base concept retrieval.


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

==========  =======  =====================  =====================================
 required    name     content-type value      description
==========  =======  =====================  =====================================
 required    id       String                 ID of the concept to be retrieved
==========  =======  =====================  =====================================

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
- rawName: The term name

- preferredTerm: The normalised term name

- nerd_score: NERD score confidence

- nerd_selection_score: NERD selection score confidence

- wikipediaExternalRef: unique identifier of the concept

- definitions: list of wikipedia definitions (usually in wikipedia a concept contains one and only one definition). Each definition is characterized by three properties:

 - definition: The text of the definition

 - source: The knowledge base from which the definition comes from (in this case can be wikipedia-en, wikipedia-de and wikipedia-fr)

 - lang: the language of the definition

- categories: This provides a list of Wikipedia categories7 directly coming from the wikipedia page of the disambiguated Named Entity. Each category is characterised by the following properties:

 - category: The category name

 - source: The knowledge base from which the definition comes from.

 - pageId: the Id of the page describing the category

- domains: For each entry, Wikipedia provides a huge set of categories, that are not always well curated (1 milion categories in the whole wikipedia). Domains are generic classification of concepts, they are mapped from the wikipedia categories.

- multilingual: provides references to multi-languages resources referring to the same entity. E.g. the entity country called Austria is Österreich in German wikipedia and Autriche in French wikipedia. The page_id provided here relates to the language-specific Wikipedia (e.g. in the above example the page_id for the country Autriche in the French Wikipedia is 15).


Term Lookup
***********

This service is used to search terms in the knowledge base.

This service is useful to verify how many ambiguity a certain term can generate.

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

==========  =======  =====================  =====================================
 required    name     content-type value      description
==========  =======  =====================  =====================================
 required    term      String                 The term to be retrieved
==========  =======  =====================  =====================================

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

Identify the language of the text provided indicating the confidence score of the identification.

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
This API allows to manage customisations for the (N)ERD instance which can then be used as a parameter by the (N)ERD services.

Customisation are identified by their name (or, also called profile in the API).

Customisation body
^^^^^^^^^^^^^^^^^^

The JSON profile of a customisation to be sent to the server for creation and extension has the following structure:
::
   {
     "wikipedia": [
       4764461,
       51499,
       1014346
     ],
     "freebase": [
       "/m/0cm2xh",
       "/m/0dl4z",
       "/m/02kxg_",
       "/m/06v9th"
     ],
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


GET /customisation/{profile}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

(1) Parameters

.. table:: Parameters
   :widths: auto

==========  =========  =====================  ================================================
 required    name       content-type value      description
==========  =========  =====================  ================================================
 required    profile     String                 name of the customisation to be retrieved
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
     "freebase": [
       "/m/0cm2xh",
       "/m/0dl4z",
       "/m/02kxg_",
       "/m/06v9th"
     ],
     "texts": [
       "World War I (WWI or WW1 or World War One), also known as the First World War or the Great War, was a global war centred in Europe that began on 28 July 1914 and lasted until 11 November 1918."
     ],
     "description": "Customisation for World War 1 domain"
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

==========  =========  =====================  ================================================
 required    name       content-type value      description
==========  =========  =====================  ================================================
 required    profile     String                 profile of the customisation to be created
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
     "status": "ok",
     "profile": "profileName"
     "customisation": {
     "wikipedia": [
       1,
       222,
       21233
     ],
     "texts": [
       "World War II"
     ],
     "description": "Customisation for World War 2 domain"
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
     "status": "ok",
     "profile": "profileName"
     "customisation": {
     "wikipedia": [
       1,
       222,
       21233
     ],
     "texts": [
       "World War II"
     ],
     "description": "Customisation for World War 2 domain"
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
