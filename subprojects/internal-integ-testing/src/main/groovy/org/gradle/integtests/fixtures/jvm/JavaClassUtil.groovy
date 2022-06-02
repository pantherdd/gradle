/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.integtests.fixtures.jvm

import groovy.transform.CompileStatic

@CompileStatic
class JavaClassUtil {

    static int getClassMajorVersion(Class<?> javaClass) {
        def classPath = javaClass.name.replace('.', '/') + '.class'
        def data = new DataInputStream(javaClass.classLoader.getResourceAsStream(classPath))
        try {
            int magicBytes = (int) 0xCAFEBABE
            int header = data.readInt()
            if (magicBytes != header) {
                throw new IOException("Invalid class header $header")
            }
            data.readUnsignedShort() // minor
            return data.readUnsignedShort() // major
        } finally {
            data.close()
        }
    }

    private JavaClassUtil() {}
}
