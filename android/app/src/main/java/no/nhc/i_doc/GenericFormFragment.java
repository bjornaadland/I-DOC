package no.nhc.i_doc;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONException;

/**
 *  Edit a JSON structure according to a schema.
 *  This fragment displays a form UI.
 */
public class GenericFormFragment extends Fragment {
    static final String TAG = "EditJSONActivity";

    // This holds the form data
    private JSONObject mObject;

    private interface ValueMapper {
        boolean hasValue();
        Object getJSONValue();
    }

    private Map<String, ValueMapper> mValueMappers = new HashMap<String, ValueMapper>();

    private static class TextMapper implements ValueMapper {
        public EditText mEditText;

        public boolean hasValue() {
            return true;
        }

        public Object getJSONValue() {
            return mEditText.getText().toString();
        }
    }

    private static class SpinnerMapper implements ValueMapper {
        public ArrayAdapter mAdapter;
        public Spinner mSpinner;

        public boolean hasValue() {
            return mSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION;
        }

        public Object getJSONValue() {
            int pos = mSpinner.getSelectedItemPosition();
            if (pos == AdapterView.INVALID_POSITION) {
                return null;
            } else {
                String value = (String)mAdapter.getItem(pos);
                return value;
            }
        }
    }

    /**
     *  Get the result of the form
     */
    public JSONObject getResult() {
        try {
            for (String key : mValueMappers.keySet()) {
                ValueMapper vm = mValueMappers.get(key);
                if (vm.hasValue()) {
                    mObject.put(key, vm.getJSONValue());
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "problem getting value from form: " + e.toString());
        }

        return mObject;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getArguments();

        Log.d(TAG, "load json editor with object: " + b.getString("object"));

        try {
            mObject = new JSONObject(b.getString("object"));
        } catch (JSONException e) {
            Log.e(TAG, "bad JSON value sent, starting with empty object");
            mObject = new JSONObject();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String schema = getArguments().getString("schema");
        if (schema == null) {
            return null;
        }
        Log.d(TAG, "onCreateView schema: " + schema);

        return buildView(readString(schema));
    }

    private Object parseJSON(String s) {
        return null;
    }

    private JsonReader readString(String json) {
        byte[] bytes;
        try {
            bytes = json.getBytes("utf8");
        } catch (java.io.UnsupportedEncodingException e) {
            // utf8 is unknown, fall back to best effort
            bytes = json.getBytes();
            Log.e(TAG, "Cannot decode utf8");
        }
        InputStream s = new ByteArrayInputStream(bytes);
        return new JsonReader(new InputStreamReader(s));
    }

    private View createHeader(String text) {
        TextView tv = new TextView(getActivity());
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setText(text);
        return tv;
    }

    private View createText(Map<String, Object> schemaProps) {
        EditText et = new EditText(getActivity());
        String key = (String)schemaProps.get("key");
        String initialValue;

        try {
            initialValue = mObject.getString(key);
        } catch (JSONException e) {
            initialValue = "";
        }

        {
            TextMapper tm = new TextMapper();
            tm.mEditText = et;
            mValueMappers.put(key, tm);
        }

        et.setText(initialValue);
        return et;
    }

    private View createSpinner(Map<String, Object> schemaProps) {
        Spinner spinner = new Spinner(getActivity());
        String key = (String)schemaProps.get("key");
        List<String> values = (List<String>)schemaProps.get("values");

        if (values == null) return null;

        ArrayAdapter<CharSequence> adapter = new
            ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        try {
            String currentValue = mObject.getString(key);
            int position = 0;

            for (String value : values) {
                if (currentValue.equals(value)) {
                    spinner.setSelection(position);
                    break;
                }
                position++;
            }
        } catch (JSONException e) {
        }

        {
            SpinnerMapper sm = new SpinnerMapper();
            sm.mAdapter = adapter;
            sm.mSpinner = spinner;
            mValueMappers.put(key, sm);
        }

        return spinner;
    }

    private String translateName(Map<String, Object> schemaProps) {
        // BUG:
        return (String)schemaProps.get("name");
    }

    private LinearLayout createVerticalLayout() {
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                       ViewGroup.LayoutParams.WRAP_CONTENT));
        return ll;
    }

    private View buildOne(Map<String, Object> schemaProps) {
        LinearLayout ll = createVerticalLayout();
        View v = null;

        ll.addView(createHeader(translateName(schemaProps)));

        switch ((String)schemaProps.get("type")) {
        case "text":
            v = createText(schemaProps);
            break;
        case "enum":
            v = createSpinner(schemaProps);
            break;
        }

        if (v != null) {
            ll.addView(v);
        }
        return ll;
    }

    private View buildView(JsonReader schema) {
        LinearLayout root = createVerticalLayout();
        try {
            schema.beginArray();
            while (schema.hasNext()) {
                Map<String, Object> props = new HashMap<String, Object>();

                schema.beginObject();
                while (schema.hasNext()) {
                    String prop = schema.nextName();
                    switch (prop) {
                    case "name": // TODO: must be translated
                    case "key":
                    case "type":
                        props.put(prop, schema.nextString());
                        break;
                    case "values":
                        List<String> values = new ArrayList<String>();
                        schema.beginArray();
                        while (schema.hasNext()) {
                            values.add(schema.nextString());
                        }
                        schema.endArray();
                        props.put(prop, values);
                        break;
                    default:
                        schema.skipValue();
                        break;
                    }
                }
                schema.endObject();
                root.addView(buildOne(props));
            }
            schema.endArray();
        } catch (IOException e) {
            Log.e(TAG, "JSONException. Bad schema?");
            return null;
        }

        return root;
    }
}
