package com.intelly.lyncwyze

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ProviderCustomSpinnerAdapter(context: Context, private var items: List<ProviderAdaptorData>) : ArrayAdapter<ProviderAdaptorData>(context, 0, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.provider_custom_spinner_item, parent, false)

        val textLine1 = view.findViewById<TextView>(R.id.textLine1)
        val textLine2 = view.findViewById<TextView>(R.id.textLine2)
        val textLine3 = view.findViewById<TextView>(R.id.textLine3)

        val item = getItem(position)
        item?.let {
            textLine1.text = it.line1
            textLine1.visibility = if (it.line1.isBlank()) View.GONE else View.VISIBLE
            textLine2.text = it.line2
            textLine2.visibility = if (it.line2.isBlank()) View.GONE else View.VISIBLE
            textLine3.text = it.line3
            textLine3.visibility = if (it.line3.isBlank()) View.GONE else View.VISIBLE
        }
        return view
    }
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}

data class ProviderAdaptorData(
    var line1: String,
    var line2: String,
    var line3: String,
)