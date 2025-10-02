package com.dema.documentsreadermlkit

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dema.documentsreadermlkit.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var imageUri : Uri

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        getTextByImage(imageUri)
    }

    private val contractGallery =
        registerForActivityResult(PickVisualMedia()) { uri ->
            uri?.let {
                getTextByImage(uri)
            }
        }

    private val tag = javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageUri = createImageURI()
    }

    private fun getTextByImage(uri: Uri?) {
        val thisContext = this@MainActivity

        if (uri != null) {
            var bitmap : Bitmap? = null
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            CoroutineScope(Dispatchers.Main).launch {
                val source = ImageDecoder.createSource(thisContext.contentResolver, uri)
                bitmap = ImageDecoder.decodeBitmap(source)
                binding.imgDoc.setImageBitmap(bitmap)
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(inputImage)
                    .addOnSuccessListener { docText ->
                        val resultText = docText.text
                        binding.txtImgDoc.text = resultText
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Error to load text from image", e)
                    }
            }
        } else {
            Toast.makeText(thisContext, "Nao foi possivel ter acesso a imagem", Toast.LENGTH_LONG).show()
        }
    }

    private fun createImageURI() : Uri {
        val image = File(filesDir, "camera_photo.png")
        return FileProvider.getUriForFile(this,
            "com.dallas.documentreadermlkit.FileProvider",
            image)
    }

    private fun takePhoto() {
        contract.launch(imageUri)
    }

    private fun takeFromGallery() {
        contractGallery.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    private fun showDialogChooseOptionGetImage() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_choose_option_get_image)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.setCancelable(true)

        val optionTakePhoto = dialog.findViewById<CardView>(R.id.option_take_photo)
        optionTakePhoto.setOnClickListener { takePhoto() }

        val optionTakeFromGallery = dialog.findViewById<CardView>(R.id.option_take_from_gallery)
        optionTakeFromGallery.setOnClickListener { takeFromGallery() }

        dialog.show()
    }

    override fun onStart() {
        super.onStart()

        binding.btnGetImage.setOnClickListener { showDialogChooseOptionGetImage() }
    }
}