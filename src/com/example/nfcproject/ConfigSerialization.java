package com.example.nfcproject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import android.net.wifi.WifiConfiguration;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
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
    public ConfigSerialization(String sSID, String password,
            String secondaryPassword, boolean isTwoFactor, boolean isHidden,
            int keyMangement) {
        this.SSID = sSID;
        this.password = password;
        this.secondaryPassword = secondaryPassword;
        this.isTwoFactor = isTwoFactor;
        this.isHidden = isHidden;
        this.keyManagement = keyMangement;
    }

    public ConfigSerialization(NdefMessage ndefMessage) {
        byte[] bytes = ndefMessage.toByteArray();
        Parcel parcel = Parcel.obtain();
//        parcel.readByteArray(bytes);
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        try {
            parcel.readValue(ConfigSerialization.class.getClassLoader());
        } catch (Exception e) {
        	e.printStackTrace();
        }
        finally {
            parcel.recycle();
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(SSID);
        dest.writeInt(isHidden ? 1 : 0);
        dest.writeByte((byte) (isHidden ? 1 : 0));
        dest.writeByte((byte) (isTwoFactor ? 1 : 0));
        dest.writeString(password);
        dest.writeString(secondaryPassword);
        dest.writeByte((byte) keyManagement);
    }

    /** Implement the Parcelable interface {@hide} */
    public static final Creator<ConfigSerialization> CREATOR = new Creator<ConfigSerialization>() {
        @Override
        public ConfigSerialization createFromParcel(Parcel in) {
            String SSID = in.readString();
            boolean isHidden = in.readByte() != 0;
            boolean isTwoFactor = in.readByte() != 0;
            String password = in.readString();
            String secondaryPassword = in.readString();
            int keyManagement = in.readByte();

            ConfigSerialization config = new ConfigSerialization(SSID, password, secondaryPassword, isTwoFactor, isHidden, keyManagement);
            return config;
        }

        @Override
        public ConfigSerialization[] newArray(int size) {
            // TODO Auto-generated method stub
            return null;
        }

    };

    public WifiConfiguration toWifiConfig() {
        WifiConfiguration conf = new WifiConfiguration();
        String passwordForConfig = null;

        boolean isOpen = this.keyManagement == WifiConfiguration.KeyMgmt.NONE && this.password.isEmpty();
        boolean isWEP = this.keyManagement == WifiConfiguration.KeyMgmt.NONE && (!this.password.isEmpty());
        boolean isWPA = this.keyManagement == WifiConfiguration.KeyMgmt.WPA_PSK;

        conf.SSID = this.SSID;
        conf.status = WifiConfiguration.Status.ENABLED;

        if (isTwoFactor) {
            try {
                passwordForConfig = Hashing.passwordToSHA256(this.password, this.secondaryPassword);
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            passwordForConfig = this.password;
        }

        if (isOpen) {
            // For open networks
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        } else if (isWEP || isWPA) {
            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

            if (isWEP) {
                // For WEP networks
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                conf.wepKeys[0] = "\"".concat(passwordForConfig).concat("\"");
                conf.wepTxKeyIndex = 0;
            } else if (isWPA) {
                // For WPA and WPA2 networks
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                conf.preSharedKey = "\"" + passwordForConfig + "\"";
            }
        }

        // For hidden networks
        conf.hiddenSSID = this.isHidden ? true : false;

        return conf;
    }

    public NdefMessage toNdefMessage() {
        NdefMessage message = null;
        Parcel parcel = Parcel.obtain();
        this.writeToParcel(parcel, 0);
        byte[] temp = parcel.marshall();
        byte[] parcelByteArray = new byte[1 + temp.length];
        parcelByteArray[0] = 0x01;
        System.arraycopy(temp, 0, parcelByteArray, 1, temp.length);

        NdefRecord rtdTextRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, new byte[0], parcelByteArray);

		new NdefMessage(new NdefRecord[] { rtdTextRecord });
		
        try {
            message = new NdefMessage(new NdefRecord[] { rtdTextRecord });
        }
        finally {
            parcel.recycle();
        }

        return message;
    }

}
