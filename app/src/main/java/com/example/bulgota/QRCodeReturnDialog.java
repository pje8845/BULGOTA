package com.example.bulgota;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bulgota.api.BullgoTAService;
import com.example.bulgota.api.RequestReturnModel;
import com.example.bulgota.api.ResponseReturnModel;
import com.example.bulgota.api.ResponseSelectModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QRCodeReturnDialog extends Dialog implements View.OnClickListener{

    private Button btnOk;
    private Button btnCancle;
    private EditText edtModel;
    private Context context;
    private TextView tvWarning;

    private String modelNum;

    private GpsTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;

    private CustomDialogListener customDialogListener;

    public QRCodeReturnDialog(Context context) {
        super(context);
        this.context = context;
    }


    //인터페이스 설정
    interface CustomDialogListener{
        void onPositiveClicked(String modelNum, int data, int status);
        void onNegativeClicked();
    }

    //호출할 리스너 초기화
    public void setDialogListener(CustomDialogListener customDialogListener){
        this.customDialogListener = customDialogListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_qr_code);

        //init
        btnOk = (Button)findViewById(R.id.btn_ok);
        btnCancle = (Button)findViewById(R.id.btn_cancle);
        tvWarning = (TextView)findViewById(R.id.tv_warning);
        edtModel = (EditText)findViewById(R.id.edt_model);

        //버튼 클릭 리스너 등록
        btnOk.setOnClickListener(this);
        btnCancle.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        modelNum = edtModel.getText().toString();

        if(v.getId() == R.id.btn_ok && modelNum.getBytes().length <= 0) {
            tvWarning.setText("QR코드 번호를 입력해주세요");
            tvWarning.setVisibility(View.VISIBLE);
        } else if(v.getId() == R.id.btn_ok) {

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BullgoTAService.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            BullgoTAService bullgoTAService = retrofit.create(BullgoTAService.class);
            bullgoTAService.checkModel(modelNum).enqueue(new Callback<ResponseSelectModel>() {
                @Override
                public void onResponse(Call<ResponseSelectModel> call, Response<ResponseSelectModel> response) {
                    if (response.body().getSuccess()) {
                        //유효한 모델이면
                        gpsTracker = new GpsTracker(context);

                        double latitude = gpsTracker.getLatitude();
                        double longitude = gpsTracker.getLongitude();
                        Log.e("latitude", String.valueOf(latitude));
                        Log.e("longitude", String.valueOf(longitude));

                        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl(BullgoTAService.BASE_URL)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();
                        BullgoTAService bullgoTAService = retrofit.create(BullgoTAService.class);
                        bullgoTAService.returnModel(modelNum, new RequestReturnModel(latitude, longitude)).enqueue(new Callback<ResponseReturnModel>() {
                            @Override
                            public void onResponse(Call<ResponseReturnModel> call, Response<ResponseReturnModel> response) {
                                int status = 2;
                                int data = 0;
                                if (response.body().getSuccess()) {
                                    //유효한 모델이면
                                    double result = (double) response.body().getData();
                                    data = (int) result;
                                    status = 0;

                                    customDialogListener.onPositiveClicked(modelNum, data, status);
                                    dismiss();
                                } else {
                                    switch(response.body().getMessage()) {
                                        case "이미 반납된 킥보드입니다.":
                                            status = 1;
                                            break;
                                        case "킥보드 대여 실패":
                                            status = 2;
                                            break;
                                    }

                                    customDialogListener.onPositiveClicked(modelNum, data, status);
                                    dismiss();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseReturnModel> call, Throwable t) {
                                tvWarning.setText("오류가 발생했습니다. 다시 시도해주세요.");
                                tvWarning.setVisibility(View.VISIBLE);
                            }
                        });
                    } else {
                        tvWarning.setText("유효하지않은 제품번호입니다. 다시입력해주세요.");
                        tvWarning.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(Call<ResponseSelectModel> call, Throwable t) {
                    tvWarning.setText("오류가 발생했습니다. 다시 시도해주세요.");
                    tvWarning.setVisibility(View.VISIBLE);
                }
            });
        } else {
            dismiss();
        }
    }
}