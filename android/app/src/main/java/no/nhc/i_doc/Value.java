package no.nhc.i_doc;

/**
 *  Value class, used by Metadata, for properties that
 *  are restricted to a predefined set of values. These values
 *  are not hardcoded in the application, but fetched from the
 *  cloud. Therefore a special translation system is needed
 *  for display in the UI.
 */
public abstract class Value {
    public static class Interest extends Value {}
    public static class Violation extends Value {}
    public static class ViolationType extends Value {}
    public static class Vulnerability extends Value {}
    public static class OriginalCollection extends Value {}
    public static class ICHLStatus extends Value {}
    public static class RoleAndBelonging extends Value {}
    public static class WitnessType extends Value {}
    public static class Reliability extends Value {}
    public static class LegalStatus extends Value {}
    public static class NotorietyLevel extends Value {}
    public static class Typology extends Value {}
    public static class ImportanceLevel extends Value {}
    public static class ContextForm extends Value {}
    public static class InstitutionalBelonging extends Value {}
    public static class AgeCategory extends Value {}

    private Object mKey;

    public Object getKey() { return mKey; }
    public void setKey(Object key) { mKey = key; }

    public String toString() {
        return mKey.toString();
    }
}
