package com.washer.backend.cloudbase.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.cloudbase.CloudBasePgClient;

import java.lang.reflect.Proxy;

public final class CloudBaseMapperFactory {

    private CloudBaseMapperFactory() {
    }

    public static <T> T create(Class<T> mapperType, CloudBasePgClient client, ObjectMapper objectMapper) {
        return mapperType.cast(Proxy.newProxyInstance(
            mapperType.getClassLoader(),
            new Class<?>[]{mapperType},
            new CloudBaseMapperInvocationHandler(mapperType, client, objectMapper)
        ));
    }
}
