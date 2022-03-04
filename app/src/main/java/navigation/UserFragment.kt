package navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.bumptech.glide.request.RequestOptions
import com.example.jinstargram.LoginActivity
import com.example.jinstargram.MainActivity
import com.example.jinstargram.R
import com.example.jinstargram.databinding.ActivityMainBinding
import com.example.jinstargram.databinding.FragmentDetailBinding
import com.example.jinstargram.databinding.FragmentUserBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import model.AlarmDTO
import model.ContentDTO
import model.FollowDTO
import org.w3c.dom.Text

class UserFragment : Fragment(){
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? =null
    var currentUserUid : String? = null
    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }
    lateinit var bd : FragmentUserBinding
    lateinit var mainbd : ActivityMainBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        bd = FragmentUserBinding.inflate(inflater,container,false)
        mainbd = ActivityMainBinding.inflate(inflater,container,false)
       uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if(uid == currentUserUid){
            //my page
            bd.accountBtnFollowSignout.text = getString(R.string.signout)
            bd.accountBtnFollowSignout.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            //other user page
            bd.accountBtnFollowSignout.text = getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            mainbd.toolbarTvUsername.text = arguments?.getString("userId")

            mainbd.toolbarBtnBack.setOnClickListener {
                mainbd.bottomNavigation.selectedItemId = R.id.action_Home
            }
            mainbd.toolbarTitleImage.visibility = View.GONE
           mainbd.toolbarTvUsername.visibility = View.VISIBLE
            mainbd.toolbarBtnBack.visibility = View.VISIBLE
            bd.accountBtnFollowSignout?.setOnClickListener {
                requestFollow()
            }
        }

        bd.accountRecyclerView.adapter = UserFragmentRecyclerViewAdapter()
        bd.accountRecyclerView.layoutManager = GridLayoutManager(requireActivity(),3)
        bd.accountIvProfile.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return bd.root
    }
    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

    }
    private fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener
            var followDTO = value.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                bd.accountTvFollowingCount.text = followDTO?.followingCount?.toString()
            }else if (followDTO?.followerCount != null){
                bd.accountTvFollowerCount.text = followDTO?.followerCount?.toString()
                if (followDTO?.followers.containsKey(currentUserUid!!)){
                    bd.accountBtnFollowSignout.text = getString(R.string.follow_cancel)
                    bd.accountBtnFollowSignout.background?.setColorFilter(ContextCompat.getColor(requireActivity(),R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else{

                    if(uid != currentUserUid){
                        bd.accountBtnFollowSignout.text = getString(R.string.follow)
                        bd.accountBtnFollowSignout.background?.colorFilter = null
                    }
                }
            }
        }

    }
    private fun requestFollow(){
        //Save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction {
            var followDTO = it.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true

                it.set(tsDocFollowing,followDTO)
                return@runTransaction
            }
            if(followDTO.followings.containsKey(uid)){
                followDTO.followingCount = followDTO?.followingCount - 1
                followDTO?.followings?.remove(uid)
            }else{
                followDTO.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }
            it.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        //Save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction {
            var followDTO = it.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                it.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }
            if(followDTO!!.followers.containsKey(currentUserUid)){
                followDTO!!.followerCount = followDTO?.followerCount?.minus(1)!!
                followDTO?.followers?.remove(currentUserUid!!)
            }else{
                followDTO!!.followerCount = followDTO?.followerCount?.plus(1)!!
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            it.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }
    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener
            if(value.data != null){
                var url = value?.data!!["image"]
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(bd.accountIvProfile!!)
            }
        }
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.whereEqualTo("uid",uid)
                ?.addSnapshotListener { value, error ->
                if(value == null) return@addSnapshotListener

                //get data
                for(snapshot in value.documents)
                {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                    bd.accountTvPostCount.text  = contentDTOs.size.toString()
                    notifyDataSetChanged()
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }

}