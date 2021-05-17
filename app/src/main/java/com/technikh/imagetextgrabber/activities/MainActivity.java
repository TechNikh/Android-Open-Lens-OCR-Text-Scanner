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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Build;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.technikh.imagetextgrabber.R;
import com.technikh.imagetextgrabber.models.ImageViewSettingsModel;
import com.technikh.imagetextgrabber.models.VisionWordModel;
import com.technikh.imagetextgrabber.room.entity.Highlights;
import com.technikh.imagetextgrabber.room.entity.Images;
import com.technikh.imagetextgrabber.widgets.MultiSelectSpinnerWidget;
import com.technikh.imagetextgrabber.widgets.TouchImageView;

import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

    int SELECT_PICTURE = 101;
    int SELECT_PDF = 102;
    TouchImageView ivImage;
    TextView saveNoteTV;
    RelativeLayout imageParentLayout;
    public EditText et_image_text;
    private SlidingUpPanelLayout mLayout;
    private String TAG = "MainActivity";
    private String PREF_SPINNER_USER_SETTINGS = "spinner_user_settings";
    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_basic";
    private FirebaseAnalytics mFirebaseAnalytics;
    ImageViewSettingsModel imageViewSettingsModel;
    public static com.technikh.imagetextgrabber.room.MyDatabase db;

    public static final String DBNAME="mydb";
    private com.technikh.imagetextgrabber.room.dao.HighlightDataAccess markerDao;
    private com.technikh.imagetextgrabber.room.dao.ImagesDataAccess imagesDao;
    public static String currentUri="default";
    public static java.util.List<MyVisionWordModel> savedRects=new ArrayList<>();
    private ArrayList<String> colorArray;
    private GridView gridView;
    private  AlertDialog alertDialog;
    private Integer recentHighlight=null;


    public class MyVisionWordModel extends VisionWordModel{
        public String color;
        public String note;

        public MyVisionWordModel(Rect rect, String text, String color,String note) {
            super(rect, text);
            this.color=color;
            this.note=note;
        }


    }



    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        //Toast.makeText(getApplicationContext(),Wiki.getTextExtract("Stack Overflow"),Toast.LENGTH_LONG).show();

        try {


            saveNoteTV = findViewById(R.id.save_note);
            saveNoteTV.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {


                    EditText et = findViewById(R.id.et);
                    String notes = et.getText().toString().trim();
                    if (notes.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Please enter note", Toast.LENGTH_LONG).show();
                    } else {
                        ivImage.saveNote(notes);
                        //ToDo:Addtoastinsavenotefunction
                        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_LONG).show();

                    }
                }
            });

            gridView = getLayoutInflater().inflate(R.layout.grid, null, false).findViewById(R.id.grid);
            alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setView(gridView)
                    .create();


            colorArray = new ArrayList<>();
            colorArray.add("#f6e58d");

            db = androidx.room.Room.databaseBuilder(getApplicationContext(),
                    com.technikh.imagetextgrabber.room.MyDatabase.class, DBNAME).build();


            markerDao = db.getHighlightsDao();
            imagesDao = db.getImagesDao();











        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

            MultiSelectSpinnerWidget mySpin = (MultiSelectSpinnerWidget) findViewById(R.id.spinner_options);
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
                    java.util.List<Integer> list = mySpin.getSelectedIndicies();
                    String delimitedString = TextUtils.join(",", list);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREF_SPINNER_USER_SETTINGS, delimitedString);
                    editor.commit();

                    imageViewSettingsModel.setSelectedItems(mySpin.getSelectedIndicies());
                    ivImage.initOptions(imageViewSettingsModel);

                    android.os.Bundle bundle = new android.os.Bundle();
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
                        com.bumptech.glide.Glide.with(MainActivity.this)
                                .load(imageUri)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(ivImage);
                        loadDefaultImage = false;
                    }
                } else if (type.startsWith("application/pdf")) {
                    Uri pdfUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    android.util.Log.d(TAG, "onCreate: pdfUri " + pdfUri);
                    ivImage.setVisibility(android.view.View.GONE);

                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("uri", pdfUri.toString());
                    startActivity(new Intent(MainActivity.this, PdfRendererBasicFragment.class)
                            .putExtra("bundle", args)
                    );
                }
            } else if (Intent.ACTION_VIEW.equals(action) && type != null) {
                android.util.Log.d(TAG, "onCreate: type " + type);
                android.os.Bundle bundle = intent.getExtras();
                android.util.Log.d(TAG, "onCreate: intent.getData() " + intent.getData());
                if (bundle != null) {
                    for (String key : bundle.keySet()) {
                        Object value = bundle.get(key);
                        android.util.Log.d(TAG, String.format("%s %s (%s)", key,
                                value.toString(), value.getClass().getName()));
                    }
                }
                if (type.startsWith("image/")) {
                    Uri imageUri = (Uri) intent.getData();
                    if (imageUri != null) {
                        com.bumptech.glide.Glide.with(MainActivity.this)
                                .load(imageUri)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(ivImage);
                        loadDefaultImage = false;
                    }
                } else if (type.startsWith("application/pdf")) {
                    Uri pdfUri = (Uri) intent.getData();
                    android.util.Log.d(TAG, "onCreate: pdfUri " + pdfUri);
                    ivImage.setVisibility(android.view.View.GONE);

                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("uri", pdfUri.toString());
                    startActivity(new Intent(MainActivity.this, PdfRendererBasicFragment.class)
                            .putExtra("bundle", args)
                    );


                }
            }
            if (loadDefaultImage) {
                com.bumptech.glide.Glide.with(MainActivity.this)
                        .load(Uri.parse("file:///android_asset/Example.png"))
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {

                                showSavedHighlights(resource);
                                return false;
                            }
                        })
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivImage);
            }
            initImageView();


            com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    pickImage();
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                }
            });

            com.google.android.material.floatingactionbutton.FloatingActionButton fabPdf = findViewById(R.id.fabPdf);
            fabPdf.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        pickPdf();
                    } else {
                        android.os.Bundle bundle = new android.os.Bundle();
                        mFirebaseAnalytics.logEvent("DEVICE_NO_SUPPORT_PDF", bundle);
                        Snackbar.make(view, "Your device version doesn't support our PDF opening library!", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
            });
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
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
                ivImage.setVisibility(android.view.View.VISIBLE);
                findViewById(R.id.container).setVisibility(android.view.View.GONE);
                //ImageViewUtils.updateImageViewMatrix(ivImage, ((BitmapDrawable) ivImage.getDrawable()).getBitmap());
                //ivImage.resetOCR();

                com.bumptech.glide.Glide.with(MainActivity.this)
                        .load(data.getData())
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {

                                showSavedHighlights(resource);
                                return false;
                            }
                        })

                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivImage);

                initImageView();

                android.os.Bundle bundle = new android.os.Bundle();
                mFirebaseAnalytics.logEvent("IMAGE_CHANGE", bundle);
            }
            else if (requestCode == SELECT_PDF) {
                android.os.Bundle bundle = new android.os.Bundle();
                mFirebaseAnalytics.logEvent("PDF_CHANGE", bundle);
                ivImage.setVisibility(android.view.View.GONE);
                findViewById(R.id.container).setVisibility(android.view.View.VISIBLE);


                android.os.Bundle args = new android.os.Bundle();
                args.putString("uri", data.getData().toString());
                startActivity(new Intent(MainActivity.this,PdfRendererBasicFragment.class)
                        .putExtra("bundle",args)
                );
            }
        }
    }



    public void showSavedHighlights(android.graphics.drawable.Drawable drawable){
        //get last read image
        //...


        //show highlights on image
        new Thread(){
            @Override
            public void run() {
                savedRects.clear();
                for(Images imageInfo:imagesDao.getAllImage(currentUri)){
                    Rect rect=new Rect(imageInfo.left,imageInfo.top,imageInfo.right,imageInfo.bottom);
                    MyVisionWordModel visionWordModel=new MyVisionWordModel(rect,imageInfo.text,imageInfo.color,imageInfo.note);
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
                            if(visionWordModel.note.isEmpty()) {
                                canvas.drawRect(visionWordModel.mrect, paint);
                            }
                            else{

                            }
                            if(finalI ==savedRects.size()){
                                ivImage.invalidate();
                            }
                        }
                    });


                }


            }
        }.start();
    }


    public void highlightSelected(View v){
        if(((ImageView)v).getDrawable()!=null){
            //remove highlight and x
            recentHighlight=v.getId();
            ImageView iv=findViewById(recentHighlight);
            iv.setImageDrawable(null);

            ivImage.highlight("#00000000");
            ivImage.invalidate();

        }
        else {
            //add highlight and remove x from previous marker
            if(recentHighlight!=null){
                ImageView iv=findViewById(recentHighlight);
                iv.setImageDrawable(null);
            }

            recentHighlight=v.getId();
            ImageView iv=findViewById(recentHighlight);
            iv.setImageDrawable(getResources().getDrawable(R.drawable.ic_clear));

            ivImage.highlight(v.getTag().toString());
            ivImage.invalidate();
        }

    }


    class MyGridView extends ArrayAdapter {


        public MyGridView(Context c){
            super(c, android.R.layout.simple_list_item_1,colorArray);
        }


        @androidx.annotation.NonNull
        @Override
        public android.view.View getView(int position, @androidx.annotation.Nullable android.view.View convertView, @androidx.annotation.NonNull ViewGroup parent) {
            convertView=getLayoutInflater().inflate(R.layout.marker,null,false);
            android.widget.TextView tv=convertView.findViewById(R.id.tv);
            tv.setBackgroundColor(Color.parseColor(colorArray.get(position)));
            tv.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    ivImage.highlight(colorArray.get(position));
                }
            });
            return convertView;
        }
    }
}



