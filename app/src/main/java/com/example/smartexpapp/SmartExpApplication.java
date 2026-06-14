package com.example.smartexpapp;

import android.app.Application;

public class SmartExpApplication extends Application {
    public AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }
}
