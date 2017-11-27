`.. topic:: Build and install NERD.

Install, build and run
======================

*entity-fishing* requires JDK 1.8 and maven 3. It supports Linux-64 and Mac OS environments. Below, we make available the LMDB binary data for these two architectures. 

Running the service requires at least 2GB of RAM, but more RAM will be exploited if available for speeding up access to the compiled Wikidata and Wikipedia data (including Wikidata statements associated to entities).
After decompressing all the index data, 40 GB of disk space will be used - be sure to have enough free space. SSD is recommended for best performance and experience.

First install ``GROBID`` and ``grobid-ner``, see the relative instruction of `GROBID <http://github.com/kermitt2/grobid>`_ and `grobid-ner <http://github.com/kermitt2/grobid-ner>`_.

The path to grobid-home shall indicated in the file ``data/config/mention.yaml``, for instance:
::
   # path to the GROBID home (for grobid-ner, grobid, etc.)
   grobidHome: ../grobid/grobid-home/

Install *entity-fishing*:
::
   $ git clone https://github.com/kermitt2/nerd


Then install the compiled indexed data:

#. Download the zipped data files corresponding to your environment (warning: total around 13 GB) at the following address:

    **Linux**

        - https://grobid.s3.amazonaws.com/entity-fishing/0.0.3/db-kb.zip (4.1 GB)

        - https://grobid.s3.amazonaws.com/entity-fishing/0.0.3/db-en.zip (5.4 GB)

        - https://grobid.s3.amazonaws.com/entity-fishing/0.0.3/db-fr.zip (1.8 GB)

        - https://grobid.s3.amazonaws.com/entity-fishing/0.0.3/db-de.zip (2.0 GB)

        - https://grobid.s3.amazonaws.com/entity-fishing/0.0.3/embeddings.zip (1.6 GB)

    **Max OSX**

        


#. Unzip the 4 db archives files under ``data/db/``.

    This will install four sub-directories ``data/db/db-kb/``, ``data/db/db-en/``, ``data/db/db-de/`` and ``data/db/db-fr/``.
    The uncompressed data is about 40 GB.

#. Unzip the embeddings archives files (``embeddings.zip``) under ``data/embeddings/``.

#. Build the project, under the *entity-fishing* project repository.
   ::
      $ mvn clean install

   Some tests will be executed. If all tests are successful, you should be now ready to run the service.

#. Run the service with Jetty:
   ::
      $ mvn -Dmaven.test.skip=true jetty:run-war

The test console is available at port ``:8090`` by opening in your browser (preferably *Firefox* or *Chrome*, *Internet Explorer* has not been tested): http://localhost:8090

For more information, see the next section on the *entity-fishing* Console.
