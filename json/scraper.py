#!/usr/bin/python
# -*- coding: utf-8 -*-

import urllib2, urllib
from BeautifulSoup import BeautifulSoup
from datetime import date, timedelta
import itertools

class GareJour(object):
  def __init__(self, jour, gare, gare_code):
    day, month, year = jour.split("/")
    self.__jour = date(int(year), int(month), int(day))
    self.__gare = gare
    self.__gare_code = gare_code
    self.__elevator = []

  def add_elevator(self, code, position, direction, state):
    self.__elevator.append(ElevatorState(self.__jour, self.__gare_code, self.__gare, code,
                                         position, direction, state))

  def get_elevators(self):
    return self.__elevator

class ElevatorState(object):
  def __init__(self, date, station_code, station, code, position, direction, state):
    self.__date = date
    self.__station_code = station_code
    self.__station = station
    self.__code = code
    self.__position = position
    self.__direction = direction
    self.__state = state

  def str_data(self):
    return [str(self.__date), str(self.__station_code), self.__station, self.__code, self.__position, self.__direction, self.__state]

  def dict_data(self):
    ascenseur_data = {}
    ascenseur_data["code"] = self.__code
    ascenseur_data["code_gare"] = self.__station_code
    ascenseur_data["gare"] = self.__station
    ascenseur_data["situation"] = self.__position
    ascenseur_data["direction"] = self.__direction
    ascenseur_data["status"] = self.__state
    return ascenseur_data

class Scraper(object):
  class ScraperException(Exception):
    pass

  class ElevatorStateUnknown(Exception):
    pass

  def __init__(self, max_station, min_date, max_date):
    self.__stations = range(1, max_station + 1)
    self.__min_date = min_date
    self.__max_date = max_date

  def get_url(self, station, year, month, day):
    BASE_URL="http://www.transport-idf.com/infomobi/rechercherDispoAsc.do?"
    url = BASE_URL + "codeGare=" + str(station) + "&dateRecherche=" \
        + "%02d" % day + "%2F" + "%02d" % month + "%2F" + str(year) \
        + "&serverAction=VALIDEZ"
    return url

  def scrape(self, f):
    current_date = self.__max_date
    station_blacklist = []
    while (current_date > self.__min_date):
      print "Processing " + str(current_date)
      for station in self.__stations:
        if station in station_blacklist:
          continue
        try:
          gj = self.scrape_one_page(station, current_date)
          for elevator in gj.get_elevators():
            s="\t".join(elevator.str_data()) + "\n"
            f.write(s.encode("utf-8"))
        except urllib2.HTTPError:
          station_blacklist.append(station)
          print "Blacklisting station #" + str(station)
        except Scraper.ElevatorStateUnknown:
          print "Unknown state for station #" + str(station)
      current_date -= timedelta(1)
    #return map(GareJour.get_elevators, availabilities)
    return

  def scrape_one_day(self, requested_date):
    for station in self.__stations:
      try:
        gj = self.scrape_one_page(station, requested_date)
        for elevator in gj.get_elevators():
          data = elevator.dict_data()
          data["date"] = requested_date.strftime("%Y-%m-%d")
          yield data
      except urllib2.HTTPError:
        pass
      except Scraper.ElevatorStateUnknown:
        pass

  def scrape_one_page(self, station_code, requested_date):
    url = self.get_url(station_code, requested_date.year,
                                     requested_date.month,
                                     requested_date.day)
    page = urllib2.urlopen(url)
    soup = BeautifulSoup(page)
    table = soup.find(id="reponse_alerte")
    if table == None:
      raise Scraper.ScraperException("No reponse_alerte table: " + str(soup))
    day = table.find("p", id="date_consultation").contents[1].strip()
    self.verify_dates(day, requested_date)
    station = table.find("p", id="situation_gare").contents[1].strip()
    gare = GareJour(day, station, station_code)

    tbody = table.tbody
    for row in tbody.findAll("tr"):
      cols = row.findAll("td")
      if cols[0].string == None:
        continue
      code = cols[0].string.strip()
      position = cols[1].string.strip()
      direction = cols[2].string.strip()
      state = cols[3].string.strip()
      gare.add_elevator(code, position, direction, state)
    return gare

  def verify_dates(self, current_date, requested_date):
    day, month, year = current_date.split("/")
    page_date = date(int(year), int(month), int(day))
    if (page_date != requested_date):
      raise Scraper.ElevatorStateUnknown()

if __name__ == "__main__":
    import json
    s=Scraper(533, date.today(), date.today())
    for ascenseur in s.scrape_one_day(date.today()):
        try:
            output = urllib2.urlopen("http://sterops.pythonanywhere.com/json/LoadAscenseur/", urllib.urlencode({"json": json.dumps(ascenseur)}))

        except urllib2.URLError, e:
            print e.code
            print e.read()
            raise e


