package no.nhc.i_doc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Spinner;
import android.widget.TextView;

public class EditEvidenceActivity extends Activity {
    static final String TAG = "EditEvidenceActivity";

    private Document mDocument;

    private static class TextMap {
        public Metadata mMetadata;
        public Enum mProperty;
        public EditText mEditText;
    }

    private java.util.List<TextMap> mTextMaps = new java.util.ArrayList<TextMap>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_evidence);

        mDocument = getDocument();

        EditText t = (EditText) findViewById(R.id.editTitle);

        if (mDocument != null) {
            Log.d(TAG, "setting title to " + mDocument.getTitle());
            t.setText(mDocument.getTitle(), TextView.BufferType.EDITABLE);

            buildDynamicForm();
        } else {
            Log.d(TAG, "no document");
            t.setText("[unknown evidence]", TextView.BufferType.NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDocument != null) {
            Log.d(TAG, "saving document...");
            EditText t = (EditText) findViewById(R.id.editTitle);
            mDocument.setTitle(t.getText().toString());

            for (TextMap tm : mTextMaps) {
                tm.mMetadata.set(tm.mProperty, tm.mEditText.getText().toString());
            }

            DocumentDB.get(this).saveDocument(mDocument);
            Log.d(TAG, "... done saving");
        }
    }

    private Document getDocument() {
        return DocumentDB.get(this).getDocument(getIntent().getData());
    }

    private void buildDynamicForm() {
        ViewGroup vg = (ViewGroup) findViewById(R.id.editDynamicGroup);
        for (Metadata md : mDocument.getMetadata()) {
            vg.addView(createDynamicView(md));
        }
    }

    /**
     *  Create an EditText for the specified property in the metadata md
     */
    private View createMappedText(Metadata md, Enum property, int hintResource) {
        TextMap tm = new TextMap();
        EditText et = new EditText(this);
        String prop = (String)md.get(property);

        et.setHint(hintResource);

        if (prop != null) {
            et.setText(prop);
        }

        tm.mMetadata = md;
        tm.mProperty = property;
        tm.mEditText = et;
        mTextMaps.add(tm);
        return et;
    }

    private View createMappedSpinner(Metadata md,
                                     Enum property,
                                     Metadata.PropertyType type,
                                     int hintResource) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<CharSequence> adapter = new
            ArrayAdapter(this, android.R.layout.simple_spinner_item,
                         DocumentDB.get(this).getValueSet(type.getType()));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        return spinner;
    }

    private View createUnsupported(String rep) {
        TextView tv = new TextView(this);
        tv.setText(rep);
        return tv;
    }

    private View createControl(Metadata md, Enum property, int hintResource) {
        Metadata.PropertyType pt = md.getPropertyType(property);
        java.lang.Class type = pt.getType();

        if (java.lang.CharSequence.class.isAssignableFrom(type)) {
            return createMappedText(md, property, hintResource);
        } else if (type.getEnclosingClass() == Value.class) {
            if (pt.isList()) {
                /* multiple choice */
                return createUnsupported("list of " + type.getSimpleName());
            } else {
                /* single choice */
                return createMappedSpinner(md, property, pt, hintResource);
            }
        } else {
            return createUnsupported("no UI for " + type.getSimpleName());
        }
    }

    /**
     *  Create a dynamic view for a metadata object
     */
    private View createDynamicView(Metadata md) {
        LinearLayout root = new LinearLayout(this);
        java.lang.Class type = md.getType();

        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                       ViewGroup.LayoutParams.WRAP_CONTENT));

        {
            TextView tv = new TextView(this);
            tv.setText(type.getSimpleName());
            root.addView(tv);
        }

        if (type == Metadata.Victim.class) {
            root.addView(createControl(md, Metadata.Victim.InterestsViolated, R.string.meta_prop_notes));
            root.addView(createControl(md, Metadata.Victim.ViolationType, R.string.meta_prop_notes));
            root.addView(createControl(md, Metadata.Victim.ParticularVulnerability, R.string.meta_prop_notes));
            root.addView(createControl(md, Metadata.Victim.OriginalCollection, R.string.meta_prop_notes));
            root.addView(createControl(md, Metadata.Victim.ICHLStatus, R.string.meta_prop_notes));
            root.addView(createControl(md, Metadata.Victim.RoleAndBelonging, R.string.meta_prop_notes));
            root.addView(createControl(md, Metadata.Victim.Notes, R.string.meta_prop_notes));

        } else if (type == Metadata.Witness.class) {
            root.addView(createControl(md, Metadata.Witness.Rank, R.string.meta_witness_rank));
            root.addView(createControl(md, Metadata.Witness.Notes, R.string.meta_prop_notes));
        } else if (type == Metadata.Suspect.class) {
            root.addView(createControl(md, Metadata.Suspect.Notes, R.string.meta_prop_notes));
        } else if (type == Metadata.ProtectedObject.class) {
            root.addView(createControl(md, Metadata.ProtectedObject.Name, R.string.meta_prop_name));
            root.addView(createControl(md, Metadata.ProtectedObject.Notes, R.string.meta_prop_notes));
        } else if (type == Metadata.Context.class) {
            root.addView(createControl(md, Metadata.Context.Name, R.string.meta_prop_name));
            root.addView(createControl(md, Metadata.Context.Notes, R.string.meta_prop_notes));
        } else if (type == Metadata.OrgUnit.class) {
            root.addView(createControl(md, Metadata.OrgUnit.Name, R.string.meta_prop_name));
            root.addView(createControl(md, Metadata.OrgUnit.Notes, R.string.meta_prop_notes));
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
    private void addMetadata(java.lang.Class type) {
        Metadata md = DocumentDB.get(this).createMetadata(type);
        ViewGroup vg = (ViewGroup) findViewById(R.id.editDynamicGroup);

        mDocument.addMetadata(md);
        vg.addView(createDynamicView(md));
    }

    public void showAddMetadataPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.edit_add_meta, popup.getMenu());
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                addMetadata(menuIdToMetaType(item.getItemId()));
                return true;
            }
        });
        popup.show();
    }
}
