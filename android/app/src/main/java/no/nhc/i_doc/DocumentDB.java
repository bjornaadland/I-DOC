package no.nhc.i_doc;

import android.content.Context;
import java.util.List;

/**
 *  DocumentDB concerns the set of stored documents,
 *  and manages adding and removing of documents from the database.
 */
public abstract class DocumentDB
{
    private static volatile DocumentDB inst;

    protected DocumentDB() {}

    public static DocumentDB get(android.content.Context context) {
        if (inst == null) {
            inst = new CouchDocumentDB(context);
        }

        return inst;
    }

    /**
     *  Get the full list of stored documents.
     */
    abstract List<Document> getDocumentList();

    /**
     *  Create an empty document
     */
    abstract Document createDocument();

    /**
     *  Save a new or update an already existing Document
     */
    abstract void saveDocument(Document d);
}
