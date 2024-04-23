package me.micartey.yawen.json;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class Release {

    @SerializedName("assets")
    public Asset[] assets;

    @SerializedName("name")
    public String releaseName;

}
