package com.valkoja.streamwatch;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class Stream
{
    @PrimaryKey @NonNull
    public String id;

    public boolean online;
    public String name;
    public String desc;
    public String url;

    // Room:in vaatimuksia jotta toisen rakentajan voi toteuttaa
    Stream() {}

    @Ignore
    Stream(String argID, String argName)
    {
        id = argID;
        url = "";
        name = argName;
        desc = "Offline";
        online = false;
    }
}
