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
    "refusee": ["regrettons", "pas retenue", "refus", "malheureusement", "négatif", "désolé", "refusé", "rejetté", "ne nous permettent pas"],
    "entretien": ["entretien", "convocation", "rendez-vous", "disponibilités", "succès", "réussi", "nous sommes heureux"],
    "envoyee": ["nous avons bien reçu", "confirmation de réception", "accusé de réception", "a été crée", "a bien été reçu", "a bien été enregistrée", "est arrivée"]
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

def split_camel_case(s):
    return re.sub(r'([a-z])([A-Z])', r'\1 \2', s)

def clean_company_name(name):
    common_words = ['groupe', 'group', 'inc', 'sa', 'sas', 'sarl', 'ltd', 'limited', 'corp', 'careers']
    name = name.lower()
    name = name.replace('-', ' ').replace('_', ' ').replace('.', ' ')
    words = name.split()
    words = [w for w in words if w not in common_words]
    
    return ' '.join(word.capitalize() for word in words if word)

def extract_company(sender: str):
    name_part = sender.split("<")[0].strip()
    email_part = sender.split("<")[-1].replace(">", "").strip() if "<" in sender else sender

    doc = nlp(name_part)
    orgs = [ent.text for ent in doc.ents if ent.label_ == "ORG"]
    if orgs:
        return clean_company_name(orgs[0])

    match = re.search(r'([\w\.-]+)@([\w\.-]+)', email_part)
    if match:
        user_part, domain = match.groups()
        domain_parts = domain.split('.')
        
        generic_domains = {'gmail', 'yahoo', 'hotmail', 'outlook', 'wanadoo', 
                         'orange', 'profils', 'noreply', 'careers', 'free', 
                         'laposte', 'aol', 'recruitment'}

        if domain_parts[0].lower() not in generic_domains:
            return clean_company_name(domain_parts[0])
        
        cleaned_user = clean_company_name(user_part)
        if cleaned_user and len(cleaned_user) > 2:
            return cleaned_user

    return clean_company_name(name_part)

def parse_mail(msg):
    headers = msg['payload']['headers']
    subject = next((h['value'] for h in headers if h['name'] == 'Subject'), "Pas d'objet")
    sender = next((h['value'] for h in headers if h['name'] == 'From'), "Expéditeur inconnu")

    internal_date = msg.get("internalDate")
    date = datetime.fromtimestamp(int(internal_date)/1000).strftime("%d/%m/%Y %H:%M") if internal_date else "Date inconnue"

    if "-" in subject:
        job_title = subject.split("-")[-1].strip()
    else:
        job_title = subject

    company = extract_company(sender)

    mail_body = ""
    if "parts" in msg["payload"]:
        for part in msg["payload"]["parts"]:
            if part["mimeType"] == "text/plain" and "data" in part["body"]:
                mail_body = base64.urlsafe_b64decode(part["body"]["data"]).decode("utf-8", errors="ignore")
                break
    elif "data" in msg["payload"]["body"]:
        mail_body = base64.urlsafe_b64decode(msg["payload"]["body"]["data"]).decode("utf-8", errors="ignore")

    text = mail_body.lower()
    etat = "en_attente"
    for status, keywords in CATEGORIES.items():
        if any(kw in text for kw in keywords):
            etat = status
            break

    return {
        "job": job_title,
        "company": company,
        "date": date,
        "etat": etat
    }

def get_user_email(service):
    try:
        profile = service.users().getProfile(userId='me').execute()
        return profile['emailAddress']
    except Exception as e:
        print(f"Erreur lors de la récupération de l'email : {e}")
        return None

def get_last_mails(service, n=20):
    results = service.users().messages().list(userId='me', maxResults=n).execute()
    messages = results.get('messages', [])
    mails_data = []
    user_email = get_user_email(service)

    for msg_info in messages:
        msg = service.users().messages().get(userId='me', id=msg_info['id']).execute()
        mails_data.append(parse_mail(msg))

    response_data = {
        "userEmail": user_email,
        "mails": mails_data
    }

    print(json.dumps(response_data, ensure_ascii=False))

if __name__ == '__main__':
    service = gmail_auth()
    get_last_mails(service, 20)