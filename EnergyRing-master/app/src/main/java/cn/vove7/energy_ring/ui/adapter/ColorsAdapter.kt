package cn.vove7.energy_ring.ui.adapter

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import cn.vove7.energy_ring.R
import cn.vove7.energy_ring.floatwindow.FloatRingWindow
import cn.vove7.energy_ring.util.pickColor

/**
 * # ColorsAdapter
 *
 * @author Vove
 * 2020/5/11
 */
class ColorsAdapter(val getColors: () -> IntArray, val setColors: (IntArray) -> Unit) : RecyclerView.Adapter<ColorsAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val colorView = TextView(parent.context)
        colorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        colorView.setTextColor(Color.WHITE)
        colorView.setPadding(20, 10, 20, 10)
        colorView.gravity = Gravity.CENTER
        colorView.layoutParams = FrameLayout.LayoutParams(-2, -1).also {
            it.setMargins(10, 10, 10, 10)
        }
        return ColorViewHolder(colorView)
    }

    private val MAX_COLOR_COUNT = 10

    private val hasPlus get() = getColors().size < MAX_COLOR_COUNT

    override fun getItemCount(): Int = getColors().size + if (hasPlus) 1 else 0

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        (holder.itemView as TextView).apply {
            if (hasPlus && position == itemCount - 1) {
                setOnLongClickListener(null)
                setOnClickListener {
                    pickColor(context)
                }
                text = ""
                background = ContextCompat.getDrawable(context, R.drawable.ic_add_circle)
            } else {
                text = getRangeByPos(position).let { "${it.first}-${it.second}" }
                setBackgroundColor(getColors()[position])
                setOnClickListener {
                    pickColor(context, holder.adapterPosition)
                }
                setOnLongClickListener {
                    val cs = getColors()
                    if (cs.size <= 1) {
                        Toast.makeText(context, "最少设置一个颜色", Toast.LENGTH_SHORT).show()
                        return@setOnLongClickListener true
                    }
                    kotlin.runCatching {
                        setColors(cs.toMutableList().apply { removeAt(position) }.toIntArray())
                        notifyDataSetChanged()
                        FloatRingWindow.onShapeTypeChanged()
                    }
                    true
                }
            }
        }
    }

    private fun getRangeByPos(pos: Int): Pair<Int, Int> {
        val len = getColors().size
        val perf = 100f / len

        return (perf * pos).toInt() to (perf * (pos + 1)).toInt()
    }

    private fun pickColor(context: Context, pos: Int? = null) {
        pickColor(context, initColor = pos?.let { getColors().getOrNull(it) }) { c ->
            if (pos == null) {
                setColors(getColors().toMutableList().apply { add(c) }.toIntArray())
                notifyDataSetChanged()
            } else {
                setColors(getColors().also { it[pos] = c })
                notifyDataSetChanged()
            }
            FloatRingWindow.onShapeTypeChanged()
        }
    }

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}