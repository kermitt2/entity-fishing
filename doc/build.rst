.. topic:: Build and install entity-fishing.

Install, build, run, and monitor
================================

Install, build, and run
***********************

*entity-fishing* requires JDK 1.8 or higher. It supports Linux-64. 

Mac OS environments should work fine, but it is *officially* not supported. 
Please use a Linux-64 environment for any production works. Below, we make available the up-to-date and full binary index data for Linux-64 architecture.

Running the service requires at least 3GB of RAM for processing text inputs, but more RAM will be exploited if available for speeding up access to the compiled Wikidata and Wikipedia data (including Wikidata statements associated to entities) and for enabling high rate parallel processing. In case PDF are processed, a mimimum of 8GB is required due to additional PDF parsing and structuring requirements. For parallel processing of PDF exploiting multhreading (e.g. 10 parallel threads), 16GB is recommended. 

After decompressing all the index data, up to 100 GB of disk space will be used if you wish to use all the supported languages (en, fr, de, it, es, ar, zh, ru, ja, pt, fa) - be sure to have enough free space. For running English language only, you will need around 50 GB. SSD is highly recommended for best performance and experience, in particular with a low amount of available RAM (e.g. RAM < 4GB).

First install ``GROBID`` and ``grobid-ner``, see the relative instruction of `GROBID <http://github.com/kermitt2/grobid>`_ and `grobid-ner <http://github.com/kermitt2/grobid-ner>`_.

You need to install latest current stable version ``0.7.1`` of ``GROBID`` and ``grobid-ner``. For GROBID:

Clone GROBID source code from github, latest stable version (currently 0.7.1):
::
   $ git clone https://github.com/kermitt2/grobid.git  --branch 0.7.1

Then build Grobid, in the main directory:
::
  $ cd grobid
  $ ./gradlew clean install


The path to grobid-home shall indicated in the file ``data/config/mention.yaml`` of the entity-fishing project, for instance:
::
   # path to the GROBID home (for grobid-ner, grobid, etc.)
   grobidHome: ../grobid/grobid-home/


For ``grobid-ner`` now, under ``grobid/``, install ``grobid-ner``:
::
  $ git clone https://github.com/kermitt2/grobid-ner.git

Then build ``grobid-ner``, in the sub-project directory:
::
  $ cd grobid-ner
  $ ./gradlew copyModels 
  $ ./gradlew clean install

Install *entity-fishing*:
::
   $ git clone https://github.com/kermitt2/entity-fishing.git

Then install the compiled indexed data:

#. Download the zipped data files corresponding to your environment. The knowledge-base (Wikidata, ``db-kb.zip``) and the English Wikipedia data (``db-en.zip``) must always been installed as minimal set-up. You can then add your languages of choice at the following links. Total is around 29 GB compressed, and around 90 GB uncompressed. The data for this version ``0.0.6`` correspond to the Wikidata and Wikipedia dumps from Jan. 2023. The Knowledge Base part contains around 96 million entities. In this available KB data file, only the statements for entities having at least one Wikipedia page in one of the 9 supported languages are loaded (it's possible to load all of them by regenerating the KB with a dedicated parameter). 

    **Linux**

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-kb.zip (8.7 GB) (minimum requirement)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-en.zip (7.0 GB) (minimum requirement)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-fr.zip (4.3 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-de.zip (2.6 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-es.zip (1.9 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-it.zip (1.7 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-ar.zip (1.3 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-zh.zip (1.3 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-ru.zip (2.4 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-ja.zip (1.8 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-pt.zip (1.2 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-fa.zip (1.1 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-uk.zip (1.3 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-sv.zip (1.4 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-bn.zip (0.3 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.6/db-hi.zip (0.2 GB)

MacOS is officially not supported and should not be used for production. For convenience, we still make available the MacOS data version ``0.0.3`` corresponding to the Wikidata and Wikipedia dumps from mid-2018 (Intel architecture). Although outdated and many languages not available, they are still compatible with the *entity-fishing* version ``0.0.4`` to ``0.0.6`` and could be used for test/development. However, we strongly recommend to use the Linux version for any serious works.

    **MacOS**

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-kb.zip (4.1 GB) (minimum requirement)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-en.zip (5.5 GB) (minimum requirement)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-fr.zip (1.9 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-de.zip (2.0 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-es.zip (1.5 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-it.zip (1.3 GB)


#. Unzip the db archives files under ``data/db/``.

    This will install several sub-directories, one per language, plus wikidata (``db-kb``): ``data/db/db-XY/``, with XY equal to ``fr``, ``en``, ``it``, ``es``, ``en``, ``ar``, ``zh``, ``ru``, ``ja``, ``pt``, ``fa``, ``uk``, ``sv``, ``bn`` and ``hi``. The full uncompressed data is more than 90 GB.

#. Build the project, under the *entity-fishing* project repository.
   ::
      $ ./gradlew clean build

   You should be now ready to run the service.

 
#. Run the service:
   ::
      $ ./gradlew run

The test console is available at port ``:8090`` by opening in your browser: http://localhost:8090

The service port, CORS parameters, and logging parameters can be configured in the file ``data/config/service.yaml``.

For more information, see the next section on the *entity-fishing* Console.

Metrics and monitoring
**********************

As the server is started, the Dropwizard administrative/service console can be accessed at http://localhost:8091/ (default hostname and port)

DropWizard metrics are available at http://localhost:8091/metrics?pretty=true

Prometheus metrics (e.g. for Graphana monitoring) are available at http://localhost:8091/metrics/prometheus

Creating a new Knowledge Base version 
*************************************

The knowledge base used by *entity-fishing* can be updated with new versions of Wikidata and Wikipedia using the pre-processing from the library `GRISP <https://github.com/kermitt2/grisp>`_.

The files generated by GRISP (see `listing all necessary files <https://github.com/kermitt2/grisp?tab=readme-ov-file#final-hierarchy-of-files>`_) should be used via the configuration:

    - ``dataDirectory`` in the files ``wikipedia-XY.yml`` (with XY equal to the language, e.g. ``en``, ``fr``) for the Wikipedia related knowledge base. Note: The ``XYwiki-latest-pages-articles-multistream.xml.bz2`` can be left compressed

    - ``dataDirectory`` in the file ``kb.yml`` for the Wikidata knowledge base (db-kb)
