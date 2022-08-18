package dimitar.udemy.phonebook.android.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dimitar.udemy.phonebook.android.databinding.ItemPhoneOverviewBinding
import dimitar.udemy.phonebook.models.data.PhoneModel

class RecyclerViewAdapterOverview(
    private val list: List<PhoneModel>,
    private val context: Context
) : RecyclerView.Adapter<RecyclerViewAdapterOverview.MyViewHolder>(){
    class MyViewHolder(binding: ItemPhoneOverviewBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvPhone = binding.tvPhone
        val llContact = binding.llContact
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemPhoneOverviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position]

        holder.tvPhone.text = model.baseModel.number
        holder.llContact.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", model.baseModel.number, null)))
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}