.. topic:: Guide on how to upgrade between nerd versions

Upgrade Guide
=============
This page explains differences between versions and how to adapt in order to support the new version. 


From 0.0.2 to 0.0.3
*******************

1. the parameter `OnlyNER` has been deprecated and is limited to text processing only (not PDF) for English and French.
This option will be removed in the next release.


2. the mention recognition (prior dismabiguation) has been redesigned to accomodate different type of recognitions.
Shipped with NERD there is now grobid-ner for Named Entity Recognition and Wikipedia.
They can be selected by using the parameter `mentions` and specifying a list of `recognitors`, example:

::
{
    "text": "Sample text",
    "mentions": [
        "ner",
        "wikipedia"
    ]
}


3. the option `resultLanguages` has been removed, the translated results will be provided in all languages anyway
when fetching the concept information from the knowledge base.