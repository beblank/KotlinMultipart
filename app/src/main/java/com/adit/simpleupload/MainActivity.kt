package com.adit.simpleupload

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import android.content.Intent
import android.provider.MediaStore
import android.app.Activity
import android.util.Log
import okhttp3.*
import retrofit2.Call
import retrofit2.Response
import java.io.File
import android.net.Uri
import rebus.permissionutils.PermissionEnum
import rebus.permissionutils.PermissionManager


class MainActivity : AppCompatActivity() {

    val PICK_IMAGE = 100

    lateinit var service:Service

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        service = Retrofit.Builder().baseUrl("http://localhost:8081").client(client).build()
            .create(Service::class.java)

        btn_upload.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE)
        }

        PermissionManager.Builder()
            .permission(PermissionEnum.READ_EXTERNAL_STORAGE)
            .ask(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val selectedImage:Uri = data!!.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage, filePathColumn, null, null, null) ?: return
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val filePath = cursor.getString(columnIndex)
            cursor.close()

            val file = File(filePath)

            val reqFile = RequestBody.create(MediaType.parse("image/*"), file)
            val body = MultipartBody.Part.createFormData("uploadFile", file.name, reqFile)
            val name = RequestBody.create(MediaType.parse("text/plain"), "upload_test")


            val req = service.postImage(body, name)
            req.enqueue(object:retrofit2.Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("Upload error:", t.message)
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.v("Upload", "success")
                }

            })
        }
    }
}
