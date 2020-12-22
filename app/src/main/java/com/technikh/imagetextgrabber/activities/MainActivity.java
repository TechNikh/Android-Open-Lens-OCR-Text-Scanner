package com.technikh.imagetextgrabber.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.technikh.imagetextgrabber.R;
import com.technikh.imagetextgrabber.fragments.PdfRendererBasicFragment;
import com.technikh.imagetextgrabber.models.ImageViewSettingsModel;
import com.technikh.imagetextgrabber.widgets.MultiSelectSpinnerWidget;
import com.technikh.imagetextgrabber.widgets.TouchImageView;

import androidx.appcompat.app.AppCompatActivity;
//import io.apptik.widget.multiselectspinner.MultiSelectSpinner;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity{

    int SELECT_PICTURE = 101;
    int SELECT_PDF = 102;
    TouchImageView ivImage;
    //ImageView ivStartCursor, ivEndCursor;
    EditText et_image_text;
    private SlidingUpPanelLayout mLayout;
    private String TAG = "MainActivity";
    private String PREF_SPINNER_USER_SETTINGS = "spinner_user_settings";
    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_basic";
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        MultiSelectSpinnerWidget mySpin = (MultiSelectSpinnerWidget)findViewById(R.id.spinner_options);
        ImageViewSettingsModel imageViewSettingsModel = new ImageViewSettingsModel();

        mySpin.setItems(imageViewSettingsModel.getAllItems());

        String savedString = sharedPref.getString(PREF_SPINNER_USER_SETTINGS, imageViewSettingsModel.getDefaultItemsString());
        String[] items = savedString.split(",");
        int[] savedList = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            savedList[i] = Integer.parseInt(items[i]);
        }
        mySpin.setSelection(savedList);
        mySpin.refreshSpinner();
        imageViewSettingsModel.setSelectedItems(mySpin.getSelectedIndicies());
        mySpin.setOnMultiChoiceClickListener(new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                //Log.d(TAG, "onClick: setOnMultiChoiceClickListener");
                List<Integer> list = mySpin.getSelectedIndicies();
                String delimitedString = TextUtils.join(",", list);
                //Log.d(TAG, "onClick: PREF_SPINNER_USER_SETTINGS "+delimitedString);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(PREF_SPINNER_USER_SETTINGS, delimitedString);
                editor.commit();

                imageViewSettingsModel.setSelectedItems(mySpin.getSelectedIndicies());
                ivImage.initOptions(imageViewSettingsModel);

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, delimitedString);
                mFirebaseAnalytics.logEvent("SPINNER_SETTINGS_CHANGE", bundle);
            }
        });

        /*ArrayList<String> options = new ArrayList<>();
        options.add("1");
        options.add("2");
        options.add("3");
        options.add("A");
        options.add("B");
        options.add("C");
        MultiSelectSpinner multiSelectSpinner = (MultiSelectSpinner) findViewById(R.id.spinner_options);
        ArrayAdapter<String> adapter = new ArrayAdapter <String>(this, android.R.layout.simple_list_item_multiple_choice, options);

        multiSelectSpinner
                .setListAdapter(adapter)
                .setSelectAll(true)
                .setMinSelectedItems(1);*/

        ivImage = findViewById(R.id.ivImage);
        /*ivStartCursor = findViewById(R.id.ivStartCursor);
        ivEndCursor = findViewById(R.id.ivEndCursor);*/
        et_image_text = findViewById(R.id.et_image_text);
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.setAnchorPoint(0.7f);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        boolean loadDefaultImage = true;

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    Glide.with(MainActivity.this)
                            .load(imageUri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ivImage);
                    loadDefaultImage = false;
                }
            }else if (type.startsWith("application/pdf")) {
                Uri pdfUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                Log.d(TAG, "onCreate: pdfUri "+pdfUri);
                ivImage.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, new PdfRendererBasicFragment(),
                                FRAGMENT_PDF_RENDERER_BASIC)
                        .commit();
            }
        }else if (Intent.ACTION_VIEW.equals(action) && type != null) {
            Log.d(TAG, "onCreate: type "+type);
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    Glide.with(MainActivity.this)
                            .load(imageUri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ivImage);
                    loadDefaultImage = false;
                }
            }else if (type.startsWith("application/pdf")) {
                Uri pdfUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                Log.d(TAG, "onCreate: pdfUri "+pdfUri);
                ivImage.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, new PdfRendererBasicFragment(),
                                FRAGMENT_PDF_RENDERER_BASIC)
                        .commit();
            }
        }
        if(loadDefaultImage) {
            //RequestOptions options = new RequestOptions();
            //options.fitCenter();
            //options.centerCrop();
            Glide.with(MainActivity.this)
                    .load(Uri.parse("file:///android_asset/Example.png"))
                    //.apply(options)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivImage);
        }
        ivImage.initOptions(imageViewSettingsModel);

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

        FloatingActionButton fabPdf = findViewById(R.id.fabPdf);
        fabPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickPdf();
            }
        });
    }

    private void handleIntent(){

    }

    public void pickPdf() {

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), SELECT_PDF);

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

                Bundle bundle = new Bundle();
                mFirebaseAnalytics.logEvent("IMAGE_CHANGE", bundle);
            }else if (requestCode == SELECT_PDF) {
                Log.d(TAG, "onActivityResult: data.getData() "+data.getData().toString());
                ivImage.setVisibility(View.GONE);

                PdfRendererBasicFragment f = new PdfRendererBasicFragment();
                // Supply index input as an argument.
                Bundle args = new Bundle();
                args.putString("uri", data.getData().toString());
                f.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, f,
                                FRAGMENT_PDF_RENDERER_BASIC)
                        .commit();
            }
        }
    }
/*
    @Override
    public void onItemsSelected(boolean[] items) {
        Log.d(TAG, "onItemsSelected: "+items.toString());
    }

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
