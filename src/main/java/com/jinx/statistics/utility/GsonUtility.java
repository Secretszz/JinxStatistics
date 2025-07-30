package com.jinx.statistics.utility;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import lombok.Getter;

public class GsonUtility {

    @Getter
    static Gson gson;

    private GsonUtility() {
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static<T> T fromJson(String json, Class<T> classOfT){
        return gson.fromJson(json, classOfT);
    }

    static {
        gson = (new GsonBuilder()).disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).addSerializationExclusionStrategy(new ExclusionStrategy() {
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                Expose expose = fieldAttributes.getAnnotation(Expose.class);
                return expose != null && !expose.serialize();
            }

            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).addDeserializationExclusionStrategy(new ExclusionStrategy() {
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                Expose expose = fieldAttributes.getAnnotation(Expose.class);
                return expose != null && !expose.deserialize();
            }

            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).create();
    }
}
