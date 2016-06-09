package com.example.brainbeats.basicbb.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.brainbeats.basicbb.MainActivity;

public class DeleteTagsActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Tagger(this).deleteAll();
        Toast.makeText(getApplicationContext(), "Deleted tags!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
