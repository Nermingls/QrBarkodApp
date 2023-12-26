package dev.nermingules.qrbarkodapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.nermingules.qrbarkodapp.databinding.FragmentTaramaBinding
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer


typealias LumaListener = (luma: Double) -> Unit

class TaramaFragment : Fragment() {

    private var _binding: FragmentTaramaBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null


    private lateinit var cameraExecutor: ExecutorService


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaramaBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        binding.imageCaptureButton.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.goToHomePage.setOnClickListener {
            val action =TaramaFragmentDirections.actionTaramaFragmentToMainScreenFragment()
            Navigation.findNavController(view).navigate(action)
        }
    }

    private fun takePhoto() {
        activity?.let {
            // Get a stable reference of the modifiable image capture use case
            val imageCapture = imageCapture ?: return

            // Create time stamped name and MediaStore entry.
            val name =
                SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                }
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    it.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                ).build()

            // Set up image capture listener, which is triggered after photo has
            // been taken
            imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(it.applicationContext),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val msg = "Photo capture succeeded: ${output.savedUri}"
                        Toast.makeText(it.baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                        // Metin tanıma işlemini başlat
                        recognizeText(Uri.parse(output.savedUri.toString()))
                    }
                })
        }
    }


    private fun startCamera() {
        activity?.let {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(it.applicationContext)

            cameraProviderFuture.addListener({
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

                imageCapture = ImageCapture.Builder().build()

                val imageAnalyzer = ImageAnalysis.Builder().build().also {
                        it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                            Log.d(TAG, "Average luminosity: $luma")
                        })
                    }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture, imageAnalyzer
                    )

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(it.applicationContext))
        }

    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireActivity().baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && it.value == false) permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(
                requireActivity().baseContext, "Permission request denied", Toast.LENGTH_SHORT
            ).show()
        } else {
            startCamera()
        }
    }
    private fun recognizeText(imageUri: Uri) {
        val inputImage = InputImage.fromFilePath(requireContext(), imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { text ->
                // Metin tanıma işlemi başarılı olduğunda burada işlem yapabilirsiniz
                val lines = textToLines(text)
                val sortedLines = sortLinesByCoordinates(lines)
                val mergedText = mergeLines(sortedLines)

                binding.info.text = mergedText // Sıralı metni göster
                val myViewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
                myViewModel.myData.value = mergedText // ViewModel'a da aktarabilirsiniz
            }
            .addOnFailureListener { e ->
                // Metin tanıma işlemi sırasında hata oluştuğunda burada işlem yapabilirsiniz
                Log.e(TAG, "Text recognition failed: $e")
            }
    }

    private fun textToLines(text: Text): List<Text.Line> {
        val lines = mutableListOf<Text.Line>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                lines.add(line)
            }
        }
        return lines
    }

    private fun sortLinesByCoordinates(lines: List<Text.Line>): List<Text.Line> {
        return lines.sortedBy { it.boundingBox?.top }
    }

    private fun mergeLines(lines: List<Text.Line>): String {
        val sortedLines = sortLinesByCoordinates(lines)
        val mergedText = StringBuilder()
        for (line in sortedLines) {
            mergedText.append(line.text).append("\n") // Satır sonlarına "\n" ekleme
        }
        return mergedText.toString()
    }

}