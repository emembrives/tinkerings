function AccessMap(mapEl, captionEl) {
  this._mapEl = mapEl;
  this._captionEl = captionEl;
  this._points = [];
  this._data = undefined;
  this._lastSelectedPoint = undefined;
  this._d3Voronoi = d3.geom.voronoi().x(function(d) {
    return d.x;
  }).y(function(d) { return d.y; });
  this._setupMapbox();
}

AccessMap._DISPOTRAINS_STATIONS =
    "http://dispotrains.membrives.fr/app/GetStations/";
AccessMap._STATIONS_CSV = "full-list.csv";

AccessMap.prototype._setupMapbox = function() {
  L.mapbox.accessToken =
      'pk.eyJ1IjoiZW1lbWJyaXZlcyIsImEiOiIwNDViZWQyODJhNTczNTg4ZWEzNzI4MzllNzk4ODk1NyJ9.ijO7LzQGt_kX1IwAOrUYzA';
  this._map =
      L.mapbox.map(this._mapEl, 'emembrives.d5f86755')
          .fitBounds([ [ 49.241299, 3.55852 ], [ 48.120319, 1.4467 ] ]);

  var self = this;

  var mapLayer = {
    onAdd : function(map) {
      map.on('viewreset moveend', function() { self.loadAndDraw(); });
      self.loadAndDraw();
    }
  };

  this._map.on('ready', function() {
    self.loadAndDraw();
    self._map.addLayer(mapLayer);
  });
};

AccessMap.prototype._getData = function() {
  var availabilityPromise =
      d3.promise.json(AccessMap._DISPOTRAINS_STATIONS)
          .then(function(stations) {
            for (var j = 0; j < stations.length; j++) {
              var d = stations[j];
              var good = true;
              for (var i = 0; i < d.elevators.length; i++) {
                if (d.elevators[i].status.state != "Disponible") {
                  good = false;
                  break;
                }
              }
              d.good = good;
            }
            return stations;
          });
  var stationsPromise = d3.promise.csv(AccessMap._STATIONS_CSV);
  return Promise.all([ availabilityPromise, stationsPromise ])
      .then(this._mergeData);
};

AccessMap.prototype._mergeData = function(values) {
  var availabilities = values[0];

  var stations = values[1];

  var merged_stations = stations.map(function(d) {
    d.accessible = d.accessible === "True";
    if (!d.accessible) {
      return d;
    }
    for (var i = 0; i < availabilities.length; i++) {
      if (d.name === availabilities[i].name) {
        d.name = availabilities[i].displayname;
        d.good = availabilities[i].good;
        return d;
      }
    }
  });
  return merged_stations;
};

AccessMap.prototype.loadAndDraw = function() {
  var self = this;
  if (this._data === undefined) {
    this._data = this._getData();
  }
  return this._data.then(function(p) { self.draw(p); });
};

var metersPerPixel = function(latitude, zoomLevel) {
  var earthCircumference = 40075017;
  var latitudeRadians = latitude * (Math.PI / 180);
  return earthCircumference * Math.cos(latitudeRadians) /
         Math.pow(2, zoomLevel + 8);
};

var pixelValue = function(latitude, meters, zoomLevel) {
  return meters / metersPerPixel(latitude, zoomLevel);
};

AccessMap.prototype.draw = function(points) {
  var self = this;
  d3.select('#overlay').remove();

  var bounds = this._map.getBounds();
  var topLeft = this._map.latLngToLayerPoint(bounds.getNorthWest());
  var bottomRight = this._map.latLngToLayerPoint(bounds.getSouthEast());
  var existing = d3.set();
  var drawLimit = bounds.pad(0.4);

  filteredPoints = points.filter(function(d, i) {
    var latlng = new L.LatLng(d.latitude, d.longitude);

    if (!drawLimit.contains(latlng)) {
      return false
    };

    var point = self._map.latLngToLayerPoint(latlng);

    key = point.toString();
    if (existing.has(key)) {
      return false
    };
    existing.add(key);

    d.x = point.x;
    d.y = point.y;
    return true;
  });

  var svg = d3.select(this._map.getPanes().overlayPane)
                .append("svg")
                .attr('id', 'overlay')
                .attr("class", "leaflet-zoom-hide")
                .style("width", this._map.getSize().x + 'px')
                .style("height", this._map.getSize().y + 'px')
                .style("margin-left", topLeft.x + "px")
                .style("margin-top", topLeft.y + "px")
                .append("g")
                .attr("transform",
                      "translate(" + (-topLeft.x) + "," + (-topLeft.y) + ")");

  var clips = svg.append("svg:g").attr("id", "point-clips");
  var points = svg.append("svg:g").attr("id", "points");
  var paths = svg.append("svg:g").attr("id", "point-paths");

  clips.selectAll("clipPath")
      .data(filteredPoints)
      .enter()
      .append("svg:clipPath")
      .attr("id", function(d, i) { return "clip-" + i; })
      .append("svg:circle")
      .attr('cx', function(d) { return d.x; })
      .attr('cy', function(d) { return d.y; })
      .attr('r', pixelValue(48.8534100, 20000, this._map.getZoom()));

  var datapointFunc = function(datapoint) {
    return "M" + datapoint.join(",") + "Z";
  };
  paths.selectAll("path")
      .data(this._d3Voronoi(filteredPoints))
      .enter()
      .append("svg:path")
      .attr("d", datapointFunc)
      .attr("id", function(d, i) { return "path-" + i; })
      .attr("clip-path", function(d, i) { return "url(#clip-" + i + ")"; })
      .style("stroke", d3.rgb(50, 50, 50))
      .on('click', function(d) { self._selectPoint(d3.select(this), d); })
      .classed("selected", function(d) { return this._lastSelectedPoint == d })
      .classed("inaccessible", function(d) { return !d.point.accessible; })
      .classed("malfunction",
               function(d) { return d.point.accessible && !d.point.good; })
      .classed(
          "ok", function(d) { return d.point.accessible && d.point.good; });

  paths.selectAll("path")
      .on("mouseover",
          function(d, i) {
            self._selectPoint(d3.select(this), d);
            d3.select(this).style('fill', d3.rgb(31, 120, 180));
            svg.select('circle#point-' + i).style('fill', d3.rgb(31, 120, 180))
          })
      .on("mouseout", function(d, i) {
        d3.select(this).style("fill", null);
        svg.select('circle#point-' + i).style('fill', 'black')
      });

  points.selectAll("circle")
      .data(filteredPoints)
      .enter()
      .append("svg:circle")
      .attr("id", function(d, i) { return "point-" + i; })
      .attr("transform",
            function(d) { return "translate(" + d.x + "," + d.y + ")"; })
      .attr("r", 1.5)
      .attr('stroke', 'none');
};

AccessMap.prototype._selectPoint = function(cell, point) {
  d3.selectAll('.selected').classed('selected', false);

  this._lastSelectedPoint = point;
  cell.classed('selected', true);

  d3.select('#selected h1')
      .html('')
      .append('a')
      .text(point.point.name + ", " + point.point.osm_id)
      //    .attr('href', point.url)
      .attr('target', '_blank')
};
