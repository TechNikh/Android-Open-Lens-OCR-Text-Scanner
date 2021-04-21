/*
 * Copyright (c) 2019. Nikhil Dubbaka from TechNikh.com under GNU AFFERO GENERAL PUBLIC LICENSE
 * Copyright and license notices must be preserved.
 * When a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.
 */

package com.technikh.imagetextgrabber.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.github.dhaval2404.colorpicker.model.ColorSwatch;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.technikh.imagetextgrabber.R;
import com.technikh.imagetextgrabber.models.ImageViewSettingsModel;
import com.technikh.imagetextgrabber.models.VisionWordModel;
import com.technikh.imagetextgrabber.room.MyDatabase;
import com.technikh.imagetextgrabber.room.dao.HighlightDataAccess;
import com.technikh.imagetextgrabber.room.dao.ImagesDataAccess;
import com.technikh.imagetextgrabber.room.entity.Highlights;
import com.technikh.imagetextgrabber.room.entity.Images;
import com.technikh.imagetextgrabber.widgets.MultiSelectSpinnerWidget;
import com.technikh.imagetextgrabber.widgets.TouchImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    private MaterialColorPickerDialog colorPickerDialog;

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
    public static MyDatabase db;

    public static final String DBNAME="mydb";
    private HighlightDataAccess markerDao;
    private ImagesDataAccess imagesDao;
    public static String currentUri="default";
    private List<MyVisionWordModel> savedRects=new ArrayList<>();
    private ArrayList<String> colorArray;
    private GridView gridView;
    private  AlertDialog alertDialog;


    class MyVisionWordModel extends VisionWordModel{
        String color;

        public MyVisionWordModel(Rect rect, String text, String color) {
            super(rect, text);
            this.color=color;
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView=getLayoutInflater().inflate(R.layout.grid,null,false).findViewById(R.id.grid);
        alertDialog=new AlertDialog.Builder(MainActivity.this)
                .setView(gridView)
                .create();




        colorArray = new ArrayList<>();
        colorArray.add("#f6e58d");

        db=Room.databaseBuilder(getApplicationContext(),
                MyDatabase.class, DBNAME).build();


        markerDao=db.getHighlightsDao();
        imagesDao=db.getImagesDao();







        colorPickerDialog=new MaterialColorPickerDialog
                .Builder(this)

                // Option 1: Pass Hex Color Codes
                .setColors(colorArray)
                .setColorSwatch(ColorSwatch.A300)
                .setPositiveButton("OK")
                .setNegativeButton("CANCEL")

                // Option 2: Pass Hex Color Codes from string.xml
                //.setColors(getResources().getStringArray(R.array.themeColorHex))

                // Option 3: Pass color array from colors.xml
                //.setColorRes(getResources().getIntArray(R.array.themeColors))



                .setColorListener(new ColorListener() {
                    @Override
                    public void onColorSelected(int i, String s) {
                            ivImage.highlight(s);
                            ivImage.invalidate();

                        //Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();

                    }


                }).build();

        findViewById(R.id.highlight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @Override
                    public void run() {
                        loadColors();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //colorPickerDialog.show();
                                gridView.setAdapter(new MyGridView(MainActivity.this));
                                alertDialog.show();
                            }
                        });

                    }
                }.start();

            }
        });

        findViewById(R.id.add_highlight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*new AlertDialog.Builder(MainActivity.this)
                        .setTitle("")
                        .setMessage("")
                        .setNegativeButton("ADD", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })


                        .setPositiveButton("REMOVE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new MaterialColorPickerDialog
                                        .Builder(MainActivity.this)

                                        // Option 1: Pass Hex Color Codes
                                        .setColors(colorArray)
                                        .setColorSwatch(ColorSwatch.A300)
                                        .setPositiveButton("REMOVE")
                                        .setNegativeButton("")
                                        .setTitle("Remove Marker")

                                        // Option 2: Pass Hex Color Codes from string.xml
                                        //.setColors(getResources().getStringArray(R.array.themeColorHex))

                                        // Option 3: Pass color array from colors.xml
                                        //.setColorRes(getResources().getIntArray(R.array.themeColors))



                                        .setColorListener(new ColorListener() {
                                            @Override
                                            public void onColorSelected(int i, String s) {
                                                Highlights h=new Highlights();
                                                h.color=s;
                                                new Thread() {
                                                    @Override
                                                    public void run() {
                                                        markerDao.remove(h);
                                                    }
                                                }.start();
                                               // colorArray.remove(s);

                                            }


                                        }).build().show();
                            }
                        })
                        .create()
                        .show();*/

                new ColorPickerDialog
                        .Builder(MainActivity.this)

                        // Option 1: Pass Hex Color Codes

                        // Option 2: Pass Hex Color Codes from string.xml
                        //.setColors(getResources().getStringArray(R.array.themeColorHex))

                        // Option 3: Pass color array from colors.xml
                        //.setColorRes(getResources().getIntArray(R.array.themeColors))



                        .setColorListener(new ColorListener() {
                            @Override
                            public void onColorSelected(int i, String s) {

                                //colorArray.add(s);
                                Highlights h=new Highlights();
                                h.color=s;
                                new Thread(){
                                    @Override
                                    public void run() {
                                        markerDao.add(h);

                                    }
                                }.start();

                                //Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();

                            }


                        }).build().show();
            }
        });


        findViewById(R.id.delete_highlight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(){
                    @Override
                    public void run() {
                        loadColors();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new MaterialColorPickerDialog
                                        .Builder(MainActivity.this)

                                        // Option 1: Pass Hex Color Codes
                                        .setColors(colorArray)
                                        .setColorSwatch(ColorSwatch.A300)
                                        .setPositiveButton("REMOVE")
                                        .setNegativeButton("")
                                        .setTitle("Remove Marker")

                                        // Option 2: Pass Hex Color Codes from string.xml
                                        //.setColors(getResources().getStringArray(R.array.themeColorHex))

                                        // Option 3: Pass color array from colors.xml
                                        //.setColorRes(getResources().getIntArray(R.array.themeColors))



                                        .setColorListener(new ColorListener() {
                                            @Override
                                            public void onColorSelected(int i, String s) {
                                                Highlights h=new Highlights();
                                                h.color=s;
                                                new Thread() {
                                                    @Override
                                                    public void run() {
                                                        markerDao.remove(h);
                                                    }
                                                }.start();
                                                // colorArray.remove(s);

                                            }


                                        }).build().show();

                            }
                        });

                    }
                }.start();

            }
        });



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

                Bundle args = new Bundle();
                args.putString("uri", pdfUri.toString());
                startActivity(new Intent(MainActivity.this,PdfRendererBasicFragment.class)
                        .putExtra("bundle",args)
                );
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

                Bundle args = new Bundle();
                args.putString("uri", pdfUri.toString());
                startActivity(new Intent(MainActivity.this,PdfRendererBasicFragment.class)
                        .putExtra("bundle",args)
                );


            }
        }
        if(loadDefaultImage) {
            Glide.with(MainActivity.this)
                    .load(Uri.parse("file:///android_asset/Example.png"))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {

                            showSavedHighlights(resource);
                            return false;
                        }
                    })
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

    private void loadColors() {
        colorArray.clear();
        for(Highlights marker:markerDao.getMarkers()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    colorArray.add(marker.color);
                }
            });

        }
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
            currentUri=data.getData().toString();
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
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {

                                showSavedHighlights(resource);
                                return false;
                            }
                        })

                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivImage);

                initImageView();

                Bundle bundle = new Bundle();
                mFirebaseAnalytics.logEvent("IMAGE_CHANGE", bundle);
            }
            else if (requestCode == SELECT_PDF) {
                Bundle bundle = new Bundle();
                mFirebaseAnalytics.logEvent("PDF_CHANGE", bundle);
                ivImage.setVisibility(View.GONE);
                findViewById(R.id.container).setVisibility(View.VISIBLE);


                Bundle args = new Bundle();
                args.putString("uri", data.getData().toString());
                startActivity(new Intent(MainActivity.this,PdfRendererBasicFragment.class)
                        .putExtra("bundle",args)
                );
            }
        }
    }



    public void showSavedHighlights(Drawable drawable){
        //get last read image
        //...


        //show highlights on image
        new Thread(){
            @Override
            public void run() {
                savedRects.clear();
                for(Images imageInfo:imagesDao.getAllImage(currentUri)){
                    Rect rect=new Rect(imageInfo.left,imageInfo.top,imageInfo.right,imageInfo.bottom);
                    MyVisionWordModel visionWordModel=new MyVisionWordModel(rect,imageInfo.text,imageInfo.color);
                    savedRects.add(visionWordModel);

                }

                //draw on image with color


                //canvas.drawBitmap(originalBitmap, 0, 0, paint);
                //canvas.drawText("Testing...", 10, 10, paint);

                final Bitmap originalBitmap;
        /*if(longPressMode){
            originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);
        }else {*/
                originalBitmap = ((BitmapDrawable) drawable).getBitmap();
                //}
                Canvas canvas = new Canvas(originalBitmap);

                for(int i=0;i<savedRects.size();++i) {
                    MyVisionWordModel visionWordModel=savedRects.get(i);
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.FILL);
                    paint.setStrokeWidth(2);
                    paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                    paint.setColor(Color.parseColor(visionWordModel.color));
                    paint.setAntiAlias(true);
                    int finalI = i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            canvas.drawRect(visionWordModel.mrect, paint);
                            if(finalI ==savedRects.size()){
                                ivImage.invalidate();
                            }
                        }
                    });


                }


            }
        }.start();
    }



    class MyGridView extends ArrayAdapter {


        public MyGridView(Context c){
            super(c, android.R.layout.simple_list_item_1,colorArray);
        }


        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            convertView=getLayoutInflater().inflate(R.layout.marker,null,false);
            TextView tv=convertView.findViewById(R.id.tv);
            tv.setBackgroundColor(Color.parseColor(colorArray.get(position)));
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ivImage.highlight(colorArray.get(position));
                }
            });
            return convertView;
        }
    }
}



