package jp.techacademy.rie.ijichi.qa_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

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

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        //データが変わったことを伝えてリストを再描画
        mAdapter.notifyDataSetChanged()

        //ログインしていなければログイン画面に遷移させ、ログインしていれば後ほど作成する回答作成画面に遷移させる
        fab.setOnClickListener { view ->
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            } else {
                //ログインしていれば回答作成画面に遷移させる
                val intent = Intent(this,AnswerSendActivity::class.java)
                intent.putExtra("question",mQuestion)
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
}
