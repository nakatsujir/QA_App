package jp.techacademy.rie.ijichi.qa_app

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_detail.view.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private var mGenre: Int = 0
    private val user = FirebaseAuth.getInstance().currentUser
    private val databaseReference = FirebaseDatabase.getInstance().reference

    private var favoriteFlg = false

    //データに追加・変化があった時に受け取るChildEventListener
    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, p1: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        }

        override fun onChildRemoved(p0: DataSnapshot) {
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }

        override fun onCancelled(p0: DatabaseError) {
        }
    }

    //渡ってきたQuestionクラスのインスタンスを保持し、タイトルを設定します。そして、ListViewの準備をします。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        // 渡ってきたジャンルの番号を保持する
        mGenre = extras.getInt("genre")

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        //データが変わったことを伝えてリストを再描画
        mAdapter.notifyDataSetChanged()

        if (user != null) {
            //取得
            val favoriteRef = databaseReference.child(FavoritePATH).child(user!!.uid).child(mQuestion.questionUid)
            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as Map<*, *>?
                    if (data != null) {
                        favorite_image.setImageResource(R.drawable.btn)
                    } else {
                        favorite_image.setImageResource(R.drawable.btn_pressed)
                    }
                }
                override fun onCancelled(snapshot: DatabaseError) {
                }
            })

            favorite_image.setOnClickListener {
                favoriteFlg = !favoriteFlg
                if (favoriteFlg) {
                    favoriteRegister()
                    favorite_image.setImageResource(R.drawable.btn)
                    Toast.makeText(this, "お気に入りに登録しました", Toast.LENGTH_SHORT).show()
                } else {
                    favoriteRef.removeValue()
                    favorite_image.setImageResource(R.drawable.btn_pressed)
                }
            }
        } else {
            // ログインしていなければログイン画面に遷移させる
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        fab.setOnClickListener { view ->
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            } else {
                //ログインしていれば回答作成画面に遷移させる
                val intent = Intent(this, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        //Firebaseへのリスナーの登録。回答作成画面から戻ってきた時にその回答を表示させるために登録しておきます
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef =
            dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
                .child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

    }

    private fun favoriteRegister() {
        //ファイヤーベースに登録
        val favoriteRef = databaseReference.child(FavoritePATH).child(user!!.uid).child(mQuestion.questionUid)
        val data = HashMap<String, Int>()
        data["genre"] = mQuestion.genre
        favoriteRef.setValue(data)
    }

}

