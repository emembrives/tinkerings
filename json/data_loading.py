# -*- coding: utf-8 -*-
import datetime, json, re, unicodedata

from django.http import HttpResponse
from django.views.decorators.http import require_POST

from models import *

parentheses = re.compile("\(.+\)")

TRANSLATION_TABLE = {u"-": u" ", u"d'": u" ", u"l'": " ", u"œ": u"oe",
                     u"<br/>": u" ", u" l ": " ", u"–": " ", u" d ": " "}
WHITESPACE = re.compile('\s+')

def normalise(s):
    if isinstance(s, unicode):
        s = ''.join((c for c in unicodedata.normalize('NFD', s) if unicodedata.category(c) != 'Mn'))
    s = s.lower().strip()
    s = re.sub(parentheses, "", s)
    for old, new in TRANSLATION_TABLE.items():
        s = s.replace(old, new)
    s = re.sub(WHITESPACE, " ", s).strip()
    return s

def get_or_create_wikigare(wiki_url):
    try:
        gare_wikipedia = GareWikipedia.objects.get(url=wiki_url)
    except GareWikipedia.DoesNotExist:
        gare_wikipedia = GareWikipedia(url=wiki_url)
    return gare_wikipedia

def get_or_create_gareinfomobi(nom):
    try:
        gare_infomobi = GareInfomobi.objects.get(nom=nom)
    except GareInfomobi.DoesNotExist:
        gare_infomobi = GareInfomobi(nom=nom, nom_normalise=normalise(nom))
    return gare_infomobi

def get_or_create_gare(wiki_url, nom):
    if (Gare.objects.filter(articles_wikipedia__url=wiki_url,
        gare_infomobi__nom=nom).count() != 0):
        return Gare.objects.get(articles_wikipedia__url=wiki_url,
            gare_infomobi__nom=nom), False
    elif (Gare.objects.filter(articles_wikipedia__url=wiki_url).count() != 0):
        gare = Gare.objects.get(articles_wikipedia__url=wiki_url)
        gare.save()
        gare_infomobi = get_or_create_gareinfomobi(nom)
        gare_infomobi.gare = gare
        gare_infomobi.save()
        return gare, False
    elif (Gare.objects.filter(gare_infomobi__nom=nom).count() != 0):
        gare = Gare.objects.get(gare_infomobi__nom=nom)
        gare.save()
        gare_wikipedia = GareWikipedia(url=wiki_url, gare=gare)
        gare_wikipedia.save()
        return gare, False
    else:
        gare = Gare()
        gare.save()
        gare_wikipedia = GareWikipedia(url=wiki_url, gare=gare)
        gare_infomobi = get_or_create_gareinfomobi(nom)
        gare_infomobi.gare = gare
        gare_wikipedia.save()
        gare_infomobi.save()
        return gare, True

def parseDate(date_str):
    return datetime.datetime.strptime(date_str, "%Y-%m-%d").date()

def get_gare_by_name(gare_nom):
    gare_liste = Gare.objects.filter(gare_infomobi__nom_normalise__iexact=gare_nom).distinct()
    if len(gare_liste) == 1:
        return gare_liste[0]
    elif len(gare_liste) > 1:
        raise KeyError("Plusieurs gares au nom de " + gare_nom)
    reduced_name = normalise(gare_nom)
    gare_liste = Gare.objects.filter(gare_infomobi__nom_normalise__iexact=reduced_name).distinct()
    if len(gare_liste) == 1:
        return gare_liste[0]
    elif len(gare_liste) > 1:
        raise KeyEError("Plusieurs gares au nom de " + reduced_name)
    gare_liste = Gare.objects.filter(gare_infomobi__nom_normalise__icontains=reduced_name).distinct()
    if len(gare_liste) == 1:
        return gare_liste[0]
    elif len(gare_liste) > 1:
        raise KeyError("Plusieurs gares contenant " + reduced_name)
    else:
        raise KeyError("Gare non trouvee pour " + gare_nom + " | " + reduced_name)

def get_or_create_ascenseur(code, nom_gare):
    try:
        ascenseur = Ascenseur.objects.get(code=code)
        return ascenseur
    except Ascenseur.DoesNotExist:
        gare_obj = get_gare_by_name(nom_gare)
        ascenseur = Ascenseur(code=code, gare=gare_obj)
        return ascenseur

@require_POST
def load_ascenseur(request):
    json_data = request.POST["json"]
    ascenseur_dict = json.loads(json_data)
    code = ascenseur_dict["code"]
    gare = ascenseur_dict["gare"]
    situation = ascenseur_dict["situation"]
    direction = ascenseur_dict["direction"]
    status = ascenseur_dict["status"]
    last_update = ascenseur_dict["date"]
    try:
        ascenseur = get_or_create_ascenseur(code, gare)
        ascenseur.situation = situation
        ascenseur.direction = direction
        ascenseur.status = status
        ascenseur.mise_a_jour = parseDate(last_update)
        ascenseur.save()
        return HttpResponse("OK")
    except KeyError:
        return HttpResponse("Ascenseur " + code + " pour la gare de " + gare + " non charge.")

@require_POST
def load_data(request):
    data_type = request.POST["type"]
    if (data_type == "gare"):
        json_data = request.POST["json"]
        gare_dict = json.loads(json_data)
        if "update" in request.POST:
            try:
                wiki_url = gare_dict["wikipedia_id"]
                gare = Gare.objects.get(articles_wikipedia__url=wiki_url)
            except Gare.DoesNotExist:
                return HttpResponse("NotFound")
        else:
            gare, creation = get_or_create_gare(gare_dict['wikipedia_id'].strip(), gare_dict['nom'].strip())
        if "latitude" in gare_dict:
            gare.latitude = float(gare_dict["latitude"])
        if "longitude" in gare_dict:
            gare.longitude = float(gare_dict["longitude"])
        if "lignes" in gare_dict:
            for ligne in gare_dict["lignes"]:
                reseau = ligne["reseau"]
                ligne_reseau = ligne["ligne"]
                try:
                    ligne_obj = Ligne.objects.get(reseau=reseau, ligne=ligne_reseau)
                except Ligne.DoesNotExist:
                    ligne_obj = Ligne(reseau=reseau, ligne=ligne_reseau)
                    ligne_obj.save()
                if not ligne_obj in gare.ligne.all():
                    gare.ligne.add(ligne_obj)
        if "ville" in gare_dict:
            ville = gare_dict["ville"]
            try:
                ville_obj = Ville.objects.get(nom=ville)
            except Ville.DoesNotExist:
                ville_obj = Ville(nom=ville)
                ville_obj.save()
            gare.ville = ville_obj
        gare.save()
        return HttpResponse("OK")
    elif (data_type == "ascenseur"):
        return load_ascenseur(request)
    else:
        return HttpResponse("Failed")