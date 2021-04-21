package com.technikh.imagetextgrabber.models;

/*
 * Copyright (c) 2019. Nikhil Dubbaka from TechNikh.com under GNU AFFERO GENERAL PUBLIC LICENSE
 * Copyright and license notices must be preserved.
 * When a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.
 */

import android.graphics.Rect;

public class VisionWordModel {
    public String mtext;
    public Rect mrect;

    public VisionWordModel(Rect rect, String text) {
        this.mtext = text;
        this.mrect = rect;
    }

    public int getLeft() {
        return this.mrect.left;

    }

    public int getRight() {
        return this.mrect.right;

    }

    public int getTop() {
        return this.mrect.top;
    }

    public int getBottom() {
        return this.mrect.bottom;
    }

    public String getText() {
        return this.mtext;
    }

    public int getHeight() {
        return this.mrect.height();
    }
}