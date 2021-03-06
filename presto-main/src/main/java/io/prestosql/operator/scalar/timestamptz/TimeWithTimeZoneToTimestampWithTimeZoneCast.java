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
package io.prestosql.operator.scalar.timestamptz;

import io.prestosql.spi.function.LiteralParameters;
import io.prestosql.spi.function.ScalarOperator;
import io.prestosql.spi.function.SqlType;
import io.prestosql.spi.type.LongTimestampWithTimeZone;
import io.prestosql.spi.type.StandardTypes;

import static io.prestosql.spi.function.OperatorType.CAST;
import static io.prestosql.spi.type.DateTimeEncoding.unpackMillisUtc;
import static io.prestosql.spi.type.DateTimeEncoding.unpackZoneKey;

@ScalarOperator(CAST)
public final class TimeWithTimeZoneToTimestampWithTimeZoneCast
{
    private TimeWithTimeZoneToTimestampWithTimeZoneCast() {}

    @LiteralParameters("p")
    @SqlType("timestamp(p) with time zone")
    public static long castToShort(@SqlType(StandardTypes.TIME_WITH_TIME_ZONE) long time)
    {
        return time;
    }

    @LiteralParameters("p")
    @SqlType("timestamp(p) with time zone")
    public static LongTimestampWithTimeZone castToLong(@SqlType(StandardTypes.TIME_WITH_TIME_ZONE) long time)
    {
        return LongTimestampWithTimeZone.fromEpochMillisAndFraction(unpackMillisUtc(time), 0, unpackZoneKey(time));
    }
}
