/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.api.internal.tasks

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.FileTreeInternal
import org.gradle.util.UsesNativeServices
import spock.lang.Specification

import java.util.concurrent.Callable

@UsesNativeServices
class DefaultTaskInputsTest extends Specification {
    private final File treeFile = new File('tree')
    private final tree = [getFiles: { [treeFile] as Set}] as FileTreeInternal
    private final FileResolver resolver = [
            resolve: { new File((String) it) },
            resolveFilesAsTree: {tree}
    ] as FileResolver

    private def taskStatusNagger = Stub(TaskMutator) {
        mutate(_, _) >> { String method, Runnable action -> action.run() }
    }
    private final DefaultTaskInputs inputs = new DefaultTaskInputs(resolver, "test", taskStatusNagger)

    def "default values"() {
        expect:
        inputs.files.empty
        inputs.properties.isEmpty()
        !inputs.hasInputs
        !inputs.hasSourceFiles
        inputs.sourceFiles.empty
    }

    def canRegisterInputFiles() {
        when:
        inputs.files('a')

        then:
        inputs.files.files == [new File('a')] as Set
    }

    def canRegisterInputDir() {
        when:
        inputs.dir('a')

        then:
        inputs.files.files == [treeFile] as Set
    }

    def "can register input file"() {
        when: inputs.includeFile("a")
        then:
        inputs.files.files.toList() == [new File('a')]
        inputs.fileProperties*.propertyName.toList() == ['$1']
        inputs.fileProperties*.files*.files.flatten() == [new File("a")]
    }

    def "can register input file with property name"() {
        when: inputs.includeFile("a").withPropertyName("prop")
        then:
        inputs.files.files.toList() == [new File('a')]
        inputs.fileProperties*.propertyName.toList() == ['prop']
        inputs.fileProperties*.files*.files.flatten() == [new File("a")]
    }

    def "can register input files"() {
        when: inputs.includeFiles("a", "b")
        then:
        inputs.files.files.toList() == [new File("a"), new File("b")]
        inputs.fileProperties*.propertyName == ['$1']
        inputs.fileProperties*.files*.files.flatten() == [new File("a"), new File("b")]
    }

    def "can register input files with property naem"() {
        when: inputs.includeFiles("a", "b").withPropertyName("prop")
        then:
        inputs.files.files.sort() == [new File("a"), new File("b")]
        inputs.fileProperties*.propertyName == ['prop']
        inputs.fileProperties*.files*.files.flatten() == [new File("a"), new File("b")]
    }

    def "can register input dir"() {
        when: inputs.includeDir("a")
        then:
        inputs.files.files.toList() == [treeFile]
        inputs.fileProperties*.propertyName == ['$1']
        inputs.fileProperties*.files*.files.flatten() == [treeFile]
    }

    def "can register input dir with property name"() {
        when: inputs.includeDir("a").withPropertyName("prop")
        then:
        inputs.files.files.toList() == [treeFile]
        inputs.fileProperties*.propertyName == ['prop']
        inputs.fileProperties*.files*.files.flatten() == [treeFile]
    }

    def canRegisterInputProperty() {
        when:
        inputs.property('a', 'value')

        then:
        inputs.properties == [a: 'value']
    }

    def canRegisterInputPropertyUsingAClosure() {
        when:
        inputs.property('a', { 'value' })

        then:
        inputs.properties == [a: 'value']
    }

    def canRegisterInputPropertyUsingACallable() {
        when:
        inputs.property('a', { 'value' } as Callable)

        then:
        inputs.properties == [a: 'value']
    }

    def canRegisterInputPropertyUsingAFileCollection() {
        def files = [new File('file')] as Set

        when:
        inputs.property('a', [getFiles: { files }] as FileCollection)

        then:
        inputs.properties == [a: files]
    }

    def inputPropertyCanBeNestedCallableAndClosure() {
        def files = [new File('file')] as Set
        def fileCollection = [getFiles: { files }] as FileCollection
        def callable = {fileCollection} as Callable

        when:
        inputs.property('a', { callable })

        then:
        inputs.properties == [a: files]
    }

    def "GString input property values are evaluated to avoid serialization issues"() {
        when:
        inputs.property('a', { "hey ${new NotSerializable()}" })

        then:
        inputs.properties == [a: "hey Joe"]
        String.is inputs.properties.a.class
    }

    class NotSerializable {
        String toString() { "Joe" }
    }

    def "can register source files"() {
        when: inputs.includeFiles("a", "b").withPropertyName("prop")
        then:
        inputs.hasInputs
        !inputs.hasSourceFiles

        when: inputs.includeFiles(["s1", "s2"]).skipWhenEmpty()
        then:
        inputs.hasSourceFiles
        inputs.files.files.toList() == [new File("a"), new File("b"), new File("s1"), new File("s2")]
        inputs.sourceFiles.files.toList() == [new File("s1"), new File("s2")]
        inputs.fileProperties*.propertyName == ['prop', '$1']
        inputs.fileProperties*.files*.files*.toList() == [[new File("a"), new File("b")], [new File("s1"), new File("s2")]]
        inputs.fileProperties*.skipWhenEmpty == [false, true]
    }

    def canRegisterSourceFile() {
        when:
        inputs.source('file')

        then:
        inputs.sourceFiles.files == ([new File('file')] as Set)
    }

    def canRegisterSourceFiles() {
        when:
        inputs.source('file', 'file2')

        then:
        inputs.sourceFiles.files == ([new File('file'), new File('file2')] as Set)
    }

    def canRegisterSourceDir() {
        when:
        inputs.sourceDir('dir')

        then:
        inputs.sourceFiles.files == [treeFile] as Set
    }

    def sourceFilesAreAlsoInputFiles() {
        when:
        inputs.source('file')

        then:
        inputs.sourceFiles.files == ([new File('file')] as Set)
        inputs.files.files == ([new File('file')] as Set)
    }

    def hasInputsWhenEmptyInputFilesRegistered() {
        when:
        inputs.files([])

        then:
        inputs.hasInputs
        !inputs.hasSourceFiles
    }

    def hasInputsWhenNonEmptyInputFilesRegistered() {
        when:
        inputs.files('a')

        then:
        inputs.hasInputs
        !inputs.hasSourceFiles
    }

    def hasInputsWhenInputPropertyRegistered() {
        when:
        inputs.property('a', 'value')

        then:
        inputs.hasInputs
        !inputs.hasSourceFiles
    }

    def hasInputsWhenEmptySourceFilesRegistered() {
        when:
        inputs.source([])

        then:
        inputs.hasInputs
        inputs.hasSourceFiles
    }

    def hasInputsWhenSourceFilesRegistered() {
        when:
        inputs.source('a')

        then:
        inputs.hasInputs
        inputs.hasSourceFiles
    }

    def "configuration actions are delayed"() {
        def action = Mock(Action)
        when:
        inputs.configure(action)

        then:
        0 * action.execute(_)

        when:
        inputs.ensureConfigured()
        then:
        1 * action.execute(inputs)
    }

    def "configuration actions can call configure"() {
        def action = Mock(Action)
        when:
        inputs.configure {
            it.configure(action)
        }

        then:
        0 * action.execute(_)

        when:
        inputs.ensureConfigured()
        then:
        1 * action.execute(inputs)
    }
}
