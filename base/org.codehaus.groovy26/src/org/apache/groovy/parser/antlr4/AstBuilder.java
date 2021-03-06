/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.groovy.parser.antlr4;

import groovy.lang.Tuple2;
import groovyjarjarantlr4.v4.runtime.ANTLRErrorListener;
import groovyjarjarantlr4.v4.runtime.CharStream;
import groovyjarjarantlr4.v4.runtime.CharStreams;
import groovyjarjarantlr4.v4.runtime.CommonTokenStream;
import groovyjarjarantlr4.v4.runtime.RecognitionException;
import groovyjarjarantlr4.v4.runtime.Recognizer;
import groovyjarjarantlr4.v4.runtime.Token;
import groovyjarjarantlr4.v4.runtime.atn.PredictionMode;
import groovyjarjarantlr4.v4.runtime.misc.ParseCancellationException;
import groovyjarjarantlr4.v4.runtime.tree.ParseTree;
import groovyjarjarantlr4.v4.runtime.tree.TerminalNode;
import org.apache.groovy.parser.antlr4.internal.AtnManager;
import org.apache.groovy.parser.antlr4.internal.DescriptiveErrorStrategy;
import org.apache.groovy.parser.antlr4.util.PositionConfigureUtils;
import org.apache.groovy.parser.antlr4.util.StringUtils;
import org.apache.groovy.util.Maps;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.antlr.EnumHelper;
import org.codehaus.groovy.antlr.LocationSupport;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.EnumConstantClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.ImmutableClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.NodeMetaDataHandler;
import org.codehaus.groovy.ast.PackageNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ClosureListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.LambdaExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.MethodReferenceExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PostfixExpression;
import org.codehaus.groovy.ast.expr.PrefixExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.codehaus.groovy.ast.expr.SpreadMapExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.UnaryMinusExpression;
import org.codehaus.groovy.ast.expr.UnaryPlusExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.AssertStatement;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ContinueStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.codehaus.groovy.syntax.Numbers;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Types;
import groovyjarjarasm.asm.Opcodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.apache.groovy.parser.antlr4.GroovyLangParser.ADD;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AS;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AdditiveExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AndExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AnnotatedQualifiedClassNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AnnotationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AnnotationNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AnnotationsOptContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AnonymousInnerClassDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ArgumentsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ArrayInitializerContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AssertStatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AssertStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.AssignmentExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BlockContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BlockStatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BlockStatementsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BlockStatementsOptContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BooleanLiteralAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BreakStatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BreakStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BuiltInTypeContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.BuiltInTypePrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CASE;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CastExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CastParExpressionContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CatchClauseContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CatchTypeContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassBodyContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassBodyDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassOrInterfaceModifierContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassOrInterfaceModifiersContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassOrInterfaceModifiersOptContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassOrInterfaceTypeContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassicalForControlContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClassifiedModifiersContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClosureContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ClosurePrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CommandArgumentContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CommandExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CommandExpressionContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CompilationUnitContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ConditionalExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ConditionalStatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ConditionalStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ContinueStatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ContinueStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CreatedNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.CreatorContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.DEC;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.DEF;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.DEFAULT;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.DimsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.DimsOptContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.DoWhileStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.DynamicMemberNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ElementValueArrayInitializerContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ElementValueContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ElementValuePairContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ElementValuePairsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ElementValuesContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.EnhancedArgumentListContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.EnhancedArgumentListElementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.EnhancedForControlContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.EnhancedStatementExpressionContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.EnumConstantContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.EnumConstantsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.EqualityExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ExclusiveOrExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ExpressionContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ExpressionInParContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ExpressionListContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ExpressionListElementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ExpressionStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.FieldDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.FinallyBlockContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.FloatingPointLiteralAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ForControlContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ForInitContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ForStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ForUpdateContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.FormalParameterContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.FormalParameterListContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.FormalParametersContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.GE;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.GT;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.GroovyParserRuleContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.GstringContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.GstringPathContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.GstringPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.GstringValueContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.IN;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.INC;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.INSTANCEOF;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.IdentifierContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.IdentifierPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.IfElseStatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ImportDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ImportStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.InclusiveOrExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.IndexPropertyArgsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.IntegerLiteralAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.KeywordsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LE;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LT;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LabeledStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LambdaBodyContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LambdaPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ListContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ListPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LiteralPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LocalVariableDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LocalVariableDeclarationStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LogicalAndExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LogicalOrExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.LoopStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MapContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MapEntryContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MapEntryLabelContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MapEntryListContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MapPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MemberDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MethodBodyContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MethodDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MethodDeclarationStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MethodNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ModifierContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ModifiersContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ModifiersOptContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MultipleAssignmentExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.MultiplicativeExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.NOT_IN;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.NOT_INSTANCEOF;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.NamePartContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.NamedPropertyArgsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.NewPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.NonWildcardTypeArgumentsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.NormalExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.NullLiteralAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.PRIVATE;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.PackageDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ParExpressionContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ParenPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.PathElementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.PathExpressionContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.PostfixExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.PostfixExpressionContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.PowerExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.PrimitiveTypeContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.QualifiedClassNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.QualifiedClassNameListContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.QualifiedNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.QualifiedNameElementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.QualifiedStandardClassNameContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.RegexExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.RelationalExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ResourceContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ResourceListContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ResourcesContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ReturnStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ReturnTypeContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.STATIC;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.SUB;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ShiftExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.StandardLambdaExpressionContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.StandardLambdaParametersContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.StatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.StatementsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.StringLiteralAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.StringLiteralContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.SuperPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.SwitchBlockStatementGroupContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.SwitchLabelContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.SwitchStatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.SynchronizedStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ThisFormalParameterContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ThisPrmrAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.ThrowStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TryCatchStatementContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TryCatchStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeArgumentContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeArgumentsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeArgumentsOrDiamondContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeBoundContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeDeclarationStmtAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeListContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeNamePairContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeNamePairsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeParameterContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.TypeParametersContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.UnaryAddExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.UnaryNotExprAltContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableDeclarationContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableDeclaratorContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableDeclaratorIdContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableDeclaratorsContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableInitializerContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableInitializersContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableModifierContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableModifiersContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableModifiersOptContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.VariableNamesContext;
import static org.apache.groovy.parser.antlr4.GroovyLangParser.WhileStmtAltContext;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.last;

/**
 * Building the AST from the parse tree generated by Antlr4.
 */
public class AstBuilder extends GroovyParserBaseVisitor<Object> {

    public AstBuilder(SourceUnit sourceUnit) {
        this.sourceUnit = sourceUnit;
        this.moduleNode = new ModuleNode(sourceUnit);

        CharStream charStream = createCharStream(sourceUnit);

        this.lexer = new GroovyLangLexer(charStream);
        this.parser = new GroovyLangParser(new CommonTokenStream(this.lexer));
        this.parser.setErrorHandler(new DescriptiveErrorStrategy(charStream));

        this.tryWithResourcesASTTransformation = new TryWithResourcesASTTransformation(this);
        this.groovydocManager = GroovydocManager.getInstance();

        // GRECLIPSE add
        try (BufferedReader reader = new BufferedReader(sourceUnit.getSource().getReader())) {
            // TODO: Can this be done without boxing/unboxing offsets or juggling temp arrays?
            int chr, off = 0; List<Integer> ends = new ArrayList<>(32); ends.add(0);
            while ((chr = reader.read()) != -1) { off += 1;
                if (chr == '\n') ends.add(off);
            }
            ends.add(off);

            int[] arr = new int[ends.size()];
            for (int i = 0, n = arr.length; i < n; i += 1) {
                arr[i] = ends.get(i);
            }
            this.locationSupport = new LocationSupport(arr);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred reading the source code.", e);
        }
        // GRECLIPSE end
    }

    // GRECLIPSE add
    private <T extends ASTNode> T configureAST(T astNode) {
        if (astNode.getLineNumber() > 0) {
            astNode.setStart(locationSupport.findOffset(astNode.getLineNumber(), astNode.getColumnNumber()));
            astNode.setEnd(locationSupport.findOffset(astNode.getLastLineNumber(), astNode.getLastColumnNumber()));
        }
        return astNode;
    }

    private <T extends ASTNode> T configureAST(T astNode, Token t) {
        PositionConfigureUtils.configureAST(astNode, t);
        return configureAST(astNode);
    }

    private <T extends ASTNode> T configureAST(T astNode, TerminalNode n) {
        PositionConfigureUtils.configureAST(astNode, n);
        return configureAST(astNode);
    }

    private <T extends ASTNode> T configureAST(T astNode, ASTNode source) {
        PositionConfigureUtils.configureAST(astNode, source);
        return configureAST(astNode);
    }

    private <T extends ASTNode> T configureAST(T astNode, ASTNode start, ASTNode until) {
        PositionConfigureUtils.configureAST(astNode, start, until);
        return configureAST(astNode);
    }

    private <T extends ASTNode> T configureAST(T astNode, GroovyParser.GroovyParserRuleContext ctx) {
        PositionConfigureUtils.configureAST(astNode, ctx);
        return configureAST(astNode);
    }

    private <T extends ASTNode> T configureAST(T astNode, GroovyParser.GroovyParserRuleContext ctx, ASTNode until) {
        PositionConfigureUtils.configureAST(astNode, ctx, until);
        return configureAST(astNode);
    }
    // GRECLIPSE end

    private CharStream createCharStream(SourceUnit sourceUnit) {
        CharStream charStream;

        try {
            charStream = CharStreams.fromReader(
                    new BufferedReader(sourceUnit.getSource().getReader()),
                    sourceUnit.getName());
        } catch (IOException e) {
            throw new RuntimeException("Error occurred when reading source code.", e);
        }

        return charStream;
    }

    private GroovyParserRuleContext buildCST() throws CompilationFailedException {
        GroovyParserRuleContext result;

        try {
            // parsing have to wait util clearing is complete.
            AtnManager.RRWL.readLock().lock();
            try {
                result = buildCST(PredictionMode.SLL);
            } catch (Throwable t) {
                // if some syntax error occurred in the lexer, no need to retry the powerful LL mode
                if (t instanceof GroovySyntaxError && GroovySyntaxError.LEXER == ((GroovySyntaxError) t).getSource()) {
                    throw t;
                }

                result = buildCST(PredictionMode.LL);
            } finally {
                AtnManager.RRWL.readLock().unlock();
            }
        } catch (Throwable t) {
            throw convertException(t);
        }

        return result;
    }

    private GroovyParserRuleContext buildCST(PredictionMode predictionMode) {
        parser.getInterpreter().setPredictionMode(predictionMode);

        if (PredictionMode.SLL.equals(predictionMode)) {
            this.removeErrorListeners();
        } else {
            parser.getInputStream().seek(0);
            this.addErrorListeners();
        }

        return parser.compilationUnit();
    }

    private CompilationFailedException convertException(Throwable t) {
        CompilationFailedException cfe;

        if (t instanceof CompilationFailedException) {
            cfe = (CompilationFailedException) t;
        } else if (t instanceof ParseCancellationException) {
            cfe = createParsingFailedException(t.getCause());
        } else {
            cfe = createParsingFailedException(t);
        }

        return cfe;
    }

    public ModuleNode buildAST() {
        try {
            return (ModuleNode) this.visit(this.buildCST());
        } catch (Throwable t) {
            throw convertException(t);
        }
    }

    @Override
    public ModuleNode visitCompilationUnit(CompilationUnitContext ctx) {
        this.visit(ctx.packageDeclaration());

        for (ASTNode e : this.visitStatements(ctx.statements())) {
            if (e instanceof DeclarationListStatement) { // local variable declaration
                for (Statement ds : ((DeclarationListStatement) e).getDeclarationStatements()) {
                    moduleNode.addStatement(ds);
                }
            } else if (e instanceof Statement) {
                moduleNode.addStatement((Statement) e);
            } else if (e instanceof MethodNode) { // script method
                moduleNode.addMethod((MethodNode) e);
            }
        }
        for (ClassNode cl : this.classNodeList) {
            moduleNode.addClass(cl);
        }
        if (this.isPackageInfoDeclaration()) {
            this.addPackageInfoClassNode();
        } else {
            // if groovy source file only contains blank(including EOF), add "return null" to the AST
            if (this.isBlankScript()) {
                this.addEmptyReturnStatement();
            }
        }

        this.configureScriptClassNode();

        if (null != this.numberFormatError) {
            throw createParsingFailedException(this.numberFormatError.getSecond().getMessage(), this.numberFormatError.getFirst());
        }

        // GRECLIPSE add
        moduleNode.setLineNumber(1);
        moduleNode.setColumnNumber(1);
        moduleNode.setEnd(locationSupport.getEnd());
        moduleNode.setLastLineNumber(locationSupport.getEndLine());
        moduleNode.setLastColumnNumber(locationSupport.getEndColumn());
        BlockStatement blockStatement = moduleNode.getStatementBlock();
        if (!blockStatement.isEmpty() || !moduleNode.getMethods().isEmpty()) {
            ASTNode alpha = findAlpha(blockStatement, moduleNode.getMethods());
            ASTNode omega = findOmega(blockStatement, moduleNode.getMethods());
            if (!blockStatement.isEmpty()) {
                blockStatement.setStart(alpha.getStart());
                blockStatement.setLineNumber(alpha.getLineNumber());
                blockStatement.setColumnNumber(alpha.getColumnNumber());
                blockStatement.setEnd(omega.getEnd());
                blockStatement.setLastLineNumber(omega.getLastLineNumber());
                blockStatement.setLastColumnNumber(omega.getLastColumnNumber());
            }
            if (!moduleNode.getClasses().isEmpty()) {
                ClassNode scriptClass = moduleNode.getClasses().get(0);
                scriptClass.setStart(alpha.getStart());
                scriptClass.setLineNumber(alpha.getLineNumber());
                scriptClass.setColumnNumber(alpha.getColumnNumber());
                scriptClass.setEnd(omega.getEnd());
                scriptClass.setLastLineNumber(omega.getLastLineNumber());
                scriptClass.setLastColumnNumber(omega.getLastColumnNumber());

                // fix the run method to contain the start and end locations of the statement block
                MethodNode runMethod = scriptClass.getDeclaredMethod("run", Parameter.EMPTY_ARRAY);
                runMethod.setStart(alpha.getStart());
                runMethod.setLineNumber(alpha.getLineNumber());
                runMethod.setColumnNumber(alpha.getColumnNumber());
                runMethod.setEnd(omega.getEnd());
                runMethod.setLastLineNumber(omega.getLastLineNumber());
                runMethod.setLastColumnNumber(omega.getLastColumnNumber());
            }
        }
        moduleNode.putNodeMetaData(LocationSupport.class, locationSupport);
        sourceUnit.setComments(lexer.getComments());
        // GRECLIPSE end

        return moduleNode;
    }

    // GRECLIPSE add
    /** Returns the first method or statement node in the script. */
    private ASTNode findAlpha(BlockStatement blockStatement, List<MethodNode> methods) {
        MethodNode method = (!methods.isEmpty() ? methods.get(0) : null);
        Statement statement = (!blockStatement.isEmpty() ? blockStatement.getStatements().get(0) : null);
        if (method == null && (statement == null || (statement.getStart() == 0 && statement.getLength() == 0))) {
            // a script with no methods or statements; use a synthetic statement after the end of the package declaration/import statements
            statement = createEmptyScriptStatement();
        }
        int statementStart = (statement != null ? statement.getStart() : Integer.MAX_VALUE);
        int methodStart = (method != null ? method.getStart() : Integer.MAX_VALUE);
        return (statementStart <= methodStart ? statement : method);
    }

    /** Returns the final method or statement node in the script. */
    private ASTNode findOmega(BlockStatement blockStatement, List<MethodNode> methods) {
        MethodNode method = (!methods.isEmpty() ? last(methods) : null);
        Statement statement = (!blockStatement.isEmpty() ? last(blockStatement.getStatements()) : null);
        if (method == null && (statement == null || (statement.getStart() == 0 && statement.getLength() == 0))) {
            // a script with no methods or statements; add a synthetic statement after the end of the package declaration/import statements
            statement = createEmptyScriptStatement();
        }
        int statementStart = (statement != null ? statement.getEnd() : Integer.MIN_VALUE);
        int methodStart = (method != null ? method.getStart() : Integer.MIN_VALUE);
        return (statementStart >= methodStart ? statement : method);
    }

    private Statement createEmptyScriptStatement() {
        Statement statement = ReturnStatement.RETURN_NULL_OR_VOID;
        ASTNode target = null;
        if (asBoolean(moduleNode.getImports())) {
            target = last(moduleNode.getImports());
        } else if (moduleNode.hasPackage()) {
            target = moduleNode.getPackage();
        }
        if (target != null) {
            // import/package nodes do not include trailing semicolon, so use end of line instead of end of node
            int off = Math.min(locationSupport.findOffset(target.getLastLineNumber() + 1, 1), locationSupport.getEnd() - 1);
            int[] row_col = locationSupport.getRowCol(off);

            statement = new ReturnStatement(ConstantExpression.NULL);
            statement.setStart(off);
            statement.setEnd(off);
            statement.setLineNumber(row_col[0]);
            statement.setColumnNumber(row_col[1]);
            statement.setLastLineNumber(row_col[0]);
            statement.setLastColumnNumber(row_col[1]);
        }
        return statement;
    }
    // GRECLIPSE end

    @Override
    public List<ASTNode> visitStatements(StatementsContext ctx) {
        if (!asBoolean(ctx)) {
            return Collections.emptyList();
        }

        List<ASTNode> nodeList = new ArrayList<>(ctx.statement().size());

        for (StatementContext statementContext : ctx.statement()) {
            nodeList.add((ASTNode) this.visit(statementContext));
        }

        return nodeList;
    }

    @Override
    public PackageNode visitPackageDeclaration(PackageDeclarationContext ctx) {
        String packageName = this.visitQualifiedName(ctx.qualifiedName());
        moduleNode.setPackageName(packageName + DOT_STR);

        PackageNode packageNode = moduleNode.getPackage();

        packageNode.addAnnotations(this.visitAnnotationsOpt(ctx.annotationsOpt()));

        // GRECLIPSE edit
        //return configureAST(packageNode, ctx);
        return configureAST(packageNode, ctx.qualifiedName());
        // GRECLIPSE end
    }

    @Override
    public ImportNode visitImportDeclaration(ImportDeclarationContext ctx) {
        ImportNode importNode;

        boolean hasStatic = asBoolean(ctx.STATIC());
        boolean hasStar = asBoolean(ctx.MUL());
        boolean hasAlias = asBoolean(ctx.alias);

        List<AnnotationNode> annotationNodeList = this.visitAnnotationsOpt(ctx.annotationsOpt());

        if (hasStatic) {
            if (hasStar) { // e.g. import static java.lang.Math.*
                String qualifiedName = this.visitQualifiedName(ctx.qualifiedName());
                ClassNode type = ClassHelper.make(qualifiedName);
                // GRECLIPSE edit
                //configureAST(type, ctx);
                ASTNode typeNode = configureAST(type instanceof ImmutableClassNode ? new ClassExpression(type) : type, ctx.qualifiedName());
                // GRECLIPSE end

                moduleNode.addStaticStarImport(type.getText(), type, annotationNodeList);

                importNode = last(moduleNode.getStaticStarImports().values());
                // GRECLIPSE add
                if (importNode.getType() != typeNode) {
                    importNode.setNodeMetaData(ClassExpression.class, typeNode);
                }
                // GRECLIPSE end
            } else { // e.g. import static java.lang.Math.pow
                // GRECLIPSE edit
                //List<QualifiedNameElementContext> identifierList = new LinkedList<>(ctx.qualifiedName().qualifiedNameElement());
                List<? extends QualifiedNameElementContext> identifierList = ctx.qualifiedName().qualifiedNameElement();
                // GRECLIPSE end
                int identifierListSize = identifierList.size();
                String name = identifierList.get(identifierListSize - 1).getText();
                StringBuilder builder = new StringBuilder();
                long limit = identifierListSize - 1;
                for (GroovyParserRuleContext groovyParserRuleContext : identifierList) {
                    if (limit-- == 0) break;
                    String text = groovyParserRuleContext.getText();
                    if (builder.length() > 0) {
                        builder.append(DOT_STR);
                    }
                    builder.append(text);
                }
                ClassNode classNode =
                        ClassHelper.make(
                                builder.toString());
                String alias = hasAlias
                        ? ctx.alias.getText()
                        : name;
                // GRECLIPSE edit
                //configureAST(classNode, ctx);
                ASTNode typeNode = PositionConfigureUtils.configureAST(classNode instanceof ImmutableClassNode ? new ClassExpression(classNode) : classNode, ctx.qualifiedName());
                PositionConfigureUtils.configureEndPosition(typeNode, identifierList.get(Math.max(0, identifierListSize - 2)).getStop());
                configureAST(typeNode);
                // GRECLIPSE end

                moduleNode.addStaticImport(classNode, name, alias, annotationNodeList);

                importNode = last(moduleNode.getStaticImports().values());
                // GRECLIPSE add
                ConstantExpression nameExpr = new ConstantExpression(name);
                configureAST(nameExpr, last(identifierList));
                importNode.setFieldNameExpr(nameExpr);
                if (hasAlias) {
                    ConstantExpression aliasExpr = new ConstantExpression(alias);
                    configureAST(aliasExpr, ctx.alias);
                    importNode.setAliasExpr(aliasExpr);
                }
                if (importNode.getType() != typeNode) {
                    importNode.setNodeMetaData(ClassExpression.class, typeNode);
                }
                // GRECLIPSE end
            }
        } else {
            if (hasStar) { // e.g. import java.util.*
                String qualifiedName = this.visitQualifiedName(ctx.qualifiedName());

                moduleNode.addStarImport(qualifiedName + DOT_STR, annotationNodeList);

                importNode = last(moduleNode.getStarImports());
            } else { // e.g. import java.util.Map
                String qualifiedName = this.visitQualifiedName(ctx.qualifiedName());
                String name = last(ctx.qualifiedName().qualifiedNameElement()).getText();
                ClassNode classNode = ClassHelper.make(qualifiedName);
                String alias = hasAlias
                        ? ctx.alias.getText()
                        : name;
                // GRECLIPSE edit
                //configureAST(classNode, ctx);
                ASTNode typeNode = configureAST(classNode instanceof ImmutableClassNode ? new ClassExpression(classNode) : classNode, last(ctx.qualifiedName().qualifiedNameElement()));
                // GRECLIPSE end

                moduleNode.addImport(alias, classNode, annotationNodeList);

                importNode = last(moduleNode.getImports());
                // GRECLIPSE add
                if (hasAlias) {
                    ConstantExpression aliasExpr = new ConstantExpression(alias);
                    configureAST(aliasExpr, ctx.alias);
                    importNode.setAliasExpr(aliasExpr);
                }
                if (importNode.getType() != typeNode) {
                    importNode.setNodeMetaData(ClassExpression.class, typeNode);
                }
                // GRECLIPSE end
            }
        }

        return configureAST(importNode, ctx);
    }

    // statement {    --------------------------------------------------------------------
    @Override
    public AssertStatement visitAssertStatement(AssertStatementContext ctx) {
        visitingAssertStatementCnt++;

        Expression conditionExpression = (Expression) this.visit(ctx.ce);

        if (conditionExpression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) conditionExpression;

            if (binaryExpression.getOperation().getType() == Types.ASSIGN) {
                throw createParsingFailedException("Assignment expression is not allowed in the assert statement", conditionExpression);
            }
        }

        BooleanExpression booleanExpression =
                configureAST(
                        new BooleanExpression(conditionExpression), conditionExpression);

        if (!asBoolean(ctx.me)) {
            return configureAST(
                    new AssertStatement(booleanExpression), ctx);
        }

        AssertStatement result = configureAST(new AssertStatement(booleanExpression,
                        (Expression) this.visit(ctx.me)),
                ctx);

        visitingAssertStatementCnt--;

        return result;
    }

    @Override
    public AssertStatement visitAssertStmtAlt(AssertStmtAltContext ctx) {
        return configureAST(this.visitAssertStatement(ctx.assertStatement()), ctx);
    }

    @Override
    public Statement visitConditionalStmtAlt(ConditionalStmtAltContext ctx) {
        return configureAST(this.visitConditionalStatement(ctx.conditionalStatement()), ctx);
    }

    @Override
    public Statement visitConditionalStatement(ConditionalStatementContext ctx) {
        if (asBoolean(ctx.ifElseStatement())) {
            return configureAST(this.visitIfElseStatement(ctx.ifElseStatement()), ctx);
        } else if (asBoolean(ctx.switchStatement())) {
            return configureAST(this.visitSwitchStatement(ctx.switchStatement()), ctx);
        }

        throw createParsingFailedException("Unsupported conditional statement", ctx);
    }

    @Override
    public IfStatement visitIfElseStatement(IfElseStatementContext ctx) {
        Expression conditionExpression = this.visitExpressionInPar(ctx.expressionInPar());
        BooleanExpression booleanExpression =
                configureAST(
                        new BooleanExpression(conditionExpression), conditionExpression);

        Statement ifBlock =
                this.unpackStatement(
                        (Statement) this.visit(ctx.tb));
        Statement elseBlock =
                this.unpackStatement(
                        asBoolean(ctx.ELSE())
                                ? (Statement) this.visit(ctx.fb)
                                : EmptyStatement.INSTANCE);

        return configureAST(new IfStatement(booleanExpression, ifBlock, elseBlock), ctx);
    }


    @Override
    public Statement visitLoopStmtAlt(LoopStmtAltContext ctx) {
        visitingLoopStatementCnt++;
        Statement result = configureAST((Statement) this.visit(ctx.loopStatement()), ctx);
        visitingLoopStatementCnt--;

        return result;
    }

    @Override
    public ForStatement visitForStmtAlt(ForStmtAltContext ctx) {
        Tuple2<Parameter, Expression> controlPair = this.visitForControl(ctx.forControl());

        Statement loopBlock = this.unpackStatement((Statement) this.visit(ctx.statement()));

        return configureAST(
                new ForStatement(controlPair.getFirst(), controlPair.getSecond(), asBoolean(loopBlock) ? loopBlock : EmptyStatement.INSTANCE),
                ctx);
    }

    @Override
    public Tuple2<Parameter, Expression> visitForControl(ForControlContext ctx) {
        if (asBoolean(ctx.enhancedForControl())) { // e.g. for(int i in 0..<10) {}
            return this.visitEnhancedForControl(ctx.enhancedForControl());
        }

        if (asBoolean(ctx.classicalForControl())) { // e.g. for(int i = 0; i < 10; i++) {}
            return this.visitClassicalForControl(ctx.classicalForControl());
        }

        throw createParsingFailedException("Unsupported for control: " + ctx.getText(), ctx);
    }

    @Override
    public Expression visitForInit(ForInitContext ctx) {
        if (!asBoolean(ctx)) {
            return EmptyExpression.INSTANCE;
        }

        if (asBoolean(ctx.localVariableDeclaration())) {
            DeclarationListStatement declarationListStatement = this.visitLocalVariableDeclaration(ctx.localVariableDeclaration());
            List<? extends Expression> declarationExpressionList = declarationListStatement.getDeclarationExpressions();

            if (declarationExpressionList.size() == 1) {
                return configureAST((Expression) declarationExpressionList.get(0), ctx);
            } else {
                return configureAST(new ClosureListExpression((List<Expression>) declarationExpressionList), ctx);
            }
        }

        if (asBoolean(ctx.expressionList())) {
            return this.translateExpressionList(ctx.expressionList());
        }

        throw createParsingFailedException("Unsupported for init: " + ctx.getText(), ctx);
    }

    @Override
    public Expression visitForUpdate(ForUpdateContext ctx) {
        if (!asBoolean(ctx)) {
            return EmptyExpression.INSTANCE;
        }

        return this.translateExpressionList(ctx.expressionList());
    }

    private Expression translateExpressionList(ExpressionListContext ctx) {
        List<Expression> expressionList = this.visitExpressionList(ctx);

        if (expressionList.size() == 1) {
            return configureAST(expressionList.get(0), ctx);
        } else {
            return configureAST(new ClosureListExpression(expressionList), ctx);
        }
    }

    @Override
    public Tuple2<Parameter, Expression> visitEnhancedForControl(EnhancedForControlContext ctx) {
        Parameter parameter = configureAST(
                new Parameter(this.visitType(ctx.type()), this.visitVariableDeclaratorId(ctx.variableDeclaratorId()).getName()),
                ctx.variableDeclaratorId());
        // GRECLIPSE add
        parameter.setNameEnd(parameter.getEnd());
        parameter.setNameStart(parameter.getStart());
        parameter.setLineNumber(ctx.variableModifiersOpt().getStart().getLine());
        parameter.setColumnNumber(ctx.variableModifiersOpt().getStart().getCharPositionInLine() + 1);
        parameter.setStart(locationSupport.findOffset(parameter.getLineNumber(), parameter.getColumnNumber()));
        // GRECLIPSE end

        return new Tuple2<>(parameter, (Expression) this.visit(ctx.expression()));
    }

    @Override
    public Tuple2<Parameter, Expression> visitClassicalForControl(ClassicalForControlContext ctx) {
        ClosureListExpression closureListExpression = new ClosureListExpression();

        closureListExpression.addExpression(this.visitForInit(ctx.forInit()));
        closureListExpression.addExpression(asBoolean(ctx.expression()) ? (Expression) this.visit(ctx.expression()) : EmptyExpression.INSTANCE);
        closureListExpression.addExpression(this.visitForUpdate(ctx.forUpdate()));

        return new Tuple2<Parameter, Expression>(ForStatement.FOR_LOOP_DUMMY, closureListExpression);
    }

    @Override
    public WhileStatement visitWhileStmtAlt(WhileStmtAltContext ctx) {
        Expression conditionExpression = this.visitExpressionInPar(ctx.expressionInPar());
        BooleanExpression booleanExpression =
                configureAST(
                        new BooleanExpression(conditionExpression), conditionExpression);

        Statement loopBlock = this.unpackStatement((Statement) this.visit(ctx.statement()));

        return configureAST(
                new WhileStatement(booleanExpression, asBoolean(loopBlock) ? loopBlock : EmptyStatement.INSTANCE),
                ctx);
    }

    @Override
    public DoWhileStatement visitDoWhileStmtAlt(DoWhileStmtAltContext ctx) {
        Expression conditionExpression = this.visitExpressionInPar(ctx.expressionInPar());

        BooleanExpression booleanExpression =
                configureAST(
                        new BooleanExpression(conditionExpression),
                        conditionExpression
                );

        Statement loopBlock = this.unpackStatement((Statement) this.visit(ctx.statement()));

        return configureAST(
                new DoWhileStatement(booleanExpression, asBoolean(loopBlock) ? loopBlock : EmptyStatement.INSTANCE),
                ctx);
    }

    @Override
    public Statement visitTryCatchStmtAlt(TryCatchStmtAltContext ctx) {
        return configureAST(this.visitTryCatchStatement(ctx.tryCatchStatement()), ctx);
    }

    @Override
    public Statement visitTryCatchStatement(TryCatchStatementContext ctx) {
        boolean resourcesExists = asBoolean(ctx.resources());
        boolean catchExists = asBoolean(ctx.catchClause());
        boolean finallyExists = asBoolean(ctx.finallyBlock());

        if (!(resourcesExists || catchExists || finallyExists)) {
            throw createParsingFailedException("Either a catch or finally clause or both is required for a try-catch-finally statement", ctx);
        }

        TryCatchStatement tryCatchStatement =
                new TryCatchStatement((Statement) this.visit(ctx.block()),
                        this.visitFinallyBlock(ctx.finallyBlock()));

        if (asBoolean(ctx.resources())) {
            for (ExpressionStatement e : this.visitResources(ctx.resources())) {
                tryCatchStatement.addResource(e);
            }
        }

        for (CatchClauseContext cc : ctx.catchClause()) {
            List<CatchStatement> list = this.visitCatchClause(cc);
            for (CatchStatement cs : list) {
                tryCatchStatement.addCatch(cs);
            }
        }
        return configureAST(
                tryWithResourcesASTTransformation.transform(
                        configureAST(tryCatchStatement, ctx)),
                ctx);
    }


    @Override
    public List<ExpressionStatement> visitResources(ResourcesContext ctx) {
        return this.visitResourceList(ctx.resourceList());
    }

    @Override
    public List<ExpressionStatement> visitResourceList(ResourceListContext ctx) {
        List<ExpressionStatement> list = new ArrayList<>();
        for (ResourceContext resourceContext : ctx.resource()) {
            ExpressionStatement expressionStatement = visitResource(resourceContext);
            list.add(expressionStatement);
        }
        return list;
    }

    @Override
    public ExpressionStatement visitResource(ResourceContext ctx) {
        if (asBoolean(ctx.localVariableDeclaration())) {
            List<ExpressionStatement> declarationStatements = this.visitLocalVariableDeclaration(ctx.localVariableDeclaration()).getDeclarationStatements();

            if (declarationStatements.size() > 1) {
                throw createParsingFailedException("Multi resources can not be declared in one statement", ctx);
            }

            return declarationStatements.get(0);
        } else if (asBoolean(ctx.expression())) {
            Expression expression = (Expression) this.visit(ctx.expression());
            if (!(expression instanceof BinaryExpression
                    && Types.ASSIGN == ((BinaryExpression) expression).getOperation().getType()
                    && ((BinaryExpression) expression).getLeftExpression() instanceof VariableExpression)) {

                throw createParsingFailedException("Only variable declarations are allowed to declare resource", ctx);
            }

            BinaryExpression assignmentExpression = (BinaryExpression) expression;

            return configureAST(
                    new ExpressionStatement(
                            configureAST(
                                    new DeclarationExpression(
                                            configureAST(
                                                    new VariableExpression(assignmentExpression.getLeftExpression().getText()),
                                                    assignmentExpression.getLeftExpression()
                                            ),
                                            assignmentExpression.getOperation(),
                                            assignmentExpression.getRightExpression()
                                    ), ctx)
                    ), ctx);
        }

        throw createParsingFailedException("Unsupported resource declaration: " + ctx.getText(), ctx);
    }

    /**
     * Multi-catch(1..*) clause will be unpacked to several normal catch clauses, so the return type is List
     *
     * @param ctx the parse tree
     * @return a list of CatchStatement instances
     */
    @Override
    public List<CatchStatement> visitCatchClause(CatchClauseContext ctx) {
        // GRECLIPSE add
        ASTNode nameNode = null;
        Parameter catchParameter = null;
        // GRECLIPSE end
        List<CatchStatement> list = new ArrayList<>();
        for (ClassNode e : this.visitCatchType(ctx.catchType())) {
            CatchStatement catchStatement = configureAST(
                    new CatchStatement(
                            new Parameter(e, this.visitIdentifier(ctx.identifier())),
                            this.visitBlock(ctx.block())),
                    ctx);
            // GRECLIPSE add
            catchParameter = configureAST(catchStatement.getVariable(), ctx.catchType() != null ? ctx.catchType() : ctx.identifier());
            nameNode = configureAST(new ConstantExpression(catchParameter.getName()), ctx.identifier());
            catchParameter.setNameStart(nameNode.getStart()); catchParameter.setNameEnd(nameNode.getEnd());
            // GRECLIPSE end
            list.add(catchStatement);
        }
        // GRECLIPSE add
        if (catchParameter != null) {
            catchParameter.setEnd(nameNode.getEnd());
            catchParameter.setLastLineNumber(nameNode.getLastLineNumber());
            catchParameter.setLastColumnNumber(nameNode.getLastColumnNumber());

            catchParameter = list.get(0).getVariable();
            catchParameter.setLineNumber(ctx.variableModifiersOpt().getStart().getLine());
            catchParameter.setColumnNumber(ctx.variableModifiersOpt().getStart().getCharPositionInLine() + 1);
            catchParameter.setStart(locationSupport.findOffset(catchParameter.getLineNumber(), catchParameter.getColumnNumber()));
        }
        // GRECLIPSE end
        return list;
    }

    @Override
    public List<ClassNode> visitCatchType(CatchTypeContext ctx) {
        if (!asBoolean(ctx)) {
            return Collections.singletonList(ClassHelper.OBJECT_TYPE);
        }

        List<ClassNode> list = new ArrayList<>();
        for (QualifiedClassNameContext qualifiedClassNameContext : ctx.qualifiedClassName()) {
            ClassNode classNode = visitQualifiedClassName(qualifiedClassNameContext);
            list.add(classNode);
        }
        return list;
    }


    @Override
    public Statement visitFinallyBlock(FinallyBlockContext ctx) {
        if (!asBoolean(ctx)) {
            return EmptyStatement.INSTANCE;
        }

        return configureAST(
                this.createBlockStatement((Statement) this.visit(ctx.block())),
                ctx);
    }

    @Override
    public SwitchStatement visitSwitchStatement(SwitchStatementContext ctx) {
        visitingSwitchStatementCnt++;

        List<Statement> statementList = new LinkedList<>();
        for (SwitchBlockStatementGroupContext c : ctx.switchBlockStatementGroup()) {
            statementList.addAll(this.visitSwitchBlockStatementGroup(c));
        }

        List<CaseStatement> caseStatementList = new LinkedList<>();
        List<Statement> defaultStatementList = new LinkedList<>();

        for (Statement e : statementList) {
            if (e instanceof CaseStatement) {
                caseStatementList.add((CaseStatement) e);
            } else if (isTrue(e, IS_SWITCH_DEFAULT)) {
                defaultStatementList.add(e);
            }
        }

        int defaultStatementListSize = defaultStatementList.size();
        if (defaultStatementListSize > 1) {
            throw createParsingFailedException("switch statement should have only one default case, which should appear at last", defaultStatementList.get(0));
        }

        if (defaultStatementListSize > 0 && last(statementList) instanceof CaseStatement) {
            throw createParsingFailedException("default case should appear at last", defaultStatementList.get(0));
        }

        SwitchStatement result = configureAST(
                new SwitchStatement(
                        this.visitExpressionInPar(ctx.expressionInPar()),
                        caseStatementList,
                        defaultStatementListSize == 0 ? EmptyStatement.INSTANCE : defaultStatementList.get(0)
                ),
                ctx);

        visitingSwitchStatementCnt--;

        return result;

    }


    @Override
    public List<Statement> visitSwitchBlockStatementGroup(SwitchBlockStatementGroupContext ctx) {
        int labelCnt = ctx.switchLabel().size();
        List<Token> firstLabelHolder = new ArrayList<>(1);

        List<Statement> statementList = new ArrayList<>(4);
        for (SwitchLabelContext e : ctx.switchLabel()) {
            Tuple2<Token, Expression> tuple = this.visitSwitchLabel(e);

            boolean isLast = labelCnt - 1 == statementList.size();

            switch (tuple.getFirst().getType()) {
                case CASE: {
                    if (!asBoolean(statementList)) {
                        firstLabelHolder.add(tuple.getFirst());
                    }

                    statementList.add(
                            configureAST(
                                    new CaseStatement(
                                            tuple.getSecond(),

                                            // check whether processing the last label. if yes, block statement should be attached.
                                            isLast ? this.visitBlockStatements(ctx.blockStatements())
                                                    : EmptyStatement.INSTANCE
                                    ),
                                    firstLabelHolder.get(0)));

                    break;
                }
                case DEFAULT: {

                    BlockStatement blockStatement = this.visitBlockStatements(ctx.blockStatements());
                    blockStatement.putNodeMetaData(IS_SWITCH_DEFAULT, true);

                    statementList.add(
                            // configureAST(blockStatement, tuple.getKey())
                            blockStatement
                    );

                    break;
                }
            }
        }
        return statementList;
    }

    @Override
    public Tuple2<Token, Expression> visitSwitchLabel(SwitchLabelContext ctx) {
        if (asBoolean(ctx.CASE())) {
            return new Tuple2<>(ctx.CASE().getSymbol(), (Expression) this.visit(ctx.expression()));
        } else if (asBoolean(ctx.DEFAULT())) {
            return new Tuple2<>(ctx.DEFAULT().getSymbol(), (Expression) EmptyExpression.INSTANCE);
        }

        throw createParsingFailedException("Unsupported switch label: " + ctx.getText(), ctx);
    }

    @Override
    public SynchronizedStatement visitSynchronizedStmtAlt(SynchronizedStmtAltContext ctx) {
        return configureAST(
                new SynchronizedStatement(this.visitExpressionInPar(ctx.expressionInPar()), this.visitBlock(ctx.block())),
                ctx);
    }

    @Override
    public ExpressionStatement visitExpressionStmtAlt(ExpressionStmtAltContext ctx) {
        return (ExpressionStatement) this.visit(ctx.statementExpression());
    }

    @Override
    public ReturnStatement visitReturnStmtAlt(ReturnStmtAltContext ctx) {
        return configureAST(new ReturnStatement(asBoolean(ctx.expression())
                        ? (Expression) this.visit(ctx.expression())
                        : ConstantExpression.EMPTY_EXPRESSION),
                ctx);
    }

    @Override
    public ThrowStatement visitThrowStmtAlt(ThrowStmtAltContext ctx) {
        return configureAST(
                new ThrowStatement((Expression) this.visit(ctx.expression())),
                ctx);
    }

    @Override
    public Statement visitLabeledStmtAlt(LabeledStmtAltContext ctx) {
        Statement statement = (Statement) this.visit(ctx.statement());

        statement.addStatementLabel(this.visitIdentifier(ctx.identifier()));

        return statement; // configureAST(statement, ctx);
    }

    @Override
    public BreakStatement visitBreakStatement(BreakStatementContext ctx) {
        if (0 == visitingLoopStatementCnt && 0 == visitingSwitchStatementCnt) {
            throw createParsingFailedException("break statement is only allowed inside loops or switches", ctx);
        }

        String label = asBoolean(ctx.identifier())
                ? this.visitIdentifier(ctx.identifier())
                : null;

        return configureAST(new BreakStatement(label), ctx);
    }

    @Override
    public BreakStatement visitBreakStmtAlt(BreakStmtAltContext ctx) {
        return configureAST(this.visitBreakStatement(ctx.breakStatement()), ctx);
    }

    @Override
    public ContinueStatement visitContinueStatement(ContinueStatementContext ctx) {
        if (0 == visitingLoopStatementCnt) {
            throw createParsingFailedException("continue statement is only allowed inside loops", ctx);
        }

        String label = asBoolean(ctx.identifier())
                ? this.visitIdentifier(ctx.identifier())
                : null;

        return configureAST(new ContinueStatement(label), ctx);

    }

    @Override
    public ContinueStatement visitContinueStmtAlt(ContinueStmtAltContext ctx) {
        return configureAST(this.visitContinueStatement(ctx.continueStatement()), ctx);
    }

    @Override
    public ImportNode visitImportStmtAlt(ImportStmtAltContext ctx) {
        return configureAST(this.visitImportDeclaration(ctx.importDeclaration()), ctx);
    }

    @Override
    public ClassNode visitTypeDeclarationStmtAlt(TypeDeclarationStmtAltContext ctx) {
        return configureAST(this.visitTypeDeclaration(ctx.typeDeclaration()), ctx);
    }


    @Override
    public Statement visitLocalVariableDeclarationStmtAlt(LocalVariableDeclarationStmtAltContext ctx) {
        return configureAST(this.visitLocalVariableDeclaration(ctx.localVariableDeclaration()), ctx);
    }

    @Override
    public MethodNode visitMethodDeclarationStmtAlt(MethodDeclarationStmtAltContext ctx) {
        return configureAST(this.visitMethodDeclaration(ctx.methodDeclaration()), ctx);
    }

    // } statement    --------------------------------------------------------------------

    @Override
    public ClassNode visitTypeDeclaration(TypeDeclarationContext ctx) {
        if (asBoolean(ctx.classDeclaration())) { // e.g. class A {}
            ctx.classDeclaration().putNodeMetaData(TYPE_DECLARATION_MODIFIERS, this.visitClassOrInterfaceModifiersOpt(ctx.classOrInterfaceModifiersOpt()));
            return configureAST(this.visitClassDeclaration(ctx.classDeclaration()), ctx);
        }

        throw createParsingFailedException("Unsupported type declaration: " + ctx.getText(), ctx);
    }

    private void initUsingGenerics(ClassNode classNode) {
        if (classNode.isUsingGenerics()) {
            return;
        }

        if (!classNode.isEnum()) {
            classNode.setUsingGenerics(classNode.getSuperClass().isUsingGenerics());
        }

        if (!classNode.isUsingGenerics() && null != classNode.getInterfaces()) {
            for (ClassNode anInterface : classNode.getInterfaces()) {
                classNode.setUsingGenerics(classNode.isUsingGenerics() || anInterface.isUsingGenerics());

                if (classNode.isUsingGenerics())
                    break;
            }
        }
    }

    @Override
    public ClassNode visitClassDeclaration(ClassDeclarationContext ctx) {
        String packageName = moduleNode.getPackageName();
        packageName = null != packageName ? packageName : "";

        List<ModifierNode> modifierNodeList = ctx.getNodeMetaData(TYPE_DECLARATION_MODIFIERS);
        Objects.requireNonNull(modifierNodeList, "modifierNodeList should not be null");

        ModifierManager modifierManager = new ModifierManager(this, modifierNodeList);
        int modifiers = modifierManager.getClassModifiersOpValue();

        boolean syntheticPublic = ((modifiers & Opcodes.ACC_SYNTHETIC) != 0);
        modifiers &= ~Opcodes.ACC_SYNTHETIC;

        final ClassNode outerClass = classNodeStack.peek();
        ClassNode classNode;
        String className = this.visitIdentifier(ctx.identifier());
        if (asBoolean(ctx.ENUM())) {
            classNode =
                    EnumHelper.makeEnumNode(
                            asBoolean(outerClass) ? className : packageName + className,
                            modifiers, null, outerClass);
        } else {
            if (asBoolean(outerClass)) {
                classNode =
                        new InnerClassNode(
                                outerClass,
                                outerClass.getName() + "$" + className,
                                modifiers | (outerClass.isInterface() ? Opcodes.ACC_STATIC : 0),
                                ClassHelper.OBJECT_TYPE);
            } else {
                classNode =
                        new ClassNode(
                                packageName + className,
                                modifiers,
                                ClassHelper.OBJECT_TYPE);
            }
        }

        configureAST(classNode, ctx);
        // GRECLIPSE add
        ASTNode nameNode = configureAST(new ConstantExpression(className), ctx.identifier());
        classNode.setNameStart(nameNode.getStart()); classNode.setNameEnd(nameNode.getEnd() - 1);
        // GRECLIPSE end
        classNode.putNodeMetaData(CLASS_NAME, className);
        classNode.setSyntheticPublic(syntheticPublic);

        if (asBoolean(ctx.TRAIT())) {
            attachTraitAnnotation(classNode);
        }
        classNode.addAnnotations(modifierManager.getAnnotations());
        classNode.setGenericsTypes(this.visitTypeParameters(ctx.typeParameters()));

        boolean isInterface = asBoolean(ctx.INTERFACE()) && !asBoolean(ctx.AT());
        boolean isInterfaceWithDefaultMethods = false;

        // declaring interface with default method
        if (isInterface && this.containsDefaultMethods(ctx)) {
            isInterfaceWithDefaultMethods = true;
            attachTraitAnnotation(classNode);
            classNode.putNodeMetaData(IS_INTERFACE_WITH_DEFAULT_METHODS, true);
        }

        if (asBoolean(ctx.CLASS()) || asBoolean(ctx.TRAIT()) || isInterfaceWithDefaultMethods) { // class OR trait OR interface with default methods
            classNode.setSuperClass(this.visitType(ctx.sc));
            classNode.setInterfaces(this.visitTypeList(ctx.is));

            this.initUsingGenerics(classNode);
        } else if (isInterface) { // interface(NOT annotation)
            classNode.setModifiers(classNode.getModifiers() | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT);

            classNode.setSuperClass(ClassHelper.OBJECT_TYPE);
            classNode.setInterfaces(this.visitTypeList(ctx.scs));

            this.initUsingGenerics(classNode);

            this.hackMixins(classNode);
        } else if (asBoolean(ctx.ENUM())) { // enum
            classNode.setModifiers(classNode.getModifiers() | Opcodes.ACC_ENUM | Opcodes.ACC_FINAL);

            classNode.setInterfaces(this.visitTypeList(ctx.is));

            this.initUsingGenerics(classNode);
        } else if (asBoolean(ctx.AT())) { // annotation
            classNode.setModifiers(classNode.getModifiers() | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_ANNOTATION);

            classNode.addInterface(ClassHelper.Annotation_TYPE);

            this.hackMixins(classNode);
        } else {
            throw createParsingFailedException("Unsupported class declaration: " + ctx.getText(), ctx);
        }

        // we put the class already in output to avoid the most inner classes
        // will be used as first class later in the loader. The first class
        // there determines what GCL#parseClass for example will return, so we
        // have here to ensure it won't be the inner class
        if (asBoolean(ctx.CLASS()) || asBoolean(ctx.TRAIT())) {
            classNodeList.add(classNode);
        }

        int oldAnonymousInnerClassCounter = this.anonymousInnerClassCounter;
        classNodeStack.push(classNode);
        ctx.classBody().putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
        this.visitClassBody(ctx.classBody());
        classNodeStack.pop();
        this.anonymousInnerClassCounter = oldAnonymousInnerClassCounter;

        if (!(asBoolean(ctx.CLASS()) || asBoolean(ctx.TRAIT()))) {
            classNodeList.add(classNode);
        }

        groovydocManager.handle(classNode, ctx);

        return classNode;
    }

    private void attachTraitAnnotation(ClassNode classNode) {
        classNode.addAnnotation(new AnnotationNode(ClassHelper.make(GROOVY_TRANSFORM_TRAIT)));
    }

    private boolean containsDefaultMethods(ClassDeclarationContext ctx) {
        for (ClassBodyDeclarationContext c : ctx.classBody().classBodyDeclaration()) {
            MemberDeclarationContext memberDeclarationContext = c.memberDeclaration();
            if(memberDeclarationContext != null) {
                MethodDeclarationContext methodDeclarationContext = memberDeclarationContext.methodDeclaration();
                if (methodDeclarationContext != null) {
                    if (createModifierManager(methodDeclarationContext).contains(DEFAULT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Void visitClassBody(ClassBodyContext ctx) {
        ClassNode classNode = ctx.getNodeMetaData(CLASS_DECLARATION_CLASS_NODE);
        Objects.requireNonNull(classNode, "classNode should not be null");

        if (asBoolean(ctx.enumConstants())) {
            ctx.enumConstants().putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
            this.visitEnumConstants(ctx.enumConstants());
        }

        for (ClassBodyDeclarationContext e : ctx.classBodyDeclaration()) {
            e.putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
            this.visitClassBodyDeclaration(e);
        }

        return null;
    }

    @Override
    public List<FieldNode> visitEnumConstants(EnumConstantsContext ctx) {
        ClassNode classNode = ctx.getNodeMetaData(CLASS_DECLARATION_CLASS_NODE);
        Objects.requireNonNull(classNode, "classNode should not be null");

        List<FieldNode> list = new LinkedList<>();
        for (EnumConstantContext e : ctx.enumConstant()) {
            e.putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
            list.add(visitEnumConstant(e));
        }
        return list;
    }

    @Override
    public FieldNode visitEnumConstant(EnumConstantContext ctx) {
        ClassNode classNode = ctx.getNodeMetaData(CLASS_DECLARATION_CLASS_NODE);
        Objects.requireNonNull(classNode, "classNode should not be null");

        InnerClassNode anonymousInnerClassNode = null;
        if (asBoolean(ctx.anonymousInnerClassDeclaration())) {
            ctx.anonymousInnerClassDeclaration().putNodeMetaData(ANONYMOUS_INNER_CLASS_SUPER_CLASS, classNode);
            anonymousInnerClassNode = this.visitAnonymousInnerClassDeclaration(ctx.anonymousInnerClassDeclaration());
        }

        FieldNode enumConstant =
                EnumHelper.addEnumConstant(
                        classNode,
                        this.visitIdentifier(ctx.identifier()),
                        createEnumConstantInitExpression(ctx.arguments(), anonymousInnerClassNode));

        enumConstant.addAnnotations(this.visitAnnotationsOpt(ctx.annotationsOpt()));

        groovydocManager.handle(enumConstant, ctx);

        // GRECLIPSE add
        ASTNode nameNode = configureAST(new ConstantExpression(enumConstant.getName()), ctx.identifier());
        enumConstant.setNameStart(nameNode.getStart()); enumConstant.setNameEnd(nameNode.getEnd() - 1);
        // GRECLIPSE end
        return configureAST(enumConstant, ctx);
    }

    private Expression createEnumConstantInitExpression(ArgumentsContext ctx, InnerClassNode anonymousInnerClassNode) {
        if (!asBoolean(ctx) && !asBoolean(anonymousInnerClassNode)) {
            return null;
        }

        TupleExpression argumentListExpression = (TupleExpression) this.visitArguments(ctx);
        List<Expression> expressions = argumentListExpression.getExpressions();

        if (expressions.size() == 1) {
            Expression expression = expressions.get(0);

            if (expression instanceof NamedArgumentListExpression) { // e.g. SOME_ENUM_CONSTANT(a: "1", b: "2")
                List<MapEntryExpression> mapEntryExpressionList = ((NamedArgumentListExpression) expression).getMapEntryExpressions();
                List<Expression> list = new ArrayList<>();
                for (MapEntryExpression e : mapEntryExpressionList) {
                    Expression e1 = (Expression) e;
                    list.add(e1);
                }
                ListExpression listExpression =
                        new ListExpression(
                                list);

                if (asBoolean(anonymousInnerClassNode)) {
                    listExpression.addExpression(
                            configureAST(
                                    new ClassExpression(anonymousInnerClassNode),
                                    anonymousInnerClassNode));
                }

                if (mapEntryExpressionList.size() > 1) {
                    listExpression.setWrapped(true);
                }

                return configureAST(listExpression, ctx);
            }

            if (!asBoolean(anonymousInnerClassNode)) {
                if (expression instanceof ListExpression) {
                    ListExpression listExpression = new ListExpression();
                    listExpression.addExpression(expression);

                    return configureAST(listExpression, ctx);
                }

                return expression;
            }

            ListExpression listExpression = new ListExpression();

            if (expression instanceof ListExpression) {
                for (Expression e : ((ListExpression) expression).getExpressions()) {
                    listExpression.addExpression(e);
                }
            } else {
                listExpression.addExpression(expression);
            }

            listExpression.addExpression(
                    configureAST(
                            new ClassExpression(anonymousInnerClassNode),
                            anonymousInnerClassNode));

            return configureAST(listExpression, ctx);
        }

        ListExpression listExpression = new ListExpression(expressions);
        if (asBoolean(anonymousInnerClassNode)) {
            listExpression.addExpression(
                    configureAST(
                            new ClassExpression(anonymousInnerClassNode),
                            anonymousInnerClassNode));
        }

        if (asBoolean(ctx)) {
            listExpression.setWrapped(true);
        }

        return asBoolean(ctx)
                ? configureAST(listExpression, ctx)
                : configureAST(listExpression, anonymousInnerClassNode);
    }


    @Override
    public Void visitClassBodyDeclaration(ClassBodyDeclarationContext ctx) {
        ClassNode classNode = ctx.getNodeMetaData(CLASS_DECLARATION_CLASS_NODE);
        Objects.requireNonNull(classNode, "classNode should not be null");

        if (asBoolean(ctx.memberDeclaration())) {
            ctx.memberDeclaration().putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
            this.visitMemberDeclaration(ctx.memberDeclaration());
        } else if (asBoolean(ctx.block())) {
            Statement statement = this.visitBlock(ctx.block());

            if (asBoolean(ctx.STATIC())) { // e.g. static { }
                classNode.addStaticInitializerStatements(Collections.singletonList(statement), false);
                // GRECLIPSE add
                MethodNode clinit = classNode.getDeclaredMethod("<clinit>", Parameter.EMPTY_ARRAY);
                if (clinit.getEnd() < 1) { // set source position for first initializer only
                    configureAST(clinit, ctx.STATIC());
                }
                // GRECLIPSE end
            } else { // e.g.  { }
                classNode.addObjectInitializerStatements(
                        configureAST(
                                this.createBlockStatement(statement),
                                statement));
            }
        }

        return null;
    }

    @Override
    public Void visitMemberDeclaration(MemberDeclarationContext ctx) {
        ClassNode classNode = ctx.getNodeMetaData(CLASS_DECLARATION_CLASS_NODE);
        Objects.requireNonNull(classNode, "classNode should not be null");

        if (asBoolean(ctx.methodDeclaration())) {
            ctx.methodDeclaration().putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
            this.visitMethodDeclaration(ctx.methodDeclaration());
        } else if (asBoolean(ctx.fieldDeclaration())) {
            ctx.fieldDeclaration().putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
            this.visitFieldDeclaration(ctx.fieldDeclaration());
        } else if (asBoolean(ctx.classDeclaration())) {
            ctx.classDeclaration().putNodeMetaData(TYPE_DECLARATION_MODIFIERS, this.visitModifiersOpt(ctx.modifiersOpt()));
            ctx.classDeclaration().putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
            this.visitClassDeclaration(ctx.classDeclaration());
        }

        return null;
    }

    @Override
    public GenericsType[] visitTypeParameters(TypeParametersContext ctx) {
        if (!asBoolean(ctx)) {
            return null;
        }

        List<GenericsType> list = new ArrayList<>();
        for (TypeParameterContext typeParameterContext : ctx.typeParameter()) {
            GenericsType genericsType = visitTypeParameter(typeParameterContext);
            list.add(genericsType);
        }
        return list.toArray(new GenericsType[0]);
    }

    @Override
    public GenericsType visitTypeParameter(TypeParameterContext ctx) {
        return configureAST(
                new GenericsType(
                        // GRECLIPSE edit
                        //configureAST(ClassHelper.make(this.visitClassName(ctx.className())), ctx),
                        configureAST(ClassHelper.make(this.visitClassName(ctx.className())), ctx.className()),
                        // GRECLIPSE end
                        this.visitTypeBound(ctx.typeBound()),
                        null
                ),
                ctx);
    }

    @Override
    public ClassNode[] visitTypeBound(TypeBoundContext ctx) {
        if (!asBoolean(ctx)) {
            return null;
        }

        List<ClassNode> list = new ArrayList<>();
        for (TypeContext typeContext : ctx.type()) {
            ClassNode classNode = visitType(typeContext);
            list.add(classNode);
        }
        return list.toArray(new ClassNode[0]);
    }

    @Override
    public Void visitFieldDeclaration(FieldDeclarationContext ctx) {
        ClassNode classNode = ctx.getNodeMetaData(CLASS_DECLARATION_CLASS_NODE);
        Objects.requireNonNull(classNode, "classNode should not be null");

        ctx.variableDeclaration().putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, classNode);
        this.visitVariableDeclaration(ctx.variableDeclaration());

        return null;
    }

    private ConstructorCallExpression checkThisAndSuperConstructorCall(Statement statement) {
        if (!(statement instanceof BlockStatement)) { // method code must be a BlockStatement
            return null;
        }

        BlockStatement blockStatement = (BlockStatement) statement;
        List<Statement> statementList = blockStatement.getStatements();

        for (int i = 0, n = statementList.size(); i < n; i++) {
            Statement s = statementList.get(i);
            if (s instanceof ExpressionStatement) {
                Expression expression = ((ExpressionStatement) s).getExpression();
                if ((expression instanceof ConstructorCallExpression) && 0 != i) {
                    return (ConstructorCallExpression) expression;
                }
            }
        }

        return null;
    }

    private ModifierManager createModifierManager(MethodDeclarationContext ctx) {
        List<ModifierNode> modifierNodeList = Collections.emptyList();

        if (asBoolean(ctx.modifiersOpt())) {
            modifierNodeList = this.visitModifiersOpt(ctx.modifiersOpt());
        }

        return new ModifierManager(this, modifierNodeList);
    }

    private void validateParametersOfMethodDeclaration(Parameter[] parameters, ClassNode classNode) {
        if (!classNode.isInterface()) {
            return;
        }

        for (Parameter e : parameters) {
            if (e.hasInitialExpression()) {
                throw createParsingFailedException("Cannot specify default value for method parameter '" + e.getName() + " = " + e.getInitialExpression().getText() + "' inside an interface", e);
            }
        }
    }

    @Override
    public MethodNode visitMethodDeclaration(MethodDeclarationContext ctx) {
        validateMethodDeclaration(ctx);

        ModifierManager modifierManager = createModifierManager(ctx);
        String methodName = this.visitMethodName(ctx.methodName());
        ClassNode returnType = this.visitReturnType(ctx.returnType());
        Parameter[] parameters = this.visitFormalParameters(ctx.formalParameters());
        ClassNode[] exceptions = this.visitQualifiedClassNameList(ctx.qualifiedClassNameList());

        anonymousInnerClassesDefinedInMethodStack.push(new LinkedList<InnerClassNode>());
        Statement code = this.visitMethodBody(ctx.methodBody());
        List<InnerClassNode> anonymousInnerClassList = anonymousInnerClassesDefinedInMethodStack.pop();

        MethodNode methodNode;
        // if classNode is not null, the method declaration is for class declaration
        ClassNode classNode = ctx.getNodeMetaData(CLASS_DECLARATION_CLASS_NODE);
        if (asBoolean(classNode)) {
            validateParametersOfMethodDeclaration(parameters, classNode);

            methodNode = createConstructorOrMethodNodeForClass(ctx, modifierManager, methodName, returnType, parameters, exceptions, code, classNode);
        } else { // script method declaration
            methodNode = createScriptMethodNode(modifierManager, methodName, returnType, parameters, exceptions, code);
        }
        for (InnerClassNode e : anonymousInnerClassList) {
            e.setEnclosingMethod(methodNode);
        }

        methodNode.setGenericsTypes(this.visitTypeParameters(ctx.typeParameters()));
        methodNode.setSyntheticPublic(
                this.isSyntheticPublic(
                        this.isAnnotationDeclaration(classNode),
                        classNode instanceof EnumConstantClassNode,
                        asBoolean(ctx.returnType()),
                        modifierManager));

        if (modifierManager.contains(STATIC)) {
            for (Parameter e : methodNode.getParameters()) {
                e.setInStaticContext(true);
            }
            methodNode.getVariableScope().setInStaticContext(true);
        }

        configureAST(methodNode, ctx);
        // GRECLIPSE add
        ASTNode nameNode = configureAST(new ConstantExpression(methodName), ctx.methodName());
        methodNode.setNameStart(nameNode.getStart()); methodNode.setNameEnd(nameNode.getEnd() - 1);
        // roll back stop for abstract/interface methods
        if (ctx.getStop().getType() == GroovyParser.NL) {
            methodNode.setLastLineNumber(last(ctx.nls()).getStart().getLine());
            methodNode.setLastColumnNumber(last(ctx.nls()).getStart().getCharPositionInLine() + 1);
            methodNode.setEnd(locationSupport.findOffset(methodNode.getLastLineNumber(), methodNode.getLastColumnNumber()));
        }
        // GRECLIPSE end

        validateMethodDeclaration(ctx, methodNode, modifierManager, classNode);

        groovydocManager.handle(methodNode, ctx);

        return methodNode;
    }

    private void validateMethodDeclaration(MethodDeclarationContext ctx) {
        if (1 == ctx.t || 2 == ctx.t || 3 == ctx.t) { // 1: normal method declaration; 2: abstract method declaration; 3: normal method declaration OR abstract method declaration
            if (!(asBoolean(ctx.modifiersOpt().modifiers()) || asBoolean(ctx.returnType()))) {
                throw createParsingFailedException("Modifiers or return type is required", ctx);
            }
        }

        if (1 == ctx.t) {
            if (!asBoolean(ctx.methodBody())) {
                throw createParsingFailedException("Method body is required", ctx);
            }
        }

        if (2 == ctx.t) {
            if (asBoolean(ctx.methodBody())) {
                throw createParsingFailedException("Abstract method should not have method body", ctx);
            }
        }
    }

    private void validateMethodDeclaration(MethodDeclarationContext ctx, MethodNode methodNode, ModifierManager modifierManager, ClassNode classNode) {
        boolean isAbstractMethod = methodNode.isAbstract();
        boolean hasMethodBody = asBoolean(methodNode.getCode());

        if (9 == ctx.ct) { // script
            if (isAbstractMethod || !hasMethodBody) { // method should not be declared abstract in the script
                throw createParsingFailedException("You can not define a " + (isAbstractMethod ? "abstract" : "") + " method[" + methodNode.getName() + "] " + (!hasMethodBody ? "without method body" : "") + " in the script. Try " + (isAbstractMethod ? "removing the 'abstract'" : "") + (isAbstractMethod && !hasMethodBody ? " and" : "") + (!hasMethodBody ? " adding a method body" : ""), methodNode);
            }
        } else {
            if (!isAbstractMethod && !hasMethodBody) { // non-abstract method without body in the non-script(e.g. class, enum, trait) is not allowed!
                throw createParsingFailedException("You defined a method[" + methodNode.getName() + "] without body. Try adding a method body, or declare it abstract", methodNode);
            }

            boolean isInterfaceOrAbstractClass = asBoolean(classNode) && classNode.isAbstract() && !classNode.isAnnotationDefinition();
            if (isInterfaceOrAbstractClass && !modifierManager.contains(DEFAULT) && isAbstractMethod && hasMethodBody) {
                throw createParsingFailedException("You defined an abstract method[" + methodNode.getName() + "] with body. Try removing the method body" + (classNode.isInterface() ? ", or declare it default" : ""), methodNode);
            }
        }

        modifierManager.validate(methodNode);

        if (methodNode instanceof ConstructorNode) {
            modifierManager.validate((ConstructorNode) methodNode);
        }
    }

    private MethodNode createScriptMethodNode(ModifierManager modifierManager, String methodName, ClassNode returnType, Parameter[] parameters, ClassNode[] exceptions, Statement code) {
        MethodNode methodNode;
        methodNode =
                new MethodNode(
                        methodName,
                        modifierManager.contains(PRIVATE) ? Opcodes.ACC_PRIVATE : Opcodes.ACC_PUBLIC,
                        returnType,
                        parameters,
                        exceptions,
                        code);

        modifierManager.processMethodNode(methodNode);
        return methodNode;
    }

    private MethodNode createConstructorOrMethodNodeForClass(MethodDeclarationContext ctx, ModifierManager modifierManager, String methodName, ClassNode returnType, Parameter[] parameters, ClassNode[] exceptions, Statement code, ClassNode classNode) {
        MethodNode methodNode;
        String className = classNode.getNodeMetaData(CLASS_NAME);
        int modifiers = modifierManager.getClassMemberModifiersOpValue();

        boolean hasReturnType = asBoolean(ctx.returnType());
        boolean hasMethodBody = asBoolean(ctx.methodBody());

        if (!hasReturnType
                && hasMethodBody
                && methodName.equals(className)) { // constructor declaration

            methodNode = createConstructorNodeForClass(methodName, parameters, exceptions, code, classNode, modifiers);
        } else { // class memeber method declaration
            if (!hasReturnType && hasMethodBody && (0 == modifierManager.getModifierCount())) {
                throw createParsingFailedException("Invalid method declaration: " + methodName, ctx);
            }

            methodNode = createMethodNodeForClass(ctx, modifierManager, methodName, returnType, parameters, exceptions, code, classNode, modifiers);
        }

        modifierManager.attachAnnotations(methodNode);
        return methodNode;
    }

    private MethodNode createMethodNodeForClass(MethodDeclarationContext ctx, ModifierManager modifierManager, String methodName, ClassNode returnType, Parameter[] parameters, ClassNode[] exceptions, Statement code, ClassNode classNode, int modifiers) {
        MethodNode methodNode;
        if (asBoolean(ctx.elementValue())) { // the code of annotation method
            code = configureAST(
                    new ExpressionStatement(
                            this.visitElementValue(ctx.elementValue())),
                    ctx.elementValue());

        }

        modifiers |= !modifierManager.contains(STATIC) && (classNode.isInterface() || (isTrue(classNode, IS_INTERFACE_WITH_DEFAULT_METHODS) && !modifierManager.contains(DEFAULT))) ? Opcodes.ACC_ABSTRACT : 0;

        checkWhetherMethodNodeWithSameSignatureExists(classNode, methodName, parameters, ctx);

        methodNode = classNode.addMethod(methodName, modifiers, returnType, parameters, exceptions, code);

        methodNode.setAnnotationDefault(asBoolean(ctx.elementValue()));
        return methodNode;
    }

    private void checkWhetherMethodNodeWithSameSignatureExists(ClassNode classNode, String methodName, Parameter[] parameters, MethodDeclarationContext ctx) {
        MethodNode sameSigMethodNode = classNode.getDeclaredMethod(methodName, parameters);

        if (null == sameSigMethodNode) {
            return;
        }

        throw createParsingFailedException("The method " +  sameSigMethodNode.getText() + " duplicates another method of the same signature", ctx);
    }

    private ConstructorNode createConstructorNodeForClass(String methodName, Parameter[] parameters, ClassNode[] exceptions, Statement code, ClassNode classNode, int modifiers) {
        ConstructorCallExpression thisOrSuperConstructorCallExpression = this.checkThisAndSuperConstructorCall(code);
        if (asBoolean(thisOrSuperConstructorCallExpression)) {
            throw createParsingFailedException(thisOrSuperConstructorCallExpression.getText() + " should be the first statement in the constructor[" + methodName + "]", thisOrSuperConstructorCallExpression);
        }

        return classNode.addConstructor(
                modifiers,
                parameters,
                exceptions,
                code);
    }

    @Override
    public String visitMethodName(MethodNameContext ctx) {
        if (asBoolean(ctx.identifier())) {
            return this.visitIdentifier(ctx.identifier());
        }

        if (asBoolean(ctx.stringLiteral())) {
            return this.visitStringLiteral(ctx.stringLiteral()).getText();
        }

        throw createParsingFailedException("Unsupported method name: " + ctx.getText(), ctx);
    }

    @Override
    public ClassNode visitReturnType(ReturnTypeContext ctx) {
        if (!asBoolean(ctx)) {
            return ClassHelper.OBJECT_TYPE;
        }

        if (asBoolean(ctx.type())) {
            return this.visitType(ctx.type());
        }

        if (asBoolean(ctx.VOID())) {
            return ClassHelper.VOID_TYPE;
        }

        throw createParsingFailedException("Unsupported return type: " + ctx.getText(), ctx);
    }

    @Override
    public Statement visitMethodBody(MethodBodyContext ctx) {
        if (!asBoolean(ctx)) {
            return null;
        }

        return configureAST(this.visitBlock(ctx.block()), ctx);
    }

    @Override
    public DeclarationListStatement visitLocalVariableDeclaration(LocalVariableDeclarationContext ctx) {
        return configureAST(this.visitVariableDeclaration(ctx.variableDeclaration()), ctx);
    }

    private ModifierManager createModifierManager(VariableDeclarationContext ctx) {
        return new ModifierManager(this, this.visitClassifiedModifiers(ctx.classifiedModifiers()));
    }

    private DeclarationListStatement createMultiAssignmentDeclarationListStatement(VariableDeclarationContext ctx, ModifierManager modifierManager) {
        /*
        if (!modifierManager.contains(DEF)) {
            throw createParsingFailedException("keyword def is required to declare tuple, e.g. def (int a, int b) = [1, 2]", ctx);
        }
        */

        List<Expression> list = new ArrayList<>();
        for (Expression e : this.visitTypeNamePairs(ctx.typeNamePairs())) {
            modifierManager.processVariableExpression((VariableExpression) e);
            list.add(e);
        }
        return configureAST(
                new DeclarationListStatement(
                        configureAST(
                                modifierManager.attachAnnotations(
                                        new DeclarationExpression(
                                                new ArgumentListExpression(
                                                        list
                                                ),
                                                this.createGroovyTokenByType(ctx.ASSIGN().getSymbol(), Types.ASSIGN),
                                                this.visitVariableInitializer(ctx.variableInitializer())
                                        )
                                ),
                                ctx
                        )
                ),
                ctx
        );
    }

    @Override
    public List<ModifierNode> visitClassifiedModifiers(ClassifiedModifiersContext ctx) {
        List<ModifierNode> modifierNodeList = Collections.emptyList();

        if (!asBoolean(ctx)) {
            return modifierNodeList;
        }

        if (asBoolean(ctx.variableModifiers())) {
            modifierNodeList = this.visitVariableModifiers(ctx.variableModifiers());
        } if (asBoolean(ctx.modifiers())) {
            modifierNodeList = this.visitModifiers(ctx.modifiers());
        }

        return modifierNodeList;
    }

    @Override
    public DeclarationListStatement visitVariableDeclaration(VariableDeclarationContext ctx) {
        ModifierManager modifierManager = this.createModifierManager(ctx);

        if (asBoolean(ctx.typeNamePairs())) { // e.g. def (int a, int b) = [1, 2]
            return this.createMultiAssignmentDeclarationListStatement(ctx, modifierManager);
        }

        ClassNode variableType = this.visitType(ctx.type());
        ctx.variableDeclarators().putNodeMetaData(VARIABLE_DECLARATION_VARIABLE_TYPE, variableType);
        List<DeclarationExpression> declarationExpressionList = this.visitVariableDeclarators(ctx.variableDeclarators());

        // if classNode is not null, the variable declaration is for class declaration. In other words, it is a field declaration
        ClassNode classNode = ctx.getNodeMetaData(CLASS_DECLARATION_CLASS_NODE);

        if (asBoolean(classNode)) {
            return createFieldDeclarationListStatement(ctx, modifierManager, variableType, declarationExpressionList, classNode);
        }

        for (DeclarationExpression e : declarationExpressionList) {
            VariableExpression variableExpression = (VariableExpression) e.getLeftExpression();

            modifierManager.processVariableExpression(variableExpression);
            modifierManager.attachAnnotations(e);
        }

        int size = declarationExpressionList.size();
        if (size > 0) {
            DeclarationExpression declarationExpression = declarationExpressionList.get(0);

            if (1 == size) {
                configureAST(declarationExpression, ctx);
            } else {
                // Tweak start of first declaration
                declarationExpression.setLineNumber(ctx.getStart().getLine());
                declarationExpression.setColumnNumber(ctx.getStart().getCharPositionInLine() + 1);
            }
        }

        return configureAST(new DeclarationListStatement(declarationExpressionList), ctx);
    }

    private DeclarationListStatement createFieldDeclarationListStatement(VariableDeclarationContext ctx, ModifierManager modifierManager, ClassNode variableType, List<DeclarationExpression> declarationExpressionList, ClassNode classNode) {
        for (int i = 0, n = declarationExpressionList.size(); i < n; i++) {
            DeclarationExpression declarationExpression = declarationExpressionList.get(i);
            VariableExpression variableExpression = (VariableExpression) declarationExpression.getLeftExpression();

            String fieldName = variableExpression.getName();

            int modifiers = modifierManager.getClassMemberModifiersOpValue();

            Expression initialValue = EmptyExpression.INSTANCE.equals(declarationExpression.getRightExpression()) ? null : declarationExpression.getRightExpression();
            Object defaultValue = findDefaultValueByType(variableType);

            if (classNode.isInterface()) {
                if (!asBoolean(initialValue)) {
                    initialValue = !asBoolean(defaultValue) ? null : new ConstantExpression(defaultValue);
                }

                modifiers |= Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
            }

            if (isFieldDeclaration(modifierManager, classNode)) {
                declareField(ctx, modifierManager, variableType, classNode, i, variableExpression, fieldName, modifiers, initialValue);
            } else {
                declareProperty(ctx, modifierManager, variableType, classNode, i, variableExpression, fieldName, modifiers, initialValue);
            }
        }

        return null;
    }

    private void declareProperty(VariableDeclarationContext ctx, ModifierManager modifierManager, ClassNode variableType, ClassNode classNode, int i, VariableExpression variableExpression, String fieldName, int modifiers, Expression initialValue) {
        if (classNode.hasProperty(fieldName)) {
            throw createParsingFailedException("The property '" + fieldName + "' is declared multiple times", ctx);
        }

        PropertyNode propertyNode;
        FieldNode fieldNode = classNode.getDeclaredField(fieldName);

        if (fieldNode != null && !classNode.hasProperty(fieldName)) {
            classNode.getFields().remove(fieldNode);

            propertyNode = new PropertyNode(fieldNode, modifiers | Opcodes.ACC_PUBLIC, null, null);
            classNode.addProperty(propertyNode);
        } else {
            propertyNode =
                    classNode.addProperty(
                            fieldName,
                            modifiers | Opcodes.ACC_PUBLIC,
                            variableType,
                            initialValue,
                            null,
                            null);

            fieldNode = propertyNode.getField();
        }

        fieldNode.setModifiers(modifiers & ~Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE);
        fieldNode.setSynthetic(!classNode.isInterface());
        modifierManager.attachAnnotations(fieldNode);

        groovydocManager.handle(fieldNode, ctx);
        groovydocManager.handle(propertyNode, ctx);

        if (0 == i) {
            configureAST(fieldNode, ctx, initialValue);
            configureAST(propertyNode, ctx, initialValue);
        } else {
            configureAST(fieldNode, variableExpression, initialValue);
            configureAST(propertyNode, variableExpression, initialValue);
        }
        // GRECLIPSE add
        fieldNode.setNameStart(variableExpression.getStart());
        fieldNode.setNameEnd(variableExpression.getEnd() - 1);
        propertyNode.setNameStart(variableExpression.getStart());
        propertyNode.setNameEnd(variableExpression.getEnd() - 1);
        if (initialValue == null) {
            fieldNode.setEnd(variableExpression.getEnd());
            fieldNode.setLastLineNumber(variableExpression.getLastLineNumber());
            fieldNode.setLastColumnNumber(variableExpression.getLastColumnNumber());
            propertyNode.setEnd(variableExpression.getEnd());
            propertyNode.setLastLineNumber(variableExpression.getLastLineNumber());
            propertyNode.setLastColumnNumber(variableExpression.getLastColumnNumber());
        }
        // GRECLIPSE end
    }

    private void declareField(VariableDeclarationContext ctx, ModifierManager modifierManager, ClassNode variableType, ClassNode classNode, int i, VariableExpression variableExpression, String fieldName, int modifiers, Expression initialValue) {
        FieldNode existingFieldNode = classNode.getDeclaredField(fieldName);
        if (null != existingFieldNode && !existingFieldNode.isSynthetic()) {
            throw createParsingFailedException("The field '" + fieldName + "' is declared multiple times", ctx);
        }

        FieldNode fieldNode;
        PropertyNode propertyNode = classNode.getProperty(fieldName);

        if (null != propertyNode && propertyNode.getField().isSynthetic()) {
            classNode.getFields().remove(propertyNode.getField());
            fieldNode = new FieldNode(fieldName, modifiers, variableType, classNode.redirect(), initialValue);
            propertyNode.setField(fieldNode);
            classNode.addField(fieldNode);
        } else {
            fieldNode =
                    classNode.addField(
                            fieldName,
                            modifiers,
                            variableType,
                            initialValue);
        }

        modifierManager.attachAnnotations(fieldNode);
        groovydocManager.handle(fieldNode, ctx);

        if (0 == i) {
            configureAST(fieldNode, ctx, initialValue);
        } else {
            configureAST(fieldNode, variableExpression, initialValue);
        }
        // GRECLIPSE add
        fieldNode.setNameStart(variableExpression.getStart());
        fieldNode.setNameEnd(variableExpression.getEnd() - 1);
        if (initialValue == null) {
            fieldNode.setEnd(variableExpression.getEnd());
            fieldNode.setLastLineNumber(variableExpression.getLastLineNumber());
            fieldNode.setLastColumnNumber(variableExpression.getLastColumnNumber());
        }
        // GRECLIPSE end
    }

    private boolean isFieldDeclaration(ModifierManager modifierManager, ClassNode classNode) {
        return classNode.isInterface() || modifierManager.containsVisibilityModifier();
    }

    @Override
    public List<Expression> visitTypeNamePairs(TypeNamePairsContext ctx) {
        List<Expression> list = new ArrayList<>();
        for (TypeNamePairContext typeNamePairContext : ctx.typeNamePair()) {
            VariableExpression variableExpression = visitTypeNamePair(typeNamePairContext);
            list.add(variableExpression);
        }
        return list;
    }

    @Override
    public VariableExpression visitTypeNamePair(TypeNamePairContext ctx) {
        /* GRECLIPSE edit
        return configureAST(
                new VariableExpression(
                        this.visitVariableDeclaratorId(ctx.variableDeclaratorId()).getName(),
                        this.visitType(ctx.type())),
                ctx);
        */
        VariableExpression var = new VariableExpression(visitIdentifier(ctx.variableDeclaratorId().identifier()), visitType(ctx.type()));
        return configureAST(var, ctx.variableDeclaratorId());
        // GRECLIPSE end
    }

    @Override
    public List<DeclarationExpression> visitVariableDeclarators(VariableDeclaratorsContext ctx) {
        ClassNode variableType = ctx.getNodeMetaData(VARIABLE_DECLARATION_VARIABLE_TYPE);
        Objects.requireNonNull(variableType, "variableType should not be null");

        List<DeclarationExpression> list = new LinkedList<>();
        for (VariableDeclaratorContext e : ctx.variableDeclarator()) {
            e.putNodeMetaData(VARIABLE_DECLARATION_VARIABLE_TYPE, variableType);
            list.add(this.visitVariableDeclarator(e));
            }
            return list;
    }

    @Override
    public DeclarationExpression visitVariableDeclarator(VariableDeclaratorContext ctx) {
        ClassNode variableType = ctx.getNodeMetaData(VARIABLE_DECLARATION_VARIABLE_TYPE);
        Objects.requireNonNull(variableType, "variableType should not be null");

        org.codehaus.groovy.syntax.Token token;
        if (asBoolean(ctx.ASSIGN())) {
            token = createGroovyTokenByType(ctx.ASSIGN().getSymbol(), Types.ASSIGN);
        } else {
            token = new org.codehaus.groovy.syntax.Token(Types.ASSIGN, ASSIGN_STR, ctx.start.getLine(), 1);
        }

        return configureAST(
                new DeclarationExpression(
                        configureAST(
                                new VariableExpression(
                                        this.visitVariableDeclaratorId(ctx.variableDeclaratorId()).getName(),
                                        variableType
                                ),
                                ctx.variableDeclaratorId()),
                        token,
                        this.visitVariableInitializer(ctx.variableInitializer())),
                ctx);
    }

    @Override
    public Expression visitVariableInitializer(VariableInitializerContext ctx) {
        if (!asBoolean(ctx)) {
            return EmptyExpression.INSTANCE;
        }

        return configureAST(
                this.visitEnhancedStatementExpression(ctx.enhancedStatementExpression()),
                ctx);
    }

    @Override
    public List<Expression> visitVariableInitializers(VariableInitializersContext ctx) {
        if (!asBoolean(ctx)) {
            return Collections.emptyList();
        }

        List<Expression> list = new ArrayList<>();
        for (VariableInitializerContext variableInitializerContext : ctx.variableInitializer()) {
            Expression expression = visitVariableInitializer(variableInitializerContext);
            list.add(expression);
        }
        return list;
    }

    @Override
    public List<Expression> visitArrayInitializer(ArrayInitializerContext ctx) {
        if (!asBoolean(ctx)) {
            return Collections.emptyList();
        }

        return this.visitVariableInitializers(ctx.variableInitializers());
    }

    @Override
    public Statement visitBlock(BlockContext ctx) {
        if (!asBoolean(ctx)) {
            return this.createBlockStatement();
        }

        return configureAST(
                this.visitBlockStatementsOpt(ctx.blockStatementsOpt()),
                ctx);
    }


    @Override
    public ExpressionStatement visitNormalExprAlt(NormalExprAltContext ctx) {
        return configureAST(new ExpressionStatement((Expression) this.visit(ctx.expression())), ctx);
    }

/*
    @Override
    public Expression visitEnhancedExpression(EnhancedExpressionContext ctx) {
        Expression expression;

        if (asBoolean(ctx.expression())) {
            expression = (Expression) this.visit(ctx.expression());
        } else if (asBoolean(ctx.standardLambdaExpression())) {
            expression = this.visitStandardLambdaExpression(ctx.standardLambdaExpression());
        } else {
            throw createParsingFailedException("Unsupported enhanced expression: " + ctx.getText(), ctx);
        }

        return configureAST(expression, ctx);
    }
*/

    @Override
    public ExpressionStatement visitCommandExprAlt(CommandExprAltContext ctx) {
        return configureAST(new ExpressionStatement(this.visitCommandExpression(ctx.commandExpression())), ctx);
    }

    @Override
    public Expression visitCommandExpression(CommandExpressionContext ctx) {
        Expression baseExpr = this.visitPathExpression(ctx.pathExpression());
        Expression arguments = this.visitEnhancedArgumentList(ctx.enhancedArgumentList());

        MethodCallExpression methodCallExpression;
        if (baseExpr instanceof PropertyExpression) { // e.g. obj.a 1, 2
            methodCallExpression =
                    configureAST(
                            this.createMethodCallExpression(
                                    (PropertyExpression) baseExpr, arguments),
                            arguments);

        } else if (baseExpr instanceof MethodCallExpression && !isInsideParentheses(baseExpr)) { // e.g. m {} a, b  OR  m(...) a, b
            if (asBoolean(arguments)) {
                // The error should never be thrown.
                throw new GroovyBugError("When baseExpr is a instance of MethodCallExpression, which should follow NO argumentList");
            }

            methodCallExpression = (MethodCallExpression) baseExpr;
        } else if (
                !isInsideParentheses(baseExpr)
                        && (baseExpr instanceof VariableExpression /* e.g. m 1, 2 */
                        || baseExpr instanceof GStringExpression /* e.g. "$m" 1, 2 */
                        || (baseExpr instanceof ConstantExpression && isTrue(baseExpr, IS_STRING)) /* e.g. "m" 1, 2 */)
                ) {
            methodCallExpression =
                    configureAST(
                            this.createMethodCallExpression(baseExpr, arguments),
                            arguments);
        } else { // e.g. a[x] b, new A() b, etc.
            methodCallExpression = configureAST(this.createCallMethodCallExpression(baseExpr, arguments), arguments);
        }

        methodCallExpression.putNodeMetaData(IS_COMMAND_EXPRESSION, true);
        // GRECLIPSE add
        methodCallExpression.setColumnNumber(baseExpr.getColumnNumber());
        methodCallExpression.setLineNumber(baseExpr.getLineNumber());
        methodCallExpression.setStart(baseExpr.getStart());

        if (methodCallExpression.getMethod() instanceof ConstantExpression) {
            Expression nameExpr = methodCallExpression.getMethod();
            methodCallExpression.setNameStart(nameExpr.getStart());
            methodCallExpression.setNameEnd(nameExpr.getEnd() - 1);
        }
        // GRECLIPSE end

        if (!asBoolean(ctx.commandArgument())) {
            return configureAST(methodCallExpression, ctx);
        }

        Expression r = methodCallExpression;
        for (CommandArgumentContext commandArgumentContext : ctx.commandArgument()) {
            commandArgumentContext.putNodeMetaData(CMD_EXPRESSION_BASE_EXPR, r);
            r = this.visitCommandArgument(commandArgumentContext);
        }

        return configureAST(r, ctx);
    }

    @Override
    public Expression visitCommandArgument(CommandArgumentContext ctx) {
        // e.g. x y a b     we call "x y" as the base expression
        Expression baseExpr = ctx.getNodeMetaData(CMD_EXPRESSION_BASE_EXPR);

        Expression primaryExpr = (Expression) this.visit(ctx.primary());

        if (asBoolean(ctx.enhancedArgumentList())) { // e.g. x y a b
            if (baseExpr instanceof PropertyExpression) { // the branch should never reach, because a.b.c will be parsed as a path expression, not a method call
                throw createParsingFailedException("Unsupported command argument: " + ctx.getText(), ctx);
            }

            // the following code will process "a b" of "x y a b"
            MethodCallExpression methodCallExpression =
                    new MethodCallExpression(
                            baseExpr,
                            this.createConstantExpression(primaryExpr),
                            this.visitEnhancedArgumentList(ctx.enhancedArgumentList())
                    );
            methodCallExpression.setImplicitThis(false);

            return configureAST(methodCallExpression, ctx);
        } else if (asBoolean(ctx.pathElement())) { // e.g. x y a.b
            Expression pathExpression =
                    this.createPathExpression(
                            configureAST(
                                    new PropertyExpression(baseExpr, this.createConstantExpression(primaryExpr)),
                                    primaryExpr
                            ),
                            ctx.pathElement()
                    );

            return configureAST(pathExpression, ctx);
        }

        // e.g. x y a
        return configureAST(
                new PropertyExpression(
                        baseExpr,
                        primaryExpr instanceof VariableExpression
                                ? this.createConstantExpression(primaryExpr)
                                : primaryExpr
                ),
                primaryExpr
        );
    }


    // expression {    --------------------------------------------------------------------

    @Override
    public ClassNode visitCastParExpression(CastParExpressionContext ctx) {
        return this.visitType(ctx.type());
    }

    @Override
    public Expression visitParExpression(ParExpressionContext ctx) {
        Expression expression = this.visitExpressionInPar(ctx.expressionInPar());

        Integer insideParenLevel = expression.getNodeMetaData(INSIDE_PARENTHESES_LEVEL);
        if (null != insideParenLevel) {
            insideParenLevel++;
        } else {
            insideParenLevel = 1;
        }
        expression.putNodeMetaData(INSIDE_PARENTHESES_LEVEL, insideParenLevel);

        // GRECLIPSE edit
        //return configureAST(expression, ctx);
        return expression;
        // GRECLIPSE end
    }

    @Override
    public Expression visitExpressionInPar(ExpressionInParContext ctx) {
        //return this.visitEnhancedExpression(ctx.enhancedExpression());
        return this.visitEnhancedStatementExpression(ctx.enhancedStatementExpression());
    }

    @Override
    public Expression visitEnhancedStatementExpression(EnhancedStatementExpressionContext ctx) {
        Expression expression;

        if (asBoolean(ctx.statementExpression())) {
            expression = ((ExpressionStatement) this.visit(ctx.statementExpression())).getExpression();
        } else if (asBoolean(ctx.standardLambdaExpression())) {
            expression = this.visitStandardLambdaExpression(ctx.standardLambdaExpression());
        } else {
            throw createParsingFailedException("Unsupported enhanced statement expression: " + ctx.getText(), ctx);
        }

        return configureAST(expression, ctx);
    }


    @Override
    public Expression visitPathExpression(PathExpressionContext ctx) {
        return this.createPathExpression((Expression) this.visit(ctx.primary()), ctx.pathElement());
    }

    @Override
    public Expression visitPathElement(PathElementContext ctx) {
        Expression baseExpr = ctx.getNodeMetaData(PATH_EXPRESSION_BASE_EXPR);
        Objects.requireNonNull(baseExpr, "baseExpr is required!");

        if (asBoolean(ctx.namePart())) {
            Expression namePartExpr = this.visitNamePart(ctx.namePart());
            GenericsType[] genericsTypes = this.visitNonWildcardTypeArguments(ctx.nonWildcardTypeArguments());


            if (asBoolean(ctx.DOT())) {
                boolean isSafeChain = isTrue(baseExpr, PATH_EXPRESSION_BASE_EXPR_SAFE_CHAIN);

                return createDotExpression(ctx, baseExpr, namePartExpr, genericsTypes, isSafeChain);
            } else if (asBoolean(ctx.SAFE_DOT())) {
                return createDotExpression(ctx, baseExpr, namePartExpr, genericsTypes, true);
            } else if (asBoolean(ctx.SAFE_CHAIN_DOT())) { // e.g. obj??.a  OR obj??.@a
                Expression expression = createDotExpression(ctx, baseExpr, namePartExpr, genericsTypes, true);
                expression.putNodeMetaData(PATH_EXPRESSION_BASE_EXPR_SAFE_CHAIN, true);

                return expression;
            }  else if (asBoolean(ctx.METHOD_POINTER())) { // e.g. obj.&m
                return configureAST(new MethodPointerExpression(baseExpr, namePartExpr), ctx);
            } else if (asBoolean(ctx.METHOD_REFERENCE())) { // e.g. obj::m
                return configureAST(new MethodReferenceExpression(baseExpr, namePartExpr), ctx);
            } else if (asBoolean(ctx.SPREAD_DOT())) {
                if (asBoolean(ctx.AT())) { // e.g. obj*.@a
                    AttributeExpression attributeExpression = new AttributeExpression(baseExpr, namePartExpr, true);

                    attributeExpression.setSpreadSafe(true);

                    return configureAST(attributeExpression, ctx);
                } else { // e.g. obj*.p
                    PropertyExpression propertyExpression = new PropertyExpression(baseExpr, namePartExpr, true);
                    propertyExpression.putNodeMetaData(PATH_EXPRESSION_BASE_EXPR_GENERICS_TYPES, genericsTypes);

                    propertyExpression.setSpreadSafe(true);

                    return configureAST(propertyExpression, ctx);
                }
            }
        }

        if (asBoolean(ctx.indexPropertyArgs())) { // e.g. list[1, 3, 5]
            Tuple2<Token, Expression> tuple = this.visitIndexPropertyArgs(ctx.indexPropertyArgs());
            boolean isSafeChain = isTrue(baseExpr, PATH_EXPRESSION_BASE_EXPR_SAFE_CHAIN);

            return configureAST(
                    new BinaryExpression(baseExpr, createGroovyToken(tuple.getFirst()), tuple.getSecond(), isSafeChain || asBoolean(ctx.indexPropertyArgs().QUESTION())),
                    ctx);
        }

        if (asBoolean(ctx.namedPropertyArgs())) { // this is a special way to new instance, e.g. Person(name: 'Daniel.Sun', location: 'Shanghai')
            List<MapEntryExpression> mapEntryExpressionList =
                    this.visitNamedPropertyArgs(ctx.namedPropertyArgs());

            Expression right;
            if (mapEntryExpressionList.size() == 1) {
                MapEntryExpression mapEntryExpression = mapEntryExpressionList.get(0);

                if (mapEntryExpression.getKeyExpression() instanceof SpreadMapExpression) {
                    right = mapEntryExpression.getKeyExpression();
                } else {
                    right = mapEntryExpression;
                }
            } else {
                List<Expression> list = new LinkedList<>();
                for (MapEntryExpression e : mapEntryExpressionList) {
                    list.add(e.getKeyExpression() instanceof SpreadMapExpression ?
                            e.getKeyExpression() : e);
                }
                ListExpression listExpression = configureAST(new ListExpression(list), ctx.namedPropertyArgs());
                listExpression.setWrapped(true);
                right = listExpression;
            }

            return configureAST(
                    new BinaryExpression(baseExpr, createGroovyToken(ctx.namedPropertyArgs().LBRACK().getSymbol()), right),
                    ctx);
        }

        if (asBoolean(ctx.arguments())) {
            Expression argumentsExpr = this.visitArguments(ctx.arguments());
            // GRECLIPSE edit
            //configureAST(argumentsExpr, ctx);
            // GRECLIPSE end

            if (isInsideParentheses(baseExpr)) { // e.g. (obj.x)(), (obj.@x)()
                return configureAST(createCallMethodCallExpression(baseExpr, argumentsExpr), ctx);
            }

            if (baseExpr instanceof AttributeExpression) { // e.g. obj.@a(1, 2)
                AttributeExpression attributeExpression = (AttributeExpression) baseExpr;
                attributeExpression.setSpreadSafe(false); // whether attributeExpression is spread safe or not, we must reset it as false

                return configureAST(this.createCallMethodCallExpression(attributeExpression, argumentsExpr, true), ctx);
            }

            if (baseExpr instanceof PropertyExpression) { // e.g. obj.a(1, 2)
                MethodCallExpression methodCallExpression =
                        this.createMethodCallExpression((PropertyExpression) baseExpr, argumentsExpr);

                return configureAST(methodCallExpression, ctx);
            }

            if (baseExpr instanceof VariableExpression) { // void and primitive type AST node must be an instance of VariableExpression
                String baseExprText = baseExpr.getText();
                if (VOID_STR.equals(baseExprText)) { // e.g. void()
                    return configureAST(this.createCallMethodCallExpression(this.createConstantExpression(baseExpr), argumentsExpr), ctx);
                } else if (PRIMITIVE_TYPE_SET.contains(baseExprText)) { // e.g. int(), long(), float(), etc.
                    throw createParsingFailedException("Primitive type literal: " + baseExprText + " cannot be used as a method name", ctx);
                }
            }

            if (baseExpr instanceof VariableExpression
                    || baseExpr instanceof GStringExpression
                    || (baseExpr instanceof ConstantExpression && isTrue(baseExpr, IS_STRING))) { // e.g. m(), "$m"(), "m"()

                String baseExprText = baseExpr.getText();
                if (SUPER_STR.equals(baseExprText) || THIS_STR.equals(baseExprText)) { // e.g. this(...), super(...)
                    // class declaration is not allowed in the closure,
                    // so if this and super is inside the closure, it will not be constructor call.
                    // e.g. src/test/org/codehaus/groovy/transform/MapConstructorTransformTest.groovy:
                    // @MapConstructor(pre={ super(args?.first, args?.last); args = args ?: [:] }, post = { first = first?.toUpperCase() })
                    if (visitingClosureCnt > 0) {
                        return configureAST(
                                new MethodCallExpression(
                                        baseExpr,
                                        baseExprText,
                                        argumentsExpr
                                ),
                                ctx);
                    }

                    return configureAST(
                            new ConstructorCallExpression(
                                    SUPER_STR.equals(baseExprText)
                                            ? ClassNode.SUPER
                                            : ClassNode.THIS,
                                    argumentsExpr
                            ),
                            ctx);
                }

                MethodCallExpression methodCallExpression =
                        this.createMethodCallExpression(baseExpr, argumentsExpr);

                return configureAST(methodCallExpression, ctx);
            }

            // e.g. 1(), 1.1(), ((int) 1 / 2)(1, 2), {a, b -> a + b }(1, 2), m()()
            return configureAST(createCallMethodCallExpression(baseExpr, argumentsExpr), ctx);
        }

        if (asBoolean(ctx.closure())) {
            ClosureExpression closureExpression = this.visitClosure(ctx.closure());

            if (baseExpr instanceof MethodCallExpression) {
                MethodCallExpression methodCallExpression = (MethodCallExpression) baseExpr;
                Expression argumentsExpression = methodCallExpression.getArguments();

                if (argumentsExpression instanceof ArgumentListExpression) { // normal arguments, e.g. 1, 2
                    ArgumentListExpression argumentListExpression = (ArgumentListExpression) argumentsExpression;
                    argumentListExpression.getExpressions().add(closureExpression);

                    return configureAST(methodCallExpression, ctx);
                }

                if (argumentsExpression instanceof TupleExpression) { // named arguments, e.g. x: 1, y: 2
                    TupleExpression tupleExpression = (TupleExpression) argumentsExpression;
                    NamedArgumentListExpression namedArgumentListExpression = (NamedArgumentListExpression) tupleExpression.getExpression(0);

                    if (asBoolean(tupleExpression.getExpressions())) {
                        List<Expression> list = new ArrayList<>();
                        for (Expression expression : Arrays.asList(
                                configureAST(
                                        new MapExpression(namedArgumentListExpression.getMapEntryExpressions()),
                                        namedArgumentListExpression
                                ),
                                closureExpression
                        )) {
                            list.add(expression);
                        }
                        methodCallExpression.setArguments(
                                configureAST(
                                        new ArgumentListExpression(
                                                list
                                        ),
                                        tupleExpression
                                )
                        );
                    } else {
                        // the branch should never reach, because named arguments must not be empty
                        methodCallExpression.setArguments(
                                configureAST(
                                        new ArgumentListExpression(closureExpression),
                                        tupleExpression));
                    }


                    return configureAST(methodCallExpression, ctx);
                }

            }

            // e.g. 1 {}, 1.1 {}
            if (baseExpr instanceof ConstantExpression && isTrue(baseExpr, IS_NUMERIC)) {
                return configureAST(this.createCallMethodCallExpression(
                        baseExpr,
                        configureAST(
                            new ArgumentListExpression(closureExpression),
                            closureExpression
                        )
                ), ctx);
            }

            if (baseExpr instanceof PropertyExpression) { // e.g. obj.m {  }
                PropertyExpression propertyExpression = (PropertyExpression) baseExpr;

                MethodCallExpression methodCallExpression =
                        this.createMethodCallExpression(
                                propertyExpression,
                                configureAST(
                                        new ArgumentListExpression(closureExpression),
                                        closureExpression
                                )
                        );

                return configureAST(methodCallExpression, ctx);
            }

            // e.g.  m { return 1; }
            MethodCallExpression methodCallExpression =
                    new MethodCallExpression(
                            VariableExpression.THIS_EXPRESSION,

                            (baseExpr instanceof VariableExpression)
                                    ? this.createConstantExpression(baseExpr)
                                    : baseExpr,

                            configureAST(
                                    new ArgumentListExpression(closureExpression),
                                    closureExpression)
                    );


            return configureAST(methodCallExpression, ctx);
        }

        throw createParsingFailedException("Unsupported path element: " + ctx.getText(), ctx);
    }

    private Expression createDotExpression(PathElementContext ctx, Expression baseExpr, Expression namePartExpr, GenericsType[] genericsTypes, boolean safe) {
        if (asBoolean(ctx.AT())) { // e.g. obj.@a  OR  obj?.@a
            return configureAST(new AttributeExpression(baseExpr, namePartExpr, safe), ctx);
        } else { // e.g. obj.p  OR  obj?.p
            PropertyExpression propertyExpression = new PropertyExpression(baseExpr, namePartExpr, safe);
            propertyExpression.putNodeMetaData(PATH_EXPRESSION_BASE_EXPR_GENERICS_TYPES, genericsTypes);

            return configureAST(propertyExpression, ctx);
        }
    }

    private MethodCallExpression createCallMethodCallExpression(Expression baseExpr, Expression argumentsExpr) {
        return createCallMethodCallExpression(baseExpr, argumentsExpr, false);
    }

    private MethodCallExpression createCallMethodCallExpression(Expression baseExpr, Expression argumentsExpr, boolean implicitThis) {
        MethodCallExpression methodCallExpression =
                new MethodCallExpression(baseExpr, CALL_STR, argumentsExpr);

        methodCallExpression.setImplicitThis(implicitThis);

        return methodCallExpression;
    }

    @Override
    public GenericsType[] visitNonWildcardTypeArguments(NonWildcardTypeArgumentsContext ctx) {
        if (!asBoolean(ctx)) {
            return null;
        }

        List<GenericsType> list = new ArrayList<>();
        for (ClassNode classNode : this.visitTypeList(ctx.typeList())) {
            GenericsType genericsType = createGenericsType(classNode);
            list.add(genericsType);
        }
        return list.toArray(new GenericsType[0]);
    }

    @Override
    public ClassNode[] visitTypeList(TypeListContext ctx) {
        if (!asBoolean(ctx)) {
            return ClassNode.EMPTY_ARRAY;
        }

        List<ClassNode> list = new ArrayList<>();
        for (TypeContext typeContext : ctx.type()) {
            ClassNode classNode = visitType(typeContext);
            list.add(classNode);
        }
        return list.toArray(new ClassNode[0]);
    }

    @Override
    public Expression visitArguments(ArgumentsContext ctx) {
        if (asBoolean(ctx) && asBoolean(ctx.COMMA()) && !asBoolean(ctx.enhancedArgumentList())) {
            throw createParsingFailedException("Expression expected", ctx.COMMA());
        }

        if (!asBoolean(ctx) || !asBoolean(ctx.enhancedArgumentList())) {
            // GRECLIPSE edit -- exclude parentheses from source range
            //return new ArgumentListExpression();
            ArgumentListExpression ale = new ArgumentListExpression();
            if (ctx != null) {
                ale.setStart(locationSupport.findOffset(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine() + 2));
                ale.setEnd(locationSupport.findOffset(ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine() + 1));
                int[] row_col = locationSupport.getRowCol(ale.getStart());
                ale.setLineNumber(row_col[0]);
                ale.setColumnNumber(row_col[1]);
                row_col = locationSupport.getRowCol(ale.getEnd());
                ale.setLastLineNumber(row_col[0]);
                ale.setLastColumnNumber(row_col[1]);
            }
            return ale;
            // GRECLIPSE end
        }

        // GRECLIPSE edit
        //return configureAST(this.visitEnhancedArgumentList(ctx.enhancedArgumentList()), ctx);
        return visitEnhancedArgumentList(ctx.enhancedArgumentList());
        // GRECLIPSE end
    }

    @Override
    public Expression visitEnhancedArgumentList(EnhancedArgumentListContext ctx) {
        if (!asBoolean(ctx)) {
            return null;
        }

        List<Expression> expressionList = new LinkedList<>();
        List<MapEntryExpression> mapEntryExpressionList = new LinkedList<>();

        for (EnhancedArgumentListElementContext c : ctx.enhancedArgumentListElement()) {
            Expression e = visitEnhancedArgumentListElement(c);
            if (e instanceof MapEntryExpression) {
                MapEntryExpression mapEntryExpression = (MapEntryExpression) e;
                validateDuplicatedNamedParameter(mapEntryExpressionList, mapEntryExpression);

                mapEntryExpressionList.add(mapEntryExpression);
            } else {
                expressionList.add(e);
            }
        }
        if (!asBoolean(mapEntryExpressionList)) { // e.g. arguments like  1, 2 OR  someArg, e -> e
            return configureAST(
                    new ArgumentListExpression(expressionList),
                    ctx);
        }

        if (!asBoolean(expressionList)) { // e.g. arguments like  x: 1, y: 2
            return configureAST(
                    new TupleExpression(
                            configureAST(
                                    new NamedArgumentListExpression(mapEntryExpressionList),
                                    ctx)),
                    ctx);
        }

        if (asBoolean(mapEntryExpressionList) && asBoolean(expressionList)) { // e.g. arguments like x: 1, 'a', y: 2, 'b', z: 3
            ArgumentListExpression argumentListExpression = new ArgumentListExpression(expressionList);
            argumentListExpression.getExpressions().add(0, new MapExpression(mapEntryExpressionList)); // TODO: confirm BUG OR NOT? All map entries will be put at first, which is not friendly to Groovy developers

            return configureAST(argumentListExpression, ctx);
        }

        throw createParsingFailedException("Unsupported argument list: " + ctx.getText(), ctx);
    }

    private void validateDuplicatedNamedParameter(List<MapEntryExpression> mapEntryExpressionList, MapEntryExpression mapEntryExpression) {
        Expression keyExpression = mapEntryExpression.getKeyExpression();

        if (null == keyExpression) {
            return;
        }

        if (isInsideParentheses(keyExpression)) {
            return;
        }

        String parameterName = keyExpression.getText();
        boolean isDuplicatedNamedParameter = false;

        for (MapEntryExpression me : mapEntryExpressionList) {
            if (me.getKeyExpression().getText().equals(parameterName)) {
                isDuplicatedNamedParameter = true;
                break;
            }
        }

        if (!isDuplicatedNamedParameter) {
            return;
        }

        throw createParsingFailedException("Duplicated named parameter '" + parameterName + "' found", mapEntryExpression);
    }

    @Override
    public Expression visitEnhancedArgumentListElement(EnhancedArgumentListElementContext ctx) {
        if (asBoolean(ctx.expressionListElement())) {
            return configureAST(this.visitExpressionListElement(ctx.expressionListElement()), ctx);
        }

        if (asBoolean(ctx.standardLambdaExpression())) {
            return configureAST(this.visitStandardLambdaExpression(ctx.standardLambdaExpression()), ctx);
        }

        if (asBoolean(ctx.mapEntry())) {
            return configureAST(this.visitMapEntry(ctx.mapEntry()), ctx);
        }

        throw createParsingFailedException("Unsupported enhanced argument list element: " + ctx.getText(), ctx);
    }

    @Override
    public ConstantExpression visitStringLiteral(StringLiteralContext ctx) {
        String text = parseStringLiteral(ctx.StringLiteral().getText());

        ConstantExpression constantExpression = new ConstantExpression(text, true);
        constantExpression.putNodeMetaData(IS_STRING, true);

        return configureAST(constantExpression, ctx);
    }

    private String parseStringLiteral(String text) {
        int slashyType = getSlashyType(text);
        boolean startsWithSlash = false;

        if (text.startsWith(TSQ_STR) || text.startsWith(TDQ_STR)) {
            text = StringUtils.removeCR(text); // remove CR in the multiline string

            text = StringUtils.trimQuotations(text, 3);
        } else if (text.startsWith(SQ_STR) || text.startsWith(DQ_STR) || (startsWithSlash = text.startsWith(SLASH_STR))) {
            if (startsWithSlash) { // the slashy string can span rows, so we have to remove CR for it
                text = StringUtils.removeCR(text); // remove CR in the multiline string
            }

            text = StringUtils.trimQuotations(text, 1);
        } else if (text.startsWith(DOLLAR_SLASH_STR)) {
            text = StringUtils.removeCR(text);

            text = StringUtils.trimQuotations(text, 2);
        }

        //handle escapes.
        return StringUtils.replaceEscapes(text, slashyType);
    }

    private int getSlashyType(String text) {
        return text.startsWith(SLASH_STR) ? StringUtils.SLASHY :
                    text.startsWith(DOLLAR_SLASH_STR) ? StringUtils.DOLLAR_SLASHY : StringUtils.NONE_SLASHY;
    }

    @Override
    public Tuple2<Token, Expression> visitIndexPropertyArgs(IndexPropertyArgsContext ctx) {
        List<Expression> expressionList = this.visitExpressionList(ctx.expressionList());


        if (expressionList.size() == 1) {
            Expression expr = expressionList.get(0);

            Expression indexExpr;
            if (expr instanceof SpreadExpression) { // e.g. a[*[1, 2]]
                ListExpression listExpression = new ListExpression(expressionList);
                listExpression.setWrapped(false);

                indexExpr = listExpression;
            } else { // e.g. a[1]
                indexExpr = expr;
            }

            return new Tuple2<>(ctx.LBRACK().getSymbol(), indexExpr);
        }

        // e.g. a[1, 2]
        ListExpression listExpression = new ListExpression(expressionList);
        listExpression.setWrapped(true);

        return new Tuple2<>(ctx.LBRACK().getSymbol(), (Expression) configureAST(listExpression, ctx));
    }

    @Override
    public List<MapEntryExpression> visitNamedPropertyArgs(NamedPropertyArgsContext ctx) {
        return this.visitMapEntryList(ctx.mapEntryList());
    }

    @Override
    public Expression visitNamePart(NamePartContext ctx) {
        if (asBoolean(ctx.identifier())) {
            return configureAST(new ConstantExpression(this.visitIdentifier(ctx.identifier())), ctx);
        } else if (asBoolean(ctx.stringLiteral())) {
            return configureAST(this.visitStringLiteral(ctx.stringLiteral()), ctx);
        } else if (asBoolean(ctx.dynamicMemberName())) {
            return configureAST(this.visitDynamicMemberName(ctx.dynamicMemberName()), ctx);
        } else if (asBoolean(ctx.keywords())) {
            return configureAST(new ConstantExpression(ctx.keywords().getText()), ctx);
        }

        throw createParsingFailedException("Unsupported name part: " + ctx.getText(), ctx);
    }

    @Override
    public Expression visitDynamicMemberName(DynamicMemberNameContext ctx) {
        if (asBoolean(ctx.parExpression())) {
            return configureAST(this.visitParExpression(ctx.parExpression()), ctx);
        } else if (asBoolean(ctx.gstring())) {
            return configureAST(this.visitGstring(ctx.gstring()), ctx);
        }

        throw createParsingFailedException("Unsupported dynamic member name: " + ctx.getText(), ctx);
    }

    @Override
    public Expression visitPostfixExpression(PostfixExpressionContext ctx) {
        Expression pathExpr = this.visitPathExpression(ctx.pathExpression());

        if (asBoolean(ctx.op)) {
            PostfixExpression postfixExpression = new PostfixExpression(pathExpr, createGroovyToken(ctx.op));

            if (visitingAssertStatementCnt > 0) {
                // powerassert requires different column for values, so we have to copy the location of op
                return configureAST(postfixExpression, ctx.op);
            } else {
                return configureAST(postfixExpression, ctx);
            }
        }

        return configureAST(pathExpr, ctx);
    }

    @Override
    public Expression visitPostfixExprAlt(PostfixExprAltContext ctx) {
        return this.visitPostfixExpression(ctx.postfixExpression());
    }

    @Override
    public Expression visitUnaryNotExprAlt(UnaryNotExprAltContext ctx) {
        if (asBoolean(ctx.NOT())) {
            return configureAST(
                    new NotExpression((Expression) this.visit(ctx.expression())),
                    ctx);
        }

        if (asBoolean(ctx.BITNOT())) {
            return configureAST(
                    new BitwiseNegationExpression((Expression) this.visit(ctx.expression())),
                    ctx);
        }

        throw createParsingFailedException("Unsupported unary expression: " + ctx.getText(), ctx);
    }

    @Override
    public CastExpression visitCastExprAlt(CastExprAltContext ctx) {
        /* GRECLIPSE edit
        return configureAST(
                new CastExpression(
                        this.visitCastParExpression(ctx.castParExpression()),
                        (Expression) this.visit(ctx.expression())
                ),
                ctx
        );
        */
        CastExpression cast = new CastExpression(visitCastParExpression(ctx.castParExpression()), (Expression) visit(ctx.expression()));
        Expression name = configureAST(new ConstantExpression(null), ctx.castParExpression().type().primitiveType() != null
            ? ctx.castParExpression().type().primitiveType() : ctx.castParExpression().type().classOrInterfaceType());
        cast.setNameStart(name.getStart()); cast.setNameEnd(name.getEnd());
        return configureAST(cast, ctx);
        // GRECLIPSE end
    }

    @Override
    public BinaryExpression visitPowerExprAlt(PowerExprAltContext ctx) {
        return this.createBinaryExpression(ctx.left, ctx.op, ctx.right, ctx);
    }

    @Override
    public Expression visitUnaryAddExprAlt(UnaryAddExprAltContext ctx) {
        ExpressionContext expressionCtx = ctx.expression();
        Expression expression = (Expression) this.visit(expressionCtx);

        Boolean insidePar = isInsideParentheses(expression);

        switch (ctx.op.getType()) {
            case ADD: {
                if (expression instanceof ConstantExpression && !insidePar) {
                    return configureAST(expression, ctx);
                }

                return configureAST(new UnaryPlusExpression(expression), ctx);
            }
            case SUB: {
                if (expression instanceof ConstantExpression && !insidePar) {
                    ConstantExpression constantExpression = (ConstantExpression) expression;

                    try {
                        String integerLiteralText = constantExpression.getNodeMetaData(INTEGER_LITERAL_TEXT);
                        if (null != integerLiteralText) {

                            ConstantExpression result = new ConstantExpression(Numbers.parseInteger(null, SUB_STR + integerLiteralText));

                            this.numberFormatError = null; // reset the numberFormatError

                            return configureAST(result, ctx);
                        }

                        String floatingPointLiteralText = constantExpression.getNodeMetaData(FLOATING_POINT_LITERAL_TEXT);
                        if (null != floatingPointLiteralText) {
                            ConstantExpression result = new ConstantExpression(Numbers.parseDecimal(SUB_STR + floatingPointLiteralText));

                            this.numberFormatError = null; // reset the numberFormatError

                            return configureAST(result, ctx);
                        }
                    } catch (Exception e) {
                        throw createParsingFailedException(e.getMessage(), ctx);
                    }

                    throw new GroovyBugError("Failed to find the original number literal text: " + constantExpression.getText());
                }

                return configureAST(new UnaryMinusExpression(expression), ctx);
            }

            case INC:
            case DEC:
                return configureAST(new PrefixExpression(this.createGroovyToken(ctx.op), expression), ctx);

            default:
                throw createParsingFailedException("Unsupported unary operation: " + ctx.getText(), ctx);
        }
    }

    @Override
    public BinaryExpression visitMultiplicativeExprAlt(MultiplicativeExprAltContext ctx) {
        return this.createBinaryExpression(ctx.left, ctx.op, ctx.right, ctx);
    }

    @Override
    public BinaryExpression visitAdditiveExprAlt(AdditiveExprAltContext ctx) {
        return this.createBinaryExpression(ctx.left, ctx.op, ctx.right, ctx);
    }

    @Override
    public Expression visitShiftExprAlt(ShiftExprAltContext ctx) {
        Expression left = (Expression) this.visit(ctx.left);
        Expression right = (Expression) this.visit(ctx.right);

        if (asBoolean(ctx.rangeOp)) {
            return configureAST(new RangeExpression(left, right, !ctx.rangeOp.getText().endsWith("<")), ctx);
        }

        org.codehaus.groovy.syntax.Token op;
        Token antlrToken;

        if (asBoolean(ctx.dlOp)) {
            op = this.createGroovyToken(ctx.dlOp, 2);
            antlrToken = ctx.dlOp;
        } else if (asBoolean(ctx.dgOp)) {
            op = this.createGroovyToken(ctx.dgOp, 2);
            antlrToken = ctx.dgOp;
        } else if (asBoolean(ctx.tgOp)) {
            op = this.createGroovyToken(ctx.tgOp, 3);
            antlrToken = ctx.tgOp;
        } else {
            throw createParsingFailedException("Unsupported shift expression: " + ctx.getText(), ctx);
        }

        BinaryExpression binaryExpression = new BinaryExpression(left, op, right);
        if (isTrue(ctx, IS_INSIDE_CONDITIONAL_EXPRESSION)) {
            return configureAST(binaryExpression, antlrToken);
        }

        return configureAST(binaryExpression, ctx);
    }

    @Override
    public Expression visitRelationalExprAlt(RelationalExprAltContext ctx) {
        switch (ctx.op.getType()) {
            case AS:
                /* GRECLIPSE edit
                return configureAST(
                        CastExpression.asExpression(this.visitType(ctx.type()), (Expression) this.visit(ctx.left)),
                        ctx);
                */
                CastExpression cast = CastExpression.asExpression(visitType(ctx.type()), (Expression) visit(ctx.left));
                Expression name = configureAST(new ConstantExpression(null), ctx.type().primitiveType() != null
                    ? ctx.type().primitiveType() : ctx.type().classOrInterfaceType());
                cast.setNameStart(name.getStart()); cast.setNameEnd(name.getEnd());
                return configureAST(cast, ctx);
                // GRECLIPSE end

            case INSTANCEOF:
            case NOT_INSTANCEOF:
                ctx.type().putNodeMetaData(IS_INSIDE_INSTANCEOF_EXPR, true);
                return configureAST(
                        new BinaryExpression((Expression) this.visit(ctx.left),
                                this.createGroovyToken(ctx.op),
                                configureAST(new ClassExpression(this.visitType(ctx.type())), ctx.type())),
                        ctx);

            case LE:
            case GE:
            case GT:
            case LT:
            case IN:
            case NOT_IN: {
                if (ctx.op.getType() == IN || ctx.op.getType() == NOT_IN ) {
                    return this.createBinaryExpression(ctx.left, ctx.op, ctx.right, ctx);
                }

                return configureAST(
                        this.createBinaryExpression(ctx.left, ctx.op, ctx.right),
                        ctx);
            }

            default:
                throw createParsingFailedException("Unsupported relational expression: " + ctx.getText(), ctx);
        }
    }

    @Override
    public BinaryExpression visitEqualityExprAlt(EqualityExprAltContext ctx) {
        return configureAST(
                this.createBinaryExpression(ctx.left, ctx.op, ctx.right),
                ctx);
    }

    @Override
    public BinaryExpression visitRegexExprAlt(RegexExprAltContext ctx) {
        return configureAST(
                this.createBinaryExpression(ctx.left, ctx.op, ctx.right),
                ctx);
    }

    @Override
    public BinaryExpression visitAndExprAlt(AndExprAltContext ctx) {
        return this.createBinaryExpression(ctx.left, ctx.op, ctx.right, ctx);
    }

    @Override
    public BinaryExpression visitExclusiveOrExprAlt(ExclusiveOrExprAltContext ctx) {
        return this.createBinaryExpression(ctx.left, ctx.op, ctx.right, ctx);
    }

    @Override
    public BinaryExpression visitInclusiveOrExprAlt(InclusiveOrExprAltContext ctx) {
        return this.createBinaryExpression(ctx.left, ctx.op, ctx.right, ctx);
    }

    @Override
    public BinaryExpression visitLogicalAndExprAlt(LogicalAndExprAltContext ctx) {
        return configureAST(
                this.createBinaryExpression(ctx.left, ctx.op, ctx.right),
                ctx);
    }

    @Override
    public BinaryExpression visitLogicalOrExprAlt(LogicalOrExprAltContext ctx) {
        return configureAST(
                this.createBinaryExpression(ctx.left, ctx.op, ctx.right),
                ctx);
    }

    @Override
    public Expression visitConditionalExprAlt(ConditionalExprAltContext ctx) {
        ctx.fb.putNodeMetaData(IS_INSIDE_CONDITIONAL_EXPRESSION, true);

        if (asBoolean(ctx.ELVIS())) { // e.g. a == 6 ?: 0
            return configureAST(
                    new ElvisOperatorExpression((Expression) this.visit(ctx.con), (Expression) this.visit(ctx.fb)),
                    ctx);
        }

        ctx.tb.putNodeMetaData(IS_INSIDE_CONDITIONAL_EXPRESSION, true);

        return configureAST(
                new TernaryExpression(
                        configureAST(new BooleanExpression((Expression) this.visit(ctx.con)),
                                ctx.con),
                        (Expression) this.visit(ctx.tb),
                        (Expression) this.visit(ctx.fb)),
                ctx);
    }

    @Override
    public BinaryExpression visitMultipleAssignmentExprAlt(MultipleAssignmentExprAltContext ctx) {
        return configureAST(
                new BinaryExpression(
                        this.visitVariableNames(ctx.left),
                        this.createGroovyToken(ctx.op),
                        ((ExpressionStatement) this.visit(ctx.right)).getExpression()),
                ctx);
    }

    @Override
    public BinaryExpression visitAssignmentExprAlt(AssignmentExprAltContext ctx) {
        Expression leftExpr = (Expression) this.visit(ctx.left);

        if (leftExpr instanceof VariableExpression
                && isInsideParentheses(leftExpr)) { // it is a special multiple assignment whose variable count is only one, e.g. (a) = [1]

            if ((Integer) leftExpr.getNodeMetaData(INSIDE_PARENTHESES_LEVEL) > 1) {
                throw createParsingFailedException("Nested parenthesis is not allowed in multiple assignment, e.g. ((a)) = b", ctx);
            }

            return configureAST(
                    new BinaryExpression(
                            configureAST(new TupleExpression(leftExpr), ctx.left),
                            this.createGroovyToken(ctx.op),
                            this.visitEnhancedStatementExpression(ctx.enhancedStatementExpression())),
                    ctx);
        }

        // the LHS expression should be a variable which is not inside any parentheses
        if (
                !(
                        (leftExpr instanceof VariableExpression
//                                && !(THIS_STR.equals(leftExpr.getText()) || SUPER_STR.equals(leftExpr.getText()))     // commented, e.g. this = value // this will be transformed to $this
                                && !isInsideParentheses(leftExpr)) // e.g. p = 123

                                || leftExpr instanceof PropertyExpression // e.g. obj.p = 123

                                || (leftExpr instanceof BinaryExpression
//                                && !(((BinaryExpression) leftExpr).getRightExpression() instanceof ListExpression)    // commented, e.g. list[1, 2] = [11, 12]
                                && Types.LEFT_SQUARE_BRACKET == ((BinaryExpression) leftExpr).getOperation().getType()) // e.g. map[a] = 123 OR map['a'] = 123 OR map["$a"] = 123
                )

                ) {

            throw createParsingFailedException("The LHS of an assignment should be a variable or a field accessing expression", ctx);
        }

        return configureAST(
                new BinaryExpression(
                        leftExpr,
                        this.createGroovyToken(ctx.op),
                        this.visitEnhancedStatementExpression(ctx.enhancedStatementExpression())),
                ctx);
    }

// } expression    --------------------------------------------------------------------


    // primary {       --------------------------------------------------------------------
    @Override
    public Expression visitIdentifierPrmrAlt(IdentifierPrmrAltContext ctx) {
        if (asBoolean(ctx.typeArguments())) {
            ClassNode classNode = ClassHelper.make(ctx.identifier().getText());

            classNode.setGenericsTypes(
                    this.visitTypeArguments(ctx.typeArguments()));

            return configureAST(new ClassExpression(classNode), ctx);
        }

        return configureAST(new VariableExpression(this.visitIdentifier(ctx.identifier())), ctx);
    }

    @Override
    public ConstantExpression visitLiteralPrmrAlt(LiteralPrmrAltContext ctx) {
        return configureAST((ConstantExpression) this.visit(ctx.literal()), ctx);
    }

    @Override
    public GStringExpression visitGstringPrmrAlt(GstringPrmrAltContext ctx) {
        return configureAST((GStringExpression) this.visit(ctx.gstring()), ctx);
    }

    @Override
    public Expression visitNewPrmrAlt(NewPrmrAltContext ctx) {
        return configureAST(this.visitCreator(ctx.creator()), ctx);
    }

    @Override
    public VariableExpression visitThisPrmrAlt(ThisPrmrAltContext ctx) {
        return configureAST(new VariableExpression(ctx.THIS().getText()), ctx);
    }

    @Override
    public VariableExpression visitSuperPrmrAlt(SuperPrmrAltContext ctx) {
        return configureAST(new VariableExpression(ctx.SUPER().getText()), ctx);
    }


    @Override
    public Expression visitParenPrmrAlt(ParenPrmrAltContext ctx) {
        // GRECLIPSE edit
        //return configureAST(this.visitParExpression(ctx.parExpression()), ctx);
        return visitParExpression(ctx.parExpression());
        // GRECLIPSE end
    }

    @Override
    public ClosureExpression visitClosurePrmrAlt(ClosurePrmrAltContext ctx) {
        return configureAST(this.visitClosure(ctx.closure()), ctx);
    }

    @Override
    public ClosureExpression visitLambdaPrmrAlt(LambdaPrmrAltContext ctx) {
        return configureAST(this.visitStandardLambdaExpression(ctx.standardLambdaExpression()), ctx);
    }

    @Override
    public ListExpression visitListPrmrAlt(ListPrmrAltContext ctx) {
        return configureAST(this.visitList(ctx.list()), ctx);
    }

    @Override
    public MapExpression visitMapPrmrAlt(MapPrmrAltContext ctx) {
        return configureAST(this.visitMap(ctx.map()), ctx);
    }

    @Override
    public VariableExpression visitBuiltInTypePrmrAlt(BuiltInTypePrmrAltContext ctx) {
        return configureAST(this.visitBuiltInType(ctx.builtInType()), ctx);
    }


// } primary       --------------------------------------------------------------------

    @Override
    public Expression visitCreator(CreatorContext ctx) {
        ClassNode classNode = this.visitCreatedName(ctx.createdName());
        Expression arguments = this.visitArguments(ctx.arguments());

        if (asBoolean(ctx.arguments())) { // create instance of class
            if (asBoolean(ctx.anonymousInnerClassDeclaration())) {
                ctx.anonymousInnerClassDeclaration().putNodeMetaData(ANONYMOUS_INNER_CLASS_SUPER_CLASS, classNode);
                InnerClassNode anonymousInnerClassNode = this.visitAnonymousInnerClassDeclaration(ctx.anonymousInnerClassDeclaration());

                List<InnerClassNode> anonymousInnerClassList = anonymousInnerClassesDefinedInMethodStack.peek();
                if (null != anonymousInnerClassList) { // if the anonymous class is created in a script, no anonymousInnerClassList is available.
                    anonymousInnerClassList.add(anonymousInnerClassNode);
                }

                ConstructorCallExpression constructorCallExpression = new ConstructorCallExpression(anonymousInnerClassNode, arguments);
                // GRECLIPSE add
                ASTNode nameNode = configureAST(new ConstantExpression(classNode.getName()), ctx.createdName().qualifiedClassName());
                anonymousInnerClassNode.setNameStart(nameNode.getStart());
                anonymousInnerClassNode.setNameEnd(nameNode.getEnd() - 1);
                constructorCallExpression.setNameStart(nameNode.getStart());
                constructorCallExpression.setNameEnd(nameNode.getEnd() - 1);
                // GRECLIPSE end
                constructorCallExpression.setUsingAnonymousInnerClass(true);

                return configureAST(constructorCallExpression, ctx);
            }

            /* GRECLIPSE edit
            return configureAST(
                    new ConstructorCallExpression(classNode, arguments),
                    ctx);
            */
            ConstructorCallExpression constructorCallExpression = new ConstructorCallExpression(classNode, arguments);
            ASTNode nameNode = configureAST(new ConstantExpression(classNode.getName()), ctx.createdName().qualifiedClassName());
            constructorCallExpression.setNameStart(nameNode.getStart());
            constructorCallExpression.setNameEnd(nameNode.getEnd() - 1);
            return configureAST(constructorCallExpression, ctx);
            // GRECLIPSE end
        }

        if (asBoolean(ctx.LBRACK()) || asBoolean(ctx.dims())) { // create array
            ArrayExpression arrayExpression;
            List<List<AnnotationNode>> allDimList;
            // GRECLIPSE add
            List<AnnotationsOptContext> annOptCtxt = new ArrayList<>();
            // GRECLIPSE end

            if (asBoolean(ctx.arrayInitializer())) {
                ClassNode elementType = classNode;
                allDimList = this.visitDims(ctx.dims());
                // GRECLIPSE add
                annOptCtxt.addAll(ctx.dims().annotationsOpt());
                // GRECLIPSE end

                for (int i = 0, n = allDimList.size() - 1; i < n; i++) {
                    elementType = elementType.makeArray();
                }

                arrayExpression =
                        new ArrayExpression(
                            elementType,
                            this.visitArrayInitializer(ctx.arrayInitializer()));

            } else {
                Expression[] empties;
                List<List<AnnotationNode>> emptyDimList = this.visitDimsOpt(ctx.dimsOpt());

                if (asBoolean(emptyDimList)) {
                    empties = new Expression[emptyDimList.size()];
                    for (int i = 0; i < empties.length; ++i) {
                        empties[i] = ConstantExpression.EMPTY_EXPRESSION;
                    }
                } else {
                    empties = Expression.EMPTY_ARRAY;
                }

                List<Expression> sizes = new LinkedList<>();
                for (ExpressionContext e : ctx.expression()) {
                    sizes.add((Expression)this.visit(e));
                }
                for (Expression e : empties) {
                    sizes.add(e);
                }

                arrayExpression =
                        new ArrayExpression(
                                classNode,
                                null,
                                sizes);

                List<List<AnnotationNode>> exprDimList = new ArrayList<List<AnnotationNode>>();
                for (AnnotationsOptContext annotationsOptContext : ctx.annotationsOpt()) {
                    exprDimList.add(this.visitAnnotationsOpt(annotationsOptContext));
                }
                allDimList = new ArrayList<>(exprDimList);
                Collections.reverse(emptyDimList);
                allDimList.addAll(emptyDimList);
                Collections.reverse(allDimList);
                // GRECLIPSE add
                annOptCtxt.addAll(ctx.annotationsOpt());
                if (asBoolean(ctx.dimsOpt().dims())) annOptCtxt.addAll(ctx.dimsOpt().dims().annotationsOpt());
                // GRECLIPSE end
            }

            // GRECLIPSE edit
            //arrayExpression.setType(createArrayType(classNode, allDimList));
            ClassNode componentType = arrayExpression.getType();
            if (!asBoolean(ctx.dims())) {
                configureAST(componentType, ctx);
            } else {
                configureAST(componentType, ctx, configureAST(new ConstantExpression(""), ctx.dims()));
            }
            for (int i = annOptCtxt.size() - 1; i > 0; i -= 1) {
                componentType = componentType.getComponentType();
                configureAST(componentType, ctx, configureAST(new ConstantExpression(""), annOptCtxt.get(i)));
            }

            ASTNode nameNode = configureAST(new ConstantExpression(classNode.getName()),ctx.createdName().primitiveType() != null
                ? ctx.createdName().primitiveType() : ctx.createdName().qualifiedClassName());
            arrayExpression.setNameStart(nameNode.getStart());
            arrayExpression.setNameEnd(nameNode.getEnd() - 1);
            // GRECLIPSE end
            return configureAST(arrayExpression, ctx);
        }

        throw createParsingFailedException("Unsupported creator: " + ctx.getText(), ctx);
    }

    private ClassNode createArrayType(ClassNode classNode, List<List<AnnotationNode>> dimList) {
        ClassNode arrayType = classNode;
        for (int i = 0, n = dimList.size(); i < n; i++) {
            arrayType = arrayType.makeArray();
            arrayType.addAnnotations(dimList.get(i));
        }
        return arrayType;
    }


    private String genAnonymousClassName(String outerClassName) {
        return outerClassName + "$" + this.anonymousInnerClassCounter++;
    }

    @Override
    public InnerClassNode visitAnonymousInnerClassDeclaration(AnonymousInnerClassDeclarationContext ctx) {
        ClassNode superClass = ctx.getNodeMetaData(ANONYMOUS_INNER_CLASS_SUPER_CLASS);
        Objects.requireNonNull(superClass, "superClass should not be null");

        InnerClassNode anonymousInnerClass;

        ClassNode outerClass = this.classNodeStack.peek();
        outerClass = asBoolean(outerClass) ? outerClass : moduleNode.getScriptClassDummy();

        String fullName = this.genAnonymousClassName(outerClass.getName());
        if (1 == ctx.t) { // anonymous enum
            anonymousInnerClass = new EnumConstantClassNode(outerClass, fullName, superClass.getModifiers() | Opcodes.ACC_FINAL, superClass.getPlainNodeReference());

            // and remove the final modifier from classNode to allow the sub class
            superClass.setModifiers(superClass.getModifiers() & ~Opcodes.ACC_FINAL);
        } else { // anonymous inner class
            anonymousInnerClass = new InnerClassNode(outerClass, fullName, Opcodes.ACC_PUBLIC, superClass);
        }

        anonymousInnerClass.setUsingGenerics(false);
        anonymousInnerClass.setAnonymous(true);
        anonymousInnerClass.putNodeMetaData(CLASS_NAME, fullName);
        configureAST(anonymousInnerClass, ctx);

        classNodeStack.push(anonymousInnerClass);
        ctx.classBody().putNodeMetaData(CLASS_DECLARATION_CLASS_NODE, anonymousInnerClass);
        this.visitClassBody(ctx.classBody());
        classNodeStack.pop();

        classNodeList.add(anonymousInnerClass);

        return anonymousInnerClass;
    }


    @Override
    public ClassNode visitCreatedName(CreatedNameContext ctx) {
        ClassNode classNode = null;

        if (asBoolean(ctx.qualifiedClassName())) {
            classNode = this.visitQualifiedClassName(ctx.qualifiedClassName());

            if (asBoolean(ctx.typeArgumentsOrDiamond())) {
                classNode.setGenericsTypes(
                        this.visitTypeArgumentsOrDiamond(ctx.typeArgumentsOrDiamond()));
            }
            // GRECLIPSE edit
            //classNode = configureAST(classNode, ctx);
            // GRECLIPSE end
        } else if (asBoolean(ctx.primitiveType())) {
            classNode = configureAST(
                    this.visitPrimitiveType(ctx.primitiveType()),
                    ctx);
        }

        if (!asBoolean(classNode)) {
            throw createParsingFailedException("Unsupported created name: " + ctx.getText(), ctx);
        }

        classNode.addAnnotations(this.visitAnnotationsOpt(ctx.annotationsOpt()));

        return classNode;
    }


    @Override
    public MapExpression visitMap(MapContext ctx) {
        return configureAST(
                new MapExpression(this.visitMapEntryList(ctx.mapEntryList())),
                ctx);
    }

    @Override
    public List<MapEntryExpression> visitMapEntryList(MapEntryListContext ctx) {
        if (!asBoolean(ctx)) {
            return Collections.emptyList();
        }

        return this.createMapEntryList(ctx.mapEntry());
    }

    private List<MapEntryExpression> createMapEntryList(List<? extends MapEntryContext> mapEntryContextList) {
        if (!asBoolean(mapEntryContextList)) {
            return Collections.emptyList();
        }

        List<MapEntryExpression> list = new ArrayList<>();
        for (MapEntryContext mapEntryContext : mapEntryContextList) {
            MapEntryExpression mapEntryExpression = visitMapEntry(mapEntryContext);
            list.add(mapEntryExpression);
        }
        return list;
    }

    @Override
    public MapEntryExpression visitMapEntry(MapEntryContext ctx) {
        Expression keyExpr;
        Expression valueExpr = (Expression) this.visit(ctx.expression());

        if (asBoolean(ctx.MUL())) {
            keyExpr = configureAST(new SpreadMapExpression(valueExpr), ctx);
        } else if (asBoolean(ctx.mapEntryLabel())) {
            keyExpr = this.visitMapEntryLabel(ctx.mapEntryLabel());
        } else {
            throw createParsingFailedException("Unsupported map entry: " + ctx.getText(), ctx);
        }

        return configureAST(
                new MapEntryExpression(keyExpr, valueExpr),
                ctx);
    }

    @Override
    public Expression visitMapEntryLabel(MapEntryLabelContext ctx) {
        if (asBoolean(ctx.keywords())) {
            return configureAST(this.visitKeywords(ctx.keywords()), ctx);
        } else if (asBoolean(ctx.primary())) {
            Expression expression = (Expression) this.visit(ctx.primary());

            // if the key is variable and not inside parentheses, convert it to a constant, e.g. [a:1, b:2]
            if (expression instanceof VariableExpression && !isInsideParentheses(expression)) {
                expression =
                        configureAST(
                                new ConstantExpression(((VariableExpression) expression).getName()),
                                expression);
            }

            // GRECLIPSE edit
            //return configureAST(expression, ctx);
            return expression;
            // GRECLIPSE end
        }

        throw createParsingFailedException("Unsupported map entry label: " + ctx.getText(), ctx);
    }

    @Override
    public ConstantExpression visitKeywords(KeywordsContext ctx) {
        return configureAST(new ConstantExpression(ctx.getText()), ctx);
    }

    @Override
    public VariableExpression visitBuiltInType(BuiltInTypeContext ctx) {
        String text;
        if (asBoolean(ctx.VOID())) {
            text = ctx.VOID().getText();
        } else if (asBoolean(ctx.BuiltInPrimitiveType())) {
            text = ctx.BuiltInPrimitiveType().getText();
        } else {
            throw createParsingFailedException("Unsupported built-in type: " + ctx, ctx);
        }

        return configureAST(new VariableExpression(text), ctx);
    }

    @Override
    public ListExpression visitList(ListContext ctx) {
        if (asBoolean(ctx.COMMA()) && !asBoolean(ctx.expressionList())) {
            throw createParsingFailedException("Empty list constructor should not contain any comma(,)", ctx.COMMA());
        }

        return configureAST(
                new ListExpression(
                        this.visitExpressionList(ctx.expressionList())),
                ctx);
    }

    @Override
    public List<Expression> visitExpressionList(ExpressionListContext ctx) {
        if (!asBoolean(ctx)) {
            return Collections.emptyList();
        }

        return this.createExpressionList(ctx.expressionListElement());
    }

    private List<Expression> createExpressionList(List<? extends ExpressionListElementContext> expressionListElementContextList) {
        if (!asBoolean(expressionListElementContextList)) {
            return Collections.emptyList();
        }

        List<Expression> list = new ArrayList<>();
        for (ExpressionListElementContext expressionListElementContext : expressionListElementContextList) {
            Expression expression = visitExpressionListElement(expressionListElementContext);
            list.add(expression);
        }
        return list;
    }

    @Override
    public Expression visitExpressionListElement(ExpressionListElementContext ctx) {
        Expression expression = (Expression) this.visit(ctx.expression());

        validateExpressionListElement(ctx, expression);

        if (asBoolean(ctx.MUL())) {
            return configureAST(new SpreadExpression(expression), ctx);
        }

        return configureAST(expression, ctx);
    }

    private void validateExpressionListElement(ExpressionListElementContext ctx, Expression expression) {
        if (!(expression instanceof MethodCallExpression && isTrue(expression, IS_COMMAND_EXPRESSION))) {
            return;
        }

        // statements like `foo(String a)` is invalid
        MethodCallExpression methodCallExpression = (MethodCallExpression) expression;
        String methodName = methodCallExpression.getMethodAsString();
        if (Character.isUpperCase(methodName.codePointAt(0)) || PRIMITIVE_TYPE_SET.contains(methodName)) {
            throw createParsingFailedException("Invalid method declaration", ctx);
        }
    }


    // literal {       --------------------------------------------------------------------
    @Override
    public ConstantExpression visitIntegerLiteralAlt(IntegerLiteralAltContext ctx) {
        String text = ctx.IntegerLiteral().getText();

        Number num = null;
        try {
            num = Numbers.parseInteger(null, text);
        } catch (Exception e) {
            this.numberFormatError = new Tuple2<GroovyParserRuleContext, Exception>(ctx, e);
        }

        ConstantExpression constantExpression = new ConstantExpression(num, !text.startsWith(SUB_STR));
        constantExpression.putNodeMetaData(IS_NUMERIC, true);
        constantExpression.putNodeMetaData(INTEGER_LITERAL_TEXT, text);

        return configureAST(constantExpression, ctx);
    }

    @Override
    public ConstantExpression visitFloatingPointLiteralAlt(FloatingPointLiteralAltContext ctx) {
        String text = ctx.FloatingPointLiteral().getText();

        Number num = null;
        try {
            num = Numbers.parseDecimal(text);
        } catch (Exception e) {
            this.numberFormatError = new Tuple2<GroovyParserRuleContext, Exception>(ctx, e);
        }

        ConstantExpression constantExpression = new ConstantExpression(num, !text.startsWith(SUB_STR));
        constantExpression.putNodeMetaData(IS_NUMERIC, true);
        constantExpression.putNodeMetaData(FLOATING_POINT_LITERAL_TEXT, text);

        return configureAST(constantExpression, ctx);
    }

    @Override
    public ConstantExpression visitStringLiteralAlt(StringLiteralAltContext ctx) {
        return configureAST(
                this.visitStringLiteral(ctx.stringLiteral()),
                ctx);
    }

    @Override
    public ConstantExpression visitBooleanLiteralAlt(BooleanLiteralAltContext ctx) {
        return configureAST(new ConstantExpression("true".equals(ctx.BooleanLiteral().getText()), true), ctx);
    }

    @Override
    public ConstantExpression visitNullLiteralAlt(NullLiteralAltContext ctx) {
        return configureAST(new ConstantExpression(null), ctx);
    }


// } literal       --------------------------------------------------------------------


    // gstring {       --------------------------------------------------------------------
    @Override
    public GStringExpression visitGstring(GstringContext ctx) {
        final List<ConstantExpression> stringLiteralList = new LinkedList<>();
        final String begin = ctx.GStringBegin().getText();
        final String beginQuotation = beginQuotation(begin);
        stringLiteralList.add(configureAST(new ConstantExpression(parseGStringBegin(ctx, beginQuotation)), ctx.GStringBegin()));

        List<ConstantExpression> partStrings = new LinkedList<>();
        for (TerminalNode e : ctx.GStringPart()) {
            partStrings.add(configureAST(new ConstantExpression(parseGStringPart(e, beginQuotation)), e));
        }
        stringLiteralList.addAll(partStrings);

        stringLiteralList.add(configureAST(new ConstantExpression(parseGStringEnd(ctx, beginQuotation)), ctx.GStringEnd()));

        List<Expression> values = new LinkedList<>();
        for (GstringValueContext e : ctx.gstringValue()) {
            Expression expression = this.visitGstringValue(e);

            if (expression instanceof ClosureExpression && !asBoolean(e.closure().ARROW())) {
                List<Statement> statementList = ((BlockStatement) ((ClosureExpression) expression).getCode()).getStatements();

                // Java 8: if (statementList.stream().allMatch(x -> !asBoolean(x))) {
                boolean allFalse = true;
                for (Statement x : statementList) {
                    if (asBoolean(x)) {
                        allFalse = false;
                        break;
                    }
                }
                if (allFalse) {
                    values.add(configureAST(new ConstantExpression(null), e));
                } else {
                    values.add(configureAST(this.createCallMethodCallExpression(expression, new ArgumentListExpression(), true), e));
                }
            } else {
                values.add(expression);
            }
        }

        StringBuilder verbatimText = new StringBuilder(ctx.getText().length());
        for (int i = 0, n = stringLiteralList.size(), s = values.size(); i < n; i++) {
            verbatimText.append(stringLiteralList.get(i).getValue());

            if (i == s) {
                continue;
            }

            Expression value = values.get(i);
            if (!asBoolean(value)) {
                continue;
            }

            verbatimText.append(DOLLAR_STR);
            verbatimText.append(value.getText());
        }

        return configureAST(new GStringExpression(verbatimText.toString(), stringLiteralList, values), ctx);
    }

    private String parseGStringEnd(GstringContext ctx, String beginQuotation) {
        StringBuilder text = new StringBuilder(ctx.GStringEnd().getText());
        text.insert(0, beginQuotation);

        return this.parseStringLiteral(text.toString());
    }

    private String parseGStringPart(TerminalNode e, String beginQuotation) {
        StringBuilder text = new StringBuilder(e.getText());
        text.deleteCharAt(text.length() - 1);  // remove the tailing $
        text.insert(0, beginQuotation).append(QUOTATION_MAP.get(beginQuotation));

        return this.parseStringLiteral(text.toString());
    }

    private String parseGStringBegin(GstringContext ctx, String beginQuotation) {
        StringBuilder text = new StringBuilder(ctx.GStringBegin().getText());
        text.deleteCharAt(text.length() - 1);  // remove the tailing $
        text.append(QUOTATION_MAP.get(beginQuotation));

        return this.parseStringLiteral(text.toString());
    }

    private String beginQuotation(String text) {
        if (text.startsWith(TDQ_STR)) {
            return TDQ_STR;
        } else if (text.startsWith(DQ_STR)) {
            return DQ_STR;
        } else if (text.startsWith(SLASH_STR)) {
            return SLASH_STR;
        } else if (text.startsWith(DOLLAR_SLASH_STR)) {
            return DOLLAR_SLASH_STR;
        } else {
            return String.valueOf(text.charAt(0));
        }
    }


    @Override
    public Expression visitGstringValue(GstringValueContext ctx) {
        if (asBoolean(ctx.gstringPath())) {
            return configureAST(this.visitGstringPath(ctx.gstringPath()), ctx);
        }

        if (asBoolean(ctx.LBRACE())) {
            if (asBoolean(ctx.statementExpression())) {
                return configureAST(((ExpressionStatement) this.visit(ctx.statementExpression())).getExpression(), ctx.statementExpression());
            } else { // e.g. "${}"
                return configureAST(new ConstantExpression(null), ctx);
            }
        }

        if (asBoolean(ctx.closure())) {
            return configureAST(this.visitClosure(ctx.closure()), ctx);
        }

        throw createParsingFailedException("Unsupported gstring value: " + ctx.getText(), ctx);
    }

    @Override
    public Expression visitGstringPath(GstringPathContext ctx) {
        VariableExpression variableExpression = new VariableExpression(this.visitIdentifier(ctx.identifier()));

        if (asBoolean(ctx.GStringPathPart())) {
            Expression propertyExpression = configureAST(variableExpression, ctx.identifier());
            for (TerminalNode e : ctx.GStringPathPart()) {
                Expression expression = configureAST((Expression) new ConstantExpression(e.getText().substring(1)), e);
                // GRECLIPSE add
                expression.setStart(expression.getStart() + 1);
                int[] row_col = locationSupport.getRowCol(expression.getStart());
                expression.setLineNumber(row_col[0]); expression.setColumnNumber(row_col[1]);
                // GRECLIPSE end
                propertyExpression = configureAST(new PropertyExpression(propertyExpression, expression), expression);
                // GRECLIPSE add
                propertyExpression.setStart(((PropertyExpression) propertyExpression).getObjectExpression().getStart());
                propertyExpression.setLineNumber(((PropertyExpression) propertyExpression).getObjectExpression().getLineNumber());
                propertyExpression.setColumnNumber(((PropertyExpression) propertyExpression).getObjectExpression().getColumnNumber());
                // GRECLIPSE end
            }

            // GRECLIPSE edit
            //return configureAST(propertyExpression, ctx);
            return propertyExpression;
            // GRECLIPSE end
        }

        // GRECLIPSE edit
        //return configureAST(variableExpression, ctx);
        return variableExpression;
        // GRECLIPSE end
    }
// } gstring       --------------------------------------------------------------------

    @Override
    public LambdaExpression visitStandardLambdaExpression(StandardLambdaExpressionContext ctx) {
        return configureAST(this.createLambda(ctx.standardLambdaParameters(), ctx.lambdaBody()), ctx);
    }

    private LambdaExpression createLambda(StandardLambdaParametersContext standardLambdaParametersContext, LambdaBodyContext lambdaBodyContext) {
        return new LambdaExpression(
                this.visitStandardLambdaParameters(standardLambdaParametersContext),
                this.visitLambdaBody(lambdaBodyContext));
    }

    @Override
    public Parameter[] visitStandardLambdaParameters(StandardLambdaParametersContext ctx) {
        if (asBoolean(ctx.variableDeclaratorId())) {
            return new Parameter[]{
                    configureAST(
                            new Parameter(
                                    ClassHelper.OBJECT_TYPE,
                                    this.visitVariableDeclaratorId(ctx.variableDeclaratorId()).getName()
                            ),
                            ctx.variableDeclaratorId()
                    )
            };
        }

        Parameter[] parameters = this.visitFormalParameters(ctx.formalParameters());

        if (0 == parameters.length) {
            return null;
        }

        return parameters;
    }

    @Override
    public Statement visitLambdaBody(LambdaBodyContext ctx) {
        if (asBoolean(ctx.statementExpression())) {
            return configureAST((ExpressionStatement) this.visit(ctx.statementExpression()), ctx);
        }

        if (asBoolean(ctx.block())) {
            return configureAST(this.visitBlock(ctx.block()), ctx);
        }

        throw createParsingFailedException("Unsupported lambda body: " + ctx.getText(), ctx);
    }

    @Override
    public ClosureExpression visitClosure(ClosureContext ctx) {
        visitingClosureCnt++;

        Parameter[] parameters = asBoolean(ctx.formalParameterList())
                ? this.visitFormalParameterList(ctx.formalParameterList())
                : null;

        if (!asBoolean(ctx.ARROW())) {
            parameters = Parameter.EMPTY_ARRAY;
        }

        Statement code = this.visitBlockStatementsOpt(ctx.blockStatementsOpt());

        ClosureExpression result = configureAST(new ClosureExpression(parameters, code), ctx);

        visitingClosureCnt--;

        return result;
    }

    @Override
    public Parameter[] visitFormalParameters(FormalParametersContext ctx) {
        if (!asBoolean(ctx)) {
            return Parameter.EMPTY_ARRAY;
        }

        return this.visitFormalParameterList(ctx.formalParameterList());
    }

    @Override
    public Parameter[] visitFormalParameterList(FormalParameterListContext ctx) {
        if (!asBoolean(ctx)) {
            return Parameter.EMPTY_ARRAY;
        }

        List<Parameter> parameterList = new LinkedList<>();

        if (asBoolean(ctx.thisFormalParameter())) {
            parameterList.add(this.visitThisFormalParameter(ctx.thisFormalParameter()));
        }

        List<? extends FormalParameterContext> formalParameterList = ctx.formalParameter();
        if (asBoolean(formalParameterList)) {
            validateVarArgParameter(formalParameterList);

            List<Parameter> list = new ArrayList<>();
            for (FormalParameterContext formalParameterContext : formalParameterList) {
                Parameter parameter = visitFormalParameter(formalParameterContext);
                // GRECLIPSE add
                ASTNode nameNode = configureAST(new ConstantExpression(parameter.getName()), formalParameterContext.variableDeclaratorId());
                parameter.setNameStart(nameNode.getStart()); parameter.setNameEnd(nameNode.getEnd());
                // GRECLIPSE end
                list.add(parameter);
            }
            parameterList.addAll(list);
        }

        validateParameterList(parameterList);

        return parameterList.toArray(Parameter.EMPTY_ARRAY);
    }

    private void validateVarArgParameter(List<? extends FormalParameterContext> formalParameterList) {
        for (int i = 0, n = formalParameterList.size(); i < n - 1; i++) {
            FormalParameterContext formalParameterContext = formalParameterList.get(i);
            if (asBoolean(formalParameterContext.ELLIPSIS())) {
                throw createParsingFailedException("The var-arg parameter strs must be the last parameter", formalParameterContext);
            }
        }
    }

    private void validateParameterList(List<Parameter> parameterList) {
        for (int n = parameterList.size(), i = n - 1; i >= 0; i--) {
            Parameter parameter = parameterList.get(i);

            for (Parameter otherParameter : parameterList) {
                if (otherParameter == parameter) {
                    continue;
                }

                if (otherParameter.getName().equals(parameter.getName())) {
                    throw createParsingFailedException("Duplicated parameter '" + parameter.getName() + "' found.", parameter);
                }
            }
        }
    }

    @Override
    public Parameter visitFormalParameter(FormalParameterContext ctx) {
        return this.processFormalParameter(ctx, ctx.variableModifiersOpt(), ctx.type(), ctx.ELLIPSIS(), ctx.variableDeclaratorId(), ctx.expression());
    }

    @Override
    public Parameter visitThisFormalParameter(ThisFormalParameterContext ctx) {
        return configureAST(new Parameter(this.visitType(ctx.type()), THIS_STR), ctx);
    }

    @Override
    public List<ModifierNode> visitClassOrInterfaceModifiersOpt(ClassOrInterfaceModifiersOptContext ctx) {
        if (asBoolean(ctx.classOrInterfaceModifiers())) {
            return this.visitClassOrInterfaceModifiers(ctx.classOrInterfaceModifiers());
        }

        return Collections.emptyList();
    }

    @Override
    public List<ModifierNode> visitClassOrInterfaceModifiers(ClassOrInterfaceModifiersContext ctx) {
        List<ModifierNode> list = new ArrayList<>();
        for (ClassOrInterfaceModifierContext classOrInterfaceModifierContext : ctx.classOrInterfaceModifier()) {
            ModifierNode modifierNode = visitClassOrInterfaceModifier(classOrInterfaceModifierContext);
            list.add(modifierNode);
        }
        return list;
    }


    @Override
    public ModifierNode visitClassOrInterfaceModifier(ClassOrInterfaceModifierContext ctx) {
        if (asBoolean(ctx.annotation())) {
            return configureAST(new ModifierNode(this.visitAnnotation(ctx.annotation()), ctx.getText()), ctx);
        }

        if (asBoolean(ctx.m)) {
            return configureAST(new ModifierNode(ctx.m.getType(), ctx.getText()), ctx);
        }

        throw createParsingFailedException("Unsupported class or interface modifier: " + ctx.getText(), ctx);
    }

    @Override
    public ModifierNode visitModifier(ModifierContext ctx) {
        if (asBoolean(ctx.classOrInterfaceModifier())) {
            return configureAST(this.visitClassOrInterfaceModifier(ctx.classOrInterfaceModifier()), ctx);
        }

        if (asBoolean(ctx.m)) {
            return configureAST(new ModifierNode(ctx.m.getType(), ctx.getText()), ctx);
        }

        throw createParsingFailedException("Unsupported modifier: " + ctx.getText(), ctx);
    }

    @Override
    public List<ModifierNode> visitModifiers(ModifiersContext ctx) {
        List<ModifierNode> list = new ArrayList<>();
        for (ModifierContext modifierContext : ctx.modifier()) {
            ModifierNode modifierNode = visitModifier(modifierContext);
            list.add(modifierNode);
        }
        return list;
    }

    @Override
    public List<ModifierNode> visitModifiersOpt(ModifiersOptContext ctx) {
        if (asBoolean(ctx.modifiers())) {
            return this.visitModifiers(ctx.modifiers());
        }

        return Collections.emptyList();
    }


    @Override
    public ModifierNode visitVariableModifier(VariableModifierContext ctx) {
        if (asBoolean(ctx.annotation())) {
            return configureAST(new ModifierNode(this.visitAnnotation(ctx.annotation()), ctx.getText()), ctx);
        }

        if (asBoolean(ctx.m)) {
            return configureAST(new ModifierNode(ctx.m.getType(), ctx.getText()), ctx);
        }

        throw createParsingFailedException("Unsupported variable modifier", ctx);
    }

    @Override
    public List<ModifierNode> visitVariableModifiersOpt(VariableModifiersOptContext ctx) {
        if (asBoolean(ctx.variableModifiers())) {
            return this.visitVariableModifiers(ctx.variableModifiers());
        }

        return Collections.emptyList();
    }

    @Override
    public List<ModifierNode> visitVariableModifiers(VariableModifiersContext ctx) {
        List<ModifierNode> list = new ArrayList<>();
        for (VariableModifierContext variableModifierContext : ctx.variableModifier()) {
            ModifierNode modifierNode = visitVariableModifier(variableModifierContext);
            list.add(modifierNode);
        }
        return list;
    }

    @Override
    public List<List<AnnotationNode>> visitDims(DimsContext ctx) {
        List<List<AnnotationNode>> dimList = new ArrayList<List<AnnotationNode>>();
        for (AnnotationsOptContext annotationsOptContext : ctx.annotationsOpt()) {
            dimList.add(this.visitAnnotationsOpt(annotationsOptContext));
        }

        Collections.reverse(dimList);

        return dimList;
    }

    @Override
    public List<List<AnnotationNode>> visitDimsOpt(DimsOptContext ctx) {
        if (!asBoolean(ctx.dims())) {
            return Collections.emptyList();
        }

        return this.visitDims(ctx.dims());
    }

    // type {       --------------------------------------------------------------------
    @Override
    public ClassNode visitType(TypeContext ctx) {
        if (!asBoolean(ctx)) {
            return ClassHelper.OBJECT_TYPE;
        }

        ClassNode classNode = null;

        if (asBoolean(ctx.classOrInterfaceType())) {
            ctx.classOrInterfaceType().putNodeMetaData(IS_INSIDE_INSTANCEOF_EXPR, ctx.getNodeMetaData(IS_INSIDE_INSTANCEOF_EXPR));
            classNode = this.visitClassOrInterfaceType(ctx.classOrInterfaceType());
        } else if (asBoolean(ctx.primitiveType())) {
            classNode = this.visitPrimitiveType(ctx.primitiveType());
        }

        if (!asBoolean(classNode)) {
            if (VOID_STR.equals(ctx.getText())) { // TODO refine error message for `void`
                throw createParsingFailedException("void is not allowed here", ctx);
            }

            throw createParsingFailedException("Unsupported type: " + ctx.getText(), ctx);
        }

        classNode.addAnnotations(this.visitAnnotationsOpt(ctx.annotationsOpt()));

        List<List<AnnotationNode>> dimList = this.visitDimsOpt(ctx.dimsOpt());
        if (asBoolean(dimList)) {
            // GRECLIPSE edit
            //classNode.setGenericsTypes(null);
            //classNode.setUsingGenerics(false);
            // GRECLIPSE end
            classNode = this.createArrayType(classNode, dimList);
            // GRECLIPSE add
            configureAST(classNode, ctx);
            ClassNode componentType = classNode;
            for (int i = dimList.size() - 1; i > 0; i -= 1) {
                componentType = componentType.getComponentType();
                AnnotationsOptContext aoc = ctx.dimsOpt().dims().annotationsOpt(i);
                configureAST(componentType, ctx, configureAST(new ConstantExpression(""), aoc));
            }
            // GRECLIPSE end
        }

        // GRECLIPSE edit
        //return configureAST(classNode, ctx);
        return classNode;
        // GRECLIPSE end
    }

    @Override
    public ClassNode visitClassOrInterfaceType(ClassOrInterfaceTypeContext ctx) {
        ClassNode classNode;
        if (asBoolean(ctx.qualifiedClassName())) {
            ctx.qualifiedClassName().putNodeMetaData(IS_INSIDE_INSTANCEOF_EXPR, ctx.getNodeMetaData(IS_INSIDE_INSTANCEOF_EXPR));
            classNode = this.visitQualifiedClassName(ctx.qualifiedClassName());
        } else {
            ctx.qualifiedStandardClassName().putNodeMetaData(IS_INSIDE_INSTANCEOF_EXPR, ctx.getNodeMetaData(IS_INSIDE_INSTANCEOF_EXPR));
            classNode = this.visitQualifiedStandardClassName(ctx.qualifiedStandardClassName());
        }

        if (asBoolean(ctx.typeArguments())) {
            classNode.setGenericsTypes(this.visitTypeArguments(ctx.typeArguments()));
        }

        // GRECLIPSE edit
        //return configureAST(classNode, ctx);
        return classNode;
        // GRECLIPSE end
    }

    @Override
    public GenericsType[] visitTypeArgumentsOrDiamond(TypeArgumentsOrDiamondContext ctx) {
        if (asBoolean(ctx.typeArguments())) {
            return this.visitTypeArguments(ctx.typeArguments());
        }

        if (asBoolean(ctx.LT())) { // e.g. <>
            return GenericsType.EMPTY_ARRAY;
        }

        throw createParsingFailedException("Unsupported type arguments or diamond: " + ctx.getText(), ctx);
    }


    @Override
    public GenericsType[] visitTypeArguments(TypeArgumentsContext ctx) {
        List<GenericsType> list = new ArrayList<>();
        for (TypeArgumentContext typeArgumentContext : ctx.typeArgument()) {
            GenericsType genericsType = visitTypeArgument(typeArgumentContext);
            list.add(genericsType);
        }
        return list.toArray(new GenericsType[0]);
    }

    @Override
    public GenericsType visitTypeArgument(TypeArgumentContext ctx) {
        if (asBoolean(ctx.QUESTION())) {
            ClassNode baseType = configureAST(ClassHelper.makeWithoutCaching(QUESTION_STR), ctx.QUESTION());

            baseType.addAnnotations(this.visitAnnotationsOpt(ctx.annotationsOpt()));

            if (!asBoolean(ctx.type())) {
                GenericsType genericsType = new GenericsType(baseType);
                genericsType.setWildcard(true);
                genericsType.setName(QUESTION_STR);

                return configureAST(genericsType, ctx);
            }

            ClassNode[] upperBounds = null;
            ClassNode lowerBound = null;

            ClassNode classNode = this.visitType(ctx.type());
            if (asBoolean(ctx.EXTENDS())) {
                upperBounds = new ClassNode[]{classNode};
            } else if (asBoolean(ctx.SUPER())) {
                lowerBound = classNode;
            }

            GenericsType genericsType = new GenericsType(baseType, upperBounds, lowerBound);
            genericsType.setWildcard(true);
            genericsType.setName(QUESTION_STR);

            return configureAST(genericsType, ctx);
        } else if (asBoolean(ctx.type())) {
            return configureAST(
                    this.createGenericsType(
                            this.visitType(ctx.type())),
                    ctx);
        }

        throw createParsingFailedException("Unsupported type argument: " + ctx.getText(), ctx);
    }

    @Override
    public ClassNode visitPrimitiveType(PrimitiveTypeContext ctx) {
        return configureAST(ClassHelper.make(ctx.getText()), ctx);
    }
// } type       --------------------------------------------------------------------

    @Override
    public VariableExpression visitVariableDeclaratorId(VariableDeclaratorIdContext ctx) {
        return configureAST(new VariableExpression(this.visitIdentifier(ctx.identifier())), ctx);
    }

    @Override
    public TupleExpression visitVariableNames(VariableNamesContext ctx) {
        List<Expression> list = new ArrayList<>();
        for (VariableDeclaratorIdContext variableDeclaratorIdContext : ctx.variableDeclaratorId()) {
            VariableExpression variableExpression = visitVariableDeclaratorId(variableDeclaratorIdContext);
            list.add(variableExpression);
        }
        return configureAST(
                new TupleExpression(
                        list
                ),
                ctx);
    }

    @Override
    public BlockStatement visitBlockStatementsOpt(BlockStatementsOptContext ctx) {
        if (asBoolean(ctx.blockStatements())) {
            return configureAST(this.visitBlockStatements(ctx.blockStatements()), ctx);
        }

        return configureAST(this.createBlockStatement(), ctx);
    }

    @Override
    public BlockStatement visitBlockStatements(BlockStatementsContext ctx) {
        List<Statement> result = new ArrayList<>();
        for (BlockStatementContext blockStatementContext : ctx.blockStatement()) {
            Statement e = visitBlockStatement(blockStatementContext);
            if (asBoolean(e)) {
                result.add(e);
            }
        }
        return configureAST(this.createBlockStatement(result), ctx);
    }

    @Override
    public Statement visitBlockStatement(BlockStatementContext ctx) {
        if (asBoolean(ctx.localVariableDeclaration())) {
            return configureAST(this.visitLocalVariableDeclaration(ctx.localVariableDeclaration()), ctx);
        }

        if (asBoolean(ctx.statement())) {
            Object astNode = this.visit(ctx.statement()); //configureAST((Statement) this.visit(ctx.statement()), ctx);

            if (astNode instanceof MethodNode) {
                throw createParsingFailedException("Method definition not expected here", ctx);
            } else {
                return (Statement) astNode;
            }
        }

        throw createParsingFailedException("Unsupported block statement: " + ctx.getText(), ctx);
    }

    @Override
    public List<AnnotationNode> visitAnnotationsOpt(AnnotationsOptContext ctx) {
        if (!asBoolean(ctx)) {
            return Collections.emptyList();
        }

        List<AnnotationNode> list = new LinkedList<>();
        for (AnnotationContext c : ctx.annotation()) {
            list.add(this.visitAnnotation(c));
        }
        return list;
    }

    @Override
    public AnnotationNode visitAnnotation(AnnotationContext ctx) {
        String annotationName = this.visitAnnotationName(ctx.annotationName());
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(annotationName));
        List<Tuple2<String, Expression>> annotationElementValues = this.visitElementValues(ctx.elementValues());

        for (Tuple2<String, Expression> e : annotationElementValues) {
            annotationNode.addMember(e.getFirst(), e.getSecond());
        }

        // GRECLIPSE edit
        /*return*/ configureAST(annotationNode, ctx);
        // save the full source range of the annotation for future use
        long start = annotationNode.getStart(), until = annotationNode.getEnd();
        annotationNode.setNodeMetaData("source.offsets", (start << 32) | until);
        configureAST(annotationNode.getClassNode(), ctx.annotationName());
        // Eclipse has different requirements for error reporting:
        configureAST(annotationNode, ctx.annotationName());
        annotationNode.setEnd(annotationNode.getEnd() - 1);
        return annotationNode;
        // GRECLIPSE end
    }

    @Override
    public List<Tuple2<String, Expression>> visitElementValues(ElementValuesContext ctx) {
        if (!asBoolean(ctx)) {
            return Collections.emptyList();
        }

        List<Tuple2<String, Expression>> annotationElementValues = new LinkedList<>();

        if (asBoolean(ctx.elementValuePairs())) {
            for (Map.Entry<String, Expression> e : this.visitElementValuePairs(ctx.elementValuePairs()).entrySet()) {
                annotationElementValues.add(new Tuple2<>(e.getKey(), e.getValue()));
            }
        } else if (asBoolean(ctx.elementValue())) {
            annotationElementValues.add(new Tuple2<>(VALUE_STR, this.visitElementValue(ctx.elementValue())));
        }

        return annotationElementValues;
    }


    @Override
    public String visitAnnotationName(AnnotationNameContext ctx) {
        return this.visitQualifiedClassName(ctx.qualifiedClassName()).getName();
    }

    @Override
    public Map<String, Expression> visitElementValuePairs(ElementValuePairsContext ctx) {
        LinkedHashMap<String, Expression> map = new LinkedHashMap<>();
        for (ElementValuePairContext elementValuePairContext : ctx.elementValuePair()) {
            Tuple2<String, Expression> stringExpressionPair = visitElementValuePair(elementValuePairContext);
            if (map.containsKey(stringExpressionPair.getFirst())) {
                throw new IllegalStateException(String.format("Duplicate key %s", stringExpressionPair.getFirst()));
            } else{
                map.put(stringExpressionPair.getFirst(), stringExpressionPair.getSecond());
            }
        }
        return map;
    }

    @Override
    public Tuple2<String, Expression> visitElementValuePair(ElementValuePairContext ctx) {
        return new Tuple2<>(ctx.elementValuePairName().getText(), this.visitElementValue(ctx.elementValue()));
    }

    @Override
    public Expression visitElementValue(ElementValueContext ctx) {
        if (asBoolean(ctx.expression())) {
            return configureAST((Expression) this.visit(ctx.expression()), ctx);
        }

        if (asBoolean(ctx.annotation())) {
            return configureAST(new AnnotationConstantExpression(this.visitAnnotation(ctx.annotation())), ctx);
        }

        if (asBoolean(ctx.elementValueArrayInitializer())) {
            return configureAST(this.visitElementValueArrayInitializer(ctx.elementValueArrayInitializer()), ctx);
        }

        throw createParsingFailedException("Unsupported element value: " + ctx.getText(), ctx);
    }

    @Override
    public ListExpression visitElementValueArrayInitializer(ElementValueArrayInitializerContext ctx) {
        List<Expression> list = new ArrayList<>();
        for (ElementValueContext elementValueContext : ctx.elementValue()) {
            Expression expression = visitElementValue(elementValueContext);
            list.add(expression);
        }
        return configureAST(new ListExpression(list), ctx);
    }

    @Override
    public String visitClassName(ClassNameContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitIdentifier(IdentifierContext ctx) {
        return ctx.getText();
    }


    @Override
    public String visitQualifiedName(QualifiedNameContext ctx) {
        StringBuilder builder = new StringBuilder();
        for (QualifiedNameElementContext qualifiedNameElementContext : ctx.qualifiedNameElement()) {
            String text = qualifiedNameElementContext.getText();
            if (builder.length() > 0) {
                builder.append(DOT_STR);
            }
            builder.append(text);
        }
        return builder.toString();
    }

    @Override
    public ClassNode visitAnnotatedQualifiedClassName(AnnotatedQualifiedClassNameContext ctx) {
        ClassNode classNode = this.visitQualifiedClassName(ctx.qualifiedClassName());

        classNode.addAnnotations(this.visitAnnotationsOpt(ctx.annotationsOpt()));

        return classNode;
    }

    @Override
    public ClassNode[] visitQualifiedClassNameList(QualifiedClassNameListContext ctx) {
        if (!asBoolean(ctx)) {
            return ClassNode.EMPTY_ARRAY;
        }

        List<ClassNode> list = new ArrayList<>();
        for (AnnotatedQualifiedClassNameContext annotatedQualifiedClassNameContext : ctx.annotatedQualifiedClassName()) {
            ClassNode classNode = visitAnnotatedQualifiedClassName(annotatedQualifiedClassNameContext);
            list.add(classNode);
        }
        return list.toArray(new ClassNode[0]);
    }

    @Override
    public ClassNode visitQualifiedClassName(QualifiedClassNameContext ctx) {
        return this.createClassNode(ctx);
    }

    @Override
    public ClassNode visitQualifiedStandardClassName(QualifiedStandardClassNameContext ctx) {
        return this.createClassNode(ctx);
    }

    private ClassNode createClassNode(GroovyParserRuleContext ctx) {
        ClassNode result = ClassHelper.make(ctx.getText());

        if (!isTrue(ctx, IS_INSIDE_INSTANCEOF_EXPR)) { // type in the "instanceof" expression should not have proxy to redirect to it
            result = this.proxyClassNode(result);
        }

        return configureAST(result, ctx);
    }

    private ClassNode proxyClassNode(ClassNode classNode) {
        if (!classNode.isUsingGenerics()) {
            return classNode;
        }

        ClassNode cn = ClassHelper.makeWithoutCaching(classNode.getName());
        cn.setRedirect(classNode);

        return cn;
    }

    /**
     * Visit tree safely, no NPE occurred when the tree is null.
     *
     * @param tree an AST node
     * @return the visiting result
     */
    @Override
    public Object visit(ParseTree tree) {
        if (!asBoolean(tree)) {
            return null;
        }

        return super.visit(tree);
    }


    // e.g. obj.a(1, 2) or obj.a 1, 2
    private MethodCallExpression createMethodCallExpression(PropertyExpression propertyExpression, Expression arguments) {
        MethodCallExpression methodCallExpression =
                new MethodCallExpression(
                        propertyExpression.getObjectExpression(),
                        propertyExpression.getProperty(),
                        arguments
                );

        methodCallExpression.setImplicitThis(false);
        methodCallExpression.setSafe(propertyExpression.isSafe());
        methodCallExpression.setSpreadSafe(propertyExpression.isSpreadSafe());

        // method call obj*.m(): "safe"(false) and "spreadSafe"(true)
        // property access obj*.p: "safe"(true) and "spreadSafe"(true)
        // so we have to reset safe here.
        if (propertyExpression.isSpreadSafe()) {
            methodCallExpression.setSafe(false);
        }

        // if the generics types meta data is not empty, it is a generic method call, e.g. obj.<Integer>a(1, 2)
        methodCallExpression.setGenericsTypes( (GenericsType[])
                propertyExpression.getNodeMetaData(PATH_EXPRESSION_BASE_EXPR_GENERICS_TYPES));

        return methodCallExpression;
    }

    // e.g. m(1, 2) or m 1, 2
    private MethodCallExpression createMethodCallExpression(Expression baseExpr, Expression arguments) {
        return new MethodCallExpression(
                VariableExpression.THIS_EXPRESSION,

                (baseExpr instanceof VariableExpression)
                        ? this.createConstantExpression(baseExpr)
                        : baseExpr,

                arguments
        );
    }

    private Parameter processFormalParameter(GroovyParserRuleContext ctx,
                                             VariableModifiersOptContext variableModifiersOptContext,
                                             TypeContext typeContext,
                                             TerminalNode ellipsis,
                                             VariableDeclaratorIdContext variableDeclaratorIdContext,
                                             ExpressionContext expressionContext) {

        ClassNode classNode = this.visitType(typeContext);

        if (asBoolean(ellipsis)) {
            // GRECLIPSE edit
            //classNode = configureAST(classNode.makeArray(), classNode);
            if (!asBoolean(typeContext)) {
                classNode = configureAST(classNode.makeArray(), ellipsis);
            } else {
                classNode = configureAST(classNode.makeArray(), typeContext, configureAST(new ConstantExpression("..."), ellipsis));
            }
            // GRECLIPSE end
        }

        Parameter parameter =
                new ModifierManager(this, this.visitVariableModifiersOpt(variableModifiersOptContext))
                        .processParameter(
                                configureAST(
                                        new Parameter(
                                                classNode,
                                                this.visitVariableDeclaratorId(variableDeclaratorIdContext).getName()
                                        ),
                                        ctx
                                )
                        );

        if (asBoolean(expressionContext)) {
            parameter.setInitialExpression((Expression) this.visit(expressionContext));
        }

        return parameter;
    }

    private Expression createPathExpression(Expression primaryExpr, List<? extends PathElementContext> pathElementContextList) {
        Expression expr = primaryExpr;
        for (PathElementContext pathElementContext : pathElementContextList) {
            pathElementContext.putNodeMetaData(PATH_EXPRESSION_BASE_EXPR, expr);

            boolean isSafeChain = isTrue(expr, PATH_EXPRESSION_BASE_EXPR_SAFE_CHAIN);

            expr = this.visitPathElement(pathElementContext);
            // GRECLIPSE add
            Expression base = pathElementContext.getNodeMetaData(PATH_EXPRESSION_BASE_EXPR);
            expr.setColumnNumber(base.getColumnNumber());
            expr.setLineNumber(base.getLineNumber());
            expr.setStart(base.getStart());

            if (expr instanceof MethodCallExpression) {
                Expression meth = ((MethodCallExpression) expr).getMethod();
                if (meth instanceof ConstantExpression) {
                    expr.setNameStart(meth.getStart());
                    expr.setNameEnd(meth.getEnd() - 1);
                }
            }
            // GRECLIPSE end

            if (isSafeChain) {
                expr.putNodeMetaData(PATH_EXPRESSION_BASE_EXPR_SAFE_CHAIN, true);
            }
        }
        return expr;
    }

    private GenericsType createGenericsType(ClassNode classNode) {
        return configureAST(new GenericsType(classNode), classNode);
    }

    private ConstantExpression createConstantExpression(Expression expression) {
        if (expression instanceof ConstantExpression) {
            return (ConstantExpression) expression;
        }

        return configureAST(new ConstantExpression(expression.getText()), expression);
    }

    private BinaryExpression createBinaryExpression(ExpressionContext left, Token op, ExpressionContext right) {
        return new BinaryExpression((Expression) this.visit(left), this.createGroovyToken(op), (Expression) this.visit(right));
    }

    private BinaryExpression createBinaryExpression(ExpressionContext left, Token op, ExpressionContext right, ExpressionContext ctx) {
        BinaryExpression binaryExpression = this.createBinaryExpression(left, op, right);

        if (isTrue(ctx, IS_INSIDE_CONDITIONAL_EXPRESSION)) {
            return configureAST(binaryExpression, op);
        }

        return configureAST(binaryExpression, ctx);
    }

    private Statement unpackStatement(Statement statement) {
        if (statement instanceof DeclarationListStatement) {
            List<ExpressionStatement> expressionStatementList = ((DeclarationListStatement) statement).getDeclarationStatements();

            if (1 == expressionStatementList.size()) {
                return expressionStatementList.get(0);
            }

            return configureAST(this.createBlockStatement(statement), statement); // if DeclarationListStatement contains more than 1 declarations, maybe it's better to create a block to hold them
        }

        return statement;
    }

    BlockStatement createBlockStatement(Statement... statements) {
        return this.createBlockStatement(Arrays.asList(statements));
    }

    private BlockStatement createBlockStatement(List<Statement> statementList) {
        return this.appendStatementsToBlockStatement(new BlockStatement(), statementList);
    }

    public BlockStatement appendStatementsToBlockStatement(BlockStatement bs, Statement... statements) {
        return this.appendStatementsToBlockStatement(bs, Arrays.asList(statements));
    }

    private BlockStatement appendStatementsToBlockStatement(BlockStatement bs, List<Statement> statementList) {
        for (Statement e : statementList) {
            if (e instanceof DeclarationListStatement) {
                for (ExpressionStatement s : ((DeclarationListStatement) e).getDeclarationStatements()) {
                    bs.addStatement(s);
                }
            } else {
                bs.addStatement(e);
            }
        }
        return bs;
    }

    private boolean isAnnotationDeclaration(ClassNode classNode) {
        return asBoolean(classNode) && classNode.isAnnotationDefinition();
    }

    private boolean isSyntheticPublic(
            boolean isAnnotationDeclaration,
            boolean isAnonymousInnerEnumDeclaration,
            boolean hasReturnType,
            ModifierManager modifierManager
    ) {
        return this.isSyntheticPublic(
                isAnnotationDeclaration,
                isAnonymousInnerEnumDeclaration,
                modifierManager.containsAnnotations(),
                modifierManager.containsVisibilityModifier(),
                modifierManager.containsNonVisibilityModifier(),
                hasReturnType,
                modifierManager.contains(DEF));
    }

    /**
     * @param isAnnotationDeclaration         whether the method is defined in an annotation
     * @param isAnonymousInnerEnumDeclaration whether the method is defined in an anonymous inner enum
     * @param hasAnnotation                   whether the method declaration has annotations
     * @param hasVisibilityModifier           whether the method declaration contains visibility modifier(e.g. public, protected, private)
     * @param hasModifier                     whether the method declaration has modifier(e.g. final, static and so on)
     * @param hasReturnType                   whether the method declaration has an return type(e.g. String, generic types)
     * @param hasDef                          whether the method declaration using def keyword
     * @return the result
     */
    private boolean isSyntheticPublic(
            boolean isAnnotationDeclaration,
            boolean isAnonymousInnerEnumDeclaration,
            boolean hasAnnotation,
            boolean hasVisibilityModifier,
            boolean hasModifier,
            boolean hasReturnType,
            boolean hasDef) {

        if (hasVisibilityModifier) {
            return false;
        }
        return true;
        /* GRECLIPSE edit
        if (isAnnotationDeclaration) {
            return true;
        }

        if (hasDef && hasReturnType) {
            return true;
        }

        if (hasModifier || hasAnnotation || !hasReturnType) {
            return true;
        }

        return isAnonymousInnerEnumDeclaration;
        */
    }

    // the mixins of interface and annotation should be null
    private void hackMixins(ClassNode classNode) {
        classNode.setMixins(null);
    }

    private static final Map<ClassNode, Object> TYPE_DEFAULT_VALUE_MAP = Maps.<ClassNode, Object>of(
            ClassHelper.int_TYPE, 0,
            ClassHelper.long_TYPE, 0L,
            ClassHelper.double_TYPE, 0.0D,
            ClassHelper.float_TYPE, 0.0F,
            ClassHelper.short_TYPE, (short) 0,
            ClassHelper.byte_TYPE, (byte) 0,
            ClassHelper.char_TYPE, (char) 0,
            ClassHelper.boolean_TYPE, Boolean.FALSE
    );

    private Object findDefaultValueByType(ClassNode type) {
        return TYPE_DEFAULT_VALUE_MAP.get(type);
    }

    private boolean isPackageInfoDeclaration() {
        String name = this.sourceUnit.getName();

        return null != name && name.endsWith(PACKAGE_INFO_FILE_NAME);

    }

    private boolean isBlankScript() {
        return moduleNode.getStatementBlock().isEmpty() && moduleNode.getMethods().isEmpty() && moduleNode.getClasses().isEmpty();
    }

    private boolean isInsideParentheses(NodeMetaDataHandler nodeMetaDataHandler) {
        Integer insideParenLevel = nodeMetaDataHandler.getNodeMetaData(INSIDE_PARENTHESES_LEVEL);

        return null != insideParenLevel && insideParenLevel > 0;

    }

    private void addEmptyReturnStatement() {
        moduleNode.addStatement(ReturnStatement.RETURN_NULL_OR_VOID);
    }

    private void addPackageInfoClassNode() {
        List<ClassNode> classNodeList = moduleNode.getClasses();
        ClassNode packageInfoClassNode = ClassHelper.make(moduleNode.getPackageName() + PACKAGE_INFO);

        if (!classNodeList.contains(packageInfoClassNode)) {
            moduleNode.addClass(packageInfoClassNode);
        }
    }

    private org.codehaus.groovy.syntax.Token createGroovyTokenByType(Token token, int type) {
        if (null == token) {
            throw new IllegalArgumentException("token should not be null");
        }

        return new org.codehaus.groovy.syntax.Token(type, token.getText(), token.getLine(), token.getCharPositionInLine());
    }

    private org.codehaus.groovy.syntax.Token createGroovyToken(Token token) {
        return this.createGroovyToken(token, 1);
    }

    private org.codehaus.groovy.syntax.Token createGroovyToken(Token token, int cardinality) {
        String text = StringGroovyMethods.multiply((CharSequence) token.getText(), cardinality);
        return new org.codehaus.groovy.syntax.Token(
                "..<".equals(token.getText()) || "..".equals(token.getText())
                        ? Types.RANGE_OPERATOR
                        : Types.lookup(text, Types.ANY),
                text,
                token.getLine(),
                token.getCharPositionInLine() + 1
        );
    }

    /*
    private org.codehaus.groovy.syntax.Token createGroovyToken(String text, int startLine, int startColumn) {
        return new org.codehaus.groovy.syntax.Token(
                Types.lookup(text, Types.ANY),
                text,
                startLine,
                startColumn
        );
    }
    */

    /**
     * set the script source position
     */
    private void configureScriptClassNode() {
        ClassNode scriptClassNode = moduleNode.getScriptClassDummy();

        if (!asBoolean(scriptClassNode)) {
            return;
        }

        List<Statement> statements = moduleNode.getStatementBlock().getStatements();
        if (!statements.isEmpty()) {
            Statement firstStatement = statements.get(0);
            Statement lastStatement = statements.get(statements.size() - 1);

            scriptClassNode.setSourcePosition(firstStatement);
            scriptClassNode.setLastColumnNumber(lastStatement.getLastColumnNumber());
            scriptClassNode.setLastLineNumber(lastStatement.getLastLineNumber());
        }

    }

    private boolean isTrue(NodeMetaDataHandler nodeMetaDataHandler, String key) {
        Object nmd = nodeMetaDataHandler.getNodeMetaData(key);

        if (null == nmd) {
            return false;
        }

        if (!(nmd instanceof Boolean)) {
            throw new GroovyBugError(nodeMetaDataHandler + " node meta data[" + key + "] is not an instance of Boolean");
        }

        return (Boolean) nmd;
    }

    private CompilationFailedException createParsingFailedException(String msg, GroovyParserRuleContext ctx) {
        return createParsingFailedException(
                new SyntaxException(msg,
                        ctx.start.getLine(),
                        ctx.start.getCharPositionInLine() + 1,
                        ctx.stop.getLine(),
                        ctx.stop.getCharPositionInLine() + 1 + ctx.stop.getText().length()));
    }

    CompilationFailedException createParsingFailedException(String msg, ASTNode node) {
        Objects.requireNonNull(node, "node passed into createParsingFailedException should not be null");

        return createParsingFailedException(
                new SyntaxException(msg,
                        node.getLineNumber(),
                        node.getColumnNumber(),
                        node.getLastLineNumber(),
                        node.getLastColumnNumber()));
    }


    private CompilationFailedException createParsingFailedException(String msg, TerminalNode node) {
        return createParsingFailedException(msg, node.getSymbol());
    }

    private CompilationFailedException createParsingFailedException(String msg, Token token) {
        return createParsingFailedException(
                new SyntaxException(msg,
                        token.getLine(),
                        token.getCharPositionInLine() + 1,
                        token.getLine(),
                        token.getCharPositionInLine() + 1 + token.getText().length()));
    }

    private CompilationFailedException createParsingFailedException(Throwable t) {
        if (t instanceof SyntaxException) {
            this.collectSyntaxError((SyntaxException) t);
        } else if (t instanceof GroovySyntaxError) {
            GroovySyntaxError groovySyntaxError = (GroovySyntaxError) t;

            this.collectSyntaxError(
                    new SyntaxException(
                            groovySyntaxError.getMessage(),
                            groovySyntaxError,
                            groovySyntaxError.getLine(),
                            groovySyntaxError.getColumn()));
        } else if (t instanceof Exception) {
            this.collectException((Exception) t);
        }

        return new CompilationFailedException(
                CompilePhase.PARSING.getPhaseNumber(),
                this.sourceUnit,
                t);
    }

    private void collectSyntaxError(SyntaxException e) {
        sourceUnit.getErrorCollector().addFatalError(new SyntaxErrorMessage(e, sourceUnit));
    }

    private void collectException(Exception e) {
        sourceUnit.getErrorCollector().addException(e, this.sourceUnit);
    }

    private ANTLRErrorListener createANTLRErrorListener() {
        return new ANTLRErrorListener() {
            @Override
            public void syntaxError(
                    Recognizer recognizer,
                    Object offendingSymbol, int line, int charPositionInLine,
                    String msg, RecognitionException e) {

                collectSyntaxError(new SyntaxException(msg, line, charPositionInLine + 1));
            }
        };
    }

    private void removeErrorListeners() {
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
    }

    private void addErrorListeners() {
        lexer.removeErrorListeners();
        lexer.addErrorListener(this.createANTLRErrorListener());

        parser.removeErrorListeners();
        parser.addErrorListener(this.createANTLRErrorListener());
    }

    /*
    private String createExceptionMessage(Throwable t) {
        StringWriter sw = new StringWriter();

        try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
        }

        return sw.toString();
    }
    */

    private class DeclarationListStatement extends Statement {
        private final List<ExpressionStatement> declarationStatements;

        public DeclarationListStatement(DeclarationExpression... declarations) {
            this(Arrays.asList(declarations));
        }

        public DeclarationListStatement(List<DeclarationExpression> declarations) {
            List<ExpressionStatement> list = new ArrayList<>();
            for (DeclarationExpression e : declarations) {
                ExpressionStatement expressionStatement = configureAST(new ExpressionStatement(e), e);
                list.add(expressionStatement);
            }
            this.declarationStatements =
                    list;
        }

        public List<ExpressionStatement> getDeclarationStatements() {
            List<String> declarationListStatementLabels = this.getStatementLabels();

            for (ExpressionStatement e: this.declarationStatements) {
                if (null != declarationListStatementLabels) {
                    // clear existing statement labels before setting labels
                    if (null != e.getStatementLabels()) {
                        e.getStatementLabels().clear();
                    }
                    for (String s : declarationListStatementLabels) {
                        e.addStatementLabel(s);
                    }
                }
            }

            return this.declarationStatements;
        }

        public List<DeclarationExpression> getDeclarationExpressions() {
            List<DeclarationExpression> list = new ArrayList<>();
            for (ExpressionStatement e : this.declarationStatements) {
                DeclarationExpression expression = (DeclarationExpression) e.getExpression();
                list.add(expression);
            }
            return list;
        }
    }


    private final ModuleNode moduleNode;
    private final SourceUnit sourceUnit;
    private final GroovyLangLexer lexer;
    private final GroovyLangParser parser;
    private final TryWithResourcesASTTransformation tryWithResourcesASTTransformation;
    private final GroovydocManager groovydocManager;
    // GRECLIPSE add
    private final LocationSupport locationSupport;
    // GRECLIPSE add
    private final List<ClassNode> classNodeList = new LinkedList<>();
    private final Deque<ClassNode> classNodeStack = new ArrayDeque<>();
    private final Deque<List<InnerClassNode>> anonymousInnerClassesDefinedInMethodStack = new ArrayDeque<>();
    private int anonymousInnerClassCounter = 1;

    private Tuple2<GroovyParserRuleContext, Exception> numberFormatError;

    private int visitingLoopStatementCnt;
    private int visitingSwitchStatementCnt;
    private int visitingAssertStatementCnt;
    private int visitingClosureCnt;

    private static final String QUESTION_STR = "?";
    private static final String DOT_STR = ".";
    private static final String SUB_STR = "-";
    private static final String ASSIGN_STR = "=";
    private static final String VALUE_STR = "value";
    private static final String DOLLAR_STR = "$";
    private static final String CALL_STR = "call";
    private static final String THIS_STR = "this";
    private static final String SUPER_STR = "super";
    private static final String VOID_STR = "void";
    private static final String SLASH_STR = "/";
    private static final String SLASH_DOLLAR_STR = "/$";
    private static final String TDQ_STR = "\"\"\"";
    private static final String TSQ_STR = "'''";
    private static final String SQ_STR = "'";
    private static final String DQ_STR = "\"";
    private static final String DOLLAR_SLASH_STR = "$/";

    private static final Map<String, String> QUOTATION_MAP = Maps.of(
            DQ_STR, DQ_STR,
            SQ_STR, SQ_STR,
            TDQ_STR, TDQ_STR,
            TSQ_STR, TSQ_STR,
            SLASH_STR, SLASH_STR,
            DOLLAR_SLASH_STR, SLASH_DOLLAR_STR
    );

    private static final String PACKAGE_INFO = "package-info";
    private static final String PACKAGE_INFO_FILE_NAME = PACKAGE_INFO + ".groovy";
    private static final String GROOVY_TRANSFORM_TRAIT = "groovy.transform.Trait";
    private static final Set<String> PRIMITIVE_TYPE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("boolean", "char", "byte", "short", "int", "long", "float", "double")));

    private static final String INSIDE_PARENTHESES_LEVEL = "_INSIDE_PARENTHESES_LEVEL";

    private static final String IS_INSIDE_INSTANCEOF_EXPR = "_IS_INSIDE_INSTANCEOF_EXPR";
    private static final String IS_SWITCH_DEFAULT = "_IS_SWITCH_DEFAULT";
    private static final String IS_NUMERIC = "_IS_NUMERIC";
    private static final String IS_STRING = "_IS_STRING";
    private static final String IS_INTERFACE_WITH_DEFAULT_METHODS = "_IS_INTERFACE_WITH_DEFAULT_METHODS";
    private static final String IS_INSIDE_CONDITIONAL_EXPRESSION = "_IS_INSIDE_CONDITIONAL_EXPRESSION";
    private static final String IS_COMMAND_EXPRESSION = "_IS_COMMAND_EXPRESSION";

    private static final String PATH_EXPRESSION_BASE_EXPR = "_PATH_EXPRESSION_BASE_EXPR";
    private static final String PATH_EXPRESSION_BASE_EXPR_GENERICS_TYPES = "_PATH_EXPRESSION_BASE_EXPR_GENERICS_TYPES";
    private static final String PATH_EXPRESSION_BASE_EXPR_SAFE_CHAIN = "_PATH_EXPRESSION_BASE_EXPR_SAFE_CHAIN";
    private static final String CMD_EXPRESSION_BASE_EXPR = "_CMD_EXPRESSION_BASE_EXPR";
    private static final String TYPE_DECLARATION_MODIFIERS = "_TYPE_DECLARATION_MODIFIERS";
    private static final String CLASS_DECLARATION_CLASS_NODE = "_CLASS_DECLARATION_CLASS_NODE";
    private static final String VARIABLE_DECLARATION_VARIABLE_TYPE = "_VARIABLE_DECLARATION_VARIABLE_TYPE";
    private static final String ANONYMOUS_INNER_CLASS_SUPER_CLASS = "_ANONYMOUS_INNER_CLASS_SUPER_CLASS";
    private static final String INTEGER_LITERAL_TEXT = "_INTEGER_LITERAL_TEXT";
    private static final String FLOATING_POINT_LITERAL_TEXT = "_FLOATING_POINT_LITERAL_TEXT";

    private static final String CLASS_NAME = "_CLASS_NAME";
}
