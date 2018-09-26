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
	
	// for detailed view
	var responseJsonNERDQuery = null;

	var api_key = "AIzaSyBLNMpXpWZxcR9rbjjFQHn_ULbU-w1EZ5U";

	function defineBaseSecondaryURL(ext) {
		var baseUrl = null;
		if ( $(location).attr('href').indexOf("index.html") != -1)
			baseUrl = $(location).attr('href').replace("index.html", "service/" + ext);
		else
			baseUrl = $(location).attr('href') + "service/" + ext;
		return baseUrl;
	}

	function setBaseUrl(ext) {
		var baseUrl = "localhost:8080/" + ext;
		$('#gbdForm').attr('action', baseUrl);
	}

	function setBaseSecondaryUrl(ext) {
		var baseUrl = defineBaseSecondaryURL(ext);
		$('#gbdForm2').attr('action', baseUrl);
	}

	$(document).ready(function() {   
		// components / detailed / simple
		
		//$("#subTitle").html("GROBID+NERD");
		$("#divServices").show();
		$("#nerd-text").show();
//		$("#nerd-query").hide();
		
		//createInputTextArea('query');
		
		createInputFile('processFulltextDocument');
		setBaseUrl('processFulltextDocument'); 
		setBaseSecondaryUrl('processNERDQueryScience');  
		
		$("#selectedService").val('processNERDQueryScience');
		//$("#default_lid").attr('checked', 'checked');             
        //setBaseUrlDetailed('parseText');

		$('#selectedService').change(function() {
			processChange();
			return true;
		});
		
		$('#submitRequest').bind('click', submitQuery);
		
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
	    $('#requestResult').html('<font color="grey">Requesting server...</font>');
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
		// we submit the document to Grobid
		$('#gbdForm').ajaxForm({
            beforeSubmit: ShowRequest,
            success: SubmitSuccesful,
            error: AjaxError,
            dataType: "text"
        });
		$('#gbdForm').attr('enctype', 'multipart/form-data');
		$('#gbdForm').attr('method', 'post'); 
		var baseUrl = "localhost:8080/" + ext;
		$('#gbdForm').attr('action', baseUrl);
		
		
		
		
		
		// reinit the entity map
		entityMap = new Object();
		var urlLocal = $('#gbdForm').attr('action');
		//console.log(JSON.stringify($('#textInputArea').val()));
		if ( urlLocal.indexOf('NERDText') != -1 ) { 
			$.ajax({
			  type: 'GET',
			  url: urlLocal,
			  data: { text : $('#input').val(), 
			  		  onlyNER : $('#onlyNER').is(':checked'),
				  	  shortText : $('#shortText').is(':checked'),
				  	  nbest : $('#nbest').is(':checked'),
					  sentence : $('#sentence').is(':checked'),
					  customisation : $('#customisation').val() },
//			  processData: false,
			  success: SubmitSuccesful,
			  error: AjaxError,
			  contentType:false  
			//contentType: "multipart/form-data"
			});
		}
		else {
			$.ajax({
			  type: 'POST',
			  url: urlLocal,
			  data: $('#input').val(),
		 	  //data: JSON.stringify($('#textInputArea').val()),
			  //beforeSubmit: ShowRequest2,
			  success: SubmitSuccesful,
			  error: AjaxError,
			  dataType: "text"
			});
		}
		
		$('#requestResult').html('<font color="grey">Requesting server...</font>');
	}

	function AjaxError() {
		$('#requestResult').html("<font color='red'>Error encountered while requesting the server.</font>");      
		responseJson = null;
	}
    
	/*function AjaxError2() {
		$('#requestResult2').html("<font color='red'>Error encountered while requesting the server.</font>");      
		responseJsonParse = null;
	}*/

	function SubmitSuccesful(responseText, statusText) { 
		var selected = $('#selectedService option:selected').attr('value');
		var display = "<pre class='prettyprint lang-xml' id='xmlCode'>";  
		var testStr = vkbeautify.xml(responseText);
	
		display += htmll(testStr);

		display += "</pre>";
		$('#requestResult').html(display);
		window.prettyPrint && prettyPrint();
		$('#requestResult').show();
	}

	function htmll(s) {
    	return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  	}

	function SubmitSuccesfulNERD(responseText, statusText) {          					             
		//console.log(responseText);
		responseJson = responseText;
		
		if ( (responseJson == null) || (responseJson.length == 0) ) {
			$('#requestResult')
				.html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");   
			return;
		}

		if ( ($('#selectedService').attr('value') == 'processNERDQuery') || 
			($('#selectedService').attr('value') == 'processNERDQueryScience') ) {
			responseJson = jQuery.parseJSON(responseJson);
		}

		var display = '<div class=\"note-tabs\"> \
			<ul id=\"resultTab\" class=\"nav nav-tabs\"> \
		   		<li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Annotations</a></li> \
				<li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
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
				if (responseJson.entities) {
					var currentAnnotationIndex = responseJson.entities.length-1;
					for(var m=responseJson.entities.length-1; m>=0; m--) {
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
//console.log(entityMap);
					string = "<p>" + string.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
					//string = string.replace("<p></p>", "");
				
					display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>'+string+'</p></td>';
					display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';	
				}
				display += '</tr>';
			}
			else {
				//for(var sentence in responseJson.sentences) 
				{    
					display += '<tr style="background-color:#FFF;">';  
					
					display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p><span id="sentence_ner">'+
						" "+'</span></p></td>';
					display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';	
					display += '</tr>';
				}
			}
			
			display += '</table>\n';
		}
		
		display += '</pre>\n';
		
		//$('#requestResult').html(display);  
		
		display += '</div> \
					<div class="tab-pane " id="navbar-fixed-json">\n';
		// JSON visualisation component 	
		// with pretty print
		display += "<pre class='prettyprint' id='jsonCode'>";  
		
		display += "<pre class='prettyprint lang-json' id='xmlCode'>";  
		var testStr = vkbeautify.json(responseText);
		
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
	
		/*if (responseJson.entities) {
		    for(var m=responseJson.entities.length-1; m>=0; m--) {
				$('#annot-'+m).bind('hover', viewEntity);  
				$('#annot-'+m).bind('click', viewEntity); 
			}
		} 
		$('#detailed_annot-0').hide();	 */
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
					var label = entity.type;	
					if (!label)
						label = entity.rawName;
					// prediction result from Nerd-Kid
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
//console.log(entityMap);
			//currentSentence = "<p>" + currentSentence.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
			//string = string.replace("<p></p>", "");
		
			//display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>'+string+'</p></td>';
			//display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';	
		}
					
			
					
											 
		/*if (responseJson.entities) {
			for(var m=responseJson.entities.length-1; m>=0; m--) {
				if ( (responseJson.entities[m].offsetStart>=responseJson.sentences[sentence].offsetStart) &&
					 (responseJson.entities[m].offsetEnd<=responseJson.sentences[sentence].offsetEnd) ) {
					var entity = responseJson.entities[m];
					var label = entity.type;	
					if (!label)
						label = entity.rawName;
			    	var start = parseInt(entity.offsetStart,10) - responseJson.sentences[sentence].offsetStart;
				    var end = parseInt(entity.offsetEnd,10) - responseJson.sentences[sentence].offsetStart;                    
					currentSentence = currentSentence.substring(0,start) 
						+ '<span id="annot-'+m+'" rel="popover" data-color="'+label+'">'
						+ '<span class="label ' + label + '" style="cursor:hand;cursor:pointer;" >'
						+ currentSentence.substring(start,end) + '</span></span>' 
						+ currentSentence.substring(end,currentSentence.length+1); 
				}
			}
		}*/
		
		$('#sentence_ner').html(currentSentence); 
	
		for (var key in entityMap) {
		  	if (entityMap.hasOwnProperty(key)) {
				$('#annot-'+key).bind('hover', viewEntity);  
				$('#annot-'+key).bind('click', viewEntity);  	
		  	}
		}
		$('#detailed_annot-0').hide();	
	
		/*if (responseJson.entities) {
		    for(var m=responseJson.entities.length-1; m>=0; m--) {
				$('#annot-'+m).bind('hover', viewEntity);  
				$('#annot-'+m).bind('click', viewEntity);  	
				$('#detailed_annot-0').hide();	 
			}
		}*/
			
	}

	function viewEntity() {
		var localID = $(this).attr('id');

		if (responseJson.entities == null) {
			return;
		}

		var ind1 = localID.indexOf('-');
		//var ind2 = localID.lastIndexOf('-');
		//var localSentenceNumber = parseInt(localID.substring(ind1+1,ind2));
		var localEntityNumber = parseInt(localID.substring(ind1+1,localID.length));

		if ( (entityMap[localEntityNumber] == null) || (entityMap[localEntityNumber].length == 0) ) {
			// this should never be the case
			console.log("Error for visualising annotation with id " + localEntityNumber 
				+ ", empty list of entities");
		}

		var string = "";
		for(var entityListIndex=entityMap[localEntityNumber].length-1; 
				entityListIndex>=0; 
				entityListIndex--) {

			//var entity = responseJson.entities[localEntityNumber];
			var entity = entityMap[localEntityNumber][entityListIndex];
			var domains = entity.domains;
			var type = entity.type;
			/*if (!type) {
				if (domains && domains.length>0) {
					type = 
				}
				else
					type = entity.rawName;
			}*/
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
			var definitions = entity.definitions;
			var wikipedia = entity.wikipediaExternalRef;
			var freebase = entity.freeBaseExternalRef;
			var content = entity.rawName; //$(this).text();
			
			var sense = null;
			if (entity.sense)
				sense = entity.sense.fineSense;

			string += "<div class='info-sense-box "+colorLabel+"'><h3 style='color:#FFF;padding-left:10px;'>"+content.toUpperCase()+
				"</h3>";
			string += "<div class='container-fluid' style='background-color:#F9F9F9;color:#70695C;border:padding:5px;margin-top:5px;'>" +
				"<table style='width:100%;background-color:#fff;border:0px'><tr style='background-color:#fff;border:0px;'><td style='background-color:#fff;border:0px;'>";
				
			if (type)	
				string += "<p>Type: <b>"+type+"</b></p>";

			if (sense)
				string += "<p>Sense: <b>"+sense+"</b></p>";
			
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

			if (freebase != null) {
				var urlImage = 'https://usercontent.googleapis.com/freebase/v1/image' + freebase;
				    urlImage += '?maxwidth=150';
				    urlImage += '&maxheight=150';
				    urlImage += '&key=' + api_key;
				string += '<img src="' + urlImage + '" alt="' + freebase + '"/>';
			}		

			string += "</td></tr></table>";

			if ((definitions != null) && (definitions.length > 0)) {
				console.log(definitions[0].definition);
				string += "<p>"+definitions[0].definition+"</p>";
			}
			if ( (wikipedia != null) || (freebase != null) ) {
				string += '<p>Reference: '
				if (wikipedia != null) {
					string += '<a href="http://en.wikipedia.org/wiki?curid=' + 
					wikipedia + 
					'" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" src="resources/img/wikipedia.png"/></a>';
				}
				if (freebase != null) {
					string += '<a href="http://www.freebase.com' + 
					freebase + 
					'" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" src="resources/img/freebase_icon.png"/></a>';
				
				}
				string += '</p>';
			}
		
			string += "</div></div>";
		}
		$('#detailed_annot-0').html(string);	
		$('#detailed_annot-0').show();
	}


	var queryTemplate = { "text" : "", "language" : { "lang" : "en" }, "entities" : [], "onlyNER" : false, 
						  "shortText" : false, "nbest" : false, "sentence" : false, "format" : "JSON",
 						  "customisation" : "generic" };

	function processChange() {
		var selected = $('#selectedService').attr('value');

		if (selected == 'processNERDQuery') {
			setBaseSecondaryUrl('processNERDQuery');
			$("#nerd-text").hide();     
			//$("#default_nerd_query").attr('checked', 'checked');
			//$("#nerd-query").show();
			//$("#lid").hide();
			$('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplate)));
		} 
		else if (selected == 'processNERDQueryScience') {
			setBaseSecondaryUrl('processNERDQueryScience');
			$("#nerd-text").hide();     
			//$("#default_nerd_query").attr('checked', 'checked');
			//$("#nerd-query").show();
			//$("#lid").hide();
			$('#input').attr('value', vkbeautify.json(JSON.stringify(queryTemplate)));
		} 
	}
	
	/*function createInputFile(selected) {
		//$('#label').html('&nbsp;'); 
		$('#textInputDiv').hide();
		//$('#fileInputDiv').fileupload({uploadtype:'file'});
		//$('#fileInputDiv').fileupload('reset');
		$('#fileInputDiv').show();
		
		$('#gbdForm').attr('enctype', 'multipart/form-data');
		$('#gbdForm').attr('method', 'post'); 
	}*/
	
	function createInputFile(selected) {
		$('#label').html('Select a pdf file');
		$('#input').remove();
		$('#field').append(
				$('<input/>').attr('type', 'file').attr('id', 'input').attr(
						'name', 'input'));				
		
	}


	/*function createInputTextArea(nameInput) {
		//$('#label').html('Enter ' + nameInput);             
		$('#label').html('&nbsp;'); 					                   
		$('#input').remove();

		$('#field').append('<table><tr><td><textarea class="span7" rows="5" id="input" name="'+nameInput+'" /></td>'+
			"<td><span style='padding-left:20px;'>&nbsp;</span></td>"+
		"<td><table><tr style='line-height:130%;'><td><span id='example1' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 1' data-content='"+textExamples[0]+"' data-trigger='hover'>example_1</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='example2' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 2' data-content='"+textExamples[1]+"' data-trigger='hover'>example_2</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='example3' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 3' data-content='"+textExamples[2]+"' data-trigger='hover'>example_3</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='example4' style='font-size:90%;' rel='popover' data-placement='right' data-original-title='Example 4' data-content='"+textExamples[3]+"' data-trigger='hover'>example_4</span></td></tr>"+		
			"</table></td>"+
			"<td><span style='padding-left:20px;'>&nbsp;</span></td>"+
		"<td><table><tr style='line-height:130%;'><td><span id='reuters1' style='font-size:90%;'>Reuters_1</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='reuters2' style='font-size:90%;'>Reuters_2</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='reuters3' style='font-size:90%;'>Reuters_3</span></td></tr>"+
		"<tr style='line-height:130%;'><td><span id='reuters4' style='font-size:90%;'>Reuters_4</span></td></tr>"+
			"</table></td>"+
			"</tr></table>");
		// binding of the examples
		$('#example1').popover();
		$('#example1').bind('click',
			function(event) {
				resetExamplesClasses();
			    $(this).addClass('section-active').removeClass('section-non-active');
				var selected = $('#selectedService option:selected').attr('value');
				if (selected == 'processNERDQuery') {
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
				if (selected == 'processNERDQuery') {
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
				if (selected == 'processNERDQuery') {
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
				if (selected == 'processNERDQuery') {
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
				if (selected == 'processNERDQuery') {
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
				if (selected == 'processNERDQuery') {
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
				if (selected == 'processNERDQuery') {
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
				if (selected == 'processNERDQuery') {
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
	}*/
                         
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
		if("fr.inria.nerd.service.admin.pw"==selectedAdmKey.split('-').join('.')){
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
	
	function changePropertySuccesful(responseText, statusText) {
		$("#"+selectedAdmKey).find("div").html(responseText);
		$('#admMessage').html("<font color='green'>Property "+selectedAdmKey.split('-').join('.')+" updated with success</font>");
	}
	
	function changePropertyError() {
		$('#admMessage').html("<font color='red'>An error occured while updating property"+selectedAdmKey.split('-').join('.')+"</font>");
	}

})(jQuery);