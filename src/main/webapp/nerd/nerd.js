/**
*  Javascript functions for the front end.
*        
*  Author: Patrice Lopez
*/

var nerd = (function($) {
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
		if ( $(location).attr('href').indexOf("index.html") != -1)
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

	$(document).ready(function() {   
		// components / detailed / simple

		$("#subTitle").html("About");
		$("#divAbout").show();
		$("#divServices").hide();
		$("#divDoc").hide(); 
		$("#divAdmin").hide(); 
		$("#nerd-text").show();
		$("#nerd-query").hide();

		createInputTextArea('query');
		setBaseUrl('processNERDText');  
		$("#selectedService").val('processNERDQuery');
		processChange();

		$('#selectedService').change(function() {
			processChange();
			return true;
		});

		$('#submitRequest').bind('click', submitQuery);

		$('#adminForm').attr("action",$(location).attr('href')+"allProperties");
		$('#TabAdminProps').hide();
		$('#adminForm').ajaxForm({
	        beforeSubmit: adminShowRequest,
	        success: adminSubmitSuccesful,
	        error: adminAjaxError,
	        dataType: "text"
	        });

		$("#about").click(function() {
			$("#about").attr('class', 'section-active');
			$("#services").attr('class', 'section-non-active');
			$("#doc").attr('class', 'section-non-active');
			$("#demo").attr('class', 'section-non-active');

			$("#subTitle").html("About"); 
			//$("#subTitle2").hide();
			$("#subTitle").show();

			$("#divAbout").show();
			$("#divServices").hide();
			//$("#divDemo").hide();
			$("#divAdmin").hide();
			$("#divDoc").hide();

			$("#nerd-text").hide();

			return false;
		});
		$("#services").click(function() {
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
			$("#divAdmin").hide();

			return false;
		});
		$("#doc").click(function() {
			$("#doc").attr('class', 'section-active');
			$("#services").attr('class', 'section-non-active');
			$("#about").attr('class', 'section-non-active');
			$("#demo").attr('class', 'section-non-active');

			$("#subTitle").html("Doc"); 
			//$("#subTitle2").hide(); 
			$("#subTitle").show();

			$("#divDoc").show();
			$("#divAbout").hide();
			$("#divServices").hide();
			$("#divAdmin").hide();

			return false;
		});
		$("#admin").click(function() {
			$("#admin").attr('class', 'section-active');
			$("#doc").attr('class', 'section-non-active');
			$("#services").attr('class', 'section-non-active');
			$("#about").attr('class', 'section-non-active');
			$("#demo").attr('class', 'section-non-active');

			$("#subTitle").html("Admin"); 
			//$("#subTitle2").hide(); 
			$("#subTitle").show();        

			$("#divDoc").hide();
			$("#divAbout").hide();
			$("#divServices").hide();
			$("#divAdmin").show();

			return false;
		});

		// extend customisation field with the registered existing ones
		$.ajax({
		  type: 'GET',
		  url: 'service/NERDCustomisations',
//		  data: { text : $('#input').val() },
		  success: fillCustumisationField,
		  error: AjaxError,
		  contentType:false  
		});
	});

	function ShowRequest(formData, jqForm, options) {
	    var queryString = $.param(formData);
	    $('#infoResult').html('<font color="grey">Requesting server...</font>');
	    return true;
	}

	function fillCustumisationField(response, statusText) { 
		if (response) {
			for(var ind in response) {
				var option = '<option value=\"'+response[ind]+'\">'+response[ind]+'</option>';	
				$('#customisation').append(option);
			}
		}
	}

	function submitQuery() {
		$('#infoResult').html('<font color="grey">Requesting server...</font>');
        $('#requestResult').html('');

		// reinit the entity map
		entityMap = new Object();
		conceptMap = new Object();
		var urlLocal = $('#gbdForm').attr('action');
		var selected = $('#selectedService').attr('value');

		//console.log(JSON.stringify($('#textInputArea').val()));
		if ( (urlLocal.indexOf('LId') != -1) || (urlLocal.indexOf('SentenceSegmentation') != -1) ) { 
			$.ajax({
			  type: 'GET',
			  url: urlLocal,
			  data: { text : $('#input').val() },
//			  processData: false,
			  success: SubmitSuccesful,
			  error: AjaxError,
			  contentType:false  
			});
		}
		else if ( urlLocal.indexOf('KBTermLookup') != -1 ) { 
			$.ajax({
			  type: 'GET',
			  url: urlLocal,
			  data: { term : $('#input2').val().trim(), 
					  lang : $('#lang').val() },
//			  processData: false,
			  success: SubmitSuccesful,
			  error: AjaxError,
			  contentType:false  
			//contentType: "multipart/form-data"
			});
		}
		else if ( urlLocal.indexOf('KBConcept') != -1 ) { 
			$.ajax({
			  type: 'GET',
			  url: urlLocal,
			  data: { id : $('#input2').val().trim(), 
					  lang : $('#lang').val() },
//			  processData: false,
			  success: SubmitSuccesful,
			  error: AjaxError,
			  contentType:false  
			//contentType: "multipart/form-data"
			});
		}
		else if (selected.indexOf('PDF') != -1 ) {
console.log(document.getElementById("inputFile").files[0].name);
			if ((document.getElementById("inputFile").files[0].type == 'application/pdf') ||
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
	                            annot.setAttribute('id', 'detailed_annot-' + (page.pageIndex+1));
	                            td2.setAttribute('style', 'vertical-align:top;width:30%;');
	                            td2.appendChild(annot);

	                            container.appendChild(table);
	                            
	                            //fitToContainer(canvas);

	                            // we could think about a dynamic way to set the scale based on the available parent width
	                            //var scale = 1.2;
	                            //var viewport = page.getViewport(scale);
	                            var viewport = page.getViewport((td1.offsetWidth*0.98) / page.getViewport(1.0).width);

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
	            }
	            reader.readAsArrayBuffer(document.getElementById("inputFile").files[0]);

	            xhr.onreadystatechange = function (e) {
	                if (xhr.readyState == 4 && xhr.status == 200) {
	                    var response = e.target.response;
	                    //var response = JSON.parse(xhr.responseText);
	                    //console.log(response);
	                    setupAnnotations(response);
	                } else if (xhr.status != 200) {
	                    AjaxError2("Response " + xhr.status + ": ");
	                }
	            };
	            xhr.send(formData);
			}
		}
		else {
			var formData = new FormData();
			formData.append("query", $('#input').val());
			$.ajax({
			  type: 'POST',
			  url: urlLocal,
			  data: formData,
		 	  //data: JSON.stringify($('#textInputArea').val()),
			  //beforeSubmit: ShowRequest2,
			  contentType: false,
			  processData: false,
			  success: SubmitSuccesful,
			  error: AjaxError
			});
		}
		
		$('#infoResult').html('<font color="grey">Requesting server...</font>');
	}

	function AjaxError() {
		$('#infoResult').html("<font color='red'>Error encountered while requesting the server.</font>");      
		responseJson = null;
	}

	//function SubmitSuccesful(responseJson, statusText) { 
	function SubmitSuccesful(responseText, statusText, xhr) {	
		responseJson = responseText;
		var selected = $('#selectedService').attr('value');
		if ( (selected == 'processNERDQuery') && (responseJson.text != null) && (responseJson.text.length > 0) ) {
			SubmitSuccesfulNERD(responseJson, statusText);          
		}
		else if (selected == 'processLIdText') {
			SubmitSuccesfulLId(responseJson, statusText);
		}
		else if (selected == 'processSentenceSegmentation') {
			SubmitSuccesfulSentenceSegmentation(responseJson, statusText);
		}
		else if ( (selected == 'processNERDQuery') && (responseJson.shortText != null) && (responseJson.shortText.length > 0) ) {
			SubmitSuccesfulERDSearch(responseJson, statusText);           
		}
		else if ( (selected == 'processNERDQuery') && (responseJson.termVector != null) && (responseJson.termVector.length > 0) ) {
			console.log("front end for term vector disambiguation not implemented yet !");
		}
		else if (selected == 'KBTermLookup') {
			SubmitSuccesfulKBTermLookup(responseJson, statusText);           
		}
		else if (selected == 'KBConcept') {
			SubmitSuccesfulKBConceptLookup(responseJson, statusText);           
		}
		
	}

	function htmll(s) {
    	return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  	}

	function SubmitSuccesfulNERD(responseJson, statusText) {     
		$('#infoResult').html('');
		//console.log(responseText);
		
		if ( (responseJson == null) || (responseJson.length == 0) ) {
			$('#infoResult')
				.html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");   
			return;
		}

		var display = '<div class=\"note-tabs\"> \
			<ul id=\"resultTab\" class=\"nav nav-tabs\"> \
		   		<li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Annotations</a></li>\n';
		if (responseJson.global_categories)
		   display += '<li><a href=\"#navbar-fixed-categories\" data-toggle=\"tab\">Categories</a></li>\n';
		display += '<li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
			</ul> \
			<div class="tab-content"> \
			<div class="tab-pane active" id="navbar-fixed-annotation">\n';   	
		 
		var nbest = false; 
		if (responseJson.nbest == true)
			nbest = true;
		 
		if (responseJson.sentences) {
			display += 
				'<div style="max-height:150px; overflow:auto;"><table id="sentenceIndex" class="table table-bordered table-condensed">';  
			var m = 0;
			var text = responseJson.text;
			for(var sentence in responseJson.sentences) {    
				if (m == 0) { 	
	  				display += '<tr class="highlight" id="sent_'+m+'" rank="'+m+'" >'; 
				}   
				else {
					display += '<tr id="sent_'+m+'" rank="'+m+'" >';     
				}
				display += 
				  '<td style="width:25px;height:13px;font-size:small;">'+m+'</td>'  
				var start = responseJson.sentences[sentence].offsetStart;
				var end = responseJson.sentences[sentence].offsetEnd;
				display += '<td style="font-size:small;height:13px;color:#333;">'+text.substring(start,end)+'</td>';
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
		{    
			display += '<table id="sentenceNER" style="width:100%;table-layout:fixed;" class="table">'; 
			var string = responseJson.text;
			if (!responseJson.sentences || (responseJson.sentences.length == 0)) {
				display += '<tr style="background-color:#FFF;">';     
				var lang = 'en'; //default
		 		var language = responseJson.language;
		 		if (language)
		 			lang = language.lang;
				if (responseJson.entities) {
					var currentAnnotationIndex = responseJson.entities.length-1;
					for(var m=responseJson.entities.length-1; m>=0; m--) {
						var entity = responseJson.entities[m];
						var identifier = entity.wikipediaExternalRef;
						//var language = entity.lang;
						if (identifier && (conceptMap[identifier] == null)) {
							$.ajax({
							  	type: 'GET',
							  	url: 'service/KBConcept',
							  	data: { id : identifier, lang : lang },
							  	success: function(result) { conceptMap[result.wikipediaExternalRef] = result; },
							  	dataType: 'json'  
							});
						}
						var domains = entity.domains;
						var label = null;
						if (entity.type)
							label = entity.type;
						else if (domains && domains.length>0) {
							label = domains[0].toLowerCase();
						}
						else 
							label = entity.rawName;

				    	var start = parseInt(entity.offsetStart,10);
					    var end = parseInt(entity.offsetEnd,10);       
						
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
							string = string.substring(0,start) 
								+ '<span id="annot-'+m+'" rel="popover" data-color="'+label+'">'
								+ '<span class="label ' + label + '" style="cursor:hand;cursor:pointer;" >'
								+ string.substring(start,end) + '</span></span>' + string.substring(end,string.length+1); 
							lastMaxIndex = start;
							currentAnnotationIndex = m;
							entityMap[currentAnnotationIndex] = [];
							entityMap[currentAnnotationIndex].push(responseJson.entities[m]);
						}						
				    } 
				}
//console.log(entityMap);
				string = "<p>" + string.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
			
				display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>'+string+'</p></td>';
				display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';	

				display += '</tr>';
			}
			else {
				display += '<tr style="background-color:#FFF;">';  
					
				display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p><span id="sentence_ner">'+
					" "+'</span></p></td>';
				display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';	
				display += '</tr>';
			}
			
			display += '</table>\n';
		}
		
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
			for(var category in categories) {   
				var theCategory = responseJson.global_categories[category].category;

				var score = categories[category].weight;
				var pageId = categories[category].page_id;
				var source = categories[category].source;

				var lang = 'en';
				if (source) {
					var ind = source.indexOf('-');
					if (ind != -1) {
						lang = source.substring(ind+1,source.length);
					}
				}

				if (score && (score.toString().length > 6)) {
					score = score.toString().substring(0, 6);
				}
				if (score == '0.0000')
					continue;
				display += '<tr><td><span style="color:#7F2121;">'+theCategory+'</span></td><td>' + 
				'<a href="http://'+lang+'.wikipedia.org/wiki?curid=' + pageId + 
		'" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/wikipedia.png"/></a></td><td>'+score+'</td>';
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
			for(var p=0;p<nbSentences;p++) {
				//$('#sent'+p).bind('click',viewSentenceResults());      
				$('#sentenceIndex').on('click', 'tbody tr', function(event) {
				    $(this).addClass('highlight').siblings().removeClass('highlight');       
					viewSentenceResults($(this).attr('rank'));
				});
			} 
			viewSentenceResults('0');
		}		
	
		for (var key in entityMap) {
		  	if (entityMap.hasOwnProperty(key)) {
				$('#annot-'+key).bind('hover', viewEntity);  
				$('#annot-'+key).bind('click', viewEntity);  	
		  	}
		}
		$('#detailed_annot-0').hide();	
	}
	
	function SubmitSuccesfulERDSearch(responseJson, statusText) {
		$('#infoResult').html('');   
		if ( (responseJson == null) || (responseJson.length == 0) ) {
			$('#infoResult')
				.html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");   
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
				$.ajax({
				  	type: 'GET',
				  	url: 'service/KBConcept',
				  	data: { id : identifier, lang : lang },
				  	success: function(result) { 
				  		var localIdentifier = result.wikipediaExternalRef;
				  		conceptMap[localIdentifier] = result; 
				  		var definitions = result.definitions;
				  		var preferredTerm = result.preferredTerm;
				  		var localHtml = "";
				  		if (definitions && (definitions.length > 0)) {
							localHtml = wiki2html(definitions[0]['definition'], lang);
							$("#def-"+localIdentifier).html(localHtml);
				  		}
				  		if (preferredTerm)
					  		$("#pref-"+localIdentifier).html(preferredTerm);
				  	},
				  	dataType: 'json'  
				});
			}
		}
	}

	var SubmitSuccesfulKBTermLookup = function(responseText, statusText) {
		$('#infoResult').html('');   
		if ( (responseJson == null) || (responseJson.length == 0) ) {
			$('#infoResult')
				.html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");   
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
		<div>Number of ambiguous concepts: '+responseJson.senses.length+'</div> \
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
				$.ajax({
				  	type: 'GET',
				  	url: 'service/KBConcept',
				  	data: { id : identifier, lang : lang },
				  	success: function(result) { 
				  		var localIdentifier = result.wikipediaExternalRef;
				  		conceptMap[localIdentifier] = result; 
				  		var definitions = result.definitions;
				  		var localHtml = "";
				  		if (definitions && (definitions.length > 0)) {
							localHtml = wiki2html(definitions[0]['definition'], lang);
							$("#def-"+localIdentifier).html(localHtml);
				  		}
				  	},
				  	dataType: 'json'  
				});
			}
		}
	}

	var SubmitSuccesfulKBConceptLookup = function(responseText, statusText) {
		$('#infoResult').html('');   
		if ( (responseJson == null) || (responseJson.length == 0) ) {
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
	}

	var getPieceShowConcept = function(entity, lang) {
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

        var piece = '<tr><td>';
        // we ouput here all fields
      
        // term in target language
        piece += '<table class="concept" style="width:100%;">';
		piece += '<tr><td style="width:100%;border-top:0px;"><table><tr><td style="padding-left:0px;border-top:0px;"><p><b>' + 
				entity.preferredTerm + ' (' + lang + ')</b></p></td>';

		// multilingual terms
		if (entity.multilingual) {
			for(var mult in entity.multilingual) {
				piece += '<td style="border-top:0px;"><p>' + entity.multilingual[mult].term + ' (' + entity.multilingual[mult].lang + ')</p></td>';
			}
		}
		piece += '</tr></table></td></tr>';

		// definition
		var definitions = entity.definitions;
		var localHtml = "";
		if (definitions && (definitions.length > 0))
			localHtml += wiki2html(definitions[0]['definition'], lang);       
        piece += '<tr><td><div class="wiky_preview_area2">'+ localHtml + '</div></td></tr>';

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

        // properties
        var properties = entity.properties;
		if ((properties != null) && (properties.length > 0)) {
			localHtml = "";
			for(var i in properties) {
				var property = properties[i];
				if (property.template) {
					localHtml += "<tr><td>" + property.attribute + "</td><td>" + property.value + "</td></tr>"
				}
			}
			piece += "<tr><td><table><tr><td style='padding:0px;border-top:0px;'><p><b>Properties: </b></p></td><td style='border-top:0px;'><div><table class='properties' style='width:100%;background-color:#fff;border:0px'>"+
						localHtml+"</table></div></td></tr></table></td></tr>";
		}

		// relations
		var relations = entity.relations;
		if ((relations != null) && (relations.length > 0)) {
			localHtml = "";
			for(var i in relations) {
				var relation = relations[i];
				if (relation.template) {
					localHtml += "<tr><td>" + relation.relationName + "</td><td>" + relation.target + "</td></tr>"
				}
			}
			piece += "<tr><td><table><tr><td style='padding:0px;border-top:0px;'><p><b>Relations: </b></p></td><td style='border-top:0px;'><div><table class='properties' style='width:100%;background-color:#fff;border:0px'>"+
						localHtml+"</table></div></td></tr></table></td></tr>";
		}

		piece += '</table>';

		// wikimedia image
		piece += '</td><td>';
		piece += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("'+wikipedia+'", "'+lang+'")</script></span>';
        
        // clickable wikipedia icon
        piece += '<td>';
        if (wikipedia) {
            piece += '<a href="http://en.wikipedia.org/wiki?curid=' +
                    wikipedia +
                    '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/wikipedia.png"/></a>';
        }
        piece += '</td></tr>';
	    
	    return piece;
	}

	var getPieceShowexpandNERD = function(jsonObject, lang) {
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
					localHtml1 = "<span id=\"def-"+wikipedia+"\"></span>";
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
				piece += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("'+wikipedia+'", "'+lang+'")</script></span>';
	            piece += '</td><td>';
	            piece += '<table><tr><td>';

	            if (wikipedia) {
	                piece += '<a href="http://en.wikipedia.org/wiki?curid=' +
	                        wikipedia +
	                        '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/wikipedia.png"/></a>';
	            }
	            piece += '</td></tr><tr><td>';
	            piece += '</td></tr></table>';

	            piece += '</td></tr>';
	        }
	        piece += '</table>';
	    }

		piece += '</div></div>';
	    return piece;
	}

	var getPieceShowSenses = function (jsonObject, lang){
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
	            var content = entity.preferred; 

	            piece += '<tr id="selectLine' + sens + '" href="'
	                    + wikipedia + '" >' 
						+ '<td id="selectArea' + sens + '" href="'
	                    + wikipedia + '" width="15%">';
				piece += '<p><b>' + content + '</b></p></td>';
				var localHtml1 = "<span id=\"def-"+wikipedia+"\"></span>";
                piece += '<td>';
				if (prob_c)
					 piece += '<p><b>Cond. prob.</b>: ' + prob_c + '</p>';

				piece += localHtml1;
				piece += '</td><td width="25%">';
				piece += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("'+wikipedia+'", "'+lang+'")</script></span>';
	            piece += '</td><td>';
	            piece += '<table><tr><td>';



	            if (wikipedia) {
	                piece += '<a href="http://en.wikipedia.org/wiki?curid=' +
	                        wikipedia +
	                        '" target="_blank"><img style="max-width:28px;max-height:22px;" src="resources/img/wikipedia.png"/></a>';
	            }
	            piece += '</td></tr><tr><td>';
	            piece += '</td></tr></table>';

	            piece += '</td></tr>';
	        }
	        piece += '</table>';
	    }

		piece += '</div></div>';
	    return piece;
	}
		
	const wikimediaURL_EN = 'https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=200&pageids=';
	const wikimediaURL_FR = 'https://fr.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=200&pageids=';
	const wikimediaURL_DE = 'https://de.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=200&pageids=';

	var imgCache = {};

	window.lookupWikiMediaImage = function (wikipedia, lang) {
		// first look in the local cache
		if (lang+wikipedia in imgCache) {
			var imgUrl = imgCache[lang+wikipedia];
			var document = (window.content) ? window.content.document : window.document;
    		var spanNode = document.getElementById("img-"+wikipedia);
			spanNode.innerHTML = '<img src="'+imgUrl+'"/>';
		} else { 
			// otherwise call the wikipedia API
			var theUrl = null;
			if (lang === 'fr')
				theUrl = wikimediaURL_FR + wikipedia;
			else if (lang === 'de')
				theUrl = wikimediaURL_DE + wikipedia;
			else
				theUrl = wikimediaURL_EN + wikipedia;
			// note: we could maybe use the en crosslingual correspondance for getting more images in case of non-English pages
		    $.ajax({
		    	url : theUrl,
		    	jsonp: "callback",
		    	dataType: "jsonp",
		    	xhrFields: { withCredentials: true },
		    	success: function(response) { 
		    		var document = (window.content) ? window.content.document : window.document;
	        		var spanNode = document.getElementById("img-"+wikipedia);
					if (response.query && spanNode) {
						if (response.query.pages[wikipedia]) {
							if (response.query.pages[wikipedia].thumbnail) {
								var imgUrl = response.query.pages[wikipedia].thumbnail.source;
			        			spanNode.innerHTML = '<img src="'+imgUrl+'"/>';
			        			// add to local cache for next time
								imgCache[lang+wikipedia] = imgUrl;
			        		}
			        	}
	        		}
	        	}
		    });
		}
	}

	function viewSentenceResults(numb) {           
		var sentence = parseInt(numb, 10);
		console.log("select sentence " + sentence);       

		var text = responseJson.text;
		var currentSentence = text.substring(responseJson.sentences[sentence].offsetStart,
											 responseJson.sentences[sentence].offsetEnd);   
											 
		var lastMaxIndex = responseJson.text.length;
		if (responseJson.entities) {
			var currentAnnotationIndex = responseJson.entities.length-1;
			for(var m=responseJson.entities.length-1; m>=0; m--) {
				if ( (responseJson.entities[m].offsetStart>=responseJson.sentences[sentence].offsetStart) &&
					 (responseJson.entities[m].offsetEnd<=responseJson.sentences[sentence].offsetEnd) ) {				
					var entity = responseJson.entities[m];
					var domains = entity.domains;
					var label = null;
					if (entity.type)
						label = entity.type;
					else if (domains && domains.length>0) {
						label = domains[0].toLowerCase();
					}
					else 
						label = entity.rawName;

			    	//var start = parseInt(entity.offsetStart,10);
				    //var end = parseInt(entity.offsetEnd,10);       
			    	var start = parseInt(entity.offsetStart,10) - responseJson.sentences[sentence].offsetStart;
				    var end = parseInt(entity.offsetEnd,10) - responseJson.sentences[sentence].offsetStart;  
				
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
						currentSentence = currentSentence.substring(0,start) 
							+ '<span id="annot-'+m+'" rel="popover" data-color="'+label+'">'
							+ '<span class="label ' + label + '" style="cursor:hand;cursor:pointer;" >'
							+ currentSentence.substring(start,end) + '</span></span>' + currentSentence.substring(end,currentSentence.length+1); 
						lastMaxIndex = start;
						currentAnnotationIndex = m;
						entityMap[currentAnnotationIndex] = [];
						entityMap[currentAnnotationIndex].push(responseJson.entities[m]);
					}	
				}					
		    } 
			currentSentence = "<p>" + currentSentence.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
			string = string.replace("<p></p>", "");
		
			display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>'+string+'</p></td>';
			display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';	
		}
								 
		$('#sentence_ner').html(currentSentence); 
	
		for (var key in entityMap) {
		  	if (entityMap.hasOwnProperty(key)) {
				$('#annot-'+key).bind('hover', viewEntity);  
				$('#annot-'+key).bind('click', viewEntity);  	
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
            entities.forEach(function(entity, n) {
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
					$.ajax({
					  	type: 'GET',
					  	url: 'service/KBConcept',
					  	data: { id : identifier, lang : lang },
					  	success: function(result) { conceptMap[result.wikipediaExternalRef] = result; },
					  	dataType: 'json'  
					});
				}

                //var theId = measurement.type;
                //var theUrl = null;
                //var theUrl = annotation.url;
                var pos = entity.pos;
                if ( (pos != null) && (pos.length > 0) ) {
                    pos.forEach(function(thePos, m) {
                        // get page information for the annotation
                        var pageNumber = thePos.p;
                        if (pageInfo[pageNumber-1]) {
                            page_height = pageInfo[pageNumber-1].page_height;
                            page_width = pageInfo[pageNumber-1].page_width;
                        }
                        annotateEntity(entityType, thePos, page_height, page_width, n, m);
                    });   
                }
            });
        }
	}

	function annotateEntity(theType, thePos, page_height, page_width, entitytIndex, positionIndex) {
        var page = thePos.p;
        var pageDiv = $('#page-'+page);
        var canvas = pageDiv.children('canvas').eq(0);
        //var canvas = pageDiv.find('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

        var x = thePos.x * scale_x - 1;
        var y = thePos.y * scale_y - 1 ;
        var width = thePos.w * scale_x + 1;
        var height = thePos.h * scale_y + 1;

        //make clickable the area
        theType = "" + theType;
        if (theType)
            theType = theType.replace(" ", "_");
        var element = document.createElement("a");
        var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+
            y+"px; left:"+x+"px;";
        element.setAttribute("style", attributes + "border-width: 2px;border-style:solid; "); //border-color: " + getColor(theId) +";");
        //element.setAttribute("style", attributes + "border:2px solid;");
        element.setAttribute("class", theType.toLowerCase());
        element.setAttribute("id", 'annot-' + entitytIndex  + '-' + positionIndex);
        element.setAttribute("page", page);

        pageDiv.append(element);

        $('#annot-' + entitytIndex + '-' + positionIndex).bind('hover', viewEntityPDF);
        $('#annot-' + entitytIndex + '-' + positionIndex).bind('click', viewEntityPDF);
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
		var localEntityNumber = parseInt(localID.substring(ind1+1,localID.length));

		if ( (entityMap[localEntityNumber] == null) || (entityMap[localEntityNumber].length == 0) ) {
			// this should never be the case
			console.log("Error for visualising annotation with id " + localEntityNumber 
				+ ", empty list of entities");
		}

		var lang = 'en'; //default
		var language = responseJson.language;
		if (language)
			lang = language.lang;
		var string = "";
		for(var entityListIndex = entityMap[localEntityNumber].length-1; 
				entityListIndex >= 0; 
				entityListIndex--) {
			var entity = entityMap[localEntityNumber][entityListIndex];
			var wikipedia = entity.wikipediaExternalRef;
			var domains = entity.domains;
			var type = entity.type;

			var colorLabel = null;
			if (type)
				colorLabel = type;
			else if (domains && domains.length>0) {
				colorLabel = domains[0].toLowerCase();
			}
			else 
				colorLabel = entity.rawName;

			var subType = entity.subtype;
			var conf = entity.nerd_score;
			//var definitions = entity.definitions;
			var definitions = getDefinitions(wikipedia);
			
			var content = entity.rawName; 
			//var normalized = entity.preferredTerm; 
			var normalized = getPreferredTerm(wikipedia);
			
			var sense = null;
			if (entity.sense)
				sense = entity.sense.fineSense;

			string += "<div class='info-sense-box "+colorLabel+"'";
			if (topPos != -1) 
	            string += " style='vertical-align:top; position:relative; top:" + topPos + "'";

			string += "><h3 style='color:#FFF;padding-left:10px;'>"+content.toUpperCase()+
				"</h3>";
			string += "<div class='container-fluid' style='background-color:#F9F9F9;color:#70695C;border:padding:5px;margin-top:5px;'>" +
				"<table style='width:100%;background-color:#fff;border:0px'><tr style='background-color:#fff;border:0px;'><td style='background-color:#fff;border:0px;'>";
				
			if (type)	
				string += "<p>Type: <b>"+type+"</b></p>";

			if (sense) {
				// to do: cut the sense string to avoid a string too large 
				if (sense.length <= 20)
					string += "<p>Sense: <b>"+sense+"</b></p>";
				else {
					var ind = sense.indexOf('_');
					if (ind != -1) {
						string += "<p>Sense: <b>"+sense.substring(0, ind+1)+"<br/>"+
							sense.substring(ind+1, sense.length)+"</b></p>";
					}
					else 
						string += "<p>Sense: <b>"+sense+"</b></p>";
				}
			}
			if (normalized)
				string += "<p>Normalized: <b>"+normalized+"</b></p>";
			
			if (domains && domains.length>0) {
				string += "<p>Domains: <b>";
				for(var i=0; i<domains.length; i++) {
					if (i != 0) 
						string += ", ";
					string += domains[i];
				}
				string += "</b></p>";
			}

			string += "<p>conf: <i>"+conf+ "</i></p>";
			string += "</td><td style='align:right;bgcolor:#fff'>";
			string += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("'+wikipedia+'", "'+lang+'")</script></span>';

			string += "</td></tr></table>";

			if ((definitions != null) && (definitions.length > 0)) {
				var localHtml = wiki2html(definitions[0]['definition'], lang);
				string += "<p><div class='wiky_preview_area2'>"+localHtml+"</div></p>";
			}

			// properties and relations if taxon
			var properties = getProperties(wikipedia);
			if ((properties != null) && (properties.length > 0)) {
				var localHtml = "";
				for(var i in properties) {
					var property = properties[i];
					if (property.template && (property.template == 'Taxobox')) {
						localHtml += "<tr><td>" + property.attribute + "</td><td>" + property.value + "</td></tr>"
					}
				}
				string += "<p><div><table class='properties' style='width:100%;background-color:#fff;border:0px'>"+localHtml+"</table></div></p>";
			}

			if (wikipedia != null) {
				string += '<p>Reference: '
				if (wikipedia != null) {
					string += '<a href="http://'+lang+'.wikipedia.org/wiki?curid=' + 
					wikipedia + 
					'" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" '+
					' src="resources/img/wikipedia.png"/></a>';
				}
				string += '</p>';
			}
		
			string += "</div></div>";
		}
		$('#detailed_annot-'+pageIndex).html(string);
        $('#detailed_annot-'+pageIndex).show();
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

	function getProperties(identifier) {
		var localEntity = conceptMap[identifier];
		if (localEntity != null) {
			return localEntity.properties;
		} else
			return null;
	}

	function getRelations(identifier) {
		var localEntity = conceptMap[identifier];
		if (localEntity != null) {
			return localEntity.relations;
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
		var localEntityNumber = parseInt(localID.substring(ind1+1,localID.length));

		if ( (entityMap[localEntityNumber] == null) || (entityMap[localEntityNumber].length == 0) ) {
			// this should never be the case
			console.log("Error for visualising annotation with id " + localEntityNumber 
				+ ", empty list of entities");
		}

		var lang = 'en'; //default
		var language = responseJson.language;
		if (language)
			lang = language.lang;
		var string = "";
		for(var entityListIndex=entityMap[localEntityNumber].length-1; 
				entityListIndex>=0; 
				entityListIndex--) {
			var entity = entityMap[localEntityNumber][entityListIndex];
			var wikipedia = entity.wikipediaExternalRef;
			var domains = entity.domains;
			var type = entity.type;

			var colorLabel = null;
			if (type)
				colorLabel = type;
			else if (domains && domains.length>0) {
				colorLabel = domains[0].toLowerCase();
			}
			else 
				colorLabel = entity.rawName;

			var subType = entity.subtype;
			var conf = entity.nerd_score;
			//var definitions = entity.definitions;
			var definitions = getDefinitions(wikipedia);
			
			var content = entity.rawName; 
			//var normalized = entity.preferredTerm; 
			var normalized = getPreferredTerm(wikipedia);
			
			var sense = null;
			if (entity.sense)
				sense = entity.sense.fineSense;

			string += "<div class='info-sense-box "+colorLabel+
			"'><h3 style='color:#FFF;padding-left:10px;'>"+content.toUpperCase()+
				"</h3>";
			string += "<div class='container-fluid' style='background-color:#F9F9F9;color:#70695C;border:padding:5px;margin-top:5px;'>" +
				"<table style='width:100%;background-color:#fff;border:0px'><tr style='background-color:#fff;border:0px;'><td style='background-color:#fff;border:0px;'>";
				
			if (type)	
				string += "<p>Type: <b>"+type+"</b></p>";

			if (sense) {
				// to do: cut the sense string to avoid a string too large 
				if (sense.length <= 20)
					string += "<p>Sense: <b>"+sense+"</b></p>";
				else {
					var ind = sense.indexOf('_');
					if (ind != -1) {
						string += "<p>Sense: <b>"+sense.substring(0, ind+1)+"<br/>"+
							sense.substring(ind+1, sense.length)+"</b></p>";
					}
					else 
						string += "<p>Sense: <b>"+sense+"</b></p>";
				}
			}
			if (normalized)
				string += "<p>Normalized: <b>"+normalized+"</b></p>";
			
			if (domains && domains.length>0) {
				string += "<p>Domains: <b>";
				for(var i=0; i<domains.length; i++) {
					if (i != 0) 
						string += ", ";
					string += domains[i];
				}
				string += "</b></p>";
			}

			string += "<p>conf: <i>"+conf+ "</i></p>";
			string += "</td><td style='align:right;bgcolor:#fff'>";
			string += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("'+wikipedia+'", "'+lang+'")</script></span>';

			string += "</td></tr></table>";

			if ((definitions != null) && (definitions.length > 0)) {
				var localHtml = wiki2html(definitions[0]['definition'], lang);
				string += "<p><div class='wiky_preview_area2'>"+localHtml+"</div></p>";
			}

			// properties and relations if taxon
			var properties = getProperties(wikipedia);
			if ((properties != null) && (properties.length > 0)) {
				var localHtml = "";
				for(var i in properties) {
					var property = properties[i];
					if (property.template && (property.template == 'Taxobox')) {
						localHtml += "<tr><td>" + property.attribute + "</td><td>" + property.value + "</td></tr>"
					}
				}
				string += "<p><div><table class='properties' style='width:100%;background-color:#fff;border:0px'>"+localHtml+"</table></div></p>";
			}

			if (wikipedia != null) {
				string += '<p>Reference: '
				if (wikipedia != null) {
					string += '<a href="http://'+lang+'.wikipedia.org/wiki?curid=' + 
					wikipedia + 
					'" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" '+
					' src="resources/img/wikipedia.png"/></a>';
				}
				string += '</p>';
			}
		
			string += "</div></div>";
		}
		$('#detailed_annot-0').html(string);	
		$('#detailed_annot-0').show();
	}

	function SubmitSuccesfulLId(responseJson, statusText) {
		$('#infoResult').html(''); 
		if ( (responseJson == null) || (responseJson.length == 0) ){
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
		if ( (responseJson == null) || (responseJson.length == 0) ){
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
			for(var sentence in responseJson.sentences) {    
				if (m%2 == 0) { 	
	  				display += '<tr class="highlight" id="sent_'+m+'" rank="'+m+'" >'; 
				}   
				else {
					display += '<tr id="sent_'+m+'" rank="'+m+'" >';     
				}
				display += 
				  '<td style="width:25px;height:13px;font-size:small;">'+m+'</td>'  
				var start = responseJson.sentences[sentence].offsetStart;
				var end = responseJson.sentences[sentence].offsetEnd;
				display += '<td style="font-size:small;height:13px;color:#333;">'+text.substring(start,end)+'</td>';
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
	var queryTemplate = { "text" : "", "shortText" : "", "termVector" : [], "language" : { "lang" : "en" }, "entities" : [], "onlyNER" : false, "resultLanguages" : [ "de", "fr"],
						  "nbest" : false, "sentence" : false, "format" : "JSON", 
 						  "customisation" : "generic" };

 	// query + PDF
	var queryTemplate2 = { "language" : { "lang" : "en" }, "onlyNER" : false, "resultLanguages" : [ "de", "fr"],
						  "nbest" : false, "format" : "JSON",
 						  "customisation" : "generic" };

 	// term lookup
 	var queryTemplate3 = { "term" : "", "language" : { "lang" : "en" } };

	function processChange() {
		var selected = $('#selectedService').attr('value');

		if (selected == 'processNERDQuery') {
			createInputTextArea('query');
			setBaseUrl('processNERDQuery');
			removeInputFile();
			//$("#nerd-text").hide();
			//$("#default_nerd_query").attr('checked', 'checked');
			//$("#nerd-query").show();
			//$("#lid").hide();
			$('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplate)));
		} 
		else if (selected == 'processNERDQueryPDF') {
			createInputTextArea('query');
			createInputFile();
			setBaseUrl('processNERDQuery');
			//$("#nerd-text").hide();
			//$("#default_nerd_query").attr('checked', 'checked');
			//$("#nerd-query").show();
			//$("#lid").hide();
			$('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplate2)));
		}  
		else if (selected == 'processLIdText') {
			createInputTextArea('query');
			setBaseUrl('processLIdText'); 
			removeInputFile(); 
			//$("#lid").show();   
			//$("#nerd-query").hide();  
			//$("#nerd-text").hide();     
			//$("#default_lid").attr('checked', 'checked');
		}
		else if (selected == 'processSentenceSegmentation') {
			createInputTextArea('query');
			setBaseUrl('processSentenceSegmentation'); 
			removeInputFile(); 
			//$("#lid").show(); 
			//$("#nerd-query").hide();  
			//$("#nerd-text").hide();     
			//$("#default_lid").attr('checked', 'checked');
		}
		else if (selected == 'KBTermLookup') {
			createSimpleTextFieldArea('query', 'term');
			setBaseUrl('KBTermLookup');
			//$("#nerd-text").hide();  
			removeInputFile();
			//$("#default_nerd_query").attr('checked', 'checked');
			//$("#nerd-query").show();
			//$("#lid").hide();
			$('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplate3)));
		}
		else if (selected == 'KBConcept') {
			createSimpleTextFieldArea('query', 'concept ID');
			setBaseUrl('KBConcept');
			//$("#nerd-text").hide();  
			removeInputFile();
			//$("#default_nerd_query").attr('checked', 'checked');
			//$("#nerd-query").show();
			//$("#lid").hide();
			$('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplate3)));
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
"Methane is a powerful greenhouse gas and its concentration in the atmosphere has increased over the past decades. Methane produced by methanogenic Archae can be consumed through aerobic and anaerobic oxidation pathways. In anoxic conditions found in freshwater environments such as meromictic lakes, CH4 oxidation pathways involving different terminal electron acceptors such as NO 3 , SO2 4 , and oxides of Fe and Mn are thermodynamically possible. In this study, a reactive transport model was developed to assess the relative significance of the different pathways of CH4 consumption in the water column of Lake Pavin. In most cases, the model reproduced experimental data collected from the field from June 2006 to June 2007. Although the model and the field measurements suggest that anaerobic CH4 oxidation may contribute to CH4 consumption in the water column of Lake Pavin, aerobic oxidation remains the major sink of CH4 in this lake." ];
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
		"",
		""
	];

	function resetExamplesClasses() {
		$('#example1').removeClass('section-active').addClass('section-non-active');
	  	$('#example2').removeClass('section-active').addClass('section-non-active');
		$('#example3').removeClass('section-active').addClass('section-non-active');
		$('#example4').removeClass('section-active').addClass('section-non-active');
		$('#reuters1').removeClass('section-active').addClass('section-non-active');
	  	$('#reuters2').removeClass('section-active').addClass('section-non-active');
		$('#reuters3').removeClass('section-active').addClass('section-non-active');
		$('#reuters4').removeClass('section-active').addClass('section-non-active');
	}

	function createSimpleTextFieldArea(nameInput, entryType) {
		$('#label').html('&nbsp;'); 					                   
		$('#input').remove();
		$('#withExamples').remove();
		$('#field').html("");
 
		var piece = '<table><tr>' + 
					'<td><textarea style="height:28px;" rows="1" id="input2" name="'+nameInput+'" placeholder="Enter a ' + entryType + '..."/></td>' +
					'<td style="width:10px;"></td>' +
					'<td><select style="height:auto;width:auto;top:-10px;right:10px;" name="lang" id="lang">' +
					'<option value="en" selected>en</option>' + 
					'<option value="de">de</option>' +
					'<option value="fr">fr</option></select></td>' +
					'</tr></table>';
		$('#field').append(piece);
	}

	function createInputTextArea(nameInput) {
		//$('#label').html('Enter ' + nameInput);             
		$('#label').html('&nbsp;'); 					                   
		$('#input').remove();
		$('#field').html("");
 
		$('#field').append('<table id="withExamples"><tr><td><textarea class="span7" rows="5" id="input" name="'+nameInput+'" /></td>'+
			"<td><span style='padding-left:20px;'>&nbsp;</span></td>"+
		"<td><table id='examplesBlock1'><tr style='line-height:130%;'><td><span id='example1' style='font-size:90%;'>Cendari</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='example2' style='font-size:90%;'>PubMed_1</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='example3' style='font-size:90%;'>PubMed_2</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='example4' style='font-size:90%;'>HAL_1</span></td></tr>"+		
			"</table></td>"+
			"<td><span style='padding-left:20px;'>&nbsp;</span></td>"+
		"<td><table id='examplesBlock2'><tr style='line-height:130%;'><td><span id='reuters1' style='font-size:90%;'>Reuters_1</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='reuters2' style='font-size:90%;'>Reuters_2</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='reuters3' style='font-size:90%;'>French_1</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='reuters4' style='font-size:90%;'>German_1</span></td></tr>"+
			"</table></td>"+
			"</tr></table>");
		// binding of the examples
		$('#example1').popover();
		$('#example1').bind('click',
			function(event) {
				resetExamplesClasses();
			    $(this).addClass('section-active').removeClass('section-non-active');
				var selected = $('#selectedService option:selected').attr('value');
				if ( (selected == 'processNERDQuery') || (selected == 'processERDQuery') ) {
					var queryInstance = queryTemplate
					queryInstance.text=textExamples[0];
					$('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
				}
				else 
					$('#input').attr('value', textExamples[0]); 
			});
		$('#example2').popover();
		$('#example2').bind('click',
			function(event) {
				resetExamplesClasses();
			    $(this).addClass('section-active').removeClass('section-non-active');  
				var selected = $('#selectedService option:selected').attr('value'); 
				if ( (selected == 'processNERDQuery') || (selected == 'processERDQuery') ) {
					var queryInstance = queryTemplate
					queryInstance.text=textExamples[1];
					$('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
				}
				else 
					$('#input').attr('value', textExamples[1]); 
			});
		$('#example3').popover();
		$('#example3').bind('click',
			function(event) {
				resetExamplesClasses();
			    $(this).addClass('section-active').removeClass('section-non-active');  
				var selected = $('#selectedService option:selected').attr('value'); 
				if ( (selected == 'processNERDQuery') || (selected == 'processERDQuery') ){
					var queryInstance = queryTemplate
					queryInstance.text=textExamples[2];
					$('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
				}
				else 
					$('#input').attr('value', textExamples[2]); 
			});
		$('#example4').popover();
		$('#example4').bind('click',
			function(event) {
				resetExamplesClasses();
			    $(this).addClass('section-active').removeClass('section-non-active');   
				var selected = $('#selectedService option:selected').attr('value'); 
				if ( (selected == 'processNERDQuery') || (selected == 'processERDQuery') ){
					var queryInstance = queryTemplate
					queryInstance.text=textExamples[3];
					$('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
				}
				else 
					$('#input').attr('value', textExamples[3]); 
			});
		
		$('#reuters1').bind('click',
			function(event) {
				resetExamplesClasses();
			    $(this).addClass('section-active').removeClass('section-non-active');
				var selected = $('#selectedService option:selected').attr('value'); 
				if ( (selected == 'processNERDQuery') || (selected == 'processERDQuery') ) {
					var queryInstance = queryTemplate
					queryInstance.text=reutersExamples[0];
					$('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
				}
				else 
					$('#input').attr('value', reutersExamples[0]); 				
			}
		);
		$('#reuters2').bind('click',
			function(event) {
				resetExamplesClasses();
				$(this).addClass('section-active').removeClass('section-non-active');
				var selected = $('#selectedService option:selected').attr('value'); 
				if ( (selected == 'processNERDQuery') || (selected == 'processERDQuery') ) {
					var queryInstance = queryTemplate
					queryInstance.text=reutersExamples[1];
					$('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
				}
				else 
					$('#input').attr('value', reutersExamples[1]); 		
			}
		);
		$('#reuters3').bind('click',
			function(event) {
				resetExamplesClasses();
				$(this).addClass('section-active').removeClass('section-non-active');
				var selected = $('#selectedService option:selected').attr('value'); 
				if ( (selected == 'processNERDQuery') || (selected == 'processERDQuery') ) {
					var queryInstance = queryTemplate
					queryInstance.text=reutersExamples[2];
					$('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
				}
				else 
					$('#input').attr('value', reutersExamples[2]); 		
			}
		);
		$('#reuters4').bind('click',
			function(event) {
				resetExamplesClasses();
				$(this).addClass('section-active').removeClass('section-non-active');
				var selected = $('#selectedService option:selected').attr('value'); 
				if ( (selected == 'processNERDQuery') || (selected == 'processERDQuery') ) {
					var queryInstance = queryTemplate
					queryInstance.text=reutersExamples[3];
					$('#input').attr('value', vkbeautify.json(JSON.stringify(queryInstance)));
				}
				else 
					$('#input').attr('value', reutersExamples[3]);
			}
		);
		
		//$('#gbdForm').attr('enctype', '');
		//$('#gbdForm').attr('method', 'post');

		$('#field2').append('<table><tr><td><textarea class="span7" rows="5" id="input2" name="'+nameInput+'" /></td>'+
		"<td><span style='padding-left:20px;'>&nbsp;</span></td><td><table>"+
			"<tr style='line-height:130%;'><td><span id='example21' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 1' data-content='"+textExamples[0]+"' data-trigger='hover'>example 1</span></td></tr>"+
		  	"<tr style='line-height:130%;'><td><span id='example22' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 2' data-content='"+textExamples[1]+"' data-trigger='hover'>example 2</span></td></tr>"+
			"<tr style='line-height:130%;'><td><span id='example23' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 3' data-content='"+textExamples[2]+"' data-trigger='hover'>example 3</span></td></tr>"+
		  	"<tr style='line-height:130%;'><td><span id='example24' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 4' data-content='"+textExamples[3]+"' data-trigger='hover'>example 4</span></td></tr>"+
			
			"</table></td></tr></table>");
		// binding of the examples
		$('#example21').popover();
		$('#example21').bind('click',
			function(event) {
			    $(this).addClass('section-active').removeClass('section-non-active');
			  	$('#example22').removeClass('section-active').addClass('section-non-active');
				$('#example23').removeClass('section-active').addClass('section-non-active');
				$('#example24').removeClass('section-active').addClass('section-non-active');
				$('#input2').attr('value', textExamples[0]); 
			});	
		$('#example22').popover();
		$('#example22').bind('click',
			function(event) {
			    $(this).addClass('section-active').removeClass('section-non-active');
				$('#example21').removeClass('section-active').addClass('section-non-active');
				$('#example23').removeClass('section-active').addClass('section-non-active');
				$('#example24').removeClass('section-active').addClass('section-non-active');     
				$('#input2').attr('value', textExamples[1]); 
			});
		$('#example23').popover();
		$('#example23').bind('click',
			function(event) {
			    $(this).addClass('section-active').removeClass('section-non-active');
				$('#example21').removeClass('section-active').addClass('section-non-active');
				$('#example22').removeClass('section-active').addClass('section-non-active');
				$('#example24').removeClass('section-active').addClass('section-non-active');     
				$('#input2').attr('value', textExamples[2]); 
			});
		$('#example24').popover();
		$('#example24').bind('click',
			function(event) {
			    $(this).addClass('section-active').removeClass('section-non-active');
				$('#example21').removeClass('section-active').addClass('section-non-active');
				$('#example22').removeClass('section-active').addClass('section-non-active');
				$('#example23').removeClass('section-active').addClass('section-non-active');     
				$('#input2').attr('value', textExamples[3]); 
			});
							
		$('#gbdForm2').attr('enctype', '');
		$('#gbdForm2').attr('method', 'post');
	}
                         
	function convert2NicelyTabulated(jsonParses, indexSentence) {
		var result = 
'<table class="table table-condensed" style="border-width:0px;font-size:small;background-color:#FEE9CC;">';      
		connlParse= jsonParses[indexSentence].parse;
		// we remove the first index            					
		var lines = connlParse.split(/\r?\n/);
		for(var line in lines) {    
			if (lines[line].trim().length == 0)
				continue;
	 result += '<tr style="align:left;border-width: 0px;font-size:small;background-color:#FEE9CC;">';
			var tokens = lines[line].split(/\s/);
		    var n = 0;
			for(var token in tokens) {     
				if (tokens[token].trim().length == 0)
					continue;     
	 result += '<td style="align:left;border-width: 0px;font-size:small;background-color:#FEE9CC;">';	
				if (n == 1) {	
					result += '<b>'+tokens[token]+'</b>';    
				} 
				else if (n == 3) {
					if (tokens[token] == '.')
						result += 'DOT'; 
					else if (tokens[token] == ',')
						result += 'PUNCT';
					else 
						result += tokens[token];  
				}
				else {
					result += tokens[token];
				}       
				result += '</td>';
				n++;
			}
			result+='</tr>'; 
		}  

		result += '</table>';             
		return result;
	}
          
	function viewBrat(loc, docData, collData) {        
		console.log('viewBrat');        
		console.log(docData);   
		//$('#loading-icon-brat-syntax').show(); 	   
		// for activating brat data visualisation

		var dispatcher = Util.embed(
	        // id of the div element where brat should embed the visualisations
	        loc,
	        // object containing collection data
	        collData,
	        // object containing document data
	        docData,
	        // Array containing locations of the visualisation fonts
	        webFontURLs
	   );     
	   //if (callback2) callback(dispatcher2);     
	   //dispatcher2.post('ajax', callback2);   
		$('#loading-icon-brat-syntax').hide(); 				   
	}                  				

   	function convert2JsonSyntacticDep(jsonParses, indexSentence) {   
		connlParse= jsonParses[indexSentence].parse;
		var docData = {}; 
		var entities = [];  
		var relations = [];   
		var attributes = [];       				
		var text = "";
		var lines = connlParse.split(/\r?\n/);
		var countEntity = 0;    
		var countRelation = 0; 
		var countAttribute = 0;    
		var offset = 0;
		for(var line in lines) {
			var tokens = lines[line].split(/\s/); 
			var n = 0;   
			var lastWord = ""; 
			var currentHead = "";      
		    for(var token in tokens) { 
				if (tokens[token].trim().length == 0)
					continue;
				if (n == 1) {
					text += tokens[token]+" "; 
					lastWord = tokens[token];
				}
				else if (n == 3) {
					// we add an entity for this token
					var pos = tokens[token];    
					if (pos == '.')
						pos = 'DOT'; 
					if (pos == ',')
						pos = 'PUNCT';	
					var toto = [
					            "T"+countEntity,
					            pos,
					            [ [ offset, offset+lastWord.length] ]
					];  
					entities.push(toto);    
					countEntity++; 
					offset = offset+lastWord.length + 1;
				}    
				else if (n == 5) { 
					// give the syntactic head 
					currentHead = parseInt(tokens[token], 10)-1;
				}
				else if (n == 6) { 
					// give the syntactic relation label                
					if (tokens[token] == "root") {
						// root is represented as an entity_attribute_types   
						/*var toto = [ [ "A"+countAttribute, tokens[token], "T"+(line) ] ];
						attributes.push(toto);   
						countAttribute++;   */     
						var toto = [
					            "R"+countRelation,
					            tokens[token],
					            [ [ "Arg1", "T"+(line) ], [ "Arg2", "T"+(line) ] ]
					        ];    
					    relations.push(toto);  
						countRelation++;
					}
					else {
				   	 	var toto = [
					            "R"+countRelation,
					            tokens[token],
					            [ [ "Arg1", "T"+(line) ], [ "Arg2", "T"+currentHead ] ]
					        ];    
					    relations.push(toto);  
						countRelation++;   
					}      
				}	
				n++;	
			}
		}
		
		docData['text'] = text.trim();
		if (countEntity > 0) 
			docData['entities'] = entities;  
		if (countAttribute > 0) 
			docData['attributes'] = attributes;	
		if (countRelation > 0) 
			docData['relations'] = relations;
		return docData;
	}
	  
	function convert2JsonSemanticDep(jsonParses, indexSentence) {    
		connlParse= jsonParses[indexSentence].parse;
		var docData = {}; 
		var entities = [];  
		var relations = [];   
		var attributes = [];       				
		var text = "";
		var lines = connlParse.split(/\r?\n/);
		var countEntity = 0;    
		var countRelation = 0; 
		var countAttribute = 0;    
		var offset = 0;       
		var usedEntities = [];
		for(var line in lines) {
			var tokens = lines[line].split(/\s/); 
			var n = 0;   
			var lastWord = ""; 
			var currentHead = "";      
		    for(var token in tokens) { 
				if (tokens[token].trim().length == 0)
					continue;
				if (n == 1) {
					//text += tokens[token]+" "; 
					//lastWord = tokens[token];
				}
				else if (n == 2) {
					// we add an entity per lemma	 										   			
					var lemma = tokens[token];  
					
					//text += lemma+" "; 
					if (lemma != '0')
						lastWord = lemma;
					else
						lastWord = tokens[1];
				}      
				else if (n == 4) {
					// features: e.g. roleset, pb=verb.01          
					if (tokens[token] == '_') {     			
						// no feature			
						var toto = [
						            "T"+countEntity,
						            "arg",
						            [ [ offset, offset+lastWord.length] ]
						];  
						entities.push(toto);
						countEntity++;
						offset = offset+lastWord.length + 1;						
					}
					else {       
						// current features are (separated by a |):
						// - p2: second best POS
						// - pb: best verb synset
						// = vn: verbnet class
						var features = tokens[token].split('|');
						for(var feature in features) {
							var ind = features[feature].indexOf('=');
							var attribute = features[feature].substring(0,ind);;
							var value = features[feature].substring(ind+1,features[feature].length);
							if (attribute == 'p2') { 
								continue;
							}
							else if (attribute == 'pb') {
								lastWord = value;
							}
							else if (attribute == 'vn') {
								if (value != 'unknown') {
									lastWord += "|" + value;
								}
							}
						}
						
						var toto = [
						            "T"+countEntity,
						            "pred",
						            [ [ offset, offset+lastWord.length] ]
						];  
						entities.push(toto);    
						countEntity++;
						offset = offset+lastWord.length + 1;						
					}
					text += lastWord+" ";   
				} 
				else if (n == 7) {   
					var sem_label = tokens[token];   				   
					if (sem_label == '_')
						continue;     	
					var rels = sem_label.split(';');
					for(var rel in rels) { 
						if (rels[rel].trim().length == 0)
							continue;  
				   	 	var ind = rels[rel].indexOf(':');     
						if (ind == -1) {
							continue;
						}    
						currentHead = parseInt(rels[rel].substring(0,ind), 10)-1;  
						var toto = [
					            "R"+countRelation,
					            rels[rel].substring(ind+1,rels[rel].length),
					            [ [ "Arg1", "T"+currentHead ], [ "Arg2", "T"+(line) ] ]
					        ];    
					    relations.push(toto);  
						countRelation++;    
						if (usedEntities.indexOf(currentHead) == -1)
							usedEntities.push(currentHead);  
						if (usedEntities.indexOf(line) == -1)    	
							usedEntities.push(line);  
					}
				}
				n++;	
			}
		}
		    
		// we clean the non used entities for better visualisation 
		var newEntities = [];
		for(var i in usedEntities) {
			newEntities.push(entities[usedEntities[i]]);
		}
		entities = newEntities;
		
		docData['text'] = text.trim();
		if (countEntity > 0) 
			docData['entities'] = entities;  
		if (countAttribute > 0) 
			docData['attributes'] = attributes;	
		if (countRelation > 0) 
			docData['relations'] = relations;
		return docData;
	}
	
	function sortCategories(categories) {
		var newCategories = []; 
		if (categories) {
			newCategories = categories.sort(function(a, b) {
    			return (b.weight) - (a.weight);
    		});;
		}
		return newCategories;
	}

	var bratLocation = 'brat/';
	/*head.js(
	    // External libraries
	    bratLocation + '/client/lib/jquery.min.js',
	    bratLocation + '/client/lib/jquery.svg.min.js',
	    bratLocation + '/client/lib/jquery.svgdom.min.js',

		'resources/bootstrap/js/bootstrap.min.js',  
        'resources/bootstrap/js/prettify.js',
           'resources/bootstrap/js/lang-ml.js',

	    // brat helper modules
	    bratLocation + '/client/src/configuration.js',
	    bratLocation + '/client/src/util.js',
	    bratLocation + '/client/src/annotation_log.js',
	    bratLocation + '/client/lib/webfont.js',

	    // brat modules
	    bratLocation + '/client/src/dispatcher.js',
	    bratLocation + '/client/src/url_monitor.js',
	    bratLocation + '/client/src/visualizer.js'
	);       */

	var webFontURLs = [
	    bratLocation + '/static/fonts/Astloch-Bold.ttf',
	    bratLocation + '/static/fonts/PT_Sans-Caption-Web-Regular.ttf',
	    bratLocation + '/static/fonts/Liberation_Sans-Regular.ttf'
	];   
	
	/** admin functions */
		
	var selectedAdmKey="", selectedAdmValue, selectedAdmType;
		
	function adminShowRequest(formData, jqForm, options) {
		$('#TabAdminProps').show();
		$('#admMessage').html('<font color="grey">Requesting server...</font>');
	    return true;
	}

	function adminAjaxError() {
		$('#admMessage').html("<font color='red'>Autentication error.</font>");
	}

	function adminSubmitSuccesful(responseText, statusText) {
		$('#admMessage').html("<font color='green'>Welcome to the admin console.</font>");
		parseXml(responseText);
		rowEvent();
	}
	
	function parseXml(xml){
		var out="<pre><table class='table-striped table-hover'><thead><tr align='left'><th>Property</th><th align='left'>value</th></tr></thead>";
		$(xml).find("property").each(function(){
			var dsipKey = $(this).find("key").text();
			var key = dsipKey.split('.').join('-');
			var value = $(this).find("value").text();
			var type = $(this).find("type").text();
			out+="<tr class='admRow' id='"+key+"'><td><input type='hidden' value='"+type+"'/>"+dsipKey+"</td><td><div>"+value+"</div></td></tr>";
		});
		out+="</table></pre>";
		$('#TabAdminProps').html(out);
	}
	
	function rowEvent(){
		$('.admRow').click(function() {
			$("#"+selectedAdmKey).find("div").html($("#val"+selectedAdmKey).attr("value"));
			selectedAdmKey=$(this).attr("id");
			selectedAdmValue=$(this).find("div").text();
			selectedAdmType=$(this).find("input").attr("value");
			$(this).find("div").html("<input type='text' id='val"+selectedAdmKey+"' size='80' value='"+selectedAdmValue+"' class='input-xxlarge'/>");
			$("#val"+selectedAdmKey).focus();
		});
		
		$('.admRow').keypress(function(event) {
			var keycode = (event.keyCode ? event.keyCode : event.which);
			selectedAdmKey=$(this).attr("id");
			// Enter key
			if(keycode == '13') {				
				var newVal = $("#val"+selectedAdmKey).val();	
				$("#"+selectedAdmKey).find("div").html(newVal);
				selectedAdmValue=newVal;
				selectedAdmType=$(this).find("input").attr("value");				
				generateXmlRequest();
			}
			// Escape key
			if(keycode == '27') {
				$("#"+selectedAdmKey).find("div").html(selectedAdmValue);
			}
		});
	}
	
	function generateXmlRequest(){
		var xmlReq= "<changeProperty><password>"+$('#admPwd').val()+"</password>";
		xmlReq+="<property><key>"+selectedAdmKey.split('-').join('.')+"</key><value>"+selectedAdmValue+"</value><type>"+selectedAdmType+"</type></property></changeProperty>";
		if("fr.inria.nerd.service.admin.pw"==selectedAdmKey.split('-').join('.')) {
			$('#admPwd').attr('value', selectedAdmValue);
		}
		$.ajax({
			  type: 'POST',
			  url: $(location).attr('href')+"changePropertyValue",
			  data: {xml: xmlReq},
			  success: changePropertySuccesful,
			  error: changePropertyError
			});
	}
	
	function fitToContainer(canvas){
	  	// make a canvas visually fill the positioned parent
	  	canvas.style.width ='100%';
	  	canvas.style.height='100%';
	  	// ...then set the internal size to match
	  	canvas.width  = canvas.offsetWidth;
	  	canvas.height = canvas.offsetHeight;
	}

	function changePropertySuccesful(responseText, statusText) {
		$("#"+selectedAdmKey).find("div").html(responseText);
		$('#admMessage').html("<font color='green'>Property "+selectedAdmKey.split('-').join('.')+" updated with success</font>");
	}
	
	function changePropertyError() {
		$('#admMessage').html("<font color='red'>An error occured while updating property"+selectedAdmKey.split('-').join('.')+"</font>");
	}

})(jQuery);