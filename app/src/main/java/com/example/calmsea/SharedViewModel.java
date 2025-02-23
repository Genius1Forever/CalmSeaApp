package com.example.calmsea;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Boolean> dataUpdated = new MutableLiveData<>(false);

    public LiveData<Boolean> getDataUpdated() {
        return dataUpdated;
    }

    public void setDataUpdated(boolean updated) {
        dataUpdated.setValue(updated);
    }
}


