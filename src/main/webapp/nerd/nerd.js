/**
 *  Javascript functions for the front end.
 *
 */

var nerd = (function ($) {
    var supportedLanguages = ["en", "es", "it", "fr", "de"];

    // for components view
    var responseJson = null;

    // for associating several entities to an annotation position (to support nbest mode visualisation)
    var entityMap = new Object();

    // for complete concept information, resulting of additional calls to the knowledge base service
    var conceptMap = new Object();

    // for detailed view
    var responseJsonLId = null;
    var responseJsonNERDText = null;
    var responseJsonNERDQuery = null;


    // query for text XOR shortText content
    var queryTemplateText = {
        "text": "",
        "shortText": "",
        "termVector": [],
        "language": {"lang": "en"},
        "entities": [],
        "mentions": ["ner", "wikipedia"],
        "nbest": false,
        "sentence": false
    };

    // query + PDF
    var queryTemplatePDF = {
        // "language": {"lang": "en"},
        "mentions": ["ner", "wikipedia"],
        "nbest": false,
        "structure": "grobid"
    };

    // term lookup
    //var queryTemplate3 = {"term": "", "language": {"lang": "en"}};

    function defineBaseURL(ext) {
        var baseUrl = null;
        if ($(location).attr('href').indexOf("index.html") !== -1)
            baseUrl = $(location).attr('href').replace("index.html", "service/" + ext);
        else
            baseUrl = $(location).attr('href') + "service/" + ext;
        return baseUrl;
    }

    function setBaseUrl(ext) {
        var baseUrl = defineBaseURL(ext);
        $('#gbdForm').attr('action', baseUrl);
    }

    function setBaseUrlDetailed(ext) {
        var baseUrl = defineBaseURL(ext);
        $('#gbdForm2').attr('action', baseUrl);
    }

    $(document).ready(function () {
        // components / detailed / simple

        $("#subTitle").html("About");
        $("#divAbout").show();
        $("#divServices").hide();
        $("#divDoc").hide();
        //$("#divAdmin").hide();
        $("#nerd-text").show();
        $("#nerd-query").hide();

        //createInputTextArea('query');
        setBaseUrl('processNERDText');
        $("#selectedService").val('processNERDQuery');
        processChange();

        $('#selectedService').change(function () {
            processChange();
            return true;
        });

        $('#submitRequest').bind('click', submitQuery);
        $('#clearRequest').bind('click', clearQuery);

        $("#about").click(function () {
            $("#about").attr('class', 'section-active');
            $("#services").attr('class', 'section-non-active');
            $("#doc").attr('class', 'section-non-active');
            $("#demo").attr('class', 'section-non-active');

            $("#subTitle").html("About");
            $("#subTitle").show();

            $("#divAbout").show();
            $("#divServices").hide();
            $("#divDoc").hide();

            $("#nerd-text").hide();

            return false;
        });
        $("#services").click(function () {
            $("#services").attr('class', 'section-active');
            $("#doc").attr('class', 'section-non-active');
            $("#about").attr('class', 'section-non-active');
            $("#demo").attr('class', 'section-non-active');

            $("#subTitle").hide();
            $("#subTitle2").html("Test");
            $("#subTitle2").show();

            $("#divServices").show();
            $("#divAbout").hide();
            $("#divDoc").hide();

            return false;
        });

    });

    function clearQuery() {
        var selected = $('#selectedService').find('option:selected').attr('value');
        if (selected === 'processNERDQuery' || selected === 'processERDQuery') {
            const cloneQueryTemplateText = JSON.parse(JSON.stringify(queryTemplateText))
            $('#input').attr('value', vkbeautify.json(JSON.stringify(cloneQueryTemplateText)));
            $('#requestResult').html('');
        } else if (selected === 'processNERDQueryPDF') {
            $('#inputFile').val('');
            const cloneQueryTemplatePDF = JSON.parse(JSON.stringify(queryTemplatePDF))
            $('#input').attr('value', vkbeautify.json(JSON.stringify(cloneQueryTemplatePDF)));
            $('#requestResult').html('');
        } else if (selected === 'processLanguage') {
            $('#input').attr('value', "");
            $('#requestResult').html('');
        } else if (selected === 'processSentenceSegmentation') {
            $('#input').attr('value', "");
            $('#requestResult').html('');
        } else if (selected === 'KBTermLookup') {
            $('#input2').attr('value', "");
            $('#requestResult').html('');
        } else if (selected === 'KBConcept') {
            $('#input2').attr('value', "");
            $('#requestResult').html('');
        }
        resetExamplesClasses();
    }

    function submitQuery() {
        $('#infoResult').html('<font color="grey">Requesting server...</font>');
        $('#requestResult').html('');

        // re-init the entity map
        entityMap = new Object();
        conceptMap = new Object();
        var urlLocal = $('#gbdForm').attr('action');
        var selected = $('#selectedService').attr('value');

        if ((urlLocal.indexOf('language') !== -1) || (urlLocal.indexOf('segmentation') !== -1)) {
            // url = urlLocal + '?text=' + $('#input').val();

            var formData = new FormData();
            formData.append("text", $('#input').val());

            $.ajax({
                type: 'POST',
                url: urlLocal,
                data: formData,
                contentType: false,
                processData: false,
                success: handleSuccessfulResponse,
                error: displayErrorMessage,
                contentType: false
            });
        } else if (urlLocal.indexOf('kb/term') !== -1) {
            $.ajax({
                type: 'GET',
                url: urlLocal + '/' + $('#input2').val().trim() + '?lang=' + $('#lang').val(),
                success: handleSuccessfulResponse,
                error: displayErrorMessage,
                contentType: false
            });
        } else if (urlLocal.indexOf('kb/concept') !== -1) {
            $.ajax({
                type: 'GET',
                url: urlLocal + '/' + $('#input2').val().trim(),
                success: handleSuccessfulResponse,
                error: displayErrorMessage,
                contentType: false
            });
        } else if (selected.indexOf('PDF') !== -1) {
            if (document.getElementById("inputFile").files.length === 0) {
                displayErrorMessage("No PDF Selected");
                return;
            }
            console.log(document.getElementById("inputFile").files[0].name);

            if ((document.getElementById("inputFile").files[0].type === 'application/pdf') ||
                (document.getElementById("inputFile").files[0].name.endsWith(".pdf")) ||
                (document.getElementById("inputFile").files[0].name.endsWith(".PDF"))) {
                console.log("process pdf...");
                var formData = new FormData();
                formData.append("file", document.getElementById("inputFile").files[0]);
                formData.append("query", $('#input').val());

                // request for the annotation information
                var xhr = new XMLHttpRequest();
                var url = urlLocal;
                xhr.contentType = false;
                xhr.processData = false;
                xhr.open('POST', url, true);

                // display the local PDF
                var nbPages = -1;
                var reader = new FileReader();
                reader.onloadend = function () {
                    // to avoid cross origin issue
                    //PDFJS.disableWorker = true;
                    var pdfAsArray = new Uint8Array(reader.result);
                    // Use PDFJS to render a pdfDocument from pdf array
                    PDFJS.getDocument(pdfAsArray).then(function (pdf) {
                        // Get div#container and cache it for later use
                        var container = document.getElementById("requestResult");
                        // enable hyperlinks within PDF files.
                        //var pdfLinkService = new PDFJS.PDFLinkService();
                        //pdfLinkService.setDocument(pdf, null);

                        //$('#requestResult').html('');
                        nbPages = pdf.numPages;

                        // Loop from 1 to total_number_of_pages in PDF document
                        for (var i = 1; i <= nbPages; i++) {

                            // Get desired page
                            pdf.getPage(i).then(function (page) {
                                var table = document.createElement("table");
                                table.setAttribute('style', 'table-layout: fixed; width: 100%;')
                                var tr = document.createElement("tr");
                                var td1 = document.createElement("td");
                                var td2 = document.createElement("td");

                                tr.appendChild(td1);
                                tr.appendChild(td2);
                                table.appendChild(tr);

                                var div0 = document.createElement("div");
                                div0.setAttribute("style", "text-align: center; margin-top: 1cm;");
                                var pageInfo = document.createElement("p");
                                var t = document.createTextNode("page " + (page.pageIndex + 1) + "/" + (nbPages));
                                pageInfo.appendChild(t);
                                div0.appendChild(pageInfo);

                                td1.appendChild(div0);


                                var div = document.createElement("div");

                                // Set id attribute with page-#{pdf_page_number} format
                                div.setAttribute("id", "page-" + (page.pageIndex + 1));

                                // This will keep positions of child elements as per our needs, and add a light border
                                div.setAttribute("style", "position: relative; ");


                                // Create a new Canvas element
                                var canvas = document.createElement("canvas");
                                canvas.setAttribute("style", "border-style: solid; border-width: 1px; border-color: gray;");

                                // Append Canvas within div#page-#{pdf_page_number}
                                div.appendChild(canvas);

                                // Append div within div#container
                                td1.setAttribute('style', 'width:70%;');
                                td1.appendChild(div);

                                var annot = document.createElement("div");
                                annot.setAttribute('style', 'vertical-align:top;');
                                annot.setAttribute('id', 'detailed_annot-' + (page.pageIndex + 1));
                                td2.setAttribute('style', 'vertical-align:top;width:30%;');
                                td2.appendChild(annot);

                                container.appendChild(table);

                                //fitToContainer(canvas);

                                // we could think about a dynamic way to set the scale based on the available parent width
                                //var scale = 1.2;
                                //var viewport = page.getViewport(scale);
                                var viewport = page.getViewport((td1.offsetWidth * 0.98) / page.getViewport(1.0).width);

                                var context = canvas.getContext('2d');
                                canvas.height = viewport.height;
                                canvas.width = viewport.width;

                                var renderContext = {
                                    canvasContext: context,
                                    viewport: viewport
                                };

                                // Render PDF page
                                page.render(renderContext).then(function () {
                                    // Get text-fragments
                                    return page.getTextContent();
                                })
                                    .then(function (textContent) {
                                        // Create div which will hold text-fragments
                                        var textLayerDiv = document.createElement("div");

                                        // Set it's class to textLayer which have required CSS styles
                                        textLayerDiv.setAttribute("class", "textLayer");

                                        // Append newly created div in `div#page-#{pdf_page_number}`
                                        div.appendChild(textLayerDiv);

                                        // Create new instance of TextLayerBuilder class
                                        var textLayer = new TextLayerBuilder({
                                            textLayerDiv: textLayerDiv,
                                            pageIndex: page.pageIndex,
                                            viewport: viewport
                                        });

                                        // Set text-fragments
                                        textLayer.setTextContent(textContent);

                                        // Render text-fragments
                                        textLayer.render();
                                    });
                            });
                        }
                    });
                };
                reader.readAsArrayBuffer(document.getElementById("inputFile").files[0]);

                xhr.onreadystatechange = function (e) {
                    if (xhr.readyState === 4) {
                        if (xhr.status === 200) {
                            var response = e.target.response;
                            setupAnnotations(response);
                        } else {
                            displayErrorMessage(xhr)
                        }
                    }
                };
                xhr.send(formData);
            }

        } else {
            var formData = new FormData();
            formData.append("query", $('#input').val());
            $.ajax({
                type: 'POST',
                url: urlLocal,
                data: formData,
                contentType: false,
                processData: false,
                success: handleSuccessfulResponse,
                error: displayErrorMessage
            });
        }

        $('#infoResult').html('<font color="grey">Requesting server...</font>');
    };

    function displayErrorMessage(response) {
        var message = "";

        //Not found get handled first
        if (response.status === 404) {
            message = "Not found. Concept or terms not existing or invalid identifier"
        } else if (response.responseText) {
            message = response.responseText
        } else if (typeof(response) === "string") {
            message = response
        }

        $('#infoResult').html("<font color='red'>Error encountered while requesting the server.<br/>" + message + "</font>");
        responseJson = null;
    }

    function handleSuccessfulResponse(responseText, statusText, xhr) {
        responseJson = responseText;

        var selected = $('#selectedService').attr('value');
        if ((selected === 'processNERDQuery') && (responseJson.text != null && responseJson.text.length > 0)) {
            SubmitSuccesfulNERD(responseJson, statusText);
        }
        else if (selected === 'processLanguage') {
            SubmitSuccesfulLId(responseJson, statusText);
        }
        else if (selected === 'processSentenceSegmentation') {
            SubmitSuccesfulSentenceSegmentation(responseJson, statusText);
        }
        else if ((selected === 'processNERDQuery') && (responseJson.shortText != null) && (responseJson.shortText.length > 0)) {
            SubmitSuccesfulERDSearch(responseJson, statusText);
        }
        else if ((selected === 'processNERDQuery') && (responseJson.termVector != null) && (responseJson.termVector.length > 0)) {
            console.log("front end for term vector disambiguation not implemented yet !");
        }
        else if (selected === 'KBTermLookup') {
            SubmitSuccesfulKBTermLookup(responseJson, statusText);
        }
        else if (selected === 'KBConcept') {
            SubmitSuccesfulKBConceptLookup(responseJson, statusText);
        }

    }

    function fetchConcept(identifier, lang, successFunction) {
        $.ajax({
            type: 'GET',
            url: 'service/kb/concept/' + identifier + '?lang=' + lang,
            success: successFunction,
            dataType: 'json'
        });
    }

    function htmll(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function SubmitSuccesfulNERD(responseJson, statusText) {
        $('#infoResult').html('');
        //console.log(responseText);

        if ((responseJson == null) || (responseJson.length == 0)) {
            $('#infoResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
            return;
        }

        var display = '<div class=\"note-tabs\"> \
			<ul id=\"resultTab\" class=\"nav nav-tabs\"> \
		   		<li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Annotations</a></li>\n';
        /*if (responseJson.global_categories)
           display += '<li><a href=\"#navbar-fixed-categories\" data-toggle=\"tab\">Categories</a></li>\n';*/
        display += '<li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
			</ul> \
			<div class="tab-content"> \
			<div class="tab-pane active" id="navbar-fixed-annotation">\n';

        var nbest = false;
        if (responseJson.nbest === true)
            nbest = true;

        if (responseJson.sentences) {
            display +=
                '<div style="max-height:150px; overflow:auto;"><table id="sentenceIndex" class="table table-bordered table-condensed sentence">';
            var m = 0;
            var text = responseJson.text.replace(/\n/g, " ");
            for (var sentence in responseJson.sentences) {
                if (m == 0) {
                    display += '<tr class="highlight" id="sent_' + m + '" rank="' + m + '" >';
                }
                else {
                    display += '<tr id="sent_' + m + '" rank="' + m + '" >';
                }
                display +=
                    '<td style="width:25px;height:13px;font-size:small;">' + m + '</td>'
                var start = responseJson.sentences[sentence].offsetStart;
                var end = responseJson.sentences[sentence].offsetEnd;
                display += '<td style="font-size:small;height:13px;color:#333;">' + text.substring(start, end) + '</td>';
                display += '</tr>';
                m++;
            }
            display += '</table></div>\n';
        }

        display += '<pre style="background-color:#FFF;width:95%;" id="displayAnnotatedText">';

        // this variable is used to keep track of the last annotation and avoid "overlapping"
        // annotations in case of nbest results.
        // in case of nbest results, we haveonly one annotation in the text, but this can
        // lead to the visualisation of several info boxes on the right panel (one per entity candidate)
        var lastMaxIndex = responseJson.text.length;

        display += '<table id="sentenceNER" style="width:100%;table-layout:fixed;" class="table">';
        //var string = responseJson.text.replace(/\n/g, " ");
        var string = responseJson.text;
        if (!responseJson.sentences || (responseJson.sentences.length === 0)) {
            display += '<tr style="background-color:#FFF;">';
            var lang = 'en'; //default
            var language = responseJson.language;
            if (language)
                lang = language.lang;
            if (responseJson.entities) {
                var currentAnnotationIndex = responseJson.entities.length - 1;
                for (var m = responseJson.entities.length - 1; m >= 0; m--) {
                    var entity = responseJson.entities[m];
                    var identifier = entity.wikipediaExternalRef;
                    var wikidataId = entity.wikidataId;

                    if (identifier && (conceptMap[identifier] == null)) {
                        fetchConcept(identifier, lang, function (result) {
                            conceptMap[result.wikipediaExternalRef] = result;
                        });
                    }
                    var domains = entity.domains;
                    var label = null;
                    if (entity.type)
                        label = entity.type;
                    else if (domains && domains.length > 0) {
                        label = domains[0].toLowerCase();
                    }
                    else
                        label = entity.rawName;

                    var start = parseInt(entity.offsetStart, 10);
                    var end = parseInt(entity.offsetEnd, 10);

                    if (start > lastMaxIndex) {
                        // we have a problem in the initial sort of the entities
                        // the server response is not compatible with the client
                        console.log("Sorting of entities as present in the server's response not valid for this client.");
                    }
                    else if (start === lastMaxIndex) {
                        // the entity is associated to the previous map
                        entityMap[currentAnnotationIndex].push(responseJson.entities[m]);
                    }
                    else if (end > lastMaxIndex) {
                        end = lastMaxIndex;
                        lastMaxIndex = start;
                        // the entity is associated to the previous map
                        entityMap[currentAnnotationIndex].push(responseJson.entities[m]);
                    }
                    else {
                        string = string.substring(0, start)
                            + '<span id="annot-' + m + '" rel="popover" data-color="' + label + '">'
                            + '<span class="label ' + label + '" style="cursor:hand;cursor:pointer;" >'
                            + string.substring(start, end) + '</span></span>' + string.substring(end, string.length + 1);
                        lastMaxIndex = start;
                        currentAnnotationIndex = m;
                        entityMap[currentAnnotationIndex] = [];
                        entityMap[currentAnnotationIndex].push(responseJson.entities[m]);
                    }
                }
            }
//console.log(entityMap);
            string = "<p>" + string.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";

            display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>' + string + '</p></td>';
            display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';

            display += '</tr>';
        }
        else {
            display += '<tr style="background-color:#FFF;">';

            display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p><span id="sentence_ner">' +
                " " + '</span></p></td>';
            display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';
            display += '</tr>';
        }

        display += '</table>\n';
        display += '</pre>\n';
        display += '</div> \
					<div class="tab-pane " id="navbar-fixed-categories">\n';

        display += '<pre style="background-color:#FFF;width:50%;" id="displayCategories">';
        // display global categories information if available
        if (responseJson.global_categories) {
            display += '<p>';
            display += '<table class="table table-striped" style="width:100%;border:1px solid white;">';
            display += '<tr style="border-top:0px;"><td style="border-top:0px;"><span style="color:black;"><b>category</b></span></td><td style="border-top:0px;"></td><td style="border-top:0px;">';
            display += '<span style="color:black;"><b>score</b></span></td><td style="border-top:0px;">';
            //display += '<span style="color:black;"><b>entities</b></span></td></tr>';
            var categories = sortCategories(responseJson.global_categories);
            for (var category in categories) {
                var theCategory = responseJson.global_categories[category].category;

                var score = categories[category].weight;
                var pageId = categories[category].page_id;
                var source = categories[category].source;

                var lang = 'en';
                if (source) {
                    var ind = source.indexOf('-');
                    if (ind != -1) {
                        lang = source.substring(ind + 1, source.length);
                    }
                }

                if (score && (score.toString().length > 6)) {
                    score = score.toString().substring(0, 6);
                }
                if (score == '0.0000')
                    continue;
                display += '<tr><td><span style="color:#7F2121;">' + theCategory + '</span></td><td>' +
                    '<a href="http://' + lang + '.wikipedia.org/wiki?curid=' + pageId +
                    '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/wikipedia.png"/></a></td><td>' + score + '</td>';
                display += '</tr>';
            }
            display += '</table>';
            display += '</p></pre>\n';
        }

        display += '</div> \
					<div class="tab-pane " id="navbar-fixed-json">\n';
        // JSON visualisation component
        // with pretty print
        display += "<pre class='prettyprint' id='jsonCode'>";

        display += "<pre class='prettyprint lang-json' id='xmlCode'>";
        var testStr = vkbeautify.json(responseJson);

        display += htmll(testStr);

        display += "</pre>";
        display += '</div></div></div>';

        $('#requestResult').html(display);
        window.prettyPrint && prettyPrint();

        if (responseJson.sentences) {
            // bind the sentence table line with the appropriate sentence result display
            var nbSentences = responseJson.sentences.length;
            for (var p = 0; p < nbSentences; p++) {
                //$('#sent'+p).bind('click',viewSentenceResults());
                $('#sentenceIndex').on('click', 'tbody tr', function (event) {
                    $(this).addClass('highlight').siblings().removeClass('highlight');
                    viewSentenceResults($(this).attr('rank'));
                });
            }
            viewSentenceResults('0');
        }

        for (var key in entityMap) {
            if (entityMap.hasOwnProperty(key)) {
                $('#annot-' + key).bind('hover', viewEntity);
                $('#annot-' + key).bind('click', viewEntity);
            }
        }
        $('#detailed_annot-0').hide();
    }

    function SubmitSuccesfulERDSearch(responseJson) {
        $('#infoResult').html('');
        if ((responseJson == null) || (responseJson.length == 0)) {
            displayErrorMessage("The response is empty");
            return;
        }

        var lang = 'en'; //default
        var language = responseJson.language;
        if (language)
            lang = language.lang;

        var display = '<div class=\"note-tabs\"> \
		<ul id=\"resultTab\" class=\"nav nav-tabs\"> \
	   		<li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Entities</a></li> \
			<li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
		</ul> \
		<div class="tab-content"> \
		<div class="tab-pane active" id="navbar-fixed-annotation">\n';

        display += '<pre style="background-color:#FFF;width:95%;" id="displayAnnotatedText">';

        display += '<table id="sentenceNER" style="width:100%;table-layout:fixed;" class="table">';
        display += '<tr style="background-color:#FFF;"><td>';

        display += getPieceShowexpandNERD(responseJson, lang);
        display += '</td></tr>';
        display += '</table>';

        display += '</pre>\n';

        //$('#requestResult').html(display);

        display += '</div> \
					<div class="tab-pane " id="navbar-fixed-json">\n';
        // JSON visualisation component
        // with pretty print
        display += "<pre class='prettyprint' id='jsonCode'>";

        display += "<pre class='prettyprint lang-json' id='xmlCode'>";
        var testStr = vkbeautify.json(responseJson);

        display += htmll(testStr);

        display += "</pre>";
        display += '</div></div></div>';


        //$('#requestResult').append(piece);

        // we need to bind the checkbox...
        /*for (var sens in jsonObject['entities']) {
            $('input#selectEntity' + sens).bind('change', clickfilterchoice);
        }*/
        //$('#disambiguation_panel').show();
        $('#requestResult').html(display);
        window.prettyPrint && prettyPrint();

        for (var sens in responseJson['entities']) {
            var entity = responseJson['entities'][sens];
            var identifier = entity.wikipediaExternalRef;
            if (identifier && (conceptMap[identifier] == null)) {
                fetchConcept(identifier, lang, function (result) {
                    var localIdentifier = result.wikipediaExternalRef;
                    conceptMap[localIdentifier] = result;
                    var definitions = result.definitions;
                    var preferredTerm = result.preferredTerm;
                    var localHtml = "";
                    if (definitions && (definitions.length > 0)) {
                        localHtml = wiki2html(definitions[0]['definition'], lang);
                        $("#def-" + localIdentifier).html(localHtml);
                    }
                    if (preferredTerm)
                        $("#pref-" + localIdentifier).html(preferredTerm);
                });
            }
        }
    }

    var SubmitSuccesfulKBTermLookup = function (responseJson) {
        $('#infoResult').html('');
        if ((responseJson == null) || (responseJson.length == 0)) {
            displayErrorMessage("The response is empty");
            return;
        }

        var lang = 'en'; //default
        if (responseJson.lang)
            lang = responseJson.lang;

        var display = '<div class=\"note-tabs\"> \
		<ul id=\"resultTab\" class=\"nav nav-tabs\"> \
	   		<li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Entities</a></li> \
			<li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
		</ul> \
		<div class="tab-content"> \
		<div>Number of ambiguous concepts: ' + responseJson.senses.length + '</div> \
		<div class="tab-pane active" id="navbar-fixed-annotation">\n';

        display += '<pre style="background-color:#FFF;width:95%;" id="displayAnnotatedText">';

        display += '<table id="sentenceNER" style="width:100%;table-layout:fixed;" class="table">';
        display += '<tr style="background-color:#FFF;"><td>';

        display += getPieceShowSenses(responseJson, lang);
        display += '</td></tr>';
        display += '</table>';

        display += '</pre>\n';

        //$('#requestResult').html(display);

        display += '</div> \
					<div class="tab-pane " id="navbar-fixed-json">\n';
        // JSON visualisation component
        // with pretty print
        display += "<pre class='prettyprint' id='jsonCode'>";

        display += "<pre class='prettyprint lang-json' id='xmlCode'>";
        var testStr = vkbeautify.json(responseJson);

        display += htmll(testStr);

        display += "</pre>";
        display += '</div></div></div>';

        $('#requestResult').html(display);
        window.prettyPrint && prettyPrint();

        for (var sens in responseJson['senses']) {
            var entity = responseJson['senses'][sens];
            var identifier = entity.pageid;
            if (identifier && (conceptMap[identifier] == null)) {
                fetchConcept(identifier, lang, function (result) {
                    var localIdentifier = result.wikipediaExternalRef;
                    conceptMap[localIdentifier] = result;
                    var definitions = result.definitions;
                    if (definitions && (definitions.length > 0)) {
                        var localHtml = wiki2html(definitions[0]['definition'], lang);
                        $("#def-" + localIdentifier).html(localHtml);
                    }
                    var wikidataId = result.wikidataId;
                    if (wikidataId) {
                        var localHtml = '<a href="https://www.wikidata.org/wiki/' + wikidataId +
                            '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/Wikidata-logo.svg"/></a>';
                        $("#wikidata-" + localIdentifier).html(localHtml);
                    }
                });
            }
        }
    };

    var SubmitSuccesfulKBConceptLookup = function (responseText, statusText) {
        $('#infoResult').html('');
        if ((responseJson == null) || (responseJson.length == 0)) {
            $('#infoResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
            return;
        }

        var lang = 'en'; //default
        if ($('#lang').val())
            lang = $('#lang').val();

        var display = '<div class=\"note-tabs\"> \
		<ul id=\"resultTab\" class=\"nav nav-tabs\"> \
	   		<li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Entity</a></li> \
			<li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
		</ul> \
		<div class="tab-content"> \
		<div class="tab-pane active" id="navbar-fixed-annotation">\n';

        //display += '<pre style="background-color:#FFF;width:95%;" id="displayAnnotatedText">';
        display += '<table style="width:100%;table-layout:fixed;" class="table">';
        display += '<colgroup><col style="width: 70%;"><col style="width: 25%;"><col style="width: 5%;"></colgroup>';
        //display += '<tr style="background-color:#FFF;"><td>';
        display += getPieceShowConcept(responseJson, lang);
        //display += '</td></tr>';
        display += '</table>';

        display += '</div> \
					<div class="tab-pane " id="navbar-fixed-json">\n';
        // JSON visualisation component
        // with pretty print
        display += "<pre class='prettyprint lang-json' id='jsonCode'>";
        var testStr = vkbeautify.json(responseJson);
        display += htmll(testStr);
        display += "</pre>";
        display += '</div></div></div>';
        $('#requestResult').html(display);
        window.prettyPrint && prettyPrint();
    };

    var getPieceShowConcept = function (entity, lang) {
        var type = entity.type;

        var colorLabel = null;
        if (type)
            colorLabel = type;
        else if (domains && domains.length > 0) {
            colorLabel = domain;
        }
        else
            colorLabel = entity.preferredTerm;

        var subType = entity.subtype;
        var wikipedia = entity.wikipediaExternalRef;
        var wikidataId = entity.wikidataId;

        var piece = '<tr><td>';
        // we ouput here all fields

        // term in target language
        piece += '<table class="concept" style="width:100%;">';
        piece += '<tr><td style="width:100%;border-top:0px;"><table><tr><td style="padding-left:0px;border-top:0px;"><p><b>' +
            entity.preferredTerm + ' (' + lang + ')</b></p></td>';

        // multilingual terms
        if (entity.multilingual) {
            for (var mult in entity.multilingual) {
                piece += '<td style="border-top:0px;"><p>' + entity.multilingual[mult].term + ' (' + entity.multilingual[mult].lang + ')</p></td>';
            }
        }
        piece += '</tr></table></td></tr>';

        // definition
        var definitions = entity.definitions;
        var localHtml = "";
        if (definitions && (definitions.length > 0))
            localHtml += wiki2html(definitions[0]['definition'], lang);
        piece += '<tr><td><div class="wiky_preview_area2">' + localHtml + '</div></td></tr>';

        // domains
        var domains = entity.domains;
        localHtml = "";
        if (domains && (domains.length > 0)) {
            for (var domain in domains) {
                if (domain != 0)
                    localHtml += ", "
                localHtml += domains[domain];
            }
        }
        piece += '<tr><td><p><b>Domains</b>: ' + localHtml + '</p></td></tr>';

        // categories
        var categories = entity.categories;
        localHtml = "";
        if (categories && (categories.length > 0)) {
            for (var cat in categories) {
                if (cat != 0)
                    localHtml += ", "
                localHtml += categories[cat].category;
            }
        }
        piece += '<tr><td><p><b>Categories</b>: ' + localHtml + '</p></td></tr>';

        // statements
        var statements = entity.statements;
        if ((statements != null) && (statements.length > 0)) {
            localHtml = "";
            for (var i in statements) {
                var statement = statements[i];
                localHtml += displayStatement(statement);
            }
            piece += "<tr><td><table><tr><td style='padding:0px;border-top:0px;'><p><b>Statements: </b></p></td><td style='border-top:0px;'><div><table class='properties' style='width:100%;background-color:#fff;border:0px'>" +
                localHtml + "</table></div></td></tr></table></td></tr>";
        }

        piece += '</table>';

        // wikimedia image
        piece += '</td><td>';
        piece += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("' + wikipedia + '", "' + lang + '")</script></span>';

        // clickable wikipedia icon
        piece += '<td style="align:center;">';
        if (wikipedia) {
            piece += '<a href="http://' + lang + '.wikipedia.org/wiki?curid=' +
                wikipedia +
                '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/wikipedia.png"/></a>';
        }
        if (wikidataId) {
            piece += '<a href="https://www.wikidata.org/wiki/' +
                wikidataId +
                '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/Wikidata-logo.svg"/></a>';
        }
        piece += '</td></tr>';

        return piece;
    };

    var getPieceShowexpandNERD = function (jsonObject, lang) {
        var piece = '<div class="mini-layout fluid" style="background-color:#F7EDDC;"> \
					   		 <div class="row-fluid"><div class="span12" style="width:100%;">';
//							 <div class="row-fluid"><div class="span11" style="width:95%;">';
        /*var lang = 'en'; //default
        var language = jsonObject.language;
        if (language)
            lang = language.lang;*/
        if (jsonObject['entities']) {
            piece += '<table class="table" style="width:100%;border:1px solid white;">';
            for (var sens in jsonObject['entities']) {
                var entity = jsonObject['entities'][sens];

                var domains = entity.domains;
                if (domains && domains.length > 0) {
                    domain = domains[0].toLowerCase();
                }
                var type = entity.type;

                var colorLabel = null;
                if (type)
                    colorLabel = type;
                else if (domains && domains.length > 0) {
                    colorLabel = domain;
                }
                else
                    colorLabel = entity.rawName;

                var start = parseInt(entity.offsetStart, 10);
                var end = parseInt(entity.offsetEnd, 10);

                var subType = entity.subtype;
//                var conf = entity.nerd_score;
                var conf = entity.confidence_score;
                if (conf && conf.length > 4)
                    conf = conf.substring(0, 4);

                //var definitions = getDefinitions(identifier);

                var wikipedia = entity.wikipediaExternalRef;
                var wikidataId = entity.wikidataId;
                var content = entity.rawName; //$(this).text();
                //var preferredTerm = getPreferredTerm(identifier);

                piece += '<tr id="selectLine' + sens + '" href="'
                    + wikipedia + '" >'
                    + '<td id="selectArea' + sens + '" href="'
                    + wikipedia + '" width="15%">';
                piece += '<p><b>' + entity.rawName + '</b></p></td>';
                //piece += '<td><strong>' + entity.rawName + '&nbsp;</strong></td><td>'+
                var localHtml1 = "";
                /*if (definitions && (definitions.length > 0))
                    localHtml = wiki2html(definitions[0]['definition'], lang);
                else*/
                localHtml1 = "<span id=\"def-" + wikipedia + "\"></span>";
                piece += '<td>';
                if (conf)
                    piece += '<p><b>Conf</b>: ' + conf + '</p>';
                /*if ( preferredTerm && (entity.rawName.toLowerCase() != preferredTerm.toLowerCase()) ) {
                    piece += '<p><b>' + preferredTerm + ': </b>' + localHtml + '</p>';
                }
                else {
                    piece += '<p>' + localHtml + '</p>';
                }*/
                //var localHtml2 = "<span id=\"pref-"+wikipedia+"\"></span>";
                //piece += '<p><b>' + localHtml2 + ': </b>' + localHtml1 + '</p>';
                piece += localHtml1;
                piece += '</td><td width="25%">';
                piece += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("' + wikipedia + '", "' + lang + '")</script></span>';
                piece += '</td><td>';
                piece += '<table><tr><td>';

                if (wikipedia) {
                    piece += '<a href="http://' + lang + '.wikipedia.org/wiki?curid=' +
                        wikipedia +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/wikipedia.png"/></a>';
                }
                if (wikidataId) {
                    piece += '<a href="https://www.wikidata.org/wiki/' +
                        wikidataId +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/Wikidata-logo.svg"/></a>';
                }
                piece += '</td></tr><tr><td>';
                piece += '</td></tr></table>';

                piece += '</td></tr>';
            }
            piece += '</table>';
        }

        piece += '</div></div>';
        return piece;
    };

    var getPieceShowSenses = function (jsonObject, lang) {
        var piece = '<div class="mini-layout fluid" style="background-color:#F7EDDC;"> \
					   		 <div class="row-fluid"><div class="span12" style="width:100%;">';
        if (jsonObject['senses']) {
            piece += '<table class="table" style="width:100%;border:1px solid white;">';
            for (var sens in jsonObject['senses']) {
                var entity = jsonObject['senses'][sens];

                var domains = entity.domains;
                if (domains && domains.length > 0) {
                    domain = domains[0].toLowerCase();
                }

                var colorLabel = null;
                if (domains && domains.length > 0) {
                    colorLabel = domain;
                }
                else
                    colorLabel = entity.rawName;

                var prob_c = entity.prob_c;
                /*if (prob_c && prob_c.length > 6)
                    prob_c = prob_c.substring(0, 6);*/

                var wikipedia = entity.pageid;
                var wikidataId = entity.wikidataId;
                var content = entity.preferred;

                piece += '<tr id="selectLine' + sens + '" href="'
                    + wikipedia + '" >'
                    + '<td id="selectArea' + sens + '" href="'
                    + wikipedia + '" width="15%">';
                piece += '<p><b>' + content + '</b></p></td>';
                var localHtml1 = "<span id=\"def-" + wikipedia + "\"></span>";
                piece += '<td>';
                if (prob_c)
                    piece += '<p><b>Cond. prob.</b>: ' + prob_c + '</p>';

                piece += localHtml1;
                piece += '</td><td width="25%">';
                piece += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("' + wikipedia + '", "' + lang + '")</script></span>';
                piece += '</td><td>';
                piece += '<table><tr><td>';

                if (wikipedia) {
                    piece += '<a href="http://' + lang + '.wikipedia.org/wiki?curid=' +
                        wikipedia +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/wikipedia.png"/></a>';
                }
                if (wikidataId) {
                    piece += '<a href="https://www.wikidata.org/wiki/' +
                        wikidataId +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/Wikidata-logo.svg"/></a>';
                } else {
                    piece += '<span id="wikidata-' + wikipedia + '"></span>';
                }
                piece += '</td></tr><tr><td>';
                piece += '</td></tr></table>';

                piece += '</td></tr>';
            }
            piece += '</table>';
        }

        piece += '</div></div>';
        return piece;
    };

    const wikimediaURL_prefix = 'https://';
    const wikimediaURL_suffix = '.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=200&pageids=';

    wikimediaUrls = {};
    for (var i = 0; i < supportedLanguages.length; i++) {
        var lang = supportedLanguages[i];
        wikimediaUrls[lang] = wikimediaURL_prefix + lang + wikimediaURL_suffix
    }

    var imgCache = {};

    window.lookupWikiMediaImage = function (wikipedia, lang) {
        // first look in the local cache
        if (lang + wikipedia in imgCache) {
            var imgUrl = imgCache[lang + wikipedia];
            var document = (window.top) ? window.top.document : window.document;
            var spanNode = document.getElementById("img-" + wikipedia);
            spanNode.innerHTML = '<img src="' + imgUrl + '"/>';
        } else {
            // otherwise call the wikipedia API
            var theUrl = wikimediaUrls[lang] + wikipedia;

            // note: we could maybe use the en cross-lingual correspondence for getting more images in case of
            // non-English pages
            $.ajax({
                url: theUrl,
                jsonp: "callback",
                dataType: "jsonp",
                xhrFields: {withCredentials: true},
                success: function (response) {
                    var document = (window.top) ? window.top.document : window.document;
                    var spanNode = document.getElementById("img-" + wikipedia);
                    if (response.query && spanNode) {
                        if (response.query.pages[wikipedia]) {
                            if (response.query.pages[wikipedia].thumbnail) {
                                var imgUrl = response.query.pages[wikipedia].thumbnail.source;
                                spanNode.innerHTML = '<img src="' + imgUrl + '"/>';
                                // add to local cache for next time
                                imgCache[lang + wikipedia] = imgUrl;
                            }
                        }
                    }
                }
            });
        }
    };

    function viewSentenceResults(numb) {
        var sentence = parseInt(numb, 10);
        console.log("select sentence " + sentence);

        var text = responseJson.text;
        var currentSentence = text.substring(responseJson.sentences[sentence].offsetStart,
            responseJson.sentences[sentence].offsetEnd);

        var lastMaxIndex = responseJson.text.length;
        if (responseJson.entities) {
            var currentAnnotationIndex = responseJson.entities.length - 1;
            for (var m = responseJson.entities.length - 1; m >= 0; m--) {
                if ((responseJson.entities[m].offsetStart >= responseJson.sentences[sentence].offsetStart) &&
                    (responseJson.entities[m].offsetEnd <= responseJson.sentences[sentence].offsetEnd)) {
                    var entity = responseJson.entities[m];
                    var domains = entity.domains;
                    var label = null;
                    if (entity.type)
                        label = entity.type;
                    else if (domains && domains.length > 0) {
                        label = domains[0].toLowerCase();
                    }
                    else
                        label = entity.rawName;

                    //var start = parseInt(entity.offsetStart,10);
                    //var end = parseInt(entity.offsetEnd,10);
                    var start = parseInt(entity.offsetStart, 10) - responseJson.sentences[sentence].offsetStart;
                    var end = parseInt(entity.offsetEnd, 10) - responseJson.sentences[sentence].offsetStart;

                    if (start > lastMaxIndex) {
                        // we have a problem in the initial sort of the entities
                        // the server response is not compatible with the client
                        console.log("Sorting of entities as present in the server's response not valid for this client.");
                    }
                    else if (start == lastMaxIndex) {
                        // the entity is associated to the previous map
                        entityMap[currentAnnotationIndex].push(responseJson.entities[m]);
                    }
                    else if (end > lastMaxIndex) {
                        end = lastMaxIndex;
                        lastMaxIndex = start;
                        // the entity is associated to the previous map
                        entityMap[currentAnnotationIndex].push(responseJson.entities[m]);
                    }
                    else {
                        currentSentence = currentSentence.substring(0, start)
                            + '<span id="annot-' + m + '" rel="popover" data-color="' + label + '">'
                            + '<span class="label ' + label + '" style="cursor:hand;cursor:pointer;" >'
                            + currentSentence.substring(start, end) + '</span></span>' + currentSentence.substring(end, currentSentence.length + 1);
                        lastMaxIndex = start;
                        currentAnnotationIndex = m;
                        entityMap[currentAnnotationIndex] = [];
                        entityMap[currentAnnotationIndex].push(responseJson.entities[m]);
                    }
                }
            }
            currentSentence = "<p>" + currentSentence.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
            // string = string.replace("<p></p>", "");

            // display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>'+string+'</p></td>';
            // display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';
        }

        $('#sentence_ner').html(currentSentence);

        for (var key in entityMap) {
            if (entityMap.hasOwnProperty(key)) {
                $('#annot-' + key).bind('hover', viewEntity);
                $('#annot-' + key).bind('click', viewEntity);
            }
        }
        $('#detailed_annot-0').hide();
    }

    function setupAnnotations(response) {
        // TBD: we must check/wait that the corresponding PDF page is rendered at this point
        if ((response == null) || (response.length == 0)) {
            $('#infoResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
            return;
        } else {
            $('#infoResult').html('');
        }

        responseJson = jQuery.parseJSON(response);

        var pageInfo = responseJson.pages;
        var page_height = 0.0;
        var page_width = 0.0;

        var entities = responseJson.entities;
        if (entities) {
            // hey bro, this must be asynchronous to avoid blocking the brothers
            entities.forEach(function (entity, n) {
                var entityType = entity.type;
                if (!entityType) {
                    if (entity.domains && entity.domains.length > 0)
                        entityType = entity.domains[0]
                }

                entityMap[n] = [];
                entityMap[n].push(entity);

                var lang = 'en'; //default
                var language = responseJson.language;
                if (language)
                    lang = language.lang;

                var identifier = entity.wikipediaExternalRef;
                if (identifier && (conceptMap[identifier] == null)) {
                    fetchConcept(identifier, lang, function (result) {
                        conceptMap[result.wikipediaExternalRef] = result;
                    });
                }

                //var theId = measurement.type;
                //var theUrl = null;
                //var theUrl = annotation.url;
                var pos = entity.pos;
                if ((pos != null) && (pos.length > 0)) {
                    pos.forEach(function (thePos, m) {
                        // get page information for the annotation
                        var pageNumber = thePos.p;
                        if (pageInfo[pageNumber - 1]) {
                            page_height = pageInfo[pageNumber - 1].page_height;
                            page_width = pageInfo[pageNumber - 1].page_width;
                        }
                        annotateEntity(entityType, thePos, page_height, page_width, n, m);
                    });
                }
            });
        }
    }

    function annotateEntity(theType, thePos, page_height, page_width, entityIndex, positionIndex) {
        var page = thePos.p;
        var pageDiv = $('#page-' + page);
        var canvas = pageDiv.children('canvas').eq(0);
        //var canvas = pageDiv.find('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_y = canvasHeight / page_height;
        var scale_x = canvasWidth / page_width;

        var x = thePos.x * scale_x - 1;
        var y = thePos.y * scale_y - 1;
        var width = thePos.w * scale_x + 1;
        var height = thePos.h * scale_y + 1;

        //make clickable the area
        theType = "" + theType;
        if (theType)
            theType = theType.replace(" ", "_");
        var element = document.createElement("a");
        var attributes = "display:block; width:" + width + "px; height:" + height + "px; position:absolute; top:" +
            y + "px; left:" + x + "px;";
        element.setAttribute("style", attributes + "border-width: 2px;border-style:solid; "); //border-color: " + getColor(theId) +";");
        //element.setAttribute("style", attributes + "border:2px solid;");
        element.setAttribute("class", theType.toLowerCase());
        element.setAttribute("id", 'annot-' + entityIndex + '-' + positionIndex);
        element.setAttribute("page", page);

        pageDiv.append(element);

        $('#annot-' + entityIndex + '-' + positionIndex).bind('hover', viewEntityPDF);
        $('#annot-' + entityIndex + '-' + positionIndex).bind('click', viewEntityPDF);
    }

    function viewEntityPDF() {
        var pageIndex = $(this).attr('page');
        var localID = $(this).attr('id');

        console.log('viewEntityPDF ' + pageIndex + ' / ' + localID);

        if (responseJson == null)
            return;

        if (responseJson.entities == null) {
            return;
        }

        var topPos = $(this).position().top;

        var ind1 = localID.indexOf('-');
        var localEntityNumber = parseInt(localID.substring(ind1 + 1, localID.length));

        if ((entityMap[localEntityNumber] == null) || (entityMap[localEntityNumber].length == 0)) {
            // this should never be the case
            console.log("Error for visualising annotation with id " + localEntityNumber
                + ", empty list of entities");
        }

        var lang = 'en'; //default
        var language = responseJson.language;
        if (language)
            lang = language.lang;
        var string = "";
        for (var entityListIndex = entityMap[localEntityNumber].length - 1;
             entityListIndex >= 0;
             entityListIndex--) {
            var entity = entityMap[localEntityNumber][entityListIndex];
            var wikipedia = entity.wikipediaExternalRef;
            var wikidataId = entity.wikidataId;
            var domains = entity.domains;
            var type = entity.type;

            var colorLabel = null;
            if (type)
                colorLabel = type;
            else if (domains && domains.length > 0) {
                colorLabel = domains[0].toLowerCase();
            }
            else
                colorLabel = entity.rawName;

            var subType = entity.subtype;
            //var conf = entity.nerd_score;
//            var conf = entity.nerd_selection_score;
            var conf = entity.confidence_score;
            //var definitions = entity.definitions;
            var definitions = getDefinitions(wikipedia);

            var content = entity.rawName;
            //var normalized = entity.preferredTerm;
            var normalized = getPreferredTerm(wikipedia);

            var sense = null;
            if (entity.sense)
                sense = entity.sense.fineSense;

            string += "<div class='info-sense-box " + colorLabel + "'";
            if (topPos != -1)
                string += " style='vertical-align:top; position:relative; top:" + topPos + "'";

            string += "><h3 style='color:#FFF;padding-left:10px;'>" + content.toUpperCase() +
                "</h3>";
            string += "<div class='container-fluid' style='background-color:#F9F9F9;color:#70695C;border:padding:5px;margin-top:5px;'>" +
                "<table style='width:100%;background-color:#fff;border:0px'><tr style='background-color:#fff;border:0px;'><td style='background-color:#fff;border:0px;'>";

            if (type)
                string += "<p>Type: <b>" + type + "</b></p>";

            if (sense) {
                // to do: cut the sense string to avoid a string too large
                if (sense.length <= 20)
                    string += "<p>Sense: <b>" + sense + "</b></p>";
                else {
                    var ind = sense.indexOf('_');
                    if (ind != -1) {
                        string += "<p>Sense: <b>" + sense.substring(0, ind + 1) + "<br/>" +
                            sense.substring(ind + 1, sense.length) + "</b></p>";
                    }
                    else
                        string += "<p>Sense: <b>" + sense + "</b></p>";
                }
            }
            if (normalized)
                string += "<p>Normalized: <b>" + normalized + "</b></p>";

            if (domains && domains.length > 0) {
                string += "<p>Domains: <b>";
                for (var i = 0; i < domains.length; i++) {
                    if (i != 0)
                        string += ", ";
                    string += domains[i];
                }
                string += "</b></p>";
            }

            string += "<p>conf: <i>" + conf + "</i></p>";
            string += "</td><td style='align:right;bgcolor:#fff'>";
            string += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("' + wikipedia + '", "' + lang + '")</script></span>';

            string += "</td></tr></table>";

            // definition
            if ((definitions != null) && (definitions.length > 0)) {
                var localHtml = wiki2html(definitions[0]['definition'], lang);
                string += "<p><div class='wiky_preview_area2'>" + localHtml + "</div></p>";
            }

            // statements
            var statements = getStatements(wikipedia);
            if ((statements != null) && (statements.length > 0)) {
                var localHtml = "";
                for (var i in statements) {
                    var statement = statements[i];
                    localHtml += displayStatement(statement);
                }
                //string += "<p><div><table class='statements' style='width:100%;border-color:#fff;border:1px'>" + localHtml + "</table></div></p>";

                // make the statements information collapsible
                string += "<p><div class='panel-group' id='accordionParent'>";
                string += "<div class='panel panel-default'>";
                string += "<div class='panel-heading' style='background-color:#F9F9F9;color:#70695C;border:padding:0px;font-size:small;'>";
                // accordion-toggle collapsed: put the chevron icon down when starting the page; accordion-toggle : put the chevron icon up; show elements for every page
                string += "<a class='accordion-toggle collapsed' data-toggle='collapse' data-parent='#accordionParent' href='#collapseElement"+ pageIndex+ "' style='outline:0;'>";
                string += "<h5 class='panel-title' style='font-weight:normal;'>Wikidata statements</h5>";
                string += "</a>";
                string += "</div>";
                // panel-collapse collapse: hide the content of statemes when starting the page; panel-collapse collapse in: show it
                string += "<div id='collapseElement"+ pageIndex +"' class='panel-collapse collapse'>";
                string += "<div class='panel-body'>";
                string += "<table class='statements' style='width:100%;background-color:#fff;border:1px'>" + localHtml + "</table>";
                string += "</div></div></div></div></p>";
            }

            // reference of Wikipedia/Wikidata
            if ((wikipedia != null) || (wikidataId != null)) {
                string += '<p>References: '
                if (wikipedia != null) {
                    string += '<a href="http://' + lang + '.wikipedia.org/wiki?curid=' +
                        wikipedia +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" ' +
                        ' src="resources/img/wikipedia.png"/></a>';
                }
                if (wikidataId != null) {
                    string += '<a href="https://www.wikidata.org/wiki/' +
                        wikidataId +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" ' +
                        ' src="resources/img/Wikidata-logo.svg"/></a>';
                }
                string += '</p>';
            }

            string += "</div></div>";
        }
        $('#detailed_annot-' + pageIndex).html(string);
        $('#detailed_annot-' + pageIndex).show();
    }

    function getDefinitions(identifier) {
        var localEntity = conceptMap[identifier];
        if (localEntity != null) {
            return localEntity.definitions;
        } else
            return null;
    }

    function getCategories(identifier) {
        var localEntity = conceptMap[identifier];
        if (localEntity != null) {
            return localEntity.categories;
        } else
            return null;
    }

    function getMultilingual(identifier) {
        var localEntity = conceptMap[identifier];
        if (localEntity != null) {
            return localEntity.multilingual;
        } else
            return null;
    }

    function getPreferredTerm(identifier) {
        var localEntity = conceptMap[identifier];
        if (localEntity != null) {
            return localEntity.preferredTerm;
        } else
            return null;
    }

    function getStatements(identifier) {
        var localEntity = conceptMap[identifier];
        if (localEntity != null) {
            return localEntity.statements;
        } else
            return null;
    }

    function viewEntity() {
        var localID = $(this).attr('id');

        if (responseJson == null)
            return;

        if (responseJson.entities == null) {
            return;
        }

        var ind1 = localID.indexOf('-');
        var localEntityNumber = parseInt(localID.substring(ind1 + 1, localID.length));

        if ((entityMap[localEntityNumber] == null) || (entityMap[localEntityNumber].length == 0)) {
            // this should never be the case
            console.log("Error for visualising annotation with id " + localEntityNumber
                + ", empty list of entities");
        }

        var lang = 'en'; //default
        var language = responseJson.language;
        if (language)
            lang = language.lang;
        var string = "";
        for (var entityListIndex = entityMap[localEntityNumber].length - 1;
             entityListIndex >= 0;
             entityListIndex--) {
            var entity = entityMap[localEntityNumber][entityListIndex];
            var wikipedia = entity.wikipediaExternalRef;
            var wikidataId = entity.wikidataId;
            var domains = entity.domains;
            var type = entity.type;

            var colorLabel = null;
            if (type)
                colorLabel = type;
            else if (domains && domains.length > 0) {
                colorLabel = domains[0].toLowerCase();
            }
            else
                colorLabel = entity.rawName;

            var subType = entity.subtype;
            //var conf = entity.nerd_score;
//            var conf = entity.nerd_selection_score;
            var conf = entity.confidence_score;
            //var definitions = entity.definitions;
            var definitions = getDefinitions(wikipedia);

            var content = entity.rawName;
            //var normalized = entity.preferredTerm;
            var normalized = getPreferredTerm(wikipedia);

            var sense = null;
            if (entity.sense)
                sense = entity.sense.fineSense;

            string += "<div class='info-sense-box " + colorLabel +
                "'><h3 style='color:#FFF;padding-left:10px;'>" + content.toUpperCase() +
                "</h3>";
            string += "<div class='container-fluid' style='background-color:#F9F9F9;color:#70695C;border:padding:5px;margin-top:5px;'>" +
                "<table style='width:100%;background-color:#fff;border:0px'><tr style='background-color:#fff;border:0px;'><td style='background-color:#fff;border:0px;'>";

            if (type)
                string += "<p>Type: <b>" + type + "</b></p>";

            if (sense) {
                // to do: cut the sense string to avoid a string too large
                if (sense.length <= 20)
                    string += "<p>Sense: <b>" + sense + "</b></p>";
                else {
                    var ind = sense.indexOf('_');
                    if (ind != -1) {
                        string += "<p>Sense: <b>" + sense.substring(0, ind + 1) + "<br/>" +
                            sense.substring(ind + 1, sense.length) + "</b></p>";
                    }
                    else
                        string += "<p>Sense: <b>" + sense + "</b></p>";
                }
            }
            if (normalized)
                string += "<p>Normalized: <b>" + normalized + "</b></p>";

            if (domains && domains.length > 0) {
                string += "<p>Domains: <b>";
                for (var i = 0; i < domains.length; i++) {
                    if (i != 0)
                        string += ", ";
                    string += domains[i];
                }
                string += "</b></p>";
            }

            string += "<p>conf: <i>" + conf + "</i></p>";
            string += "</td><td style='align:right;bgcolor:#fff'>";
            string += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("' + wikipedia + '", "' + lang + '")</script></span>';

            string += "</td></tr></table>";

            // definition
            if ((definitions != null) && (definitions.length > 0)) {
                var localHtml = wiki2html(definitions[0]['definition'], lang);
                string += "<p><div class='wiky_preview_area2'>" + localHtml + "</div></p>";
            }

            // statements
            var statements = getStatements(wikipedia);
            if ((statements != null) && (statements.length > 0)) {
                var localHtml = "";
                for (var i in statements) {
                    var statement = statements[i];
                    localHtml += displayStatement(statement);
                }
//                string += "<p><div><table class='statements' style='width:100%;background-color:#fff;border:1px'>" + localHtml + "</table></div></p>";

                // make the statements information collapsible
                string += "<p><div class='panel-group' id='accordionParent'>";
                string += "<div class='panel panel-default'>";
                string += "<div class='panel-heading' style='background-color:#F9F9F9;color:#70695C;border:padding:0px;font-size:small;'>";
                // accordion-toggle collapsed: put the chevron icon down when starting the page; accordion-toggle : put the chevron icon up
                string += "<a class='accordion-toggle collapsed' data-toggle='collapse' data-parent='#accordionParent' href='#collapseElement"+ entityListIndex+ "' style='outline:0;'>";
                string += "<h5 class='panel-title' style='font-weight:normal;'>Wikidata statements</h5>";
                string += "</a>";
                string += "</div>";
                // panel-collapse collapse: hide the content of statemes when starting the page; panel-collapse collapse in: show it
                string += "<div id='collapseElement"+ entityListIndex +"' class='panel-collapse collapse'>";
                string += "<div class='panel-body'>";
                string += "<table class='statements' style='width:100%;background-color:#fff;border:1px'>" + localHtml + "</table>";
                string += "</div></div></div></div></p>";
            }

            // reference of Wikipedia/Wikidata
            if (wikipedia != null) {
                string += '<p>References: '
                if (wikipedia != null) {
                    string += '<a href="http://' + lang + '.wikipedia.org/wiki?curid=' +
                        wikipedia +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" ' +
                        ' src="resources/img/wikipedia.png"/></a>';
                }
                if (wikidataId != null) {
                    string += '<a href="https://www.wikidata.org/wiki/' +
                        wikidataId +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" ' +
                        ' src="resources/img/Wikidata-logo.svg"/></a>';
                }
                string += '</p>';
            }

            string += "</div></div>";
        }
        $('#detailed_annot-0').html(string);
        $('#detailed_annot-0').show();
    }

    function displayStatement(statement) {
        var localHtml = "";
        if (statement.propertyId) {
            if (statement.propertyName) {
                localHtml += "<tr><td>" + statement.propertyName + "</td>";
            } else if (statement.propertyId) {
                localHtml += "<tr><td>" + statement.propertyId + "</td>";
            }

            // value dislay depends on the valueType of the property
            var valueType = statement.valueType;
            if (valueType && (valueType == 'time')) {
                // we have here an ISO time expression
                if (statement.value) {
                    var time = statement.value.time;
                    if (time) {
                        var ind = time.indexOf("T");
                        if (ind == -1)
                            localHtml += "<td>" + time.substring(1) + "</td></tr>";
                        else
                            localHtml += "<td>" + time.substring(1, ind) + "</td></tr>";
                    }
                }
            } else if (valueType && (valueType == 'globe-coordinate')) {
                // we have some (Earth) GPS coordinates
                if (statement.value) {
                    var latitude = statement.value.latitude;
                    var longitude = statement.value.longitude;
                    var precision = statement.value.precision;
                    var gpsString = "";
                    if (latitude) {
                        gpsString += "latitude: " + latitude;
                    }
                    if (longitude) {
                        gpsString += ", longitude: " + longitude;
                    }
                    if (precision) {
                        gpsString += ", precision: " + precision;
                    }
                    localHtml += "<td>" + gpsString + "</td></tr>";
                }
            } else if (valueType && (valueType == 'string')) {
                if (statement.propertyId == "P2572") {
                    // twitter hashtag
                    if (statement.value) {
                        localHtml += "<td><a href='https://twitter.com/hashtag/" + statement.value.trim() + "?src=hash' target='_blank'>#" +
                            statement.value + "</a></td></tr>";
                    } else {
                        localHtml += "<td>" + "</td></tr>";
                    }
                } else {
                    if (statement.value) {
                        localHtml += "<td>" + statement.value + "</td></tr>";
                    } else {
                        localHtml += "<td>" + "</td></tr>";
                    }
                }
            }
            else {
                // default
                if (statement.valueName) {
                    localHtml += "<td>" + statement.valueName + "</td></tr>";
                } else if (statement.value) {
                    localHtml += "<td>" + statement.value + "</td></tr>";
                } else {
                    localHtml += "<td>" + "</td></tr>";
                }
            }
        }
        return localHtml;
    }

    function SubmitSuccesfulLId(responseJson, statusText) {
        $('#infoResult').html('');
        if ((responseJson == null) || (responseJson.length == 0)) {
            $('#infoResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: " +
                    "response is empty.</font>");
            return;
        }

        //responseJson = responseText;//jQuery.parseJSON(responseText);

        var display = '<pre style="background-color:#FFF;width:95%;" id="displayLanguageIdentification">';
        display += '<p id="languageId">';

        var lang = responseJson.lang;
        var conf = responseJson.conf;

        display += "Language: <b>" + lang + "</b> with confidence of <b>" + conf + "</b>";

        display += '</p></pre>\n';

        $('#requestResult').html(display);
    }

    function SubmitSuccesfulSentenceSegmentation(responseJson, statusText) {
        $('#infoResult').html('');
        if ((responseJson == null) || (responseJson.length == 0)) {
            $('#infoResult')
                .html("<font color='red'>Error encountered while receiving the server's answer: " +
                    "response is empty.</font>");
            return;
        }

        var display = '<div class=\"note-tabs\"> \
			<ul id=\"resultTab\" class=\"nav nav-tabs\"> \
		   		<li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Annotations</a></li> \
				<li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
			</ul> \
			<div class="tab-content"> \
			<div class="tab-pane active" id="navbar-fixed-annotation">\n';

        display +=
            '<div style="max-height:150px; overflow:auto;"><table id="sentenceIndex" class="table table-bordered table-condensed">';
        var m = 0;
        var text = $('#input').val();
        for (var sentence in responseJson.sentences) {
            if (m % 2 == 0) {
                display += '<tr class="highlight" id="sent_' + m + '" rank="' + m + '" >';
            }
            else {
                display += '<tr id="sent_' + m + '" rank="' + m + '" >';
            }
            display +=
                '<td style="width:25px;height:13px;font-size:small;">' + m + '</td>'
            var start = responseJson.sentences[sentence].offsetStart;
            var end = responseJson.sentences[sentence].offsetEnd;
            display += '<td style="font-size:small;height:13px;color:#333;">' + text.substring(start, end) + '</td>';
            display += '</tr>';
            m++;
        }
        display += '</table></div>\n';

        display += '</div> \
					<div class="tab-pane " id="navbar-fixed-json">\n';
        // JSON visualisation component
        // with pretty print
        display += "<pre class='prettyprint' id='jsonCode'>";

        display += "<pre class='prettyprint lang-json' id='xmlCode'>";
        var testStr = vkbeautify.json(responseJson);

        display += htmll(testStr);

        display += "</pre>";
        display += '</div></div></div>';

        $('#requestResult').html(display);
        window.prettyPrint && prettyPrint();

        $('#requestResult').html(display);
    }

    function processChange() {
        var selected = $('#selectedService').attr('value');
        if (selected === 'processNERDQuery' || selected === 'processERDQuery') {
            createInputTextArea('query');
            setBaseUrl('disambiguate');
            removeInputFile();
            const cloneQueryTemplateText = JSON.parse(JSON.stringify(queryTemplateText))
            $('#input').attr('value', vkbeautify.json(JSON.stringify(cloneQueryTemplateText)));
            $('#requestResult').html('');
        } else if (selected === 'processNERDQueryPDF') {
            createInputTextArea('query');
            createInputFile();
            setBaseUrl('disambiguate');
            $('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplatePDF)));
            $('#requestResult').html('');
        } else if (selected === 'processLanguage') {
            createInputTextArea('query');
            setBaseUrl('language');
            removeInputFile();
            $('#requestResult').html('');
        } else if (selected === 'processSentenceSegmentation') {
            createInputTextArea('query');
            setBaseUrl('segmentation');
            removeInputFile();
            $('#requestResult').html('');
        } else if (selected === 'KBTermLookup') {
            createSimpleTextFieldArea('query', 'term');
            setBaseUrl('kb/term');
            removeInputFile();
            $('#requestResult').html('');
        } else if (selected === 'KBConcept') {
            createSimpleTextFieldArea('query', 'concept ID');
            setBaseUrl('kb/concept');
            removeInputFile();
            $('#requestResult').html('');
        }
    }

    function createInputFile() {
        // we actually create the input file thingy only if not already created
        if ($('#labelFile').is(':empty')) {
            $('#labelFile').html('Select a pdf file');
            $('#fieldFile').append(
                $('<input/>').attr('type', 'file').attr('id', 'inputFile').attr('name', 'inputFile'));
            $('#gbdForm').attr('enctype', 'multipart/form-data');
            $('#gbdForm').attr('method', 'post');
        }
        $('#labelFile').show();
        $('#fieldFile').show();
    }

    function removeInputFile() {
        // we actually simply hide it...
        $('#labelFile').hide();
        $('#fieldFile').hide();
    }

    // entity-fishing query DSL examples
    const examples = [ "WW1", "PubMed_1", "PubMed_2", "HAL_1", "Italiano", "News_1", "News_2", "French", "German", "Spanish", 
                        "COVID-19", "query_1", "query_2", "query_3", "query_4" ];

    // entity-fishing query DSL and corresponding PDF examples
    const examplesPDF = [ "PMC1636350", "COVID-19-medRxiv", "PMC2808580", "species_filter", "mesh_filter" ];

    function resetExamplesClasses() {
        for (index in examples) {
            $('#example'+index).removeClass('section-active').addClass('section-non-active');
        }

        for (index in examplesPDF) {
            $('#example_pdf'+index).removeClass('section-active').addClass('section-non-active');
        }
    }

    function createSimpleTextFieldArea(nameInput, entryType) {
        $('#label').html('&nbsp;');
        $('#input').remove();
        $('#withExamples').remove();
        $('#field').html("");

        var piece = '<table><tr>' +
            '<td><input type ="text" style="height:28px;" id="input2" name="' + nameInput + '" placeholder="Enter a ' + entryType + '..."/></td>' +
            '<td style="width:10px;"></td>';

        if (entryType === 'concept ID')
            piece += '<td></td>';
        else {
            piece += '<td><select style="height:auto;width:auto;top:-10px;right:10px;" name="lang" id="lang">';
            for (var i = 0; i < supportedLanguages.length; i++) {
                var language = supportedLanguages[i];
                if (language === "en") {
                    piece += '<option value="' + language + '" selected>' + language + '</option>'
                } else {
                    piece += '<option value="' + language + '">' + language + '</option>'
                }
            }
            piece += '</select></td>';
        }
        piece += '</tr></table>';
        $('#field').append(piece);
    }

    function createInputTextArea(nameInput) {
        //$('#label').html('Enter ' + nameInput);
        $('#label').html('&nbsp;');
        $('#input').remove();
        $('#field').html("");

        var selected = $('#selectedService').find('option:selected').attr('value');
        if (selected === 'processNERDQueryPDF') {
            $('#field').append('<table id="withExamples"><tr><td><textarea class="span7" rows="5" id="input" name="' + nameInput + '" /></td>' +
                "<td><span style='padding-left:20px;'>&nbsp;</span></td>" +
                "<td><table id='examplesBlock1'>" +
                "<tr style='line-height:130%;'><td><span id='example_pdf0' style='font-size:90%;'>"+examplesPDF[0]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example_pdf1' style='font-size:90%;'>"+examplesPDF[1]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example_pdf2' style='font-size:90%;'>"+examplesPDF[2]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example_pdf3' style='font-size:90%;'>"+examplesPDF[3]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example_pdf4' style='font-size:90%;'>"+examplesPDF[4]+"</span></td></tr>" +
                "</table></td>" +
                "</tr></table>");

            // binding of the examples
            for (index in examplesPDF) {
                $('#example_pdf'+index).bind('click', function (event) {
                    resetExamplesClasses();
                    var localId = $(this).attr('id');
                    var localIndex = localId.replace("example_pdf", "");                    
                    localIndex = parseInt(localIndex, 10);
                    var selected = $('#selectedService').find('option:selected').attr('value');
                    setJsonExamplePDF(examplesPDF[localIndex]);
                    $(this).removeClass('section-non-active').addClass('section-active');
                });
            }

        } else {
            $('#field').append('<table id="withExamples"><tr><td><textarea class="span7" rows="5" id="input" name="' + nameInput + '" /></td>' +
                "<td><span style='padding-left:20px;'>&nbsp;</span></td>" +
                "<td><table id='examplesBlock1'>" +
                "<tr style='line-height:130%;'><td><span id='example0' style='font-size:90%;'>"+examples[0]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example1' style='font-size:90%;'>"+examples[1]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example2' style='font-size:90%;'>"+examples[2]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example3' style='font-size:90%;'>"+examples[3]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example4' style='font-size:90%;'>"+examples[4]+"</span></td></tr>" +
                "</table></td>" +
                "<td><span style='padding-left:20px;'>&nbsp;</span></td>" +
                "<td><table id='examplesBlock2'>" + 
                "<tr style='line-height:130%;'><td><span id='example5' style='font-size:90%;'>"+examples[5]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example6' style='font-size:90%;'>"+examples[6]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example7' style='font-size:90%;'>"+examples[7]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example8' style='font-size:90%;'>"+examples[8]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example9' style='font-size:90%;'>"+examples[9]+"</span></td></tr>" +
                "</table></td>" +
                "<td><span style='padding-left:20px;'>&nbsp;</span></td>" +
                "<td><table id='examplesBlock3'>" + 
                "<tr style='line-height:130%;'><td><span id='example10' style='font-size:90%;'>"+examples[10]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example11' style='font-size:90%;'>"+examples[11]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example12' style='font-size:90%;'>"+examples[12]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example13' style='font-size:90%;'>"+examples[13]+"</span></td></tr>" +
                "<tr style='line-height:130%;'><td><span id='example14' style='font-size:90%;'>"+examples[14]+"</span></td></tr>" +
                "</table></td>" +
                "</tr></table>");

            // binding of the examples
            for (index in examples) {
                $('#example'+index).bind('click', function (event) {
                    resetExamplesClasses();
                    var localId = $(this).attr('id');
                    var localIndex = localId.replace("example", "");
                    localIndex = parseInt(localIndex, 10);
                    var selected = $('#selectedService').find('option:selected').attr('value');
                    if (selected === 'processNERDQuery' || selected === 'processERDQuery')
                        setJsonExample(examples[localIndex], true);
                    else
                        setJsonExample(examples[localIndex], false);
                    $(this).removeClass('section-non-active').addClass('section-active');
                });
            }
        }
    }

    function setJsonExample(theExample, full) {
        $.ajax({
            'async': true,
            'global': false,
            'url': "resources/query-examples/"+theExample+".json",
            'dataType': "json",
            'success': function(data) {
                if (full)
                    $('#input').attr('value', vkbeautify.json(JSON.stringify(data)));
                else
                    $('#input').attr('value', data['text']);
            }
        });
    }

    function setJsonExamplePDF(theExample) {
        $.ajax({
            'async': true,
            'global': false,
            'url': "resources/pdf-examples/"+theExample+".json",
            'dataType': "json",
            'success': function(data) {
                $('#input').attr('value', vkbeautify.json(JSON.stringify(data)));
            }
        });

        window.open("resources/pdf-examples/"+theExample+".pdf");
    }

    function sortCategories(categories) {
        var newCategories = [];
        if (categories) {
            newCategories = categories.sort(function (a, b) {
                return (b.weight) - (a.weight);
            });
        }
        return newCategories;
    }
   

})(jQuery);