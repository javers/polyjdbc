/*
 * Copyright 2013 Adam Dubiel, Przemek Hertel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.polyjdbc.core.key;

import java.util.HashMap;
import java.util.Map;
import org.polyjdbc.core.dialect.Dialect;
import org.polyjdbc.core.dialect.DialectRegistry;

/**
 *
 * @author Adam Dubiel
 */
public final class KeyGeneratorRegistry {

    private static final Map<Class<?>, KeyGenerator> KEYGENS = new HashMap<Class<?>, KeyGenerator>();

    static {
        Dialect dialect = DialectRegistry.H2.getDialect();
        addKeyGenerator(dialect, new SequenceAllocation(dialect));

        dialect = DialectRegistry.POSTGRES.getDialect();
        addKeyGenerator(dialect, new SequenceAllocation(dialect));

        dialect = DialectRegistry.MYSQL.getDialect();
        addKeyGenerator(dialect, new AutoIncremented());

        dialect = DialectRegistry.ORACLE.getDialect();
        addKeyGenerator(dialect, new SequenceAllocation(dialect));

        dialect = DialectRegistry.MSSQL.getDialect();
        addKeyGenerator(dialect, new SequenceAllocation(dialect));
    }

    private KeyGeneratorRegistry() {
    }

    private static void addKeyGenerator(Dialect dialect, KeyGenerator keygen) {
        KEYGENS.put(dialect.getClass(), keygen);
    }

    public static boolean hasKeyGeneratorFor(Dialect dialect) {
        return KEYGENS.containsKey(dialect.getClass());
    }

    public static KeyGenerator keyGenerator(Dialect dialect) {
        if (!hasKeyGeneratorFor(dialect)) {
            throw new NoKeyGeneratorForDialectException(dialect);
        }
        return KEYGENS.get(dialect.getClass());
    }

}
