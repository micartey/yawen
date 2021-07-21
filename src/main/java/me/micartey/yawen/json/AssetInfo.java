package me.micartey.yawen.json;

import com.google.gson.annotations.SerializedName;

public class AssetInfo {

    @SerializedName("browser_download_url")
    public String browserDownloadUrl;

    @SerializedName("download_count")
    public int downloadCount;

    public String state;

    public String name;

}
