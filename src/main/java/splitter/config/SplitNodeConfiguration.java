package splitter.config;

import java.util.*;

public class SplitNodeConfiguration {
    private static final String START_INDEX = "startIndex";
    private static final String INDEX_PROPERTY_NAME = "indexProperty";
    private static final String RELATIONSHIP_TYPES = "relationshipTypes";


    private final String indexPropertyName;
    private int startIndex;
    private HashSet<String> relationshipTypes;

    public static SplitNodeConfiguration build(Map<String,Object> configuration) {
        return new SplitNodeConfiguration(configuration);
    }

    private SplitNodeConfiguration(Map<String,Object> configuration) {
        Object indexPropertyName = configuration.get(INDEX_PROPERTY_NAME);
        this.indexPropertyName = indexPropertyName == null ? null : indexPropertyName.toString();

        parseStartIndex(configuration);
        parseRelationshipTypes(configuration);
    }

    public String getIndexPropertyName() {
        return this.indexPropertyName;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public Set<String> getRelationshipTypes() {
        return this.relationshipTypes;
    }

    private void parseStartIndex(Map<String,Object> configuration) throws RuntimeException {
        Object startIndex = configuration.get(START_INDEX);
        if (startIndex != null) {
            try {
                this.startIndex = Integer.parseInt(startIndex.toString());
            }
            catch (NumberFormatException e)
            {
                throw new RuntimeException("Unable to parse " + START_INDEX + "value");
            }
        }
    }

    private void parseRelationshipTypes(Map<String,Object> configuration) throws RuntimeException {
        Object relationshipTypesValue = configuration.get(RELATIONSHIP_TYPES);
        if (relationshipTypesValue == null) {
            relationshipTypes = new HashSet<>(0);
        }
        else if (relationshipTypesValue instanceof ArrayList) {
            ArrayList<String> values = (ArrayList<String>)relationshipTypesValue;
            relationshipTypes = new HashSet<>(values);
        }
        else {
            throw new RuntimeException("Unable to parse " + RELATIONSHIP_TYPES + "value");
        }
    }
}
