package com.example.weathery

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weathery.databinding.WeatherRvItemBinding
import com.squareup.picasso.Picasso
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRVAdapter(
    private val context: Context,
    private val weatherRVModal: ArrayList<WeatherRVModal>
) : RecyclerView.Adapter<WeatherRVAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = WeatherRvItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val modal = weatherRVModal[position]
        holder.binding.temprature.text = "${modal.temperature}°c"
        Picasso.get().load("http:${modal.icon}").into(holder.binding.idtvcondition)
        holder.binding.idTVwindspeed.text = "${modal.windspeed} Km/hr"

        val input = SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault())
        val output = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        try {
            val t: Date = input.parse(modal.time) ?: Date()
            holder.binding.idTVtime.text = output.format(t)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return weatherRVModal.size
    }

    class MyViewHolder(val binding: WeatherRvItemBinding) : RecyclerView.ViewHolder(binding.root)
}
