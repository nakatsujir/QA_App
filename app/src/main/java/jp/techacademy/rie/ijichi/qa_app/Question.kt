package jp.techacademy.rie.ijichi.qa_app

import com.google.android.gms.common.config.GservicesValue.init
import java.io.Serializable

class Question(
    val title: String,
    val body: String,
    val name: String,
    val uid: String,
    val questionUid: String,
    val genre: Int,
    bytes: ByteArray,
    val answers: ArrayList<Answer>
) : Serializable {
    val imageBytes: ByteArray

    init {
        imageBytes = bytes.clone()
    }
}