.. topic:: Overview of *entity-fishing*


Overview
========

Motivation
**********

One of the backbone of the activities of scientists regarding technical and scientific information at large is the identification and resolution of specialist entities. This could be the identification of scientific terms, of nomenclature-based expressions such as chemical formula, of quantity expressions, etc. It is considered that between 30 to 80% of the content of a technical or scientific document is written in specialist language `(Ahmad, 1996) <http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.50.7956&rep=rep1&type=pdf>`_. It is therefore important to recognize entities beyond so-called Named Entities, e.g. person names, places, events, dates, organisation, etc. 

The *entity-fishing* services try to automate this recognition and disambiguisation task in a generic manner, avoiding as much as possible restrictions of domains, limitations to certain classes of entities or to specific usages. In particular, *entity-fishing* disambiguates entities against Wikidata. Wikidata offers an openly-accessible community-maintained central hub to represent knowledge about entities. This knowledge base covers various domains and has no limitation to particular entity types. One of the strengths of Wikidata is the anchoring of entities into different human languages Wikipedia instances, offering a rich source of linguistic usages for entities. This coupling of texts and semantic descriptions can be used to train disambiguisation models in a variety of languages and to take advantage of cross-lingual information. 

Tasks
*****

*entity-fishing* performs the following tasks:

* general entity recognition and disambiguation against Wikidata in a raw text, partially-annotated text segment,
.. image:: images/screen1.png
   :alt: text query processing

* general entity recognition and disambiguation against Wikidata at document level, for example a PDF with layout positioning and structure-aware annotations,
.. image:: images/screen3.png
   :alt: PDF query processing

* search query disambiguation (the *short text* mode) - below disambiguation of the search query "concrete pump sensor" in the service test console,
.. image:: images/screen8.png
   :alt: short text query processing

* weighted term vector disambiguation (a term being a phrase),
.. image:: images/screen5.png
   :alt: Weighted term vector query processing

* interactive disambiguation in text editing mode.  
.. image:: images/screen6.png
   :alt: Editor with real time entity disambiguation


Summary
*******

For an overview of the system, some design, implementation descriptions, and some evaluations, see this `Presentation of entity-fishing at WikiDataCon 2017 <https://grobid.s3.amazonaws.com/presentations/29-10-2017.pdf/>`_.

Supervised machine learning is used for the disambiguation, based on Random Forest and Gradient Tree Boosting exploiting various features. The main disambiguation techniques include graph distance to measure word and entity relatedness and distributional semantic distance based on word and entity embeddings. Training is realized exploiting Wikipedia, which offers for each language a wealth of usage data about entity mentions in context. Results include in particular Wikidata identifiers and, optionally, statements. 

The API uses a disambiguation Query DSL with many customization capacities. It offers for instance the possibility to apply filters based on Wikidata properties and values, allowing to create specialised entity identification and extraction (e.g. extract only taxon entities or only medical entities in a document) relying on million entities and statements present in Wikidata. 

The tool currently supports 11 languages, English, French, German, Spanish, Italian, Arabic, Japanese, Chinese (Mandarin), Russian, Portuguese and Farsi. For English and French, a Name Entity Recognition based on CRF `grobid-ner <https://github.com/kermitt2/grobid-ner>`_ is used in combination with the disambiguation. For each recognized entity in one language, it is possible to complement the result with crosslingual information in the other languages. A *nbest* mode is available. Domain information are produced for a large amount of entities in the technical and scientific fields, together with Wikipedia categories and confidence scores.

The tool is developed in Java and has been designed for fast processing (at least for a NERD system, around 1000-2000 tokens per second on a medium-profile linux server single thread or one PDF page of a scientific articles in less than 1 second), with limited memory (at least for a NERD system, here 3GB of RAM as minimum) and to offer relatively close to state-of-the-art accuracy (more to come!). A search query can be disambiguated in 1-10 milliseconds. *entity-fishing* uses the very fast `SMILE ML <https://haifengl.github.io/smile/>`_ library for machine learning and a `JNI integration of LMDB <https://github.com/deephacks/lmdbjni>`_ as embedded database. 


How to cite
***********

If you want to cite this work, please refer to the present GitHub project, together with the [Software Heritage](https://www.softwareheritage.org/) project-level permanent identifier. For example, with BibTeX:
::
   @misc{entity-fishing,
       title = {entity-fishing},
       howpublished = {\url{https://github.com/kermitt2/entity-fishing}},
       publisher = {GitHub},
       year = {2016--2023},
       archivePrefix = {swh},
       eprint = {1:dir:cb0ba3379413db12b0018b7c3af8d0d2d864139c}
   }

Official writing of *entity-fishing*: all lower case (in any contexts) and a dash between entity and fishing. All other writing variants are fake :D

License and contact
*******************

*entity-fishing* is distributed under `Apache 2.0 license <http://www.apache.org/licenses/LICENSE-2.0>`_.
The dependencies used in the project are either themselves also distributed under Apache 2.0 license or distributed under a compatible license.

The documentation is distributed under `CC-0 <https://creativecommons.org/publicdomain/zero/1.0/>`_ license and the annotated data under `CC-BY <https://creativecommons.org/licenses/by/4.0/>`_ license.

If you contribute to entity-fishing, you agree to share your contribution following these licenses. 

Main author and contact: Patrice Lopez (<patrice.lopez@science-miner.com>)
