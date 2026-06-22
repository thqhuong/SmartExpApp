const fs = require("fs");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} = require("@firebase/rules-unit-testing");
const {
  deleteDoc,
  doc,
  getDoc,
  setDoc
} = require("firebase/firestore");

const PROJECT_ID = "smart-exp-rules-test";

describe("firestore.rules", function () {
  this.timeout(20000);

  let testEnv;

  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: "127.0.0.1",
        port: 8080,
        rules: fs.readFileSync("firestore.rules", "utf8")
      }
    });
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  after(async () => {
    await testEnv.cleanup();
  });

  function authed(uid) {
    return testEnv.authenticatedContext(uid).firestore();
  }

  function anon() {
    return testEnv.unauthenticatedContext().firestore();
  }

  function product(ownerUserId = "user-a") {
    return {
      localId: "local-product-1",
      ownerUserId,
      name: "Milk",
      category: "Dairy",
      quantity: "1",
      unit: "pcs",
      storageLocationId: "refrigerator",
      expiryDateMillis: 123456789,
      status: "ACTIVE",
      createdAt: 100,
      updatedAt: 200
    };
  }

  it("allows an owner to create and read their product", async () => {
    const db = authed("user-a");
    const productRef = doc(db, "users/user-a/products/product-1");

    await assertSucceeds(setDoc(productRef, product()));
    await assertSucceeds(getDoc(productRef));
  });

  it("denies cross-user and unauthenticated product access", async () => {
    await assertFails(setDoc(doc(authed("user-a"), "users/user-b/products/product-1"), product("user-b")));
    await assertFails(getDoc(doc(authed("user-a"), "users/user-b/products/product-1")));
    await assertFails(setDoc(doc(anon(), "users/user-a/products/product-1"), product()));
  });

  it("denies product owner mismatch and unknown product fields", async () => {
    await assertFails(setDoc(doc(authed("user-a"), "users/user-a/products/product-1"), product("user-b")));
    await assertFails(setDoc(doc(authed("user-a"), "users/user-a/products/product-1"), {
      ...product(),
      unexpectedField: true
    }));
  });

  it("allows product tombstones for owner-scoped deletes", async () => {
    await assertSucceeds(setDoc(doc(authed("user-a"), "users/user-a/products/product-1"), {
      localId: "local-product-1",
      ownerUserId: "user-a",
      updatedAt: 300,
      deletedAt: 300
    }));
  });

  it("allows valid settings only for the owner", async () => {
    const settings = {
      ownerUserId: "user-a",
      displayName: "Kitchen Team",
      reminderDaysBefore: 3,
      reminderNotifyTimeMinutes: 540,
      dietaryPreferences: "vegetarian",
      darkMode: true,
      languageTag: "en",
      defaultStorageLocationId: "room_temp",
      notificationEnabled: true,
      createdAt: 100,
      updatedAt: 200
    };

    await assertSucceeds(setDoc(doc(authed("user-a"), "users/user-a/settings/default"), settings));
    await assertFails(setDoc(doc(authed("user-b"), "users/user-a/settings/default"), settings));
    await assertFails(setDoc(doc(authed("user-a"), "users/user-a/settings/default"), {
      ...settings,
      darkMode: "yes"
    }));
  });

  it("allows valid inventory actions only for the owner", async () => {
    const action = {
      ownerUserId: "user-a",
      localId: "action-1",
      productLocalId: "product-1",
      actionType: "CONSUMED",
      quantityChanged: 1,
      actionAt: 100,
      note: "Used",
      createdAt: 100,
      updatedAt: 100
    };

    await assertSucceeds(setDoc(doc(authed("user-a"), "users/user-a/inventoryActions/action-1"), action));
    await assertFails(setDoc(doc(authed("user-a"), "users/user-a/inventoryActions/action-2"), {
      ...action,
      actionType: "DELETED"
    }));
  });

  it("allows exact category fields for the owner", async () => {
    const category = {
      ownerUserId: "user-a",
      localId: "category-1",
      name: "Snacks",
      sortOrder: 10,
      isBuiltIn: false,
      active: true,
      createdAt: 100,
      updatedAt: 200
    };

    await assertSucceeds(setDoc(doc(authed("user-a"), "users/user-a/categories/category-1"), category));
    await assertSucceeds(setDoc(doc(authed("user-a"), "users/user-a/categories/category-1"), {
      ...category,
      active: false,
      deletedAt: 300,
      updatedAt: 300
    }));
    await assertFails(setDoc(doc(authed("user-a"), "users/user-a/categories/category-2"), {
      ...category,
      extra: "nope"
    }));
  });

  it("allows owner reads but denies client writes for storage locations", async () => {
    await assertSucceeds(getDoc(doc(authed("user-a"), "users/user-a/storageLocations/room_temp")));
    await assertFails(setDoc(doc(authed("user-a"), "users/user-a/storageLocations/room_temp"), {
      ownerUserId: "user-a",
      name: "Room Temp"
    }));
  });

  it("denies unknown user paths and top-level paths", async () => {
    await assertFails(setDoc(doc(authed("user-a"), "users/user-a/agentMessages/message-1"), {
      ownerUserId: "user-a"
    }));
    await assertFails(setDoc(doc(authed("user-a"), "products/product-1"), product()));
  });

  it("allows owner hard delete only on product and category docs", async () => {
    await assertSucceeds(deleteDoc(doc(authed("user-a"), "users/user-a/products/product-1")));
    await assertSucceeds(deleteDoc(doc(authed("user-a"), "users/user-a/categories/category-1")));
    await assertFails(deleteDoc(doc(authed("user-a"), "users/user-a/settings/default")));
  });
});
