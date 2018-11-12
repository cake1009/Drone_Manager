package com.sum10.drone_manager;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    private String dest;
    private String locker;
    private double lat;
    private double lng;
    private LatLng here;
    private Button call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("callservice");

        Intent intent = getIntent();
        dest = intent.getStringExtra("address");
        locker = intent.getStringExtra("locker");
        TextView location = (TextView) findViewById(R.id.textView);
        TextView text = (TextView) findViewById(R.id.textView2);
        location.setText(dest);
        text.setText("보관소 위치 : " + locker);

        call = (Button) findViewById(R.id.button);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("address").getValue().equals(dest)) {
                        if (snapshot.child("ack").getValue().equals(true)) {
                            call.setText("출발 승인했습니다.");
                            call.setEnabled(false);
                        }
                    }
                }
            }
                @Override
                public void onCancelled (DatabaseError databaseError){
                    Log.w("TAG: ", "Failed to read value", databaseError.toException());
                }
            });

        call.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(DetailActivity.this);
                alert_confirm.setMessage("호출 승인하시겠습니까?").setCancelable(false).setPositiveButton("호출",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'YES'
                                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            if (snapshot.child("address").getValue().equals(dest)) {
                                                Map<String, Object> update = new HashMap<>();
                                                String key = snapshot.getKey();
                                                update.put(key+"/ack", true);
                                                myRef.updateChildren(update);
                                                Toast.makeText(getApplicationContext(), "출발했습니다!", Toast.LENGTH_LONG).show();
                                                ((MainActivity) MainActivity.mContext).ListRemove(dest);
                                                finish();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Log.w("TAG: ", "Failed to read value", databaseError.toException());
                                    }
                                });
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'No'
                                return;
                            }
                        });
                AlertDialog alert = alert_confirm.create();
                alert.show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName(dest, 1);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "geocoder Error");
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
        } else {
            Address address = addresses.get(0);
            lat = address.getLatitude();
            lng = address.getLongitude();
        }
        here = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(here).title("현재 위치"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 14));
    }
}