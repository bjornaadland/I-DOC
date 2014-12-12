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
import android.widget.AutoCompleteTextView;
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

    /**
     *  The FormObject has responsibility of mapping
     *  form values into a JSON object.
     */
    private static class FormObject {
        public Map<String, ValueMapper> mValueMappers = new HashMap<String, ValueMapper>();
        public JSONObject mObject;
        public View mView;
        public String mDefaultType;

        public void initView() {
            for (String key : mValueMappers.keySet()) {
                mValueMappers.get(key).initView(mObject, key);
            }
        }

        public JSONObject getResult() {
            if (mObject == null) {
                mObject = new JSONObject();
                try {
                    mObject.put("type", mDefaultType);
                } catch (JSONException e) {
                    throw new RuntimeException("no default type specified");
                }
            }

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
    }

    // This holds the form data
    private FormObject mFormObject = new FormObject();

    /**
     *  Object connecting a property to an editor in the view
     */
    private interface ValueMapper {
        /**
         *  Set up the view initial state. The property can be fetched
         *  in object, with the given key.
         */
        void initView(JSONObject object, String key);

        /**
         *  Get whether this has any value set.
         */
        boolean hasValue();

        /**
         *  Get the JSON value for this property
         */
        Object getJSONValue();
    }

    /**
     *  A mapper for "text" properties
     */
    private static class TextMapper implements ValueMapper {
        public EditText mEditText;

        public void initView(JSONObject object, String key) {
            try {
                mEditText.setText(object.getString(key));
            } catch (JSONException e) {
            }
        }

        public boolean hasValue() {
            return true;
        }

        public Object getJSONValue() {
            return mEditText.getText().toString();
        }
    }

    /**
     *  A mapper for "enum" properties
     */
    private static class SpinnerMapper implements ValueMapper {
        public ArrayAdapter mAdapter;
        public Spinner mSpinner;
        public List<String> mValues;

        public void initView(JSONObject object, String key) {
            int position = 0;
            try {
                String currentValue = object.getString(key);
                for (String value : mValues) {
                    if (currentValue.equals(value)) {
                        mSpinner.setSelection(position);
                        break;
                    }
                    position++;
                }
            } catch (JSONException e) {
            }
        }

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
     *  A mapper for "object" properties, which manages a sub form.
     */
    private static class ObjectMapper implements ValueMapper {
        FormObject mForm;

        public void initView(JSONObject object, String key) {
            try {
                JSONObject subObject = object.getJSONObject(key);
                mForm.mObject = subObject;
                mForm.initView();
            } catch (JSONException e) {
            }
        }

        public boolean hasValue() { return true; }

        public JSONObject getJSONValue() {
            return mForm.getResult();
        }
    }

    /**
     *  Get the result of the form
     */
    public JSONObject getResult() {
        return mFormObject.getResult();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getArguments();

        Log.d(TAG, "load json editor with object: " + b.getString("object"));

        try {
            mFormObject.mObject = new JSONObject(b.getString("object"));
        } catch (JSONException e) {
            Log.e(TAG, "bad JSON value sent, starting with empty object");
            mFormObject.mObject = new JSONObject();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String schema = getArguments().getString("schema");
        if (schema == null) {
            return null;
        }
        Log.d(TAG, "onCreateView schema: " + schema);

        try {
            View view = buildView(mFormObject, readString(schema));
            mFormObject.initView();
            return view;
        } catch (IOException e) {
            Log.e(TAG, "JSONException. Bad schema?");
            return null;
        }
    }

    private void applySuggestion(FormObject form, String type, SuggestionAdapter.Suggestion sug) {
        JSONObject obj = DocumentUtils.getSuggestionData(DocumentDB.get(getActivity()), type, sug);
        form.mObject = obj;
        form.initView();
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

    private static boolean isContextual(Map<String, Object> schemaProps) {
        return schemaProps.containsKey("contextual") &&
            ((Boolean)schemaProps.get("contextual")).booleanValue();
    }

    private View createText(final FormObject form, final Map<String, Object> schemaProps) {
        EditText et;
        String key = (String)schemaProps.get("key");

        if (schemaProps.containsKey("searchable")) {
            final String searchableType = (String)schemaProps.get("searchable");
            final AutoCompleteTextView actv = new AutoCompleteTextView(getActivity());
            final SuggestionAdapter adapter = new
                SuggestionAdapter(DocumentDB.get(getActivity()),
                                  searchableType,
                                  key);

            actv.setAdapter(adapter);
            actv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "autocomplete item selected: " + Integer.toString(position));
                    applySuggestion(form, searchableType, adapter.getSuggestion(position));
                }

            });
            et = actv;
        } else {
            et = new EditText(getActivity());
        }

        if (isContextual(schemaProps)) {
            et.setHint(translateName(schemaProps));
        }

        TextMapper tm = new TextMapper();
        tm.mEditText = et;
        form.mValueMappers.put(key, tm);

        return et;
    }

    private View createSpinner(FormObject form, Map<String, Object> schemaProps) {
        Spinner spinner = new Spinner(getActivity());
        String key = (String)schemaProps.get("key");
        List<String> values = (List<String>)schemaProps.get("values");

        if (values == null) return null;

        ArrayAdapter<CharSequence> adapter = new
            ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        {
            SpinnerMapper sm = new SpinnerMapper();
            sm.mAdapter = adapter;
            sm.mSpinner = spinner;
            sm.mValues = values;
            form.mValueMappers.put(key, sm);
        }

        return spinner;
    }

    private View mapSubForm(FormObject form, Map<String, Object> schemaProps) {
        String key = (String)schemaProps.get("key");
        FormObject subForm = (FormObject)schemaProps.get("form");
        subForm.mDefaultType = (String)schemaProps.get("defaultType");

        {
            ObjectMapper om = new ObjectMapper();
            om.mForm = subForm;
            form.mValueMappers.put(key, om);
        }

        return subForm.mView;
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

    private View buildOneFromProps(FormObject form, Map<String, Object> schemaProps) {
        LinearLayout ll = createVerticalLayout();
        View v = null;

        if (!isContextual(schemaProps)) {
            ll.addView(createHeader(translateName(schemaProps)));
        }

        switch ((String)schemaProps.get("type")) {
        case "text":
            v = createText(form, schemaProps);
            break;
        case "enum":
            v = createSpinner(form, schemaProps);
            break;
        case "object":
            v = mapSubForm(form, schemaProps);
            break;
        }

        if (v != null) {
            ll.addView(v);
        }
        return ll;
    }

    /**
     *  Create a view for editing a JSON object that is a value of
     *  an enclosing JSON object
     */
    private FormObject createSubForm(JsonReader schema) throws IOException {
        FormObject form = new FormObject();
        form.mView = buildView(form, schema);
        return form;
    }

    private void buildViewList(FormObject form,
                               ViewGroup root,
                               JsonReader schema) throws IOException {
        schema.beginArray();
        while (schema.hasNext()) {
            Map<String, Object> props = new HashMap<String, Object>();
            View view = null;

            schema.beginObject();
            while (schema.hasNext()) {
                String prop = schema.nextName();
                switch (prop) {
                case "name":
                case "key":
                case "type":
                case "defaultType":
                case "searchable":
                    props.put(prop, schema.nextString());
                break;
                case "contextual":
                    props.put(prop, schema.nextBoolean());
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
                case "schema":
                    props.put("form", createSubForm(schema));
                    break;
                default:
                    schema.skipValue();
                    break;
                }
            }
            schema.endObject();

            if ((view = buildOneFromProps(form, props)) != null) {
                root.addView(view);
            }
        }
        schema.endArray();
    }

    private View buildView(FormObject form, JsonReader schema) throws IOException {
        LinearLayout root = createVerticalLayout();
        buildViewList(form, root, schema);
        return root;
    }
}
