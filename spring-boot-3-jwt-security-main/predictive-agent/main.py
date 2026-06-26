import os
from dotenv import load_dotenv
load_dotenv()
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime, timedelta
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import math
import requests
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.preprocessing import LabelEncoder

app = FastAPI(title="Predictive Downtime Agent")

try:
    from groq import Groq
except ImportError:
    Groq = None

class DowntimeRecord(BaseModel):
    ligne: Optional[str] = "UNKNOWN"
    chantier: Optional[str] = "UNKNOWN"
    severity: Optional[str] = "UNKNOWN"
    type: Optional[str] = "UNKNOWN"
    startTime: str
    endTime: Optional[str] = None

class PredictionRequest(BaseModel):
    history: List[DowntimeRecord]

class LignePrediction(BaseModel):
    ligne: str
    probability: float
    likely_type: str
    estimated_time_to_failure_hours: int
    recommended_action: str

class NCHistoryRecord(BaseModel):
    id: Optional[int] = None
    description: str
    localisation: Optional[str] = ""
    actionCorrective: Optional[str] = ""
    assigneeEmail: Optional[str] = ""
    dateDeclaration: Optional[str] = None
    dateCloture: Optional[str] = None

class NCAnalyzeRequest(BaseModel):
    description: str
    localisation: Optional[str] = ""
    history: List[NCHistoryRecord]

class NCAnalyzeResponse(BaseModel):
    suggested_root_cause: str
    recommended_action: str
    recommended_assignee: str
    confidence_score: float

class ChatCopilotRequest(BaseModel):
    message: str
    user_id: Optional[int] = None

class ChatCopilotResponse(BaseModel):
    reply: str

class OeeKpiRecord(BaseModel):
    date: str
    ligne: int
    trg: float
    fpy_vision: float
    tx_arrachement: float
    encours_depannage: float
    efficience: float

class OeePredictionRequest(BaseModel):
    history: List[OeeKpiRecord]

class OeePredictionResponse(BaseModel):
    ligne: int
    predictedTrg: float
    rootCauseIndicator: str
    recommendation: str

@app.post("/predict", response_model=List[LignePrediction])
def predict_downtimes(request: PredictionRequest):
    if not request.history:
        return []

    # Convert to pandas DataFrame
    data = [r.dict() for r in request.history]
    df = pd.DataFrame(data)
    
    # Parse dates
    df['startTime'] = pd.to_datetime(df['startTime'])
    
    # Sort by time
    df = df.sort_values(by=['ligne', 'startTime'])
    
    predictions = []
    
    # Process each line
    lignes = df['ligne'].unique()
    for ligne in lignes:
        line_data = df[df['ligne'] == ligne].copy()
        if len(line_data) < 2:
            # Not enough data for this line
            continue
            
        # Calculate time differences between failures (MTBF)
        line_data['time_diff'] = line_data['startTime'].diff().dt.total_seconds() / 3600.0 # in hours
        mtbf = line_data['time_diff'].mean()
        if pd.isna(mtbf) or mtbf <= 0:
            mtbf = 120 # Default fallback
            
        last_failure_time = line_data['startTime'].iloc[-1]
        time_since_last = (datetime.now() - last_failure_time).total_seconds() / 3600.0
        
        # Simple probabilistic model based on MTBF (exponential distribution CDF approximation for next 48h)
        # P(T <= t) = 1 - e^(-lambda * t) where lambda = 1/MTBF
        rate = 1.0 / mtbf
        future_window = 48.0 # Next 48 hours
        # Remaining time to MTBF
        expected_remaining = max(1, mtbf - time_since_last)
        
        probability = 1.0 - math.exp(-rate * (time_since_last + future_window))
        probability = min(0.95, max(0.10, probability)) # cap between 10% and 95%
        
        # Use ML to predict the MOST LIKELY TYPE of the next failure
        likely_type = line_data['type'].mode()[0] if not line_data['type'].mode().empty else "UNKNOWN"
        
        # If we have enough data, train a quick Random Forest to guess type based on day of week and hour
        if len(line_data) > 5:
            try:
                line_data['day_of_week'] = line_data['startTime'].dt.dayofweek
                line_data['hour'] = line_data['startTime'].dt.hour
                
                X = line_data[['day_of_week', 'hour']]
                y = line_data['type']
                
                # Simple encoder for types
                le = LabelEncoder()
                y_enc = le.fit_transform(y)
                
                clf = RandomForestClassifier(n_estimators=10, random_state=42)
                clf.fit(X, y_enc)
                
                # Predict for 'tomorrow' at same hour
                future_date = datetime.now() + timedelta(hours=expected_remaining)
                X_future = pd.DataFrame({'day_of_week': [future_date.weekday()], 'hour': [future_date.hour]})
                pred_enc = clf.predict(X_future)
                likely_type = le.inverse_transform(pred_enc)[0]
            except Exception as e:
                print(f"Error training RF for {ligne}: {e}")
                
        # Generate generic recommendation based on likely type
        recommendation = "Planifier une inspection générale."
        if "MECA" in likely_type.upper() or "MÉC" in likely_type.upper():
            recommendation = "Vérifier l'usure mécanique et la lubrification."
        elif "ELEC" in likely_type.upper() or "ÉLEC" in likely_type.upper():
            recommendation = "Inspecter les connexions électriques et les capteurs."
        elif "SOFT" in likely_type.upper():
            recommendation = "Vérifier les logs système et redémarrer les services si nécessaire."
        elif "QUALITE" in likely_type.upper() or "QUALITÉ" in likely_type.upper():
             recommendation = "Vérifier les paramètres de tolérance et les matériaux entrants."

        predictions.append(LignePrediction(
            ligne=ligne,
            probability=round(probability, 2),
            likely_type=likely_type,
            estimated_time_to_failure_hours=int(expected_remaining),
            recommended_action=recommendation
        ))
        
    # Sort predictions by probability descending
    predictions.sort(key=lambda x: x.probability, reverse=True)
    return predictions

@app.post("/analyze-nc", response_model=NCAnalyzeResponse)
def analyze_nc(request: NCAnalyzeRequest):
    if not request.history or not request.description:
        return NCAnalyzeResponse(
            suggested_root_cause="Insuffisant de données historiques",
            recommended_action="",
            recommended_assignee="",
            confidence_score=0.0
        )
        
    df = pd.DataFrame([r.dict() for r in request.history])
    
    # Fill NA
    df['description'] = df['description'].fillna("")
    df['localisation'] = df['localisation'].fillna("")
    df['actionCorrective'] = df['actionCorrective'].fillna("")
    
    # We combine description and localisation for similarity
    df['text_feature'] = df['localisation'] + " " + df['description']
    target_text = (request.localisation or "") + " " + request.description
    
    # TF-IDF
    vectorizer = TfidfVectorizer(stop_words=None)
    
    try:
        tfidf_matrix = vectorizer.fit_transform(df['text_feature'].tolist() + [target_text])
        # Last row is our target
        cosine_sim = cosine_similarity(tfidf_matrix[-1], tfidf_matrix[:-1]).flatten()
        
        df['similarity'] = cosine_sim
        df = df.sort_values(by='similarity', ascending=False)
        
        # Get top 5 most similar
        top_matches = df[df['similarity'] > 0.1].head(5)
        
        if top_matches.empty:
            return NCAnalyzeResponse(
                suggested_root_cause="Aucune similarité trouvée",
                recommended_action="",
                recommended_assignee="",
                confidence_score=0.0
            )
            
        best_match = top_matches.iloc[0]
        confidence = float(best_match['similarity'])
        
        # Suggest corrective action (most frequent among top matches, or just the best match)
        top_actions = top_matches['actionCorrective'][top_matches['actionCorrective'] != ""]
        if not top_actions.empty:
            recommended_action = str(top_actions.mode()[0])
        else:
            recommended_action = ""
            
        # Recommend assignee
        top_assignees = top_matches['assigneeEmail'][top_matches['assigneeEmail'] != ""]
        
        recommended_assignee = ""
        if not top_assignees.empty:
            fastest_assignee = ""
            min_time = float('inf')
            
            for idx, row in top_matches.iterrows():
                if pd.notna(row['dateDeclaration']) and pd.notna(row['dateCloture']) and row['assigneeEmail']:
                    try:
                        start = pd.to_datetime(row['dateDeclaration'])
                        end = pd.to_datetime(row['dateCloture'])
                        duration = (end - start).total_seconds()
                        if duration > 0 and duration < min_time:
                            min_time = duration
                            fastest_assignee = row['assigneeEmail']
                    except:
                        pass
            
            if fastest_assignee:
                recommended_assignee = fastest_assignee
            else:
                recommended_assignee = str(top_assignees.mode()[0])
                
        # Generate a suggested root cause based on action or description
        desc_snippet = str(best_match['description'])[:50]
        suggested_root_cause = f"Similaire à un problème passé: {desc_snippet}..."
        
        return NCAnalyzeResponse(
            suggested_root_cause=suggested_root_cause,
            recommended_action=recommended_action,
            recommended_assignee=recommended_assignee,
            confidence_score=round(confidence * 100, 2)
        )
        
    except Exception as e:
        print(f"Error in analyze-nc: {e}")
        return NCAnalyzeResponse(
            suggested_root_cause="Erreur lors de l'analyse",
            recommended_action="",
            recommended_assignee="",
            confidence_score=0.0
        )

def get_auth_headers():
    try:
        resp = requests.post("http://localhost:8081/api/v1/auth/authenticate", json={
            "email": "ai@sagemcom.com",
            "password": "aipassword"
        })
        if resp.status_code == 200:
            token = resp.json().get("access_token")
            return {"Authorization": f"Bearer {token}"}
    except Exception:
        pass
    return {}

def gather_production_context() -> str:
    context = "=== DONNÉES DE PRODUCTION EN TEMPS RÉEL ===\n"
    today = datetime.now().strftime("%Y-%m-%d")
    headers = get_auth_headers()
    
    # 1. TRG / KPI pour tous les chantiers (sur les 30 derniers jours pour avoir de la donnée)
    from datetime import timedelta
    last_month = (datetime.now() - timedelta(days=30)).strftime("%Y-%m-%d")
    chantiers = ["CMS1", "CMS2", "INTEG", "ASSEMBLY"]
    for chantier in chantiers:
        try:
            resp = requests.get(f"http://localhost:8081/api/v1/daily-production/kpi-summary?chantier={chantier}&from={last_month}&to={today}", headers=headers, timeout=1.5)
            if resp.status_code == 200:
                data = resp.json()
                # On ajoute le contexte uniquement s'il y a des données (ex: TRG n'est pas nul)
                if data.get('trg') is not None:
                    context += f"\n[KPI GLOBALES (Chantier {chantier} - 30 derniers jours)]\n"
                    context += f"- TRG Moyen: {data.get('trg')}%\n"
                    context += f"- TRS: {data.get('trs')}%\n"
                    context += f"- Qualité: {data.get('qualite')}%\n"
                    context += f"- Performance: {data.get('performance')}%\n"
                    context += f"- Disponibilité: {data.get('disponibilite')}%\n"
                    context += f"- Quantité Produite: {data.get('quantiteProduite', '0')}\n"
        except Exception as e:
            pass # Ignore in context if API fails

    # 2. Non-Conformités (NC)
    try:
        resp = requests.get("http://localhost:8081/api/v1/nc", headers=headers, timeout=2)
        if resp.status_code == 200:
            ncs = resp.json()
            open_ncs = [n for n in ncs if n.get('statut') in ['OUVERTE', 'EN_TRAITEMENT']]
            critical = [n for n in open_ncs if n.get('gravite') == 'CRITIQUE']
            context += f"\n[NON-CONFORMITÉS (NC)]\n"
            context += f"- Total actives: {len(open_ncs)}\n"
            context += f"- Critiques: {len(critical)}\n"
            for c in critical[:3]:
                context += f"  * {c.get('reference')} ({c.get('localisation')}): {str(c.get('description'))[:50]}...\n"
    except Exception as e:
         pass

    # 3. Arrêts (Downtimes)
    try:
        resp = requests.get("http://localhost:8081/api/v1/downtimes", headers=headers, timeout=2)
        if resp.status_code == 200:
            downtimes = resp.json()
            ongoing = [d for d in downtimes if d.get('statut') == 'ONGOING']
            context += f"\n[ARRÊTS / DOWNTIMES]\n"
            context += f"- Total en cours: {len(ongoing)}\n"
            for d in ongoing[:3]:
                context += f"  * Ligne {d.get('ligne')} - {d.get('type')} ({d.get('severity')}): {str(d.get('description', ''))[:50]}...\n"
    except Exception as e:
         pass
         
    return context

SYSTEM_PROMPT = """Tu es l'Agent Copilot IA de Sagemcom, un assistant expert en production manufacturière. 
Ton rôle est d'analyser les données de production en temps réel (TRG, TRS, Qualité, Arrêts/Downtimes, Non-Conformités/NC) et de fournir des conseils stratégiques pour optimiser les performances.
Réponds TOUJOURS en français, de manière claire, concise et très professionnelle, avec des puces et des emojis appropriés. 
Ne mentionne pas que tu es un modèle d'IA. Base tes réponses sur le contexte de production fourni ci-dessous.
Si l'utilisateur pose une question générale, explique ce que tu peux faire (analyse de KPI, gestion des arrêts, suivi qualité).
Si tu constates des arrêts ou des NC critiques, propose des solutions pratiques pour les résoudre (ex: maintenance préventive, analyse 8D, QRQC, ajustement des paramètres machines, formation des opérateurs, etc.)."""

@app.post("/chat-copilot", response_model=ChatCopilotResponse)
def chat_copilot(request: ChatCopilotRequest):
    groq_api_key = os.environ.get("GROQ_API_KEY", "YOUR_GROQ_API_KEY_HERE")
    if not groq_api_key or not Groq:
        return ChatCopilotResponse(reply="⚠️ **Erreur de Configuration**: La clé API n'est pas configurée. Veuillez vérifier votre fichier .env.")

    groq_client = Groq(api_key=groq_api_key)
    # Recommandé par l'utilisateur: Llama
    groq_model = os.environ.get("GROQ_MODEL", "llama-3.3-70b-versatile")

    try:
        # 1. Rassembler le contexte en temps réel
        live_context = gather_production_context()
        full_system_prompt = f"{SYSTEM_PROMPT}\n\n{live_context}"
        
        # 2. Appeler Groq (Llama)
        response = groq_client.chat.completions.create(
            model=groq_model,
            messages=[
                {"role": "system", "content": full_system_prompt},
                {"role": "user", "content": request.message}
            ],
            temperature=0.3,
            max_tokens=800
        )
        
        reply_content = response.choices[0].message.content
        return ChatCopilotResponse(reply=reply_content)
        
    except Exception as e:
        print(f"Error calling Groq API: {e}")
        return ChatCopilotResponse(reply="🚨 **Erreur**: Impossible de contacter le modèle LLM pour le moment. Veuillez réessayer plus tard.")

@app.post("/predict-oee", response_model=List[OeePredictionResponse])
def predict_oee(request: OeePredictionRequest):
    if not request.history:
        return []

    # Convert to DataFrame
    df = pd.DataFrame([r.dict() for r in request.history])
    df['date'] = pd.to_datetime(df['date'])
    df = df.sort_values(by=['ligne', 'date'])

    predictions = []

    # Features used to predict TRG
    features = ['fpy_vision', 'tx_arrachement', 'encours_depannage', 'efficience']
    feature_names = {
        'fpy_vision': 'FPY Vision',
        'tx_arrachement': 'Tx Arrachement (%)',
        'encours_depannage': 'Encours Depannage',
        'efficience': 'Efficience / Ecart DMH'
    }

    lignes = df['ligne'].unique()
    for ligne in lignes:
        line_data = df[df['ligne'] == ligne].copy()
        
        if len(line_data) < 3:
            # Not enough data, just return last TRG
            last_trg = line_data['trg'].iloc[-1] if not line_data.empty else 85.0
            if pd.isna(last_trg): last_trg = 85.0
            predictions.append(OeePredictionResponse(
                ligne=ligne,
                predictedTrg=round(float(last_trg), 1),
                rootCauseIndicator="Insuffisant de données",
                recommendation="Attendre plus de données."
            ))
            continue
            
        # We want to predict tomorrow's TRG based on today's features
        # Create shifted target: "Next Day TRG"
        line_data['next_trg'] = line_data['trg'].shift(-1)
        
        # Drop last row since it has no 'next_trg' to train on
        train_data = line_data.dropna(subset=['next_trg'])
        
        if len(train_data) < 2:
            last_trg = line_data['trg'].iloc[-1]
            if pd.isna(last_trg): last_trg = 85.0
            predictions.append(OeePredictionResponse(
                ligne=ligne,
                predictedTrg=round(float(last_trg), 1),
                rootCauseIndicator="Insuffisant de données",
                recommendation="Attendre plus de données."
            ))
            continue

        X = train_data[features]
        y = train_data['next_trg']

        # Train a quick Random Forest
        reg = RandomForestRegressor(n_estimators=20, random_state=42)
        try:
            reg.fit(X, y)
            
            # Predict for tomorrow using the LAST available row's features
            latest_features = line_data[features].iloc[-1:]
            pred_trg = reg.predict(latest_features)[0]
            
            # Feature importance tells us what drives the model
            importances = reg.feature_importances_
            
            # Find the most important feature that NEGATIVELY impacts TRG
            # Since Random Forest feature importances are just magnitude, we check correlation
            import warnings
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", category=RuntimeWarning)
                correlations = train_data[features].corrwith(y)
                correlations = correlations.fillna(0)
            
            # We look for features that have high importance AND negative correlation,
            # or in the case of FPY, positive correlation (if FPY drops, TRG drops, meaning FPY is the root cause of a drop)
            root_cause = "Inconnu"
            recommendation = "Continuer le suivi."
            
            # If predicted TRG is below target (e.g. 85)
            if pred_trg < 85.0:
                # Let's find the "worst" feature today compared to its mean
                worst_feature = None
                max_deviation = 0
                
                for feat in features:
                    current_val = latest_features[feat].iloc[0]
                    mean_val = line_data[feat].mean()
                    
                    # For FPY and Efficience, lower is worse
                    if feat in ['fpy_vision', 'efficience']:
                        deviation = (mean_val - current_val) / (mean_val if mean_val != 0 else 1)
                        if deviation > max_deviation:
                            max_deviation = deviation
                            worst_feature = feat
                    # For Arrachement and Depannage, higher is worse
                    else:
                        deviation = (current_val - mean_val) / (mean_val if mean_val != 0 else 1)
                        if deviation > max_deviation:
                            max_deviation = deviation
                            worst_feature = feat
                
                if worst_feature:
                    root_cause = feature_names[worst_feature]
                    if worst_feature == 'fpy_vision':
                        recommendation = "Inspecter le poste Vision. Vérifier la calibration."
                    elif worst_feature == 'tx_arrachement':
                        recommendation = "Vérifier la qualité des composants et les outils de serrage."
                    elif worst_feature == 'encours_depannage':
                        recommendation = "Affecter plus de techniciens au dépannage."
                    elif worst_feature == 'efficience':
                        recommendation = "Vérifier l'affectation du personnel et les temps de cycle."
            else:
                root_cause = "Performances nominales"
                recommendation = "Maintenir les paramètres actuels."

            pred_trg = float(pred_trg)
            predictions.append(OeePredictionResponse(
                ligne=ligne,
                predictedTrg=round(pred_trg, 1),
                rootCauseIndicator=root_cause,
                recommendation=recommendation
            ))

        except Exception as e:
            print(f"Error predicting OEE for line {ligne}: {e}")
            last_trg = line_data['trg'].iloc[-1] if not line_data.empty else 85.0
            if pd.isna(last_trg): last_trg = 85.0
            predictions.append(OeePredictionResponse(
                ligne=ligne,
                predictedTrg=round(float(last_trg), 1),
                rootCauseIndicator="Erreur modèle",
                recommendation=""
            ))

    # Sort lines
    predictions.sort(key=lambda x: x.ligne)
    return predictions


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
