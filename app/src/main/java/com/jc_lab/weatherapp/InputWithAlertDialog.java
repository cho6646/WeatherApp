package com.jc_lab.weatherapp;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InputWithAlertDialog {
    Context mContext;
    AlertDialog.Builder ad;

    public InputWithAlertDialog(Context context)
    {
        this.mContext = context;
    }

    public void apply()
    {
        ad = new AlertDialog.Builder(mContext);
        ad.setTitle("도시 이름 설정");
        ad.setMessage("추가할 도시 이름을 넣어주세요");

        final EditText et = new EditText(mContext);
        ad.setView(et);

        ad.setPositiveButton("추가", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String value = et.getText().toString();
                Geocoder geocoder = new Geocoder(mContext);
                List<Address> addresses = new ArrayList<>();
                try{
                    addresses = geocoder.getFromLocationName(value, 3);
                }catch(IOException ioException){
                    Toast.makeText(mContext,"Geocoder Service Not Available", Toast.LENGTH_SHORT).show();
                }catch(IllegalArgumentException illegalArgumentException){
                    Toast.makeText(mContext,"Incorrect GPS Coordination", Toast.LENGTH_SHORT).show();
                }
                if(addresses==null || addresses.size()==0)
                {
                    Toast.makeText(mContext, "해당 지역은 없는 지역입니다", Toast.LENGTH_SHORT);
                    dialog.dismiss();
                    return;
                }
                MainActivity.addedCity.setValue(addresses.get(0).getAdminArea());
                dialog.dismiss();
            }
        });

        ad.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        ad.show();
    }
}
