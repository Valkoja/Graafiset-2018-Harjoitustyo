package com.valkoja.streamwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class Settings extends AppCompatActivity
{
    private EditText _inputID;
    private EditText _inputName;
    private ListView _listView;
    private ArrayList<Stream> _streamList = new ArrayList<>();

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver()
    {
        @Override public void onReceive(Context context, Intent intent)
        {
            switch ((Broadcast) intent.getSerializableExtra("act"))
            {
                case SQLINSERTED:
                    shortToast(R.string.msgStreamAdded);
                    refreshList();
                    break;

                case SQLDELETED:
                    shortToast(R.string.msgStreamRemoved);
                    refreshList();
                    break;

                case LISTUPDATED:
                    _listView.invalidateViews();
                    break;
            }
        }
    };

    // Manifest valitti puuttuvasta rakentajasta
    public Settings() {}

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Custom actionbar, ei tarpeellinen tässä mutta teema poisti globaalisti defaultit
        Toolbar settingsToolbar = findViewById(R.id.actionbar_settings);
        setSupportActionBar(settingsToolbar);
        getSupportActionBar().setTitle(R.string.actionbar_settings);

        // Elementit streamien lisäämiseksi
        _inputID = findViewById(R.id.activity_settings_id);
        _inputName = findViewById(R.id.activity_settings_name);
        _listView = findViewById(R.id.activity_settings_list);

        // Listalle oma adapter
        _listView.setAdapter(new SettingsAdapter(this, _streamList));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Vastaanotto broadcastille
        LocalBroadcastManager.getInstance(this).registerReceiver(_broadcastReceiver, new IntentFilter("com.valkoja.streamwatch.BROADCAST"));

        // Päivitetään lista
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
        // Jatkoa actionbar:in muuttamiselle
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_settings, menu);

        return super.onCreateOptionsMenu(menu);
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
                Stream[] streams = dbHandle.streamDao().selectForSettings();

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

    public void addStream(View view)
    {
        // Haetaan ja tarkistetaan uusi ID
        final String addID = _inputID.getText().toString();

        if (addID.length() < 1)
        {
            Toast.makeText(getApplicationContext(), R.string.msgAddIDEmpty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (addID.contains(" "))
        {
            Toast.makeText(getApplicationContext(), R.string.msgAddIDSpaces, Toast.LENGTH_SHORT).show();
            return;
        }

        // Haetaan ja tarkistetaan uusi nimi
        final String addName = _inputName.getText().toString();

        if (addName.length() < 1)
        {
            Toast.makeText(getApplicationContext(), R.string.msgAddNameError, Toast.LENGTH_SHORT).show();
            return;
        }

        // Data ok -> nollataan kentät
        _inputID.setText("");
        _inputName.setText("");

        // Sulje näppäimistö
        View currentFocus = this.getCurrentFocus();

        if (currentFocus != null)
        {
            InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }

        // Poista valinta tekstikentästä
        this.findViewById(R.id.layout_settings).requestFocus();

        // Tietokannan kahva
        final StreamDatabase dbHandle = StreamDatabase.getDatabase(this);

        // Query ajetaan async
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
                // Stream kantaan, paluuarvoja tarjolla kovin huonosti
                dbHandle.streamDao().insertStream(new Stream(addID, addName));

                // Toivotaan että tieto meni kantaan, ilmoitetaan siitä
                Intent intent = new Intent();
                intent.setAction("com.valkoja.streamwatch.BROADCAST");
                intent.putExtra("act", Broadcast.SQLINSERTED);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        });
    }

    public void confirmDelete(final View view)
    {
        // Rakennetaan popup kysymään oletko varma
        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setCancelable(true);
        builder.setTitle(R.string.confirmTitle);
        builder.setMessage(R.string.confirmMessage);

        // Kyllä -napin toiminta
        builder.setPositiveButton(R.string.confirmYes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                removeStream(view);
            }
        });

        // Ei -napin toiminta
        builder.setNegativeButton(R.string.confirmNo, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        // Luodaan itse ikkuna
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void removeStream(View view)
    {
        // Tietokannan kahva, poistettava olio
        final StreamDatabase dbHandle = StreamDatabase.getDatabase(this);
        final Stream stream = (Stream) view.getTag();

        // Query ajetaan async
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
                // Stream pois kannasta, paluuarvoja tarjolla kovin huonosti
                dbHandle.streamDao().deleteStreams(stream);

                // Toivotaan että tieto poistettiin kannasta, ilmoitetaan siitä
                Intent intent = new Intent();
                intent.setAction("com.valkoja.streamwatch.BROADCAST");
                intent.putExtra("act", Broadcast.SQLDELETED);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        });
    }

    private void shortToast(int resource)
    {
        // Luodaan toast
        final Toast toast = Toast.makeText(getApplicationContext(), resource, Toast.LENGTH_SHORT);
        toast.show();

        // Koska Toast.LENGTH_SHORT on liian pitkä pikaiseen ilmoitukseen, peruutetaan se sekunnin kuluttua
        Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                toast.cancel();
            }
        }, 1000);
    }
}