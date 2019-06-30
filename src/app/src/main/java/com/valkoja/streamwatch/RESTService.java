package com.valkoja.streamwatch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.JsonReader;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RESTService extends Service
{
    private long _timerInterval = 600000;
    private Handler _timerHandler = new Handler();

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver()
    {
        @Override public void onReceive(Context context, Intent intent)
        {
            switch ((Broadcast) intent.getSerializableExtra("act"))
            {
                case RESTSTART:
                    stopTimer();
                    fetchStreams();
                    break;

                case RESTFINISH:
                    startTimer();
                    break;
            }
        }
    };

    // Manifest valittaa puuttuvasta rakentajasta
    public RESTService() {}

    // Speksin mukaan onBind pitää olla
    @Override
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Vastaanotto broadcastille
        LocalBroadcastManager.getInstance(this).registerReceiver(_broadcastReceiver, new IntentFilter("com.valkoja.streamwatch.BROADCAST"));

        // Käynnistetään ajastin
        startTimer();
    }

    @Override
    public void onDestroy()
    {
        // Ajastin seis, broadcast irti
        stopTimer();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(_broadcastReceiver);

        super.onDestroy();
    }

    private void startTimer()
    {
        // Ajastimen toiminta, kutsutaan fetchStreamsia _timerintervalin välein
        _timerHandler.postDelayed(new Runnable()
        {
            @Override public void run()
            {
                fetchStreams();
            }
        }, _timerInterval);
    }

    private void stopTimer()
    {
        // Ajastimen pysäytys
        _timerHandler.removeCallbacksAndMessages(null);
    }

    private void fetchStreams()
    {
        // Tietokannan kahva
        final StreamDatabase dbHandle = StreamDatabase.getDatabase(this);

        // Kaikki verkko ja tietokantatoimninta toiseen threadiin
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
                JsonReader jsonReader = null;
                HttpURLConnection connectionHandle = null;

                try
                {
                    if (!isNetworkAvailable())
                    {
                        return;
                    }

                    // Haetaan streamit kannasta
                    Stream[] streams = dbHandle.streamDao().selectForSettings();

                    if (streams.length < 1)
                    {
                        return;
                    }

                    // Kerätään stream-id:t yhteen
                    String prefix = "";
                    StringBuilder buildUrl = new StringBuilder();

                    for (int i = 0; i < streams.length; i++)
                    {
                        buildUrl.append(prefix);
                        prefix = ",";
                        buildUrl.append(streams[i].id);

                        // nollataan olio myöhempää JSON parsea varten
                        streams[i].url = "";
                        streams[i].online = false;
                        streams[i].desc = "Offline";
                    }

                    // URL, headerit, yhteys
                    URL urlHandle = new URL("https://api.twitch.tv/kraken/streams?channel=" + buildUrl.toString());
                    connectionHandle = (HttpURLConnection) urlHandle.openConnection();

                    connectionHandle.setRequestMethod("GET");
                    connectionHandle.setRequestProperty("Client-ID", "up759gmy3wjdj3zbduhb97k2fcicrz");

                    // Palvelimen vastaus
                    if (connectionHandle.getResponseCode() != 200)
                    {
                        return;
                    }

                    // JSON käyntiin. Pakko quotea tuota sivustoa ja katsoa alla olevaa pyramidia..
                    // "The Android SDK has a class called JsonReader, which makes it very easy for you to parse JSON documents."
                    InputStream inStream = connectionHandle.getInputStream();
                    jsonReader = new JsonReader(new InputStreamReader(inStream, "UTF-8"));
                    jsonReader.beginObject();

                    String name;

                    while (jsonReader.hasNext())
                    {
                        name = jsonReader.nextName();

                        if (name.equals("_total"))
                        {
                            if (jsonReader.nextInt() == 0)
                            {
                                // Ei streameja, voidaan samantien lopettaa
                                break;
                            }
                        }
                        else if (name.equals("streams"))
                        {
                            jsonReader.beginArray();

                            while (jsonReader.hasNext())
                            {
                                jsonReader.beginObject();

                                String streamID = null;
                                String streamUrl = null;
                                String streamDesc = null;
                                boolean streamLive = false;

                                while (jsonReader.hasNext())
                                {
                                    name = jsonReader.nextName();

                                    if (name.equals("stream_type"))
                                    {
                                        if (jsonReader.nextString().equals("live"))
                                        {
                                            streamLive = true;
                                        }
                                    }
                                    else if (name.equals("channel"))
                                    {
                                        jsonReader.beginObject();

                                        while (jsonReader.hasNext())
                                        {
                                            name = jsonReader.nextName();

                                            if (name.equals("status"))
                                            {
                                                streamDesc = jsonReader.nextString();
                                            }
                                            else if (name.equals("display_name"))
                                            {
                                                streamID = jsonReader.nextString();
                                            }
                                            else if (name.equals("url"))
                                            {
                                                streamUrl = jsonReader.nextString();
                                            }
                                            else
                                            {
                                                jsonReader.skipValue();
                                            }
                                        }

                                        jsonReader.endObject();
                                    }
                                    else
                                    {
                                        jsonReader.skipValue();
                                    }
                                }

                                // Sijoitetaan saadut tiedot olioon
                                for (int i = 0; i < streams.length; i++)
                                {
                                    if (streams[i].id.equals(streamID))
                                    {
                                        streams[i].url = streamUrl;
                                        streams[i].desc = streamDesc;
                                        streams[i].online = streamLive;

                                        Log.v("Debug", "Stream " + i + " updated");
                                    }
                                }

                                jsonReader.endObject();
                            }

                            jsonReader.endArray();
                        }
                        else
                        {
                            jsonReader.skipValue();
                        }
                    }

                    jsonReader.endObject();

                    // Viedään päivitetyt luokat kantaan
                    dbHandle.streamDao().updateStreams(streams);
                    //dbHandle.streamDao().insertStream(streams);
                }
                catch (Exception ex)
                {
                    // Log.v("Debug", ex.toString());
                    return;
                }
                finally
                {
                    try
                    {
                        if (jsonReader != null)
                        {
                            jsonReader.close();
                        }
                    }
                    catch (Exception ex)
                    {
                        // Johan on systeemi..
                    }

                    if (connectionHandle != null)
                    {
                        connectionHandle.disconnect();
                    }
                }

                // Ilmoitetaan että lista on päivitetty
                Intent intent = new Intent();
                intent.setAction("com.valkoja.streamwatch.BROADCAST");
                intent.putExtra("act", Broadcast.RESTFINISH);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        });
    }

    private boolean isNetworkAvailable()
    {
        ConnectivityManager conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected())
        {
            return true;
        }

        return false;
    }
}
