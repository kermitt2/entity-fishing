.. topic:: Train and evaluate

Train and evaluate
==================

Trained models for entity recognition and disambiguation are provided in the project repository. The following section explains how to retrain the models. 

Training with Wikipedia
***********************

Currently a random sample of Wikipedia articles is used for training. The full article content is therefore necessary and a dedicated database will be created the first time the training is launched. This additional database is used and is required only for training. You will need the Wikipedia XML dump corresponding to the target languages available in a directory indicated in the ``yaml`` config files by the parameter ``dataDirectory``. A warning here, as this additional database contains the whole textual content of all Wikipedia articles (with wiki markups), it is quite big, around 7.6G GB for the English Wikipedia (dump from May 2020). This database (stored under the ``dbDirectory`` indicated in the language config file and called ``markupFull``) will be built automatically if not present, so typically at first launch of the training for a given language, and the process will take a bit more than one hour for building the English version for example. 

The following command will build the two models used in *entity-fishing*, the ``ranker`` and the ``selector`` model (Gradient Tree Boosting for the first one, Random Forest for the second one) and preliminary build the full article content database the first time for the English Wikipedia:
::
	$ ./gradlew train_wikipedia -Plang=en


For other languages, replace the ending language code (``en``) by the desired one (``fr``, ``de``, ``it``, ``es``, ``ar``, ``zh``, ``ru`` and ``ja`` are supported), e.g.:
::
	$ ./gradlew train_annotate -Plang=fr
	$ ./gradlew train_annotate -Plang=de

Models will be saved under ``data/models``. ``ARFF`` training data files used to build the model are saved under ``data/wikipedia/training/``.

Evaluation with Wikipedia
*************************

An evaluation is produced at the end of training base on a random sample of Wikipedia articles, providing macro- and micro-average precision, recall and f1-score. 

Note that the ratio of disambiguated mentions in a Wikipedia article is low. As a consequence, the precision of our models will be very low because they are built for disambiguating a maximum of entities. Recall is probably a more meaningful measure when evaluating with Wikipedia.

For an evaluation of the NED aspect (ranker in our framework) with well-known datasets, which is much more standard and allows comparison with other similar works, see the evaluation section.

Training with an annotated corpus
*********************************

It is possible to train the entity-fishing models with several well-known available datasets. For convenience, the datasets indicated here :doc:`evaluation` are present in the *entity-fishing* distribution.

Use the following command with a dataset name and a language identifier for running a training with this dataset:
::
	$ ./gradlew train_corpus -Pcorpus=aquaint -Plang=en

For instance for training with the train subset of the AIDA-CONLL, use: 
::
	$ ./gradlew train_corpus -Pcorpus=aida-train -Plang=en 

*entity-fishing* also included the possibility to generate additional pre-annotated corpus, for instance to be further corrected manually. See :doc:`evaluation` for the explanations.

The evaluation with annotated corpus is also described in the page :doc:`evaluation`.

Creating entity embeddings
**************************

Entity embeddings are used to improve entity disambiguation. They are created from word embeddings and entity descriptions generated from Wikidata and Wikipedia. Embeddings resources are provided with the project data resources, so you normally don't have to create yourself these embeddings. For reference, we document here how to create these entity embeddings. The process is as follow: 

1. Download available pretrained word embeddings for a target language - this could be for instance word2vec, FastText, or lexvec. Word embeddings need initially to be in the standard .vec format (a text format). word2vec binary format can be transformed into .vec format with the simple utility `convertvec <https://github.com/marekrei/convertvec>`_

Note: English and Arabic word embeddings used in the current *entity-fishing* are Glove "flavor". Arabic embeddings are available at https://archive.org/details/arabic_corpus, see https://ia803100.us.archive.org/4/items/arabic_corpus/vectors.txt.xz. Other languages are using fastText word embeddings. 

2. Quantize word embeddings

Quantize will simplify the vector given an acceptable quantization factor (by default the error rate for quantizing is 0.01, but it could be changed with the argument ``-Perror``)
::
	$ ./gradlew quantize_word_embeddings -Pi=/media/lopez/data/embeddings/glove-vectors.vec -Po=/media/lopez/data/embeddings/word.embeddings.quantized

Here some Glove word embeddings ``glove-vectors.vec`` given as input (``-i``) will be quantized and saved as ``word.embeddings.quantized``. 
By default, the flag ``-hashheader`` is used and indicates that the first line (a header to be ignored) must be skipped. In case there is no header, ``-hashheader`` should be removed in the corresponding gradle task ``quantize_word_embeddings`` (see file ``build.gradle``). 

3. Create Wikidata entity description to be used for producing entity embeddings. The command for creating description is the following one:
::
	$./gradlew generate_entity_description -Plang=en

Replace the ``en`` argument by the language of interest. 

The generated description are saved under ``data/embeddings/en/``), given the language of interest (here ``en``).  

4. Create entity embeddings from the generated description. 

This step might take a lot of time and exploiting multithreading is particularly hepful. The number of threads to be used is given by the argument ``-n``:
::
	$ ./gradlew generate_entity_embeddings -Pin=entity.description -Pv=word.embeddings.quantized -Pout=entity.embeddings.vec -Pn=10

The following parameters are available:

* **-h**: displays help
* **-in**: path to an entity description data file
* **-v**: the path to the word embedding file in .vec format (e.g. one originally of word2vec, faster, lexvec, etc.), optionally quantized
* **-out**: path to the result entity embeddings file (not quantized, this is to be done afterwards)
* **-n**: number of threads to be used, default is 1 but it is advice to used as much as possible
* **-rho**: rho negative sampling parameters, if it's < 0 use even sampling, default is -1 (must be an integer)
* **-max**: maximum words per entity, if < 0 use all the words, default is -1 (must be an integer)

5. Quantize entity embeddings

Finally, similarly as the steps 2., we apply a quantization to the entity embeddings:
::
	$ ./gradlew quantize_word_embeddings -Pi=/media/lopez/data/embeddings/entity.embeddings.vec -Po=/media/lopez/data/embeddings/entity.embeddings.quantized

The entity embeddings are now ready to be loaded in the embedded database of *entity-fishing*. 

6. Copy the quantized embeddings files (e.g. ``entity.embeddings.quantized``) under the *entity-fishing* data repository (the one containing the csv files). *entity-fishing* expects compressed files with ``.gz`` extension:  ``word.embeddings.quantized.gz`` and ``entity.embeddings.quantized.gz``. Starting *entity-fishing* will load automatically the embeddings in the embedded database LMDB as binary data.
