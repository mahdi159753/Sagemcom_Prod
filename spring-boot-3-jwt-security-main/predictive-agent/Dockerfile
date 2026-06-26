# ============================================================
# Etape 1: Builder - Bch ninstallou les dépendances Python
# ============================================================
FROM python:3.11-slim AS builder

WORKDIR /app

# Nbaddlou PATH bch ywalli el packages mta3 pip accessible
ENV PATH="/root/.local/bin:$PATH"

# Ninstallou les dépendances fi dossier /install bch na7zouhom lel image finale
COPY requirements.txt .
RUN pip install --user --no-cache-dir -r requirements.txt

# ============================================================
# Etape 2: Runner - Image finale sghira w ndhifa
# ============================================================
FROM python:3.11-slim

WORKDIR /app

# Na7zou les packages eli installina fel etape l'lola
COPY --from=builder /root/.local /root/.local

# Nbaddlou PATH marra okhra
ENV PATH="/root/.local/bin:$PATH"

# Nkoppiw ken les fichiers eli ta7tajhom (men ghir __pycache__ wala .env local)
COPY main.py .
COPY requirements.txt .

# El GROQ_API_KEY bch yji men Kubernetes Secret (mech men .env)
# Nhotu port mtaa el agent
EXPOSE 8000

# El commande mta3 el launch kima fi runcmd.txt
CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
