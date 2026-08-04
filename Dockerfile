# Build for the VoxBox proxy.
#
# This lives at the repository root on purpose. Render resolves the Dockerfile path from the repo
# root and an existing service keeps the build settings it was created with, so a Dockerfile nested
# in server/ fails with "open Dockerfile: no such file or directory" even after the blueprint is
# corrected. A root Dockerfile works with Render's defaults and with every other platform.
#
# `.dockerignore` keeps the Android project, evidence and build outputs out of the context, so the
# context stays a few kilobytes despite being rooted here.
FROM node:22-alpine

WORKDIR /app

# The proxy has no runtime dependencies, so there is no install step and no lockfile to honour.
COPY server/package.json ./
COPY server/server.mjs ./

# Never run as root.
USER node

# Platforms inject PORT. The default matches local development.
ENV PORT=8787
ENV HOST=0.0.0.0
EXPOSE 8787

# No secrets are baked in. OPENROUTER_API_KEY and VOXBOX_CLIENT_TOKEN must come from the platform's
# secret store at runtime; the server refuses to forward provider traffic without them.
CMD ["node", "server.mjs"]
