/*
 * Copyright © 2015 Stefan Niederhauser (nidin@gmx.ch)
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
package guru.nidi.graphviz;

import edu.umd.cs.findbugs.Priorities;
import guru.nidi.codeassert.checkstyle.*;
import guru.nidi.codeassert.config.AnalyzerConfig;
import guru.nidi.codeassert.config.In;
import guru.nidi.codeassert.dependency.*;
import guru.nidi.codeassert.findbugs.*;
import guru.nidi.codeassert.junit.CodeAssertJunit5Test;
import guru.nidi.codeassert.model.Model;
import guru.nidi.codeassert.pmd.*;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.*;
import guru.nidi.graphviz.service.CommandRunner;
import net.sourceforge.pmd.RulePriority;
import org.junit.jupiter.api.Test;

import static guru.nidi.codeassert.junit.CodeAssertMatchers.matchesRulesExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class CodeAnalysisTest extends CodeAssertJunit5Test {

    @Test
    void dependencies() {
        assertThat(dependencyResult(), matchesRulesExactly());
    }

    @Override
    protected DependencyResult analyzeDependencies() {
        class GuruNidiGraphviz extends DependencyRuler {
            DependencyRule model, attribute, attributeValidate, engine, parse, service, use;

            public void defineRules() {
                base().mayBeUsedBy(all());
                engine.mayUse(model, service);
                parse.mayUse(model, attribute, attributeValidate);
                model.mayUse(attribute);
                attributeValidate.mayUse(attribute);
                use.mayUse(all());
            }
        }
        final DependencyRules rules = DependencyRules.denyAll()
                .withExternals("java.*", "javax.*", "com.*", "org.*")
                .withRelativeRules(new GuruNidiGraphviz());
        return new DependencyAnalyzer(AnalyzerConfig.maven().main()).rules(rules).analyze();
    }

    @Override
    protected Model createModel() {
        return Model.from(AnalyzerConfig.maven().main().getClasses()).read();
    }

    @Override
    protected FindBugsResult analyzeFindBugs() {
        final BugCollector collector = new BugCollector().minPriority(Priorities.NORMAL_PRIORITY)
                .apply(FindBugsConfigs.dependencyTestIgnore(CodeAnalysisTest.class))
                .because("It's examples", In.loc("ReadmeTest").ignore("DLS_DEAD_LOCAL_STORE"))
                .because("GraphvizServer is on localhost",
                        In.locs("GraphvizServer", "GraphvizServerEngine")
                                .ignore("UNENCRYPTED_SERVER_SOCKET", "UNENCRYPTED_SOCKET"))
                .because("It's ok",
                        In.loc("Datatype").ignore("NP_BOOLEAN_RETURN_NULL"),
                        In.loc("BuiltInRasterizer").ignore("NP_NONNULL_RETURN_VIOLATION"),
                        In.loc("CommandLineExecutor").ignore("DM_DEFAULT_ENCODING"),
                        In.loc("GraphvizServer").ignore("COMMAND_INJECTION", "CRLF_INJECTION_LOGS"),
                        In.locs("AbstractGraphvizEngine", "Options", "GraphvizCmdLineEngine", "EngineTest", "SystemUtils", "Renderer").ignore("PATH_TRAVERSAL_IN"),
                        In.locs("EngineTest", "RendererTest", "EngineResult").ignore("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"),
                        In.loc("PortSource").ignore("NP_NONNULL_RETURN_VIOLATION"),
                        In.loc("OptionsTest").ignore("DMI_HARDCODED_ABSOLUTE_FILENAME"),
                        In.loc("SimpleLabel").ignore("IM_BAD_CHECK_FOR_ODD"),
                        In.loc("JavascriptEngineTest").ignore("PREDICTABLE_RANDOM"),
                        In.loc("Communicator").ignore("RR_NOT_CHECKED"));
        return new FindBugsAnalyzer(AnalyzerConfig.maven().mainAndTest(), collector).analyze();
    }

    @Override
    protected PmdResult analyzePmd() {
        final PmdViolationCollector collector = new PmdViolationCollector().minPriority(RulePriority.MEDIUM)
                .apply(PmdConfigs.minimalPmdIgnore())
                .because("It's examples", In.locs("ExampleTest", "ReadmeTest")
                        .ignore("JUnitTestsShouldIncludeAssert", "LocalVariableCouldBeFinal", "UnusedLocalVariable"))
                .because("It's a test", In.loc("*Test")
                        .ignore("ExcessiveMethodLength"))
                .because("It's a bug in PMD?",
                        In.clazz(MutableNode.class).ignore("ConstructorCallsOverridableMethod"),
                        In.loc("Serializer#doGraph").ignore("ConsecutiveLiteralAppends"))
                .because("There are a lot of colors", In.clazz(Color.class)
                        .ignore("FieldDeclarationsShouldBeAtStartOfClass"))
                .because("it's ok here",
                        In.locs("Format", "AttributeConfigs").ignore("AvoidDuplicateLiterals"),
                        In.locs("LabelTest", "RankTest", "AttributeValidatorTest", "ParserTest", "JavascriptEngineTest").ignore("JUnitTestContainsTooManyAsserts"),
                        In.loc("Serializer").ignore("AvoidStringBufferField", "CompareObjectsWithEquals"),
                        In.locs("ThrowingFunction", "IoUtils").ignore("AvoidThrowingRawExceptionTypes", "AvoidCatchingGenericException"),
                        In.loc("GraphvizServer").ignore("AvoidInstantiatingObjectsInLoops"),
                        In.clazz(Shape.class).ignore("AvoidFieldNameMatchingTypeName"),
                        In.loc("CommandRunnerTest").ignore("JUnitTestsShouldIncludeAssert"),
                        In.locs("Lexer", "ParserImpl", "ImmutableGraph", "MutableGraph", "Label#applyTo", "Rank$GraphRank#applyTo", "Options#toJson", "Options#fromJson")
                                .ignore("CyclomaticComplexity", "StdCyclomaticComplexity", "ModifiedCyclomaticComplexity", "NPathComplexity"),
                        In.classes(GraphvizJdkEngine.class, GraphvizV8Engine.class, GraphvizServerEngine.class, AbstractGraphvizEngine.class)
                                .ignore("PreserveStackTrace", "SignatureDeclareThrowsException", "AvoidCatchingGenericException"),
                        In.locs("MutableGraph", "Serializer", "ParserImpl", "Label").ignore("GodClass"),
                        In.locs("ImmutableGraph", "MutableGraph").ignore("ExcessiveMethodLength", "ExcessiveParameterList", "LooseCoupling"),
                        In.locs("Format", "ImmutableGraph$GraphAttributed").ignore("AccessorMethodGeneration"),
                        In.locs("AttributeConfigs", "AttributeValidator").ignore("TooManyStaticImports"),
                        In.classes(MutableNode.class, Rasterizer.class).ignore("ConfusingTernary"),
                        In.clazz(ThrowingFunction.class).ignore("AvoidRethrowingException"),
                        In.classes(ThrowingFunction.class, ThrowingBiConsumer.class).ignore("SignatureDeclareThrowsException"))
                .because("It's command line tool", In.loc("GraphvizServer")
                        .ignore("AvoidCatchingGenericException", "PreserveStackTrace"))
                .because("I don't understand the message",
                        In.locs("CommandRunnerTest", "AbstractJsGraphvizEngine").ignore("SimplifiedTernary"))
                .because("I don't agree",
                        In.loc("Datatype").ignore("PositionLiteralsFirstInCaseInsensitiveComparisons"),
                        In.clazz(CommandRunner.class).ignore("OptimizableToArrayCall"),
                        In.everywhere().ignore("SimplifyStartsWith"))
                .because("It's wrapping an Exception with a RuntimeException",
                        In.classes(Graphviz.class, CreationContext.class).ignore("AvoidCatchingGenericException"));
        return new PmdAnalyzer(AnalyzerConfig.maven().mainAndTest(), collector)
                .withRulesets(PmdConfigs.defaultPmdRulesets())
                .analyze();
    }

    @Override
    protected CpdResult analyzeCpd() {
        final CpdMatchCollector collector = new CpdMatchCollector()
                .apply(PmdConfigs.cpdIgnoreEqualsHashCodeToString())
                .because("It's java",
                        In.loc("*Graph").ignore("Graph(strict, directed, cluster, name,", "if (strict != graph.strict) {"),
                        In.loc("Format").ignore("String preProcess(String src) {"))
                .just(In.locs("GraphvizGraalEngine", "GraphvizNashornEngine").ignore("void doInit()"));
        return new CpdAnalyzer(AnalyzerConfig.maven().main(), 36, collector).analyze();
    }

    @Override
    protected CheckstyleResult analyzeCheckstyle() {
        final StyleEventCollector collector = new StyleEventCollector()
                .apply(CheckstyleConfigs.minimalCheckstyleIgnore())
                .just(In.locs("Color", "Arrow", "Rank", "Shape", "Token", "Style", "Options", "Records", "SystemUtils", "GraphAttr").ignore("empty.line.separator"))
                .just(In.clazz(For.class).ignore("one.top.level.class"))
                .just(In.locs("EngineResult", "IOFunction").ignore("abbreviation.as.word"))
                .just(In.clazz(Renderer.class).ignore("indentation.error"));
        final StyleChecks checks = CheckstyleConfigs.adjustedGoogleStyleChecks();
        return new CheckstyleAnalyzer(AnalyzerConfig.maven().main(), checks, collector).analyze();
    }
}
