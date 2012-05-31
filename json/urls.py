import views
import data_loading
from django.conf.urls.defaults import patterns, include, url

urlpatterns = patterns('',
    (r'^GetDepartements/$', views.get_departements),
    (r'^GetLignes/$', views.get_lignes),
    (r'^GetVillesParDepartement/([^/]+)/$', views.get_villes),
    (r'^GetGaresParLigne/(\d+)/$', views.get_gares_par_ligne),
    (r'^GetGaresParVille/(\d+)/$', views.get_gares_par_ville),
    (r'^GetGare/(\d+)/$', views.get_gare),
    (r'^LoadData/', data_loading.load_data),
    (r'^LoadAscenseur/', data_loading.load_ascenseur),
)
