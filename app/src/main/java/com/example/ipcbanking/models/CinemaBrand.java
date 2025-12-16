package com.example.ipcbanking.models;

public class CinemaBrand {
    private String name;
    private String logoUrl;

    public CinemaBrand() {
        // Default constructor required for calls to DataSnapshot.getValue(CinemaBrand.class)
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
