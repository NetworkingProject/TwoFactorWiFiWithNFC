package com.example.nfcproject;

import android.os.Parcel;
import android.os.Parcelable;

// A ParcelableUtil taken from Stack Overflow
public class ParcelableUtil {
    public static byte[] marshall(Parcelable parceable) {
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            parceable.writeToParcel(parcel, 0);
            return parcel.marshall();
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }

    /**
     * Note: this creates a Parcel which the caller is responsible for
     * recycling!
     */
    public static Parcel unmarshall(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return parcel;
    }

    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {

        Parcel parcel = null;
        try {
            parcel = unmarshall(bytes);
            return creator.createFromParcel(parcel);
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }
}
