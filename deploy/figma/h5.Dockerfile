FROM node:22-alpine AS build
WORKDIR /app
COPY uniapp-figma/package.json uniapp-figma/package-lock.json ./
RUN npm ci
COPY uniapp-figma/ ./
RUN npm run build:h5

FROM nginx:1.27-alpine
COPY --from=build /app/dist/build/h5/ /usr/share/nginx/html/
COPY deploy/figma/h5.nginx.conf.template /etc/nginx/templates/default.conf.template
EXPOSE 80
