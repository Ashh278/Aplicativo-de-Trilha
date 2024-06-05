package com.bernardo.atvmapa;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    @SerializedName("routes")
    List<DirectionsRoute> routes;
}
