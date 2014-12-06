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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
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

    /**
     *  Get the result of the form
     */
    public JSONObject getResult() {
        // TODO: actually pick up data from form ;)
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
        InputStream s = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
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

        et.setText(initialValue);
        return et;
    }

    private View createSpinner(Map<String, Object> schemaProps) {
        Spinner spinner = new Spinner(getActivity());
        List<String> values = (List<String>)schemaProps.get("values");

        if (values == null) return null;

        ArrayAdapter<CharSequence> adapter = new
            ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
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
