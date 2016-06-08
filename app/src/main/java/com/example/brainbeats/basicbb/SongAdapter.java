package com.example.brainbeats.basicbb;

import android.support.v4.media.MediaMetadataCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.brainbeats.basicbb.data.Tagger;

public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private LayoutInflater songInf;
    private RadioButton exited;
    private RadioButton calm;
    private RadioGroup radioGroup;
    private Tagger tagger;

    public SongAdapter(Context c, ArrayList<Song> theSongs){
        songs=theSongs;
        songInf=LayoutInflater.from(c);
        tagger = new Tagger(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        LinearLayout songLay = (LinearLayout)songInf.inflate(R.layout.song, parent, false);
        //get title and artist views
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        //get song using position
        final Song currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());


        exited = (RadioButton) songLay.findViewById(R.id.exitedButton);
        exited.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String tag = exited.getText().toString();
                currSong.setState(tag);
                tagger.addTagToSong(currSong.getID(), tag);
            }
        });
        calm = (RadioButton)songLay.findViewById(R.id.calmButton);
        calm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String tag = calm.getText().toString();
                currSong.setState(tag);
                tagger.addTagToSong(currSong.getID(), tag);
            }
        });
        radioGroup = (RadioGroup) songLay.findViewById(R.id.tag);
        if (currSong.getState().equals(exited.getText())) {
            radioGroup.check(exited.getId());
        } else if (currSong.getState().equals(calm.getText())) {
            radioGroup.check(calm.getId());
        }

        //set position as tag
        songLay.setTag(position);
        return songLay;
    }
}
