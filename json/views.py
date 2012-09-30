# Create your views here.
import json
from datetime import date

from django.http import HttpResponse
from django.views.decorators.http import require_POST

from models import *

def get_departements(request):
    # Return all known departements
    extract_departement = lambda x: x.departement
    all_departements = list(set(map(extract_departement, Ville.objects.all())))
    return HttpResponse(json.dumps(all_departements))

def get_villes(request, departement):
    # Return all cities within a departement
    villes = Ville.objects.filter(departement=departement)
    villes_liste = []
    for ville in villes:
        pk = ville.pk
        nom = ville.nom
        json_data = {'id': pk, 'nom': nom}
        villes_liste.append(json_data)
    return HttpResponse(json.dumps(villes_liste))

def dictify_gare_simple(gare):
    nom_gare = gare.nom
    if nom_gare.isupper():
        nom_gare = nom_gare.title()
    json_data = {'id': gare.pk, 'nom': nom_gare}
    lignes = gare.ligne.all()
    json_data['arrets'] = []
    for ligne in lignes:
        json_data['arrets'].append({'reseau': ligne.reseau, 'ligne': ligne.ligne, 'id': ligne.id})
    json_data['infomobi'] = filter(lambda x: x!=None, map(lambda w: w.code, gare.gare_infomobi.all()))
    json_data["status"] = True
    if bool(gare.ascenseurs.exclude(status__iexact="Disponible")):
        json_data["status"] = False
    return json_data

def get_gares_par_ville(request, ville):
    # Return all stations within a city
    gares = Gare.objects.filter(ville__pk=ville)
    gares_liste = map(dictify_gare_simple, gares)
    return HttpResponse(json.dumps(gares_liste))

def get_lignes(request):
    # Return all known lines
    to_dict = lambda l: {'reseau': l.reseau, 'ligne': l.ligne, 'id': l.pk}
    lignes = map(to_dict, Ligne.objects.all().order_by("reseau", "ligne"))
    return HttpResponse(json.dumps(lignes))

def get_gares(request):
    # Return all known lines
    to_dict = lambda l: {'reseau': l.reseau, 'ligne': l.ligne, 'id': l.pk}
    lignes = map(to_dict, Ligne.objects.all().order_by("reseau", "ligne"))
    # Retourn all known stations
    gares = map(dictify_gare_simple, Gare.objects.all())
    return HttpResponse(json.dumps({"lines": lignes, "stations": gares}))

def get_gares_par_ligne(request, ligne):
    gares = Gare.objects.filter(ligne__pk=ligne).order_by("nom")
    ligne_obj = Ligne.objects.get(pk=ligne)

    ascenseurs = Ascenseur.objects.filter(gare__ligne__pk=ligne)
    maj = date(1970, 1, 1)
    for ascenseur in ascenseurs:
        if ascenseur.mise_a_jour > maj:
            maj = ascenseur.mise_a_jour

    gares_liste = map(dictify_gare_simple, gares)
    gares_ok = filter(lambda g: g["status"], gares_liste)
    gares_pb = filter(lambda g: not g["status"], gares_liste)

    return HttpResponse(json.dumps({'reseau': ligne_obj.reseau,
                                    'ligne': ligne_obj.ligne,
                                    'gares': gares_liste,
                                    'gares_ok': gares_ok,
                                    'gares_pb': gares_pb,
                                    'maj': (maj.year, maj.month, maj.day)}))

def dictify_gare_detail(gare):
    json_data = dictify_gare_simple(gare)
    json_data['wikipedia'] = map(lambda w: w.url, gare.articles_wikipedia.all())
    json_data['ascenseurs'] = []
    ascenseurs = gare.ascenseurs.all()
    for ascenseur in ascenseurs:
        ascenseur_json = {'code': ascenseur.code,
                          'situation': ascenseur.situation,
                          'direction': ascenseur.direction,
                          'status': ascenseur.status,
                          'maj': ascenseur.mise_a_jour.strftime("%Y-%m-%d")}
        json_data['ascenseurs'].append(ascenseur_json)
    if len(ascenseurs) == 0:
        ascenseur_json = {'code': 'rampe',
                          'situation': "Rampes",
                          'direction': "L'acc&egrave;s &agrave; cette gare ne necessite pas d'ascenseur",
                          }
        json_data['ascenseurs'].append(ascenseur_json)

    ville = gare.ville
    json_data['ville'] = ville.nom
    json_data['departement'] = ville.departement
    json_data['latitude'] = gare.latitude
    json_data['longitude'] = gare.longitude
    return json_data

def get_gare(request, gare_id):
    gare = Gare.objects.get(pk=gare_id)
    return HttpResponse(json.dumps(dictify_gare_detail(gare)))
