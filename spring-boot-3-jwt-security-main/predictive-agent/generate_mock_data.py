import requests
from datetime import datetime, timedelta
import random

# Base URL for the Spring Boot application
BASE_URL = "http://localhost:8081/api/v1/daily-production"

# Auth credentials (adjust if needed, assuming the ones in main.py work)
AUTH_URL = "http://localhost:8081/api/v1/auth/authenticate"
CREDENTIALS = {
    "email": "admin@mail.com",
    "password": "password"
}

def get_token():
    try:
        response = requests.post(AUTH_URL, json=CREDENTIALS)
        response.raise_for_status()
        return response.json().get("access_token")
    except Exception as e:
        print(f"Failed to get token: {e}")
        # Return empty, assume the endpoint might not be strictly secured or we can use the ai one
        try:
             res2 = requests.post(AUTH_URL, json={"email":"ai@sagemcom.com", "password":"aipassword"})
             return res2.json().get("access_token")
        except:
             print("Could not authenticate. Proceeding without token.")
             return ""

def generate_mock_data():
    token = get_token()
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"} if token else {"Content-Type": "application/json"}
    
    chantier = "INTEG"
    days_to_generate = 14
    today = datetime.now()
    
    # Base profiles for lines
    line_profiles = {
        1: {"trg": 88, "fpy": 98, "arr": 0.5, "dep": 2}, # Good line
        2: {"trg": 84, "fpy": 95, "arr": 1.0, "dep": 5}, # Average line
        3: {"trg": 75, "fpy": 90, "arr": 3.0, "dep": 15}, # Problematic line
        4: {"trg": 86, "fpy": 96, "arr": 0.8, "dep": 4},
        # 5-8 are mostly inactive but we generate some data
    }
    
    print(f"Generating {days_to_generate} days of mock data for {chantier}...")
    
    for day_offset in range(days_to_generate, -1, -1):
        date_str = (today - timedelta(days=day_offset)).strftime("%Y-%m-%d")
        batch = []
        
        for ligne, profile in line_profiles.items():
            # Add some randomness to the profile
            trg = profile["trg"] + random.uniform(-5, 5)
            fpy = profile["fpy"] + random.uniform(-3, 2)
            arr = max(0, profile["arr"] + random.uniform(-0.5, 1.5))
            dep = max(0, int(profile["dep"] + random.uniform(-2, 5)))
            efficience = 100 + random.uniform(-10, 5)
            produced = int(trg * 10) + random.randint(-50, 50)
            
            # Sometimes a line has a really bad day (e.g. ligne 3)
            if ligne == 3 and random.random() < 0.3:
                fpy -= 10
                dep += 20
                trg -= 15
                
            batch.extend([
                {"date": date_str, "chantier": chantier, "ligne": ligne, "indicatorName": "TRG (%)", "value": str(round(trg, 1))},
                {"date": date_str, "chantier": chantier, "ligne": ligne, "indicatorName": "FPY Vision / FCT2 (%)", "value": str(round(fpy, 1))},
                {"date": date_str, "chantier": chantier, "ligne": ligne, "indicatorName": "Tx Arrachement (%)", "value": str(round(arr, 1))},
                {"date": date_str, "chantier": chantier, "ligne": ligne, "indicatorName": "Encours Depannage", "value": str(dep)},
                {"date": date_str, "chantier": chantier, "ligne": ligne, "indicatorName": "Efficience / Ecart DMH", "value": str(round(efficience, 1))},
                {"date": date_str, "chantier": chantier, "ligne": ligne, "indicatorName": "Quantites Produites", "value": str(produced)},
                {"date": date_str, "chantier": chantier, "ligne": ligne, "indicatorName": "Objectif Planning (%)", "value": "100"}
            ])
            
        # Send batch
        print(f"Sending batch for {date_str} ({len(batch)} indicators)...")
        try:
            response = requests.post(f"{BASE_URL}/batch", json=batch, headers=headers)
            response.raise_for_status()
        except Exception as e:
            print(f"Failed to save batch for {date_str}: {e}")

if __name__ == "__main__":
    generate_mock_data()
    print("Done generating mock data!")
