package com.android.ranit.smartthermostat.model;

/**
 * Created by: Ranit Raj Ganguly on 21/06/2021
 *
 * Singleton Class used to cache and access non-persistent data throughout
 * the application lifecycle.
 */
public class DataManager {
    private static final String TAG = DataManager.class.getSimpleName();

    // Private instance variable
    private static DataManager INSTANCE;

    // Private Constructor
    private DataManager() {}

    // Public method to get instance of Singleton class
    public DataManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DataManager();
        }
        return INSTANCE;
    }

}
