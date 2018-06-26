[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Documentation Status](https://readthedocs.org/projects/nerd/badge/?version=latest)](https://readthedocs.org/projects/nerd/?badge=latest)
<!-- [![Build Status](https://travis-ci.org/kermitt2/nerd.svg?branch=master)](https://travis-ci.org/kermitt2/nerd) -->
<!-- [![Coverage Status](https://coveralls.io/repos/kermitt2/nerd/badge.svg)](https://coveralls.io/r/kermitt2/nerd) -->

# Documentation

The documentation of *entity-fishing* is available [here](http://nerd.readthedocs.io).

For upgrade from the previous version please check the [upgrade guide](http://nerd.readthedocs.io/en/latest/upgradeGuide.html).

# entity-fishing

*entity-fishing* performs the following tasks:

* entity recognition and disambiguation against Wikidata and Wikipedia in a raw text or partially-annotated text segment,
![entity-fishing](doc/images/screen1.png)

* entity recognition and disambiguation against Wikidata and Wikipedia at document level, for example a PDF with layout positioning and structure-aware annotations,
![entity-fishing](doc/images/screen3.png)

* search query disambiguation (the _short text_ mode) - bellow disambiguation of the search query "concrete pump sensor" in the service test console,
![Search query disambiguation](doc/images/screen8.png)

* weighted term vector disambiguation (a term being a phrase),
![Search query disambiguation](doc/images/screen5.png)

* interactive disambiguation in text editing mode.  
![Editor with real time disambiguation](doc/images/screen6.png)

# Current version

*entity-fishing* is a **work-in-progress**! Latest release version is `0.0.3`. 

This version supports English, French, German, Italian and Spanish, with an in-house Named Entity Recognizer for English and French. The knowledge base includes 37 million entities from Wikidata. 

**Runtime**: on local machine (Intel Haswel i7-4790K CPU 4.00GHz - 8 cores - 16TB - SSD)

* 800 pubmed abstracts (172 787 tokens) processed in 126s with 1 client (1371 tokens/s) 

* 4800 pubmed abstracts (1 036 722 tokens) processed in 216s with 6 concurrent clients (4800 tokens/s) 

* 136 PDF (3443 pages, 1 422 943 tokens) processed in 1284s with 1 client (2.6 pages/s, 1108.2 tokens/s)

* 816 PDF (20658 pages, 8 537 658 tokens) processed in 2094s with 6 concurrent clients (9.86 pages/s, 4077 tokens/s)

**Accuracy**: f-score for disambiguation only between 76.5 and 89.1 on standard datasets (ACE2004, AIDA-CONLL-testb, AQUAINT, MSNBC) - to be improved in the next versions.

The knowledge base contains more than 1 billion objects, not far from 15 millions word and entity embeddings, however *entity-fishing* will work with 3-4 GB RAM memory after a 15 second start-up for the server (but please use SSD!). 

Have a look at our [presentation at WikiDataCon 2017](https://grobid.s3.amazonaws.com/presentations/29-10-2017.pdf) for some design and implementation descriptions.


## License and contact

Distributed under [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). The dependencies used in the project are either themselves also distributed under Apache 2.0 license or distributed under a compatible license. 

Main author and contact: Patrice Lopez (<patrice.lopez@science-miner.com>)

*entity-fishing* is developed by [SCIENCE-MINER](http://science-miner.com/entity-disambiguation/) with contributions of [Inria](http://inria.fr) Paris. 

Inria contributors are supported by the H2020 [HIRMEOS](http://www.hirmeos.eu), [IPERION-CH](http://www.iperionch.eu) and [DESIR](https://www.dariah.eu/activities/projects-and-affiliations/desir/) EU projects. 

<a href="https://www.dariah.eu/activities/projects-and-affiliations/desir/" target="_blank"><img align="right" width="75" height="50" src="doc/images/dariah.png"/></a><a href="http://www.iperionch.eu" target="_blank"><img align="right" width="160" height="40" src="doc/images/iperion.png"/></a><a href="http://www.hirmeos.eu" target="_blank"><img align="right" width="120" height="40" src="doc/images/hirmeos.png"/></a>

