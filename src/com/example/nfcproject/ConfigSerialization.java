package com.example.nfcproject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import android.net.wifi.WifiConfiguration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Parcel;
import android.os.Parcelable;

public class ConfigSerialization implements Parcelable {
    public String SSID;
    public String password;
    public boolean isTwoFactor;
    public boolean isHidden;
    public int keyManagement; // This is 0-3

    // With Secondary Password
    public ConfigSerialization(String sSID, String password,
            boolean isTwoFactor, boolean isHidden, int keyMangement) {
        this.SSID = sSID;
        this.password = password;
        this.isTwoFactor = isTwoFactor;
        this.isHidden = isHidden;
        this.keyManagement = keyMangement;
    }

    public ConfigSerialization(NdefMessage ndefMessage) {
        byte[] bytes = ndefMessage.toByteArray();
        Parcel parcel = Parcel.obtain();
        // parcel.readByteArray(bytes);
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        try {
            parcel.readValue(ConfigSerialization.class.getClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            parcel.recycle();
        }

    }

    public ConfigSerialization(Parcel in) {
        SSID = in.readString();
        isHidden = in.readByte() != 0;
        isTwoFactor = in.readByte() != 0;
        password = in.readString();
        keyManagement = in.readByte();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(SSID);
        dest.writeByte((byte) (isHidden ? 1 : 0));
        dest.writeByte((byte) (isTwoFactor ? 1 : 0));
        dest.writeString(password);
        dest.writeByte((byte) keyManagement);
    }

    /** Implement the Parcelable interface {@hide} */
    public static final Creator<ConfigSerialization> CREATOR = new Creator<ConfigSerialization>() {
        @Override
        public ConfigSerialization createFromParcel(Parcel in) {
            return new ConfigSerialization(in);
        }

        @Override
        public ConfigSerialization[] newArray(int size) {
            return new ConfigSerialization[size];
        }

    };

    public WifiConfiguration toWifiConfig(String secondaryPassword) {
        WifiConfiguration conf = new WifiConfiguration();
        String passwordForConfig = null;

        // Good ole hardcoded values here, nothing to see move along
        // <item>None</item>
        // <item>WEP</item>
        // <item>WPA/WPA2 PSK</item>
        // <item>802.1x EAP</item>

        boolean isOpen = this.keyManagement == 0 && this.password.isEmpty();
        boolean isWEP = (this.keyManagement == 1);
        boolean isWPA = (this.keyManagement == 2);

        conf.SSID = "\"" + this.SSID + "\"";
        conf.status = WifiConfiguration.Status.ENABLED;

        if (isTwoFactor) {
            try {
                passwordForConfig = Hashing.passwordToSHA256(this.password, secondaryPassword);
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

        byte[] parcelByteArray = ParcelableUtil.marshall(this);

        NdefRecord rtdTextRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], parcelByteArray);

        message = new NdefMessage(new NdefRecord[] { rtdTextRecord });

        return message;
    }

}
