# -*- coding: utf-8 -*-
from models import *
from data_loading import get_or_create_ascenseur

def add_accessible_line(csv_file):
    ligne_objects = {"A": Ligne(reseau="RER", ligne="A"),
                     "B": Ligne(reseau="RER", ligne="B"),
                     "C": Ligne(reseau="RER", ligne="C"),
                     "D": Ligne(reseau="RER", ligne="D"),
                     "E": Ligne(reseau="RER", ligne="E")}
    for ligne in ligne_objects.values():
        ligne.save()

    for row in csv_file:
        print row
        if row[1][0:4] == "RER " and len(row[1]) == 5:
            ligne = ligne_objects[row[1][4]]
            gare = Gare.objects.get(gare_infomobi__nom=row[0])
            gare.ligne.add(ligne)
            ligne.save()
            gare.save()

def get_or_create_ville(ville_name):
    try:
        ville = Ville.objects.get(nom=ville_name)
        return ville
    except Ville.DoesNotExist:
        ville = Ville(nom=ville_name)
        ville.save()
        return ville

def ajout_villes():
    gares = Gare.objects.filter(ville__isnull=True)
    for gare in gares:
        print gare.gare_infomobi.all()[0].nom, gare.articles_wikipedia.all()[0].url
        ville = raw_input("Ville: ")
        gare.ville = get_or_create_ville(ville)
        gare.save()

def load_ascenseur_manuel(ascenseur_data):
    code = ascenseur_data["code"]
    code_gare = ascenseur_data["code_gare"]
    nom_gare = ascenseur_data["gare"]
    situation = ascenseur_data["situation"]
    direction = ascenseur_data["direction"]
    status = ascenseur_data["status"]
    last_update = ascenseur_data["date"]
    try:
        ascenseur = get_or_create_ascenseur(code, nom_gare)
        ascenseur.situation = situation
        ascenseur.direction = direction
        ascenseur.status = status
        ascenseur.mise_a_jour = last_update
        ascenseur.save()
    except KeyError:
        print "Ascenseur " + code + " pour la gare de " + nom_gare + " non charge."
