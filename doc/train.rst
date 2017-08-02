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

Evaluation is produced at the end of training base on a random sample of Wikipedia articles, providing macro- and micro-average precision, recall and f1-score. 

Note that the ratio of disambiguated mentions in a Wikipedia article is low. As a consequence, the precision of our models will be very low because they are built for disambiguating a maximum of entities. Recall is a more meaningful measure when evaluating with Wikipedia.