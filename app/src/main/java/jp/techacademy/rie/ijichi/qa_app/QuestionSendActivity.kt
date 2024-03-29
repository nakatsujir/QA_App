package jp.techacademy.rie.ijichi.qa_app

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_question_send.*
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayInputStream
import android.util.Base64
import android.util.Log
import android.util.Log.d
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.*
import kotlin.math.log

class QuestionSendActivity : AppCompatActivity(), DatabaseReference.CompletionListener {

    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    private var mGenre: Int = 0
    private var mPictureUri: Uri? = null

    //渡ってきたジャンルの番号を保持。UIの準備。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // 渡ってきたジャンルの番号を保持する
        val extras = intent.extras
        mGenre = extras.getInt("genre")

        // UIの準備
        title = "質問作成"

        sendButton.setOnClickListener{
            onClickSndButton(it)
        }

        imageView.setOnClickListener{
            onClickImageView()
        }
    }

    //許可を求めるダイアログからの結果を受け取る。
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したとき
                    showChooser()
                }
                return
            }
        }
    }

    //Intent連携で取得した画像をリサイズしてImageViewに設定。
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("AAA","onActivityResult")
        if (requestCode == CHOOSER_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null

                }
                return
            }
            // 画像を取得
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            // URIからBitmapを取得する
            val image: Bitmap
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            } catch (e: Exception) {
                return
            }

            // 取得したBimapの長辺を500ピクセルにリサイズする
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight) //(1)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizeImage = Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

            // BitmapをImageViewに設定する
            imageView.setImageBitmap(resizeImage)

            mPictureUri = null
        }
    }

    //Firebaseへの保存完了時に呼ばれる。
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(findViewById(R.id.content), "投稿に失敗しました", Snackbar.LENGTH_LONG).show()
        }
    }

    //Intent連携の選択ダイアログを表示する。
    private fun showChooser() {
        // ギャラリーから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // カメラで撮影するIntent
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val value = ContentValues()
        value.put(MediaStore.Images.Media.TITLE, filename)
        value.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        mPictureUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        val chooserIntent = Intent.createChooser(galleryIntent, "画像を取得")

        // EXTRA_INITIAL_INTENTS にカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }

    private fun onClickImageView(){
        // パーミッションの許可状態を確認する
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                showChooser()
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)

                return
            }
        } else {
            showChooser()
        }
    }

    private fun onClickSndButton(v: View){
        // キーボードが出てたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        val genreRef = dataBaseReference.child(ContentsPATH).child(mGenre.toString())

        val data = HashMap<String, String>()

        // UID
        data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

        // タイトルと本文を取得する
        val title = titleText.text.toString()
        val body = bodyText.text.toString()

        if (title.isEmpty()) {
            // タイトルが入力されていない時はエラーを表示するだけ
            Snackbar.make(v, "タイトル入力してください", Snackbar.LENGTH_LONG).show()
            return
        }
        if (body.isEmpty()) {
            // 質問が入力されていない時はエラーを表示するだけ
            Snackbar.make(v, "質問を入力してください", Snackbar.LENGTH_LONG).show()
            return
        }

        // Preferenceから名前を取る
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")

        data["title"] = title
        data["body"] = body
        data["name"] = name

        // 添付画像を取得する
        val drawable = imageView.drawable as? BitmapDrawable

        // 添付画像が設定されていれば画像を取り出してBASE64エンコードする
        if (drawable != null) {
            val bitmap = drawable.bitmap
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            data["image"] = bitmapString
        }

        genreRef.push().setValue(data, this)
        progressBar.visibility = View.VISIBLE
    }

}
