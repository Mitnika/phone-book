package dimitar.udemy.phonebook.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dimitar.udemy.phonebook.android.databinding.ItemPhoneBinding
import dimitar.udemy.phonebook.models.base.BasePhoneModel

class RecyclerViewAdapterAdd(
    private val list: ArrayList<BasePhoneModel>
    ) : ListAdapter<BasePhoneModel, RecyclerViewAdapterAdd.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BasePhoneModel>() {
            override fun areItemsTheSame(
                oldItem : BasePhoneModel,
                newItem : BasePhoneModel
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem : BasePhoneModel,
                newItem : BasePhoneModel
            ): Boolean {
                return oldItem.number == newItem.number
            }

        }
    }

    class MyViewHolder(binding: ItemPhoneBinding) : RecyclerView.ViewHolder(binding.root) {
        val etPhoneNumber   = binding.etPhoneNumber
        val ibEdit          = binding.ibDoneEditing
        val ibDelete        = binding.ibDelete
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemPhoneBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = getItem(position)

        holder.etPhoneNumber.setText(model.number)

        holder.ibEdit.setOnClickListener {
            edit(model, holder.etPhoneNumber.text.toString())
        }

        holder.ibDelete.setOnClickListener {
            delete(model)
        }
    }

    private fun delete(model: BasePhoneModel) {
        val index = list.indexOf(model)
        list.remove(model)
        submitList(list)
        notifyItemRemoved(index)
    }

    private fun edit(model: BasePhoneModel, newNum: String) {
        val position = list.indexOf(model)
        list[position] = BasePhoneModel(newNum, null)
        submitList(list)
        notifyItemChanged(position)
    }

    fun addAPhoneNumber(newPhoneNumber: BasePhoneModel) {
        list.add(newPhoneNumber)
        submitList(list)
        notifyItemInserted(list.lastIndex)
    }

    fun getPhoneNumbers() = list
}