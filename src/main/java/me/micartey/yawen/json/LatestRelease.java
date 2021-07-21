package me.micartey.yawen.json;

import com.google.gson.annotations.SerializedName;

public class LatestRelease {

    @SerializedName("assets")
    public AssetInfo[] assetInfos;

    @SerializedName("name")
    public String releaseName;

}
