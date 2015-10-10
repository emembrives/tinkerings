#!/usr/bin/python
# vim: set fileencoding=utf-8 :

import xml.etree.ElementTree as ET

def prepare_names(raw_name):
    name = ' ' + raw_name.lower() + ' '
    name = name.replace('-', ' ')
    name = name.replace(' st ', ' saint ')
    name = name.replace(u'é', 'e')
    name = name.replace(u'è', 'e')
    name = name.replace(u'ê', 'e')
    name = name.replace(u'ë', 'e')
    name = name.replace(u'ô', 'o')
    name = name.replace(u'à', 'a')
    name = name.replace(u'ç', 'c')
    return name.strip()


class OSMStation(object):
    def __init__(self, node, missing_name):
        self.osm_id = int(node.attrib["id"])
        self.lat = float(node.attrib["lat"])
        self.lon = float(node.attrib["lon"])
        name_found = False
        for tag in node:
            assert tag.tag == 'tag'
            if tag.attrib["k"] == "name":
                self.name = tag.attrib["v"]
                name_found = True
                break
        if not name_found:
            missing_name[self.osm_id] = self
        
    def __init(self, name, lat, lon, osm_id):
        self.name = name
        self.lat = lat
        self.lon = lon
        self.osm_id = osm_id

    def attach(self, station):
        self.dispotrains_station = station

    @property
    def nameset(self):
        return set(self.name.lower().split(' '))

    def str(self):
        return self.name + ' (' + str(self.osm_id) + ')'


tree = ET.parse('stations.osm')

root = tree.getroot()

missing_names = {}
osm_stations = [OSMStation(node, missing_names) for node in root if node.tag == 'node']

for way in root:
    if way.tag != 'way':
        continue
    match = False
    for nd in way:
        if nd.tag != 'nd':
            continue
        ref = int(nd.attrib['ref'])
        if ref in missing_names:
            match = True
            break
    if not match:
        continue
    for tag in way:
        if tag.tag != 'tag':
            continue
        if tag.attrib["k"] == 'name':
            missing_names[ref].name = tag.attrib["v"]
            del missing_names[ref]
            break

for s in missing_names.values():
    osm_stations.remove(s)

##################################################

import json

dispotrains_stations = []
with open('stations.json') as f:
    for line in f.readlines():
        station = json.loads(line.strip())
        if len(station["lines"]) == 1 and station["lines"][0]["id"] == "Tzen1":
            # TZen 1 is a bus line
            continue
        if station["name"] == "GARE DE GISORS":
            # Gisors is not in Ile-de-France
            continue
        dispotrains_stations.append(station)

##################################################

from math import radians, cos, sin, asin, sqrt

def haversine(lon1, lat1, lon2, lat2):
    """
    Calculate the great circle distance between two points
    on the earth (specified in decimal degrees)
    """
    # convert decimal degrees to radians 
    lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])

    # haversine formula 
    dlon = lon2 - lon1 
    dlat = lat2 - lat1 
    a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
    c = 2 * asin(sqrt(a)) 
    r = 6371.0 # Radius of earth in kilometers. Use 3956 for miles
    return c * r


OSM_MANUAL = {
u"Gare de Cergy Préfecture": 2320446018,
u"CITE UNIVERSITAIRE": 2656855599,
u"Cité Universitaire": 1773529787, # Tram
u"Gare de Cergy St Christophe": 2320446019,
u"LA COURNEUVE - 8 MAI 1945": 270244850,
u"Maurice Lachâtre": 2283276387,
u"GARE D'ACHERES VILLE": 2320446016,
u"Gare de vaucresson": 415275831,
u"GARE DE NEUVILLE UNIVERSITE": 2320446021,
u"GARE DE NOISY CHAMPS": 195306266,
u"Aéroport CDG Terminal 2 TGV": 225119209,
u"GARE DE VAL D EUROPE": 1059567460,
u"GARE DE ST GERMAIN EN LAYE": 87360108,
u"GARE DE ST GERMAIN EN LAYE GRANDE CEINTURE": 2315855073,
u"GARE DE SEVRAN BEAUDOTTES": 3571816329,
u"CHATELET LES HALLES": 3190883103,
u"GARE DE TORCY MARNE LA VALLEE": 245282439,
        }


unmatched_stations = osm_stations[:]

for station in dispotrains_stations:
    best_station = None
    best_station_val = 1
    for osm_station in unmatched_stations:
        if station['name'] in OSM_MANUAL and osm_station.osm_id == OSM_MANUAL[station['name']]:
            station['osm'] = osm_station
            station['d'] = haversine(float(station['position']['longitude']),
                    float(station['position']['latitude']),
                    osm_station.lon,
                    osm_station.lat)
            osm_station.attach(station)
            unmatched_stations.remove(osm_station)
            break
        d = haversine(float(station['position']['longitude']),
                      float(station['position']['latitude']),
                      osm_station.lon,
                      osm_station.lat)
        if d < best_station_val:
            best_station_val = d
            best_station = osm_station
    if best_station != None:
        unmatched_stations.remove(best_station)
        station['osm'] = best_station
        station['d'] = best_station_val
        best_station.attach(station)

import csv
with open('stations-coordinates.csv', 'w') as f:
    writer = csv.writer(f)
    for station in dispotrains_stations:
        writer.writerow([station['name'].encode('utf-8'), station['city'].encode('utf-8'), station['osm'].lat, station['osm'].lon, station['osm'].osm_id])
    

