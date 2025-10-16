Etape mise en place google OAuth :
Étape 1 : Créer un projet Google Cloud
1.	Va sur : Google Cloud Console.
2.	Connecte-toi avec ton compte Gmail.
3.	Clique sur “Sélectionner un projet” → “Nouveau projet”.
4.	Donne un nom à ton projet (ex. CandidatureTracker).
5.	Clique sur Créer.
Conseil : note le nom et l’ID du projet, tu en auras besoin plus tard.

Étape 2 : Activer l’API Gmail
1.	Avec ton projet sélectionné, va dans API et services → Bibliothèque.
2.	Cherche Gmail API.
3.	Clique dessus, puis Activer.

Étape 3 : Créer des identifiants OAuth2
1.	Toujours dans API et services, va dans Identifiants.
2.	Clique sur Créer des identifiants → ID client OAuth.
3.	Google te demande de configurer l’écran de consentement :
o	Choisis Externe (car c’est pour ton compte perso).
o	Mets un nom d’application (ex. Candidature Tracker).
o	Mets ton email comme support.
o	Laisse-le reste par défaut et Enregistrer.
4.	Une fois l’écran de consentement configuré, choisis le type d’application :
o	Application de bureau si tu testes en local.
5.	Google te donne alors ton Client ID et Client Secret.
Note bien ces deux informations, elles sont nécessaires pour ton script Python.

A installer: pip install --upgrade google-api-python-client google-auth-httplib2 google-auth-oauthlib

Créez un fichier "credentials.json" et ajoutez ça avec les infos OAuth dans les guillemets vides
{
    "installed":{
        "client_id":"",
        "project_id":"","auth_uri":"https://accounts.google.com/o/oauth2/auth",
        "token_uri":"https://oauth2.googleapis.com/token",
        "auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs",
        "client_secret":"",
        "redirect_uris":["http://localhost/"]
        }
}
