package com.example.nfcproject;

import android.os.Parcel;
import android.os.Parcelable;

public class ConfigSerialization implements Parcelable {
    public String SSID;
    public String password;
    public String secondaryPassword;
    public boolean isTwoFactor;
    public boolean isHidden;
    public int keyManagement; // This is 0-3

    // With Secondary Password
    public ConfigSerialization(String sSID, String password, String secondaryPassword, boolean isTwoFactor, boolean isHidden, int keyMangement) {
        this.SSID = sSID;
        this.password = password;
        this.secondaryPassword = secondaryPassword;
        this.isTwoFactor = isTwoFactor;
        this.isHidden = isHidden;
        this.keyManagement = keyMangement;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(SSID);
        dest.writeInt(isHidden ? 1 : 0);
        dest.writeInt(isTwoFactor ? 1 : 0);
        dest.writeString(password);
        dest.writeString(secondaryPassword);
        dest.writeInt(keyManagement);
    }

    /** Implement the Parcelable interface {@hide} */
    public static final Creator<ConfigSerialization> CREATOR =
        new Creator<ConfigSerialization>() {
            @Override
            public ConfigSerialization createFromParcel(Parcel in) {
                String SSID = in.readString();
                int isHiddenInt = in.readInt();
                int isTwoFactorInt = in.readInt();
                String password = in.readString();
                String secondaryPassword = in.readString();
                int keyManagement = in.readInt();

                boolean isHidden = isHiddenInt == 1 ? true : false;
                boolean isTwoFactor = isTwoFactorInt == 1 ? true : false;
                ConfigSerialization config = new ConfigSerialization(SSID, password, secondaryPassword,
                        isTwoFactor, isHidden, keyManagement);
                return config;
            }

            @Override
            public ConfigSerialization[] newArray(int size) {
                // TODO Auto-generated method stub
                return null;
            }

        };
}
