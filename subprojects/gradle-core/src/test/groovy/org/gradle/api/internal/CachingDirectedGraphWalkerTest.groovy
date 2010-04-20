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



package org.gradle.api.internal

import spock.lang.Specification

class CachingDirectedGraphWalkerTest extends Specification {
    private final DirectedGraph<Integer, String> graph = Mock()
    private final CachingDirectedGraphWalker walker = new CachingDirectedGraphWalker(graph)

    def collectsValuesForASingleNode() {
        when:
        walker.add(1)
        def values = walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[1] << '1' }
        0 * _._
        values == ['1'] as Set
    }

    def collectsValuesForEachSuccessorNode() {
        when:
        walker.add(1)
        def values = walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[1] << '1'; args[2] << 2; args[2] << 3 }
        1 * graph.getNodeValues(2, _, _) >> { args -> args[1] << '2'; args[2] << 3 }
        1 * graph.getNodeValues(3, _, _) >> { args -> args[1] << '3' }
        3 * graph.getEdgeValues(_, _, _)
        0 * _._
        values == ['1', '2', '3'] as Set
    }

    def collectsValuesForEachEdgeTraversed() {
        when:
        walker.add(1)
        def values = walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[2] << 2; args[2] << 3 }
        1 * graph.getEdgeValues(1, 2, _) >> { args -> args[2] << '1->2' }
        1 * graph.getEdgeValues(1, 3, _) >> { args -> args[2] << '1->3' }
        1 * graph.getNodeValues(2, _, _) >> { args -> args[2] << 3 }
        1 * graph.getEdgeValues(2, 3, _) >> { args -> args[2] << '2->3' }
        1 * graph.getNodeValues(3, _, _)
        0 * _._
        values == ['1->2', '1->3', '2->3'] as Set
    }

    def collectsValuesForAllStartNodes() {
        when:
        walker.add(1, 2)
        def values = walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[1] << '1'; args[2] << 3 }
        1 * graph.getNodeValues(2, _, _) >> { args -> args[1] << '2'; args[2] << 3 }
        1 * graph.getNodeValues(3, _, _) >> { args -> args[1] << '3' }
        2 * graph.getEdgeValues(_, _, _)
        0 * _._
        values == ['1', '2', '3'] as Set
    }

    def collectsValuesWhenCycleIsPresentInGraph() {
        when:
        walker.add(1)
        def values = walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[1] << '1'; args[2] << 2 }
        1 * graph.getEdgeValues(1, 2, _) >> { args -> args[2] << '1->2' }
        1 * graph.getNodeValues(2, _, _) >> { args -> args[1] << '2'; args[2] << 3 }
        1 * graph.getEdgeValues(2, 3, _) >> { args -> args[2] << '2->3' }
        1 * graph.getNodeValues(3, _, _) >> { args -> args[1] << '3'; args[2] << 1 }
        1 * graph.getEdgeValues(3, 1, _) >> { args -> args[2] << '3->1' }
        0 * _._
        values == ['1', '1->2', '2', '2->3', '3', '3->1'] as Set
    }

    def collectsValuesWhenNodeConnectedToItself() {
        when:
        walker.add(1)
        def values = walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[1] << '1'; args[2] << 1 }
        1 * graph.getEdgeValues(1, 1, _) >> { args -> args[2] << '1->1' }
        0 * _._
        values == ['1', '1->1'] as Set
    }

    def collectsValuesWhenMultipleCyclesInGraph() {
        when:
        walker.add(1)
        def values = walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[1] << '1'; args[2] << 1; args[2] << 2 }
        1 * graph.getNodeValues(2, _, _) >> { args -> args[1] << '2'; args[2] << 3; args[2] << 4 }
        1 * graph.getNodeValues(3, _, _) >> { args -> args[1] << '3'; args[2] << 2 }
        1 * graph.getNodeValues(4, _, _) >> { args -> args[1] << '4' }
        5 * graph.getEdgeValues(_, _, _) >> { args -> args[2] << "${args[0]}->${args[1]}".toString() }
        0 * _._
        values == ['1', '1->1', '1->2', '2', '2->3', '2->4', '3', '3->2', '4'] as Set
    }

    def canReuseWalkerForMultipleSearches() {
        when:
        walker.add(1)
        def values = walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[1] << '1'; args[2] << 2; args[2] << 3 }
        1 * graph.getNodeValues(2, _, _) >> { args -> args[1] << '2'; args[2] << 3 }
        1 * graph.getNodeValues(3, _, _) >> { args -> args[1] << '3' }
        3 * graph.getEdgeValues(_, _, _)
        0 * _._
        values == ['1', '2', '3'] as Set


        when:
        walker.add(4)
        values = walker.findValues()

        then:
        1 * graph.getNodeValues(4, _, _) >> { args -> args[1] << '4'; args[2] << 1 }
        1 * graph.getEdgeValues(4, 1, _) >> { args -> args[2] << '4->1' }
        0 * _._
        values == ['4', '4->1', '1', '2', '3'] as Set

        when:
        walker.add(2)
        values = walker.findValues()

        then:
        values == ['2', '3'] as Set
    }

    def canReuseWalkerWhenGraphContainsACycle() {
        when:
        walker.add(1)
        walker.findValues()

        then:
        1 * graph.getNodeValues(1, _, _) >> { args -> args[1] << '1'; args[2] << 2 }
        1 * graph.getNodeValues(2, _, _) >> { args -> args[1] << '2'; args[2] << 3 }
        1 * graph.getNodeValues(3, _, _) >> { args -> args[1] << '3'; args[2] << 1; args[2] << 4 }
        1 * graph.getNodeValues(4, _, _) >> { args -> args[1] << '4'; args[2] << 2}
        5 * graph.getEdgeValues(_, _, _)
        0 * _._

        when:
        walker.add(1)
        def values = walker.findValues()

        then:
        values == ['1', '2', '3', '4'] as Set

        when:
        walker.add(2)
        values = walker.findValues()

        then:
        values == ['1', '2', '3', '4'] as Set

        when:
        walker.add(3)
        values = walker.findValues()

        then:
        values == ['1', '2', '3', '4'] as Set

        when:
        walker.add(4)
        values = walker.findValues()

        then:
        values == ['1', '2', '3', '4'] as Set
    }
}
