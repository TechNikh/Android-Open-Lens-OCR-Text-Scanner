package com.technikh.imagetextgrabber.models;

/*
 * Copyright (c) 2019. Nikhil Dubbaka from TechNikh.com under GNU AFFERO GENERAL PUBLIC LICENSE
 * Copyright and license notices must be preserved.
 * When a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.
 */

import android.util.Log;

import java.util.List;

public class ImageViewSettingsModel {
    private String TAG = "ImageViewSettingsModel";
    public Boolean allowZoom = false, showMargins = false, snapToWord = false, allWordBorders = false, toggleWordOnClick = false;
    String[] strings = { "Snap on Word", "Allow Zoom", "Show Margins", "All Word Borders", "Toggle Word onClick" };
    List<Integer> mSettings;

    public ImageViewSettingsModel() {
    }

    public void setSelectedItems(List<Integer> settings) {
        mSettings = settings;
        if(mSettings.contains(0)){
            snapToWord = true;
        }else{
            snapToWord = false;
        }
        if(mSettings.contains(1)){
            allowZoom = true;
        }else{
            allowZoom = false;
        }
        if(mSettings.contains(2)){
            showMargins = true;
        }else{
            showMargins = false;
        }
        if(mSettings.contains(3)){
            allWordBorders = true;
        }else{
            allWordBorders = false;
        }
        if(mSettings.contains(4)){
            toggleWordOnClick = true;
        }else{
            toggleWordOnClick = false;
        }
    }

    public boolean isSnaptoWordMode() {
        return this.snapToWord;
    }

    public boolean isZoomAllowed() {
        return this.allowZoom;
    }

    public boolean isMarginsEnabled() {
        return this.showMargins;
    }

    public boolean isAllWordBordersEnabled() {
        return this.allWordBorders;
    }

    public boolean isToggleWordEnabled() {
        return this.toggleWordOnClick;
    }

    public String[] getAllItems() {
        return this.strings;
    }

    public int[] getDefaultItems() {
        return new int[] {0, 1, 2};
    }

    public String getDefaultItemsString() {
        StringBuilder str = new StringBuilder();
        int[] list = getDefaultItems();
        for (int i = 0; i < list.length; i++) {
            str.append(list[i]).append(",");
        }
        return str.toString();
    }
}