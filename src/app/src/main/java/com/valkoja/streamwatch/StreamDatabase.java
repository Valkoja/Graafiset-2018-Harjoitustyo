package com.valkoja.streamwatch;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(version = 1, entities = {Stream.class}, exportSchema = false)
public abstract class StreamDatabase extends RoomDatabase
{
    private static StreamDatabase INSTANCE;
    abstract public StreamDao streamDao();

    // Yksi tapa toteuttaa singleton, koska ilmeisesti tietokannan build() on aika raskas
    public static StreamDatabase getDatabase(Context context)
    {
        if (INSTANCE == null)
        {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), StreamDatabase.class, "stream-database").build();
        }

        return INSTANCE;
    }

    public static void destroyInstance()
    {
        INSTANCE = null;
    }
}