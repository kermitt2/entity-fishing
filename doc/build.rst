.. topic:: Build and install NERD

Build and install
=================

(N)ERD requires JDK 1.8 and maven 3. It supports Linux-64 and Mac OS environments. Below, we make available the LMDB binary data for Linux-64 architecture. 

Running the service requires at least 2GB of RAM, but more RAM will be exploited if available for speeding up access to the compiled Wikidata and Wikipedia data (including statements associated to entities).
After decompressing all the index data, 19GB of disk space will be used - be sure to have enough free space. SSD is recommended for best performance and experience.

First install ```GROBID``` and ```grobid-ner```, see the relative instruction of `GROBID <http://github.com/kermitt2/grobid>`_ and `grobid-ner <http://github.com/kermitt2/grobid-ner>`_.

The path to grobid-home shall indicated in the file `src/main/resource/nerd.properties`, for instance:
::
	com.scienceminer.nerd.grobid_home=../grobid/grobid-home/
	com.scienceminer.nerd.grobid_properties=../grobid/grobid-home/config/grobid.properties


Then install the Wikipedia index:

- download the zipped index files (warning: total around 10 GB!) at the following address:

    **Linux**


    **Max OSX**

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/lmdb/db-en.osx.zip (5.3 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/lmdb/db-fr.osx.zip (1.8 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/lmdb/db-de.osx.zip (2 GB)

        - https://s3.eu-central-1.amazonaws.com/storagescienceminer/NERD/lmdb/db-kb.osx.zip (1.8 GB)

- unzip the 5 archives files under ```data/wikipedia/```.

    This will install three sub-directories ```data/wikipedia/db-kb/```, ```data/wikipedia/db-en/```, ```data/wikipedia/db-de/``` and ```data/wikipedia/db-fr/```.
    The uncompressed data is about 20 GB.

- Build the project, under the NERD project repository.
    With maven: 
    ::
	    > mvn clean install

    Some tests will be executed.

Congratulation, you should be now ready to run the service.
