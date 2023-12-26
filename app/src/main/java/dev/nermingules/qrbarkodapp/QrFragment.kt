package dev.nermingules.qrbarkodapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import dev.nermingules.qrbarkodapp.databinding.FragmentQrBinding
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import org.json.JSONObject



class QrFragment : Fragment() {

    private var _binding: FragmentQrBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var codeScanner: CodeScanner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        val view = binding.root

            val scannerView = binding.qrScanner
//Kamera izni
            activity?.let {
                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
                    if(ContextCompat.checkSelfPermission(it.applicationContext,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                        val permissionList = arrayOf(Manifest.permission.CAMERA)
                        requestPermissions(permissionList,1)
                    }
                }

                codeScanner = CodeScanner(it.applicationContext,scannerView)
                codeScanner.camera = CodeScanner.CAMERA_BACK
                codeScanner.formats = CodeScanner.ALL_FORMATS
                codeScanner.autoFocusMode = AutoFocusMode.SAFE
                codeScanner.isAutoFocusEnabled = true
                codeScanner.isFlashEnabled = false
                codeScanner.decodeCallback = DecodeCallback {

                    val qrCodeData = it.text // QR kodundan okunan veri

                     /*   val action = QrFragmentDirections.actionQrFragment2ToMainScreenFragment()
                        Navigation.findNavController(view).navigate(action) */

                    requireActivity().runOnUiThread{
                        Toast.makeText(requireActivity().applicationContext,"Sonuç :  ${it.text}", Toast.LENGTH_LONG).show()
                        binding.info.text = it.text
                        val myViewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
                        myViewModel.myData.value = qrCodeData
                    }
                }
                codeScanner.errorCallback = ErrorCallback {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireActivity().applicationContext,"Hata :  ${it.message}", Toast.LENGTH_LONG).show()
                        println(it.message)
                    }
                }
                scannerView.setOnClickListener {
                    codeScanner.startPreview()
                }

            }
        return view
    }
    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }
    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}