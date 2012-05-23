from django.db import models

# Create your models here.

class Ascenseur(models.Model):
    code = models.CharField(max_length=100, unique=True)
    situation = models.CharField(max_length=100)
    direction = models.CharField(max_length=100)
    status = models.CharField(max_length=100)
    mise_a_jour = models.DateField()
    gare = models.ForeignKey('Gare', related_name='ascenseurs')

class Ligne(models.Model):
    reseau = models.CharField(max_length=40)
    ligne = models.CharField(max_length=10)

class GareWikipedia(models.Model):
    url = models.CharField(max_length=255, primary_key=True)
    gare = models.ForeignKey('Gare', related_name='articles_wikipedia')

class GareInfomobi(models.Model):
    nom = models.CharField(max_length=100, primary_key=True)
    nom_normalise = models.CharField(max_length=100, null=True)
    code = models.IntegerField(null=True, unique=True)
    gare = models.ForeignKey('Gare', related_name='gare_infomobi')

class Gare(models.Model):
    nom = models.CharField(max_length=100)
    latitude = models.FloatField(null=True)
    longitude = models.FloatField(null=True)

    ville = models.ForeignKey('Ville', null=True)
    ligne = models.ManyToManyField('Ligne', null=True)

class Ville(models.Model):
    nom = models.CharField(max_length=100, unique=True)
    departement = models.CharField(max_length=100)
