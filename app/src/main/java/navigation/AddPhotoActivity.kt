package navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.jinstargram.R
import com.example.jinstargram.databinding.ActivityAddPhotoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import model.ContentDTO
import java.text.SimpleDateFormat
import java.util.*

private lateinit var bd2 : ActivityAddPhotoBinding

class AddPhotoActivity : AppCompatActivity() {
    private var PICK_IMAGE_FROM_ALBUM = 0
    private var storage : FirebaseStorage? = null
    private var photoUri : Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        bd2 = ActivityAddPhotoBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(bd2.root)


        //초기화
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        //앨범 열기
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)

        //이미지 추가 업로드
        bd2.addphotoBtnUpload.setOnClickListener {
            contentUpload()
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                //이미지가 선택될 때
                photoUri = data?.data
                bd2.addphotoImage.setImageURI(photoUri)
            }else{
                finish()
            }
        }
    }
     private fun contentUpload() {
        //파일이름 만들기
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
        var uploadTask = storageRef?.putFile(photoUri!!)
         //파일 업로드
         uploadTask?.addOnSuccessListener {
             //업로드 성공시
             storageRef?.downloadUrl?.addOnSuccessListener {
                var contentDTO = ContentDTO()

                 //이미지 다운로드
                 contentDTO.imageUrl = it.toString()
                 //user uid
                 contentDTO.uid = auth?.currentUser?.uid

                 //user id
                 contentDTO.userId = auth?.currentUser?.email
                 //explain
                 contentDTO.explain = bd2.addphothEditExplain.text.toString()
                 //timestamp
                 contentDTO.timestamp = System.currentTimeMillis()

                 firestore?.collection("images")?.document()?.set(contentDTO)

                 setResult(Activity.RESULT_OK)

                 finish()

             }
         }
         uploadTask?.addOnFailureListener {
             //업로드 실패시
             Log.d("jinnnnnnnnnnn", it.toString())
         }
    }
}