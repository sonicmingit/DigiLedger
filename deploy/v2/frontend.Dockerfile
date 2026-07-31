FROM node:22-alpine AS build
WORKDIR /app
COPY fronten2.0/package.json fronten2.0/package-lock.json ./
RUN npm ci
COPY fronten2.0/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist/ /usr/share/nginx/html/
COPY deploy/v2/nginx.conf.template /etc/nginx/templates/default.conf.template
EXPOSE 80
