package no.nhc.i_doc;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class EditEvidenceActivity extends Activity {
    static final String TAG = "EditEvidenceActivity";

    private JSONObject mDocument;
    private JSONArray mMetadata;

    private int mMetadataIdCounter = 0;

    static final String KEY_M_OBJECT = "m_object";
    static final String KEY_MID_COUNTER = "mid_counter";
    static final String KEY_EDITED_MID = "edited_mid";

    static final int ACTIVITY_METADATA_FORM = 1;

    static final boolean USE_FRAGMENTS = false;
    static final boolean USE_ADAPTER = true;

    private MetadataAdapter mAdapter;

    private class MetadataAdapter extends BaseAdapter {
        private JSONArray mArray;

        public MetadataAdapter(JSONArray array) {
            super();
            mArray = array;
        }

        @Override
        public int getCount() {
            Log.d(TAG, "metadata adapter size: " + Integer.toString(mArray.length()));
            return mArray.length();
        }

        @Override
        public Object getItem(int i) {
            try {
                return mArray.getJSONObject(i);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public long getItemId(int i) {
            try {
                return mArray.getJSONObject(i).getInt("id");
            } catch (JSONException e) {
                return i;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater)parent.getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            } else {
                view = convertView;
            }

            JSONObject obj = (JSONObject)getItem(position);
            TextView t1 = (TextView)view.findViewById(android.R.id.text1);
            TextView t2 = (TextView)view.findViewById(android.R.id.text2);

            if (obj != null) {
                try {
                    t1.setText(obj.getString("type"));

                    if (obj.has("Person")) {
                        JSONObject person = obj.getJSONObject("Person");
                        t2.setText(person.getString("FamilyName") + ", " + person.getString("GivenName"));
                    } else {
                        t2.setText("data.....");
                    }
                    return view;
                } catch (JSONException e) {
                }
            }

            t1.setText("...");
            t2.setText("...");
            return view;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString(KEY_M_OBJECT, mDocument.toString());
        state.putInt(KEY_MID_COUNTER, mMetadataIdCounter);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.edit_evidence);

        if (state == null) {
            // Load from database
            try {
                Document doc = DocumentDB.get(this).getDocument(getIntent().getData());
                mDocument = DocumentUtils.documentToJSON(doc);
                mMetadata = mDocument.getJSONArray("metadata");

                for (int i = 0; i < mMetadata.length(); ++i) {
                    assignMetadataId(mMetadata.getJSONObject(i));
                }
            } catch (JSONException e) {
                Log.e(TAG, "can't instantiate editor: " + e);
            }
        } else {
            // Load from bundle
            String json = state.getString(KEY_M_OBJECT);

            try {
                mDocument = new JSONObject(json);
                mMetadata = mDocument.getJSONArray("metadata");
                mMetadataIdCounter = state.getInt(KEY_MID_COUNTER);
            } catch (JSONException e) {
                Log.e(TAG, "can't restore document " + json + " : " + e);
            }
        }

        EditText t = (EditText) findViewById(R.id.editTitle);

        if (mDocument != null) {
            try {
                Log.d(TAG, "setting title to " + mDocument.getString("title"));
                t.setText(mDocument.getString("title"), TextView.BufferType.EDITABLE);

                buildDynamicForm();
            } catch (JSONException e) {
                Log.e(TAG, "problem " + e);
            }
        } else {
            Log.d(TAG, "no document");
            t.setText("[unknown evidence]", TextView.BufferType.NORMAL);
        }
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        if (requestCode == ACTIVITY_METADATA_FORM) {
            try {
                JSONObject newData = new JSONObject(data.getExtras().getString("object"));
                JSONObject md = findMetadata(newData.getInt("id"));

                if (md == null) {
                    Log.e(TAG, "can't find metadata to write to: " + newData.toString());
                    return;
                }

                copyJSONObject(newData, md);

                Log.d(TAG, "form data " + newData.toString() + " now in metadata: " + md.toString());
            } catch (JSONException e) {
                Log.e(TAG, "no metadata to write to");
            }
        }
    }

    private void copyJSONObject(JSONObject from, JSONObject to) throws JSONException {
        for (java.util.Iterator<String> it = from.keys(); it.hasNext();) {
            String key = it.next();
            to.put(key, from.get(key));
        }
    }

    private void save() {
        DocumentDB db = DocumentDB.get(this);
        Document doc = db.getDocument(getIntent().getData());

        if (USE_FRAGMENTS) {
            try {
                EditText t = (EditText) findViewById(R.id.editTitle);
                mDocument.put("title", t.getText().toString());

                for (int i = 0; i < mMetadata.length(); ++i) {
                    JSONObject md = mMetadata.getJSONObject(i);
                    MetadataEditFragment fragment = (MetadataEditFragment)
                        getFragmentManager().findFragmentByTag(getFragmentTag(md));

                    if (fragment != null) {
                        copyJSONObject(fragment.getResult(), md);
                    }
                }

                mDocument.put("metadata", mMetadata);
            } catch (JSONException e) {
            }
        }

        DocumentUtils.documentAssignJSON(db, doc, mDocument);
        db.saveDocument(doc);
    }

    /**
     *  Associate a metadata JSON object with an id used by the form code.
     */
    private void assignMetadataId(JSONObject md) throws JSONException {
        md.put("id", mMetadataIdCounter++);
    }

    private JSONObject findMetadata(int id) throws JSONException {
        for (int i = 0; i < mMetadata.length(); ++i) {
            JSONObject o = mMetadata.getJSONObject(i);
            if (o.getInt("id") == id) {
                return o;
            }
        }
        return null;
    }

    /**
     *  Return the tag which is used to tie an editor fragment to a specific
     *  metadata object/id.
     */
    private String getFragmentTag(JSONObject metadata) throws JSONException {
        return "edit" + new Integer(metadata.getInt("id")).toString();
    }

    private void buildDynamicForm() throws JSONException {
        if (USE_ADAPTER) {
            AdapterLinearLayout layout = (AdapterLinearLayout)findViewById(R.id.meta_list_view);
            mAdapter = new MetadataAdapter(mMetadata);
            layout.setAdapter(mAdapter);

            layout.setOnItemClickListener(new AdapterLinearLayout.OnItemClickListener() {
                public void onItemClick(AdapterLinearLayout parent, View view, int position) {
                    try {
                        editMetadata((JSONObject)mAdapter.getItem(position));
                    } catch (JSONException e) {
                        Log.e(TAG, "can't edit metadata: " + e);
                    }
                }
                });
        } else if (USE_FRAGMENTS) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            for (int i = 0; i < mMetadata.length(); ++i) {
                JSONObject metadata = mMetadata.getJSONObject(i);
                transaction.add(R.id.edit_fragments,
                                createEditFragment(mMetadata.getJSONObject(i)),
                                getFragmentTag(metadata));
            }

            transaction.commit();
        }
    }

    private View createUnsupported(String rep) {
        TextView tv = new TextView(this);
        tv.setText(rep);
        return tv;
    }

    /**
     *  Produce a Bundle compatible with GenericForm[Fragment|Activity]
     *  in order to build a form for editing a Metadata object.
     */
    private Bundle getFormBundle(JSONObject md) throws JSONException {
        Bundle b = new Bundle();

        b.putCharSequence("object", md.toString());
        b.putCharSequence("schema", DocumentUtils.getEditJSONSchema(
                              DocumentDB.get(this), md.getString("type")).toString());

        return b;
    }

    private void editMetadata(JSONObject md) throws JSONException {
        if (false) {
            // landscape, etc??
            /*
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment f = new GenericFormFragment();
            f.setArguments(getFormBundle(md));
            ft.add(R.id.testEditFragment, getFormFragment(md));
            //ft.add(getFormFragment(md), "tag");
            ft.addToBackStack(null);
            ft.commit();
            */
        } else {
            // Separate activity:
            Bundle b = getFormBundle(md);
            Intent intent = new Intent(this, GenericFormActivity.class);

            intent.putExtras(getFormBundle(md));
            intent.putExtra("title", md.getString("type"));
            startActivityForResult(intent, ACTIVITY_METADATA_FORM);
        }
    }

    /**
     *  Create a dynamic view for a metadata object
     */
    private Fragment createEditFragment(final JSONObject md) throws JSONException {
        Bundle bundle = new Bundle();
        MetadataEditFragment fragment = new MetadataEditFragment();
        bundle.putCharSequence("object", md.toString());
        fragment.setArguments(bundle);
        return fragment;
    }

    private java.lang.Class menuIdToMetaType(int id) {
        // BUG: there must exist a more dynamic way to map this....
        switch (id) {
        case R.id.edit_add_meta_victim:
            return Metadata.Victim.class;
        case R.id.edit_add_meta_witness:
            return Metadata.Witness.class;
        case R.id.edit_add_meta_suspect:
            return Metadata.Suspect.class;
        case R.id.edit_add_meta_protectedobject:
            return Metadata.ProtectedObject.class;
        case R.id.edit_add_meta_context:
            return Metadata.Context.class;
        case R.id.edit_add_meta_orgunit:
            return Metadata.OrgUnit.class;
        default:
            return null;
        }
    }

    /**
     *  Add a new metadata object and update the view
     */
    private void addMetadata(java.lang.Class type) throws JSONException {
        JSONObject md = DocumentUtils.createJSONMetadata(type);
        assignMetadataId(md);

        mMetadata.put(mMetadata.length(), md);

        if (USE_ADAPTER) {
            mAdapter.notifyDataSetChanged();
        } else if (USE_FRAGMENTS) {
            getFragmentManager().beginTransaction().add(
                R.id.edit_fragments,
                createEditFragment(md),
                getFragmentTag(md)).commit();
        }
    }

    public void showAddMetadataPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.edit_add_meta, popup.getMenu());
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    addMetadata(menuIdToMetaType(item.getItemId()));
                } catch (JSONException e) {
                    // Error adding..
                    Log.e(TAG, "can't add metadata: " + e);
                }
                return true;
            }
        });
        popup.show();
    }
}
