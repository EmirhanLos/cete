package com.emirhan.gangs.gang;

import java.util.UUID;

public class GangMember {

    private final UUID uuid;
    private String gangId;

    public GangMember(UUID uuid, String gangId) {
        this.uuid = uuid;
        this.gangId = gangId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getGangId() {
        return gangId;
    }

    public void setGangId(String gangId) {
        this.gangId = gangId;
    }
}
