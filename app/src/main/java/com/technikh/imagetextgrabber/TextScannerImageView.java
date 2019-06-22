package com.technikh.imagetextgrabber;

/*
 * Copyright (c) 2019. Nikhil Dubbaka from TechNikh.com under GNU AFFERO GENERAL PUBLIC LICENSE
 * Copyright and license notices must be preserved.
 * When a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler;
import com.bogdwellers.pinchtozoom.ImageViewerCorrector;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class TextScannerImageView extends AppCompatImageView {

    private static final String TAG = "TouchImageView";
    private Context mContext;

    private float lscaledImageWidth;
    private float lscaledImageHeight;
    private float loriginalImageWidth;
    private float loriginalImageHeight;
    float widthZoomFactor = 1, heightZoomFactor = 1;
    int zoomedOffsetX, zoomedOffsetY;
    Map<Rect, String> visionTextRectangles = new HashMap<Rect, String>();
    private List<Rect> selectedVisionTextRectangles = new ArrayList<Rect>();
    private List<String> selectedVisionText = new ArrayList<String>();
    private FirebaseVisionImage visionImage;
    Bitmap unChangedOriginalBitmap = null;

    OnCustomEventListener mListener;
    private Drawable mStartCursor;

    public interface OnCustomEventListener {
        void onEvent();
    }

    public void setCustomEventListener(OnCustomEventListener eventListener) {
        mListener = eventListener;
    }

    public TextScannerImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TextScannerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    public TextScannerImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        sharedConstructing(context);
    }

    public void resetOCR () {
        Log.d(TAG, "resetOCR: 1");
        unChangedOriginalBitmap = null;
    }

    private void init(final Context context) {
        final Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_format_clear);
        final Drawable wrappedDrawable = DrawableCompat.wrap(drawable); //Wrap the drawable so that it can be tinted pre Lollipop
        DrawableCompat.setTint(wrappedDrawable, getResources().getColor(android.R.color.holo_red_dark));
        mStartCursor = wrappedDrawable;
        mStartCursor.setBounds(0, 0, mStartCursor.getIntrinsicHeight(), mStartCursor.getIntrinsicHeight());
        TextScannerImageView.super.setImageDrawable(mStartCursor);
        setClearIconVisible(true);
    }

    private void setClearIconVisible(final boolean visible) {
        mStartCursor.setVisible(visible, false);
        /*final Drawable[] compoundDrawables = getCompoundDrawables();
        setCompoundDrawables(
                compoundDrawables[0],
                compoundDrawables[1],
                visible ? mStartCursor : null,
                compoundDrawables[3]);*/
    }

    private void sharedConstructing(Context context) {
        this.mContext = context;

        init(context);

        ImageViewerCorrector corrector = new ImageViewerCorrector() {
/*
            @Override
            public void setMatrix(Matrix matrix) {
                super.setMatrix(matrix);
            }

            @Override
            public float correctAbsolute(int vector, float x) {

                float y = super.correctAbsolute(vector, x);
                Log.d(TAG, "aqa correctAbsolute: vector " + vector + " before correction " + x + " after correction " + y);
                return y;
            }
*/
            @Override
            public void performAbsoluteCorrections() {
                super.performAbsoluteCorrections();
                lscaledImageWidth = getScaledImageWidth();
                lscaledImageHeight = getScaledImageHeight();
                Log.d(TAG, "aqa updateScaledImageDimensions: getScaledImageWidth " + getScaledImageWidth());
            }
        };

        ImageMatrixTouchHandler imageMatrixTouchHandler = new ImageMatrixTouchHandler(mContext, corrector) {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                //Log.d(TAG, "onTouch: ");
                Log.d(TAG, "zxc onTouch: " + event.getRawX() + " ; " + event.getX());
                super.onTouch(view, event);

                ImageView imageView;
                try {
                    imageView = (ImageView) view;
                } catch (ClassCastException e) {
                    throw new IllegalStateException("View must be an instance of ImageView", e);
                }
                // Get the matrix
                Matrix matrix = imageView.getImageMatrix();
                /*float[] pts = {0, 0};
                matrix.mapPoints(pts);
                Log.d(TAG, "jkl onTouch: mapPoints " + pts[0] + ", " + pts[1]);*/

                final float[] values = new float[9];
                matrix.getValues(values);

                float transX = values[Matrix.MTRANS_X];
                float transY = values[Matrix.MTRANS_Y];

                Log.d(TAG, "fgh onTouch: lscaledImageWidth " + lscaledImageWidth + " loriginalImageWidth " + loriginalImageWidth);
                if (lscaledImageWidth < loriginalImageWidth) {
                    lscaledImageWidth = loriginalImageWidth;
                }
                if (lscaledImageHeight < loriginalImageHeight) {
                    lscaledImageHeight = loriginalImageHeight;
                }
                widthZoomFactor = lscaledImageWidth / loriginalImageWidth;
                if(widthZoomFactor == 1){
                    zoomedOffsetX = (int) (-1*(Math.abs(transX)) / widthZoomFactor);
                }else {
                    zoomedOffsetX = (int) ((Math.abs(transX)) / widthZoomFactor);
                }
                heightZoomFactor = lscaledImageHeight / loriginalImageHeight;
                if(heightZoomFactor == 1){
                    //zoomedOffsetY = 0;
                    // If there is a Toolbar widget at the top, that will add to offset
                    zoomedOffsetY = (int) (1*(Math.abs(transY)) / heightZoomFactor);
                }else {
                    zoomedOffsetY = (int) ((Math.abs(transY)) / heightZoomFactor);
                }

                Log.d(TAG, "jkl getValues " + transX + " : " + transY);


                final MotionEvent event1 = event;

                final GestureDetector gestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
                    public void onLongPress(MotionEvent e) {
                        final int touchX = (int) e.getX();
                        final int touchY = (int) e.getY();


                        //Drawable drawable = ivImage.getDrawable();
                        Log.d(TAG, "wqw onTouch:  getX    X: " + e.getX() + " Y: " + e.getY() + " event X: " + event1.getX() + " Y: " + event1.getY());
                        Log.d(TAG, "wqw onTouch:  getRawX X: " + e.getRawX() + " Y: " + e.getRawY() + " event X: " + event1.getRawX() + " Y: " + event1.getRawY());
                        //Log.e(TAG, "Longpress detected event "+event.getAction()+" e "+e.getAction());
                        if (event1.getAction() == MotionEvent.ACTION_UP) {
                            selectWordOnTouch(touchX, touchY);
                        } else if (event1.getAction() == MotionEvent.ACTION_DOWN) {
                          //  Log.d(TAG, "onLongPress: real detected X: " + e.getX() + " Y: " + e.getY() + " event X: " + event.getX() + " Y: " + event.getY() + " raw Y" + event.getRawY());
                            Toast.makeText(mContext, "Long Clicked ", Toast.LENGTH_SHORT).show();

                            //Matrix matrix = ivImage.getImageMatrix();

                                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    bitmap = ((BitmapDrawable) ivImage.getForeground()).getBitmap();
                                }else{*/

                            if (unChangedOriginalBitmap == null) {
                                Log.d(TAG, "xyz onLongPress: unChangedOriginalBitmap null");
                                unChangedOriginalBitmap = ((BitmapDrawable) TextScannerImageView.super.getDrawable()).getBitmap();
                                loriginalImageWidth = unChangedOriginalBitmap.getWidth();
                                loriginalImageHeight = unChangedOriginalBitmap.getHeight();
                                Log.d(TAG, "wqw onLongPress: originalBitmap.getWidth()" + unChangedOriginalBitmap.getWidth());
                                Log.d(TAG, "wqw onLongPress: originalBitmap.getHeight()" + unChangedOriginalBitmap.getHeight());
                            } else {
                                Log.d(TAG, "xyz onLongPress: unChangedOriginalBitmap not null");
                            }
                            final Bitmap originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);

                            visionImage = FirebaseVisionImage.fromBitmap(originalBitmap);
                            FirebaseVisionTextRecognizer visionDetector = FirebaseVision.getInstance()
                                    .getOnDeviceTextRecognizer();

                            Task<FirebaseVisionText> result =
                                    visionDetector.processImage(visionImage)
                                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                                @Override
                                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                                    // Task completed successfully
                                                    // ...
                                                    Log.d(TAG, "visionDetector onSuccess: " + firebaseVisionText.getText());
                                                    List<FirebaseVisionText.TextBlock> textBlocks = firebaseVisionText.getTextBlocks();
                                                    visionTextRectangles.clear();
                                                    selectedVisionText.clear();
                                                    selectedVisionTextRectangles.clear();
                                                    for (int i = 0; i < textBlocks.size(); i++) {
                                                        FirebaseVisionText.TextBlock tBlock = textBlocks.get(i);
                                                        List<FirebaseVisionText.Line> tBlockLines = tBlock.getLines();
                                                        for (int j = 0; j < tBlockLines.size(); j++) {
                                                            FirebaseVisionText.Line tLine = tBlockLines.get(j);
                                                            List<FirebaseVisionText.Element> tLineElements = tLine.getElements();
                                                            for (int k = 0; k < tLineElements.size(); k++) {
                                                                FirebaseVisionText.Element tElement = tLineElements.get(k);
                                                                Rect boundingRect = tElement.getBoundingBox();
                                                                Log.d(TAG, "wqw onSuccess: visionTextRectangles " + tElement.getText());
                                                                Log.d(TAG, "wqw onSuccess: boundingRect " + boundingRect.toShortString());
                                                                visionTextRectangles.put(boundingRect, tElement.getText());

                                                                Canvas canvas = new Canvas(originalBitmap);
                                                                // Initialize a new Paint instance to draw the Rectangle
                                                                Paint paint = new Paint();
                                                                paint.setStyle(Paint.Style.STROKE);
                                                                paint.setStrokeWidth(2);
                                                                paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                                                                paint.setColor(Color.YELLOW);
                                                                paint.setAntiAlias(true);

                                                                canvas.drawBitmap(originalBitmap, 0, 0, paint);
                                                                //canvas.drawText("Testing...", 10, 10, paint);
                                                                canvas.drawRect(boundingRect, paint);
                                                            }
                                                        }
                                                    }
                                                    TextScannerImageView.super.setImageBitmap(originalBitmap);
                                                    //ivImage.invalidate();
                                                    // TODO: select the longpressed word
                                                    selectWordOnTouch(touchX, touchY);
                                                }
                                            })
                                            .addOnFailureListener(
                                                    new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            // Task failed with an exception
                                                            // ...
                                                            Log.d(TAG, "visionDetector onFailure: " + e.getMessage());
                                                        }
                                                    });
                        }
                    }
                });
                Log.d(TAG, "zxc onTouch: " + event.getRawX() + " ; " + event.getX());
                gestureDetector.onTouchEvent(event);
                Log.d(TAG, "zxc onTouch: " + event.getRawX() + " ; " + event.getX());
                return true; // indicate event was handled
            }
        };

        TextScannerImageView.super.setOnTouchListener(imageMatrixTouchHandler);
        //TouchImageView.super.setOnTouchListener(new ImageMatrixTouchHandler(mContext));
    }

    private void selectWordOnTouch(int touchX, int touchY) {

        int zoomedTouchX = zoomedOffsetX+(int)(touchX/widthZoomFactor);
        int zoomedTouchY = zoomedOffsetY+(int)(touchY/heightZoomFactor);
        Log.d(TAG, "mnop onLongPress: zoomedOffsetX "+zoomedOffsetX);
        Log.d(TAG, "mnop onLongPress: zoomedOffsetY "+zoomedOffsetY);
        Log.d(TAG, "mnop onLongPress: touchX "+touchX);
        Log.d(TAG, "mnop onLongPress: touchY "+touchY);
        Log.d(TAG, "mnop onLongPress: zoomedTouchX "+zoomedTouchX);
        Log.d(TAG, "mnop onLongPress: zoomedTouchY "+zoomedTouchY);

        Log.d(TAG, "onLongPress: MotionEvent.ACTION_UP visionTextRectangles size "+visionTextRectangles.size());
        Iterator it = visionTextRectangles.entrySet().iterator();
        Log.d(TAG, "wqw onLongPress: lscaledImageWidth "+lscaledImageWidth);
        Log.d(TAG, "wqw onLongPress: lscaledImageHeight "+lscaledImageHeight);
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Rect rect = (Rect)pair.getKey();
            // TODO: Threshold based on zoom factor
            int threshold = 5;
            if(checkRectangleMatch( rect,  zoomedTouchX,  zoomedTouchY)){
                //   Log.d(TAG, "selectWordOnTouch: checkRectangleMatch");
                // }
                // if(rect.contains(zoomedTouchX,zoomedTouchY) || rect.contains(zoomedTouchX - threshold,zoomedTouchY - threshold) || rect.contains(zoomedTouchX + threshold,zoomedTouchY + threshold)){
                Log.d(TAG, "selectWordOnTouch clicked rectangle: "+pair.getValue() + " rect: "+ rect.toShortString());
                Toast.makeText(mContext, " Clicked "+pair.getValue() , Toast.LENGTH_SHORT).show();

                final Bitmap originalBitmap;
                originalBitmap = ((BitmapDrawable) TextScannerImageView.super.getDrawable()).getBitmap();
                Canvas canvas = new Canvas(originalBitmap);

                if(selectedVisionTextRectangles.contains(rect)){
                    int pos = selectedVisionTextRectangles.indexOf(rect);
                    Log.d(TAG, "already clicked rectangle: "+pair.getValue() + " rect: "+ rect.toShortString());
                    selectedVisionTextRectangles.remove(rect);
                    selectedVisionText.remove(pos);
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                    paint.setColor(Color.YELLOW);
                    paint.setAntiAlias(true);

                    canvas.drawBitmap(originalBitmap, 0, 0, paint);
                    //canvas.drawText("Testing...", 10, 10, paint);
                    canvas.drawRect(rect, paint);
                }else {

                    selectedVisionTextRectangles.add(rect);
                    selectedVisionText.add(pair.getValue().toString());


                    // Initialize a new Paint instance to draw the Rectangle
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                    paint.setColor(Color.RED);
                    paint.setAntiAlias(true);

                    canvas.drawBitmap(originalBitmap, 0, 0, paint);
                    //canvas.drawText("Testing...", 10, 10, paint);
                    canvas.drawRect(rect, paint);
                }
                TextScannerImageView.super.setImageBitmap(originalBitmap);
                //Toast.makeText(mContext, TextUtils.join(" ", selectedVisionText), Toast.LENGTH_SHORT).show();
                TextScannerImageView.super.setContentDescription(TextUtils.join(" ", selectedVisionText));
                //ivImage.invalidate();
                //et_image_text.setText(TextUtils.join(" ", selectedVisionText));
                //et_image_text.setSelectAllOnFocus(true);
                //et_image_text.setText(et_image_text.getText()+" "+pair.getValue().toString());
                mListener.onEvent();
                break;
            }else{
                Log.d(TAG, pair.getValue()+" onLongPress: not match (int)touchX,(int)touchY) "+(int)touchX+" Y: "+(int)touchY+" rect: left "+rect.left+" top "+rect.top+" right "+rect.right+" bottom "+rect.bottom);
                Log.d(TAG, pair.getValue()+" onLongPress: not match (int)zoomedTouchX,(int)zoomedTouchY) "+(int)zoomedTouchX+" Y: "+(int)zoomedTouchY+" rect: left "+rect.left+" top "+rect.top+" right "+rect.right+" bottom "+rect.bottom);
            }
            System.out.println(pair.getKey() + " = " + pair.getValue());
            //it.remove(); // avoids a ConcurrentModificationException
        }
        Log.d(TAG, "onLongPress: visionTextRectangles size "+visionTextRectangles.size());
    }

    public boolean checkRectangleMatch(Rect rect, int zoomedTouchX, int zoomedTouchY) {
        int threshold = 5;
        int[] possibleListX = new int[] {zoomedTouchX, zoomedTouchX - threshold, zoomedTouchX + threshold};
        int[] possibleListY = new int[] {zoomedTouchY, zoomedTouchY - threshold, zoomedTouchY + threshold};
        for (int i=0; i<possibleListX.length; i++) {
            for (int j=0; j<possibleListY.length; j++) {
                if (rect.contains(possibleListX[i], possibleListY[j])) {
                    Log.d(TAG, "selectWordOnTouch: rect.contains(zoomedTouchX,zoomedTouchY) i "+ i +" j "+j);
                    return true;
                }
            }
        }
        return false;
    }

}