.. topic:: Train and evaluate

Train and evaluate
==================

Trained models for entity recognition and disambiguation are provided in the project repository. The following section explains how to retrain the models. 

Training with Wikipedia
***********************

Currently a random sample of Wikipedia articles is used for training. The full article content is therefore necessary and a dedicated database will be created the first time the training is launched. This additional database is used and required only for training. You will need the Wikipedia XML dump corresponding to the target languages available in a directory indicated in the `yaml` config files under `data/wikipedia/` by the parameter `dataDirectory` (warning, as this additional database contains the whole textual content of all Wikipedia articles (with wiki markups), it is quite big, around 3.4 GB for the English Wikipedia). 

The following command will build the two models used in *entity-fishing*, the `ranker` and the `selector` model (both being a Random Forest) and preliminary build the full article content database the first time for the English Wikipedia:

```bash
> mvn compile exec:exec -Ptrain_annotate_en
```

For other languages, replace the ending language code (`en`) by the desired one (`fr` or `de` only supported for the moment), e.g.:


```bash
> mvn compile exec:exec -Ptrain_annotate_de
> mvn compile exec:exec -Ptrain_annotate_fr
```

Models will be saved under `data/models`. `ARFF` training data files used to build the model are saved under `data/wikipedia/training/`.

Evaluation with Wikipedia
*************************

An evaluation is produced at the end of training base on a random sample of Wikipedia articles, providing macro- and micro-average precision, recall and f1-score. 

Note that the ratio of disambiguated mentions in a Wikipedia article is low. As a consequence, the precision of our models will be very low because they are built for disambiguating a maximum of entities. Recall is probably a more meaningful measure when evaluating with Wikipedia.

For an evaluation of the NED aspect (ranker in our framework) with well-known datasets, which is much more standard and allows comparison with other similar works, see the evaluation section.

Training with an annotated corpus
******************************

It is possible to train the entity-fishing models with several well-known available datasets. For convenience, the datasets indicated here :doc:`evaluation` are present in the *entity-fishing* distribution.

Use the following maven command with a dataset and a language identifier for running a training with this dataset:
::
	$ mvn compile exec:java -Dexec.mainClass=com.scienceminer.nerd.training.CorpusTrainer -Dexec.args="aquaint en"

For instance for training with the train subset of the AIDA-CONLL, use: 
::
	$ mvn compile exec:java -Dexec.mainClass=com.scienceminer.nerd.training.CorpusTrainer -Dexec.args="aida-train en"



Creating entity embeddings
==========================

Entity embeddings are used to improve entity disambiguation. They are created from word embeddings and entity descriptions generated from Wikidata and Wikipedia. For creating these entity embeddings, the process is as follow: 

1. Download available word embeddings 

