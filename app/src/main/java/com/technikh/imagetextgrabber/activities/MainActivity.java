/*
 * Copyright (c) 2019. Nikhil Dubbaka from TechNikh.com under GNU AFFERO GENERAL PUBLIC LICENSE
 * Copyright and license notices must be preserved.
 * When a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.
 */

package com.technikh.imagetextgrabber.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.technikh.imagetextgrabber.R;
import com.technikh.imagetextgrabber.fragments.PdfRendererBasicFragment;
import com.technikh.imagetextgrabber.models.ImageViewSettingsModel;
import com.technikh.imagetextgrabber.widgets.MultiSelectSpinnerWidget;
import com.technikh.imagetextgrabber.widgets.TouchImageView;

import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity{

    int SELECT_PICTURE = 101;
    int SELECT_PDF = 102;
    TouchImageView ivImage;
    RelativeLayout imageParentLayout;
    EditText et_image_text;
    private SlidingUpPanelLayout mLayout;
    private String TAG = "MainActivity";
    private String PREF_SPINNER_USER_SETTINGS = "spinner_user_settings";
    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_basic";
    private FirebaseAnalytics mFirebaseAnalytics;
    ImageViewSettingsModel imageViewSettingsModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        MultiSelectSpinnerWidget mySpin = (MultiSelectSpinnerWidget)findViewById(R.id.spinner_options);
        imageViewSettingsModel = new ImageViewSettingsModel();

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
                List<Integer> list = mySpin.getSelectedIndicies();
                String delimitedString = TextUtils.join(",", list);
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

        ivImage = findViewById(R.id.ivImage);
        imageParentLayout = findViewById(R.id.rlParentWrapper);
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
                PdfRendererBasicFragment f = new PdfRendererBasicFragment();
                Bundle args = new Bundle();
                args.putString("uri", pdfUri.toString());
                f.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, f,
                                FRAGMENT_PDF_RENDERER_BASIC)
                        .commit();
            }
        }else if (Intent.ACTION_VIEW.equals(action) && type != null) {
            Log.d(TAG, "onCreate: type "+type);
            Bundle bundle = intent.getExtras();
            Log.d(TAG, "onCreate: intent.getData() "+intent.getData());
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    Log.d(TAG, String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
                }
            }
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getData();
                if (imageUri != null) {
                    Glide.with(MainActivity.this)
                            .load(imageUri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ivImage);
                    loadDefaultImage = false;
                }
            }else if (type.startsWith("application/pdf")) {
                Uri pdfUri = (Uri) intent.getData();
                Log.d(TAG, "onCreate: pdfUri "+pdfUri);
                ivImage.setVisibility(View.GONE);
                PdfRendererBasicFragment f = new PdfRendererBasicFragment();
                Bundle args = new Bundle();
                args.putString("uri", pdfUri.toString());
                f.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, f,
                                FRAGMENT_PDF_RENDERER_BASIC)
                        .commit();
            }
        }
        if(loadDefaultImage) {
            Glide.with(MainActivity.this)
                    .load(Uri.parse("file:///android_asset/Example.png"))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivImage);
        }
        initImageView();

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    pickPdf();
                }else{
                    Bundle bundle = new Bundle();
                    mFirebaseAnalytics.logEvent("DEVICE_NO_SUPPORT_PDF", bundle);
                    Snackbar.make(view, "Your device version doesn't support our PDF opening library!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });
    }

    private void initImageView(){
        ivImage.initOptions(imageViewSettingsModel);

        ivImage.setCustomEventListener(new TouchImageView.OnCustomEventListener() {
            public void onEvent() {
                et_image_text.setText(ivImage.getContentDescription());
                et_image_text.setSelectAllOnFocus(true);
            }
        });
        et_image_text.setText("");
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
                ViewGroup.LayoutParams lParams = ivImage.getLayoutParams();
                imageParentLayout.removeView(ivImage);
                ivImage = new TouchImageView(this);
                ivImage.setLayoutParams(lParams);
                imageParentLayout.addView(ivImage);

                //ivImage.setImageMatrix(new Matrix());
                ivImage.setVisibility(View.VISIBLE);
                findViewById(R.id.container).setVisibility(View.GONE);
                //ImageViewUtils.updateImageViewMatrix(ivImage, ((BitmapDrawable) ivImage.getDrawable()).getBitmap());
                //ivImage.resetOCR();
                Glide.with(MainActivity.this)
                        .load(data.getData())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivImage);

                initImageView();

                Bundle bundle = new Bundle();
                mFirebaseAnalytics.logEvent("IMAGE_CHANGE", bundle);
            }else if (requestCode == SELECT_PDF) {
                Bundle bundle = new Bundle();
                mFirebaseAnalytics.logEvent("PDF_CHANGE", bundle);
                ivImage.setVisibility(View.GONE);
                findViewById(R.id.container).setVisibility(View.VISIBLE);

                PdfRendererBasicFragment f = new PdfRendererBasicFragment();
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
}
