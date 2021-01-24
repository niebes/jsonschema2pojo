/**
 * Copyright © 2010-2020 Nokia
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jsonschema2pojo.integration.config;

import java.io.File;
import org.jsonschema2pojo.integration.util.Jsonschema2PojoRule;
import org.junit.Rule;
import org.junit.Test;

import static org.jsonschema2pojo.integration.util.CodeGenerationHelper.config;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KotlinIT {

    @Rule
    public Jsonschema2PojoRule schemaRule = new Jsonschema2PojoRule();

    @Test
    public void kotlinFilesAreGeneratedAndJavaFilesAreNot() {
        File outputDirectory = schemaRule.generate("/schema/properties/primitiveProperties.json", "com.example",
                config("targetLanguage", "kotlin"));

        assertTrue(new File(outputDirectory, "com/example/PrimitiveProperties.kt").exists());
        assertFalse(new File(outputDirectory, "com/example/PrimitiveProperties.java").exists());
        assertFalse(new File(outputDirectory, "com/example/PrimitiveProperties.scala").exists());
    }
}
