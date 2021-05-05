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
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.technikh.imagetextgrabber.R;
import com.technikh.imagetextgrabber.room.entity.Highlights;
import com.technikh.imagetextgrabber.widgets.TouchImageView;

import static com.technikh.imagetextgrabber.activities.MainActivity.DBNAME;
import static com.technikh.imagetextgrabber.activities.MainActivity.db;

/**
 * This fragment has a big {@ImageView} that shows PDF pages, and 2
 * {@link Button}s to move between pages. We use a
 * {@link PdfRenderer} to render PDF pages as
 * {@link Bitmap}s.
 */
public class PdfRendererBasicFragment extends AppCompatActivity implements android.view.View.OnClickListener {

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
     * {@link PdfRenderer} to render the PDF.
     */
    private PdfRenderer mPdfRenderer;

    /**
     * Page that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;

    /**
     * {@link ImageView} that shows a PDF page as a {@link Bitmap}
     */
    public TouchImageView mImageView;
    private RelativeLayout imageParentLayout;
    private ViewPager viewPager;
    EditText et_image_text;

    /**
     * {@link Button} to move to the previous page.
     */
    private Button mButtonPrevious;

    /**
     * {@link Button} to move to the next page.
     */
    private Button mButtonNext;

    /**
     * PDF page index
     */
    private int mPageIndex;
    private com.technikh.imagetextgrabber.room.dao.HighlightDataAccess markerDao;
    private com.technikh.imagetextgrabber.room.dao.ImagesDataAccess imagesDao;
    private ArrayList<String> colorArray;

    public PdfRendererBasicFragment() {
    }

    @Override
    protected void onCreate(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) {
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

        db= androidx.room.Room.databaseBuilder(getApplicationContext(),
                com.technikh.imagetextgrabber.room.MyDatabase.class, DBNAME).build();


        markerDao=db.getHighlightsDao();
        imagesDao=db.getImagesDao();











        android.os.Bundle args = getIntent().getBundleExtra("bundle");
        if(args != null) {
            pdfFileUri = Uri.parse(args.getString("uri", ""));
            android.util.Log.d(TAG, "onViewCreated: pdfFileUri " + pdfFileUri);
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
            android.os.Bundle bundle = new android.os.Bundle();
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
            android.os.Bundle bundle = new android.os.Bundle();
            mFirebaseAnalytics.logEvent("EXCEPTION_onStop", bundle);
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    /**
     * Sets up a {@link PdfRenderer} and related resources.
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
                java.io.InputStream asset = context.getContentResolver().openInputStream(pdfFileUri);
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
            android.database.Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
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
        android.util.Log.d(TAG, "getFileName: result "+result);
        return result;
    }

    /**
     * Closes the {@link PdfRenderer} and related resources.
     *
     * @throws IOException When the PDF file cannot be closed.
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
                android.os.Bundle bundle = new android.os.Bundle();
                mFirebaseAnalytics.logEvent("EXCEPTION_mCurrentPage_Close", bundle);
            }
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        android.util.Log.d(TAG, "showPage: getResources().getDisplayMetrics().widthPixels "+ getResources().getDisplayMetrics().widthPixels);
        android.util.Log.d(TAG, "showPage: getResources().getDisplayMetrics().heightPixels "+ getResources().getDisplayMetrics().heightPixels);
        float densityQuality = getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().widthPixels;
        android.util.Log.d(TAG, "showPage: densityQuality "+densityQuality);
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
        android.util.Log.d(TAG, "showPage: after bitmap generated, before render");
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
    public void onClick(android.view.View view) {
        switch (view.getId()) {
            case R.id.previous: {
                //mImageView.resetOCR();
                // Move to the previous page
                showPage(mCurrentPage.getIndex() - 1, null);
                break;
            }
            case R.id.next: {
                //mImageView.resetOCR();
                // Move to the next page
                showPage(mCurrentPage.getIndex() + 1, null);
                break;
            }
        }
    }

    private class ImagePagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            android.view.View layout = (android.view.View) inflater.inflate(R.layout.row_pager_image, collection, false);
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
        public boolean isViewFromObject(android.view.View view, Object object) {
            return view == object;
        }


    }



}
