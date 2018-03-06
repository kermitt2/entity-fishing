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

        createInputTextArea('query');
        setBaseUrl('processNERDText');
        $("#selectedService").val('processNERDQuery');
        processChange();

        $('#selectedService').change(function () {
            processChange();
            return true;
        });

        $('#submitRequest').bind('click', submitQuery);

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

    function submitQuery() {
        $('#infoResult').html('<font color="grey">Requesting server...</font>');
        $('#requestResult').html('');

        // re-init the entity map
        entityMap = new Object();
        conceptMap = new Object();
        var urlLocal = $('#gbdForm').attr('action');
        var selected = $('#selectedService').attr('value');

        if ((urlLocal.indexOf('language') !== -1) || (urlLocal.indexOf('segmentation') !== -1)) {
            url = urlLocal + '?text=' + $('#input').val();
            $.ajax({
                type: 'GET',
                url: url,
                success: handleSuccessfulResponse,
                error: displayErrorMessage,
                contentType: false
            });
        } else if ((urlLocal.indexOf('kb/term') !== -1) || (urlLocal.indexOf('kb/concept') !== -1)) {
            $.ajax({
                type: 'GET',
                url: urlLocal + '/' + $('#input2').val().trim() + '?lang=' + $('#lang').val(),
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
                '<div style="max-height:150px; overflow:auto;"><table id="sentenceIndex" class="table table-bordered table-condensed">';
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
                var conf = entity.nerd_score;
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
            var document = (window.content) ? window.content.document : window.document;
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
                    var document = (window.content) ? window.content.document : window.document;
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
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

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
            var conf = entity.nerd_selection_score;
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
                string += "<p><div><table class='statements' style='width:100%;border-color:#fff;border:1px'>" + localHtml + "</table></div></p>";
            }

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
            var conf = entity.nerd_selection_score;
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
                string += "<p><div><table class='statements' style='width:100%;background-color:#fff;border:1px'>" + localHtml + "</table></div></p>";
            }

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

    // query for text XOR shortText content
    var queryTemplateText = {
        "text": "",
        "shortText": "",
        "termVector": [],
        "language": {"lang": "en"},
        "entities": [],
        "mentions": ["ner", "wikipedia"],
        "nbest": false,
        "sentence": false,
        "customisation": "generic"
    };

    // query + PDF
    var queryTemplatePDF = {
        // "language": {"lang": "en"},
        "mentions": ["ner", "wikipedia"],
        "nbest": false,
        "customisation": "generic"
    };

    // term lookup
    var queryTemplate3 = {"term": "", "language": {"lang": "en"}};

    function processChange() {
        var selected = $('#selectedService').attr('value');

        if (selected === 'processNERDQuery') {
            createInputTextArea('query');
            setBaseUrl('disambiguate');
            removeInputFile();
            $('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplateText)));
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
            $('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplate3)));
            $('#requestResult').html('');
        } else if (selected === 'KBConcept') {
            createSimpleTextFieldArea('query', 'concept ID');
            setBaseUrl('kb/concept');
            removeInputFile();
            $('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplate3)));
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

    var textExamples = ["Austria invaded and fought the Serbian army at the Battle of Cer and Battle of Kolubara beginning on 12 August. \n\nThe army, led by general Paul von Hindenburg defeated Russia in a series of battles collectively known as the First Battle of Tannenberg (17 August – 2 September). But the failed Russian invasion, causing the fresh German troops to move to the east, allowed the tactical Allied victory at the First Battle of the Marne. \n\nUnfortunately for the Allies, the pro-German King Constantine I dismissed the pro-Allied government of E. Venizelos before the Allied expeditionary force could arrive. Beginning in 1915, the Italians under Cadorna mounted eleven offensives on the Isonzo front along the Isonzo River, northeast of Trieste.\n\n At the Siege of Maubeuge about 40000 French soldiers surrendered, at the battle of Galicia Russians took about 100-120000 Austrian captives, at the Brusilov Offensive about 325 000 to 417 000 Germans and Austrians surrendered to Russians, at the Battle of Tannenberg 92,000 Russians surrendered.\n\n After marching through Belgium, Luxembourg and the Ardennes, the German Army advanced, in the latter half of August, into northern France where they met both the French army, under Joseph Joffre, and the initial six divisions of the British Expeditionary Force, under Sir John French. A series of engagements known as the Battle of the Frontiers ensued. Key battles included the Battle of Charleroi and the Battle of Mons. In the former battle the French 5th Army was \
almost destroyed by the German 2nd and 3rd Armies and the latter delayed the German advance by a day. A general Allied retreat followed, resulting in more clashes such as the Battle of Le Cateau, the Siege of Maubeuge and the Battle of St. Quentin (Guise). \n\nThe German army came within 70 km (43 mi) of Paris, but at the First Battle of the Marne (6–12 September), French and British troops were able to force a German retreat by exploiting a gap which appeared between the 1st and 2nd Armies, ending the German advance into France. The German army retreated north of the Aisne River and dug in there, establishing the beginnings of a static western front that was to last for the next three years. Following this German setback, the opposing forces tried to outflank each other in the Race for the Sea, and quickly extended their trench systems from the North Sea to the Swiss frontier. The resulting German-occupied territory held 64% of France's pig-iron production, 24% of its steel manufacturing, dealing a serious, but not crippling setback to French industry.\n ",
        "Development and maintenance of leukemia can be partially attributed to alterations in (anti)-apoptotic gene expression. Genome-wide transcriptome analyses revealed that 89 apoptosis-associated genes were differentially expressed between patient acute myeloid leukemia (AML) CD34(+) cells and normal bone marrow (NBM) CD34(+) cells. Among these, transforming growth factor-β activated kinase 1 (TAK1) was strongly upregulated in AML CD34(+) cells. Genetic downmodulation or pharmacologic inhibition of TAK1 activity strongly impaired primary AML cell survival and cobblestone formation in stromal cocultures. TAK1 inhibition was mainly due to blockade of the nuclear factor κB (NF-κB) pathway, as TAK1 inhibition resulted in reduced levels of P-IκBα and p65 activity. Overexpression of a constitutive active variant of NF-κB partially rescued TAK1-depleted cells from apoptosis. Importantly, NBM CD34(+) cells were less sensitive to TAK1 inhibition compared with AML CD34(+) cells. Knockdown of TAK1 also severely impaired leukemia development in vivo and prolonged overall survival in a humanized xenograft mouse model. In conclusion, our results indicate that TAK1 is frequently overexpressed in AML CD34(+) cells, and that TAK1 inhibition efficiently targets leukemic stem/progenitor cells in an NF-κB-dependent manner. ",
        "Cigarette smoke (CS)-induced airway epithelial senescence has been implicated in the pathogenesis of chronic obstructive pulmonary disease (COPD) although the underlying mechanisms remain largely unknown. Growth differentiation factor 15 (GDF15) is increased in airway epithelium of COPD smokers and CS-exposed human airway epithelial cells, but its role in CS-induced airway epithelial senescence is unclear. In this study, we first analyzed expression of GDF15 and cellular senescence markers in airway epithelial cells of current smokers and nonsmokers. Second, we determined the role of GDF15 in CS-induced airway epithelial senescence by using the clustered regularly interspaced short palindromic repeats (CRISPR)/CRISPR associated-9 (Cas9) genome editing approach. Finally, we examined whether exogenous GDF15 protein promoted airway epithelial senescence through the activin receptor-like kinase 1 (ALK1)/Smad1 pathway. GDF15 up-regulation was found in parallel with increased cellular senescence markers p21, p16 and high mobility group box 1 (HMGB1) in airway epithelial cells of current smokers compared with nonsmokers. Moreover, CS extract (CSE) induced cellular senescence in cultured human airway epithelial cells, represented by induced senescence-associated β-galactosidase activity, inhibited cell proliferation, increased p21 expression, and increased release of HMGB1 and IL-6. Disruption of GDF15 significantly inhibited CSE-induced airway epithelial senescence. Lastly, GDF15 protein bound to the ALK1 receptor and promoted airway epithelial senescence via activation of the Smad1 pathway. Our findings highlight an important contribution of GDF15 in promoting airway epithelial senescence upon CS exposure. Senescent airway epithelial cells that chronically accumulate in CS-exposed lungs could contribute substantially to chronic airway inflammation in COPD development and progression.",
        "Mountain glaciers are pertinent indicators of climate change and their dynamics, in particular surface velocity change, is an essential climate variable. In order to retrieve the climatic signature from surface velocity, large-scale study of temporal trends spanning multiple decades is required. Satellite image feature-tracking has been successfully used to derive mountain glacier surface velocities, but most studies rely on manually selected pairs of images, which is not adequate for large datasets. In this paper, we propose a processing strategy to exploit complete satellite archives in a semi-automated way in order to derive robust and spatially complete glacier velocities and their uncertainties on a large spatial scale. In this approach, all available pairs within a defined time span are analysed, preprocessed to improve image quality and features are tracked to produce a velocity stack; the final velocity is obtained by selecting measures from the stack with the statistically higher level of confidence. This approach allows to compute statistical uncertainty level associated with each measured image pixel. This strategy is applied to 1536 pairs of Landsat 5 and 7 images covering the 3000 km long Pamir–Karakoram–Himalaya range for the period of 1999–2001 to produce glacier annual velocity fields. We obtain a velocity estimate for 76,000 km2 or 92% of the glacierized areas of this region. We then discuss the impact of coregistration errors and variability of glacier flow on the final velocity. The median 95% confidence interval ranges from 2.0 m/year on the average in stable areas and 4.4 m/year on the average over glaciers with variability related to data density, surface conditions and strain rate. These performances highlight the benefits of processing of a complete satellite archive to produce glacier velocity fields and to analyse glacier dynamics at regional scales. ",
        "Methane is a powerful greenhouse gas and its concentration in the atmosphere has increased over the past decades. Methane produced by methanogenic Archae can be consumed through aerobic and anaerobic oxidation pathways. In anoxic conditions found in freshwater environments such as meromictic lakes, CH4 oxidation pathways involving different terminal electron acceptors such as NO 3 , SO2 4 , and oxides of Fe and Mn are thermodynamically possible. In this study, a reactive transport model was developed to assess the relative significance of the different pathways of CH4 consumption in the water column of Lake Pavin. In most cases, the model reproduced experimental data collected from the field from June 2006 to June 2007. Although the model and the field measurements suggest that anaerobic CH4 oxidation may contribute to CH4 consumption in the water column of Lake Pavin, aerobic oxidation remains the major sink of CH4 in this lake."];
// The German cruiser Bremen and destroyer V-191 go down on the Russian minefield on 17th December 1915 in the Baltic, 1948 (w/c on paper).
    var reutersExamples = [
        "Mexico: Recovery excitement brings Mexican markets to life.\n\
        Henry Tricks\n\
        Mexico City\n\
        Emerging evidence that Mexico's economy was back on the recovery track sent Mexican markets into a buzz of excitement Tuesday, with stocks closing at record highs and interest rates at 19-month lows.\n\
        \"Mexico has been trying to stage a recovery since the beginning of this year and it's always been getting ahead of itself in terms of fundamentals,\" said Matthew Hickman of Lehman Brothers in New York.\n\
        \"Now we're at the point where the fundamentals are with us. The history is now falling out of view.\"\n\
        That history is one etched into the minds of all investors in Mexico: an economy in crisis since December 1994, a free-falling peso and stubbornly high interest rates.\n\
        This week, however, second-quarter gross domestic product was reported up 7.2 percent, much stronger than most analysts had expected. Interest rates on governent Treasury bills, or Cetes, in the secondary market fell on Tuesday to 23.90 percent, their lowest level since Jan. 25, 1995.\n\
        The stock market's main price index rallied 77.12 points, or 2.32 percent, to a record 3,401.79 points, with volume at a frenzied 159.89 million shares.\n\
        Confounding all expectations has been the strength of the peso, which ended higher in its longer-term contracts on Tuesday despite the secondary Cetes drop and expectations of lower benchmark rates in Tuesday's weekly auction.\n\
        With U.S. long-term interest rates expected to remain steady after the Federal Reserve refrained from raising short-term rates on Tuesday, the attraction of Mexico, analysts say, is that it offers robust returns for foreigners and growing confidence that they will not fall victim to a crumbling peso.\n\
        \"The focus is back on Mexican fundamentals,\" said Lars Schonander, head of researcher at Santander in Mexico City. \"You have a continuing decline in inflation, a stronger-than-expected GDP growth figure and the lack of any upward move in U.S. rates.\"\n\
        Other factors were also at play, said Felix Boni, head of research at James Capel in Mexico City, such as positive technicals and economic uncertainty in Argentina, which has put it and neighbouring Brazil's markets at risk.\n\
        \"There's a movement out of South American markets into Mexico,\" he said. But Boni was also wary of what he said could be \"a lot of hype.\"\n\
        The economic recovery was still export-led, and evidence was patchy that the domestic consumer was back with a vengeance. Also, corporate earnings need to grow strongly to justify the run-up in the stock market, he said.\n\
        \n\
        (c) Reuters Limited 1996",
        "USA: Sept U.S. layoffs up from Aug but still low - report.\n\
        Sept U.S. layoffs up from Aug but still low - report.\n\
        NEW YORK 1996-10-07\n\
        U.S. layoffs rose last month, but spot labor shortages and fewer merger-related layoffs helped make September the second-lowest job-cuts month so far in 1996, said a report by Challenger, Gray & Christmas Inc.\n\
        Announced layoffs in September totaled 29,632, up 46 percent from August's 20,309 but down 10.7 percent from 33,173 in September 1995, the report said.\n\
        The August total was the lowest for any month since April 1995, it said.\n\
        \"For the past two months, spot labor shortages in some industries have helped to suppress the number of job cuts overall,\" said John Challenger, executive vice president at the international outplacement firm.\n\
        A decline in merger-related job cuts helped contain the overall number of layoffs as well, the report said.\n\
        In May through September 1996, six percent of total employee discharges were attributed to mergers, compared with 22 percent during the same period of 1995, it said.\n\
        Companies continue to trim their payrolls in order to keep wages under control, Challenger said.\n\
        \"Management views downsizing as a valve,\" he said. \"When the number of employees reaches a level that pushes against the wage ceiling, management turns the valve, discharging employees and relieving wage pressure.\"\n\
        The report said that for the first nine months of 1996, 8.3 percent of job cuts were merger-driven, compared with 13.6 percent a year earlier.\n\
        In the first nine months of 1996, 362,297 layoffs were announced, against 302,017 in the first nine months of 1995, the report said.\n\
        -- N.A. Treasury 212-859-1660\n\
        (c) Reuters Limited 1996",
        "Edmond Ludlow (vers 1617-1692) est un parlementaire anglais, plus connu pour son implication dans l'exécution de Charles Ier, et pour ses mémoires, publiés à titre posthume et qui sont devenus une source importante pour les historiens des Guerres des Trois Royaumes. Après avoir servi dans les guerres civiles anglaises, Ludlow a été élu membre du Long Parlement. Après la création du Commonwealth en 1649, il est nommé adjoint de Ireton, commandant des forces du Parlement en Irlande, avant de rompre avec Oliver Cromwell lors de la création du Protectorat. Après la Restauration, Ludlow part en exil en Suisse, où il passe une grande partie du reste de sa vie.",
        //"Pierre-Édouard Blondin (14 décembre 1874-29 octobre 1943) fut un avocat, notaire et homme politique fédéral du Québec. Né à Saint-François-du-Lac dans la région du Centre-du-Québec, M. Blondin devint député du Parti conservateur dans la circonscription fédérale de Champlain en 1908. Réélu en 1911 et lors de l'élection partielle de 1914, il fut défait dans Laurier—Outremont par le libéral Pamphile Du Tremblay en 1917. Cette défaite fut causée par l'impopularité de la mesure de conscription au Québec. En 1918, le premier ministre Robert Laird Borden le nomma au poste de sénateur de la division des Laurentides. De 1930 à 1936, il servit en tant que président du Sénat. Il mourut en fonction en 1943. Durant son passage à la Chambre des communes, il fut ministre du Revenu intérieur de 1914 à 1915, Secrétaire d'État du Canada de 1915 à 1917, ministre des Mines de 1915 à 1917 et ministre des Postes de 1917 à 1921.",
        //"Charlemagne, du latin Carolus Magnus, ou Charles Ier dit « le Grand », né le 2 avril 742 (voire 747 ou 748)2, mort le 28 janvier 814 à Aix-la-Chapelle, est un roi des Francs et empereur. Il appartient à la dynastie des Carolingiens, à laquelle il a donné son nom.\nFils de Pépin le Bref, il est roi des Francs à partir de 768, devient par conquête roi des Lombards en 774 et est couronné empereur à Rome par le pape Léon III le 25 décembre 800, relevant une dignité disparue depuis la chute de l'Empire romain d'Occident en 476.\nRoi guerrier, il agrandit notablement son royaume par une série de campagnes militaires, en particulier contre les Saxons païens dont la soumission fut difficile et violente (772-804), mais aussi contre les Lombards en Italie et les musulmans d'Al-Andalus.",
        "Goethes literarisches Werk umfasst Lyrik, Dramen, Epik, autobiografische, kunst- und literaturtheoretische sowie naturwissenschaftliche Schriften. Daneben ist sein umfangreicher Briefwechsel von literarischer Bedeutung. Goethe war Vorbereiter und wichtigster Vertreter des Sturm und Drang. Sein Roman Die Leiden des jungen Werthers machte ihn in Europa berühmt. Selbst Napoleon bat ihn zu einer Audienz anlässlich des Erfurter Fürstenkongresses. Im Bunde mit Schiller und gemeinsam mit Herder und Wieland verkörperte er die Weimarer Klassik. Die Wilhelm-Meister-Romane wurden zu beispielgebenden Vorläufern deutschsprachiger Künstler- und Bildungsromane. Sein Faust errang den Ruf als die bedeutendste Schöpfung der deutschsprachigen Literatur. Im Alter wurde er auch im Ausland als Repräsentant des geistigen Deutschland angesehen."
    ];

    var spanishExamples = [
        "La Exposición Internacional de Barcelona tuvo lugar del 20 de mayo de 1929 al 15 de enero de 1930 en Barcelona (España). Celebrada en la montaña de Montjuic, se desarrolló en una superficie de 118 hectáreas, y tuvo un coste de 130 millones de pesetas. Entre la veintena europea de naciones que oficialmente participaron estaban países como Alemania, Bélgica, Dinamarca, Francia, Hungría, Italia, Noruega, Rumanía o Suiza. También participaron expositores privados japoneses y estadounidenses.\n" +
        "En Barcelona se guardaba un grato recuerdo de la Exposición Universal de 1888, que supuso un gran avance para la ciudad en el terreno económico y tecnológico, así como la remodelación del Parque de la Ciudadela. Por eso se proyectó esta nueva exposición para dar a conocer los nuevos adelantos tecnológicos y proyectar la imagen de la industria catalana en el exterior. De nuevo, la exposición originó una remodelación de una parte de la ciudad, en este caso la montaña de Montjuic, así como sus zonas colindantes, especialmente la plaza de España.\n" +
        "La Exposición supuso un gran desarrollo urbanístico para Barcelona, así como un banco de pruebas para los nuevos estilos arquitectónicos gestados a principios del siglo xx: a nivel local, representó la consolidación del novecentismo, estilo de corte clásico que sustituyó al modernismo preponderante en Cataluña durante la transición de siglo; además, supuso la introducción en España de las corrientes de vanguardia internacionales, especialmente el racionalismo, a través del Pabellón de Alemania de Ludwig Mies van der Rohe. La Exposición dejó numerosos edificios e instalaciones algunos de los cuales se han convertido en emblemas de la ciudad, como el Palacio Nacional, la Fuente Mágica, el Teatre Grec, el Pueblo Español y el Estadio Olímpico."


    ];

    var italianExamples = [
        "Rondò Veneziano è un ensemble musicale italiano e svizzero (il marchio è stato ceduto alla svizzera Cleo Music AG nel 1990) che si ispira alla musica barocca sposando sonorità attuali della musica pop e del rock. La genesi di questo progetto musicale risale al 1979, partendo da un'idea condivisa di Freddy Naggiar, discografico della Baby Records e Gian Piero Reverberi, noto compositore e direttore d'orchestra della scuola genovese.\n" +
        "Il vasto repertorio comprende composizioni ispirate a modelli stilistici di Vivaldi, Albinoni, Boccherini, del tardobarocco europeo e porta la firma dello stesso Reverberi, affermato arrangiatore già noto per aver collaborato con i maggiori artisti italiani tra cui Lucio Battisti, Lucio Dalla, Fabrizio De André, Bruno Lauzi, Mina, Gino Paoli, Eros Ramazzotti, Luigi Tenco e Ornella Vanoni.\n" +
        "Tuttavia, non tutti i brani dei Rondò Veneziano sono riconducibili a dei modelli settecenteschi. Il loro repertorio comprende anche composizioni che si orientano verso esempi di altre epoche della storia della musica; altre composizioni riproducono invece strutture proprie della musica leggera o del folk, limitando il collegamento con il passato alla sola sonorità orchestrale.\n" +
        "Ritiratosi quasi del tutto dalle scene italiane, il gruppo continua la propria attività con tournée europee, riscuotendo un particolare successo prevalentemente nell'area di lingua tedesca come Austria, Germania, Svizzera e Lussemburgo. Complessivamente i Rondò Veneziano hanno venduto oltre 25 milioni di dischi in tutto il mondo (aggiornato a 30 milioni nel 2017) e hanno inoltre ricevuto 15 dischi d'oro e 11 dischi di platino in Europa. "


    ];

    function resetExamplesClasses() {
        $('#example1').removeClass('section-active').addClass('section-non-active');
        $('#example2').removeClass('section-active').addClass('section-non-active');
        $('#example3').removeClass('section-active').addClass('section-non-active');
        $('#example4').removeClass('section-active').addClass('section-non-active');
        $('#spanish').removeClass('section-active').addClass('section-non-active');
        $('#reuters1').removeClass('section-active').addClass('section-non-active');
        $('#reuters2').removeClass('section-active').addClass('section-non-active');
        $('#reuters3').removeClass('section-active').addClass('section-non-active');
        $('#reuters4').removeClass('section-active').addClass('section-non-active');
        $('#italian').removeClass('section-active').addClass('section-non-active');
    }

    function createSimpleTextFieldArea(nameInput, entryType) {
        $('#label').html('&nbsp;');
        $('#input').remove();
        $('#withExamples').remove();
        $('#field').html("");

        var piece = '<table><tr>' +
            '<td><textarea style="height:28px;" rows="1" id="input2" name="' + nameInput + '" placeholder="Enter a ' + entryType + '..."/></td>' +
            '<td style="width:10px;"></td>' +
            '<td><select style="height:auto;width:auto;top:-10px;right:10px;" name="lang" id="lang">';

        for (var i = 0; i < supportedLanguages.length; i++) {
            var language = supportedLanguages[i];
            if (language === "en") {
                piece += '<option value="' + language + '" selected>' + language + '</option>'
            } else {
                piece += '<option value="' + language + '">' + language + '</option>'
            }
        }


        piece += '</select></td>' +
            '</tr></table>';
        $('#field').append(piece);
    }

    function loadExample(text, lang) {
        resetExamplesClasses();
        $(this).addClass('section-active').removeClass('section-non-active');
        var selected = $('#selectedService').find('option:selected').attr('value');
        if (selected === 'processNERDQuery' || selected === 'processERDQuery') {
            var queryInstance = queryTemplateText;
            queryInstance.language.lang = lang;
            queryInstance.text = text;
            $('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
        }
        else
            $('#input').attr('value', text);
    }

    function createInputTextArea(nameInput) {
        //$('#label').html('Enter ' + nameInput);
        $('#label').html('&nbsp;');
        $('#input').remove();
        $('#field').html("");

        $('#field').append('<table id="withExamples"><tr><td><textarea class="span7" rows="5" id="input" name="' + nameInput + '" /></td>' +
            "<td><span style='padding-left:20px;'>&nbsp;</span></td>" +
            "<td><table id='examplesBlock1'><tr style='line-height:130%;'><td><span id='example1' style='font-size:90%;'>WW1</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='example2' style='font-size:90%;'>PubMed_1</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='example3' style='font-size:90%;'>PubMed_2</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='example4' style='font-size:90%;'>HAL_1</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='italian' style='font-size:90%;'>Italiano_1</span></td></tr>" +
            "</table></td>" +
            "<td><span style='padding-left:20px;'>&nbsp;</span></td>" +
            "<td><table id='examplesBlock2'><tr style='line-height:130%;'><td><span id='reuters1' style='font-size:90%;'>Reuters_1</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='reuters2' style='font-size:90%;'>Reuters_2</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='reuters3' style='font-size:90%;'>French_1</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='reuters4' style='font-size:90%;'>German_1</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='spanish' style='font-size:90%;'>Spanish_1</span></td></tr>" +
            "</table></td>" +
            "</tr></table>");

        // binding of the examples
        $('#example1').popover();
        $('#example1').bind('click', function (event) {
            loadExample(textExamples[0], "en");
        });

        $('#example2').popover();
        $('#example2').bind('click', function (event) {
            loadExample(textExamples[1], "en")
        });

        $('#example3').popover();
        $('#example3').bind('click', function (event) {
            loadExample(textExamples[2], "en");
        });

        $('#example4').popover();
        $('#example4').bind('click', function (event) {
            loadExample(textExamples[3], "en");
        });

        $('#reuters1').bind('click',function (event) {
            loadExample(reutersExamples[0], "en");
        });

        $('#reuters2').bind('click',function (event) {
            loadExample(reutersExamples[1], "en");
        });

        $('#reuters3').bind('click', function (event) {
            loadExample(reutersExamples[2], "fr");
        });

        $('#reuters4').bind('click', function (event) {
            loadExample(reutersExamples[3], "de");
        });

        $('#spanish').bind('click', function (event) {
            loadExample(spanishExamples[0], "es");
        });

        $('#italian').bind('click', function (event) {
            loadExample(italianExamples[0], "it");
        });

        //$('#gbdForm').attr('enctype', '');
        //$('#gbdForm').attr('method', 'post');

        $('#field2').append('<table><tr><td><textarea class="span7" rows="5" id="input2" name="' + nameInput + '" /></td>' +
            "<td><span style='padding-left:20px;'>&nbsp;</span></td><td><table>" +
            "<tr style='line-height:130%;'><td><span id='example21' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 1' data-content='" + textExamples[0] + "' data-trigger='hover'>example 1</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='example22' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 2' data-content='" + textExamples[1] + "' data-trigger='hover'>example 2</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='example23' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 3' data-content='" + textExamples[2] + "' data-trigger='hover'>example 3</span></td></tr>" +
            "<tr style='line-height:130%;'><td><span id='example24' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 4' data-content='" + textExamples[3] + "' data-trigger='hover'>example 4</span></td></tr>" +

            "</table></td></tr></table>");
        // binding of the examples
        $('#example21').popover();
        $('#example21').bind('click',
            function (event) {
                $(this).addClass('section-active').removeClass('section-non-active');
                $('#example22').removeClass('section-active').addClass('section-non-active');
                $('#example23').removeClass('section-active').addClass('section-non-active');
                $('#example24').removeClass('section-active').addClass('section-non-active');
                $('#input2').attr('value', textExamples[0]);
            });
        $('#example22').popover();
        $('#example22').bind('click',
            function (event) {
                $(this).addClass('section-active').removeClass('section-non-active');
                $('#example21').removeClass('section-active').addClass('section-non-active');
                $('#example23').removeClass('section-active').addClass('section-non-active');
                $('#example24').removeClass('section-active').addClass('section-non-active');
                $('#input2').attr('value', textExamples[1]);
            });
        $('#example23').popover();
        $('#example23').bind('click',
            function (event) {
                $(this).addClass('section-active').removeClass('section-non-active');
                $('#example21').removeClass('section-active').addClass('section-non-active');
                $('#example22').removeClass('section-active').addClass('section-non-active');
                $('#example24').removeClass('section-active').addClass('section-non-active');
                $('#input2').attr('value', textExamples[2]);
            });
        $('#example24').popover();
        $('#example24').bind('click',
            function (event) {
                $(this).addClass('section-active').removeClass('section-non-active');
                $('#example21').removeClass('section-active').addClass('section-non-active');
                $('#example22').removeClass('section-active').addClass('section-non-active');
                $('#example23').removeClass('section-active').addClass('section-non-active');
                $('#input2').attr('value', textExamples[3]);
            });

        $('#gbdForm2').attr('enctype', '');
        $('#gbdForm2').attr('method', 'post');
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