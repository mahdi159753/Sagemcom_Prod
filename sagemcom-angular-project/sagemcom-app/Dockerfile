# Etape 1: Build (Node)
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration=production

# Etape 2: Run (NGINX) - Image sghira w Proxy
FROM nginx:alpine
# nfas5ou default html mtaa nginx
RUN rm -rf /usr/share/nginx/html/*
# Nhezou l'build mtaa Angular
# Remarque: nheb nhez l'dossier dist lkol w baed ken lzem ntala3 el contenue b command cp
COPY --from=builder /app/dist/sagemcom-production-manager/browser /usr/share/nginx/html/
# Nsobou l'fichier mtaa nginx.conf li sna3neh bch yaamel reverse proxy lel API
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
