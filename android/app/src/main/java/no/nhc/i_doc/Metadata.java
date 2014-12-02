package no.nhc.i_doc;

public interface Metadata {
    /**
     *  Listing of the properties of a Person
     */
    public static enum Person {
        FirstName, /* String */
        LastName   /* String */
    }

    /**
     *  Listing of the properties of a Victim
     */
    public static enum Victim {
        Person /* Person */
    }

    /**
     *  Listing of the properties of a Suspect
     */
    public static enum Suspect {
        Person /* Person */
    }

    /**
     *  Listing of the properties of a Witness
     */
    public static enum Witness {
        Person /* Person */
    }

    /**
     *  Listing of the properties of a ProtectedObject
     */
    public static enum ProtectedObject {
    }

    /**
     *  Listing of the properties of a Context
     */
    public static enum Context {
    }

    /**
     *  Listing of the properties of an OrgUnit
     */
    public static enum OrgUnit {
    }

    /**
     *  Return the actual type of this metadata object.
     *  The type will match one of the enum types.
     */
    java.lang.Class getType();

    /**
     *  Set a metadata property.
     *  All the properties must come from the same enum, that denotes the
     *  type of the Metadata object.
     */
    void set(Enum e, Object value);

    /**
     *  Get a metadata property.
     */
    Object get(Enum e);
}
