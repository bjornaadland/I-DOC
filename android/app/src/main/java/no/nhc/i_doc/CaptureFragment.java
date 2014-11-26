package no.nhc.i_doc;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class CaptureFragment extends Fragment {
    MainActivity mActivity;
    public CaptureFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity)activity;
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_capture, container, false);

        Button button = (Button) fragmentView.findViewById(R.id.button_take_picture);
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mActivity.dispatchTakePictureIntent();
                }
            });

        button = (Button) fragmentView.findViewById(R.id.button_record_video);
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mActivity.dispatchRecordVideoIntent();
                }
            });


        button = (Button) fragmentView.findViewById(R.id.button_record_sound);
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mActivity.dispatchRecordSoundIntent();
                }
            });

        return fragmentView;
    }

}
