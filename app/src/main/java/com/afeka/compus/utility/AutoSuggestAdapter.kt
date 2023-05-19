package com.afeka.compus.utility

import android.content.Context
import android.widget.ArrayAdapter
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;

class AutoSuggestAdapter(private var context: Context,
                         private var resource: Int,
                         private var items: List<String>
        ): ArrayAdapter<String>(context, resource, 0, items) {

    private var tempItems = ArrayList(items)
    private var suggestions = ArrayList<String>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(resource, parent, false)
        }
        val item = items[position]

        if (view is TextView)
            view.text = item

        return view!!
    }

    override fun getFilter(): Filter {
        return nameFilter
    }

    private val nameFilter = object: Filter() {
        override fun convertResultToString(resultValue: Any?): CharSequence {
            return resultValue as String
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return if (constraint != null) {
                suggestions.clear()
                for (names in tempItems) {
                    if (names.contains(constraint, ignoreCase = true))
                        suggestions.add(names)
                }
                val filterResults = FilterResults()
                filterResults.values = suggestions
                filterResults.count = suggestions.size
                filterResults
            } else {
                FilterResults()
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            if (results.values == null)
                return
            val filterList = results.values as ArrayList<String>
            if (results.count > 0) {
                clear()
                for (item in filterList) {
                    add(item)
                    notifyDataSetChanged()
                }
            }
        }
    }
}