package com.example.smartexpapp.util;

import android.content.Context;

import com.example.smartexpapp.R;
import com.example.smartexpapp.data.local.LocalDataContract;

public final class StorageLabelHelper {
    private StorageLabelHelper() {
    }

    public static String getLocalizedStorage(Context context, String storage) {
        String storageId = LocalDataContract.storageIdForName(storage);
        if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageId)) {
            return context.getString(R.string.storage_cool);
        }
        if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageId)) {
            return context.getString(R.string.storage_frozen);
        }
        if (LocalDataContract.STORAGE_ROOM_TEMP_ID.equals(storageId)) {
            return context.getString(R.string.storage_room_temp);
        }
        return storage;
    }
}
