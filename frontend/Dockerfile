# syntax=docker/dockerfile:1

FROM public.ecr.aws/docker/library/node:22.23.2-alpine3.24 AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

FROM public.ecr.aws/docker/library/nginx:1.30.5-alpine3.24
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
