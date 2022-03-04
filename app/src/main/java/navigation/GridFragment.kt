package navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.jinstargram.R
import com.example.jinstargram.databinding.FragmentGridBinding
import com.example.jinstargram.databinding.FragmentUserBinding
import com.google.firebase.firestore.FirebaseFirestore
import model.ContentDTO

class GridFragment : Fragment(){
    var firestore : FirebaseFirestore? = null
    lateinit var bd : FragmentGridBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firestore = FirebaseFirestore.getInstance()
        bd = FragmentGridBinding.inflate(inflater,container,false)
        bd.gridfragmentRecyclerView?.adapter = UserFragmentRecyclerViewAdapter()
        bd.gridfragmentRecyclerView?.layoutManager = GridLayoutManager(activity, 3)
        return bd.root
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")
                ?.addSnapshotListener { value, error ->
                    if(value == null) return@addSnapshotListener

                    //get data
                    for(snapshot in value.documents)
                    {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }

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
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(
                RequestOptions().centerCrop()).into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}