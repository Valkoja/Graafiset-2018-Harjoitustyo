package com.valkoja.streamwatch;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class SettingsAdapter extends ArrayAdapter<Stream>
{
    private Context _context;
    private ArrayList<Stream> _list;

    private static class SettingsViewHolder
    {
        private ConstraintLayout layout;
        private TextView idTextView;
        private TextView nameTextView;
        private Button removeButton;
    }

    public SettingsAdapter(Context context, ArrayList<Stream> list)
    {
        super(context, 0, list);

        _context = context;
        _list = list;
    }

    @NonNull @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        SettingsViewHolder holder;
        View listItem = convertView;

        if (listItem == null)
        {
            listItem = LayoutInflater.from(_context).inflate(R.layout.list_settings, parent, false);

            holder = new SettingsViewHolder();
            holder.layout = listItem.findViewById(R.id.list_settings_layout);
            holder.idTextView = listItem.findViewById(R.id.list_settings_id);
            holder.nameTextView = listItem.findViewById(R.id.list_settings_name);
            holder.removeButton = listItem.findViewById(R.id.list_settings_button);

            listItem.setTag(holder);
        }
        else
        {
            holder = (SettingsViewHolder) listItem.getTag();
        }

        Stream currentStream = _list.get(position);

        int bgcColor = position % 2 == 0 ? R.color.backgroundGray : R.color.backgroundWhite;

        holder.layout.setBackgroundColor(ContextCompat.getColor(_context, bgcColor));
        holder.idTextView.setText(currentStream.id);
        holder.nameTextView.setText(currentStream.name);
        holder.removeButton.setTag(currentStream);

        return listItem;
    }
}