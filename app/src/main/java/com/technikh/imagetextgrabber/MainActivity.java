package com.technikh.imagetextgrabber;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    int SELECT_PICTURE = 101;
    TouchImageView ivImage;
    ImageView ivStartCursor, ivEndCursor;
    EditText et_image_text;
    private SlidingUpPanelLayout mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

        ivImage = findViewById(R.id.ivImage);
        ivStartCursor = findViewById(R.id.ivStartCursor);
        ivEndCursor = findViewById(R.id.ivEndCursor);
        et_image_text = findViewById(R.id.et_image_text);
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.setAnchorPoint(0.7f);

        //RequestOptions options = new RequestOptions();
        //options.fitCenter();
        //options.centerCrop();
        Glide.with(MainActivity.this)
                .load(Uri.parse("file:///android_asset/Example.png"))
                //.apply(options)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivImage);
        //ivImage.initCursors(ivStartCursor, ivEndCursor);

        ivImage.setCustomEventListener(new TouchImageView.OnCustomEventListener() {
            public void onEvent() {
                et_image_text.setText(ivImage.getContentDescription());
                et_image_text.setSelectAllOnFocus(true);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage();
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });
    }

    public void pickImage() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Glide.with(MainActivity.this)
                        .load(data.getData())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivImage);
                ivImage.resetOCR();
            }

        }
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */
}
