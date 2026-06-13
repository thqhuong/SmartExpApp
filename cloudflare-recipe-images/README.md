# SmartExp AI Worker

Cloudflare Worker endpoints for SmartExp AI features using Workers AI.

## Setup

1. Revoke any Cloudflare token that was pasted into chat or logs.
2. Create a new least-privilege Cloudflare API token for local deployment.
3. Install dependencies:

```powershell
npm install
```

4. Log in or provide the token only in your local shell:

```powershell
npx wrangler login
```

or

```powershell
$env:CLOUDFLARE_API_TOKEN="NEW_TOKEN_VALUE"
```

5. Deploy:

```powershell
npm run deploy
```

6. Put the deployed public Worker URL into Android `local.properties`:

```properties
AI_WORKER_URL=https://smart-exp-recipe-images.YOUR_SUBDOMAIN.workers.dev
RECIPE_IMAGE_WORKER_URL=https://smart-exp-recipe-images.YOUR_SUBDOMAIN.workers.dev
PRODUCT_PARSER_WORKER_URL=https://smart-exp-recipe-images.YOUR_SUBDOMAIN.workers.dev
```

The Android app never stores a Cloudflare API token.

## Endpoints

- `GET /recipe-image`: returns a generated recipe image.
- `POST /parse-product`: returns Smart Add product drafts using Workers AI JSON output.
- `POST /generate-recipes`: returns generated recipe suggestions using Workers AI JSON output.
- `POST /answer-inventory`: returns a short inventory assistant answer.
