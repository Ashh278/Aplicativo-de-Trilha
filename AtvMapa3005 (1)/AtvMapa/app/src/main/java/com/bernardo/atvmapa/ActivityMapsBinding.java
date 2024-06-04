package com.bernardo.atvmapa;

import android.view.LayoutInflater;

public class ActivityMapsBinding {
    private final int layoutId;

    private ActivityMapsBinding(int layoutId) {
        this.layoutId = layoutId;
    }

    public static ActivityMapsBinding inflate(LayoutInflater layoutInflater, int layoutId) {
        return new ActivityMapsBinding(layoutId);
    }

    public int getRoot() {
        return layoutId;
    }
}
