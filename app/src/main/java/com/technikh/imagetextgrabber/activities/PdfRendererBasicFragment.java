/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.technikh.imagetextgrabber.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.room.Room;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.github.dhaval2404.colorpicker.model.ColorSwatch;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.technikh.imagetextgrabber.R;
import com.technikh.imagetextgrabber.activities.MainActivity;
import com.technikh.imagetextgrabber.room.MyDatabase;
import com.technikh.imagetextgrabber.room.dao.HighlightDataAccess;
import com.technikh.imagetextgrabber.room.dao.ImagesDataAccess;
import com.technikh.imagetextgrabber.room.entity.Highlights;
import com.technikh.imagetextgrabber.widgets.TouchImageView;

import static com.technikh.imagetextgrabber.activities.MainActivity.DBNAME;
import static com.technikh.imagetextgrabber.activities.MainActivity.db;

/**
 * This fragment has a big {@ImageView} that shows PDF pages, and 2
 * {@link android.widget.Button}s to move between pages. We use a
 * {@link android.graphics.pdf.PdfRenderer} to render PDF pages as
 * {@link android.graphics.Bitmap}s.
 */
public class PdfRendererBasicFragment extends AppCompatActivity implements View.OnClickListener {

    /**
     * Key string for saving the state of current page index.
     */
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    private String TAG = "PdfRendererBasicFragment";
    private Context mContext;
    private FirebaseAnalytics mFirebaseAnalytics;

    /**
     * The filename of the PDF.
     */
    private static final String FILENAME = "sample.pdf";
    private Uri pdfFileUri;

    /**
     * File descriptor of the PDF.
     */
    private ParcelFileDescriptor mFileDescriptor;

    /**
     * {@link android.graphics.pdf.PdfRenderer} to render the PDF.
     */
    private PdfRenderer mPdfRenderer;

    /**
     * Page that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;

    /**
     * {@link android.widget.ImageView} that shows a PDF page as a {@link android.graphics.Bitmap}
     */
    public TouchImageView mImageView;
    private RelativeLayout imageParentLayout;
    private ViewPager viewPager;
    EditText et_image_text;

    /**
     * {@link android.widget.Button} to move to the previous page.
     */
    private Button mButtonPrevious;

    /**
     * {@link android.widget.Button} to move to the next page.
     */
    private Button mButtonNext;

    /**
     * PDF page index
     */
    private int mPageIndex;
    private HighlightDataAccess markerDao;
    private MaterialColorPickerDialog colorPickerDialog;
    private ImagesDataAccess imagesDao;
    private ArrayList<String> colorArray;

    public PdfRendererBasicFragment() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pdf_renderer_basic);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // Retain view references.
        viewPager = findViewById(R.id.viewPager);
        //mImageView = (TouchImageView) view.findViewById(R.id.image);
        imageParentLayout = findViewById(R.id.rlParentWrapper);
        et_image_text = findViewById(R.id.et_image_text);
        mButtonPrevious = (Button) findViewById(R.id.previous);
        mButtonNext = (Button) findViewById(R.id.next);
        // Bind events.
        mButtonPrevious.setOnClickListener(this);
        mButtonNext.setOnClickListener(this);


        colorArray = new ArrayList<>();
        colorArray.add("#f6e58d");

        db= Room.databaseBuilder(getApplicationContext(),
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
                        mImageView.highlight(s);
                        mImageView.invalidate();

                        //Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();

                    }


                }).build();

        findViewById(R.id.hl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @Override
                    public void run() {
                        colorArray.clear();
                        for(Highlights marker:markerDao.getMarkers()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    colorArray.add(marker.color);
                                }
                            });

                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                colorPickerDialog.show();
                            }
                        });

                    }
                }.start();

            }
        });

        findViewById(R.id.a_r).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(PdfRendererBasicFragment.this)
                        .setTitle("")
                        .setMessage("")
                        .setNegativeButton("ADD", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new ColorPickerDialog
                                        .Builder(PdfRendererBasicFragment.this)

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
                        })


                        .setPositiveButton("REMOVE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new MaterialColorPickerDialog
                                        .Builder(PdfRendererBasicFragment.this)

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
                        .show();
            }
        });





        Bundle args = getIntent().getBundleExtra("bundle");
        if(args != null) {
            pdfFileUri = Uri.parse(args.getString("uri", ""));
            Log.d(TAG, "onViewCreated: pdfFileUri " + pdfFileUri);
        }

        mPageIndex = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            mPageIndex = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
        }

    }


    @Override
    public void onStart() {
        super.onStart();
        try {
            openRenderer(this);
            //showPage(mPageIndex, null);
        } catch (IOException e) {
            e.printStackTrace();
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("EXCEPTION_onStart", bundle);
            Toast.makeText(this, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        ImagePagerAdapter adapter;
        adapter = new ImagePagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
    }

    @Override
    public void onStop() {
        try {
            closeRenderer();
        } catch (IOException e) {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("EXCEPTION_onStop", bundle);
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    /**
     * Sets up a {@link android.graphics.pdf.PdfRenderer} and related resources.
     */
    private void openRenderer(Context context) throws IOException {
        mContext = context;
        if(pdfFileUri != null) {
            File file = new File(context.getCacheDir(), getFileName(pdfFileUri));
            if (!file.exists()) {
                // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
                // the cache directory.
                //InputStream asset = context.getAssets().open(FILENAME);
                //Uri myUri = Uri.parse("content://com.android.providers.downloads.documents/document/4406");
                InputStream asset = context.getContentResolver().openInputStream(pdfFileUri);
                FileOutputStream output = new FileOutputStream(file);
                final byte[] buffer = new byte[1024];
                int size;
                while ((size = asset.read(buffer)) != -1) {
                    output.write(buffer, 0, size);
                }
                asset.close();
                output.close();
            }
            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            // This is the PdfRenderer we use to render the PDF.
            if (mFileDescriptor != null) {
                mPdfRenderer = new PdfRenderer(mFileDescriptor);
            }
        }else{
            throw new IOException("Something wrong with fetching the PDF!");
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        Log.d(TAG, "getFileName: result "+result);
        return result;
    }

    /**
     * Closes the {@link android.graphics.pdf.PdfRenderer} and related resources.
     *
     * @throws java.io.IOException When the PDF file cannot be closed.
     */
    private void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        mPdfRenderer.close();
        mFileDescriptor.close();
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    private void showPage(int index, TouchImageView lImageView) {
        et_image_text.setText("");
        mImageView = lImageView;
        if (mPdfRenderer.getPageCount() <= index) {
            return;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            try {
                mCurrentPage.close();
            }catch (Exception e){
                // java.lang.IllegalStateException: Already closed
                e.printStackTrace();
                Bundle bundle = new Bundle();
                mFirebaseAnalytics.logEvent("EXCEPTION_mCurrentPage_Close", bundle);
            }
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Log.d(TAG, "showPage: getResources().getDisplayMetrics().widthPixels "+ getResources().getDisplayMetrics().widthPixels);
        Log.d(TAG, "showPage: getResources().getDisplayMetrics().heightPixels "+ getResources().getDisplayMetrics().heightPixels);
        float densityQuality = getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().widthPixels;
        Log.d(TAG, "showPage: densityQuality "+densityQuality);
        Bitmap bitmap = Bitmap.createBitmap(
                (int)(3 * mCurrentPage.getWidth()),
                (int)(3 * mCurrentPage.getHeight()),
                Bitmap.Config.ARGB_8888
        );
        /*
        Bitmap bitmap = Bitmap.createBitmap(
                getResources().getDisplayMetrics().densityDpi * mCurrentPage.getWidth() / 72,
                getResources().getDisplayMetrics().densityDpi * mCurrentPage.getHeight() / 72,
                Bitmap.Config.ARGB_8888
        );
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);*/
        Log.d(TAG, "showPage: after bitmap generated, before render");
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.

        /*ViewGroup.LayoutParams lParams = lImageView.getLayoutParams();
        imageParentLayout.removeAllViews();
        //imageParentLayout.removeView(mImageView);
        lImageView = new TouchImageView(mContext);
        lImageView.setLayoutParams(lParams);
        imageParentLayout.addView(lImageView);*/

        mImageView.setImageBitmap(bitmap);
        lImageView.setCustomEventListener(new TouchImageView.OnCustomEventListener() {
            public void onEvent() {
                et_image_text.setText(lImageView.getContentDescription());
                et_image_text.setSelectAllOnFocus(true);
            }
        });

        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi() {
        int index = mCurrentPage.getIndex();
        int pageCount = mPdfRenderer.getPageCount();
        mButtonPrevious.setEnabled(0 != index);
        mButtonNext.setEnabled(index + 1 < pageCount);
        setTitle(getString(R.string.app_name_with_index, index + 1, pageCount));
    }

    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing.
     *
     * @return The number of pages.
     */
    public int getPageCount() {
        return mPdfRenderer.getPageCount();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.previous: {
                mImageView.resetOCR();
                // Move to the previous page
                //showPage(mCurrentPage.getIndex() - 1, null);
                break;
            }
            case R.id.next: {
                mImageView.resetOCR();
                // Move to the next page
                //showPage(mCurrentPage.getIndex() + 1, null);
                break;
            }
        }
    }

    private class ImagePagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View layout = (View) inflater.inflate(R.layout.row_pager_image, collection, false);
            collection.addView(layout);

            TouchImageView ivImage = layout.findViewById(R.id.ivImage);
            showPage(position, ivImage);



            return layout;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((RelativeLayout) view);
        }

        @Override
        public int getCount() {
            return mPdfRenderer.getPageCount();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }


    }



}
