package dev.nermingules.qrbarkodapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class MyViewModel : ViewModel() {
    val myData = MutableLiveData<String>()
    val myFisTur = MutableLiveData<String>()
    val myVCKN = MutableLiveData<String>()
}