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
import android.widget.TextView;

import java.util.ArrayList;

public class StreamListingAdapter extends ArrayAdapter<Stream>
{
    private Context _context;
    private ArrayList<Stream> _list;

    private static class StreamListingViewHolder
    {
        private View box;
        private ConstraintLayout layout;
        private TextView nameTextView;
        private TextView descTextView;
    }

    public StreamListingAdapter(Context context, ArrayList<Stream> list)
    {
        super(context, 0, list);

        _context = context;
        _list = list;
    }

    @NonNull @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        StreamListingViewHolder holder;
        View listItem = convertView;

        if (listItem == null)
        {
            listItem = LayoutInflater.from(_context).inflate(R.layout.list_streams, parent, false);

            holder = new StreamListingViewHolder();
            holder.box = listItem.findViewById(R.id.list_streams_box);
            holder.layout = listItem.findViewById(R.id.list_streams_layout);
            holder.nameTextView = listItem.findViewById(R.id.list_streams_name);
            holder.descTextView = listItem.findViewById(R.id.list_streams_desc);

            listItem.setTag(holder);
        }
        else
        {
            holder = (StreamListingViewHolder) listItem.getTag();
        }

        Stream currentStream = _list.get(position);

        int bgcColor = position % 2 == 0 ? R.color.backgroundWhite : R.color.backgroundGray;
        int boxColor = currentStream.online ? R.color.backgroundGreen : R.color.backgroundRed;

        // Koska viewholder vie layoutin tagin, purkkaa ja data tekstin tagiin..
        holder.box.setBackgroundColor(ContextCompat.getColor(_context, boxColor));
        holder.layout.setBackgroundColor(ContextCompat.getColor(_context, bgcColor));
        holder.nameTextView.setTag(currentStream);
        holder.nameTextView.setText(currentStream.name);
        holder.descTextView.setText(currentStream.desc);

        return listItem;
    }
}
