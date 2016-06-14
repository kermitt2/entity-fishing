## Install and build 

You will need a total of approx. 15GB of free disk space for installing (N)ERD. Running the service requires at least 4GB of RAM. 

First install _grobid_ and _grobid-ner_, see http://github.com/kermitt2/grobid and http://github.com/kermitt2/grobid-ner

Indicate the path to grobid-home in the file ```src/main/resource/nerd.properties```, for instance: 

```
com.scienceminer.nerd.grobid_home=../grobid/grobid-home/
com.scienceminer.nerd.grobid_properties=../grobid/grobid-home/config/grobid.properties
```

(N)ERD only need the ```grobid-home``` subdirectory for running. 

Then install the wikipedia index:

* download the zipped index files (warning: around 6GB!) at the following address: 

* unzip the archive file under ```data/wikipedia/```. This will install three directories ```data/wikipedia/db-en/```, ```data/wikipedia/db-de/``` and ```data/wikipedia/db-fr/```. 

Then build the project, under the NERD projet repository:

```bash
> mvn clean install    
```

Some tests will be executed. Congratulation, you're now ready to run the service. 

# run the service 

```bash
> mvn -Dmaven.test.skip=true jetty:run-war
```

By default the demo/console is available at http://localhost:8090

The documentation of the service is available in the following document ```doc/nerd-service-manual.pdf```.
