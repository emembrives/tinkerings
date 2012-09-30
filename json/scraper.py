#!/usr/bin/python
# -*- coding: utf-8 -*-

import urllib2, urllib
from BeautifulSoup import BeautifulSoup
from datetime import date, timedelta
import itertools, time, random

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
  STATION_BLACKLIST = [1, 2, 3, 7, 8, 9, 10, 11, 12, 14, 15, 17, 21, 22, 24, 25,
  26, 27, 28, 30, 31, 32, 34, 35, 36, 38, 39, 40, 44, 45, 46, 47, 48, 49, 51,
  52, 53, 54, 55, 56, 57, 58, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
  74, 75, 76, 78, 79, 81, 83, 85, 86, 88, 90, 91, 94, 95, 96, 97, 98, 99, 101,
  102, 103, 105, 106, 107, 108, 110, 111, 113, 114, 115, 116, 117, 118, 119,
  121, 122, 123, 124, 125, 126, 127, 128, 130, 131, 132, 133, 134, 135, 137,
  138, 139, 140, 142, 145, 146, 148, 150, 151, 152, 153, 154, 155, 157, 159,
  160, 163, 164, 166, 168, 169, 170, 171, 173, 174, 175, 176, 178, 179, 180,
  182, 183, 184, 185, 186, 187, 188, 189, 191, 193, 195, 196, 197, 198, 199,
  200, 201, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 216,
  217, 219, 220, 222, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 235,
  236, 239, 241, 244, 245, 246, 247, 248, 249, 251, 254, 258, 259, 261, 262,
  263, 264, 265, 266, 268, 269, 270, 272, 273, 274, 275, 276, 278, 279, 281,
  282, 283, 284, 285, 286, 288, 290, 291, 292, 294, 295, 296, 297, 298, 300,
  301, 302, 303, 304, 306, 309, 311, 312, 313, 314, 315, 316, 318, 319, 320,
  321, 322, 325, 326, 327, 328, 331, 332, 333, 334, 335, 336, 338, 339, 340,
  341, 346, 347, 348, 349, 350, 352, 353, 354, 355, 356, 359, 360, 362, 363,
  364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 378, 379,
  380, 382, 383, 384, 385, 386, 388, 389, 390, 391, 392, 393, 394, 395, 396,
  397, 398, 439, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452,
  453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 464, 465, 466, 467, 468,
  470, 471, 472, 473, 474, 475, 476, 477, 478, 480, 481, 482, 483, 484, 485,
  486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 517, 522, 523, 524,
  525, 526, 528]
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
    while (current_date > self.__min_date):
      print "Processing " + str(current_date)
      for station in self.__stations:
        if station in self.STATION_BLACKLIST:
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
      if station in self.STATION_BLACKLIST:
          continue
      try:
        time.sleep(random.randint(2,20))
        gj = self.scrape_one_page(station, requested_date)
        for elevator in gj.get_elevators():
          data = elevator.dict_data()
          data["date"] = requested_date.strftime("%Y-%m-%d")
          yield data
      except urllib2.HTTPError, err:
        print "Error while processing station #" + str(station)
        pass
      except Scraper.ElevatorStateUnknown:
        pass

  def scrape_one_page(self, station_code, requested_date):
    url = self.get_url(station_code, requested_date.year,
                                     requested_date.month,
                                     requested_date.day)
    request = urllib2.Request(url)
    request.add_header('User-Agent', 'Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/6.0)')
    page = urllib2.urlopen(request)
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
    import json, csv
    s = Scraper(533, date.today(), date.today())
    f = csv.write(open("elevators.csv", "w"))
    for ascenseur in s.scrape_one_day(date.today()):
        try:
            output = urllib2.urlopen("http://sterops.pythonanywhere.com/json/LoadAscenseur/", urllib.urlencode({"json": json.dumps(ascenseur)}))
            f.writerow(ascenseur.values())
        except urllib2.URLError, e:
            print e.code
            print e.read()
            raise e


