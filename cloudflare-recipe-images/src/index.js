const IMAGE_MODEL = "@cf/stabilityai/stable-diffusion-xl-base-1.0";
const PRODUCT_PARSER_MODEL = "@cf/meta/llama-3.1-8b-instruct-fast";
const TEXT_MODEL = "@cf/meta/llama-3.1-8b-instruct-fast";
const MAX_TITLE_LENGTH = 120;
const MAX_INGREDIENTS_LENGTH = 420;
const MAX_DIET_LENGTH = 160;
const MAX_PRODUCT_INPUT_LENGTH = 1800;
const MAX_PROMPT_LENGTH = 1200;
const MAX_PRODUCTS = 40;
const CACHE_TTL_SECONDS = 60 * 60 * 24 * 30;

const UNSAFE_TERMS = [
  "nsfw",
  "nude",
  "naked",
  "porn",
  "erotic",
  "gore",
  "blood",
  "weapon",
  "kill",
  "poison",
  "toxic",
  "illegal drug"
];

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (url.pathname === "/recipe-image") {
      return handleRecipeImage(request, env, ctx, url);
    }

    if (url.pathname === "/parse-product") {
      return handleParseProduct(request, env);
    }

    if (url.pathname === "/generate-recipes") {
      return handleGenerateRecipes(request, env);
    }

    if (url.pathname === "/answer-inventory") {
      return handleAnswerInventory(request, env);
    }

    return json({ error: "Not found" }, 404);
  }
};

async function handleRecipeImage(request, env, ctx, url) {
    if (request.method !== "GET") {
      return json({ error: "Method not allowed" }, 405, { Allow: "GET" });
    }

    const title = cleanParam(url.searchParams.get("title"), MAX_TITLE_LENGTH);
    const ingredients = cleanParam(url.searchParams.get("ingredients"), MAX_INGREDIENTS_LENGTH);
    const diet = cleanParam(url.searchParams.get("diet"), MAX_DIET_LENGTH) || "none";

    if (!title || !ingredients) {
      return json({ error: "Missing required title or ingredients query parameter" }, 400);
    }

    if (containsUnsafeText(`${title} ${ingredients} ${diet}`)) {
      return json({ error: "Unsafe or non-food image request rejected" }, 400);
    }

    const cached = await caches.default.match(request);
    if (cached) {
      return cached;
    }

    const prompt = [
      `Realistic plated food photo of ${title}.`,
      `Main visible ingredients: ${ingredients}.`,
      "Food only. No people, no hands, no packaging, no labels, no watermark, no text.",
      "Natural light, clean kitchen table, appetizing but realistic.",
      `Dietary preferences: ${diet}.`
    ].join(" ");

    try {
      const image = await env.AI.run(IMAGE_MODEL, {
        prompt,
        negative_prompt: "text, labels, watermark, logo, people, hands, packaging, blurry, dark, unsafe food",
        width: 1024,
        height: 768,
        num_steps: 20,
        guidance: 7.5
      });

      const response = new Response(image, {
        headers: {
          "Content-Type": "image/png",
          "Cache-Control": `public, max-age=${CACHE_TTL_SECONDS}`,
          "X-Recipe-Image-Model": IMAGE_MODEL
        }
      });

      ctx.waitUntil(caches.default.put(request, response.clone()));
      return response;
    } catch (error) {
      return json({ error: "Image generation failed" }, 502);
    }
}

async function handleParseProduct(request, env) {
  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405, { Allow: "POST" });
  }

  let body;
  try {
    body = await request.json();
  } catch (error) {
    return json({ error: "Invalid JSON body" }, 400);
  }

  const input = cleanParam(body.input, MAX_PRODUCT_INPUT_LENGTH);
  const languageTag = normalizeLanguageTag(body.languageTag);
  if (!input) {
    return json({ error: "Missing input" }, 400);
  }

  const today = new Date().toISOString().slice(0, 10);
  const prompt = [
    `Today is ${today}.`,
    "Extract inventory products from the user's text for SmartExpApp.",
    "Return one item per distinct product mentioned by the user. Do not invent products.",
    "Normalize category to one of: Dairy, General, Meat, Pantry, Produce, Vegetables.",
    "Normalize storage to one of: Room Temp, Refrigerator, Freezer.",
    "Normalize units to one of: pcs, g, kg, ml, l, oz, lb, cup, tbsp, tsp, gal, bag, loaf.",
    "Quantity must be numeric text only, without the unit. If quantity is missing, use quantity '1' and unit 'pcs'.",
    "If expiry is relative and clear, set expiryDaysFromNow to a non-negative integer.",
    "If expiry is unclear or absent, set expiryDaysFromNow to -1 and include expiryText as an empty string.",
    "Support English and Vietnamese. Normalize Vietnamese food names with proper diacritics.",
    "Examples: thit bo -> Thịt bò, ca chua -> Cà chua, rau muong -> Rau muống.",
    "Vietnamese storage: tu lanh/ngan mat -> Refrigerator; ngan dong/dong lanh -> Freezer.",
    "Vietnamese expiry: hom nay -> 0; ngay mai -> 1.",
    "Use these exact Vietnamese mappings: thit bo -> Th\u1ecbt b\u00f2; ca chua -> C\u00e0 chua; rau muong -> Rau mu\u1ed1ng.",
    "When examples conflict, use the exact Vietnamese mappings above.",
    "Do not duplicate items. Do not split one product's quantity, expiry, or storage into a separate item.",
    "Preserve the user's product order. Apply quantity, storage, and expiry only to the product in the same clause.",
    "Do not carry an expiry or storage phrase from one comma/semicolon-separated product clause to another.",
    "Example: 'Add milk, 1 gallon, expires tomorrow, fridge' is exactly one item: milk.",
    `User language: ${languageTag}.`,
    `User text: ${input}`
  ].join("\n");

  try {
    const response = await env.AI.run(PRODUCT_PARSER_MODEL, {
      messages: [
        {
          role: "system",
          content: "You are a strict JSON product parser. Return only data matching the provided JSON schema."
        },
        {
          role: "user",
          content: prompt
        }
      ],
      temperature: 0,
      max_tokens: 500,
      response_format: {
        type: "json_schema",
        json_schema: {
          type: "object",
          properties: {
            items: {
              type: "array",
              items: {
                type: "object",
                properties: {
                  name: { type: "string" },
                  category: { type: "string" },
                  quantity: { type: "string" },
                  unit: { type: "string" },
                  storage: { type: "string" },
                  expiryText: { type: "string" },
                  expiryDaysFromNow: { type: "integer" }
                },
                required: ["name", "category", "quantity", "unit", "storage", "expiryText", "expiryDaysFromNow"]
              }
            }
          },
          required: ["items"]
        }
      }
    });

    const parsed = parseAiJson(response);
    const normalizedItems = Array.isArray(parsed.items) ? parsed.items.map(normalizeProductItem).filter(Boolean) : [];
    applySingleItemExpiryHint(normalizedItems, input);
    const items = dedupeProductItems(normalizedItems).slice(0, 12);
    return json({ items, model: PRODUCT_PARSER_MODEL }, 200);
  } catch (error) {
    return json({ error: "Product parsing failed" }, 502);
  }
}

async function handleGenerateRecipes(request, env) {
  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405, { Allow: "POST" });
  }

  let body;
  try {
    body = await request.json();
  } catch (error) {
    return json({ error: "Invalid JSON body" }, 400);
  }

  const prompt = cleanParam(body.prompt, MAX_PROMPT_LENGTH);
  const languageTag = normalizeLanguageTag(body.languageTag);
  const dietaryPreferences = cleanParam(body.dietaryPreferences, MAX_DIET_LENGTH);
  const products = normalizeProducts(body.products);

  if (containsUnsafeText(`${prompt} ${dietaryPreferences} ${productNames(products)}`)) {
    return json({ recipes: [], model: TEXT_MODEL }, 200);
  }

  const recipePrompt = buildRecipePrompt(prompt, languageTag, dietaryPreferences, products);
  try {
    const response = await env.AI.run(TEXT_MODEL, {
      messages: [
        {
          role: "system",
          content: "You are SmartExpApp's cooking assistant. Return only strict JSON matching the provided schema."
        },
        {
          role: "user",
          content: recipePrompt
        }
      ],
      temperature: 0.6,
      max_tokens: 1200,
      response_format: {
        type: "json_schema",
        json_schema: {
          type: "object",
          properties: {
            recipes: {
              type: "array",
              items: {
                type: "object",
                properties: {
                  title: { type: "string" },
                  summary: { type: "string" },
                  usedIngredients: { type: "array", items: { type: "string" } },
                  actionText: { type: "string" },
                  prepTime: { type: "string" },
                  difficulty: { type: "string" },
                  calories: { type: "string" },
                  smartTip: { type: "string" },
                  allIngredients: { type: "array", items: { type: "string" } },
                  instructions: { type: "array", items: { type: "string" } }
                },
                required: ["title", "summary", "usedIngredients", "actionText", "prepTime", "difficulty", "calories", "smartTip", "allIngredients", "instructions"]
              }
            }
          },
          required: ["recipes"]
        }
      }
    });

    const parsed = parseAiJson(response);
    const recipes = Array.isArray(parsed.recipes)
      ? parsed.recipes.map(normalizeRecipe).filter(Boolean).slice(0, 3)
      : [];
    return json({ recipes, model: TEXT_MODEL }, 200);
  } catch (error) {
    return json({ error: "Recipe generation failed" }, 502);
  }
}

async function handleAnswerInventory(request, env) {
  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405, { Allow: "POST" });
  }

  let body;
  try {
    body = await request.json();
  } catch (error) {
    return json({ error: "Invalid JSON body" }, 400);
  }

  const prompt = cleanParam(body.prompt, MAX_PROMPT_LENGTH);
  const languageTag = normalizeLanguageTag(body.languageTag);
  const products = normalizeProducts(body.products);
  if (!prompt) {
    return json({ answer: "", model: TEXT_MODEL }, 200);
  }

  if (containsUnsafeText(`${prompt} ${productNames(products)}`)) {
    return json({ answer: languageTag === "vi" ? "Tôi chỉ có thể hỗ trợ câu hỏi an toàn về thực phẩm và kho của bạn." : "I can only help with safe food and inventory questions.", model: TEXT_MODEL }, 200);
  }

  try {
    const response = await env.AI.run(TEXT_MODEL, {
      messages: [
        {
          role: "system",
          content: "You are SmartExpApp's inventory assistant. Answer concisely and never claim to save, delete, or change inventory."
        },
        {
          role: "user",
          content: buildInventoryAnswerPrompt(prompt, languageTag, products)
        }
      ],
      temperature: 0.3,
      max_tokens: 220
    });
    return json({ answer: cleanParam(aiText(response), 1200), model: TEXT_MODEL }, 200);
  } catch (error) {
    return json({ error: "Inventory answer failed" }, 502);
  }
}

function cleanParam(value, maxLength) {
  return (value || "").replace(/\s+/g, " ").trim().slice(0, maxLength);
}

function containsUnsafeText(value) {
  const lower = value.toLowerCase();
  return UNSAFE_TERMS.some((term) => lower.includes(term));
}

function parseAiJson(response) {
  if (response && typeof response.response === "string") {
    return JSON.parse(response.response);
  }
  if (response && typeof response.result === "string") {
    return JSON.parse(response.result);
  }
  if (response && typeof response.text === "string") {
    return JSON.parse(response.text);
  }
  if (response && response.items) {
    return response;
  }
  if (response && response.response && typeof response.response === "object") {
    return response.response;
  }
  throw new Error("No parseable JSON response");
}

function aiText(response) {
  if (response && typeof response.response === "string") {
    return response.response;
  }
  if (response && typeof response.result === "string") {
    return response.result;
  }
  if (response && typeof response.text === "string") {
    return response.text;
  }
  return "";
}

function buildRecipePrompt(userPrompt, languageTag, dietaryPreferences, products) {
  const hasUserPrompt = userPrompt.length > 0;
  const languageInstruction = languageTag === "vi"
    ? "Write every user-facing recipe field in Vietnamese, including title, summary, actionText, difficulty, smartTip, allIngredients, and instructions."
    : "Write every user-facing recipe field in English, including title, summary, actionText, difficulty, smartTip, allIngredients, and instructions.";
  const requestInstruction = hasUserPrompt
    ? "The user typed a recipe request. Satisfy that request first, even if it needs ingredients not currently in inventory. Use inventory items when they naturally fit, especially expiring items, but do not force every recipe to use only inventory."
    : "No specific user recipe request was provided. Suggest practical recipes that prioritize local inventory items, especially items expiring soon. You may add common pantry ingredients when needed.";
  return [
    languageInstruction,
    requestInstruction,
    "Generate food recipes only. If the user request is unsafe or not food-related, return an empty recipes array.",
    "Return exactly 3 distinct, diverse, and different practical recipe objects unless the request is unsafe (do not suggest duplicate or highly similar dishes).",
    "Crucial: The 3 recipes must be highly diverse and distinct from each other in cooking method (e.g. one sauté/skillet, one soup/stew, one baked or salad) and cuisine style. Do not suggest variations of the same dish.",
    "Fields required: title, summary, usedIngredients, actionText, prepTime, difficulty, calories, smartTip, allIngredients, instructions.",
    "prepTime must be a very short string indicating only duration, e.g., '20 min' or '1 hour' (no descriptive sentences).",
    "difficulty must be strictly one of 'Easy', 'Medium', or 'Hard' (or Vietnamese equivalents).",
    "calories must be a very short string strictly in the format 'XXX kcal', e.g., '450 kcal' (no descriptive sentences).",
    "usedIngredients must contain only inventory items that naturally fit; use an empty array when no inventory item fits.",
    "allIngredients must contain the complete ingredient list with quantities.",
    "instructions must contain concise step-by-step cooking instructions.",
    "Do not include image URLs; recipe images are generated separately.",
    `Dietary preferences: ${dietaryPreferences || "none"}.`,
    `User request: ${userPrompt || "Suggest recipes"}.`,
    `Inventory context:\n${inventoryContext(products)}`
  ].join("\n");
}

function buildInventoryAnswerPrompt(userPrompt, languageTag, products) {
  const languageInstruction = languageTag === "vi"
    ? "Answer in Vietnamese."
    : "Answer in English.";
  return [
    languageInstruction,
    "Answer concisely. Do not claim to save, delete, or change inventory.",
    "If the user asks for recipes, cooking ideas, or a specific dish, say that recipe suggestions can be refreshed and mention matching inventory items only when useful.",
    "Do not reject recipe requests just because the ingredients are not in inventory.",
    `User request: ${userPrompt}`,
    `Inventory:\n${inventoryContext(products)}`
  ].join("\n");
}

function normalizeProducts(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.slice(0, MAX_PRODUCTS).map((product) => ({
    name: cleanParam(product && product.name, 120),
    category: cleanParam(product && product.category, 40),
    quantity: cleanParam(product && product.quantity, 32),
    unit: cleanParam(product && product.unit, 16),
    storage: cleanParam(product && product.storage, 40),
    expiryStatus: cleanParam(product && product.expiryStatus, 40),
    daysUntilExpiry: Number.isFinite(Number(product && product.daysUntilExpiry)) ? Number(product.daysUntilExpiry) : null
  })).filter((product) => product.name);
}

function inventoryContext(products) {
  if (!products.length) {
    return "- No saved inventory items.";
  }
  return products.map((product) => {
    const amount = [product.quantity, product.unit].filter(Boolean).join(" ") || "1 pcs";
    const expiry = product.expiryStatus || (product.daysUntilExpiry === null ? "unknown" : `${product.daysUntilExpiry} days`);
    return `- ${product.name}, ${amount}, ${product.category || "General"}, ${product.storage || "Room Temp"}, expires in ${expiry}`;
  }).join("\n");
}

function productNames(products) {
  return products.map((product) => product.name).join(" ");
}

function normalizeRecipe(item) {
  const title = cleanParam(item && item.title, 120);
  if (!title) {
    return null;
  }
  const usedIngredients = normalizeStringArray(item.usedIngredients, 12);
  const allIngredients = normalizeStringArray(item.allIngredients, 24);
  const prepTimeVal = cleanParam(item && item.prepTime, 30) || "20 min";
  const prepTime = prepTimeVal.length > 12 ? "20 min" : prepTimeVal;
  const difficulty = cleanParam(item && item.difficulty, 30) || "Medium";
  const caloriesVal = cleanParam(item && item.calories, 30) || "400 kcal";
  const calories = caloriesVal.length > 12 ? "400 kcal" : caloriesVal;
  return {
    title,
    summary: cleanParam(item.summary, 280) || "A practical recipe suggestion for your inventory.",
    usedIngredients,
    actionText: cleanParam(item.actionText, 40) || "View Recipe",
    prepTime,
    difficulty,
    calories,
    smartTip: cleanParam(item.smartTip, 220),
    allIngredients: allIngredients.length ? allIngredients : usedIngredients,
    instructions: normalizeStringArray(item.instructions, 12)
  };
}

function normalizeStringArray(value, limit) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((item) => cleanParam(item, 180)).filter(Boolean).slice(0, limit);
}

function normalizeProductItem(item) {
  const name = normalizeProductName(item.name);
  if (!name) {
    return null;
  }
  const unit = normalizeUnit(item.unit);
  return {
    name,
    category: normalizeCategory(item.category),
    quantity: normalizeQuantity(item.quantity) || "1",
    unit,
    storage: normalizeStorage(item.storage),
    expiryText: cleanParam(item.expiryText, 120),
    expiryDaysFromNow: normalizeExpiryDays(item.expiryDaysFromNow)
  };
}

function dedupeProductItems(items) {
  const merged = new Map();
  for (const item of items) {
    const key = [
      item.name.toLowerCase(),
      item.quantity.toLowerCase(),
      item.unit.toLowerCase(),
      item.storage.toLowerCase()
    ].join("|");
    const existing = merged.get(key);
    if (!existing || (existing.expiryDaysFromNow < 0 && item.expiryDaysFromNow >= 0)) {
      merged.set(key, item);
    }
  }
  return Array.from(merged.values());
}

function applySingleItemExpiryHint(items, input) {
  if (items.length !== 1) {
    return;
  }
  const lower = cleanParam(input, MAX_PRODUCT_INPUT_LENGTH).toLowerCase();
  const inDays = lower.match(/\b(?:in|after)\s+(\d+)\s+days?\b/);
  if (inDays) {
    items[0].expiryDaysFromNow = Math.min(Number.parseInt(inDays[1], 10), 3650);
    return;
  }
  if (lower.includes("tomorrow") || lower.includes("ngay mai")) {
    items[0].expiryDaysFromNow = 1;
    return;
  }
  if (lower.includes("today") || lower.includes("hom nay")) {
    items[0].expiryDaysFromNow = 0;
  }
}

function normalizeCategory(value) {
  const normalized = cleanParam(value, 32).toLowerCase();
  if (normalized === "dairy") return "Dairy";
  if (normalized === "meat") return "Meat";
  if (normalized === "pantry") return "Pantry";
  if (normalized === "produce") return "Produce";
  if (normalized === "vegetables") return "Vegetables";
  return "General";
}

function normalizeProductName(value) {
  const name = cleanParam(value, 120);
  const key = asciiKey(name);
  const vietnameseNames = new Map([
    ["thit bo", "Th\u1ecbt b\u00f2"],
    ["thit ga", "Th\u1ecbt g\u00e0"],
    ["thit heo", "Th\u1ecbt heo"],
    ["ca chua", "C\u00e0 chua"],
    ["cu hanh", "C\u1ee7 h\u00e0nh"],
    ["hanh", "H\u00e0nh"],
    ["rau muong", "Rau mu\u1ed1ng"]
  ]);
  return vietnameseNames.get(key) || name;
}

function asciiKey(value) {
  return cleanParam(value, 120)
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[đĐ]/g, "d")
    .toLowerCase()
    .replace(/[^a-z0-9 ]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function normalizeStorage(value) {
  const normalized = cleanParam(value, 64).toLowerCase();
  if (normalized.includes("fridge") || normalized.includes("refrigerator") || normalized.includes("tu lanh") || normalized.includes("ngan mat")) {
    return "Refrigerator";
  }
  if (normalized.includes("freez") || normalized.includes("frozen") || normalized.includes("ngan dong") || normalized.includes("dong lanh")) {
    return "Freezer";
  }
  return "Room Temp";
}

function normalizeQuantity(value) {
  const normalized = cleanParam(value, 32).toLowerCase();
  const match = normalized.match(/^(\d+(?:\.\d+)?)\s*(?:pcs|pieces|piece|g|kg|ml|l|oz|lb|cup|cups|tbsp|tsp|gal|gallon|gallons|bag|bags|loaf|loaves)?$/);
  return match ? match[1] : normalized;
}

function normalizeUnit(value) {
  const normalized = cleanParam(value, 32).toLowerCase();
  const allowed = ["pcs", "g", "kg", "ml", "l", "oz", "lb", "cup", "tbsp", "tsp", "gal", "bag", "loaf"];
  if (allowed.includes(normalized)) {
    return normalized;
  }
  if (normalized === "piece" || normalized === "pieces") return "pcs";
  if (normalized === "cups") return "cup";
  if (normalized === "gallon" || normalized === "gallons") return "gal";
  if (normalized === "bags") return "bag";
  if (normalized === "loaves") return "loaf";
  return "pcs";
}

function normalizeExpiryDays(value) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return -1;
  }
  return Math.min(parsed, 3650);
}

function normalizeLanguageTag(value) {
  return cleanParam(value, 16).toLowerCase().startsWith("vi") ? "vi" : "en";
}

function json(body, status, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...extraHeaders
    }
  });
}
