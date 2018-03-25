[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Documentation Status](https://readthedocs.org/projects/nerd/badge/?version=latest)](https://readthedocs.org/projects/nerd/?badge=latest)
[![Dependency Status](https://www.versioneye.com/user/projects/5954c15f6725bd005fa19832/badge.svg)](https://www.versioneye.com/user/projects/5954c15f6725bd005fa19832)
<!-- [![Build Status](https://travis-ci.org/kermitt2/nerd.svg?branch=master)](https://travis-ci.org/kermitt2/nerd) -->
<!-- [![Coverage Status](https://coveralls.io/repos/kermitt2/nerd/badge.svg)](https://coveralls.io/r/kermitt2/nerd) -->
<!-- [![Docker Status](https://images.microbadger.com/badges/version/lfoppiano/grobid.svg)](https://hub.docker.com/r/lfoppiano/ grobid/ "Latest Docker HUB image") -->

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

This version supports English, French, German, Italian and Spanish. The Knowledge Base includes 37 million entities from Wikipedia. 

**Runtime**: on local machine (Intel Haswel i7-4790K CPU 4.00GHz - 8 cores - 16TB - SSD):

* 800 pubmed abstracts (172787 tokens) processed in 126s with 1 client (1371 tokens/s) 

* 4800 pubmed abstracts (1036722 tokens) processed in 216s with 6 concurrent clients (4800 tokens/s) 

* 136 PDF (3443 pages, 1422943 tokens) processed in 1284s with 1 client (2.6 pages/s, 1108.2 tokens/s)

* 816 PDF (20658 pages, 8537658 tokens) processed in 2094s with 6 concurrent clients (9.86 pages/s, 4077 tokens/s)

**Accuracy**: f-score between 76.5 and 89.1 on standard datasets (ACE2004, AIDA-CONLL-testb, AQUAINT, MSNBC).


# Documentation

The documentation of *entity-fishing* is available [here](http://nerd.readthedocs.io).

For upgrade from the previous version please check the [upgrade guide](http://nerd.readthedocs.io/en/latest/upgradeGuide.html).

## License and contact

Distributed under [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). 
The dependencies used in the project are either themselves also distributed under Apache 2.0 license or distributed under a compatible license. 

Main author and contact: Patrice Lopez (<patrice.lopez@science-miner.com>)

*entity-fishing* is developed by [SCIENCE-MINER](http://science-miner.com/entity-disambiguation/) with contributions of [Inria](http://inria.fr) Paris. Inria contribution is supported by the H2020 [HIRMEOS](http://www.hirmeos.eu) EU project, where *entity-fishing* has been [integrated by several partners](http://www.hirmeos.eu/2018/02/15/hirmeos-enhances-its-digital-platforms-with-entity-fishing-validation-of-the-nerd-services/). 
