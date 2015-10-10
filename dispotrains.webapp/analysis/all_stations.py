#!/bin/env python3
"""
Extracts all metro and RER stations from an OSM dump.
"""

import xml.etree.cElementTree as ET
import argparse
import csv
from math import radians, cos, sin, asin, sqrt

class Station(object):
    """A train station"""
    def __init__(self, name, osm_id, lat, lon, accessible=False):
        self._name = name
        self._osm_ids = set([int(osm_id)])
        self._lat = lat
        self._lon = lon
        self._accessible = accessible

    @property
    def name(self):
        """Name of the station."""
        return self._name

    @property
    def osm_ids(self):
        """OpenStreetMap ID"""
        return self._osm_ids

    @property
    def lat(self):
        """Latitude of the station."""
        return self._lat

    @property
    def lon(self):
        """Longitude of the station."""
        return self._lon

    @property
    def accessible(self):
        """True if the station is accessible."""
        return self._accessible

    def distance(self, other):
        """
        Calculate the great circle distance between two points
        on the earth (specified in decimal degrees)
        """
        # convert decimal degrees to radians
        lon1, lat1, lon2, lat2 = [radians(x) for x in \
                [self.lon, self.lat, other.lon, other.lat]]

        # haversine formula
        dlon = lon2 - lon1
        dlat = lat2 - lat1
        a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
        c = 2 * asin(sqrt(a))
        r = 6371.0 # Radius of earth in kilometers. Use 3956 for miles
        return c * r

    def merge(self, other):
        self._osm_ids.update(other.osm_ids)

    @staticmethod
    def from_node(node):
        """Creates a Station from an XML node in OSM format."""
        name_tags = node.findall("./tag[@k='name']")
        name = None
        if len(name_tags) != 0 :
            name = name_tags[0].get("v")
        osm_id = node.get("id")
        lat = float(node.get("lat"))
        lon = float(node.get("lon"))
        return Station(name, osm_id, lat, lon)

    def __repr__(self):
        return "Station(%d)" % (self.osm_id)

    def __eq__(self, other):
        if isinstance(other, Station):
            return self.osm_id == other.osm_id
        else:
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.__repr__())


def extract_stations_from_dump(dump_path):
    """Extract a list of |Station|s from an XML dump."""
    tree = ET.parse(dump_path)
    root = tree.getroot()
    allstation_nodes = root.findall('./node')
    allstations = {}
    for station_node in allstation_nodes:
        station = Station.from_node(station_node)
        if station.name in allstations:
            allstations[station.name].merge(station)
        else:
            allstations[station.name] = station
    return allstations.values()

def extract_accessible_stations(csv_filepath):
    """Extracts stations from a csv file listing accessible stations."""
    stations = []
    with open(csv_filepath) as reader:
        csvreader = csv.reader(reader)
        for row in csvreader:
            stations.append(Station(row[0], row[4], float(row[2]), float(row[3]), True))
    return stations


def merge_stations(all_stations, accessible_stations):
    """Merge two lists of stations."""
    merged_stations = []
    merged_count = 0
    for station1 in all_stations:
        found = False
        for station2 in accessible_stations:
            if len(station1.osm_ids.intersection(station2.osm_ids)):
                merged_stations.append(station2)
                found = True
                merged_count += 1
        if not found and station1.name:
            merged_stations.append(station1)
    print(merged_count)
    return merged_stations


def print_to_csv(stations):
    """Print a list of stations to CSV."""
    with open("full-list.csv", "w") as writer:
        csvwriter = csv.writer(writer)
        csvwriter.writerow(
            ["name", "osm_id", "latitude", "longitude", "accessible"])
        for station in stations:
            csvwriter.writerow(
                [station.name, station.osm_ids, station.lat, station.lon, station.accessible])


def _parse_args():
    """Define and parse command-line arguments."""
    parser = argparse.ArgumentParser(description='Extract station information.')
    parser.add_argument('--osm_dump', type=str,
                        help='Path of the OSM dump containing train stations')
    parser.add_argument('--accessible_csv', type=str,
                        help='Path to the list of accessible stations (CSV)')
    return parser.parse_args()


def _main():
    """Script entry-point."""
    args = _parse_args()
    all_stations = extract_stations_from_dump(args.osm_dump)
    accessible_stations = extract_accessible_stations(args.accessible_csv)
    merged_stations = merge_stations(all_stations, accessible_stations)
    print_to_csv(merged_stations)

if __name__ == '__main__':
    _main()
