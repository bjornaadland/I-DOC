package no.nhc.i_doc;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

    private static class TextMap {
        public Metadata mMetadata;
        public Enum mProperty;
        public EditText mEditText;
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

                for (java.util.Iterator<String> it = newData.keys(); it.hasNext();) {
                    String key = it.next();
                    md.put(key, newData.get(key));
                }
                Log.d(TAG, "form data " + newData.toString() + " now in metadata: " + md.toString());
            } catch (JSONException e) {
                Log.e(TAG, "no metadata to write to");
            }
        }
    }

    private void save() {
        DocumentDB db = DocumentDB.get(this);
        Document doc = db.getDocument(getIntent().getData());
        try {
            EditText t = (EditText) findViewById(R.id.editTitle);
            mDocument.put("title", t.getText().toString());
            mDocument.put("metadata", mMetadata);

            DocumentUtils.documentAssignJSON(db, doc, mDocument);
            db.saveDocument(doc);
        } catch (JSONException e) {
        }
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

    private void buildDynamicForm() throws JSONException {
        ViewGroup vg = (ViewGroup) findViewById(R.id.editDynamicGroup);
        for (int i = 0; i < mMetadata.length(); ++i) {
            vg.addView(createDynamicView(mMetadata.getJSONObject(i)));
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
    private View createDynamicView(final JSONObject md) throws JSONException {
        LinearLayout root = new LinearLayout(this);
        String type = md.getString("type");

        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                       ViewGroup.LayoutParams.WRAP_CONTENT));

        {
            Button b = new Button(this);
            b.setText(type);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        editMetadata(md);
                    } catch (JSONException e) {
                        Log.e(TAG, "can't edit. " + e);
                    }
                }
            });
            root.addView(b);
        }

        return root;
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
        ViewGroup vg = (ViewGroup) findViewById(R.id.editDynamicGroup);
        assignMetadataId(md);

        mMetadata.put(mMetadata.length(), md);

        vg.addView(createDynamicView(md));
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
