from __future__ import print_function
import os.path
import base64
import spacy
import json
import re
#import psycopg2
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from google.auth.transport.requests import Request
from datetime import datetime

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
TOKEN_PATH = os.path.join(SCRIPT_DIR, "token.json")
CREDENTIALS_PATH = os.path.join(SCRIPT_DIR, "credentials.json")

SCOPES = ['https://www.googleapis.com/auth/gmail.readonly']
nlp = spacy.load("fr_core_news_md")

CATEGORIES = {
    "refusee": ["regrettons", "pas retenue", "refus", "malheureusement", "négatif"],
    "entretien": ["entretien", "convocation", "rendez-vous", "disponibilités", "call", "meeting"],
    "envoyee": ["nous avons bien reçu", "confirmation de réception", "accusé de réception"]
}

def gmail_auth():
    creds = None
    
    # Si token existe
    if os.path.exists(TOKEN_PATH):
        try:
            creds = Credentials.from_authorized_user_file(TOKEN_PATH, SCOPES)
        except Exception as e:
            print(f"Erreur avec le token existant : {e}")
            if os.path.exists(TOKEN_PATH):
                os.remove(TOKEN_PATH)
            creds = None
    
    # Si le token n'existe pas ou n'est pas valide
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            try:
                creds.refresh(Request())
            except Exception as e:
                print(f"Erreur refresh token : {e}")
                if os.path.exists(TOKEN_PATH):
                    os.remove(TOKEN_PATH)
                creds = None
        if not creds:
            if not os.path.exists(CREDENTIALS_PATH):
                raise FileNotFoundError(
                    f"Le fichier credentials.json n'existe pas dans {SCRIPT_DIR}. "
                    "Veuillez télécharger vos credentials depuis Google Cloud Console."
                )
            
            try:
                flow = InstalledAppFlow.from_client_secrets_file(CREDENTIALS_PATH, SCOPES)
                creds = flow.run_local_server(port=0)
                
                # Sauvegarder le token
                with open(TOKEN_PATH, 'w') as token:
                    token.write(creds.to_json())
            except Exception as e:
                print(f"Erreur lors de l'authentification : {e}")
                raise
    
    try:
        service = build('gmail', 'v1', credentials=creds)
        return service
    except Exception as e:
        print(f"Erreur lors de la création du service Gmail : {e}")
        raise

def extract_company(sender: str):
    name_part = sender.split("<")[0].strip()
    email_part = sender.split("<")[-1].replace(">", "").strip() if "<" in sender else ""

    doc = nlp(name_part)
    orgs = [ent.text for ent in doc.ents if ent.label_ == "ORG"]
    if orgs:
        return orgs[0]

    match = re.search(r'@([A-Za-z0-9.-]+)', email_part)
    if match:
        domain = match.group(1).split(".")[0]
        return domain.capitalize()

    return name_part

def get_first_mail(service):
    try:
        results = service.users().messages().list(userId='me', maxResults=5).execute()
        messages = results.get('messages', [])

        if not messages:
            print("Aucun mail trouvé.")
            return

        msg = service.users().messages().get(userId='me', id=messages[0]['id']).execute()

        #Récupération infos
        headers = msg['payload']['headers']
        subject = next((h['value'] for h in headers if h['name'] == 'Subject'), "Pas d'objet")
        sender = next((h['value'] for h in headers if h['name'] == 'From'), "Expéditeur inconnu")

        #Date
        internal_date = msg.get("internalDate")
        if internal_date:
            date = datetime.fromtimestamp(int(internal_date)/1000).strftime("%d/%m/%Y %H:%M")
        else:
            date = "Date inconnue"

        #Job
        doc = nlp(subject)
        jobs = [ent.text for ent in doc.ents if ent.label_ in ["MISC", "ORG", "PER"]]
        
        if "-" in subject:
            job_title = subject.split("-")[-1].strip()
        else:
            job_title = subject

        #Entreprise
        company = extract_company(sender)

        #Corps mail
        mail_body = ""
        if "parts" in msg["payload"]:
            for part in msg["payload"]["parts"]:
                if part["mimeType"] == "text/plain":
                    mail_body = base64.urlsafe_b64decode(part["body"]["data"]).decode("utf-8")
                    break
        else:
            mail_body = base64.urlsafe_b64decode(msg["payload"]["body"]["data"]).decode("utf-8")

        text = mail_body.lower()

        #Etat
        etat = "en_attente"
        for status, keywords in CATEGORIES.items():
            if any(kw in text for kw in keywords):
                etat = status
                break

        result = {
            "job": job_title,
            "company": company,
            "date": date,
            "etat": etat
        }
    
        print(json.dumps(result))

    except Exception as e:
        print(f"Erreur : {e}")

if __name__ == '__main__':
    service = gmail_auth()
    get_first_mail(service)