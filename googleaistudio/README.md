<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />
</div>

# Run and deploy your AI Studio app

> [!NOTE]
> This directory (`googleaistudio/`) contains reference-only web prototype assets. It is **not** built or packaged into the production Android application.


This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/17ec2e24-fe02-44af-9ae1-6a080e9f6d3b

## Run Locally

**Prerequisites:**  Node.js


1. Install dependencies:
   `npm install`
2. Set the `GEMINI_API_KEY` in [.env.local](.env.local) if you are experimenting with prototype AI calls
3. Run the app:
   `npm run dev`

`npm run dev` starts the Vite frontend only. There is no checked-in Express server or prototype recipe API backend in this directory. Treat any backend-shaped calls in the prototype as reference material, not as production Android app infrastructure.
