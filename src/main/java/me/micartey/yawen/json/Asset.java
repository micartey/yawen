package me.micartey.yawen.json;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class Asset {

    @SerializedName("browser_download_url")
    public String browserDownloadUrl;

    @SerializedName("download_count")
    public int downloadCount;

    @SerializedName("updated_at")
    public String updated;

    @SerializedName("state")
    public String state;

    @SerializedName("name")
    public String name;

    @SerializedName("id")
    public String id;
}
