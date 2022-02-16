package com.dev.fa_alfygeorge_c0836170_android;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.dev.fa_alfygeorge_c0836170_android.adapter.PlacesAdapter;
import com.dev.fa_alfygeorge_c0836170_android.database.RoomDB;
import com.dev.fa_alfygeorge_c0836170_android.databinding.ActivityHomeBinding;
import com.dev.fa_alfygeorge_c0836170_android.listener.PlaceClickListener;
import com.dev.fa_alfygeorge_c0836170_android.model.Place;
import com.dev.fa_alfygeorge_c0836170_android.utils.RecyclerTouchListener;
import com.dev.fa_alfygeorge_c0836170_android.utils.SharedPrefHelper;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    ActivityHomeBinding binding;
    PlacesAdapter placesAdapter;
    List<Place> placeList = new ArrayList<>();
    RoomDB roomDB;
    Place place;
    RecyclerTouchListener touchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //initialize database
        roomDB = RoomDB.getInstance(this);
        placeList=roomDB.placeDAO().getAllPlaces();
        binding.fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
            }
        });

        if (placeList.isEmpty()){
            binding.txtInfo.setVisibility(View.VISIBLE);
            binding.recyclerHome.setVisibility(View.GONE);
        }else {
            binding.txtInfo.setVisibility(View.GONE);
            binding.recyclerHome.setVisibility(View.VISIBLE);


            updateRecycler(placeList);
        }

    }

    private void updateRecycler(List<Place> placeList) {
        binding.recyclerHome.setHasFixedSize(true);
        binding.recyclerHome.setLayoutManager(new LinearLayoutManager(this));
        placesAdapter = new PlacesAdapter(HomeActivity.this, placeList,placeClickListener);
        binding.recyclerHome.setAdapter(placesAdapter);
        setSwipeAction();

        placesAdapter.notifyDataSetChanged();
    }

    private void setSwipeAction(){
        touchListener = new RecyclerTouchListener(this,binding.recyclerHome);
        touchListener.setClickable(new RecyclerTouchListener.OnRowClickListener() {
            @Override
            public void onRowClicked(int position) {

                SharedPrefHelper.getInstance(getApplicationContext()).setBolIsUpdate(false);
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                intent.putExtra("fav_place", placeList.get(position));
                startActivity(intent);
            }

            @Override
            public void onIndependentViewClicked(int independentViewID, int position) {

            }
        })
                .setSwipeOptionViews(R.id.delete_place,R.id.edit_place)
                .setSwipeable(R.id.rowFG, R.id.rowBG, new RecyclerTouchListener.OnSwipeOptionsClickListener() {
                    @Override
                    public void onSwipeOptionClicked(int viewID, int position) {
                        switch (viewID){
                            case R.id.delete_place:
                                try {
                                    roomDB.placeDAO().delete(placeList.get(position));
                                    placeList.remove(position);
                                    placesAdapter.notifyDataSetChanged();
                                    if (placeList.isEmpty()){
                                        binding.txtInfo.setVisibility(View.VISIBLE);
                                        binding.recyclerHome.setVisibility(View.GONE);
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                                break;
                            case R.id.edit_place:
//                                SharedPrefHelper.getInstance(getApplicationContext()).setBolIsUpdate(true);
//                                Intent intent = new Intent(HomeActivity.this,MainActivity.class);
//                                intent.putExtra("update_place", placeList.get(position));
//                                startActivity(intent);
                                break;
                        }
                    }
                });
        binding.recyclerHome.addOnItemTouchListener(touchListener);
    }
    private  final PlaceClickListener placeClickListener = new PlaceClickListener() {
        @Override
        public void onClick(Place place) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("fav_place", place);
                startActivity(intent);

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        binding.recyclerHome.addOnItemTouchListener(touchListener);

    }




}