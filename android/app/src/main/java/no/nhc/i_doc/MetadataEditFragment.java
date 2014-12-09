package no.nhc.i_doc;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;
import org.json.JSONException;

public class MetadataEditFragment extends Fragment {
    static final String TAG = "MetadataEditFragment";

    private JSONObject mObject = null;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        Bundle b = getArguments();

        if (b == null) return;

        try {
            mObject = new JSONObject(b.getString("object"));
        } catch (JSONException e) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.metadata_edit_fragment, container, false);

        {
            TextView textView = (TextView)view.findViewById(R.id.metadata_title);
            try {
                textView.setText(mObject.getString("type"));
            } catch (JSONException e) {
                textView.setText("Unknown metadata");
            }
        }

        {
            Button toggle = (Button)view.findViewById(R.id.toggle_details);

            toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleDetails();
                }
            });
        }

        return view;
    }

    public JSONObject getResult() {
        Fragment fragment = getChildFragmentManager().findFragmentByTag("expanded");
        if (fragment != null) {
            return ((GenericFormFragment)fragment).getResult();
        } else {
            return mObject;
        }
    }

    private Fragment createFormFragment() {
        Bundle bundle = new Bundle();

        try {
            bundle.putCharSequence("object", mObject.toString());
            bundle.putCharSequence("schema", DocumentUtils.getEditJSONSchema(
                                       DocumentDB.get(getActivity()),
                                       mObject.getString("type")).toString());

            GenericFormFragment fragment = new GenericFormFragment();
            fragment.setArguments(bundle);
            return fragment;
        } catch (JSONException e) {
            return null;
        }
    }

    public void toggleDetails() {
        FragmentManager manager = getChildFragmentManager();
        Fragment fragment = manager.findFragmentByTag("expanded");

        if (fragment != null) {
            mObject = ((GenericFormFragment)fragment).getResult();
            manager.beginTransaction().remove(fragment).
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE).commit();
        } else {
            manager.beginTransaction().
                add(R.id.expand_view, createFormFragment(), "expanded").
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
        }
    }
}
