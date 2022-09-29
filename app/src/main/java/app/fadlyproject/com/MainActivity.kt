package app.fadlyproject.com

import android.Manifest.permission.*
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.github.drjacky.imagepicker.ImagePicker
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*


class MainActivity : AppCompatActivity(), UploadRequestBody.UploadCallback {

    private var selectedImageUri: Uri? = null
    private var camera: Button? = null
    private var image: ImageView? = null
    private var delete: ImageButton? = null
    private var layout: ScrollView? = null
    private var result: TextView? = null
    private var upload: Button? = null
    private var gallery: Button? = null
    private var permissionsToRequest: ArrayList<String>? = null
    private val permissionsRejected: ArrayList<String> = ArrayList()
    private val permissions: ArrayList<String> = ArrayList()
    private val ALL_PERMISSIONS_RESULT = 107
    private val IMAGE_RESULT = 200
    var mBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val window = this.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_200)

        image = findViewById(R.id.image_view)
        camera = findViewById(R.id.camera)
        delete = findViewById(R.id.delete)
        result = findViewById(R.id.hasil)
        upload = findViewById(R.id.upload)
        layout = findViewById(R.id.layout_root)
        gallery = findViewById(R.id.gallery)

        camera!!.setOnClickListener {
            openImageChooser()
        }

        delete!!.setOnClickListener{
            selectedImageUri = null
            image!!.setImageResource(R.drawable.ic_leafbg)
            result!!.text = "-"
        }

        upload!!.setOnClickListener {
            uploadImage()
        }

        gallery!!.setOnClickListener {
            startActivityForResult(getPickImageChooserIntent(), IMAGE_RESULT);
        }

        askPermissions()
    }

    val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data!!
                selectedImageUri = uri
                image!!.setImageURI(selectedImageUri)
            } else parseError(it)
        }

    private fun openImageChooser() {
        cameraLauncher.launch(
            ImagePicker.with(this)
                .cameraOnly()
                .createIntent()
        )
    }

    private fun parseError(activityResult: ActivityResult) {
        if (activityResult.resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(activityResult.data), Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_PICK_IMAGE -> {
                    selectedImageUri = data?.data
                    image!!.setImageURI(selectedImageUri)
                }
            }
        }

        if (resultCode === RESULT_OK) {
            if (requestCode === IMAGE_RESULT) {
                val filePath: String = getImageFilePath(data)!!
                if (filePath != null) {
                    mBitmap = BitmapFactory.decodeFile(filePath)
                    image!!.setImageBitmap(mBitmap)
                }
            }
        }
    }

    private fun uploadImage() {
        if(selectedImageUri == null)
        {
            if(mBitmap == null)
            {
                layout!!.snackbar("Select an Image First")
                return
            }
            else{
                try {
                    val filesDir = applicationContext.filesDir
                    val file = File(filesDir, "image" + ".png")
                    val bos = ByteArrayOutputStream()
                    mBitmap!!.compress(Bitmap.CompressFormat.PNG, 0, bos)
                    val bitmapdata: ByteArray = bos.toByteArray()
                    val fos = FileOutputStream(file)
                    fos.write(bitmapdata)
                    fos.flush()
                    fos.close()

                    val body = UploadRequestBody(file, "image", this)
                    MyAPI().uploadImage(
                        MultipartBody.Part.createFormData(
                            "file",
                            file.name,
                            body
                        ),
                        RequestBody.create(MediaType.parse("multipart/form-data"), "json")
                    ).enqueue(object : Callback<UploadResponse> {
                        override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                            layout!!.snackbar(t.message!!)
                        }

                        override fun onResponse(
                            call: Call<UploadResponse>,
                            response: Response<UploadResponse>
                        ) {
                            response.body()?.let {
                                layout!!.snackbar(it.message)
                                result!!.text = it.data.classes
                                layout!!.visibility = View.VISIBLE
                                Glide.with(this@MainActivity).load("http://167.172.72.26:1337/"+ it.data.image_after_preprocessing).into(
                                    image!!
                                )
                            }
                        }
                    })
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        else{
            val parcelFileDescriptor =
                contentResolver.openFileDescriptor(selectedImageUri!!, "r", null) ?: return

            val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
            val file = File(cacheDir, contentResolver.getFileName(selectedImageUri!!))
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)

            val body = UploadRequestBody(file, "image", this)
            MyAPI().uploadImage(
                MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    body
                ),
                RequestBody.create(MediaType.parse("multipart/form-data"), "json")
            ).enqueue(object : Callback<UploadResponse> {
                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    layout!!.snackbar(t.message!!)
                }

                override fun onResponse(
                    call: Call<UploadResponse>,
                    response: Response<UploadResponse>
                ) {
                    response.body()?.let {
                        layout!!.snackbar(it.message)
                        result!!.text = it.data.classes
                        layout!!.visibility = View.VISIBLE
                        Glide.with(this@MainActivity).load("http://167.172.72.26:1337/"+ it.data.image_after_preprocessing).into(
                            image!!
                        )
                    }
                }
            })
        }
    }

    override fun onProgressUpdate(percentage: Int) {
    }

    companion object {
        const val REQUEST_CODE_PICK_IMAGE = 101
    }

    private fun askPermissions() {
        permissions.add(CAMERA)
        permissions.add(WRITE_EXTERNAL_STORAGE)
        permissions.add(READ_EXTERNAL_STORAGE)
        permissionsToRequest = findUnAskedPermissions(permissions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest!!.size > 0) requestPermissions(
                permissionsToRequest!!.toTypedArray(),
                ALL_PERMISSIONS_RESULT
            )
        }
    }

    private fun findUnAskedPermissions(wanted: ArrayList<String>): ArrayList<String>? {
        val result = ArrayList<String>()
        for (perm in wanted) {
            if (!hasPermission(perm)) {
                result.add(perm)
            }
        }
        return result
    }

    private fun hasPermission(permission: String): Boolean {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }

    private fun canMakeSmores(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
    }

    fun getPickImageChooserIntent(): Intent? {
        val outputFileUri: Uri = getCaptureImageOutputUri()!!
        val allIntents: MutableList<Intent> = ArrayList()
        val packageManager = packageManager
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val listCam = packageManager.queryIntentActivities(captureIntent, 0)
        for (res in listCam) {
            val intent = Intent(captureIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            }
            allIntents.add(intent)
        }
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        val listGallery = packageManager.queryIntentActivities(galleryIntent, 0)
        for (res in listGallery) {
            val intent = Intent(galleryIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            allIntents.add(intent)
        }
        var mainIntent = allIntents[allIntents.size - 1]
        for (intent in allIntents) {
            if (intent.component!!.className == "com.android.documentsui.DocumentsActivity") {
                mainIntent = intent
                break
            }
        }
        allIntents.remove(mainIntent)
        val chooserIntent = Intent.createChooser(mainIntent, "Select source")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray<Parcelable>())
        return chooserIntent
    }

    private fun getCaptureImageOutputUri(): Uri? {
        var outputFileUri: Uri? = null
        val getImage = getExternalFilesDir("")
        if (getImage != null) {
            outputFileUri = Uri.fromFile(File(getImage.path, "profile.png"))
        }
        return outputFileUri
    }

    private fun getImageFromFilePath(data: Intent?): String? {
        val isCamera = data == null || data.data == null
        return if (isCamera) getCaptureImageOutputUri()!!.path else getPathFromURI(data!!.data!!)
    }

    fun getImageFilePath(data: Intent?): String? {
        return getImageFromFilePath(data)
    }

    private fun getPathFromURI(contentUri: Uri): String? {
        val proj = arrayOf(MediaStore.Audio.Media.DATA)
        val cursor: Cursor? = contentResolver.query(contentUri, proj, null, null, null)
        val column_index: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("pic_uri", selectedImageUri)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedImageUri = savedInstanceState.getParcelable<Parcelable>("pic_uri") as Uri?
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            ALL_PERMISSIONS_RESULT -> {
                for (perms in permissionsToRequest!!) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms)
                    }
                }
                if (permissionsRejected.size > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected[0])) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                DialogInterface.OnClickListener { dialog, which ->
                                    requestPermissions(
                                        permissionsRejected.toTypedArray(),
                                        ALL_PERMISSIONS_RESULT
                                    )
                                })
                            return
                        }
                    }
                }
            }
        }
    }

    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}