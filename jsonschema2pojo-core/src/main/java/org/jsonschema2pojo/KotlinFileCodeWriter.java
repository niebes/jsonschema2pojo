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

package org.jsonschema2pojo;

import com.mysema.scalagen.ConversionSettings;
import com.mysema.scalagen.Converter;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.writer.FileCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.FileUtils;
import org.jsonschema2pojo.kotlin.KotlinConverter;

public class KotlinFileCodeWriter extends FileCodeWriter {
    final KotlinConverter kotlinConverter = new KotlinConverter();

    public KotlinFileCodeWriter(final File target, final String encoding) throws IOException {
        super(target, encoding);
    }

    public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
        final ByteArrayOutputStream javaSourceStream = new ByteArrayOutputStream();

        final String kotlinFileName = getFile(pkg, fileName).getAbsolutePath().replaceAll("\\.java$", ".kt");
        final String kotlinSource =kotlinConverter.convert(pkg, kotlinFileName);

        return new FilterOutputStream(javaSourceStream) {
            public void close() throws IOException {
                super.close();

                FileUtils.writeStringToFile(new File(kotlinFileName), kotlinSource, encoding);
            }
        };
    }

}
