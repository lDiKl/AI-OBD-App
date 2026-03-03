FROM node:20-alpine

WORKDIR /app

# Install deps first (layer cache — only reinstalls when package.json changes)
COPY package.json package-lock.json* ./
RUN npm install

# Copy source (overridden by volume mount in dev, but needed for standalone build)
COPY . .

EXPOSE 5173

# --host makes Vite listen on 0.0.0.0 (required in Docker)
CMD ["npm", "run", "dev", "--", "--host"]
