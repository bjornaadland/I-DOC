package no.nhc.i_doc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class EditEvidenceActivity extends Activity {
    static final String TAG = "EditEvidenceActivity";

    private Document mDocument;

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

            DocumentDB.get(this).saveDocument(mDocument);
            Log.d(TAG, "... done saving");
        }
    }

    private void saveTestData() {
        Metadata person = DocumentDB.get(this).createMetadata(Metadata.Person.class);
        person.set(Metadata.Person.FirstName, "hey");
        person.set(Metadata.Person.LastName, "joe");

        {
            Metadata v = DocumentDB.get(this).createMetadata(Metadata.Victim.class);
            v.set(Metadata.Victim.Person, person);
            mDocument.addMetadata(v);
        }
        {
            Metadata s = DocumentDB.get(this).createMetadata(Metadata.Suspect.class);
            s.set(Metadata.Suspect.Person, person);
            mDocument.addMetadata(s);
        }
        {
            Metadata w = DocumentDB.get(this).createMetadata(Metadata.Witness.class);
            w.set(Metadata.Witness.Person, person);
            mDocument.addMetadata(w);
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

    private View createDynamicView(Metadata md) {
        java.lang.Class type = md.getType();
        TextView tv = new TextView(this);
        tv.setText(type.getSimpleName());
        return tv;
    }

    private java.lang.Class menuIdToMetaType(int id) {
        // BUG: there must exist a more dynamic way to map this....
        switch (id) {
        case R.id.edit_add_meta_victim:
            return Metadata.Victim.class;
        case R.id.edit_add_meta_suspect:
            return Metadata.Suspect.class;
        case R.id.edit_add_meta_witness:
            return Metadata.Witness.class;
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
