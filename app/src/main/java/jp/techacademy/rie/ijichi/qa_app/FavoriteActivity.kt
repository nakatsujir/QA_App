package jp.techacademy.rie.ijichi.qa_app

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.app_bar_main.*

class FavoriteActivity : AppCompatActivity() {

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mListView: ListView
    private lateinit var mAdapter: QuestionsListAdapter
    private val user = FirebaseAuth.getInstance().currentUser
    private lateinit var mQuestion: Question
    private var mGenre = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        title = "お気に入り"

        fav()
    }

    private fun fav() {
        if (user != null) {
            //取得
            val favoriteRef = mDatabaseReference.child(FavoritePATH).child(user!!.uid)
            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*, *>?
                    mDatabaseReference.child(ContentsPATH).child(mGenre.toString()).child(mQuestion.questionUid)
                }

                override fun onCancelled(p0: DatabaseError) {
                }
            })
        }
    }
}
