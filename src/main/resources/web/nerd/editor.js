/**
*  Javascript functions for the front end experimental editor.
*        
*  Author: Patrice Lopez
*/

var editor = (function($) {
    var conceptMap = new Object();
	var sentences = null;
	var entities = null;
	var marked = [];
	var onlyNER = false;
	var lang = null;
	
	// this timout timer will call automatically the service, independently from the amount of changes
	var currentTimer = null;
	// true if an automatic call has been realised without any changes, the logic being: if a call is realised
	// without any changes and there is no more changes inbetween the new call, the call is not realised.
	var autoCallDone = false;
	
	// for managing the call to the NERD service
	// amount of text typed since last call of NERD 
	var accumulated = null;
	// time from last call of the NERD service in millisecond
	var lastTimeCall = new Date();
	// average response time of the nerd service in millisecond
	var averageNERDCallTime = 5000;
	var modifiedSentences = [];
	
	var subToolBar = '<div id=\"subToolBar\"> \
		<table cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:collapse;border:none;width:100%;background-color:#FFF; height:83px;border:0px;border-top:0px;border-bottom:1px;\"> \
		<tr style=\"background-color:#FFF;border:0px;heigth:100%;border-bottom:1px;border-top:0px;\"> \
		<td style=\"width:100px;vertical-align:top;border-top:0px;border-bottom:1px;\"> \
		<select class="form-control" style=\"height:auto;width:auto;right:0px;\" name=\"customisation\" id=\"customisation\"> \
		<option value="generic" selected>generic</option> \
		</select> \
		</td><td style=\"width:100px;vertical-align:top;border-top:0px;border-bottom:1px;\"> \
		<div class=\"checkbox checkbox-inline checkbox-danger\"> \
		<input type=\"checkbox\" id=\"onlyNER\" name=\"onlyNER\" value=\"0\"> \
		<label for=\"onlyNER\"> only NER </label></div> \
		</td><td style="width:100px;border-top:0px;border-bottom:1px;"></td></tr> \
		</table></div>';
		
	$(document).ready(function() {   
		// components / detailed / simple
		$("#divAbout").hide();
		$("#divEditor").show();
		$("#divDoc").hide();
		
		$("#about").click(function() {
			$("#about").attr('class', 'section-active');
			$("#editor").attr('class', 'section-non-active');
			
			$("#divAbout").show();
			$("#divEditor").hide();
			$("#divDoc").hide();
			return false;
		});
		$("#editor").click(function() {
			$("#editor").attr('class', 'section-active');
			$("#about").attr('class', 'section-non-active');

			$("#divEditor").show();  
			$("#divAbout").hide();
			$("#divDoc").hide();
			return false;
		});
		
		$('#nerdToolBar').append(subToolBar);
		
		// extend customisation field with the registered existing ones
		$.ajax({
		  type: 'GET',
		  url: 'service/customisations',
//		  data: { text : $('#input').val() },
		  success: fillCustumisationField,
		  error: AjaxError,
		  contentType:false  
		});
	});

	function fillCustumisationField(response, statusText) { 
		if (response) {
			for(var ind in response) {
				var option = '<option value=\"'+response[ind]+'\">'+response[ind]+'</option>';	
				$('#customisation').append(option);
			}
		}
	}

	firepad.on('ready', function() {
		// Firepad is ready.
		// add the NERD parameters around
		//$('.firepad-toolbar').append(subToolBar);

		console.log('Firepad is ready');
		var startUpText = 'After marching through Belgium, Luxembourg and the Ardennes, the German Army advanced, in the latter half of August, into northern France where they met both the French army, under Joseph Joffre, and the initial six divisions of the British Expeditionary Force, under Sir John French.\nA series of engagements known as the Battle of the Frontiers ensued. Key battles included the Battle of Charleroi and the Battle of Mons. In the former battle the French 5th Army was almost destroyed by the German 2nd and 3rd Armies and the latter delayed the German advance by a day.'; 
		firepad.setText(startUpText);
		codeMirror.focus();

		// annotate the whole start-up text
		var lines = startUpText.split("\n");
		var posLine = 0;
		sentences = [];
		for(var n=0; n<lines.length; n++) {
			var positions = new Object();
			positions.offsetStart = posLine;
			positions.offsetEnd = posLine + lines[n].length;
			posLine = posLine + lines[n].length + 1; // +1 is for the end of line character
			sentences.push(positions);
		}
	 	var query = new Object();
		query.text = startUpText;
		query.sentences = sentences;
		query.onlyNER = onlyNER;
		query.nbest = false;
		query.customisation = 'generic';

        var formData = new FormData();
        formData.append("query", JSON.stringify(query));

		$.ajax({
		  	type: 'POST',
		  	url: 'service/disambiguate',
		  	data: formData,
		  	beforeSubmit: setTimeCall(),
		  	success: SubmitSuccesfulNERD,
		  	error: AjaxError,
			processData: false,
            contentType: false
		});

		//codeMirror
		codeMirror.on('change',function(cMirror, changeObject) {
			updateAfterChange(cMirror, changeObject);
		});
			
	});		
		
	firepad.on('synced', function(isSynced) {		
		console.log('Firepad is not yet sync');	
		
		if (entities != null) {
			//codeMirror.focus();

			for(var m=entities.length-1; m>=0; m--) {
				$('.annot-'+m).bind('hover', viewEntity);
				$('.annot-'+m).bind('click', viewEntity);  	
			}
		}
		else {

		}
		
		if (isSynced) {
			console.log('Firepad is sync');	
		}
	});
	
	function updateCallTimer() { 
//console.log('updateCallTimer');	

		// just updating the sentences for sanity 
		var currentText = codeMirror.getValue();
		var lines = currentText.split("\n");
		posLine = 0;
		sentences = [];
		for(var n=0; n<lines.length; n++) {
			var positions = new Object();
			positions.offsetStart = posLine;
			positions.offsetEnd = posLine + lines[n].length;
			posLine = posLine + lines[n].length + 1; // +1 is for the end of line character
			sentences.push(positions);
		}

		if (!autoCallDone) {
			createQuery();
			autoCallDone = true;
		}
		else {
			var somethingNew = true;
			if ( (accumulated == null) || (accumulated.length == 0) ) 
				somethingNew = false;
			if (somethingNew) {
				createQuery();
			}
		}
	}
	
	function updateAfterChange(cMirror, changeObject) {
        $('#requestResult').html("");
		console.log(changeObject);
		autoCallDone = false;
		var line = changeObject.from.line;

		var currentText = codeMirror.getValue();
		var lines = currentText.split("\n");
		var posLine = 0;
		for(var n=0; n<line; n++) {
			posLine += lines[n].length+1; // +1 is for the newline character
		}
		var start = changeObject.from.ch + posLine;

		var end = changeObject.to.ch + posLine;

		var nbModifiedChar = changeObject.text[0].length - changeObject.removed[0].length;
//console.log('nbModifiedChar = ' + nbModifiedChar);		

		// special case of the *addition* of a new line (does not appear in the change event!)
		if (nbModifiedChar == 0) {
			if ( (changeObject.from.ch == changeObject.to.ch) && (changeObject.from.line == changeObject.to.line) &&
			     (changeObject.origin == "+input") && (changeObject.text.length == 2) && 
				 (changeObject.text[0] == '') && (changeObject.text[1] == '') ) {			
				 // the new line character has to be counted
				nbModifiedChar = 1;
			}
		}
		// which sentence is changed?
		var modifiedSentence = -1;
		
		posLine = 0;
		sentences = [];
		
		// this time special case now of the *removal* of a line (does not really appear in the change event!)
//console.log(nbModifiedChar);	
		if (nbModifiedChar == 0) {
			if ( (changeObject.from.ch != changeObject.to.ch) && (changeObject.from.line+1 == changeObject.to.line) &&
			     (changeObject.origin == "+delete") && (changeObject.removed.length == 2) && 
				 (changeObject.removed[0] == '') && (changeObject.removed[1] == '') &&
			     (changeObject.text.length == 1) && (changeObject.text[0] == '') ) {	
				modifiedSentence = changeObject.from.line;
				var pp = $.inArray(modifiedSentence+1, modifiedSentences);
				if (pp != -1) {
					modifiedSentences.splice(pp, 1);
				}
			}
		}
		
		for(var n=0; n<lines.length; n++) {
			var positions = new Object();
			positions.offsetStart = posLine;
			positions.offsetEnd = posLine + lines[n].length;
			posLine = posLine + lines[n].length + 1; // +1 is for the end of line character
			sentences.push(positions);
			
			if (modifiedSentence == -1) {
				if (positions.offsetStart <= start) {
					if (positions.offsetEnd >= start) {
						modifiedSentence = n;
					}
				}
			}
		}
		
		if (modifiedSentence == -1)
			modifiedSentence = sentences.length - 1;
		if ($.inArray(modifiedSentence, modifiedSentences) == -1) {
			modifiedSentences.push(modifiedSentence);
		}					
		
		if (entities != null) {
			// we update the entity positions before the place of change
			for(var m=0; m<entities.length; m++) {

				var entityStart = entities[m].offsetStart;
				var entityEnd = entities[m].offsetEnd;
				if (entityStart > start) {
					// entities before the change are untouched, we focus on those after
					entities[m].offsetStart = entityStart + nbModifiedChar;
					entities[m].offsetEnd = entityEnd + nbModifiedChar;
				}
				else if (entityEnd > start) {
					entities[m].offsetEnd = entityEnd + nbModifiedChar;
				}
			}
		}
		
	  	// get value right from instance
		//for(var obj in changeObjects) {
		accumulated += changeObject.text;
		var now = new Date();
		//var entities = [];
		
		if ( (accumulated.length > 15) || 
			 (now - lastTimeCall > 3*averageNERDCallTime ) ) {
			createQuery();
		}
		else {
			if (entities != null) {
				//codeMirror.focus();

				for(var m=entities.length-1; m>=0; m--) {
					$('.annot-'+m).bind('hover', viewEntity);
					$('.annot-'+m).bind('click', viewEntity);  	
				}
			}
		}
	}
	
	function createQuery() {
		// neutralise the current timer
		if (currentTimer != null) {
			clearTimeout(currentTimer);
		}
	 	var query = new Object();
		query.text = codeMirror.getValue();
		//query.text = firepad.getText();
		query.sentences = sentences;
		query.entities = entities;
		query.onlyNER = onlyNER;
		query.nbest = false,
		query.processSentence = modifiedSentences;
		query.customisation = 'generic';
		// we call the NERD service via the NERD Query structure
		
		// we don't send the entities of the sentence(s) to be processed!
		if (sentences != null) { 
			for(var ind=0; ind<modifiedSentences.length; ind++) {
				if (!sentences[modifiedSentences[ind]]) {
					console.log(modifiedSentences);
					console.log("Warning: sentence at index " + ind + " is invalid");
				}
				else {
					var theStart = sentences[modifiedSentences[ind]].offsetStart;
					var theEnd = sentences[modifiedSentences[ind]].offsetEnd;
					var toBeRemoved = [];
					if (entities != null) {
						for(var ind2=0; ind2<entities.length; ind2++) {
							var entityStart = entities[ind2].offsetStart;
							var entityEnd = entities[ind2].offsetEnd;
							if ( (entityEnd < theStart) || (entityStart > theEnd) ) {
								continue;
							}
							else {
								toBeRemoved.push(ind2);
							}
						}
						
						for(var m=entities.length-1; m>=0; m--) {
							if ($.inArray(m, toBeRemoved) != -1)
								query.entities.splice(m, 1);
						}
					}
				}
			}
		}

        var formData = new FormData();
        formData.append("query", JSON.stringify(query));
		
		$.ajax({
		  	type: 'POST',
		  	url: 'service/disambiguate',
	 	 	data: formData,
		  	beforeSubmit: setTimeCall(),
		  	success: SubmitSuccesfulNERD,				  
		  	error: AjaxError,
            processData: false,
            contentType: false
		  	// dataType: "text"
		});
		accumulated = "";
		processSentence = [];
	}
	
	function SubmitSuccesfulSentenceSegmentation(responseText, statusText) {     
		sentences = responseText;
		if ( (sentences == null) || (sentences.length == 0) ){
			$('#requestResult')
				.html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>"); 
			return;
		}
console.log(sentences);	
	};
	
	function setTimeCall() {
		lastTimeCall = new Date();
	}
	
	function SubmitSuccesfulNERD(responseText, statusText) {    
		var timeForCall = new Date() - lastTimeCall;
		averageNERDCallTime = (timeForCall + averageNERDCallTime) / 2;

		var responseJson = responseText;
		if ( (responseJson == null) ){
			$('#requestResult')
				.html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");   
			return;
		}

		lang = 'en'; //default
 		var language = responseJson.language;
 		if (language)
 			lang = language.lang;

//console.log(responseJson);		
		sentences = responseJson.sentences;
		
		if (sentences == null) {
			responseJson = $.parseJSON(responseJson);
			sentences = responseJson.sentences;
		}
		
		localProcessSentence = responseJson.processSentence;
		
		var string = responseJson.text;
		var newEntities = false;
		if (responseJson.entities) {
			// do we have new entities added?
			if (entities != null) {
				if (responseJson.entities.length > entities.length) {
					newEntities = true;
				}
			}
			// unmark 
			if (marked.length > 0) {
				for (var i = 0; i < marked.length; ++i) 
					marked[i].clear();
				marked.length = 0;
			}

			var indexLine = 0;
			var currentEndOfLine = sentences[indexLine].offsetEnd;
			//for(var m=responseJson.entities.length-1; m>=0; m--) {
			for(var m=0; m<responseJson.entities.length; m++) {	
				var entity = responseJson.entities[m];

				var identifier = entity.wikipediaExternalRef;

                if (identifier && (conceptMap[identifier] == null)) {
                    $.ajax({
                        type: 'GET',
                        url: 'service/kb/concept/'+identifier+'?lang='+lang,
                        success: function(result) { conceptMap[result.wikipediaExternalRef] = result; },
                        dataType: 'json'
                    });
                }


				var label = entity.type;	
				if (!label)
					label = entity.rawName;
		    	var start = parseInt(entity.offsetStart,10);
			    var end = parseInt(entity.offsetEnd,10);   
				
				if (end > currentEndOfLine) {
					indexLine++;
					if (indexLine < sentences.length) {
						currentEndOfLine = sentences[indexLine].offsetEnd;
					}
				}
				
				if (indexLine < sentences.length) {
					var theStart = start - sentences[indexLine].offsetStart;
					var theEnd = end - sentences[indexLine].offsetStart;
		
					// do we need to adjust the mark boundaries? 
					// due to the fact that currently we miss some change event from codeMirror
					var currentText = codeMirror.getValue();
					if ( (currentText.charAt(start) == ' ') && (currentText.charAt(end) != ' ') &&
						 (currentText.length >= end+1) ) {
						if ( (currentText.charAt(start+1) != ' ') &&
							 (currentText.charAt(end+2) == ' ') ) {
							// we can safely shift by +1
							theStart += 1;
							theEnd += 1;	
						} 
					}
					else if ( (currentText.charAt(start) != ' ') && (currentText.charAt(end) == ' ') &&
						      (start >= 2) ) {
						if ( (currentText.charAt(start-2) == ' ') && 
						     (currentText.charAt(end-1) != ' ') ) {
							// we can shift by -1
							theStart -= 1;
							theEnd -= 1;
						}
					}

					marked.push(codeMirror.markText({line:indexLine,ch:theStart},{line:indexLine,ch:theEnd}, 
						{startStyle:'label '+label, endStyle:'', className:'annot-'+m}));
				}
			}
			//codeMirror.focus();
		
			for(var m=responseJson.entities.length-1; m>=0; m--) {
				$('.annot-'+m).bind('hover', viewEntity);
				$('.annot-'+m).bind('click', viewEntity);  	
			}
			entities = responseJson.entities;
			//console.log(entities);
		}
		
		// possibly auto select a new entity to be viewed
		if (newEntities) {
			var cursorPos = codeMirror.getCursor();
			var cursorLine = cursorPos.line;
			var cursorCh = cursorPos.ch;
			var cursorChar = 0;
			for (var m=0; (m<cursorLine) && (m<sentences.length); m++) {
				cursorChar += sentences[m].offsetEnd; 
				console.log('cursor offset: ' + cursorChar);
			}
			cursorChar += cursorCh;
			// find the closest entity that comes just before the cursor
			var bestEntityIndex = -1;
			for(var m=0; m<entities.length; m++) {
				var theEntity = entities[m];
				var start = parseInt(theEntity.offsetStart,10); 
				if (start > cursorChar) {
					break;
				}
				bestEntityIndex = m;
			}
			// raise a view event on this entity
			if (bestEntityIndex != -1) {
				$('.annot-'+bestEntityIndex).trigger('click');
			}
		}
		
		lastTimeCall = new Date();
		
		// register new automatic call
		var delay = Math.floor(5*averageNERDCallTime);
		if (onlyNER) {
			if (delay < 3000) {
				delay = 3000;
			}
		}	
		else if (delay < 5000) {
			delay = 5000;
		}
		currentTimer = setTimeout(updateCallTimer, delay);
console.log('set up timer at ' + delay + ' ms');
	};
	
	function AjaxError() {
		$('#requestResult').html("<font color='red'>Error encountered while requesting the server.</font>");      
		entities = null;
	}

	function viewEntity() {
		var localID = $(this).attr('class');
		if (entities == null) {
			return;
		}

		var ind1 = localID.indexOf('-');
		var ind2 = localID.indexOf(' ', ind1);
		if (ind2 == -1)
			ind2 = localID.length;
		else 
			ind2 = ind2;
	
		var localEntityNumber = parseInt(localID.substring(ind1+1,ind2));
		var string = "";
		/*for(var entityListIndex=entityMap[localEntityNumber].length-1; 
				entityListIndex>=0; 
				entityListIndex--) {*/

			var entity = entities[localEntityNumber];


		/*var localID = $(this).attr('id');

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

			//var entity = responseJson.entities[localEntityNumber];
			var entity = entityMap[localEntityNumber][entityListIndex];*/
			var domains = entity.domains;
			var type = entity.type;
        var wikipedia = entity.wikipediaExternalRef;
        var wikidataId = entity.wikidataId;

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
        	var definitions = getDefinitions(wikipedia);

			var wikipedia = entity.wikipediaExternalRef;
			//var freebase = entity.freeBaseExternalRef;
			var content = entity.rawName;
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

			/*if (freebase != null) {
				var urlImage = 'https://usercontent.googleapis.com/freebase/v1/image' + freebase;
				    urlImage += '?maxwidth=150';
				    urlImage += '&maxheight=150';
				    urlImage += '&key=' + api_key;
				string += '<img src="' + urlImage + '" alt="' + freebase + '"/>';
			}*/		

			string += '<span id="img-' + wikipedia + '"><script type="text/javascript">lookupWikiMediaImage("'+wikipedia+'")</script></span>';

			string += "</td></tr></table>";

			if ((definitions != null) && (definitions.length > 0)) {
				//console.log(definitions[0].definition);
				//var localHtml = (definitions[0]['definition']).wiki2html(lang);
				var localHtml = wiki2html(definitions[0]['definition'], lang);
				string += "<p><div class='wiky_preview_area2'>"+localHtml+"</div></p>";
			}
			if (wikipedia != null) {
				string += '<p>Reference: '
				if (wikipedia != null) {
					string += '<a href="http://'+lang+'.wikipedia.org/wiki?curid=' + 
					wikipedia + 
					'" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" '+
					' src="resources/img/wikipedia.png"/></a>';
				}
                if (wikidataId != null) {
                    string += '<a href="https://www.wikidata.org/wiki/' +
                        wikidataId +
                        '" target="_blank"><img style="max-width:28px;max-height:22px;margin-top:5px;" '+
                        ' src="resources/img/Wikidata-logo.svg"/></a>';
                }
				string += '</p>';
			}
		
			string += "</div></div>";
		//}
		$('#detailed_annot-0').html(string);	
		$('#detailed_annot-0').show();
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
			if (lang == 'fr')
				theUrl = wikimediaURL_FR + wikipedia;
			else if (lang == 'de')
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

	function viewEntity2() {
		var localID = $(this).attr('class');
		if (entities == null) {
			return;
		}

		var ind1 = localID.indexOf('-');
		var ind2 = localID.indexOf(' ', ind1);
		if (ind2 == -1)
			ind2 = localID.length;
		else 
			ind2 = ind2;
	
		var localEntityNumber = parseInt(localID.substring(ind1+1,ind2));
		var string = "";
		/*for(var entityListIndex=entityMap[localEntityNumber].length-1; 
				entityListIndex>=0; 
				entityListIndex--) {*/

			var entity = entities[localEntityNumber];
			//var entity = entityMap[localEntityNumber][entityListIndex];
//console.log(entity);			
			var type = entity.type;
			if (!type) 
				type = entity.rawName
			var subType = entity.subtype;
			var conf = entity.nerd_score;
			var definitions = entity.definitions;
			var wikipedia = entity.wikipediaExternalRef;
			var freebase = entity.freeBaseExternalRef;
			var content = entity.rawName; //$(this).text();
			var sense = null;
			if (entity.sense)
				sense = entity.sense.fineSense;
				if (sense == 'jurisdictional_cultural_adjective/J1') {
					sense = 'jurisdictional/J1';
				}
			string += "<div class='info-sense-box "+type+"'><h3 style='color:#FFF;padding-left:10px;'>"+content.toUpperCase()+
				"</h3>";
				string += "<div class='container-fluid' style='background-color:#F9F9F9;color:#70695C;border:padding:5px;margin-top:5px;'>" +
				"<table style='width:100%;background-color:#fff;border:0px'><tr style='background-color:#fff;border:0px;'><td style='background-color:#fff;border:0px;'>" +
				"<p>Type: <b>"+
				type+"</b></p>";

			if (sense)
				string += "<p>Sense: <b>"+sense+"</b></p>";
			string += "<p>conf: <i>"+conf+
				"</i></p>";

			string += "</td><td style='align:right;bgcolor:#fff'>";

			if (freebase != null) {
				var urlImage = 'https://usercontent.googleapis.com/freebase/v1/image' + freebase;
				    urlImage += '?maxwidth=150';
				    urlImage += '&maxheight=150';
				    //urlImage += '&key=' + api_key;
				string += '<img src="' + urlImage + '" alt="' + freebase + '"/>';
			}		

			string += "</td></tr></table>";

			if ((definitions != null) && (definitions.length > 0)) {
				//console.log(definitions[0].definition);
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
		
		$('#detailed_annot-0').html(string);	
		$('#detailed_annot-0').show();
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
	
	
	
})(jQuery);