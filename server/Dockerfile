# Portable across Render, Google Cloud Run and Fly.io: a plain Node process that reads PORT from
# the environment and binds all interfaces. TLS is terminated by the platform.
FROM node:22-alpine

# The proxy has no runtime dependencies, so there is no install step and no lockfile to honour.
WORKDIR /app

# Copy only what the server needs. Tests and docs are not part of the runtime image.
COPY package.json ./
COPY server.mjs ./

# Never run as root.
USER node

# Platforms inject PORT. The default matches local development.
ENV PORT=8787
ENV HOST=0.0.0.0
EXPOSE 8787

# No secrets are baked in. OPENROUTER_API_KEY and VOXBOX_CLIENT_TOKEN must come from the
# platform's secret store at runtime; the server refuses to forward provider traffic without them.
CMD ["node", "server.mjs"]
