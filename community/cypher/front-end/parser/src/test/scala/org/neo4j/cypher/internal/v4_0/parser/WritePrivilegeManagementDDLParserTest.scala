/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.v4_0.parser

import org.neo4j.cypher.internal.v4_0.ast

class WritePrivilegeManagementDDLParserTest extends DDLParserTestBase {

  Seq(
    ("GRANT", "TO", grant: privilegeFunc),
    ("DENY", "TO", deny: privilegeFunc),
    ("REVOKE", "FROM", revoke: privilegeFunc)
  ).foreach {
    case (command: String, preposition: String, func: privilegeFunc) =>

      Seq("GRAPH", "GRAPHS").foreach {
        graphKeyword =>

          Seq("ELEMENT", "ELEMENTS").foreach {
            elementKeyword =>

              Seq(
                ("*", ast.AllGraphsScope()(pos)),
                ("foo", ast.NamedGraphScope("foo")(pos)),
              ).foreach {
                case (dbName: String, graphScope: ast.GraphScope) =>

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword * $preposition role") {
                    yields(func(ast.WritePrivilege()(pos), ast.AllResource()(pos), graphScope, ast.AllQualifier() _, Seq("role")))
                  }

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword * (*) $preposition role") {
                    yields(func(ast.WritePrivilege()(pos), ast.AllResource()(pos), graphScope, ast.AllQualifier() _, Seq("role")))
                  }

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword * $preposition `r:ole`") {
                    yields(func(ast.WritePrivilege()(pos), ast.AllResource()(pos), graphScope, ast.AllQualifier() _, Seq("r:ole")))
                  }

                  // Missing `ON`

                  test(s"$command WRITE (*) $graphKeyword $dbName $elementKeyword * (*) $preposition role") {
                    failsToParse
                  }

                  // Missing role

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword * (*)") {
                    failsToParse
                  }

                  // Missing property definition

                  test(s"$command WRITE ON $graphKeyword $dbName $elementKeyword * $preposition role") {
                    failsToParse
                  }

                  test(s"$command WRITE ON $graphKeyword $dbName $elementKeyword * (*) $preposition role") {
                    failsToParse
                  }

                  // Invalid role name

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword * $preposition r:ole") {
                    failsToParse
                  }


                  // Does not support write on specific label/property yet

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword A $preposition role") {
                    failsToParse
                  }

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword A (*) $preposition role") {
                    failsToParse
                  }

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword * (foo) $preposition role") {
                    failsToParse
                  }

                  test(s"$command WRITE (*) ON $graphKeyword $dbName $elementKeyword A (foo) $preposition role") {
                    failsToParse
                  }

                  test(s"$command WRITE (prop) ON $graphKeyword $dbName $elementKeyword * $preposition role") {
                    failsToParse
                  }
              }

              test(s"$command WRITE (*) ON $graphKeyword `f:oo` $elementKeyword * $preposition role") {
                yields(func(ast.WritePrivilege()(pos), ast.AllResource() _, ast.NamedGraphScope("f:oo") _, ast.AllQualifier() _, Seq("role")))
              }

              // Invalid graph name

              test(s"$command WRITE (*) ON $graphKeyword f:oo $elementKeyword * $preposition role") {
                failsToParse
              }

              // Multiple graphs not allowed

              test(s"$command WRITE (*) ON $graphKeyword foo, baz $elementKeyword A (*) $preposition role") {
                failsToParse
              }

              test(s"$command WRITE (*) ON $graphKeyword $elementKeyword * (*) $preposition role") {
                failsToParse
              }
          }

          // Needs to be separate loop to avoid duplicate tests since the test does not have any $nodeKeyword
          Seq(
            ("*", ast.AllGraphsScope()(pos)),
            ("foo", ast.NamedGraphScope("foo")(pos))
          ).foreach {
            case (dbName: String, graphScope: ast.GraphScope) =>

              test(s"$command WRITE (*) ON $graphKeyword $dbName $preposition role") {
                yields(func(ast.WritePrivilege()(pos), ast.AllResource()(pos), graphScope, ast.AllQualifier() _, Seq("role")))
              }
          }
      }
  }
}
