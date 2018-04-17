.. topic:: Training data

Annotation Guidelines
=====================
Adding interpretative linguistic information to a corpus (Leech, 2005) in order to have a value-added corpus is a practice called corpus annotation.
As an enrichment of the raw corpus, the annotation activity itself can be done either automatically or manually.

In *entity-fishing*, the annotation is supposed to identify named entities based on the context and then to group this entities into one of 27 set of classes.
These 27 classes refers to classes in `Grobid-Ner <http://grobid-ner.readthedocs.io/en/latest/class-and-senses/>`_.

Basically, the principle of annotation in this system is similar to the principle of annotation in Grobid-Ner as well as other Conditional Random Field (CRF) models which can bootstrap training data.
*Entity-fishing* can [generate training data](train.rst) from any text and Pdf files, labeling tokens with the named entity classes based on the existing model.
Further activity, human annotators correct the generated training data by modifying the labels produced for each token.
This curated training data can then be added to the existing training data in order to get a new improved model.


# Format of XML File
As explained in [Generation of pre-annotated training/evaluation data](evaluation.rst), it is possible for *entity-fishing* to generate an XML file containing entity annotations from text or pdf files for a new corpus in the same format as the other existing ones.
These files can then be corrected manually and can be used as gold training or evaluation data as well as for semi-supervised training data.

The example of the XML file result (``data/corpus/corpus-long/``) can be seen as follow:

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<ACE2004.entityAnnotation>
	<document docName="20001115_AFP_ARB.0093.eng">
		<annotation>
			<mention>Bandar Seri Begawan</mention>
			<wikiName>Bandar Seri Begawan</wikiName>
			<offset>2</offset>
			<length>19</length>
		</annotation>
		<annotation>
			<mention>AFP</mention>
			<wikiName>Agence France-Presse</wikiName>
			<offset>29</offset>
			<length>3</length>
		</annotation>
	</document>
</ACE2004.entityAnnotation>

```

In this example, the file contains the name of the documents (text or pdf files) being processed and the annotation results for each document.
These annotation results represent the entities containing the mentions and, if it exists, the disambiguation results.
In *entity fishing*, the mentions refer to the tokens and their position in the text (the offset and the length), but they have no link to the real entity (WikiName and Wikidata references, in this case); WikiName and Wikidata ID are set to -1 or Nil/null values.
Meanwhile, the real entity refers to Wikipedia and/or Wikidata article which respectively refers to the Wikipedia page ID (e.g. `5843419` for France's page) and the Wikidata concept ID (e.g. `Q142` for the same entity).

When a certain entity is found in Wikipedia, the page title is also stored in the `<wikiName>` field with the reason that it is useful when only the lemma matches the Wikipedia's article.
For example, `anthropologist` is found in Wikipedia as `anthropology`.

# Correction
Basically, there are two main actions to be done for corrections:
1. Find the missing mentions and/or entities
2. Correct the wrong mentions and/or entities

During the correction process of annotation results which is done manually by human annotators, each mention has to be reviewed whether it has the correct meaning regarding the context of the text.
In order to help the annotators, *entity fishing* provides also a service called `Term Look-up` which is designed to provide a list of disambiguation candidates on the basis of terms.
For example, for a term `France`, *entity-fishing* gives 577 ambiguous concepts to choose from.

Let's take an example of how correcting the annotations. Given the following text:

```
À partir des différences de préoccupations politiques et de traitement des métis en Inde britannique et en Indochine française.
```

and the following annotations are generated as result:
```
<document>
    <annotation>
            <mention>Inde</mention>
            <wikiName>Inde</wikiName>
            <wikidataId>Q18384486</wikidataId>
            <wikipediaId>7503528</wikipediaId>
            <offset>84</offset>
            <length>4</length>
    </annotation>
    <annotation>
            <mention>Indochine française</mention>
            <wikiName>Indochine française</wikiName>
            <wikidataId>Q140025</wikidataId>
            <wikipediaId>1821096</wikipediaId>
            <offset>107</offset>
            <length>19</length>
    </annotation>
</document>
```

Here in the example, it can be seen that there are two types of errors:
1. Find the missing mentions and/or entities
The first mention and entity is not correct since it should be `Inde britannique`.
As a consequence, all the fields of the annotation shall be corrected, including the offset and the length.

2. Correct the wrong mentions and/or entities
Meanwhile, the second mention is correct, but the entity which it refers to is wrong, since it corresponds to `Invasion japonaise de l'Indochine` instead of `Indochine française`.
In this case, <wikidataId> <wikiName> and <wikipediaId> need to be corrected as with Wikipedia Id `8846` and Wikidata Id `Q185682`.

The corrected result should be like this:

```
<document>
    <annotation>
            <mention>Inde britannique</mention>
            <wikiName>Inde britannique</wikiName>
            <wikidataId>Q18384486</wikidataId>
            <wikipediaId>7503528</wikipediaId>
            <offset>84</offset>
            <length>16</length>
    </annotation>
    <annotation>
            <mention>Indochine française</mention>
            <wikiName>Indochine française</wikiName>
            <wikidataId>Q185682</wikidataId>
            <wikipediaId>8846</wikipediaId>
            <offset>107</offset>
            <length>19</length>
    </annotation>
</document>
```

Apart from this process, peer review is needed when doing the annotation corrections at least with two-three different annotators in order to reach mutual agreement.
