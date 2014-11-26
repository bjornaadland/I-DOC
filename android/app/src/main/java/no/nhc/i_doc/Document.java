package no.nhc.i_doc;

import java.util.List;
import java.util.ArrayList;


/**
 *  Document is a more or less abstract definition that
 *  represents an instance of "evidence".
 *  The Document has a title e.g. for display in a list of documents.
 *  Other details about documents are handled by other classes.
 */
public abstract interface Document
{
    public static class Metadata {}

    /**
     *  Retrieve Document title
     */
    abstract String getTitle();

    /**
     *  Set or update title
     */
    abstract void setTitle(String title);

    /**
     *  Retrieve the metadata associated with the document
     */
    abstract Metadata getMetadata();

    /**
     *  Set associated Metadata with the Document
     */
    abstract void setMetadata(Metadata data);

    /**
     * Get the files held by this document.
     */
    abstract List<String> getFiles();

    /**
     * Add a file to this document.
     */
    abstract void addFile(String file);
}
