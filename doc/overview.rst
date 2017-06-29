.. topic:: Overview of NERD



Overview
========

This document contains the specifications of the (N)ERD - (Named) Entity Recognition and Disambiguation - services. The specifications cover the functionalities realized by the services, the RESTful API and the description of the produced data. 

One of the backbone of the activities of scientists regarding technical and scientific information at large is the identification and resolution of specialist entities. This could be the identification of scientific terms, of nomenclature-based expressions such as chemical formula, of quantity expressions, etc. Researchers in Digital Humanities and in Social Sciences are often first of all interested in the identification and resolution of so-called named entities, e.g. person names, places, events, dates, organisation, etc. Entities can be known in advance and present in generalist or specialized knowledge bases, or can be created based on open nomenclatures and vocabularies, and frequently impossible to enumerate.

The (N)ERD services try to automate this task in a generic manner, avoiding as much as possible restrictions of research domains and limitations to particular usages.

Some key design principles
**************************

The tool (N)ERD is based on the following principles:

**Available as RESTful web services**: by focusing on the role of the component, rather than implementation details, we aim at developing a service as much as possible independent from a particular platform, infrastructure and architecture, easier to integrate, to scale and to monitor.

**Independent from a particular framework and usage scenario**: to maximise the chance of adhesion and long term support from a developer community, we address mainstream usages of entities and do not limit ourselves to a particular application consumer or technological paradigm.

**Full entity processing**: the service is not limited to the recognition of entity mentions but also covers the disambiguation and the resolution of entities against standard knowledge bases (in our case Wikipedia/Wikidata, without restriction to this particular knowledge base).

**State-of-the-art**: we aim at a service providing state-of-the-art performances in term of accuracy, coverage (entity variety), speed and exploitation of contextual information.

**No requirement for user knowledge engineering**: Knowledge workers are not knowledge engineers and we cannot expect a good adhesion to a tool which would require sophisticated preliminary human effort (rules, lexicon, ontologies, etc.). The default usage mode of the service is thus entirely automatic, relying on existing large scale knowledge bases and the ability to identify and normalize entities never seen before.

**Customisable**: The service exploits the most mature generic knowledge bases today available (Wikidata and Wikipedia) and includes a customisation mechanism to a particular domain which does not require domain-specific training data.

**Multilingual support**: even if the coverage of various languages is limited due to the available open linguistic and knowledge resources, the service is designed from the beginning to manage several multilingual resources and it integrates an automatic language identifier.