package com.digiledger.backend.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 支持字段既可以是数组，也可以直接传入单个值（例如 tagIds）
 */
public class ListOrSingleDeserializer extends StdDeserializer<List<Long>> {

    public ListOrSingleDeserializer() {
        super(List.class);
    }

    @Override
    public List<Long> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            return null;
        }
        if (p.isExpectedStartArrayToken()) {
            CollectionType listType = ctxt.getTypeFactory().constructCollectionType(List.class, Long.class);
            return ctxt.readValue(p, listType);
        }
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return List.of(p.getLongValue());
        }
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            String text = p.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return List.of(Long.parseLong(text));
            } catch (NumberFormatException ex) {
                throw JsonMappingException.from(p, "Unable to parse value as long: " + text, ex);
            }
        }
        // For any other token, attempt to deserialize as list
        CollectionType listType = ctxt.getTypeFactory().constructCollectionType(List.class, Long.class);
        return ctxt.readValue(p, listType);
    }
}
