[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Documentation Status](https://readthedocs.org/projects/nerd/badge/?version=latest)](https://readthedocs.org/projects/nerd/?badge=latest)
[![Dependency Status](https://www.versioneye.com/user/projects/5954c15f6725bd005fa19832/badge.svg)](https://www.versioneye.com/user/projects/5954c15f6725bd005fa19832)
<!-- [![Build Status](https://travis-ci.org/kermitt2/nerd.svg?branch=master)](https://travis-ci.org/kermitt2/nerd) -->
<!-- [![Coverage Status](https://coveralls.io/repos/kermitt2/nerd/badge.svg)](https://coveralls.io/r/kermitt2/nerd) -->
<!-- [![Docker Status](https://images.microbadger.com/badges/version/lfoppiano/grobid.svg)](https://hub.docker.com/r/lfoppiano/ grobid/ "Latest Docker HUB image") -->

# entity-fishing

*entity-fishing* performs the following tasks:

* entity recognition and disambiguation against Wikidata and Wikipedia in a raw text or partially-annotated text segment,
![entity-fishing](doc/images/screen2.png)

* entity recognition and disambiguation against Wikidata and Wikipedia at document level, for example a PDF with layout positioning and structure-aware annotations,
![entity-fishing](doc/images/screen7.png)

* search query disambiguation (the _short text_ mode) - below disambiguation of the search query "concrete pump sensor" in the service test console,
![Search query disambiguation](doc/images/screen8.png)

* weighted term vector disambiguation (a term being a phrase),
![Search query disambiguation](doc/images/screen4.png)

* interactive disambiguation in text editing mode.  
![Editor with real time disambiguation](doc/images/screen6.png)

*entity-fishing* is a work-in-progress !

# Documentation

The documentation of *entity-fishing* is available [here](http://nerd.readthedocs.io)

## License and contact

Distributed under [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). The dependencies used in the project are either themselves also distributed under Apache 2.0 license or distributed under a compatible license. 

Main author and contact: Patrice Lopez (<patrice.lopez@science-miner.com>)
