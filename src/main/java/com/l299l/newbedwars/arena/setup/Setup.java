package com.l299l.newbedwars.arena.setup;

import com.google.gson.annotations.SerializedName;

public enum Setup {
    NO_SETUP,
    NORMAL_SETUP,
    ADVANCED_SETUP,
    @SerializedName(value = "AUTOMATIC_SETUP", alternate = "BUILDING_MODE")
    AUTOMATIC_SETUP,
    READY
}
