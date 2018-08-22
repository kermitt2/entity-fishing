.. topic:: Guide on how to upgrade between nerd versions

Upgrade Guide
=============
This page explains differences between versions and how to adapt in order to support the new version.
If you are not upgrading, you can skip this page.
Otherwise, please read it carefully until the end before starting the upgrade. 


From 0.0.2 to 0.0.3
*******************

#. if your installation is from before January 2018 or if you are not sure, please update the Knowledge Base file.
It has been updated from the version 0.0.2.

#. the language dependant data under ``data/db`` must be updated with the embedding in LMDB format. Unzip the following files in ``data/db``:

    **Linux**

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-en.zip (2.9 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-fr.zip (1.2 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-de.zip (3.8 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-es.zip (0.8 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-it.zip (0.7 GB)

    **Max OSX**

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-en-embeddings.osx.zip (2.9 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-fr-embeddings.osx.zip (1.2 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-de-embeddings.osx.zip (1.9 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-es-embeddings.osx.zip (0.8 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/0.0.3-embeddings/embeddings/db-it-embeddings.osx.zip (0.7 GB)

#. the parameter ``OnlyNER`` has been deprecated and is limited to text processing only (not PDF) for English and French.
This option will be removed in the next release.

#. the mention recognition (step before the disambiguation) has been redesigned to accommodate different engines. Similarly to version 0.0.2, grobid-ner and Wikipedia are the included mention recognizers.
By default they are both used but they can now be explicitely selected by using the parameter ``mentions`` and specifying a list of recognizers. For example: 
::
   {
      "text": "Sample text",
      "mentions": [
         "ner",
         "wikipedia",
         "species"
      ]
   }




#. the option ``resultLanguages`` has been deprecated (it will be removed in the next release) because the translated results will be provided in all languages anyway
when fetching the concept information from the knowledge base.


#. The model for recognising Named Entities for english has been updated resulting in higher precision and recall. There are however with some modifications in some of the recognised entities:
 - Named Entities of type ``PERIOD`` are not disambiguated anymore against Wikidata/Wikipedia. They are also matched using a Longest Entity Match, this means that a string ``.. the forces fought in Paris from 12 April to 23 August`` will match the entire chunck ``from 12 April to 23 August`` as ``PERIOD``.

