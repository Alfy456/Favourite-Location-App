package com.dev.fa_alfygeorge_c0836170_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.dev.fa_alfygeorge_c0836170_android.R;
import com.dev.fa_alfygeorge_c0836170_android.listener.PlaceClickListener;
import com.dev.fa_alfygeorge_c0836170_android.model.Place;

import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesViewHolder> {
    Context context;
    List<Place> placeList;
    PlaceClickListener placeClickListener;

    public PlacesAdapter(Context context, List<Place> placeList, PlaceClickListener placeClickListener) {
        this.context = context;
        this.placeList = placeList;
        this.placeClickListener = placeClickListener;
    }

    @NonNull
    @Override
    public PlacesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PlacesViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_places_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull PlacesViewHolder holder, int position) {

        holder.txt_place_name.setText(placeList.get(position).getPlaceName());
        holder.txt_created_date.setText(placeList.get(position).getCreatedDate());

        if (placeList.get(position).isVisited) {
           // holder.card_place.setBackgroundResource(R.color.purple_200);
            holder.txt_visited.setVisibility(View.VISIBLE);

        }
            holder.card_place.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    placeClickListener.onClick(placeList.get(holder.getAdapterPosition()));
                }
            });

    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }
}

class PlacesViewHolder extends RecyclerView.ViewHolder{

    RelativeLayout card_place;
    TextView txt_place_name,txt_created_date,txt_visited;

    public PlacesViewHolder(@NonNull View itemView) {
        super(itemView);

        card_place = itemView.findViewById(R.id.card_places);
        txt_place_name = itemView.findViewById(R.id.place_name);
        txt_created_date = itemView.findViewById(R.id.created_date);
        txt_visited = itemView.findViewById(R.id.txtVisited);
    }
}
