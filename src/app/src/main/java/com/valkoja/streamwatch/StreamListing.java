package com.valkoja.streamwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreamListing extends AppCompatActivity
{
    private ListView _listView;
    private ArrayList<Stream> _streamList = new ArrayList<>();

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver()
    {
        @Override public void onReceive(Context context, Intent intent)
        {
            switch ((Broadcast) intent.getSerializableExtra("act"))
            {
                case RESTFINISH:
                case SQLUPDATED:
                    refreshList();
                    break;

                case LISTUPDATED:
                    Log.v("Debug", "LISTUPDATED");
                    _listView.invalidateViews();
                    break;
            }
        }
    };

    // Manifest valitti puuttuvasta rakentajasta
    public StreamListing() {}

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_listing);

        // Custom actionbar käyttöön
        Toolbar myToolbar = findViewById(R.id.actionbar_stream_listing);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(R.string.actionbar_stream_listing);

        // Listalle oma adapter
        _listView = findViewById(R.id.activity_stream_listing_list);
        _listView.setAdapter(new StreamListingAdapter(this, _streamList));

        // Palvelu käyntiin
        this.startService(new Intent(this, RESTService.class));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Vastaanotto broadcastille
        LocalBroadcastManager.getInstance(this).registerReceiver(_broadcastReceiver, new IntentFilter("com.valkoja.streamwatch.BROADCAST"));

        // Pyydetään suorittamaan query
        Intent intent = new Intent();
        intent.setAction("com.valkoja.streamwatch.BROADCAST");
        intent.putExtra("act", Broadcast.RESTSTART);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Siltä varalta että query ei onnistu tai on hidas, näytetään viimeisin tilanne
        refreshList();
    }

    @Override
    protected void onPause()
    {
        // Broadcast irti, activityn ei tarvitse reagoida taustalta
        LocalBroadcastManager.getInstance(this).unregisterReceiver(_broadcastReceiver);

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Custom actionbar sisältö
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_stream_listing, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Actionbar nappien toiminta
        switch (item.getItemId())
        {
            case R.id.button_refresh:
                Intent intent = new Intent();
                intent.setAction("com.valkoja.streamwatch.BROADCAST");
                intent.putExtra("act", Broadcast.RESTSTART);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                return true;

            case R.id.button_settings:
                startActivity(new Intent(this, Settings.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshList()
    {
        // Tietokannan kahva
        final StreamDatabase dbHandle = StreamDatabase.getDatabase(this);

        // Queryt ajetaan async
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
                Stream[] streams = dbHandle.streamDao().selectForListing();

                // Kannasta tuleva array -> ArrayList
                _streamList.clear();
                Collections.addAll(_streamList, streams);

                // Ilmoitetaan että lista on päivitetty
                Intent intent = new Intent();
                intent.setAction("com.valkoja.streamwatch.BROADCAST");
                intent.putExtra("act", Broadcast.LISTUPDATED);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        });
    }

    public void launchExternal(View view)
    {
        // Rakennetaan intent, koska layoutin tag meni viewholderille, purkkaa ja data tekstin elementtiin
        Stream stream = (Stream) view.findViewById(R.id.list_streams_name).getTag();

        // Vältetään nullpointer jos url ei ole asetettu
        if (TextUtils.isEmpty(stream.url))
        {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(stream.url));

        // Onnistuuko sen ajaminen
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);

        boolean isIntentSafe = activities.size() > 0;

        // Jos onnistuu, ajetaan
        if (isIntentSafe)
        {
            startActivity(intent);
        }
        else
        {
            Toast.makeText(getApplicationContext(), R.string.msgLaunchFailed, Toast.LENGTH_SHORT).show();
        }
    }
}