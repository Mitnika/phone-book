package dimitar.udemy.phonebook.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dimitar.udemy.phonebook.android.databinding.ItemPhoneBinding
import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import dimitar.udemy.phonebook.models.data.PhoneModel

class RecyclerViewAdapterEdit(
    private val list: ArrayList<PhoneModel>
) : ListAdapter<PhoneModel, RecyclerViewAdapterEdit.MyViewHolder>(DIFF_CALLBACK){

    private val allPhones = ArrayList(list)

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PhoneModel>() {
            override fun areItemsTheSame(oldItem: PhoneModel, newItem: PhoneModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PhoneModel, newItem: PhoneModel): Boolean {
                return oldItem == newItem
            }
        }
    }

    class MyViewHolder(binding: ItemPhoneBinding) : RecyclerView.ViewHolder(binding.root) {
        val etPhoneNumber   = binding.etPhoneNumber
        val ibEdit          = binding.ibDoneEditing
        val ibDelete        = binding.ibDelete
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ItemPhoneBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = getItem(position)

        holder.etPhoneNumber.setText(model.baseModel.number)

        holder.ibEdit.setOnClickListener {
            edit(model, holder.etPhoneNumber.text.toString())
        }

        holder.ibDelete.setOnClickListener {
            delete(model)
        }
    }

    private fun delete(model: PhoneModel) {
        val index       = list.indexOf(model)
        val indexAll    = allPhones.indexOf(model)

        allPhones[indexAll] = PhoneModel(
            id          = model.id,
            baseModel   = model.baseModel,
            state       = StateConstants.STATE_DELETED
        )

        list.removeAt(index)
        submitList(list)
        notifyItemRemoved(index)
    }

    private fun edit(model: PhoneModel, newNum: String) {
        val position        = list.indexOf(model)
        val positionAll     = allPhones.indexOf(model)

        val updatedModel = PhoneModel(
            id          = model.id,
            baseModel   = BasePhoneModel(
                number      = newNum,
                externalId  = model.baseModel.externalId
            ),
            state       = model.state
        )

        list[position]          = updatedModel
        allPhones[positionAll]  = updatedModel

        submitList(list)
        notifyItemChanged(position)
    }

    fun visualize() {
        submitList(list)
    }


    fun addAPhoneNumber(newPhoneNumber: BasePhoneModel) {
        val newModel = PhoneModel(
            id          = -1,
            baseModel   = newPhoneNumber,
            state       = StateConstants.STATE_UNEDITABLE
        )

        list.add(newModel)
        allPhones.add(newModel)
        submitList(list)
        notifyItemInserted(list.lastIndex)
    }

    fun getPhoneNumbers() = allPhones
}