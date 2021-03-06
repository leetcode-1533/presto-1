/*
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
package io.prestosql.sql.planner;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.prestosql.Session;
import io.prestosql.execution.warnings.WarningCollector;
import io.prestosql.plugin.tpch.ColumnNaming;
import io.prestosql.plugin.tpch.TpchConnectorFactory;
import io.prestosql.testing.LocalQueryRunner;
import io.prestosql.tpch.Customer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.runner.options.WarmupMode;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.prestosql.plugin.tpch.TpchConnectorFactory.TPCH_COLUMN_NAMING_PROPERTY;
import static io.prestosql.sql.planner.LogicalPlanner.Stage.OPTIMIZED;
import static io.prestosql.testing.TestingSession.testSessionBuilder;
import static java.lang.String.format;

@SuppressWarnings("MethodMayBeStatic")
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Fork(1)
@Measurement(iterations = 20)
@BenchmarkMode(Mode.AverageTime)
public class BenchmarkPlanner
{
    @SuppressWarnings("FieldMayBeFinal")
    @State(Scope.Benchmark)
    public static class BenchmarkData
    {
        @Param({"optimized", "created"})
        private String stage = OPTIMIZED.toString();

        private LocalQueryRunner queryRunner;
        private List<String> queries;
        private Session session;

        @Setup
        public void setup()
        {
            String tpch = "tpch";

            session = testSessionBuilder()
                    .setCatalog(tpch)
                    .setSchema("sf1")
                    .build();

            queryRunner = LocalQueryRunner.create(session);
            queryRunner.createCatalog(tpch, new TpchConnectorFactory(4), ImmutableMap.of(TPCH_COLUMN_NAMING_PROPERTY, ColumnNaming.STANDARD.name()));

            queries = IntStream.rangeClosed(1, 22)
                    .boxed()
                    .filter(i -> i != 15) // q15 has two queries in it
                    .map(i -> readResource(format("/io/airlift/tpch/queries/q%d.sql", i)))
                    .collect(toImmutableList());
        }

        @TearDown
        public void tearDown()
        {
            queryRunner.close();
            queryRunner = null;
        }

        public String readResource(String resource)
        {
            try {
                URL resourceUrl = Customer.class.getResource(resource);
                return Resources.toString(resourceUrl, StandardCharsets.UTF_8);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Benchmark
    public List<Plan> planQueries(BenchmarkData benchmarkData)
    {
        return benchmarkData.queryRunner.inTransaction(transactionSession -> {
            LogicalPlanner.Stage stage = LogicalPlanner.Stage.valueOf(benchmarkData.stage.toUpperCase());
            return benchmarkData.queries.stream()
                    .map(query -> benchmarkData.queryRunner.createPlan(transactionSession, query, stage, false, WarningCollector.NOOP))
                    .collect(toImmutableList());
        });
    }

    public static void main(String[] args)
            throws Exception
    {
        // assure the benchmarks are valid before running
        BenchmarkData data = new BenchmarkData();
        data.setup();
        try {
            new BenchmarkPlanner().planQueries(data);
        }
        finally {
            data.tearDown();
        }

        Options options = new OptionsBuilder()
                .verbosity(VerboseMode.NORMAL)
                .warmupMode(WarmupMode.BULK)
                .include(".*" + BenchmarkPlanner.class.getSimpleName() + ".*")
                .build();
        new Runner(options).run();
    }
}
