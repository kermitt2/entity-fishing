.. topic:: Train and evaluate

Train and evaluate
==================

Trained models for entity recognition and disambiguation are provided in the project repository. The following section explains how to retrain the models. 

Training with Wikipedia
***********************

Currently a random sample of Wikipedia articles is used for training. The full article content is therefore necessary and a dedicated database will be created the first time the training is launched. This additional database is used and required only for training. You will need the Wikipedia XML dump corresponding to the target languages available in a directory indicated in the `yaml` config files under `data/wikipedia/` by the parameter `dataDirectory` (warning, as this additional database contains the whole textual content of all Wikipedia articles (with wiki markups), it is quite big, around 3.4 GB for the English Wikipedia). 

The following command will build the two models used in *entity-fishing*, the `ranker` and the `selector` model (Gradient Tree Boosting for the first one, Random Forest for the second one) and preliminary build the full article content database the first time for the English Wikipedia:
::
	$ mvn compile exec:exec -Ptrain_annotate_en


For other languages, replace the ending language code (`en`) by the desired one (`fr`, `de`, `it` and `es` are supported), e.g.:
::
	$ mvn compile exec:exec -Ptrain_annotate_de
	$ mvn compile exec:exec -Ptrain_annotate_fr
	$ mvn compile exec:exec -Ptrain_annotate_es
	$ mvn compile exec:exec -Ptrain_annotate_it


Models will be saved under `data/models`. `ARFF` training data files used to build the model are saved under `data/wikipedia/training/`.

Evaluation with Wikipedia
*************************

An evaluation is produced at the end of training base on a random sample of Wikipedia articles, providing macro- and micro-average precision, recall and f1-score. 

Note that the ratio of disambiguated mentions in a Wikipedia article is low. As a consequence, the precision of our models will be very low because they are built for disambiguating a maximum of entities. Recall is probably a more meaningful measure when evaluating with Wikipedia.

For an evaluation of the NED aspect (ranker in our framework) with well-known datasets, which is much more standard and allows comparison with other similar works, see the evaluation section.

Training with an annotated corpus
*********************************

It is possible to train the entity-fishing models with several well-known available datasets. For convenience, the datasets indicated here :doc:`evaluation` are present in the *entity-fishing* distribution.

Use the following maven command with a dataset and a language identifier for running a training with this dataset:
::
	$ mvn compile exec:java -Dexec.mainClass=com.scienceminer.nerd.training.CorpusTrainer -Dexec.args="aquaint en"

For instance for training with the train subset of the AIDA-CONLL, use: 
::
	$ mvn compile exec:java -Dexec.mainClass=com.scienceminer.nerd.training.CorpusTrainer -Dexec.args="aida-train en"

*entity-fishing* also included the possibility to generate additional pre-annotated corpus, for instance to be further corrected manually. See :doc:`evaluation` for the explanations.

Creating entity embeddings
**************************

Entity embeddings are used to improve entity disambiguation. They are created from word embeddings and entity descriptions generated from Wikidata and Wikipedia. For creating these entity embeddings, the process is as follow: 

1. Download available pretrained word embeddings - this could be for instance word2vec, FastText, or lexvec.
Word embeddings need initially to be in the standard .vec format (a text format). word2vec binary format can be transformed into .vec format for instance with the simple utility convertvec https://github.com/marekrei/convertvec


2. Quantize and compress word embeddings

2.1 Quantize will simplify the vector given an acceptable quantization factor (by default the error rate for quantizing is 0.01, but it could be changed with the argument -error)
::
	$ mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.Quantizer -Dexec.args="-i wiki.en.vec -o wiki.en.quantized -hashheader"

Here the FastText word embeddings wiki.en.vec given as input (-i) will be quantized and saved as wiki.en.quantized. -hashheader indicates that the first line (a header to be ignored) must be skipped.

2.2 Compressing the word embeddings will significantly reduce its size:
::
	$ mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.EfficientWord2VecCompress -Dexec.args="wiki.en.quantized wiki.en.quantized.compressed"


3. Create Wikidata entity description to be used for producing entity embeddings. The command for creating description is the following one:
::
	$ mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.EntityDescription -Dexec.args="entity.en.description en"

The argument indicates the directory where to save the generated description. 


4. Create entity embeddings from the generated description. 

This step might take a lot of time and exploiting multithreading is particularly hepful. The number of threads to be used is given by the argument -thread
::
	$ mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.EntityEmbeddings -Dexec.args="-i entity.en.description -v wiki.en.quantized.compressed -o entity.en.embeddings -n 10"

The following parameters are available:

* -h: displays help
* -in: path to an entity description data file
* -out: path to the result entity embeddings file (not quantized nor compressed, this is to be done afterwards)
* -n: number of threads to be used, default is 1 but it is advice to used as much as possible
* -rho: rho negative sampling parameters, if it's < 0 use even sampling, default is -1 (must be an integer)
* -max: maximum words per entity, if < 0 use all the words, default is -1 (must be an integer)
* -v: the path to the word embedding file in compressed format (e.g. one originally of word2vec, faster, lexvec, etc.)

5. Quantize and compress entity embeddings

Similarly as the steps 2.1 and 2.2 for the entity embeddings, the quantization:
::
	$mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.Quantizer -Dexec.args="-i /mnt/data/wikipedia/embeddings/wiki.en.vec -o /mnt/data/wikipedia/embeddings/wiki.en.quantized -hashheader"

and the compression:
::
	$mvn exec:java -Dexec.mainClass=com.scienceminer.nerd.embeddings.EfficientWord2VecCompress -Dexec.args="/mnt/data/wikipedia/embeddings/wiki.en.q /mnt/data/wikipedia/embeddings/wiki.en.q.compressed"


The entity embeddings are now ready to use by *entity-fishing*.
