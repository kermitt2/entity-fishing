Dataset: ydata-search-query-log-to-entities-v1_0

Yahoo! queries and TREC Session track queries manually linked to Wikipedia entities, version 1.0

==================================================================

Yahoo! Webscope ReadMe

The data included herein is provided as part of the Yahoo! Webscope program for use solely under the terms of a signed Yahoo! Data Sharing Agreement.

Any publication using this data should attribute Yahoo!, ideally in the bibliography of the paper, unless Yahoo! explicitly requests no attribution. Please include the phrase Yahoo! Webscope, the web address http://research.yahoo.com/Academic_Relations and the name of the specific dataset used, including version number if applicable. For example:

Yahoo! Webscope dataset ydata-search-query-log-to-entities-v1_0
[http://labs.yahoo.com/Academic_Relations]

Please send a copy of each paper and its full citation information to research-data-requests@yahoo-inc.com upon publication.

This data may be used only for academic research purposes and may not be used for any commercial purposes, by any commercial entity, or by any party not under a signed Data Sharing Agreement. The data may not be reproduced in whole or in part, may not be posted on the web, on internal networks, or in networked data stores, and may not be archived offsite. The data must be returned to Yahoo! at the end of the research project or in three years, whichever comes first.

This dataset was produced from Yahoo!'s records and has been reviewed by an internal board to assure that no personally identifiable information is revealed.  You may not perform any analysis, reverse engineering or processing of the data or any correlation with other data sources that could be used to determine or infer personally identifiable information.

Please refer to the Data Sharing Agreement for complete terms. Contact research-data-requests@yahoo-inc.com with questions.

==================================================================


With this dataset you can train, test, and benchmark entity linking systems on the task of linking web search queries – within the context of a search session – to entities. Entities are a key enabling component for semantic search, as many information needs can be answered by returning a list of entities, their properties, and/or their relations. A first step in any such scenario is to determine which entities appear in a query – a process commonly referred to as named entity resolution, named entity disambiguation, or semantic linking.

This dataset allows researchers and other practitioners to evaluate their systems for linking web search engine queries to entities. The dataset contains manually identified links to entities in the form of Wikipedia articles and provides the means to train, test, and benchmark such systems using manually created, gold standard data. With releasing this dataset publicly, we aim to foster research into entity linking systems for web search queries. To this end, we also include sessions and queries from the TREC Session track (years 2010–2013). Moreover, since the linked entities are aligned with a specific part of each query (a "span"), this data can also be used to evaluate systems that identify spans in queries, i.e, that perform query segmentation for web search queries, in the context of search sessions.

The dataset consists of one file "ydata-search-query-log-to-entities-v1_0.xml" that contains queries that are manually linked to Wikipedia entities, i.e., Wikipedia article titles, by human annotators ("assessors"). Its key properties are as follows:

- Queries are taken from Yahoo US Web Search and from the TREC Session track (2010-2013). 
- There are 2635 queries in 980 sessions, 7482 spans, and 5964 links to Wikipedia articles in this dataset.
- The annotations include the part of the query (the "span") that is linked to each Wikipedia article. This information can also be used for query segmentation experiments.
- The annotations identify a single, "main" entity for each query, if available. 
- The annotations include annotations for non-English, navigational, quote-or-question, adult, and ambiguous queries and also out-of-Wikipedia entities (when an entity is mentioned in a query but no suitable Wikipedia article exists). 
- The file includes session information: each session consists of an anonymized id, initial query, as well as all the queries issued within the same session and their relative date/timestamp if available. 
- Sessions are demarcated using a 30 minute time-out. 

The annotators were instructed to:

1. Span the query: Decide how to segment the query, keeping words that go together (and form one concept) together as one span. Each new entity needs to be spanned separately. For example, first and last names stay together (“janet jackson” is one span), two parts of a city name stay together (“san jose” is one span), and clear organizations too (“cirque du soleil”). The primary goal for spanning a query is determining which words belong together. So, for example, in the query "brad pitt fight club pictures" we want to know that the words "brad pitt" should be together, the words "fight club" should be together, and that "pictures" can appear separately (as in "pictures OF brad pitt IN fight club”). However, we would not like to see "brad" by itself, some random words, then "pitt fight club". (Note that multiple spans for the same part of the query are possible. For instance, one annotator labeled the query "state of decay" as both the video game and the Doctor Who episode.)
2. Identify the main part: after each span, indicate if it's the main one for a query. More than one main span/entity can be chosen.
3. Identify the most likely Wikipedia article for each span. 
4. Not to correct spelling or punctuation.

The XML file contains sessions, queries within each session, and annotations on each query. Each "session" consists of a series of queries, along with an ID and the number of queries. Each query contains a "starttime" attribute. For the TREC Session track queries, these are the event times as defined in the topic files (these are only available starting from 2011). For the others, this is an incremental number, indicating the offset and ordering of each query within a session. 

The per-query attributes are defined as follows:

- Non-English – Used if it's a non-English query. Not used for non-English names or commonly used words in English (“burrito”, “schadenfreude”).
- Entity, No Wikipedia Page – This is a good entity that just doesn't have a wikipedia page. Personal names and smaller companies will fall into this classification.
- Navigational – Used only when it's clear the query means to go directly to a specific web page. "facebook", "amazon", "www.nakedjuice.com", "youjizz" are all good examples. "Wells Fargo", "Sears", "quicktime pro" are not navigational.
- Quote or Question – Sometimes the query is a copy-paste of song lyrics or a long question about a topic. They are still tagged, if possible. 
- Adult – You'll know it when you see it. 
- Ambiguous – The term could mean a few things, and the context doesn't clarify. If a good guess can be made (“madonna” is almost certainly the singer) they are resolved anyway.
- Cannot judge – The annotator could not determine the best link. 

Each "annotation" can be labeled as being the "main" span and entity for that query by the annotators. Each identified Wikipedia article is represented by its URL as well as the Wikipedia article ID, in order to facilitate easy lookup/dereferencing.

Example:

<?xml version="1.0" encoding="UTF-8"?>
<webscope numqueries="2635" numsessions="980">
   <session id="yahoo-1" numqueries="9">
      <query adult="false" ambiguous="false" assessor="1" cannot-judge="false" navigational="false" no-wp="false" non-english="false" quote-question="false" starttime="3">
         <text><![CDATA[gemini compatibility]]></text>
         <annotation main="true">
            <span><![CDATA[gemini]]></span>
            <target wiki-id="4415777"><![CDATA[http://en.wikipedia.org/wiki/Gemini_(astrology)]]></target>
         </annotation>
         <annotation main="false">
            <span><![CDATA[compatibility]]></span>
            <target wiki-id="2809535"><![CDATA[http://en.wikipedia.org/wiki/Astrological_compatibility]]></target>
         </annotation>
      </query>
      <query adult="false" ambiguous="true" assessor="1" cannot-judge="false" navigational="true" no-wp="false" non-english="false" quote-question="false" starttime="4">
         <text><![CDATA[amazon]]></text>
         <annotation main="true">
            <span><![CDATA[amazon]]></span>
            <target wiki-id="90451"><![CDATA[http://en.wikipedia.org/wiki/Amazon.com]]></target>
         </annotation>
      </query>
...
   <session id="trec-2013-131" numqueries="1">
      <query adult="false" ambiguous="false" assessor="19" cannot-judge="false" navigational="false" no-wp="false" non-english="false" quote-question="false" starttime="38.897003">
         <text><![CDATA[us space exploration costs]]></text>
         <annotation main="false">
            <span><![CDATA[us]]></span>
            <target wiki-id="3434750"><![CDATA[http://en.wikipedia.org/wiki/United_States]]></target>
         </annotation>
         <annotation main="false">
            <span><![CDATA[space exploration]]></span>
            <target wiki-id="28431"><![CDATA[http://en.wikipedia.org/wiki/Space_exploration]]></target>
         </annotation>
         <annotation main="true">
            <span><![CDATA[costs]]></span>
            <target wiki-id="166789"><![CDATA[http://en.wikipedia.org/wiki/Cost]]></target>
         </annotation>
      </query>
   </session>
   <session id="trec-2013-132" numqueries="1">
      <query adult="false" ambiguous="false" assessor="19" cannot-judge="false" navigational="false" no-wp="false" non-english="false" quote-question="false" starttime="37.940637">
         <text><![CDATA[(euro OR eurozone) crisis]]></text>
         <annotation main="true">
            <span><![CDATA[(euro OR eurozone) crisis]]></span>
            <target wiki-id="26152387"><![CDATA[http://en.wikipedia.org/wiki/European_sovereign-debt_crisis]]></target>
         </annotation>
      </query>
   </session>
   <session id="trec-2013-133" numqueries="1">
      <query adult="false" ambiguous="false" assessor="19" cannot-judge="false" navigational="false" no-wp="false" non-english="false" quote-question="false" starttime="35.464545">
         <text><![CDATA[eurozone crisis]]></text>
         <annotation main="true">
            <span><![CDATA[eurozone crisis]]></span>
            <target wiki-id="26152387"><![CDATA[http://en.wikipedia.org/wiki/European_sovereign-debt_crisis]]></target>
         </annotation>
      </query>
   </session>
</webscope>

There are 2635 queries in 980 sessions. The first session started with the query "gemini compatibility" which is linked to http://en.wikipedia.org/wiki/Astrological_compatibility (with Wikipedia article ID 2809535) and http://en.wikipedia.org/wiki/Gemini_(astrology) (with Wikipedia article ID 4415777, which is also identified as the main entity. The very last session is from the TREC Session 2013 track and contains the single query "eurozone crisis", issued at a starttime of 35.464545. This query is linked to http://en.wikipedia.org/wiki/European_sovereign-debt_crisis (with Wikipedia article ID 26152387, which is also identified as the main entity). And so on.


