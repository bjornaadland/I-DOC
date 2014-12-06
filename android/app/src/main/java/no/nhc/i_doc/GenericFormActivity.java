package no.nhc.i_doc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 *  Activity wrapping the generic form fragment
 */
public class GenericFormActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent i = getIntent();
            Bundle b = i.getExtras();

            String title = b.getString("title");
            if (title != null) setTitle(title);

            // Show fragment
            GenericFormFragment gff = new GenericFormFragment();
            gff.setArguments(b);
            getFragmentManager().beginTransaction().add(android.R.id.content, gff, "form").commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        GenericFormFragment gff = (GenericFormFragment)getFragmentManager().findFragmentByTag("form");

        Intent intent = new Intent();
        intent.putExtra("object", gff.getResult().toString());
        setResult(RESULT_OK, intent);
    }
}
