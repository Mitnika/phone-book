package dimitar.udemy.phonebook.android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dimitar.udemy.phonebook.android.R
import dimitar.udemy.phonebook.android.databinding.ItemContactBinding
import dimitar.udemy.phonebook.models.visuals.MainContactVisualization
import dimitar.udemy.phonebook.presenters.MainPresenter

class RecyclerViewAdapterMain (
    private val list: ArrayList<MainContactVisualization>,
    private val context: Context,
    private val presenter: MainPresenter
        ) : ListAdapter<MainContactVisualization, RecyclerViewAdapterMain.MyViewHolder>(DIFF_CALLBACK){

    class MyViewHolder(binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        val civPerson = binding.ivContactImage
        val tvName = binding.tvName
        val llContact = binding.llContact
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MainContactVisualization>() {
            override fun areItemsTheSame(oldItem: MainContactVisualization, newItem: MainContactVisualization): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MainContactVisualization, newItem: MainContactVisualization): Boolean {
                return oldItem == newItem
            }
        }
    }

    private val itemFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            results.values = presenter.searchForContacts(constraint.toString())
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results?.values is ArrayList<*>) {
                submitList(results.values as ArrayList<MainContactVisualization>)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = getItem(position)

        holder.tvName.text = context.resources.getString(R.string.name_person, model.displayName)

        Glide
            .with(context)
            .load(model.picture)
            .centerCrop()
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(holder.civPerson)

        holder.llContact.setOnClickListener {
            presenter.contactChosen(model.id)
        }
    }

    fun addNew(new: ArrayList<MainContactVisualization>) {
        submitList(ArrayList(new))
    }

    fun visualize() {
        submitList(list)
    }


    fun getFilter() = itemFilter
}