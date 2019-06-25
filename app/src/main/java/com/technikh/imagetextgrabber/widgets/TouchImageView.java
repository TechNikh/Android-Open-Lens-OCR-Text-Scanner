package com.technikh.imagetextgrabber.widgets;

/*
 * Copyright (c) 2019. Nikhil Dubbaka from TechNikh.com under GNU AFFERO GENERAL PUBLIC LICENSE
 * Copyright and license notices must be preserved.
 * When a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.
 */

import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler;
import com.bogdwellers.pinchtozoom.ImageViewerCorrector;
import com.bogdwellers.pinchtozoom.MultiTouchListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.technikh.imagetextgrabber.models.ImageViewSettingsModel;
import com.technikh.imagetextgrabber.models.VisionWordModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

public class TouchImageView extends AppCompatImageView {

    private static final String TAG = "TouchImageView";
    static boolean debugMode = false, experimentalFeatures = false, snapToWord = false, allowZoom = false, showMargins = false, allWordBorders = false, toggleWordOnClick = false;
    private Context mContext;
    private ImageMatrixTouchHandler imageMatrixTouchHandler;
    private MultiTouchListener multiTouchListener;

    private boolean textSelectionInProgress = false;
    private PointF startCursorPoint = new PointF(0,0);
    private PointF endCursorPoint = new PointF(0,0);
    private PointF leftMarginRulerPoint = new PointF(0,0);
    private PointF rightMarginRulerPoint = new PointF(0,0);

    private float lscaledImageWidth;
    private float lscaledImageHeight;
    private float loriginalImageWidth;
    private float loriginalImageHeight;
    float widthZoomFactor = 1, heightZoomFactor = 1;
    int zoomedOffsetX, zoomedOffsetY;

    private List<VisionWordModel> visionTextRectanglesSimplified = new ArrayList<>();
    private int minWordHeight = 1000;
    private List<VisionWordModel> zoomedVisionTextRectangles = new ArrayList<>();
    private List<VisionWordModel> selectedVisionTextRectanglesSimplified = new ArrayList<>();
    //Map<Rect, String> visionTextRectangles = new HashMap<Rect, String>();
    //private List<Rect> selectedVisionTextRectangles = new ArrayList<Rect>();
    //private List<String> selectedVisionText = new ArrayList<String>();
    private FirebaseVisionImage visionImage;
    Bitmap unChangedOriginalBitmap = null;

    OnCustomEventListener mListener;
    private Drawable mStartCursor;
    //private ImageView mivStartCursor, mivEndCursor;

    public interface OnCustomEventListener {
        void onEvent();
    }

    public void setCustomEventListener(OnCustomEventListener eventListener) {
        mListener = eventListener;
    }

    public TouchImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        sharedConstructing(context);
    }

    /*public void clearSelectedText () {
        Log.d(TAG, "clearSelectedText: 1");
        selectedVisionText.clear();
        selectedVisionTextRectangles.clear();
        TouchImageView.super.setImageBitmap(unChangedOriginalBitmap);
    }*/

    public void resetOCR () {
        Log.d(TAG, "resetOCR: 1");
        unChangedOriginalBitmap = null;
        onImageChangeInImageView();
    }

    public void initOptions (ImageViewSettingsModel imageViewSettingsModel){
        if(imageViewSettingsModel.isZoomAllowed()){
            allowZoom = true;
            //TouchImageView.super.setOnTouchListener(imageMatrixTouchHandler);
            imageMatrixTouchHandler.setScaleEnabled(true);
            imageMatrixTouchHandler.setDragOnPinchEnabled(true);
            imageMatrixTouchHandler.setTranslateEnabled(true);
        }else{
            allowZoom = false;
            //TouchImageView.super.setOnTouchListener(multiTouchListener);
            imageMatrixTouchHandler.setScaleEnabled(false);
            imageMatrixTouchHandler.setDragOnPinchEnabled(false);
            imageMatrixTouchHandler.setTranslateEnabled(false);
        }
        if(imageViewSettingsModel.isSnaptoWordMode()){
            snapToWord = true;
        }else{
            snapToWord = false;
        }
        if(imageViewSettingsModel.isMarginsEnabled()){
            showMargins = true;
        }else{
            showMargins = false;
        }
        if(imageViewSettingsModel.isAllWordBordersEnabled()){
            allWordBorders = true;
        }else{
            allWordBorders = false;
        }
        if(imageViewSettingsModel.isToggleWordEnabled()){
            toggleWordOnClick = true;
        }else{
            toggleWordOnClick = false;
        }
    }

    public void initCursors (ImageView ivStartCursor, ImageView ivEndCursor) {
        Log.d(TAG, "initCursors: 1");
        ivStartCursor.setOnTouchListener(new MyTouchListener());
        ivStartCursor.setOnDragListener(new MyDragListener());
        ivEndCursor.setOnTouchListener(new MyTouchListener());
        ivEndCursor.setOnDragListener(new MyDragListener());
        /*mivStartCursor = ivStartCursor;
        mivEndCursor = ivEndCursor;*/
    }

    private final class MyTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(
                        view);
                view.startDrag(data, shadowBuilder, view, 0);
                //view.setVisibility(View.INVISIBLE);
                Log.d(TAG, "vbn onTouch: ");
                return true;
            } else {
                return false;
            }
        }
    }

    class MyDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            Log.d(TAG, "vbn onDrag: ");
            int action = event.getAction();
            Log.d(TAG, "vbn onDrag: action "+action+" x "+event.getX());
            switch (action) {
                case DragEvent.ACTION_DRAG_LOCATION:
                    //selectWordInBetweenCursors();
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // Dropped, reassign View to ViewGroup
                    Log.d(TAG, "onDrag: ACTION_DROP "+v.getId());
                    View view = (View) event.getLocalState();
                    Log.d(TAG, "onDrag: ACTION_DROP "+view.getClass().getSimpleName());
                    ViewGroup owner = (ViewGroup) view.getParent();
                    Log.d(TAG, "onDrag: ACTION_DROP "+owner.getClass().getSimpleName());
                    //owner.removeView(view);
                    RelativeLayout container = (RelativeLayout) owner;

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
                    Log.d(TAG, "onDrag: (int)event.getX() "+(int)event.getX());
                    Log.d(TAG, "onDrag: (int)event.getY() "+(int)event.getY());
                    params.leftMargin = (int)event.getX();
                    params.topMargin = (int)event.getY();
                    view.setLayoutParams(params);
                    //container.addView(view, params);
                    //container.addView(view);
                    view.setVisibility(View.VISIBLE);
                    //selectWordInBetweenCursors();
                    //v.setVisibility(View.VISIBLE);
                    break;
            }
            return true;
        }
    }

    private void onImageChangeInImageView() {
        visionTextRectanglesSimplified.clear();
        selectedVisionTextRectanglesSimplified.clear();
        //selectedVisionTextRectangles.clear();
        //selectedVisionText.clear();
        TouchImageView.super.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){

            @Override
            public boolean onPreDraw() {
                //Log.d(TAG, "onPreDraw: ");
                try {
                    if(unChangedOriginalBitmap == null && TouchImageView.super.getDrawable() != null) {
                        unChangedOriginalBitmap = ((BitmapDrawable) TouchImageView.super.getDrawable()).getBitmap();
                        final Bitmap originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);
                        //Log.d(TAG, "onPreDraw: unChangedOriginalBitmap");
                        loriginalImageWidth = originalBitmap.getWidth();
                        loriginalImageHeight = originalBitmap.getHeight();

                        if (visionTextRectanglesSimplified.isEmpty()) {
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
                                                    //Log.d(TAG, "visionDetector onSuccess: " + firebaseVisionText.getText());
                                                    List<FirebaseVisionText.TextBlock> textBlocks = firebaseVisionText.getTextBlocks();
                                                    //visionTextRectangles.clear();
                                                    //selectedVisionText.clear();
                                                    //selectedVisionTextRectangles.clear();

                                                    selectedVisionTextRectanglesSimplified.clear();
                                                    for (int i = 0; i < textBlocks.size(); i++) {
                                                        FirebaseVisionText.TextBlock tBlock = textBlocks.get(i);
                                                        List<FirebaseVisionText.Line> tBlockLines = tBlock.getLines();
                                                        for (int j = 0; j < tBlockLines.size(); j++) {
                                                            FirebaseVisionText.Line tLine = tBlockLines.get(j);
                                                            List<FirebaseVisionText.Element> tLineElements = tLine.getElements();
                                                            for (int k = 0; k < tLineElements.size(); k++) {
                                                                FirebaseVisionText.Element tElement = tLineElements.get(k);
                                                                Rect boundingRect = tElement.getBoundingBox();
                                                                //Log.d(TAG, "wqw onSuccess: visionTextRectangles " + tElement.getText());
                                                                //Log.d(TAG, "wqw onSuccess: boundingRect " + boundingRect.toShortString());
                                                                //visionTextRectangles.put(boundingRect, tElement.getText());
                                                                VisionWordModel visionWordModel = new VisionWordModel(boundingRect, tElement.getText());
                                                                visionTextRectanglesSimplified.add(visionWordModel);
                                                                if(boundingRect.height() < minWordHeight){
                                                                    minWordHeight = boundingRect.height();
                                                                }
/*
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
                                                                    canvas.drawRect(boundingRect, paint);*/
                                                            }
                                                        }
                                                    }
                                                    Collections.sort(visionTextRectanglesSimplified, new Comparator<VisionWordModel>() {
                                                        @Override
                                                        public int compare(VisionWordModel p1, VisionWordModel p2) {
                                                            // Ascending first left as same line words can have top +/- 5
                                                            int threshold = minWordHeight;
                                                            int c = 0;
                                                            if((p1.mrect.top < p2.mrect.top + threshold) && (p1.mrect.top < p2.mrect.top - threshold)){
                                                                c = -1;
                                                            }else if((p1.mrect.top > p2.mrect.top + threshold) && (p1.mrect.top > p2.mrect.top - threshold)){
                                                                c = 1;
                                                            }
                                                            if (c == 0)
                                                                c = Double.compare(p1.mrect.left, p2.mrect.left);
                                                            return c;
                                                        }
                                                    });
                                                    //TouchImageView.super.setImageBitmap(originalBitmap);
                                                    //ivImage.invalidate();
                                                    // TODO: select the longpressed word
                                                    //selectWordOnTouch(touchX, touchY);
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
                        TouchImageView.super.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                return true;    //note, that "true" is important, since you don't want drawing pass to be canceled
            }
        });
    }

    private boolean isLongClick(MotionEvent event1, boolean cancelZoomNav) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            return (event1.getAction() == MotionEvent.ACTION_DOWN);
        }else{
            return (event1.getAction() == MotionEvent.ACTION_MOVE) && !cancelZoomNav;
        }
    }

    private void sharedConstructing(Context context) {
       // Log.d(TAG, "sharedConstructing: ");
        this.mContext = context;

        onImageChangeInImageView();

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
                //float innerFitScale = getInnerFitScale();
                //Log.d(TAG, "performAbsoluteCorrections: innerFitScale "+innerFitScale);
              //  Log.d(TAG, "aqa updateScaledImageDimensions: getScaledImageWidth " + getScaledImageWidth());
            }
        };
        multiTouchListener = new MultiTouchListener() {

        };
        imageMatrixTouchHandler = new ImageMatrixTouchHandler(mContext, corrector) {
            private int mode;
            private boolean updateTouchState = true;
            private boolean cancelZoomNav = false, startCursorMode = false, endCursorMode = false, leftRulerPointMode = false, rightRulerPointMode = false;
            private List<Integer> pointerIds = new ArrayList<>(40);;
            private SparseArray<PointF> startPoints = new SparseArray<>();

            //public static final int DRAG = 1;

            private PointF getTranslatedPoint(float touchX, float touchY){
                PointF lPoint = new PointF(touchX, touchY);
                float zoomedTouchX = zoomedOffsetX+(touchX/widthZoomFactor);
                float zoomedTouchY = zoomedOffsetY+(touchY/heightZoomFactor);
                if(heightZoomFactor <= 1){
                    zoomedTouchY = touchY - zoomedOffsetY;
                }
                if(widthZoomFactor <= 1){
                    zoomedTouchX = touchX - zoomedOffsetX;
                }
                lPoint.x = zoomedTouchX;
                lPoint.y = zoomedTouchY;
                return lPoint;
            }

            private boolean checkTouchPoint(PointF lPoint, float touchX, float touchY, int xOffset, int yOffset){
                int thresholdRadius = 20;

                float zoomedTouchX = zoomedOffsetX+(int)(touchX/widthZoomFactor);
                float zoomedTouchY = zoomedOffsetY+(int)(touchY/heightZoomFactor);
                if(heightZoomFactor <= 1){
                    zoomedTouchY = touchY - zoomedOffsetY;
                }
                if(widthZoomFactor <= 1){
                    zoomedTouchX = touchX - zoomedOffsetX;
                }
                if(zoomedTouchX >= lPoint.x + xOffset - thresholdRadius && zoomedTouchX <= lPoint.x + xOffset + thresholdRadius && zoomedTouchY >= lPoint.y + yOffset - thresholdRadius && zoomedTouchY <= lPoint.y + yOffset + thresholdRadius){
                    return true;
                }
               // Log.d(TAG, "checkTouchPoint: not match zoomedTouchX "+ zoomedTouchX +" zoomedTouchY "+zoomedTouchY + " lPoint "+lPoint.toString());
                return false;
            }
            private void evaluateTouchState(MotionEvent event, Matrix matrix) {
                //Log.d(TAG, "ter evaluateTouchState: ");
                // Update the mode
                int touchCount = getTouchCount();
                //Log.d(TAG, "ter evaluateTouchState: touchCount "+touchCount);
                if(touchCount == 0) {
                    updateTouchState = true;
                    mode = NONE;
                } else {
                    if(touchCount == 1) {
                        mode = DRAG;
                    }
                }
                //Log.d(TAG, "ter evaluateTouchState: mode "+mode);
            }

            @Override
            public boolean onTouch(View view, final MotionEvent event) {
              //  Log.d(TAG, "onTouch: startCursorPoint "+startCursorPoint.toString()+" endCursorPoint "+endCursorPoint.toString());
                ImageView imageView;
                Integer pointerId;
                try {
                    imageView = (ImageView) view;
                } catch (ClassCastException e) {
                    throw new IllegalStateException("View must be an instance of ImageView", e);
                }
                // Get the matrix
                Matrix matrix = imageView.getImageMatrix();
                //Log.d(TAG, "onTouch: ");
                //Log.d(TAG, "zxc onTouch: " + event.getRawX() + " ; " + event.getX());
                int actionMasked = event.getActionMasked();
                int actionIndex = event.getActionIndex();
/*
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d(TAG, "onTouch: actionMasked " + MotionEvent.actionToString(actionMasked) +" mode "+mode);
                }*/
                switch (actionMasked) {
                    case MotionEvent.ACTION_UP:
                        //Log.d(TAG, "onTouch: ACTION_UP mode "+mode);
                        if (mode == DRAG) {
                            cancelZoomNav = false;
                            startCursorMode = false;
                            endCursorMode = false;
                            leftRulerPointMode = false;
                            rightRulerPointMode = false;
                            // Update zoomedVisionTextRectangles
                            if((loriginalImageHeight/loriginalImageWidth) > 1.3) {
                                // Zoomed area is depending on aspect ratio of the image and device - portrait or landscape
                                if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                                    zoomedVisionTextRectangles = visionTextRectanglesSimplified.stream().filter(filteredVisionWord -> (isWordWithinVisibleArea(filteredVisionWord))).collect(Collectors.toList());
                                    //Log.d(TAG, "rdfer onTouch: zoomedVisionTextRectangles = visionTextRectanglesSimplified.stream()");
                                }
                            }else{
                                //Log.d(TAG, "rdfer onTouch: zoomedVisionTextRectangles = visionTextRectanglesSimplified ");
                                zoomedVisionTextRectangles = visionTextRectanglesSimplified;
                            }
                            /*//Paint zoomedarea rectangle
                            Log.d(TAG, "sareds onTouch: getMeasuredWidth "+imageView.getMeasuredWidth()+" getWidth "+imageView.getWidth());
                            //RectF zoomedAreaRect = new RectF(zoomedOffsetX+40,zoomedOffsetY+40,zoomedOffsetX+(loriginalImageWidth/widthZoomFactor)-40,zoomedOffsetY+(loriginalImageHeight/heightZoomFactor)-40);
                            Rect zoomedAreaRect = new Rect(0, zoomedOffsetY,(int)loriginalImageWidth,zoomedOffsetY+(int)(loriginalImageHeight/heightZoomFactor));
                            //Rect r = new Rect();
                            //imageView.getWindowVisibleDisplayFrame(r);
                            Bitmap originalBitmap = ((BitmapDrawable) TouchImageView.super.getDrawable()).getBitmap();
                            //Bitmap originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);
                            Canvas canvas = new Canvas(originalBitmap);
                            // Initialize a new Paint instance to draw the Rectangle
                            Paint paint = new Paint();
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(8);
                            paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                            paint.setColor(Color.YELLOW);
                            paint.setAntiAlias(true);

                            canvas.drawBitmap(originalBitmap, 0, 0, paint);
                            //canvas.drawText("Testing...", 10, 10, paint);
                            canvas.drawRect(zoomedAreaRect, paint);
                            TouchImageView.super.setImageBitmap(originalBitmap);*/

                        }
                        if (false && mode == DRAG) {
                            cancelZoomNav = false;
                            if(startCursorMode) {
                                startCursorPoint = getTranslatedPoint(event.getX(), event.getY());
                            }else{
                                endCursorPoint = getTranslatedPoint(event.getX(), event.getY());
                            }
                            //clearSelectedText();
/*
                            // Initialize a new Paint instance to draw the Rectangle
                            Paint paint = new Paint();
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(2);
                            paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                            paint.setColor(Color.RED);
                            paint.setAntiAlias(true);

                            final Bitmap originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);
                            Canvas canvas = new Canvas(originalBitmap);
                            PointF lTranslatedTouch = getTranslatedPoint(event.getX(), event.getY());
                            canvas.drawCircle(lTranslatedTouch.x, lTranslatedTouch.y, 30, paint);
                            TouchImageView.super.setImageBitmap(originalBitmap);*/

                            //selectWordInBetweenCursors();
                        }
                    case MotionEvent.ACTION_POINTER_UP:
                        pointerId = event.getPointerId(actionIndex);
                        pointerIds.remove(pointerId);
                        startPoints.remove(pointerId);
                        evaluateTouchState(event, matrix);
                        break;
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        pointerId = event.getPointerId(actionIndex);
                        PointF startPoint = new PointF(event.getX(actionIndex), event.getY(actionIndex));

                        // Save the starting point
                        startPoints.put(pointerId, startPoint);
                        pointerIds.add(pointerId);
                        evaluateTouchState(event, matrix);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //Log.d(TAG, "ter onTouch: updateTouchState "+updateTouchState);
                        if(updateTouchState) {
                            evaluateTouchState(event, matrix);
                            updateTouchState = false;
                        }
                        //Log.d(TAG, "ter onTouch: ACTION_MOVE mode "+mode+" DRAG "+DRAG);
                        if (mode == DRAG) {
                           // Log.d(TAG, "ter onTouch: mode == DRAG");
                            if(!textSelectionInProgress && startCursorMode) {
                                selectWordInBetweenCursors(getTranslatedPoint(event.getX(), event.getY()), null, null, null);
                            }else if(!textSelectionInProgress && endCursorMode) {
                                selectWordInBetweenCursors(null, getTranslatedPoint(event.getX(), event.getY()), null, null);
                            }else if(!textSelectionInProgress && leftRulerPointMode) {
                                selectWordInBetweenCursors(null, null, getTranslatedPoint(event.getX(), event.getY()), null);
                            }else if(!textSelectionInProgress && rightRulerPointMode) {
                                selectWordInBetweenCursors(null, null, null, getTranslatedPoint(event.getX(), event.getY()));
                            }

                            //Log.d(TAG, "ter onTouch: startPoints "+startPoints.toString());
                            if(checkTouchPoint(startCursorPoint, event.getX(), event.getY(), -20, -20)){
                               // Log.d(TAG, "ter onTouch: match startCursorPoint");
                                cancelZoomNav = true;
                                startCursorMode = true;
                            }else if(checkTouchPoint(endCursorPoint, event.getX(), event.getY(), 20, 20)){
                                cancelZoomNav = true;
                                endCursorMode = true;
                            }else if(checkTouchPoint(leftMarginRulerPoint, event.getX(), event.getY(), 0,0)){
                              //  Log.d(TAG, "ter onTouch: match startCursorPoint");
                                cancelZoomNav = true;
                                leftRulerPointMode = true;
                            }else if(checkTouchPoint(rightMarginRulerPoint, event.getX(), event.getY(), 0,0)){
                                cancelZoomNav = true;
                                rightRulerPointMode = true;
                            }else{
                               // Log.d(TAG, "ter onTouch: fail startCursorPoint startCursorPoint "+startCursorPoint.toString()+" x "+event.getX()+" y "+event.getY());
                            }
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        //Log.d(TAG, "ter onTouch: ACTION_CANCEL");
                        pointerIds.clear();
                        updateTouchState = true;
                        startPoints.clear();
                        break;
                }





                /*float[] pts = {0, 0};
                matrix.mapPoints(pts);
                Log.d(TAG, "jkl onTouch: mapPoints " + pts[0] + ", " + pts[1]);*/
/*
                // Before calculating new zoomFactor & new Offset, use the old values
                int startX = mivStartCursor.getLeft()+0;  // 100 is width of cursor
                int startY = mivStartCursor.getTop()+0;
                int endX = mivEndCursor.getLeft();
                int endY = mivEndCursor.getTop();
                int visionRectLeft = (int)((startX + zoomedOffsetX*widthZoomFactor)/widthZoomFactor);
                int visionRectTop = (int)((startY + zoomedOffsetY*heightZoomFactor)/heightZoomFactor);
                int visionRectRight = (int)((endX + zoomedOffsetX*widthZoomFactor)/widthZoomFactor);
                int visionRectBottom = (int)((endY + zoomedOffsetY*heightZoomFactor)/heightZoomFactor);*/

                final float[] values = new float[9];
                matrix.getValues(values);

                float transX = values[Matrix.MTRANS_X];
                float transY = values[Matrix.MTRANS_Y];
                //Log.d(TAG, "hfdr onTouch: transY "+transY+" MSCALE_X "+values[Matrix.MSCALE_X]+" MSKEW_X "+values[Matrix.MSKEW_X]);

                //Log.d(TAG, "fgh onTouch: lscaledImageWidth " + lscaledImageWidth + " loriginalImageWidth " + loriginalImageWidth);
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
               // Log.d(TAG, "ghy onTouch: onTouch event1.getAction() "+event.getAction());

               // Log.d(TAG, "jkl getValues " + transX + " : " + transY);


                final MotionEvent event1 = event;

                final GestureDetector gestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
                    public void onLongPress(MotionEvent e) {
                        final int touchX = (int) e.getX();
                        final int touchY = (int) e.getY();

                        //Log.d(TAG, "ghy onTouch: GestureDetector e.getAction() " + e.getAction());
                        //Log.d(TAG, "ghy onTouch: GestureDetector event1.getAction() " + event1.getAction());
                        if ((e.getAction() == MotionEvent.ACTION_DOWN) && (false && event.getAction() == MotionEvent.ACTION_MOVE)) {
                            //TODO: show selected text cursor while navigating or zooming the image
                         //   Log.d(TAG, "ghy onTouch: ACTION_DOWN GestureDetector event.getAction() " + event.getAction());
/*
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
                            params.leftMargin = (int) (visionRectLeft * widthZoomFactor - zoomedOffsetX * widthZoomFactor) - 100;
                            params.topMargin = (int) (visionRectTop * heightZoomFactor - zoomedOffsetY * heightZoomFactor) - 100;
                            mivStartCursor.setLayoutParams(params);

                            RelativeLayout.LayoutParams eparams = new RelativeLayout.LayoutParams(100, 100);
                            eparams.leftMargin = (int) (visionRectRight * widthZoomFactor - zoomedOffsetX * widthZoomFactor);
                            eparams.topMargin = (int) (visionRectBottom * heightZoomFactor - zoomedOffsetY * heightZoomFactor);
                            mivEndCursor.setLayoutParams(eparams);*/
                        }
                        //Drawable drawable = ivImage.getDrawable();
                      //  Log.d(TAG, "wqw onTouch:  getX    X: " + e.getX() + " Y: " + e.getY() + " event X: " + event1.getX() + " Y: " + event1.getY());
                      //  Log.d(TAG, "wqw onTouch:  getRawX X: " + e.getRawX() + " Y: " + e.getRawY() + " event X: " + event1.getRawX() + " Y: " + event1.getRawY());
                        //Log.e(TAG, "Longpress detected event "+event.getAction()+" e "+e.getAction());
                        if (event1.getAction() == MotionEvent.ACTION_UP) {
                            if(toggleWordOnClick) {
                                selectWordOnTouch(touchX, touchY, false);
                            }
                        } else if (isLongClick(event1, cancelZoomNav)) {
                            // For Android 4.2.2 API 17, Samsung tablet, detecting long press event1.getAction() == MotionEvent.ACTION_MOVE
                           // Log.d(TAG, "onLongPress: real detected X: " + e.getX() + " Y: " + e.getY() + " event X: " + event.getX() + " Y: " + event.getY() + " raw Y" + event.getRawY());
                            //Toast.makeText(mContext, "Long Clicked ", Toast.LENGTH_SHORT).show();
                            updateTouchState = true;

                            final Bitmap originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);


                            //selectedVisionText.clear();
                            //selectedVisionTextRectangles.clear();
                            selectedVisionTextRectanglesSimplified.clear();
                            if(debugMode || allWordBorders) {
                                //Iterator it = visionTextRectangles.entrySet().iterator();

                                //selectedVisionTextRectangles.clear();
                                //selectedVisionText.clear();
                                for (VisionWordModel visionWordModel : visionTextRectanglesSimplified) {
                                //while (it.hasNext()) {
                                    //Map.Entry pair = (Map.Entry) it.next();
                                    Rect rect = visionWordModel.mrect;

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
                                    canvas.drawRect(rect, paint);
                                }
                                TouchImageView.super.setImageBitmap(originalBitmap);
                            }
                                //ivImage.invalidate();
                                // TODO: select the longpressed word
                                selectWordOnTouch(touchX, touchY, true);

                        }
                    }
                });
              //  Log.d(TAG, "zxc onTouch: " + event.getRawX() + " ; " + event.getX());
                gestureDetector.onTouchEvent(event);
               // Log.d(TAG, "zxc onTouch: " + event.getRawX() + " ; " + event.getX());
                if(!cancelZoomNav) {
                    try {
                        super.onTouch(view, event);
                    }catch (Exception e){
                        TouchImageView.super.setImageBitmap(unChangedOriginalBitmap);
                        e.printStackTrace();
                        return false;
                    }
                }
                return true; // indicate event was handled
            }
        };

        TouchImageView.super.setOnTouchListener(imageMatrixTouchHandler);
        //TouchImageView.super.setOnTouchListener(new ImageMatrixTouchHandler(mContext));
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            Set<Object> seen = ConcurrentHashMap.newKeySet();
            return t -> seen.add(keyExtractor.apply(t));
        }
        return null;
    }

    private boolean isWordBetweenCursors(VisionWordModel filteredVisionWord, PointF fstartCursorPoint, PointF fendCursorPoint, float innerMarginAreaLeft, float innerMarginAreaRight){
        Float startTop = fstartCursorPoint.y;
        Float endTop = fendCursorPoint.y;
        boolean status = filteredVisionWord.getLeft() >= innerMarginAreaLeft && filteredVisionWord.getRight() <= innerMarginAreaRight && filteredVisionWord.getBottom() >= startTop && filteredVisionWord.getTop() <= endTop;
        //Log.d(TAG, "isWordBetweenCursors: status "+status+ " mtext "+ filteredVisionWord.mtext + " startTop "+startTop + " getTop "+ filteredVisionWord.getTop()+ " getBottom "+ filteredVisionWord.getBottom()+" endTop "+endTop);
        return status;
    }

    private boolean isWordWithinVisibleArea(VisionWordModel filteredVisionWord){
        //Rect zoomedAreaRect = new Rect(zoomedOffsetX,zoomedOffsetY,zoomedOffsetX+(int)(loriginalImageWidth/widthZoomFactor),zoomedOffsetY+(int)(loriginalImageHeight/heightZoomFactor));
        Rect zoomedAreaRect = new Rect(0, zoomedOffsetY,(int)loriginalImageWidth,zoomedOffsetY+(int)(loriginalImageHeight/heightZoomFactor));
        return zoomedAreaRect.contains(filteredVisionWord.mrect);
    }

    private boolean isWordExactlyBetweenMarginsAndCursors(Rect rect, RectF innerMarginAreaRect, RectF innerCursorAreaRect, float startX, float startY, float endX, float endY) {
        boolean innerMarginAreaRectStatus = true;
        if(showMargins){
            innerMarginAreaRectStatus = innerMarginAreaRect.contains(rect.left, rect.top);
        }
        if(!innerCursorAreaRect.isEmpty()) {
            if (innerMarginAreaRectStatus && (((rect.top < innerCursorAreaRect.top && rect.top >= startY && (rect.right >= startX || rect.left >= startX)) || (rect.top > innerCursorAreaRect.top && rect.bottom <= endY && (rect.left <= endX || rect.right <= endX))) || innerCursorAreaRect.contains(rect.left, rect.top))) {
                return true;
            }
        }else{
            if (innerMarginAreaRectStatus && ((rect.left >= startX && rect.top >= startY && rect.top <= endY) && (rect.left <= endX && rect.bottom >= startY && rect.bottom <= endY))) {
                return true;
            }
        }
        return false;
    }



    private void selectWordInBetweenCursors(PointF mstartCursorPoint, PointF mendCursorPoint, PointF mleftMarginPoint, PointF mrightMarginPoint) {
        //Map<Rect, String> lselectedVisionTextRectangles = new HashMap<Rect, String>();
        boolean changeStartCursorPoint = true, changeEndCursorPoint = true, changeLeftMarginPoint = true, changeRightMarginPoint = true;
        textSelectionInProgress = true;
        //List<PointF> allWordStartPoints = new ArrayList<>();
        List<Integer> allWordTopPositions = new ArrayList<>();
        List<Integer> allWordBottomPositions = new ArrayList<>();
        //List<Float> allWordTopPositionsUnique = new ArrayList<>();
        //List<PointF> allWordEndPoints = new ArrayList<>();
        if(mstartCursorPoint == null){
            mstartCursorPoint = startCursorPoint;
            changeStartCursorPoint = false;
        }
        if(mendCursorPoint == null){
            changeEndCursorPoint = false;
            mendCursorPoint = endCursorPoint;
        }
        if(mleftMarginPoint == null){
            mleftMarginPoint = leftMarginRulerPoint;
            changeLeftMarginPoint = false;
        }
        if(mrightMarginPoint == null){
            changeRightMarginPoint = false;
            mrightMarginPoint = rightMarginRulerPoint;
        }
        int startX = (int)mstartCursorPoint.x;
        int startY = (int)mstartCursorPoint.y;
        int endX = (int)mendCursorPoint.x;
        int endY = (int)mendCursorPoint.y;
        //RectF cursorAreaRect = new RectF();
        /*cursorAreaRect.left = (int)((startX + zoomedOffsetX*widthZoomFactor)/widthZoomFactor);
        cursorAreaRect.top = (int)((startY + zoomedOffsetY*heightZoomFactor)/heightZoomFactor);
        cursorAreaRect.right = (int)((endX + zoomedOffsetX*widthZoomFactor)/widthZoomFactor);
        cursorAreaRect.bottom = (int)((endY + zoomedOffsetY*heightZoomFactor)/heightZoomFactor);*/
        /*cursorAreaRect.left = startX ;
        cursorAreaRect.top = startY;
        cursorAreaRect.right = endX;
        cursorAreaRect.bottom = endY;*/

        RectF innerMarginAreaRect = new RectF();
        innerMarginAreaRect.left = mleftMarginPoint.x ;
        innerMarginAreaRect.top = 0;
        innerMarginAreaRect.right = mrightMarginPoint.x;
        innerMarginAreaRect.bottom = loriginalImageHeight;
        //Log.d(TAG, "selectWordInBetweenCursors: cursorAreaRect "+cursorAreaRect.toShortString());

        //Bitmap originalBitmap;
        //originalBitmap = ((BitmapDrawable) TouchImageView.super.getDrawable()).getBitmap();
        final Bitmap originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);
        Canvas canvas = new Canvas(originalBitmap);
/*
        // Initialize a new Paint instance to draw the Rectangle
        Paint paint1 = new Paint();
        paint1.setStyle(Paint.Style.FILL_AND_STROKE);
        //paint1.setStrokeWidth(2);
        //paint1.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
        paint1.setColor(Color.argb(50, 255, 0, 0));
        paint1.setAntiAlias(true);



        canvas.drawBitmap(originalBitmap, 0, 0, paint1);
        canvas.drawRect(cursorAreaRect, paint1);*/


        /*List<VisionWordModel> filteredList
                = visionTextRectanglesSimplified.newArrayList(Collections2.filter(listArticles,
                new ArticleFilter("test")));*/
        final Float startTop = mstartCursorPoint.y;
        final Float endTop = mendCursorPoint.y;
        final PointF fstartCursorPoint = mstartCursorPoint;
        final PointF fendCursorPoint = mendCursorPoint;
        List<VisionWordModel> filteredVisionWordList = visionTextRectanglesSimplified;
        if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            if(zoomedVisionTextRectangles.size() <= 0 && visionTextRectanglesSimplified.size() >= 1){
                zoomedVisionTextRectangles = visionTextRectanglesSimplified;
            }
            filteredVisionWordList = zoomedVisionTextRectangles.stream().filter(filteredVisionWord -> (isWordBetweenCursors(filteredVisionWord, fstartCursorPoint, fendCursorPoint, innerMarginAreaRect.left, innerMarginAreaRect.right))).collect(Collectors.toList());
        }
        if(allWordBorders){
            // draw Yellow Word border
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
            paint.setColor(Color.YELLOW);
            paint.setAntiAlias(true);
            for (VisionWordModel visionWordModel : zoomedVisionTextRectangles) {
                Rect rect = visionWordModel.mrect;
                canvas.drawRect(rect, paint);
            }
        }
        //Iterator it = visionTextRectangles.entrySet().iterator();

        //selectedVisionTextRectangles.clear();
        //selectedVisionText.clear();
        // TODO: get selected text, top word line & bottom word line for innerCursorAreaRect
        List<VisionWordModel> filteredVisionWordListUnique = new ArrayList<>();
        RectF innerCursorAreaRect = new RectF(0,0,0,0);
        /*if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            filteredVisionWordListUnique = filteredVisionWordList.stream()
                    .distinct()
                    .filter(distinctByKey(VisionWordModel::getTop))
                    .collect(Collectors.toList());
            for (VisionWordModel visionWordModel1 : filteredVisionWordListUnique) {
                Log.d(TAG, endY+" selectWordInBetweenCursors: visionWordModel1 mtext "+visionWordModel1.mtext+"mrect "+visionWordModel1.mrect.toShortString());
            }
            if(filteredVisionWordListUnique.size() >= 2) {
                innerCursorAreaRect.left = 0;
                innerCursorAreaRect.right = loriginalImageWidth;
                if(filteredVisionWordListUnique.size() == 2){
                    innerCursorAreaRect.top = filteredVisionWordListUnique.get(0).getBottom();
                    innerCursorAreaRect.bottom = filteredVisionWordListUnique.get(1).getTop();
                }else {
                    innerCursorAreaRect.top = filteredVisionWordListUnique.get(1).getTop();
                    innerCursorAreaRect.bottom = filteredVisionWordListUnique.get(filteredVisionWordListUnique.size() - 2).getBottom();
                }
                Log.d(TAG, "zfew selectWordInBetweenCursors: innerCursorAreaRect "+innerCursorAreaRect.toShortString()+" startY "+startY + " endY "+endY+" filteredVisionWordListUnique.get(0).getTop() "+filteredVisionWordListUnique.get(0).getTop()+" filteredVisionWordListUnique.get(filteredVisionWordListUnique.size() - 1).getBottom() "+filteredVisionWordListUnique.get(filteredVisionWordListUnique.size() - 1).getBottom());
            }else if(filteredVisionWordListUnique.size() == 1) {
                //innerCursorAreaRect = new RectF(startX,startY,endX,endY);
                //Log.d(TAG, "zfew selectWordInBetweenCursors: filteredVisionWordListUnique.size()"+filteredVisionWordListUnique.size());
            }
        }else {*/
            for (VisionWordModel visionWordModel : filteredVisionWordList) {
                Rect rect = visionWordModel.mrect;
                if (rect.top >= startY && rect.bottom <= endY) {
                    // Sometimes OCR detects word in same line as different rect.bottom +/- 2
                    if (!checkSameLineWords(allWordTopPositions, rect.top)) {
                        allWordTopPositions.add(rect.top);
                    }
                    if (!checkSameLineWords(allWordBottomPositions, rect.bottom)) {
                       // Log.d(TAG, "selectWordInBetweenCursors: rect.bottom "+Float.valueOf(rect.bottom)+" allWordBottomPositions "+allWordBottomPositions.toString());
                        allWordBottomPositions.add(rect.bottom);
                    }
                }
            }
            Collections.sort(allWordTopPositions, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Integer.compare(o1, o2);
                }
            });
            Collections.sort(allWordBottomPositions, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Integer.compare(o1, o2);
                }
            });
            Collections.reverse(allWordBottomPositions);
       // Log.d(TAG, "aweds selectWordInBetweenCursors: allWordBottomPositions"+allWordBottomPositions.toString());
            if(allWordTopPositions.size() >= 2 && allWordBottomPositions.size() >= 2) {
                innerCursorAreaRect.left = 0;
                innerCursorAreaRect.right = loriginalImageWidth;
                if(allWordTopPositions.size() == 2){
                    innerCursorAreaRect.top = allWordBottomPositions.get(1);
                    innerCursorAreaRect.bottom = allWordTopPositions.get(1);
                }else {
                    innerCursorAreaRect.top = allWordTopPositions.get(1);
                    innerCursorAreaRect.bottom = allWordBottomPositions.get(1);
                }
            }else{
              //  Log.d(TAG, "zfew selectWordInBetweenCursors: innerCursorAreaRect "+innerCursorAreaRect.toShortString());
              //  Log.d(TAG, "zfew selectWordInBetweenCursors: allWordTopPositions.size()  "+allWordTopPositions.size() +" allWordBottomPositions.size() "+allWordBottomPositions.size() );
            }
        //Log.d(TAG, "aweds selectWordInBetweenCursors: innerCursorAreaRect "+innerCursorAreaRect.toShortString());
        // }
        selectedVisionTextRectanglesSimplified.clear();
        for (VisionWordModel visionWordModel : filteredVisionWordList) {
            //while (it.hasNext()) {
            //Map.Entry pair = (Map.Entry) it.next();
            //Rect rect = (Rect) pair.getKey();
            Rect rect = visionWordModel.mrect;
            //Log.d(TAG, "cfde selectWordInBetweenCursors: mtext "+visionWordModel.mtext);
            //Log.d(TAG, "selectWordInBetweenCursors: word "+pair.getValue().toString()+" rect "+rect.toShortString());
            // Get second top most word position for innerCursorAreaRect

            //Log.d(TAG, "selectWordInBetweenCursors: size allWordStartPoints "+allWordStartPoints.size());
            //Set<Float> allWordTopPositionsUniqueSet = new HashSet<>(allWordTopPositions);
            //allWordTopPositionsUnique.addAll(allWordTopPositionsUniqueSet);
            //Log.d(TAG, "vbg selectWordInBetweenCursors: size allWordTopPositionsUnique "+allWordTopPositions.size()+" all: "+allWordTopPositions.toString());

            // Get top word of second line from top & bottom word of second line from bottom

            /*if(filteredVisionWordListUnique.size() >= 2 && filteredVisionWordListUnique.size() >= 2) {
                innerCursorAreaRect.left = 0;
                innerCursorAreaRect.top = filteredVisionWordListUnique.get(1).getTop();
                innerCursorAreaRect.right = loriginalImageWidth;
                innerCursorAreaRect.bottom = filteredVisionWordListUnique.get(filteredVisionWordListUnique.size() - 2).getBottom();
            }else{
                //innerCursorAreaRect = cursorAreaRect;
            }*/
            /*
            words to the right of startX: rect.top < innerCursorAreaRect.top && rect.top >= startY && rect.left >= startX
            words to the left of endX: rect.top > innerCursorAreaRect.top && rect.left <= endX
             */
            if (isWordExactlyBetweenMarginsAndCursors(rect, innerMarginAreaRect, innerCursorAreaRect, startX, startY, endX, endY)){
                // Add red
                //if(!selectedVisionTextRectangles.contains(rect)) {
                    //Log.d(TAG, "red selectWordInBetweenCursors: pair.getValue().toString() " + pair.getValue().toString() + " rect " + rect.toShortString());
                    //selectedVisionTextRectangles.add(rect);

                    //selectedVisionText.add(pair.getValue().toString());
                    //lselectedVisionTextRectangles.put(rect, visionWordModel.mtext);


                    // Initialize a new Paint instance to draw the Rectangle
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                    paint.setColor(Color.RED);
                    paint.setAntiAlias(true);

                    //canvas.drawBitmap(originalBitmap, 0, 0, paint);
                    //canvas.drawText("Testing...", 10, 10, paint);
                    canvas.drawRect(rect, paint);
                selectedVisionTextRectanglesSimplified.add(visionWordModel);
               // }
            }
        }
        //List<Rect> justRect = new ArrayList<>(lselectedVisionTextRectangles.keySet());
        /*Collections.sort(selectedVisionTextRectanglesSimplified, new Comparator<VisionWordModel>() {
            @Override
            public int compare(VisionWordModel p1, VisionWordModel p2) {
                // Ascending
                int c = Double.compare(p1.mrect.top, p2.mrect.top);
                if (c == 0)
                    c = Double.compare(p1.mrect.left, p2.mrect.left);
                return c;
            }
        });*/
        List<String> selectedVisionText = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            selectedVisionText =
                    selectedVisionTextRectanglesSimplified.stream()
                            .map(VisionWordModel::getText)
                            .collect(Collectors.toList());
        }else{
            for (VisionWordModel visionWordModel : selectedVisionTextRectanglesSimplified) {
                selectedVisionText.add(visionWordModel.mtext);
            }
        }
        /*for (Rect rect : justRect) {
            selectedVisionText.add(lselectedVisionTextRectangles.get(rect));
        }*/
        /*Log.d(TAG, "before srt selectWordInBetweenCursors: "+allWordStartPoints.toString());
        Collections.sort(allWordStartPoints, new Comparator<PointF>() {
            @Override
            public int compare(PointF p1, PointF p2) {
                // Ascending
                int c = Double.compare(p1.y, p2.y);
                if (c == 0)
                    c = Double.compare(p1.x, p2.x);
                return c;
            }
        });
        Collections.sort(allWordEndPoints, new Comparator<PointF>() {
            @Override
            public int compare(PointF p1, PointF p2) {
                // Ascending
                int c = Double.compare(p1.y, p2.y);
                if (c == 0)
                    c = Double.compare(p1.x, p2.x);
                return c;
            }
        });
        Collections.reverse(allWordEndPoints);*/
        //
        //Log.d(TAG, "after srt selectWordInBetweenCursors: "+allWordStartPoints.toString());
        // Get top left most
        PointF firstTopLeftWordPoint = null;
        if(selectedVisionTextRectanglesSimplified.size() >= 1) {
            firstTopLeftWordPoint = new PointF(selectedVisionTextRectanglesSimplified.get(0).mrect.left, selectedVisionTextRectanglesSimplified.get(0).mrect.top);
        }
        PointF lastBottomRightWordPoint = null;
        if(selectedVisionTextRectanglesSimplified.size() >= 1) {
            int count = selectedVisionTextRectanglesSimplified.size();
            lastBottomRightWordPoint = new PointF(selectedVisionTextRectanglesSimplified.get(count-1).mrect.right, selectedVisionTextRectanglesSimplified.get(count-1).mrect.bottom);
        }
        //
        if(changeStartCursorPoint){
            // get top left most point of the top left most word in image
            //Log.d(TAG, "selectWordInBetweenCursors: changeStartCursorPoint");
            //if(allWordStartPoints.size() >= 1) {
            if(snapToWord && (firstTopLeftWordPoint != null)){
                startCursorPoint = firstTopLeftWordPoint;
                //startCursorPoint = allWordStartPoints.get(0);
            }else {
                startCursorPoint = mstartCursorPoint;
            }
        }
        if(changeEndCursorPoint){
            // get bottom right most point of the bottom right most word in image

            //Log.d(TAG, "selectWordInBetweenCursors: changeendCursorPoint");
            if(snapToWord && (lastBottomRightWordPoint != null)){
                endCursorPoint = lastBottomRightWordPoint;
            }else {
                endCursorPoint = mendCursorPoint;
            }
        }
        if(changeLeftMarginPoint){
            // get top left most point of the top left most word in image
            //leftMarginRulerPoint.x = allWordStartPoints.get(0).x;
            leftMarginRulerPoint.x = mleftMarginPoint.x;
        }
        if(changeRightMarginPoint){
            // get bottom right most point of the bottom right most word in image
            //rightMarginRulerPoint.x = allWordEndPoints.get(0).x;
            rightMarginRulerPoint.x = mrightMarginPoint.x;
        }
        Paint paint = new Paint();
        if(debugMode) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
            paint.setColor(Color.GREEN);
            paint.setAntiAlias(true);
            //canvas.drawLine(innerCursorAreaRect.left, innerCursorAreaRect.top, innerCursorAreaRect.right, innerCursorAreaRect.top, paint);
            canvas.drawRect(innerCursorAreaRect, paint);
        }
        paintCursors(canvas);

        //Paint paint = new Paint();
        /*paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLUE);
        paint.setAntiAlias(true);
        canvas.drawCircle(startCursorPoint.x, startCursorPoint.y, 30, paint);
        canvas.drawCircle(endCursorPoint.x, endCursorPoint.y, 30, paint);
*/
        if(showMargins) {
            // Draw margin rulers
            paint.setColor(Color.GREEN);
            canvas.drawLine(leftMarginRulerPoint.x, 0, leftMarginRulerPoint.x, loriginalImageHeight, paint);
            canvas.drawCircle(leftMarginRulerPoint.x, loriginalImageHeight / 2, 30, paint);
            canvas.drawLine(rightMarginRulerPoint.x, 0, rightMarginRulerPoint.x, loriginalImageHeight, paint);
            canvas.drawCircle(rightMarginRulerPoint.x, loriginalImageHeight / 2, 30, paint);
        }

        if(debugMode && (loriginalImageHeight/loriginalImageWidth) > 1.3) {
            // Draw Zoomed area
            // Zoomed area is depending on aspect ratio of the image and device - portrait or landscape
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
            paint.setColor(Color.GRAY);
            //Log.d(TAG, "agytr selectWordInBetweenCursors: zoomedOffsetY "+zoomedOffsetY+" heightZoomFactor "+heightZoomFactor+" loriginalImageWidth "+loriginalImageWidth + " loriginalImageHeight "+loriginalImageHeight+" loriginalImageWidth/loriginalImageWidth "+(loriginalImageWidth/loriginalImageHeight));
            //Log.d(TAG, "selectWordInBetweenCursors: (loriginalImageHeight/loriginalImageWidth) "+(loriginalImageHeight/loriginalImageWidth));
            Rect zoomedAreaRect = new Rect(zoomedOffsetX, zoomedOffsetY, zoomedOffsetX + (int) (loriginalImageWidth / widthZoomFactor), zoomedOffsetY + (int) (loriginalImageHeight / heightZoomFactor));
            //Rect zoomedAreaRect = new Rect(0, zoomedOffsetY,(int)loriginalImageWidth,zoomedOffsetY+(int)(loriginalImageHeight/heightZoomFactor));
            canvas.drawRect(zoomedAreaRect, paint);
        }

        TouchImageView.super.setImageBitmap(originalBitmap);
        TouchImageView.super.setContentDescription(TextUtils.join(" ", selectedVisionText));
        mListener.onEvent();
        textSelectionInProgress = false;
    }

    private void selectWordOnTouch(int touchX, int touchY, boolean longPressMode) {
        boolean foundWord = false;
        int zoomedTouchX = zoomedOffsetX+(int)(touchX/widthZoomFactor);
        int zoomedTouchY = zoomedOffsetY+(int)(touchY/heightZoomFactor);
        if(heightZoomFactor <= 1){
            zoomedTouchY = touchY - zoomedOffsetY;
        }
        //final float fzoomedTouchX = zoomedTouchX;
        final float fzoomedTouchY = zoomedTouchY;
        if(widthZoomFactor <= 1){
            zoomedTouchX = touchX - zoomedOffsetX;
        }
        /*Log.d(TAG, "qaz onLongPress: zoomedOffsetX "+zoomedOffsetX);
        Log.d(TAG, "qaz onLongPress: zoomedOffsetY "+zoomedOffsetY);
        Log.d(TAG, "mnop onLongPress: touchX "+touchX);
        Log.d(TAG, "mnop onLongPress: touchY "+touchY);
        Log.d(TAG, "mnop onLongPress: zoomedTouchX "+zoomedTouchX);
        Log.d(TAG, "mnop onLongPress: zoomedTouchY "+zoomedTouchY);

        //Log.d(TAG, "onLongPress: MotionEvent.ACTION_UP visionTextRectangles size "+visionTextRectangles.size());
        //Iterator it = visionTextRectangles.entrySet().iterator();
        Log.d(TAG, "wqw onLongPress: lscaledImageWidth "+lscaledImageWidth);
        Log.d(TAG, "wqw onLongPress: lscaledImageHeight "+lscaledImageHeight);*/

        final Bitmap originalBitmap;
        if(longPressMode){
            originalBitmap = unChangedOriginalBitmap.copy(unChangedOriginalBitmap.getConfig(), true);
        }else {
            originalBitmap = ((BitmapDrawable) TouchImageView.super.getDrawable()).getBitmap();
        }
        Canvas canvas = new Canvas(originalBitmap);
        List<VisionWordModel> filteredVisionWordList = visionTextRectanglesSimplified;
        if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            if(zoomedVisionTextRectangles.size() <= 0 && visionTextRectanglesSimplified.size() >= 1){
                zoomedVisionTextRectangles = visionTextRectanglesSimplified;
            }
            filteredVisionWordList = zoomedVisionTextRectangles.stream().filter(filteredVisionWord -> (filteredVisionWord.getTop() >= fzoomedTouchY - filteredVisionWord.getHeight() && filteredVisionWord.getTop() <= fzoomedTouchY + filteredVisionWord.getHeight() )).collect(Collectors.toList());
        }
        //Log.d(TAG, "selectWordOnTouch: filteredVisionWordList size "+filteredVisionWordList.size() + " fzoomedTouchY "+ fzoomedTouchY);
        //Log.d(TAG, "mhgfer selectWordOnTouch: size "+filteredVisionWordList.size());
        for (VisionWordModel visionWordModel : filteredVisionWordList) {
        //while (it.hasNext()) {
            //Map.Entry pair = (Map.Entry)it.next();
            Rect rect = visionWordModel.mrect;
            // TODO: Threshold based on zoom factor
            int threshold = 5;
            if(checkRectangleMatch( rect,  zoomedTouchX,  zoomedTouchY)){
                foundWord = true;
                //   Log.d(TAG, "selectWordOnTouch: checkRectangleMatch");
                // }
                // if(rect.contains(zoomedTouchX,zoomedTouchY) || rect.contains(zoomedTouchX - threshold,zoomedTouchY - threshold) || rect.contains(zoomedTouchX + threshold,zoomedTouchY + threshold)){
                //Log.d(TAG, "qaz selectWordOnTouch: getLayoutParams "+mivStartCursor.getLayoutParams().toString());

                // Toggle
                if(selectedVisionTextRectanglesSimplified.contains(visionWordModel)){
                    Toast.makeText(mContext, " removed "+visionWordModel.mtext , Toast.LENGTH_SHORT).show();

                    // Remove - draw Yellow
                    //int pos = selectedVisionTextRectangles.indexOf(rect);
                    //Log.d(TAG, "already clicked rectangle: "+pair.getValue() + " rect: "+ rect.toShortString());
                    selectedVisionTextRectanglesSimplified.remove(visionWordModel);
                    //selectedVisionText.remove(pos);
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                    paint.setColor(Color.YELLOW);
                    paint.setAntiAlias(true);

                    //canvas.drawBitmap(originalBitmap, 0, 0, paint);
                    //canvas.drawText("Testing...", 10, 10, paint);
                    canvas.drawRect(rect, paint);
                }else {
                    Toast.makeText(mContext, " Added "+visionWordModel.mtext , Toast.LENGTH_SHORT).show();

                    // Add - Draw Red
                    //selectedVisionTextRectangles.add(rect);
                   // selectedVisionText.add(visionWordModel.mtext);
                selectedVisionTextRectanglesSimplified.add(visionWordModel);


                    // Initialize a new Paint instance to draw the Rectangle
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    paint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
                    paint.setColor(Color.RED);
                    paint.setAntiAlias(true);

                    //canvas.drawBitmap(originalBitmap, 0, 0, paint);
                    //canvas.drawText("Testing...", 10, 10, paint);
                    canvas.drawRect(rect, paint);

                    if(longPressMode) {

                        //canvas.drawCircle(rect.left, rect.bottom, 30, paint);
                        startCursorPoint.x = rect.left;
                        startCursorPoint.y = rect.top;
                        //canvas.drawCircle(rect.right, rect.bottom, 30, paint);
                        endCursorPoint.x = rect.right;
                        endCursorPoint.y = rect.bottom;
                        paintCursors(canvas);


                        leftMarginRulerPoint.x = 30;
                        leftMarginRulerPoint.y = loriginalImageHeight / 2;
                        rightMarginRulerPoint.x = loriginalImageWidth - 30;
                        rightMarginRulerPoint.y = loriginalImageHeight / 2;
                        if(showMargins) {
                            // Draw margin rulers
                            paint.setColor(Color.GREEN);
                            canvas.drawLine(leftMarginRulerPoint.x, 0, leftMarginRulerPoint.x, loriginalImageHeight, paint);
                            canvas.drawCircle(leftMarginRulerPoint.x, loriginalImageHeight / 2, 30, paint);
                            canvas.drawLine(rightMarginRulerPoint.x, 0, rightMarginRulerPoint.x, loriginalImageHeight, paint);
                            canvas.drawCircle(rightMarginRulerPoint.x, loriginalImageHeight / 2, 30, paint);
                        }

                    }
                }
                TouchImageView.super.setImageBitmap(originalBitmap);
                if(!longPressMode){
                    Collections.sort(selectedVisionTextRectanglesSimplified, new Comparator<VisionWordModel>() {
                        @Override
                        public int compare(VisionWordModel p1, VisionWordModel p2) {
                            // Ascending first left as same line words can have top +/- 5
                            int threshold = minWordHeight;
                            int c = 0;
                            if((p1.mrect.top < p2.mrect.top + threshold) && (p1.mrect.top < p2.mrect.top - threshold)){
                                c = -1;
                            }else if((p1.mrect.top > p2.mrect.top + threshold) && (p1.mrect.top > p2.mrect.top - threshold)){
                                c = 1;
                            }
                            if (c == 0)
                                c = Double.compare(p1.mrect.left, p2.mrect.left);
                            return c;
                        }
                    });
                }
                List<String> selectedVisionText = new ArrayList<>();
                if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                    selectedVisionText =
                            selectedVisionTextRectanglesSimplified.stream()
                                    .map(VisionWordModel::getText)
                                    .collect(Collectors.toList());
                }else{
                    for (VisionWordModel visionWordModelx : selectedVisionTextRectanglesSimplified) {
                        selectedVisionText.add(visionWordModelx.mtext);
                    }
                }
                //Toast.makeText(mContext, TextUtils.join(" ", selectedVisionText), Toast.LENGTH_SHORT).show();
                TouchImageView.super.setContentDescription(TextUtils.join(" ", selectedVisionText));
                //ivImage.invalidate();
                //et_image_text.setText(TextUtils.join(" ", selectedVisionText));
                //et_image_text.setSelectAllOnFocus(true);
                //et_image_text.setText(et_image_text.getText()+" "+pair.getValue().toString());
                mListener.onEvent();
                break;
            }else{
                //Log.d(TAG, pair.getValue()+" onLongPress: not match (int)touchX,(int)touchY) "+(int)touchX+" Y: "+(int)touchY+" rect: left "+rect.left+" top "+rect.top+" right "+rect.right+" bottom "+rect.bottom);
                //Log.d(TAG, pair.getValue()+" onLongPress: not match (int)zoomedTouchX,(int)zoomedTouchY) "+(int)zoomedTouchX+" Y: "+(int)zoomedTouchY+" rect: left "+rect.left+" top "+rect.top+" right "+rect.right+" bottom "+rect.bottom);
            }
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            //it.remove(); // avoids a ConcurrentModificationException
        }
        if(!foundWord && longPressMode){
            // Initialize a new Paint instance to draw the Rectangle
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.RED);
            paint.setAntiAlias(true);

            //canvas.drawCircle(touchX, touchY, 30, paint);
            startCursorPoint.x = touchX;
            startCursorPoint.y = touchY;
            //canvas.drawCircle(touchX + 100, touchY, 30, paint);
            endCursorPoint.x = touchX + 100;
            endCursorPoint.y = touchY;
            paintCursors(canvas);
        }
        //Log.d(TAG, "onLongPress: visionTextRectangles size "+visionTextRectangles.size());
    }

    private void paintCursors(Canvas canvas){
        int threshold = 30;
        int radius = 20;
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.BLUE);
        paint.setAntiAlias(true);

        PointF point1_draw = new PointF(startCursorPoint.x, startCursorPoint.y);
        PointF point2_draw = new PointF(startCursorPoint.x -threshold, startCursorPoint.y);
        PointF point3_draw = new PointF(startCursorPoint.x, startCursorPoint.y-threshold);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(point1_draw.x,point1_draw.y);
        path.lineTo(point2_draw.x,point2_draw.y);
        path.lineTo(point3_draw.x,point3_draw.y);
        path.lineTo(point1_draw.x,point1_draw.y);
        path.close();

        canvas.drawPath(path, paint);

        point1_draw = new PointF(endCursorPoint.x, endCursorPoint.y);
        point2_draw = new PointF(endCursorPoint.x +threshold, endCursorPoint.y);
        point3_draw = new PointF(endCursorPoint.x, endCursorPoint.y+threshold);

        path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(point1_draw.x,point1_draw.y);
        path.lineTo(point2_draw.x,point2_draw.y);
        path.lineTo(point3_draw.x,point3_draw.y);
        path.lineTo(point1_draw.x,point1_draw.y);
        path.close();

        canvas.drawPath(path, paint);
        canvas.drawCircle(startCursorPoint.x - radius, startCursorPoint.y - radius, radius, paint);
        canvas.drawCircle(endCursorPoint.x +radius, endCursorPoint.y+radius, radius, paint);
    }

    private boolean checkSameLineWords(List<Integer> allWordTopPositions, int top) {
        // allWordBottomPositions[283, 281,
        int threshold = minWordHeight;
        for (int i=0; i<=threshold; i++) {
            if (allWordTopPositions.contains(top+i)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkRectangleMatch(Rect rect, int zoomedTouchX, int zoomedTouchY) {
        int threshold = 5;
        int[] possibleListX = new int[] {zoomedTouchX, zoomedTouchX - threshold, zoomedTouchX + threshold};
        int[] possibleListY = new int[] {zoomedTouchY, zoomedTouchY - threshold, zoomedTouchY + threshold};
        for (int i=0; i<possibleListX.length; i++) {
            for (int j=0; j<possibleListY.length; j++) {
                if (rect.contains(possibleListX[i], possibleListY[j])) {
                    //Log.d(TAG, "selectWordOnTouch: rect.contains(zoomedTouchX,zoomedTouchY) i "+ i +" j "+j);
                    return true;
                }
            }
        }
        return false;
    }

}