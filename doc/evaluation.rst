.. topic:: Evaluation

Evaluation
==========

Datasets for long texts
***********************

It is possible to evaluate *entity-fishing* entity disambiguation models with several well-known available datasets. For convenience, the following datasets are present in the *entity-fishing* distribution:

- ```ace```: this is a subset of the documents used in the ACE 2004 Coreference documents with 36 articles and 256 mentions, annotated through crowdsourcing, see [1].

- ```aida```: AIDA-CONLL is a manually annotated dataset based on the CoNLL 2003 dataset, with 13881 Reuters news articlesand 27817 mentions, see [2]. Note that the raw texts of this dataset are not included in *entity-fishing*, they have to be obtained from NIST (free for research purpose). AIDA-CONLL dataset can be considered as the most significant gold data for entity disambiguation both in term of size, ambiguity rate and annotation quality. In addition to the complete AIDA-CONLL dataset, this corpus is divided into tree subsets that can be used for evaluation separately: 

  - ```aida-train```: corresponds to the training subset of the CoNLL 2003 dataset

  - ```aida-testa```: corresponds to the validation subset of the CoNLL 2003 dataset

  - ```aida-testb```: corresponds to the test subset of the CoNLL 2003 dataset

- ```aquaint```: this dataset has been created by Milne and Witten [3], with 50 documents and 727 mentions from a news corpus from the Xinhua News Service, the New York Times, and the Associated Press.

- ```iitb```: manually created dataset by [4] with 50 documents collected from online news sources.

- ```msnbc```: this dataset is based on 20 news articles from 10 different topics (two articles per topic) and contains a total of 656 mentions, see [5].

- ```clueweb```: WNED-Clueweb 12 dataset is a large dataset created by [6] from the Clueweb corpura automatically - it is this far less reliable than the previous ones.

- ```wikipedia```: similarly as the Clueweb dataset, this set has been created automatically by [6] from Wikipedia, thus also clearly less reliable.

- ```hirmeos```: manually created dataset using open accessible books (licence CC-BY), financed from the European project H2020 Hirmeos [7].

All these reference datasets are located under `data/corpus/corpus-long`.

Evaluation commands
*******************

Use the following maven command with the above dataset identifier for running an evaluation:
::
    $ ./gradlew evaluation -Pcorpus=[dataset]

For instance for evaluating against the testb subset of the AIDA-CONLL, use: 
::
	$ ./gradlew evaluation -Pcorpus=aida-testb

The evaluation process will provide standard metrics (accuracy, precision, recall. f1) for micro- and macro-averages for the entity disambiguation algorithm selected as ranker and for priors (as baseline). 

The recall of the candidate selection with respect to the gold annotations is also provided (e.g. the proportion of candidate sets containing the expected answer before the ranking).


Generation of pre-annotated training/evaluation data
****************************************************

In case a new corpus needs to be created, *entity-fishing* includes the possibility to automatically generate an XML file of entity annotations from text or pdf files in the same format as the other existing corpus. These generated files can then be corrected manually and used as gold training or evaluation data, or they can be used for semi-supervised training. 

For a given new corpus to be created, for instance the corpus *toto*, the following directory must be created: ``data/corpus/corpus-long/toto/``
The documents part of this corpus must be placed under the subdirectories ``RawText`` and/or ``pdf``.

If there is a directory called ``pdf`` or ``PDF``, the process will extract information (title, abstract, body) from each pdf and save it as ``pdfFileName.lang.txt`` inside the ``RawText`` directory. The tool will then look into the subdirectory ``RawText`` and process the files ``*.txt`` found inside. If the files name is in the form ``filename.lang.txt`` then the lang will be used as reference, otherwise ``en`` will be the default choice.

Use the following maven command with the above dataset identifier for generating the annotation xml file:
::
	$ ./gradlew annotatedDataGeneration -Pcorpus=[corpusname]

For instance, for a new corpus *toto*, with text or pdf documents prepared as indicated above:
::
    $ ./gradlew annotatedDataGeneration -Pcorpus=toto


References
**********

**[1]** Lev-Arie Ratinov, Dan Roth, Doug Downey, and Mike Anderson. Local and global algorithms for disambiguation to wikipedia. In Dekang Lin, Yuji Matsumoto, and Rada Mihalcea, editors, The 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies, Proceedings of the Conference, 19-24 June, 2011, Portland, Oregon, USA, pages 1375–1384. ACL. <http://www.aclweb.org/anthology/P11-1138>. 

**[2]** Johannes Hoffart, Mohamed Amir Yosef, Ilaria Bordino, Hagen Fürstenau, Manfred Pinkal, Marc Spaniol, Bilyana Taneva, Stefan Thater, and Gerhard Weikum. Robust disambiguation of named entities in text. In Proceedings of the 2011 Conference on Empirical Methods in Natural Language Processing, EMNLP 2011, 27-31 July 2011, John McIntyre Conference Centre, Edinburgh, UK, A meeting of SIGDAT, a Special Interest Group of the ACL, pages 782–792. ACL. <http://www.aclweb.org/anthology/D11-1072>.

**[3]** David N. Milne and Ian H. Witten. Learning to link with wikipedia. In James G. Shanahan, Sihem Amer-Yahia, Ioana Manolescu, Yi Zhang, David A. Evans, Aleksander Kolcz, Key-Sun Choi, and Abdur Chowdhury, editors, Proceedings of the 17th ACM Conference on Information and Knowledge Management, CIKM 2008, Napa Valley, alifornia, USA, October 26-30, 2008, pages 509–518. ACM. DOI <https://doi.org/10.1145/1458082.1458150>.

**[4]** Sayali Kulkarni, Amit Singh, Ganesh Ramakrishnan, and Soumen Chakrabarti. Collective annotation of Wikipedia entities in web text. In Proceedings of the 15th ACM SIGKDD international conference on Knowledge discovery and data mining (KDD '09), Paris, France, 2009, pages 457-466. ACM. DOI: <https://doi.org/10.1145/1557019.1557073>

**[5]** Silviu Cucerzan. Large-scale named entity disambiguation based on Wikipedia data. In Jason Eisner, editor, EMNLP-CoNLL 2007, Proceedings of the 2007 Joint Conference on Empirical Methods in Natural Language Processing and Computational Natural Language Learning, June 28-30, 2007, Prague, Czech Republic, pages 708–716. ACL. <http://www.aclweb.org/anthology/D07-1074>.

**[6]** Zhe Cao, Tao Qin, Tie-Yan Liu, Ming-Feng Tsai, and Hang Li. Learning to rank: from pairwise approach to listwise approach. In Zoubin Ghahramani, editor, Machine Learning, Proceedings of the Twenty-Fourth International Conference (ICML 2007), Corvallis, Oregon, USA, June 20-24, 2007, volume 227 of ACM International Conference Proceeding Series, pages 129–136. ACM. DOI <https://doi.org/10.1145/1273496.1273513>.

**[7]** HIRMEOS H2020 project. More information `here <http://www.hirmeos.eu>`_.
