.. topic:: Guide on how to upgrade between nerd versions

Upgrade Guide
=============
This page explains differences between versions and how to adapt in order to support the new version. 


From 0.0.2 to 0.0.3
*******************

0. the data under `data/db` must be updated with the embedding LMDB. Unzip the following files in `data/db`:  

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


1. the parameter `OnlyNER` has been deprecated and is limited to text processing only (not PDF) for English and French.
This option will be removed in the next release.


2. the mention recognition (prior disambiguation) has been redesigned to accommodate different type of recognitions. Shipped with NERD there is now grobid-ner for Named Entity Recognition and Wikipedia.
They can be selected by using the parameter `mentions` and specifying a list of `recognitors`.::

{
  "text": "Sample text",
  "mentions": [
    "ner",
      "wikipedia"
  ]
}


3. the option `resultLanguages` has been removed, the translated results will be provided in all languages anyway
when fetching the concept information from the knowledge base.


4. The NER system for Named Entity Recognition has been updated, however with some slighly modifications. Improvement in precision and recall are expected globally.
Named Entities of type `PERIOD` are not resolved anymore in wikipedia, they are also matched using a Longest Entity Match, this means that a string `.. the forces fought in Paris from 12 April to 23 August` will match the entire string `from 12 April to 23 August` as `PERIOD`.

