package com.example.tdeframond.basicbb.data;

import android.app.Activity;
import android.os.Bundle;

public class DeleteTagsActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Tagger(this).deleteAll();
        finish();
    }
}
