.. topic:: Build and install entity-fishing.

Install, build and run
======================

*entity-fishing* requires JDK 1.8 or higher. It supports Linux-64 (preferred) and Mac OS environments. Below, we make available the LMDB binary data for these two architectures. taking into account that only index data for Linux are up-to-date. 

Running the service requires at least 3GB of RAM for processing text inputs, but more RAM will be exploited if available for speeding up access to the compiled Wikidata and Wikipedia data (including Wikidata statements associated to entities) and for enabling high rate parallel processing. In case PDF are processed, a mimimum of 8GB is required due to additional PDF parsing and structuring requirements. For parallel processing of PDF exploiting multhreading (e.g. 10 parallel threads), 16GB is recommended. 

After decompressing all the index data, up to 119 GB of disk space will be used if you wish to use all the supported languages (en, fr, de , it, es) - be sure to have enough free space. For running English language only, you will need around 90 GB. SSD is highly recommended for best performance and experience, in particular with a low amount of available RAM (e.g. RAM < 4GB).

First install ``GROBID`` and ``grobid-ner``, see the relative instruction of `GROBID <http://github.com/kermitt2/grobid>`_ and `grobid-ner <http://github.com/kermitt2/grobid-ner>`_.

The path to grobid-home shall indicated in the file ``data/config/mention.yaml``, for instance:
::
   # path to the GROBID home (for grobid-ner, grobid, etc.)
   grobidHome: ../grobid/grobid-home/

Install *entity-fishing*:
::
   $ git clone https://github.com/kermitt2/entity-fishing

Then install the compiled indexed data:

#. Download the zipped data files corresponding to your environment. The knowledge-base (Wikidata, `db-kb.zip`) and the English Wikipedia data (`db-en.zip`) must always been installed as minimal set-up. You can then add your languages of choice at the following links. Total is around 36 GB compressed, and 119 GB uncompressed. The data for this version ``0.0.4`` correspond to the Wikidata and Wikipedia dumps from 20.05.2020. The Knowledge Base part contains around 87 million entities and 1.1 billion statements and has considerably grown in the three last years. 

    **Linux**

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-kb.zip (15 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-en.zip (9.1 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-fr.zip (3.2 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-de.zip (4.2 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-es.zip (2.5 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.4/linux/db-it.zip (2.2 GB)

For MacOS we still make available the data version ``0.0.3`` corresponding to the Wikidata and Wikipedia dumps from mid-2018. Although outdated, they are still compatible with the latest *entity-fishing* version 0.0.4 and could be used for test/development. 

    **MacOS**

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-kb.zip (4.1 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-en.zip (5.5 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-fr.zip (1.9 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-de.zip (2.0 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-es.zip (1.5 GB)

        - https://science-miner.s3.amazonaws.com/entity-fishing/0.0.3/macos/db-it.zip (1.3 GB)


#. Unzip the db archives files under ``data/db/``.

    This will install several sub-directories, one per language, plus wikidata (``db-kb``): ``data/db/db-XY/``, with XY equal to ``fr``, ``en``, ``it``, ``es``, ``en``. The full uncompressed data is about 119 GB.

#. Build the project, under the *entity-fishing* project repository.
   ::
      $ ./gradlew clean build

   You should be now ready to run the service.

 
#. Run the service:
   ::
      $ ./gradlew appRun

The test console is available at port ``:8090`` by opening in your browser: http://localhost:8090

For more information, see the next section on the *entity-fishing* Console.


Creating a new Knowledge Base version from new Wikidata and Wikipedia dumps
***************************************************************************

The knowledge base used by *entity-fishing* can be updated with new versions of Wikidata and Wikipedia using the pre-processing from the library `GRISP <https://github.com/kermitt2/grisp>`_, see `https://github.com/kermitt2/grisp <https://github.com/kermitt2/grisp>`_. 
