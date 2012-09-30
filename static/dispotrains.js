    function displayStation(urlObj, options) {
        var stationId = urlObj.hash.replace(/.*id=/, "");
        var main = $('#gare');
        var header = main.children( ":jqmData(role=header)" );
        var content = main.children( ":jqmData(role=content)" );
        $.mobile.showPageLoadingMsg();

        $.getJSON('http://sterops.pythonanywhere.com/json/GetGare/' + stationId + '/', function(data) {
            header.find( "h1" ).html(data.nom);

            var positionDiv = main.find('#position').find(".data").html(''),
                lignesDiv = main.find('#lignes').find(".data").html(''),
                ascenseursDiv = main.find('#ascenseurs').find(".data").html('');

            var link = $('<a></a>').attr('href', "http://maps.google.com/maps?q="+data["latitude"]+","+data["longitude"]+"&hl=fr");
            var map = $('<img></img>').attr('src', "http://maps.googleapis.com/maps/api/staticmap?center="+data["latitude"]+","+data["longitude"]
                                               + "&zoom=14&size=" + ($("#gare").width() - 45) + "x310&maptype=roadmap&markers=color:blue%7Clabel:G%7C"+data["latitude"]+","+data["longitude"]
                                               + "&sensor=false").appendTo(link);
            positionDiv.append("Gare de " + data["nom"] + " (" + data["ville"] + ", " + data["departement"] + ")").append($("<p></p>").append(link));

            var lignesUl = $('<ul>').appendTo(lignesDiv).attr("data-role", "listview").attr("data-inset", "false").attr("data-filter", "false");
            $.each(data["arrets"], function(index, ligne) {
                var li = $("<li></li>");
                var a = $("<a></a>").appendTo(li);
                a.id = ligne["id"];
                a.attr("href", "#ligne?id=" + ligne["id"]);
                a.append(ligne["reseau"] + " " + ligne["ligne"]);
                li.appendTo(lignesUl);
            });

            ascenseursUl = $('<ul>').appendTo(ascenseursDiv).attr("data-role", "listview").attr("data-inset", "false").attr("data-filter", "false");
            $.each(data["ascenseurs"], function(index, ascenseur) {
                var li = $("<li></li>");
                li.append('<h3 style="white-space: normal">' + ascenseur["situation"] + "</h3>");

                if (ascenseur["code"] != "rampe") {
                    li.append("<p>" + ascenseur["direction"] + ' - Situation en date du <strong>' + ascenseur["maj"] + '</strong></p><p class="ui-li-aside">' + ascenseur["status"]+'</p>');
                } else {
                    li.append("<p>" + ascenseur["direction"] + "</p>");
                }
                if (ascenseur["status"] != "Disponible" && ascenseur["code"] != "rampe") {
                    li.attr("data-icon", "alert");
                    li.attr("data-theme", "e");
                }
                li.appendTo(ascenseursUl);
            });

            main.page();
            content.find( ":jqmData(role=listview)" ).listview();

            // We don't want the data-url of the page we just modified
            // to be the url that shows up in the browser's location field,
	    	// so set the dataUrl option to the URL for the category
		    // we just loaded.
		    options.dataUrl = urlObj.href;

            // Do the page change
            $.mobile.hidePageLoadingMsg();
            $.mobile.changePage( main, options );
        });
    }

    function displayStationsByLine(urlObj, options) {
        var lineId = urlObj.hash.replace(/.*id=/, "");

        var main = $('#ligne');
        if (main.attr("ligneId") == lineId) {
            // Do the page change
            $.mobile.changePage( main, options );
            return;
        }

        var header = main.children( ":jqmData(role=header)" );
        var content = main.children( ":jqmData(role=content)" );

        $.mobile.showPageLoadingMsg();

        $.getJSON('http://sterops.pythonanywhere.com/json/GetGaresParLigne/' + lineId + '/', function(data) {
            content.html('');

            var reseau = data["reseau"];
            var ligne = data["ligne"];

            header.find( "h1" ).html(reseau + " " + ligne);

            var today = new Date();
            if (today.getFullYear() != data["maj"][0] || today.getMonth() + 1 != data["maj"][1] || today.getDate() != data["maj"][2]) {
                var butterBar = $('<div>').attr("class", "ui-body ui-body-e").html("Dernière mise à jour le " + data["maj"][2] + "/" + data["maj"][1] + "/" + data["maj"][0]).appendTo(content).css("margin", "0px 0px 25px 0px");
            }

            var ul = $('<ul>').appendTo(content).attr("data-role", "listview").attr("data-inset", "true").attr("data-filter", "true");

            $('<li></li>').attr("data-role", "list-divider").html("Avec dysfonctionnement(s)").appendTo(ul);
            $.each(data["gares_pb"], function(index, gare) {
                var li = $("<li></li>");
                var a = $("<a></a>").appendTo(li);
                a.id = gare["id"];
                a.attr("href", "#gare?id=" + gare["id"]);
                if (gare["status"]) {
                    //li.addClass("gareOK");
                } else {
                    li.attr("data-icon", "alert");
                    li.attr("data-theme", "e");
                }
                a.append(gare["nom"]);
                li.appendTo(ul);
            });
            $('<li></li>').attr("data-role", "list-divider").html("En fonctionnement").appendTo(ul);
            $.each(data["gares_ok"], function(index, gare) {
                var li = $("<li></li>");
                var a = $("<a></a>").appendTo(li);
                a.id = gare["id"];
                a.attr("href", "#gare?id=" + gare["id"]);
                if (gare["status"]) {
                    //li.addClass("gareOK");
                } else {
                    li.attr("data-icon", "alert");
                    li.attr("data-theme", "e");
                }
                a.append(gare["nom"]);
                li.appendTo(ul);
            });
            main.attr("ligneId", lineId);
            main.page();
            content.find( ":jqmData(role=listview)" ).listview();

            // We don't want the data-url of the page we just modified
    		// to be the url that shows up in the browser's location field,
	    	// so set the dataUrl option to the URL for the category
		    // we just loaded.
		    options.dataUrl = urlObj.href;

            // Do the page change
            $.mobile.hidePageLoadingMsg();
            $.mobile.changePage( main, options );
        });
    }

    function displayLines(urlObj, options) {
        var main = $("#main");
        var content = main.children( ":jqmData(role=content)" );
        if (content.html() != "") {
            // Do the page change
            $.mobile.changePage( main, options );
            return;
        }

        $.getJSON('http://sterops.pythonanywhere.com/json/GetLignes/', function(data) {
            content.html('');
            var ul = $('<ul>').appendTo(content).attr("data-role", "listview").attr("data-inset", "true").attr("data-filter", "false");

            $.each(data, function(index, ligne) {
                var li = $("<li></li>");
                var a = $("<a></a>").appendTo(li);
                a.id = ligne["id"];
                a.attr("href", "#ligne?id=" + ligne["id"]);
                a.append(ligne["reseau"] + ' ' + ligne["ligne"]);
                li.appendTo(ul);
            });

            main.page();

            content.find( ":jqmData(role=listview)" ).listview();

        	// We don't want the data-url of the page we just modified
    		// to be the url that shows up in the browser's location field,
	    	// so set the dataUrl option to the URL for the category
		    // we just loaded.
		    options.dataUrl = urlObj.href;

            // Do the page change
            $.mobile.changePage( main, options );
        });
    }

// Listen for any attempts to call changePage().
$(document).bind( "pagebeforechange", function( e, data ) {
    // We only want to handle changePage() calls where the caller is
	// asking us to load a page by URL.
	if ( typeof data.toPage === "string" ) {
		// We are being asked to load a page by URL, but we only
		// want to handle URLs that request the data for a specific
		// category.
		var u = $.mobile.path.parseUrl( data.toPage );
        _gaq.push(['_trackPageview', data.toPage]);
		if ( u.hash.indexOf("#ligne") == 0 ) {
			// Display one train line
            displayStationsByLine( u, data.options );

			// Make sure to tell changePage() we've handled this call so it doesn't
			// have to do anything.
        	e.preventDefault();
		} else if (u.hash.indexOf("#gare") == 0) {
            displayStation(u, data.options);

            e.preventDefault();

		} else if (u.hash == "") {
    	    displayLines(u, data.options);
            // Make sure to tell changePage() we've handled this call so it doesn't
			// have to do anything.
            e.preventDefault();
		}
	}
});

$(document).ready(function() {
    if ($.cookie('already_there') != null || window.location.hash) {
        displayLines();
    } else {
        $.mobile.changePage( $('#help'), {} );
        // Make sure to tell changePage() we've handled this call so it doesn't
        // have to do anything.
        // e.preventDefault();
    }
    $.cookie('already_there', 'yes', { expires: 60, path: '/' });
    $(document).bind('swiperight', function() {history.back()});
    });

