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

        setContentView(R.layout.generic_form);

        if (savedInstanceState == null) {
            Intent i = getIntent();
            Bundle b = i.getExtras();

            String title = b.getString("title");
            if (title != null) setTitle(title);

            // Show fragment
            GenericFormFragment gff = new GenericFormFragment();
            gff.setArguments(b);
            getFragmentManager().beginTransaction().add(R.id.ScrollView01, gff, "form").commit();
        }
    }

    @Override
    public void onBackPressed() {
        produceResult();
        super.onBackPressed();
    }

    private void produceResult() {
        GenericFormFragment gff = (GenericFormFragment)getFragmentManager().findFragmentByTag("form");

        Intent intent = new Intent();
        intent.putExtra("object", gff.getResult().toString());
        setResult(RESULT_OK, intent);
    }
}
